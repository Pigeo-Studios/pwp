package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.server.MarkerManager;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketPlaceMarker {
    public final String team, category, iconType;
    public final BlockPos pos;

    public PacketPlaceMarker(String team, String category, String iconType, BlockPos pos) {
        this.team = team;
        this.category = category;
        this.iconType = iconType;
        this.pos = pos;
    }

    public static void encode(PacketPlaceMarker msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.team);
        buf.writeUtf(msg.category);
        buf.writeUtf(msg.iconType);
        buf.writeBlockPos(msg.pos);
    }

    public static PacketPlaceMarker decode(FriendlyByteBuf buf) {
        return new PacketPlaceMarker(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBlockPos());
    }

    public static void handle(PacketPlaceMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p == null) return;
            String pName = p.getScoreboardName();
            WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
            boolean authorized = false;
            for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
                if (s.members.contains(pName)
                        && (s.leader.equals(pName) || s.bravoLeader.equals(pName) || s.charlieLeader.equals(pName))) {
                    authorized = true;
                    break;
                }
            }
            if (!authorized) {
                PlayerFeedback.deny(p, "pwpwarfare.msg.marker.leaders_only");
                return;
            }
            // поле team из пакета — ВИДИМОСТЬ маркера (squad/team/enemy), не команда: команду берёт сервер
            String scope = msg.team;
            if (!"squad".equals(scope) && !"team".equals(scope) && !"enemy".equals(scope)) return;
            if (msg.category.length() > 32 || msg.iconType.length() > 32) return;
            String ownerTeam = p.getTeam() != null ? p.getTeam().getName() : "";
            if (ownerTeam.isEmpty()) {
                PlayerFeedback.deny(p, "pwpwarfare.msg.marker.no_team");
                return;
            }
            // время маркеров — серверные тики (MarkerManager.onTick считает возраст в них же)
            long tick = GameClock.serverTicks(p.getServer());
            MapMarker m =
                    new MapMarker(UUID.randomUUID(), scope, msg.category, msg.iconType, msg.pos, p.getUUID(), tick);
            MarkerManager.add(m, ownerTeam);
            MarkerManager.broadcastToTeam(p, m);
        });
        ctx.get().setPacketHandled(true);
    }
}

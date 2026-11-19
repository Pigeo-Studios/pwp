package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketForceCMD {
    public final int squadId;
    public final String team;

    public PacketForceCMD(int squadId, String team) {
        this.squadId = squadId;
        this.team = team;
    }

    public static void encode(PacketForceCMD msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.squadId);
        buf.writeUtf(msg.team);
    }

    public static PacketForceCMD decode(FriendlyByteBuf buf) {
        return new PacketForceCMD(buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketForceCMD msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p == null) return;
            // право — единый PwpPermissions (op ИЛИ роль БД), раньше только op; без синка клиенты не видели смену
            if (!com.pwp.api.security.PwpPermissions.isAdmin(p)) {
                PlayerFeedback.deny(p, "pwpwarfare.msg.no_permission");
                return;
            }
            WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
            if (data == null) return;
            boolean isBlue = msg.team.toUpperCase().contains("BLUE");
            if (isBlue) data.squadRegistry.blueCMDId = msg.squadId;
            else data.squadRegistry.redCMDId = msg.squadId;
            data.setDirty();
            PacketHandler.syncSquadsAll(data.squadRegistry.squads);
            PacketHandler.sendToAllClients(p.serverLevel(), data);
            PlayerFeedback.info(p, "pwpwarfare.msg.cmd.forced", msg.squadId);
        });
        ctx.get().setPacketHandled(true);
    }
}

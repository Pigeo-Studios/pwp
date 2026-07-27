package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.server.MarkerManager;
import com.pigeostudios.pwp.warfare.world.MapMarker;
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
        this.team = team; this.category = category; this.iconType = iconType; this.pos = pos;
    }

    public static void encode(PacketPlaceMarker msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.team); buf.writeUtf(msg.category); buf.writeUtf(msg.iconType); buf.writeBlockPos(msg.pos);
    }

    public static PacketPlaceMarker decode(FriendlyByteBuf buf) {
        return new PacketPlaceMarker(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBlockPos());
    }

    public static void handle(PacketPlaceMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p == null) return;
            long tick = p.serverLevel().getGameTime();
            MapMarker m = new MapMarker(UUID.randomUUID(), msg.team, msg.category, msg.iconType, msg.pos, p.getUUID(), tick);
            MarkerManager.add(m);
            MarkerManager.broadcastToTeam(p, m);
        });
        ctx.get().setPacketHandled(true);
    }
}

package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.MarkerClientCache;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class PacketSyncMarker {
    public final UUID id, ownerUUID;
    public final String team, category, iconType;
    public final BlockPos pos;
    public final long remainingTicks;

    public PacketSyncMarker(MapMarker m) {
        this.id = m.id; this.team = m.team; this.category = m.category; this.iconType = m.iconType;
        this.pos = m.pos; this.ownerUUID = m.ownerUUID;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        long now = s != null ? s.getTickCount() : 0;
        long elapsed = Math.min(6000, now - m.createdAt);
        this.remainingTicks = Math.max(0, 6000 - elapsed);
    }

    private PacketSyncMarker(UUID id, String team, String category, String iconType, BlockPos pos, UUID ownerUUID, long remainingTicks) {
        this.id = id; this.team = team; this.category = category; this.iconType = iconType;
        this.pos = pos; this.ownerUUID = ownerUUID; this.remainingTicks = remainingTicks;
    }

    public static void encode(PacketSyncMarker msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.id); buf.writeUtf(msg.team); buf.writeUtf(msg.category); buf.writeUtf(msg.iconType);
        buf.writeBlockPos(msg.pos); buf.writeUUID(msg.ownerUUID); buf.writeLong(msg.remainingTicks);
    }

    public static PacketSyncMarker decode(FriendlyByteBuf buf) {
        return new PacketSyncMarker(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readUUID(), buf.readLong());
    }

    public static void handle(PacketSyncMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            long clientTick = mc.level != null ? mc.level.getGameTime() : 0;
            long adjustedCreatedAt = clientTick - (6000 - Math.min(6000, msg.remainingTicks));
            MapMarker m = new MapMarker(msg.id, msg.team, msg.category, msg.iconType, msg.pos, msg.ownerUUID, adjustedCreatedAt);
            MarkerClientCache.add(m);
        });
        ctx.get().setPacketHandled(true);
    }
}

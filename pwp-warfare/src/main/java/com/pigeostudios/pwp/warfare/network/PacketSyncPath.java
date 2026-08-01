package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.PathCache;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncPath {
    public final UUID pathId;
    public final String team, type;
    public final int squadNum;
    public final long createdAt;
    public final List<PathPoint> points;

    public PacketSyncPath(UUID pathId, String team, String type, int squadNum, long createdAt, List<PathPoint> points) {
        this.pathId = pathId; this.team = team; this.type = type;
        this.squadNum = squadNum; this.createdAt = createdAt; this.points = points;
    }

    public static void encode(PacketSyncPath msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.pathId); buf.writeUtf(msg.team); buf.writeUtf(msg.type);
        buf.writeInt(msg.squadNum); buf.writeLong(msg.createdAt);
        buf.writeInt(msg.points.size());
        for (var p : msg.points) { buf.writeDouble(p.x); buf.writeDouble(p.z); }
    }

    public static PacketSyncPath decode(FriendlyByteBuf buf) {
        UUID pathId = buf.readUUID();
        String team = buf.readUtf(), type = buf.readUtf();
        int squadNum = buf.readInt();
        long createdAt = buf.readLong();
        int size = buf.readInt();
        List<PathPoint> pts = new ArrayList<>();
        for (int i = 0; i < size; i++) pts.add(new PathPoint(buf.readDouble(), buf.readDouble()));
        return new PacketSyncPath(pathId, team, type, squadNum, createdAt, pts);
    }

    public static void handle(PacketSyncPath msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PathCache.serverPaths.put(msg.pathId, new PathCache.ServerPath(msg.type, msg.squadNum, msg.points, msg.createdAt));
        });
        ctx.get().setPacketHandled(true);
    }
}

package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.server.PathManager;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketPlacePath {
    public final UUID pathId;
    public final String team, type;
    public final int squadNum;
    public final List<PathPoint> points;

    public PacketPlacePath(UUID pathId, String team, String type, int squadNum, List<PathPoint> points) {
        this.pathId = pathId; this.team = team; this.type = type;
        this.squadNum = squadNum; this.points = points;
    }

    public static void encode(PacketPlacePath msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.pathId); buf.writeUtf(msg.team); buf.writeUtf(msg.type);
        buf.writeInt(msg.squadNum); buf.writeInt(msg.points.size());
        for (var p : msg.points) { buf.writeDouble(p.x); buf.writeDouble(p.z); }
    }

    public static PacketPlacePath decode(FriendlyByteBuf buf) {
        UUID pathId = buf.readUUID();
        String team = buf.readUtf(), type = buf.readUtf();
        int squadNum = buf.readInt();
        int size = buf.readInt();
        List<PathPoint> pts = new ArrayList<>();
        for (int i = 0; i < size; i++) pts.add(new PathPoint(buf.readDouble(), buf.readDouble()));
        return new PacketPlacePath(pathId, team, type, squadNum, pts);
    }

    public static void handle(PacketPlacePath msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p == null) return;
            String pName = p.getScoreboardName();
            WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
            boolean authorized = false;
            for (WarfareWorldData.Squad s : data.squads) {
                if (s.members.contains(pName) && (s.leader.equals(pName) || s.bravoLeader.equals(pName) || s.charlieLeader.equals(pName))) {
                    authorized = true; break;
                }
            }
            if (!authorized) return;
            PathManager.add(msg);
            PathManager.broadcastToTeam(p, msg);
        });
        ctx.get().setPacketHandled(true);
    }
}

package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlacePath;
import com.pigeostudios.pwp.warfare.network.PacketRemovePath;
import com.pigeostudios.pwp.warfare.network.PacketSyncPath;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

public class PathManager {
    public record StoredPath(UUID id, String owner, String team, String type, int squadNum, List<PathPoint> points, long createdAt) {}

    private static final List<StoredPath> all = new ArrayList<>();

    public static void add(PacketPlacePath msg) {
        long now = 0;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) now = s.getTickCount();
        all.add(new StoredPath(msg.pathId, "", msg.team, msg.type, msg.squadNum, msg.points, now));
    }

    public static void setOwner(UUID pathId, String owner) {
        for (int i = 0; i < all.size(); i++) {
            var s = all.get(i);
            if (s.id.equals(pathId)) all.set(i, new StoredPath(s.id, owner, s.team, s.type, s.squadNum, s.points, s.createdAt));
        }
    }

    public static void removeById(UUID id) { all.removeIf(s -> s.id.equals(id)); }
    public static void clearAll() { all.clear(); }

    public static StoredPath findById(UUID id) {
        return all.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
    }

    public static void broadcastToTeam(ServerPlayer player, PacketPlacePath msg) {
        String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
        setOwner(msg.pathId, player.getScoreboardName());
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;
        long now = s.getTickCount();
        PacketSyncPath sync = new PacketSyncPath(msg.pathId, msg.team, msg.type, msg.squadNum, now, msg.points);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            String pt = p.getTeam() != null ? p.getTeam().getName() : "";
            if (pt.equalsIgnoreCase(teamName))
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), sync);
        }
    }

    public static void broadcastRemovalToTeam(ServerPlayer player, UUID pathId) {
        String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;
        PacketRemovePath pkt = new PacketRemovePath(pathId);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            String pt = p.getTeam() != null ? p.getTeam().getName() : "";
            if (pt.equalsIgnoreCase(teamName))
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        }
    }

    public static boolean isTeamCMD(ServerPlayer p, String team) {
        WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
        if (data == null) return false;
        int cmdId = team.toUpperCase().contains("BLUE") ? data.blueCMDId : data.redCMDId;
        if (cmdId == -1) return false;
        String pName = p.getScoreboardName();
        for (var s : data.squads) {
            if (s.id == cmdId && s.leader.equals(pName)) return true;
        }
        return false;
    }

    public static void syncToPlayer(ServerPlayer player) {
        String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
        for (StoredPath p : all) {
            if (p.team().equalsIgnoreCase(teamName)) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncPath(p.id(), p.team(), p.type(), p.squadNum(), p.createdAt(), p.points()));
            }
        }
    }

    public static void saveTo(WarfareWorldData data) {
        data.savedPaths.clear();
        for (StoredPath p : all) {
            data.savedPaths.add(new WarfareWorldData.PathSnapshot(
                p.id(), p.owner(), p.team(), p.type(), p.squadNum(), p.createdAt(), new ArrayList<>(p.points())));
        }
    }

    public static void loadFrom(WarfareWorldData data) {
        all.clear();
        for (var snap : data.savedPaths) {
            all.add(new StoredPath(snap.id(), snap.owner(), snap.team(), snap.type(), snap.squadNum(), new ArrayList<>(snap.points()), snap.createdAt()));
        }
    }
}

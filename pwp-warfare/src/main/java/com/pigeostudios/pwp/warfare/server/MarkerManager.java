package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSyncMarker;
import com.pigeostudios.pwp.warfare.network.PacketRemoveMarker;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber
public class MarkerManager {
    private static final List<MapMarker> all = new ArrayList<>();

    public static void add(MapMarker m) { all.add(m); }
    public static void remove(MapMarker m) { all.remove(m); }
    public static void removeById(UUID id) { all.removeIf(m -> m.id.equals(id)); }
    public static void clearAll() { all.clear(); }
    public static MapMarker findById(UUID id) { return all.stream().filter(m -> m.id.equals(id)).findFirst().orElse(null); }

    public static void broadcastToTeam(ServerPlayer player, MapMarker m) {
        String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            String pt = p.getTeam() != null ? p.getTeam().getName() : "";
            if (pt.equalsIgnoreCase(teamName))
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), new PacketSyncMarker(m));
        }
    }

    public static void broadcastRemoval(UUID id) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketRemoveMarker(id));
    }

    public static void syncToPlayer(ServerPlayer player) {
        String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
        for (MapMarker m : all) {
            if (m.team.equalsIgnoreCase(teamName))
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncMarker(m));
        }
    }

    public static void saveTo(WarfareWorldData data) {
        data.savedMarkers.clear();
        for (MapMarker m : all) {
            data.savedMarkers.add(new WarfareWorldData.MarkerSnapshot(
                m.id, m.team, m.category, m.iconType, m.pos, m.ownerUUID, m.createdAt));
        }
    }

    public static void loadFrom(WarfareWorldData data) {
        all.clear();
        for (var snap : data.savedMarkers) {
            all.add(new MapMarker(snap.id(), snap.team(), snap.category(), snap.iconType(), snap.pos(), snap.ownerUUID(), snap.createdAt()));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null || s.getTickCount() % 20 != 0) return;
        long now = s.getTickCount();
        boolean changed = false;
        var it = all.iterator();
        while (it.hasNext()) {
            MapMarker m = it.next();
            if (m.isExpired(now)) {
                broadcastRemoval(m.id);
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            var level = s.overworld();
            if (level != null) {
                WarfareWorldData data = WarfareWorldData.get(level);
                if (data != null) saveTo(data);
            }
        }
    }
}

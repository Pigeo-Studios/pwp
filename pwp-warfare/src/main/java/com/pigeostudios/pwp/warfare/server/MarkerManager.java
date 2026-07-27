package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSyncMarker;
import com.pigeostudios.pwp.warfare.network.PacketRemoveMarker;
import com.pigeostudios.pwp.warfare.world.MapMarker;
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
    public static MapMarker findById(UUID id) { return all.stream().filter(m -> m.id.equals(id)).findFirst().orElse(null); }

    public static void broadcastToTeam(ServerPlayer player, MapMarker m) {
        String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            String pt = p.getTeam() != null ? p.getTeam().getName() : "";
            if (pt.equalsIgnoreCase(teamName)) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), new PacketSyncMarker(m));
            }
        }
    }

    public static void broadcastRemoval(UUID id) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketRemoveMarker(id));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null || s.getTickCount() % 20 != 0) return;
        long now = s.getTickCount();
        all.removeIf(m -> { if (m.isExpired(now)) { broadcastRemoval(m.id); return true; } return false; });
    }
}

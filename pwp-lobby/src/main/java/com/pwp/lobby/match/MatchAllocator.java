package com.pwp.lobby.match;

import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.lobby.ServerManager;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchAllocator {

    private static final Logger log = LoggerFactory.getLogger(MatchAllocator.class);

    private static final Map<Integer, MatchInfo> activeMatches = new ConcurrentHashMap<>();
    private static final Set<String> lobbyPlayers = ConcurrentHashMap.newKeySet();
    private static int minPlayersToStart = 2;
    private static int fillPercent = 80;

    public static void configure(int minPlayers, int fillPct) {
        minPlayersToStart = minPlayers;
        fillPercent = fillPct;
    }

    public static void playerJoined(String uuid) {
        lobbyPlayers.add(uuid);
    }

    public static void playerLeft(String uuid) {
        lobbyPlayers.remove(uuid);
    }

    /** Returns true if any match is in STARTING or PLAYING phase */
    public static boolean hasActiveMatch() {
        return activeMatches.values().stream()
                .anyMatch(m -> m.phase == MatchPhase.STARTING || m.phase == MatchPhase.PLAYING);
    }

    public static synchronized void startMatch(MapConfig map) {
        int lobbyCount = lobbyPlayers.size();
        if (lobbyCount < minPlayersToStart) {
            log.info("Not enough players: {}/{}", lobbyCount, minPlayersToStart);
            return;
        }

        boolean alreadyRunning = activeMatches.values().stream()
                .anyMatch(m -> m.phase == MatchPhase.STARTING || m.phase == MatchPhase.PLAYING);
        if (alreadyRunning) {
            log.info("A match is already starting or running");
            return;
        }

        ServerManager.StartResult sr = ServerManager.startMatchServer(map.name, map.maxPlayers, map.worldPath);

        MatchInfo mi = new MatchInfo();
        mi.serverId = sr.serverId;
        mi.mapName = map.name;
        mi.port = sr.port;
        mi.maxPlayers = map.maxPlayers;
        mi.phase = sr.ready ? MatchPhase.PLAYING : MatchPhase.STARTING;
        mi.playerCount = lobbyCount;

        activeMatches.put(mi.serverId, mi);
        log.info("Match {}: {} on port {} ({} players) - {}", mi.serverId, map.displayName, sr.port,
                lobbyCount, sr.ready ? "ready" : "starting");
    }

    /** Called from server tick — checks async servers and transfers players when ready */
    public static void tick() {
        for (MatchInfo mi : activeMatches.values()) {
            if (mi.phase == MatchPhase.STARTING) {
                if (ServerManager.isServerAlive(mi.serverId)) {
                    int port = ServerManager.getServerPort(mi.serverId);
                    if (port > 0 && ServerManager.waitForServerReady("127.0.0.1", port, 0)) {
                        mi.phase = MatchPhase.PLAYING;
                        mi.port = port;
                        log.info("Match {} now ready on port {}", mi.serverId, port);
                        transferPlayers(mi);
                    }
                }
            }
        }
    }

    /** Send ConnectToServerPacket to all lobby players */
    private static void transferPlayers(MatchInfo mi) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        String host = "127.0.0.1";
        log.info("Transferring {} players to {}:{} for match {}", lobbyPlayers.size(), host, mi.port, mi.mapName);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(
                    Component.literal("§e[PWP] Teleporting to match server on " + host + ":" + mi.port + "..."),
                    false);
            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ConnectToServerPacket(host, mi.port));
        }
    }

    public static void matchEnded(int serverId) {
        MatchInfo mi = activeMatches.remove(serverId);
        if (mi != null) {
            ServerManager.stopServer(serverId);
            log.info("Match ended: {} on port {}", mi.mapName, mi.port);
        }
    }

    public static Map<Integer, MatchInfo> getActiveMatches() {
        return activeMatches;
    }

    public static int getLobbyPlayerCount() {
        return lobbyPlayers.size();
    }

    public enum MatchPhase { STARTING, PLAYING, ENDING }

    public static class MatchInfo {
        public int serverId;
        public String mapName;
        public int port;
        public int playerCount;
        public int maxPlayers;
        public MatchPhase phase = MatchPhase.STARTING;
    }
}

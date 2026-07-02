package com.pwp.lobby.match;

import com.pwp.lobby.ServerManager;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
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

    public static synchronized void startMatch(MapConfig map) {
        int lobbyCount = lobbyPlayers.size();
        if (lobbyCount < minPlayersToStart) {
            log.info("Not enough players: {}/{}", lobbyCount, minPlayersToStart);
            return;
        }

        boolean alreadyRunning = activeMatches.values().stream()
                .anyMatch(m -> m.mapName.equals(map.name) && m.phase == MatchPhase.PLAYING);
        if (alreadyRunning) {
            log.info("Match for {} is already running", map.displayName);
            return;
        }

        ServerManager.StartResult sr = ServerManager.startMatchServer(map.name, map.maxPlayers, map.worldPath);
        if (sr.error != null || !sr.ready) {
            log.error("Failed to start match server: {}", sr.error);
            return;
        }

        MatchInfo mi = new MatchInfo();
        mi.serverId = sr.serverId;
        mi.mapName = map.name;
        mi.port = sr.port;
        mi.maxPlayers = map.maxPlayers;
        mi.phase = MatchPhase.PLAYING;
        mi.playerCount = lobbyCount;

        activeMatches.put(mi.serverId, mi);
        log.info("Match started: {} on port {} ({} players)", map.displayName, sr.port, lobbyCount);
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

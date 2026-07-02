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

    // Очередь игроков, ожидающих матч
    private static final Queue<String> playerQueue = new LinkedList<>();
    // Активные матчи: serverId → MatchInfo
    private static final Map<Integer, MatchInfo> activeMatches = new ConcurrentHashMap<>();
    // Игроки в лобби
    private static final Set<String> lobbyPlayers = ConcurrentHashMap.newKeySet();

    private static int minPlayersToStart = 10;
    private static int fillPercent = 80;

    public static void configure(int minPlayers, int fillPct) {
        minPlayersToStart = minPlayers;
        fillPercent = fillPct;
    }

    // Игрок зашёл в лобби
    public static void playerJoined(String uuid) {
        lobbyPlayers.add(uuid);
        log.debug("Player joined lobby: {} (total: {})", uuid, lobbyPlayers.size());
    }

    // Игрок вышел из лобби
    public static void playerLeft(String uuid) {
        lobbyPlayers.remove(uuid);
        playerQueue.remove(uuid);
        log.debug("Player left lobby: {} (total: {})", uuid, lobbyPlayers.size());
    }

    // Игрок встал в очередь на матч
    public static void enqueuePlayer(String uuid) {
        if (!playerQueue.contains(uuid)) {
            playerQueue.add(uuid);
            log.debug("Player queued: {} (queue: {})", uuid, playerQueue.size());
        }
        tryAllocate();
    }

    // Поиск подходящего сервера для игрока
    public static int findServerForPlayer(String uuid) {
        for (var entry : activeMatches.entrySet()) {
            MatchInfo mi = entry.getValue();
            MapConfig cfg = MapRegistry.get(mi.mapName);
            if (cfg == null) continue;

            int fillPct = mi.playerCount * 100 / cfg.maxPlayers;
            if (fillPct < fillPercent && mi.playerCount < cfg.maxPlayers) {
                return entry.getKey();
            }
        }
        return -1;
    }

    // Попытка запустить новый матч
    private static synchronized void tryAllocate() {
        int queueSize = playerQueue.size();
        if (queueSize < minPlayersToStart) return;

        MapConfig bestMap = MapRegistry.getBestFit(queueSize);
        if (bestMap == null) return;

        int totalPlayers = lobbyPlayers.size();
        if (totalPlayers < bestMap.minPlayers) {
            log.info("Not enough players for {}: {}/{}", bestMap.displayName, totalPlayers, bestMap.minPlayers);
            return;
        }

        // Проверяем, есть ли уже матч на этой карте
        boolean alreadyRunning = activeMatches.values().stream()
                .anyMatch(m -> m.mapName.equals(bestMap.name));
        if (alreadyRunning) {
            log.info("Match for {} is already running, waiting", bestMap.displayName);
            return;
        }

        startMatch(bestMap);
    }

    // Запуск матча
    private static void startMatch(MapConfig map) {
        try {
            int port = ServerManager.startMatchServer(map.name, map.maxPlayers);
            if (port < 0) {
                log.error("Failed to start match server for {}", map.displayName);
                return;
            }

            MatchInfo mi = new MatchInfo();
            mi.serverId = port - 25565; // FIXME: proper serverId
            mi.mapName = map.name;
            mi.port = port;
            mi.maxPlayers = map.maxPlayers;
            mi.phase = MatchPhase.STARTING;

            activeMatches.put(mi.serverId, mi);
            log.info("Match started: {} on port {} (max {})", map.displayName, port, map.maxPlayers);

            // Перемещаем игроков из очереди в матч
            List<String> toMove = new ArrayList<>();
            while (!playerQueue.isEmpty() && toMove.size() < map.maxPlayers) {
                toMove.add(playerQueue.poll());
            }

            mi.playerCount = toMove.size();
            mi.phase = MatchPhase.PLAYING;

            log.info("{} players moved to {}", toMove.size(), map.displayName);

        } catch (Exception e) {
            log.error("Failed to start match: {}", e.getMessage());
        }
    }

    // Матч завершён — освобождаем сервер
    public static void matchEnded(int serverId) {
        activeMatches.remove(serverId);
        ServerManager.stopServer(serverId);
        log.info("Match ended, server {} freed", serverId);

        // Проверяем, можно ли запустить новый матч для ожидающих
        tryAllocate();
    }

    // Проверка заполненности текущих матчей
    public static boolean hasAvailableSlot(String mapName) {
        for (var entry : activeMatches.entrySet()) {
            MatchInfo mi = entry.getValue();
            if (mi.phase != MatchPhase.PLAYING) continue;
            MapConfig cfg = MapRegistry.get(mi.mapName);
            if (cfg == null) continue;
            if (mi.playerCount < cfg.maxPlayers) {
                int fillPct = mi.playerCount * 100 / cfg.maxPlayers;
                if (fillPct < fillPercent) return true;
            }
        }
        return false;
    }

    public static Map<Integer, MatchInfo> getActiveMatches() {
        return activeMatches;
    }

    public static int getQueueSize() {
        return playerQueue.size();
    }

    public static int getLobbyPlayerCount() {
        return lobbyPlayers.size();
    }

    // ====== INNER TYPES ======

    public enum MatchPhase {
        STARTING, PLAYING, ENDING
    }

    public static class MatchInfo {
        public int serverId;
        public String mapName;
        public int port;
        public int playerCount;
        public int maxPlayers;
        public MatchPhase phase;
    }
}

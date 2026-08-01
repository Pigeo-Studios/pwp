package com.pwp.lobby.match;

import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreserver.CoreServerApi;
import com.pwp.lobby.LobbyMod;
import com.pwp.lobby.ServerConfig;
import com.pwp.lobby.ServerManager;
import com.pwp.lobby.VotingManager;
import com.pwp.lobby.maps.MapConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchAllocator {

    private static final Logger log = LoggerFactory.getLogger(MatchAllocator.class);

    private static final Map<Integer, MatchInfo> activeMatches = new ConcurrentHashMap<>();
    private static final Set<String> lobbyPlayers = ConcurrentHashMap.newKeySet();
    private static int minPlayersToStart = 2;

    public static void configure(int minPlayers) {
        minPlayersToStart = minPlayers;
    }

    public static void playerJoined(String uuid) { lobbyPlayers.add(uuid); }
    public static void playerLeft(String uuid) { lobbyPlayers.remove(uuid); }

    public static boolean hasActiveMatch() {
        return activeMatches.values().stream()
                .anyMatch(m -> m.phase == MatchPhase.STARTING || m.phase == MatchPhase.PLAYING || m.phase == MatchPhase.ENDING);
    }

    public static MatchInfo getActiveMatch() {
        return activeMatches.values().stream()
                .filter(m -> m.phase == MatchPhase.STARTING || m.phase == MatchPhase.PLAYING)
                .findFirst().orElse(null);
    }

    public static synchronized void startMatch(MapConfig map) {
        startMatch(map, map.teams.BLUE.faction, map.teams.RED.faction, map.mode,
                map.teams.BLUE.tickets, map.teams.RED.tickets, true, -1);
    }

    public static synchronized void startMatch(MapConfig map, String blueFaction, String redFaction,
                                                String mode, int blueTickets, int redTickets,
                                                boolean invasionDefenderIsRed, int patternIndex) {
        if (lobbyPlayers.size() < minPlayersToStart) {
            LobbyMod.serverBroadcast("§e[PWP] §cНедостаточно игроков для запуска матча (" + lobbyPlayers.size() + "/" + minPlayersToStart + ")");
            log.info("Not enough players: {}/{}", lobbyPlayers.size(), minPlayersToStart);
            return;
        }
        if (activeMatches.size() >= ServerConfig.getMaxMatches()) {
            LobbyMod.serverBroadcast("§e[PWP] §cДостигнут лимит активных матчей (" + ServerConfig.getMaxMatches() + ")");
            log.info("Max matches reached: {}/{}", activeMatches.size(), ServerConfig.getMaxMatches());
            return;
        }

        LobbyMod.serverBroadcast("§e[PWP] §fЗапуск матча на карте §e" + map.displayName + "§f...");
        log.info("Starting match on {} with {} players", map.displayName, lobbyPlayers.size());

        ServerManager.StartResult sr = ServerManager.startMatchServer(map.name, map.maxPlayers, map.worldPath, (matchDir) -> {
            try {
                Path configPath = matchDir.resolve("map_config.json");
                if (!Files.exists(configPath)) {
                    configPath = Path.of(matchDir.toString(), map.name, "map_config.json");
                }
                if (Files.exists(configPath)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String content = Files.readString(configPath);
                    JsonObject root = gson.fromJson(content, JsonObject.class);

                    // Override factions
                    root.getAsJsonObject("teams").getAsJsonObject("BLUE").addProperty("faction", blueFaction);
                    root.getAsJsonObject("teams").getAsJsonObject("RED").addProperty("faction", redFaction);
                    root.getAsJsonObject("teams").getAsJsonObject("BLUE").addProperty("tickets", blueTickets);
                    root.getAsJsonObject("teams").getAsJsonObject("RED").addProperty("tickets", redTickets);

                    // Set mode
                    root.addProperty("mode", mode);
                    root.addProperty("modeDisplayName", mode.equals("invasion") ? "INVASION" : "Advance and Secure");

                    // Invasion defender
                    root.addProperty("invasionDefender", invasionDefenderIsRed ? "RED" : "BLUE");

                    // Capture point pattern index (patterns array is already in the original config)
                    if (patternIndex >= 0) {
                        root.addProperty("capturePointPatternIndex", patternIndex);
                    }

                    Files.writeString(configPath, gson.toJson(root));
                    log.info("Customized map_config.json for match (factions: {}/{}, mode: {})", blueFaction, redFaction, mode);
                }
            } catch (Exception e) {
                log.error("Failed to customize map_config.json: {}", e.getMessage());
            }
        });
        if (sr.error != null) {
            LobbyMod.serverBroadcast("§e[PWP] §cОшибка запуска матча: " + sr.error);
            log.error("Failed to start match: {}", sr.error);
            return;
        }

        MatchInfo mi = new MatchInfo();
        mi.serverId = sr.serverId;
        mi.mapName = map.name;
        mi.displayName = map.displayName;
        mi.modeDisplayName = mode.equals("invasion") ? "INVASION" : map.modeDisplayName;
        mi.port = sr.port;
        mi.maxPlayers = map.maxPlayers;
        mi.playerCount = lobbyPlayers.size();
        mi.blueFaction = blueFaction;
        mi.redFaction = redFaction;
        mi.blueTickets = blueTickets;
        mi.redTickets = redTickets;
        mi.worldPath = map.worldPath;
        mi.mode = mode;
        mi.startedAt = System.currentTimeMillis();
        activeMatches.put(mi.serverId, mi);

        LobbyMod.broadcastLobbyState();
        log.info("Match {}: {} ({}) on port {} ({} players, {} vs {})",
                mi.serverId, map.displayName, mode, sr.port, lobbyPlayers.size(), blueFaction, redFaction);
    }

    public static void joinActiveMatch(ServerPlayer player) {
        MatchInfo mi = getActiveMatch();
        if (mi == null || mi.phase != MatchPhase.PLAYING) {
            player.sendSystemMessage(Component.literal("§cНет активного матча для подключения"), false);
            return;
        }
        connectPlayerToMatch(player, mi, "pigeo.asuscomm.com");
    }

    public static void joinMatchById(int serverId, ServerPlayer player) {
        MatchInfo mi = activeMatches.get(serverId);
        if (mi == null || mi.phase != MatchPhase.PLAYING) {
            player.sendSystemMessage(Component.literal("§cМатч недоступен для подключения"), false);
            return;
        }
        connectPlayerToMatch(player, mi, "pigeo.asuscomm.com");
    }

    private static void connectPlayerToMatch(ServerPlayer player, MatchInfo mi, String host) {
        player.sendSystemMessage(
                Component.literal("§e[PWP] Подключение к матчу на " + host + ":" + mi.port + "..."),
                false);
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ConnectToServerPacket(host, mi.port));
    }

    public static void tick() {
        ServerManager.tick();

        for (MatchInfo mi : activeMatches.values()) {
            if (mi.phase == MatchPhase.ENDING) {
                if (!ServerManager.isAlive(mi.serverId)) {
                    log.info("Match {} stopped gracefully", mi.serverId);
                    matchEnded(mi.serverId);
                } else if (System.currentTimeMillis() - mi.stopRequestedAt > STOP_TIMEOUT_MS) {
                    log.warn("Match {} stop timed out, force killing", mi.serverId);
                    matchEnded(mi.serverId);
                }
                continue;
            }

            if (!ServerManager.isAlive(mi.serverId)) {
                log.warn("Match {} server is dead, cleaning up", mi.serverId);
                LobbyMod.serverBroadcast("§e[PWP] §cМатч " + mi.displayName + " прерван из-за ошибки сервера!");
                matchEnded(mi.serverId);
                continue;
            }

            if (mi.phase == MatchPhase.STARTING && ServerManager.isBooted(mi.serverId)) {
                mi.phase = MatchPhase.PLAYING;
                LobbyMod.serverBroadcast("§e[PWP] §aСервер матча готов! §7Перенос игроков на §e" + mi.mapName + "§7...");
                log.info("Match {} ready on port {}, transferring players", mi.serverId, mi.port);
                LobbyMod.broadcastLobbyState();
                transferPlayers(mi);
            }
        }
    }

    private static void transferPlayers(MatchInfo mi) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        String host = "pigeo.asuscomm.com";
        log.info("Transferring players to {}:{} for match {}", host, mi.port, mi.mapName);

        // Переносим ВСЕХ игроков лобби — голосовать не обязательно
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!lobbyPlayers.contains(player.getStringUUID())) continue;
            player.sendSystemMessage(
                    Component.literal("§e[PWP] Teleporting to match server on " + host + ":" + mi.port + "..."),
                    false);
            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ConnectToServerPacket(host, mi.port));
        }
    }

    private static final long STOP_TIMEOUT_MS = 30_000;

    public static void requestMatchStop(int serverId) {
        MatchInfo mi = activeMatches.get(serverId);
        if (mi == null) return;
        mi.phase = MatchPhase.ENDING;
        mi.stopRequestedAt = System.currentTimeMillis();
        Path dir = ServerManager.getServerDirectory(serverId);
        if (dir != null) {
            try {
                Files.writeString(dir.resolve("stop.signal"), "{\"requestedAt\":" + System.currentTimeMillis() + "}");
                log.info("Stop signal sent to match {} ({})", serverId, mi.mapName);
            } catch (Exception e) {
                log.error("Failed to write stop signal for match {}: {}", serverId, e.getMessage());
            }
        }
        LobbyMod.serverBroadcast("§e[PWP] §fОстановка матча §e" + mi.displayName + "§f...");
        LobbyMod.broadcastLobbyState();
    }

    public static void matchEnded(int serverId) {
        MatchInfo mi = activeMatches.remove(serverId);
        if (mi != null) {
            ServerManager.stopServer(serverId);
            LobbyMod.serverBroadcast("§e[PWP] §fМатч §e" + mi.displayName + " §fзавершён. Возвращайтесь в лобби!");
            log.info("Match ended: {} on port {}", mi.mapName, mi.port);
        }
        LobbyMod.broadcastLobbyState();
        if (!hasActiveMatch() && !lobbyPlayers.isEmpty() && !LobbyMod.isAnyVoteActive()
            && ServerConfig.isAutoStartEnabled()) {
            VotingManager.startVoting();
        }
    }

    public static Map<Integer, MatchInfo> getActiveMatches() { return activeMatches; }
    public static int getLobbyPlayerCount() { return lobbyPlayers.size(); }

    public enum MatchPhase { STARTING, PLAYING, ENDING }

    public static class MatchInfo {
        public int serverId;
        public String mapName;
        public String displayName;
        public String modeDisplayName;
        public String mode = "aas";
        public int port;
        public int playerCount;
        public int maxPlayers;
        public String blueFaction;
        public String redFaction;
        public int blueTickets;
        public int redTickets;
        public String worldPath;
        public long startedAt;
        public long stopRequestedAt;
        public MatchPhase phase = MatchPhase.STARTING;

        public int getElapsedSeconds() {
            if (startedAt == 0) return 0;
            return (int)((System.currentTimeMillis() - startedAt) / 1000);
        }
    }
}

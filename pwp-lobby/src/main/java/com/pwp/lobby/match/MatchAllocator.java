package com.pwp.lobby.match;

import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.lobby.LobbyMod;
import com.pwp.lobby.ServerManager;
import com.pwp.lobby.maps.MapConfig;
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

    public static void configure(int minPlayers) {
        minPlayersToStart = minPlayers;
    }

    public static void playerJoined(String uuid) { lobbyPlayers.add(uuid); }
    public static void playerLeft(String uuid) { lobbyPlayers.remove(uuid); }

    public static boolean hasActiveMatch() {
        return activeMatches.values().stream()
                .anyMatch(m -> m.phase == MatchPhase.STARTING || m.phase == MatchPhase.PLAYING);
    }

    public static MatchInfo getActiveMatch() {
        return activeMatches.values().stream()
                .filter(m -> m.phase == MatchPhase.STARTING || m.phase == MatchPhase.PLAYING)
                .findFirst().orElse(null);
    }

    public static synchronized void startMatch(MapConfig map) {
        if (lobbyPlayers.size() < minPlayersToStart) {
            LobbyMod.serverBroadcast("§e[PWP] §cНедостаточно игроков для запуска матча (" + lobbyPlayers.size() + "/" + minPlayersToStart + ")");
            log.info("Not enough players: {}/{}", lobbyPlayers.size(), minPlayersToStart);
            return;
        }
        if (hasActiveMatch()) {
            LobbyMod.serverBroadcast("§e[PWP] §cМатч уже запущен!");
            log.info("A match is already running");
            return;
        }

        LobbyMod.serverBroadcast("§e[PWP] §fЗапуск матча на карте §e" + map.displayName + "§f...");
        log.info("Starting match on {} with {} players", map.displayName, lobbyPlayers.size());

        ServerManager.StartResult sr = ServerManager.startMatchServer(map.name, map.maxPlayers, map.worldPath);
        if (sr.error != null) {
            LobbyMod.serverBroadcast("§e[PWP] §cОшибка запуска матча: " + sr.error);
            log.error("Failed to start match: {}", sr.error);
            return;
        }

        MatchInfo mi = new MatchInfo();
        mi.serverId = sr.serverId;
        mi.mapName = map.name;
        mi.displayName = map.displayName;
        mi.modeDisplayName = map.modeDisplayName;
        mi.port = sr.port;
        mi.maxPlayers = map.maxPlayers;
        mi.playerCount = lobbyPlayers.size();
        mi.blueFaction = map.BLUE.faction;
        mi.redFaction = map.RED.faction;
        mi.blueTickets = map.BLUE.tickets;
        mi.redTickets = map.RED.tickets;
        mi.worldPath = map.worldPath;
        mi.startedAt = System.currentTimeMillis();
        activeMatches.put(mi.serverId, mi);
        LobbyMod.sendMatchListUpdateToAll();
        log.info("Match {}: {} on port {} ({} players)", mi.serverId, map.displayName, sr.port, lobbyPlayers.size());
    }

    public static void joinActiveMatch(ServerPlayer player) {
        MatchInfo mi = getActiveMatch();
        if (mi == null || mi.phase != MatchPhase.PLAYING) {
            player.sendSystemMessage(Component.literal("§cNo active match available to join"), false);
            return;
        }
        connectPlayerToMatch(player, mi, "127.0.0.1");
    }

    public static void joinMatchById(int serverId, ServerPlayer player) {
        MatchInfo mi = activeMatches.get(serverId);
        if (mi == null || mi.phase != MatchPhase.PLAYING) {
            player.sendSystemMessage(Component.literal("§cMatch not available to join"), false);
            return;
        }
        connectPlayerToMatch(player, mi, "127.0.0.1");
    }

    private static void connectPlayerToMatch(ServerPlayer player, MatchInfo mi, String host) {
        player.sendSystemMessage(
                Component.literal("§e[PWP] Joining match on " + host + ":" + mi.port + "..."),
                false);
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ConnectToServerPacket(host, mi.port));
    }

    public static void tick() {
        ServerManager.tick();

        for (MatchInfo mi : activeMatches.values()) {
            if (!ServerManager.isAlive(mi.serverId)) {
                log.warn("Match {} server is dead, cleaning up", mi.serverId);
                LobbyMod.serverBroadcast("§e[PWP] §cМатч " + mi.displayName + " прерван из-за ошибки сервера!");
                activeMatches.remove(mi.serverId);
                ServerManager.stopServer(mi.serverId);
                continue;
            }

            if (mi.phase == MatchPhase.STARTING && ServerManager.isBooted(mi.serverId)) {
                mi.phase = MatchPhase.PLAYING;
                LobbyMod.serverBroadcast("§e[PWP] §aСервер матча готов! §7Перенос игроков на §e" + mi.mapName + "§7...");
                log.info("Match {} ready on port {}, transferring players", mi.serverId, mi.port);
                LobbyMod.sendMatchListUpdateToAll();
                transferPlayers(mi);
            }
        }
    }

    private static void transferPlayers(MatchInfo mi) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        String host = "127.0.0.1";
        log.info("Transferring players to {}:{} for match {}", host, mi.port, mi.mapName);

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

    public static void matchEnded(int serverId) {
        MatchInfo mi = activeMatches.remove(serverId);
        if (mi != null) {
            ServerManager.stopServer(serverId);
            LobbyMod.serverBroadcast("§e[PWP] §fМатч §e" + mi.displayName + " §fзавершён. Возвращайтесь в лобби!");
            log.info("Match ended: {} on port {}", mi.mapName, mi.port);
        }
        LobbyMod.sendMatchListUpdateToAll();
    }

    public static Map<Integer, MatchInfo> getActiveMatches() { return activeMatches; }
    public static int getLobbyPlayerCount() { return lobbyPlayers.size(); }

    public enum MatchPhase { STARTING, PLAYING, ENDING }

    public static class MatchInfo {
        public int serverId;
        public String mapName;
        public String displayName;
        public String modeDisplayName;
        public int port;
        public int playerCount;
        public int maxPlayers;
        public String blueFaction;
        public String redFaction;
        public int blueTickets;
        public int redTickets;
        public String worldPath;
        public long startedAt;
        public MatchPhase phase = MatchPhase.STARTING;

        public int getElapsedSeconds() {
            if (startedAt == 0) return 0;
            return (int)((System.currentTimeMillis() - startedAt) / 1000);
        }
    }
}

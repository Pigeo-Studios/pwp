package com.pwp.lobby;

import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class FactionVotingManager {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FactionVotingManager.class);

    private static boolean active;
    private static boolean finished;
    private static long startTime;
    private static int durationSec = 120;
    private static String mapName;
    private static List<String> availableFactions = new ArrayList<>();
    private static final Map<UUID, String[]> votes = new HashMap<>();

    public static boolean hasVoted(java.util.UUID uuid) { return votes.containsKey(uuid); }
    public static boolean isActive() { return active; }
    public static int getVoteCount() { return votes.size(); }

    public static void startFactionVoting(String map, List<String> available) {
        if (MatchAllocator.hasActiveMatch() || LobbyMod.isAnyVoteActive()) {
            log.warn("Cannot start faction voting: match or another vote active");
            return;
        }
        active = true;
        finished = false;
        startTime = System.currentTimeMillis();
        mapName = map;
        availableFactions = new ArrayList<>(available);
        Collections.shuffle(availableFactions);
        votes.clear();
        LobbyMod.broadcastLobbyState();
        serverBroadcast("§e[PWP] §fГолосование за фракции началось! Осталось §e" + durationSec + "с");
    }

    public static void vote(UUID uuid, String blueFaction, String redFaction) {
        if (!active || finished) return;
        if (availableFactions.size() < 6) return;
        if (!availableFactions.subList(0, 3).contains(blueFaction)) return;
        if (!availableFactions.subList(3, 6).contains(redFaction)) return;
        votes.put(uuid, new String[]{blueFaction, redFaction});
        LobbyMod.broadcastLobbyState();
    }

    public static void tick() {
        if (!active || finished) return;
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= durationSec * 1000L) finishFactionVote();
    }

    public static int getRemainingSeconds() {
        if (!active) return 0;
        return Math.max(0, durationSec - (int)((System.currentTimeMillis() - startTime) / 1000));
    }

    public static void stop() {
        active = false;
        finished = false;
        votes.clear();
        LobbyMod.broadcastLobbyState();
    }

    private static void finishFactionVote() {
        if (finished) return;
        finished = true;
        active = false;

        if (availableFactions == null || availableFactions.size() < 2) {
            log.warn("Faction vote finished but not enough factions available");
            LobbyMod.broadcastLobbyState();
            return;
        }

        if (votes.isEmpty()) {
            List<String> shuffled = new ArrayList<>(availableFactions);
            Collections.shuffle(shuffled);
            startMatch(shuffled.get(0), shuffled.get(1));
            return;
        }

        Map<String, Integer> pairCounts = new HashMap<>();
        for (String[] pair : votes.values()) {
            String key = pair[0] + "|" + pair[1];
            pairCounts.merge(key, 1, Integer::sum);
        }
        String winner = pairCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(availableFactions.get(0) + "|" + availableFactions.get(1));
        String[] parts = winner.split("\\|");
        startMatch(parts[0], parts[1]);
    }

    private static void startMatch(String blue, String red) {
        MapConfig map = MapRegistry.get(mapName);
        if (map == null) return;
        LobbyMod.recordResult("F1", blue, (int) votes.values().stream().filter(p -> p[0].equals(blue)).count());
        LobbyMod.recordResult("F2", red, (int) votes.values().stream().filter(p -> p[1].equals(red)).count());
        LobbyMod.startMatchAfterFactionVote(map, blue, red);
    }

    // ====== Геттеры для state-пакета ======

    public static String[] getTeam1Factions() {
        if (availableFactions.size() < 3) return new String[0];
        return availableFactions.subList(0, 3).toArray(new String[3]);
    }

    public static String[] getTeam2Factions() {
        if (availableFactions.size() < 6) return new String[0];
        return availableFactions.subList(3, 6).toArray(new String[3]);
    }

    public static int[] getTeam1Votes() {
        int[] t1v = new int[3];
        if (availableFactions.size() < 6) return t1v;
        for (String[] pair : votes.values()) {
            for (int i = 0; i < 3; i++) {
                if (pair[0].equals(availableFactions.get(i))) t1v[i]++;
            }
        }
        return t1v;
    }

    public static int[] getTeam2Votes() {
        int[] t2v = new int[3];
        if (availableFactions.size() < 6) return t2v;
        for (String[] pair : votes.values()) {
            for (int i = 0; i < 3; i++) {
                if (pair[1].equals(availableFactions.get(3 + i))) t2v[i]++;
            }
        }
        return t2v;
    }

    private static void serverBroadcast(String msg) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(msg), false);
    }
}

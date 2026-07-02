package com.pwp.lobby;

import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class VotingManager {

    private static final Logger log = LoggerFactory.getLogger(VotingManager.class);

    private static final Map<UUID, String> votes = new HashMap<>();
    private static boolean active = false;
    private static int timer = 0;
    private static int maxTimer = 30;
    private static boolean voteLocked = false;

    public static void startVoting() {
        List<MapConfig> maps = MapRegistry.getVotable();
        if (maps.size() < 2) {
            log.warn("Not enough maps to start voting (need at least 2)");
            return;
        }
        active = true;
        voteLocked = false;
        votes.clear();
        timer = maxTimer;
        log.info("Voting started: {} maps available", maps.size());
    }

    public static void vote(UUID playerUuid, String mapName) {
        if (!active || voteLocked) return;
        MapConfig cfg = MapRegistry.get(mapName);
        if (cfg == null) return;
        votes.put(playerUuid, mapName);
    }

    public static void tick() {
        if (!active) return;
        if (timer > 0) {
            timer--;
            if (timer <= 0) {
                finishVoting();
            }
        }
    }

    private static void finishVoting() {
        if (voteLocked) return;
        voteLocked = true;
        active = false;

        String winner = getResult();
        log.info("Vote finished. Winner: {}", winner);
        MatchAllocator.enqueuePlayer("all"); // signal to check queue
    }

    public static String getResult() {
        Map<String, Integer> counts = new HashMap<>();
        for (String vote : votes.values()) {
            counts.merge(vote, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseGet(() -> {
                    List<MapConfig> maps = MapRegistry.getVotable();
                    return maps.isEmpty() ? "fools_road" : maps.get(0).name;
                });
    }

    public static void stopVoting() {
        active = false;
        voteLocked = false;
        votes.clear();
    }

    public static List<MapConfig> getVotableMaps() {
        return MapRegistry.getVotable();
    }

    public static Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String vote : votes.values()) {
            counts.merge(vote, 1, Integer::sum);
        }
        return counts;
    }

    public static int getTimer() { return timer; }
    public static int getMaxTimer() { return maxTimer; }
    public static boolean isActive() { return active; }
    public static boolean isLocked() { return voteLocked; }
}

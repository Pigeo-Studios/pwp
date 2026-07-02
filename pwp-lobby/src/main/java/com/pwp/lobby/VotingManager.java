package com.pwp.lobby;

import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VotingManager {

    private static final Logger log = LoggerFactory.getLogger(VotingManager.class);

    private static final Map<UUID, String> votes = new HashMap<>();
    private static boolean active = false;
    private static int timer = 0;
    private static int maxTimer = 30;
    private static boolean finished = false;
    private static String winner = null;

    public static void startVoting() {
        List<MapConfig> maps = MapRegistry.getVotable();
        if (maps.size() < 1) {
            log.warn("No maps available for voting");
            return;
        }
        active = true;
        finished = false;
        winner = null;
        votes.clear();
        timer = maxTimer;
        log.info("Voting started: {} maps available", maps.size());
    }

    public static void vote(UUID playerUuid, String mapName) {
        if (!active || finished) return;
        MapConfig cfg = MapRegistry.get(mapName);
        if (cfg == null) return;
        votes.put(playerUuid, mapName);
    }

    public static void tick() {
        if (!active || finished) return;
        if (timer > 0) {
            timer--;
            if (timer <= 0) {
                finishVoting();
            }
        }
    }

    private static void finishVoting() {
        if (finished) return;
        finished = true;
        active = false;

        Map<String, Integer> counts = getVoteCounts();
        winner = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseGet(() -> {
                    List<MapConfig> maps = MapRegistry.getVotable();
                    return maps.isEmpty() ? null : maps.get(0).name;
                });

        if (winner != null) {
            log.info("Vote finished. Winner: {}", winner);
            LobbyMod.onVoteFinished(winner);
        } else {
            log.warn("Vote finished but no winner!");
        }
    }

    public static String getWinner() {
        return winner;
    }

    public static Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String vote : votes.values()) {
            counts.merge(vote, 1, Integer::sum);
        }
        return counts;
    }

    public static void stopVoting() {
        active = false;
        finished = false;
        votes.clear();
        timer = 0;
        winner = null;
    }

    public static boolean isActive() { return active; }
    public static boolean isFinished() { return finished; }
    public static int getTimer() { return timer; }
    public static int getMaxTimer() { return maxTimer; }
    public static int getVoteCount() { return votes.size(); }
}

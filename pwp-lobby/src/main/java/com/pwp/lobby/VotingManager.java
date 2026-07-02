package com.pwp.lobby;

import java.util.*;

public class VotingManager {

    public static class MapEntry {
        public final String name;
        public final String displayName;

        public MapEntry(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
    }

    private static final List<MapEntry> availableMaps = List.of(
            new MapEntry("fools_road", "Fool's Road"),
            new MapEntry("chora_valley", "Chora Valley"),
            new MapEntry("tallil_outskirts", "Tallil Outskirts"),
            new MapEntry("mestia", "Mestia"),
            new MapEntry("belaya", "Belaya Pass")
    );

    private static final Map<UUID, String> votes = new HashMap<>();
    private static boolean active = false;

    public static void startVoting() {
        active = true;
        votes.clear();
    }

    public static void vote(UUID playerUuid, String mapName) {
        if (!active) return;
        votes.put(playerUuid, mapName);
    }

    public static String getResult() {
        Map<String, Integer> counts = new HashMap<>();
        for (String vote : votes.values()) {
            counts.merge(vote, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(availableMaps.get(0).name);
    }

    public static void stopVoting() {
        active = false;
    }

    public static List<MapEntry> getAvailableMaps() {
        return availableMaps;
    }

    public static Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String vote : votes.values()) {
            counts.merge(vote, 1, Integer::sum);
        }
        return counts;
    }
}

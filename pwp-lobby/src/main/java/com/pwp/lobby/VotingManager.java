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
    private static long voteStartTime = 0;
    private static int voteDurationSec = 300;
    private static boolean accelerated = false;
    private static boolean finished = false;
    private static String winner = null;

    private static long lastTimerBroadcast = 0;
    private static long lastLeaderBroadcast = 0;
    private static int lastBroadcastedRemaining = -1;

    public static void startVoting() {
        if (MatchAllocator.hasActiveMatch()) {
            log.warn("Cannot start voting while a match is active");
            return;
        }
        List<MapConfig> maps = MapRegistry.getVotable();
        if (maps.size() < 1) {
            log.warn("No maps available for voting");
            return;
        }
        active = true;
        finished = false;
        accelerated = false;
        winner = null;
        votes.clear();
        voteStartTime = System.currentTimeMillis();
        lastTimerBroadcast = 0;
        lastLeaderBroadcast = 0;
        lastBroadcastedRemaining = -1;

        String mapList = maps.stream().map(m -> m.displayName).collect(Collectors.joining("§7, §e"));
        LobbyMod.serverBroadcast("§e[PWP] §fГолосование началось! §7Карты: §e" + mapList);
        LobbyMod.serverBroadcast("§7Напишите §e/votemap <название> §7или откройте GUI чтобы проголосовать");
        LobbyMod.broadcastVotingUpdate();
        log.info("Voting started: {} maps available, {} seconds", maps.size(), voteDurationSec);
    }

    public static boolean vote(UUID playerUuid, String mapName) {
        if (!active || finished) return false;
        MapConfig cfg = MapRegistry.get(mapName);
        if (cfg == null) return false;
        String current = votes.get(playerUuid);
        if (mapName.equals(current)) return false;
        votes.put(playerUuid, mapName);

        int total = votes.size();
        int online = MatchAllocator.getLobbyPlayerCount();
        if (total % 5 == 0 || total == online) {
            broadcastLeader();
        }
        LobbyMod.broadcastVotingUpdate();
        return true;
    }

    public static void tick() {
        if (!active || finished) return;
        long now = System.currentTimeMillis();
        long elapsed = now - voteStartTime;
        int remaining = voteDurationSec - (int)(elapsed / 1000);

        if (!accelerated && remaining > 60) {
            int online = MatchAllocator.getLobbyPlayerCount();
            if (online > 0 && votes.size() * 10 >= online * 9) {
                accelerated = true;
                voteStartTime = now - (voteDurationSec - 60) * 1000L;
                remaining = 60;
                LobbyMod.serverBroadcast("§e[PWP] §fПочти все проголосовали! §eОсталось " + remaining + "с");
                LobbyMod.broadcastVotingUpdate();
                log.info("90% threshold reached, vote accelerated to 60s remaining");
            }
        }

        if (remaining != lastBroadcastedRemaining && now - lastTimerBroadcast > 1000) {
            if (remaining <= 5 || remaining == 10 || remaining == 15 || remaining == 30 || remaining == 60 || (remaining <= 120 && remaining % 60 == 0)) {
                if (remaining > 0) {
                    LobbyMod.serverBroadcast("§e[PWP] §fГолосование закончится через §e" + remaining + "с");
                }
                lastTimerBroadcast = now;
                lastBroadcastedRemaining = remaining;
            }
        }

        if (now - lastLeaderBroadcast > 20000 && votes.size() >= 2) {
            broadcastLeader();
            lastLeaderBroadcast = now;
        }

        if (elapsed >= voteDurationSec * 1000L) {
            finishVoting();
        }
    }

    private static void broadcastLeader() {
        if (!active || votes.isEmpty()) return;
        Map<String, Integer> counts = getVoteCounts();
        String leader = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (leader == null) return;
        int total = votes.size();
        int online = MatchAllocator.getLobbyPlayerCount();
        String stats = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(e -> "§e" + e.getKey() + "§7(" + e.getValue() + ")")
                .collect(Collectors.joining(" §8| "));
        LobbyMod.serverBroadcast("§e[PWP] §fГолоса: " + stats + " §8| §7Проголосовало §e" + total + "§7/" + online);
    }

    private static void finishVoting() {
        if (finished) return;
        finished = true;
        active = false;

        Map<String, Integer> counts = getVoteCounts();
        int total = votes.size();

        if (!counts.isEmpty()) {
            String results = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(e -> "§e" + e.getKey() + " §7(" + e.getValue() + "гол."
                        + (total > 0 ? " " + (e.getValue() * 100 / total) + "%" : "") + ")")
                    .collect(Collectors.joining(" §8| "));
            LobbyMod.serverBroadcast("§e[PWP] §fРезультаты голосования: " + results);
        }

        winner = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseGet(() -> {
                    List<MapConfig> maps = MapRegistry.getVotable();
                    return maps.isEmpty() ? null : maps.get(0).name;
                });

        if (winner != null) {
            MapConfig cfg = MapRegistry.get(winner);
            String display = cfg != null ? cfg.displayName : winner;
            LobbyMod.serverBroadcast("§e[PWP] §aПобедила карта: §e" + display + " §a— матч запускается!");
            log.info("Vote finished. Winner: {}", winner);
            LobbyMod.onVoteFinished(winner);
        } else {
            LobbyMod.serverBroadcast("§e[PWP] §cНе удалось определить победителя голосования!");
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
        accelerated = false;
        votes.clear();
        voteStartTime = 0;
        winner = null;
        lastBroadcastedRemaining = -1;
    }

    public static boolean hasVoted(java.util.UUID uuid) { return votes.containsKey(uuid); }
    public static boolean isActive() { return active; }
    public static boolean isFinished() { return finished; }
    public static int getRemainingSeconds() {
        if (!active) return 0;
        long elapsed = System.currentTimeMillis() - voteStartTime;
        int remaining = voteDurationSec - (int)(elapsed / 1000);
        return Math.max(0, remaining);
    }
    public static int getVoteDuration() { return voteDurationSec; }
    public static int getVoteCount() { return votes.size(); }
    public static int getTotalOnline() { return MatchAllocator.getLobbyPlayerCount(); }

    public static int getVoteCountForMap(String mapName) {
        return (int) votes.values().stream().filter(v -> v.equals(mapName)).count();
    }

    public static String getLeadingMap() {
        if (votes.isEmpty()) return null;
        Map<String, Integer> counts = getVoteCounts();
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
    }

    public static List<String> getVotableMapNames() {
        return MapRegistry.getVotable().stream().map(m -> m.name).collect(java.util.stream.Collectors.toList());
    }

    public static List<MapConfig> getVotableMaps() {
        return MapRegistry.getVotable();
    }
}

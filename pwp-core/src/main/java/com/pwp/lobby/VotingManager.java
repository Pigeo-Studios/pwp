package com.pwp.lobby;

import com.pwp.api.time.GameClock;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Начать голосование за карту. null — началось, иначе ключ причины отказа (раньше отказ был только в логе,
     * а команды/панель рапортовали успех — «молчащий» старт).
     */
    public static String startVoting() {
        if (MatchAllocator.hasActiveMatch()) return "pwp_core.admin.lobby.error.match_active";
        if (LobbyMod.isAnyVoteActive()) return "pwp_core.admin.lobby.error.vote_active";
        if (MatchAllocator.isCycleBusy()) return "pwp_core.lobby.prep.error.busy";
        if (!com.pwp.api.core.MatchPolicy.canStartNewMatch()) {
            String reason = com.pwp.api.core.MatchPolicy.reason();
            LobbyMod.broadcastPwp(
                    ChatFormatting.RED,
                    "pwp_lobby.msg.matches_paused",
                    reason != null && !reason.isEmpty() ? ": " + reason : "");
            log.info("Voting start blocked by match policy");
            return "pwp_core.admin.lobby.error.policy";
        }
        List<MapConfig> maps = MapRegistry.getVotable();
        if (maps.size() < 1) {
            log.warn("No maps available for voting");
            return "pwp_core.admin.lobby.error.no_maps";
        }
        if (MatchAllocator.getLobbyPlayerCount() < MatchAllocator.getMinPlayers()) {
            return "pwp_core.lobby.prep.error.not_enough_players";
        }
        active = true;
        finished = false;
        accelerated = false;
        winner = null;
        votes.clear();
        voteStartTime = GameClock.realMillis();
        lastTimerBroadcast = 0;
        lastLeaderBroadcast = 0;
        lastBroadcastedRemaining = -1;

        if (maps.size() == 1) {
            // одна карта — голосовать не за что: сразу «победитель» и следующий шаг лобби
            log.info("Voting skipped: single map {}", maps.get(0).name);
            finishVoting();
            return null;
        }
        String mapList = maps.stream().map(m -> m.displayName).collect(Collectors.joining(", "));
        LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.map_vote_started", mapList);
        LobbyMod.serverBroadcast(
                Component.translatable("pwp_lobby.msg.map_vote_hint").withStyle(ChatFormatting.GRAY));
        LobbyMod.broadcastLobbyState();
        log.info("Voting started: {} maps available, {} seconds", maps.size(), voteDurationSec);
        return null;
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
        LobbyMod.broadcastLobbyState();
        return true;
    }

    public static void tick() {
        if (!active || finished) return;
        long now = GameClock.realMillis();
        long elapsed = now - voteStartTime;
        int remaining = voteDurationSec - (int) (elapsed / 1000);

        if (!accelerated && remaining > 60) {
            int online = MatchAllocator.getLobbyPlayerCount();
            if (online > 0 && votes.size() * 10 >= online * 9) {
                accelerated = true;
                voteStartTime = now - (voteDurationSec - 60) * 1000L;
                remaining = 60;
                LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.almost_all_voted", remaining);
                LobbyMod.broadcastLobbyState();
                log.info("90% threshold reached, vote accelerated to 60s remaining");
            }
        }

        if (remaining != lastBroadcastedRemaining && now - lastTimerBroadcast > 1000) {
            if (remaining <= 5
                    || remaining == 10
                    || remaining == 15
                    || remaining == 30
                    || remaining == 60
                    || (remaining <= 120 && remaining % 60 == 0)) {
                if (remaining > 0) {
                    LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.map_vote_ends_in", remaining);
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
                .map(Map.Entry::getKey)
                .orElse(null);
        if (leader == null) return;
        int total = votes.size();
        int online = MatchAllocator.getLobbyPlayerCount();
        Component stats = LobbyMod.voteList(
                counts.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(3)
                        .toList(),
                0);
        LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.vote_stats", stats, total, online);
    }

    private static void finishVoting() {
        if (finished) return;
        finished = true;
        active = false;

        Map<String, Integer> counts = getVoteCounts();
        int total = votes.size();

        if (!counts.isEmpty()) {
            Component results = LobbyMod.voteList(
                    counts.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue()
                                    .reversed())
                            .toList(),
                    total);
            LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.map_vote_results", results);
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
            LobbyMod.broadcastPwp(ChatFormatting.GREEN, "pwp_lobby.msg.map_winner", display);
            log.info("Vote finished. Winner: {}", winner);
            LobbyMod.recordResult("MAP", winner, counts.getOrDefault(winner, 0));
            LobbyMod.broadcastLobbyState();
            LobbyMod.onVoteFinished(winner);
        } else {
            LobbyMod.broadcastPwp(ChatFormatting.RED, "pwp_lobby.msg.no_winner");
            log.warn("Vote finished but no winner!");
            LobbyMod.broadcastLobbyState();
        }
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
        LobbyMod.broadcastLobbyState();
    }

    public static boolean hasVoted(java.util.UUID uuid) {
        return votes.containsKey(uuid);
    }
    /** Голос игрока (имя карты) или null, если не голосовал. */
    public static String getVote(java.util.UUID uuid) {
        return votes.get(uuid);
    }

    public static boolean isActive() {
        return active;
    }

    public static int getRemainingSeconds() {
        if (!active) return 0;
        long elapsed = GameClock.realMillis() - voteStartTime;
        int remaining = voteDurationSec - (int) (elapsed / 1000);
        return Math.max(0, remaining);
    }

    public static int getVoteCount() {
        return votes.size();
    }

    public static int getVoteCountForMap(String mapName) {
        return (int) votes.values().stream().filter(v -> v.equals(mapName)).count();
    }

    public static String getLeadingMap() {
        if (votes.isEmpty()) return null;
        Map<String, Integer> counts = getVoteCounts();
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}

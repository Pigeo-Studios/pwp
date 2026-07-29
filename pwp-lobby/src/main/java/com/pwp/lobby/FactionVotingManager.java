package com.pwp.lobby;

import com.pwp.coreclient.network.OpenFactionVotePacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class FactionVotingManager {

    private static boolean active;
    private static boolean finished;
    private static long startTime;
    private static int durationSec = 120;
    private static String mapName;
    private static List<String> availableFactions;
    private static final Map<UUID, String[]> votes = new HashMap<>();

    public static boolean hasVoted(java.util.UUID uuid) { return votes.containsKey(uuid); }
    public static boolean isActive() { return active; }

    public static void startFactionVoting(String map, List<String> available) {
        active = true;
        finished = false;
        startTime = System.currentTimeMillis();
        mapName = map;
        availableFactions = new ArrayList<>(available);
        Collections.shuffle(availableFactions);
        votes.clear();
        broadcastUpdate();
        serverBroadcast("§e[PWP] §fГолосование за фракции началось! Осталось §e" + durationSec + "с");
    }

    public static void vote(UUID uuid, String blueFaction, String redFaction) {
        if (!active || finished) return;
        if (!availableFactions.subList(0, 3).contains(blueFaction)) return;
        if (!availableFactions.subList(3, 6).contains(redFaction)) return;
        votes.put(uuid, new String[]{blueFaction, redFaction});
        broadcastUpdate();
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
    }

    private static void finishFactionVote() {
        if (finished) return;
        finished = true;
        active = false;

        if (votes.isEmpty() || availableFactions.size() < 2) {
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
        LobbyMod.startMatchAfterFactionVote(map, blue, red);
    }

    public static OpenFactionVotePacket buildPacket() {
        int[] t1v = new int[3], t2v = new int[3];
        for (String[] pair : votes.values()) {
            for (int i = 0; i < 3; i++) {
                if (pair[0].equals(availableFactions.get(i))) t1v[i]++;
                if (pair[1].equals(availableFactions.get(3 + i))) t2v[i]++;
            }
        }
        return new OpenFactionVotePacket(getRemainingSeconds(),
                MatchAllocator.getLobbyPlayerCount(), votes.size(),
                availableFactions.subList(0, 3).toArray(new String[3]),
                availableFactions.subList(3, 6).toArray(new String[3]),
                t1v, t2v);
    }

    public static void broadcastUpdate() {
        OpenFactionVotePacket pkt = buildPacket();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(p ->
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt));
    }

    private static void serverBroadcast(String msg) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(msg), false);
    }
}

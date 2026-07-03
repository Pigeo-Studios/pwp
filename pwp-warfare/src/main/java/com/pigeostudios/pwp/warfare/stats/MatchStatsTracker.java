package com.pigeostudios.pwp.warfare.stats;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.PacketHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MatchStatsTracker {

    private static final Logger log = LoggerFactory.getLogger(MatchStatsTracker.class);
    private static final Gson GSON = new Gson();

    private static MatchStatsTracker instance;

    private final Map<String, PlayerMatchStats> players = new HashMap<>();
    private final Map<Integer, String> killFeed = new HashMap<>();

    private String mapName;
    private String mode;
    private long startedAt;
    private boolean active;

    private MatchStatsTracker() {}

    public static MatchStatsTracker get() {
        if (instance == null) instance = new MatchStatsTracker();
        return instance;
    }

    public static void reset() {
        instance = new MatchStatsTracker();
    }

    public void startMatch(String mapName, String mode) {
        this.mapName = mapName;
        this.mode = mode;
        this.startedAt = System.currentTimeMillis();
        this.active = true;
        this.players.clear();
        this.killFeed.clear();
        log.info("Match stats tracking started: {} ({})", mapName, mode);
    }

    public void endMatch() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public PlayerMatchStats getOrCreate(ServerPlayer player) {
        String uuid = player.getStringUUID();
        String nickname = player.getScoreboardName();
        String team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NONE";

        return players.computeIfAbsent(uuid, k -> {
            PlayerMatchStats s = new PlayerMatchStats(uuid, nickname, team);
            s.role = player.getPersistentData().getString("WARFARE_CurrentKit");
            s.squadId = player.getPersistentData().getInt("WARFARE_SquadID");
            s.wasSquadLeader = player.getPersistentData().getBoolean("WARFARE_IsSquadLeader");
            return s;
        });
    }

    public void recordKill(ServerPlayer killer, ServerPlayer victim, String weapon, double distance) {
        if (!active) return;
        PlayerMatchStats k = getOrCreate(killer);
        PlayerMatchStats v = getOrCreate(victim);

        k.recordKill();
        v.recordDeath();

        if (distance > k.longestKill) k.longestKill = distance;

        log.debug("KILL: {} → {} ({}, {:.1f}m)", killer.getScoreboardName(), victim.getScoreboardName(), weapon, distance);
    }

    public void recordTeamKill(ServerPlayer killer, ServerPlayer victim) {
        if (!active) return;
        PlayerMatchStats k = getOrCreate(killer);
        PlayerMatchStats v = getOrCreate(victim);

        k.kills--;
        k.score -= 50;
        v.deaths++;
        v.score -= 25;

        log.warn("TEAMKILL: {} → {}", killer.getScoreboardName(), victim.getScoreboardName());
    }

    public void recordAssist(ServerPlayer assistant, ServerPlayer victim) {
        if (!active) return;
        getOrCreate(assistant).recordAssist();
    }

    public void recordCapture(ServerPlayer player) {
        if (!active) return;
        getOrCreate(player).recordCapture();
    }

    public void recordRevive(ServerPlayer medic) {
        if (!active) return;
        getOrCreate(medic).recordRevive();
    }

    public void recordVehicleKill(ServerPlayer killer) {
        if (!active) return;
        getOrCreate(killer).recordVehicleKill();
    }

    public void recordDamage(ServerPlayer dealer, double damage) {
        if (!active) return;
        getOrCreate(dealer).recordDamage(damage);
    }

    // ====== ФИНАЛИЗАЦИЯ МАТЧА ======

    public void finalizeMatch(String winner, int blueScore, int redScore) {
        if (!active) return;
        active = false;

        long endedAt = System.currentTimeMillis();
        int durationSec = (int) ((endedAt - startedAt) / 1000);

        JsonObject match = new JsonObject();
        match.addProperty("mapName", mapName != null ? mapName : "unknown");
        match.addProperty("mode", mode != null ? mode : "AAS");
        match.addProperty("teamBlueScore", blueScore);
        match.addProperty("teamRedScore", redScore);
        match.addProperty("winner", winner);
        match.addProperty("durationSeconds", durationSec);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        match.addProperty("startedAt", java.time.LocalDateTime.now().minusSeconds(durationSec).format(fmt));
        match.addProperty("endedAt", java.time.LocalDateTime.now().format(fmt));

        JsonArray playersArr = new JsonArray();
        for (PlayerMatchStats ps : this.players.values()) {
            JsonObject p = new JsonObject();
            p.addProperty("uuid", ps.uuid);
            p.addProperty("team", ps.team);
            p.addProperty("kills", ps.kills);
            p.addProperty("deaths", ps.deaths);
            p.addProperty("assists", ps.assists);
            p.addProperty("score", ps.score);
            p.addProperty("vehicleKills", ps.vehicleKills);
            p.addProperty("captures", ps.captures);
            p.addProperty("revives", ps.revives);
            p.addProperty("shotsFired", ps.shotsFired);
            p.addProperty("shotsHit", ps.shotsHit);
            p.addProperty("damageDealt", ps.damageDealt);
            p.addProperty("healingDone", ps.healingDone);
            p.addProperty("suppliesDelivered", ps.suppliesDelivered);
            p.addProperty("longestKill", ps.longestKill);
            p.addProperty("role", ps.role != null ? ps.role : "");
            p.addProperty("squadId", ps.squadId);
            p.addProperty("wasSquadLeader", ps.wasSquadLeader);
            playersArr.add(p);
        }
        match.add("players", playersArr);

        if (ModList.get().isLoaded("pwp_core_client")) {
            log.info("Saving match result to Core API...");
            JsonObject response = CoreAPI.saveMatch(match);
            if (response != null) {
                log.info("Match saved successfully");
            } else {
                log.warn("Could not save match result (Core API may be down)");
            }

            for (PlayerMatchStats ps : this.players.values()) {
                long xp = 0;
                long coins = 0;

                // Try to calculate rewards via API, fallback to hardcoded values
                JsonObject rewardResult = CoreAPI.calculateRewards(
                    ps.uuid, ps.team, winner,
                    ps.kills, ps.assists, ps.vehicleKills,
                    ps.captures, ps.revives, 0,
                    durationSec / 60
                );

                if (rewardResult != null && rewardResult.has("data")) {
                    JsonObject data = rewardResult.getAsJsonObject("data");
                    xp = data.get("xp").getAsLong();
                    coins = data.get("coins").getAsLong();
                } else {
                    xp += ps.kills * 50L;
                    xp += ps.assists * 25L;
                    xp += ps.vehicleKills * 150L;
                    xp += ps.captures * 100L;
                    xp += ps.revives * 75L;
                    xp += durationSec / 60 * 10L;

                    coins += ps.kills * 10L;
                    coins += ps.assists * 5L;
                    coins += ps.vehicleKills * 30L;
                    coins += ps.captures * 25L;
                    coins += ps.revives * 15L;

                    if (ps.team.equals(winner)) {
                        xp += 200; coins += 50;
                    } else {
                        xp += 100; coins += 20;
                    }
                }

                CoreAPI.addXp(ps.uuid, xp, "MATCH");
                CoreAPI.addCurrency(ps.uuid, coins, "MATCH_REWARD");

                CoreAPI.checkRank(ps.uuid);

                log.info("{} earned {} XP and {} Coins", ps.nickname, xp, coins);
            }
        } else {
            log.info("pwp_core_client not installed, skipping match save");
        }

        // Transfer all players back to lobby then shut down
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(
                        Component.literal("§e[PWP] Returning to lobby..."), false);
                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new ConnectToServerPacket("127.0.0.1", 25565));
            }
            server.execute(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                server.halt(false);
            });
        }

        reset();
    }

    public Map<String, PlayerMatchStats> getAllPlayers() {
        return players;
    }
}

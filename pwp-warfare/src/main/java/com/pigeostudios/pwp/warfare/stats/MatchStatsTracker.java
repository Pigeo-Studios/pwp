package com.pigeostudios.pwp.warfare.stats;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pwp.api.core.CoreApi;
import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreserver.TransferHosts;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        AssistTracker.clear();
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
            s.role = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
            s.squadId = WarfareData.of(player).getInt(WarfareKeys.SQUAD_ID);
            s.wasSquadLeader = WarfareData.of(player).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
            return s;
        });
    }

    public void recordKill(ServerPlayer killer, ServerPlayer victim, String weapon, double distance) {
        if (!active) return;
        PlayerMatchStats k = getOrCreate(killer);
        PlayerMatchStats v = getOrCreate(victim);

        k.recordKill();
        v.recordDeath();
        for (java.util.UUID helper : AssistTracker.takeAssists(victim, killer.getUUID())) {
            PlayerMatchStats h = players.get(helper.toString());
            if (h != null) h.assists++;
        }

        if (distance > k.longestKill) k.longestKill = distance;

        log.debug(
                "KILL: {} → {} ({}, {:.1f}m)",
                killer.getScoreboardName(),
                victim.getScoreboardName(),
                weapon,
                distance);
    }

    public void recordTeamKill(ServerPlayer killer, ServerPlayer victim) {
        if (!active) return;
        PlayerMatchStats k = getOrCreate(killer);
        PlayerMatchStats v = getOrCreate(victim);

        k.kills--;
        k.score -= 50;
        v.deaths++;
        v.score -= 25;
        k.recordTeamKillStat();

        log.warn("TEAMKILL: {} → {}", killer.getScoreboardName(), victim.getScoreboardName());
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

    public void recordHealing(ServerPlayer healer, double amount) {
        if (!active) return;
        getOrCreate(healer).recordHealing(amount);
    }

    public void recordVehicleDestroyed(ServerPlayer destroyer, Entity vehicle) {
        if (!active) return;
        String vType = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TYPE);
        boolean isAir = vType.equalsIgnoreCase("HELICOPTER")
                || vType.toUpperCase().contains("CAS")
                || vType.equalsIgnoreCase("Supply Helicopter");
        getOrCreate(destroyer).recordVehicleDestroyed(isAir);
        log.info("{} destroyed {} ({})", destroyer.getScoreboardName(), vType, isAir ? "AIR" : "GROUND");
    }

    public void recordSuppliesDelivered(ServerPlayer deliverer, int amount) {
        if (!active) return;
        getOrCreate(deliverer).suppliesDelivered += amount;
    }

    // Survival time tracking
    private final Map<UUID, Long> spawnTimes = new HashMap<>();

    public void recordSpawn(ServerPlayer player) {
        if (!active) return;
        spawnTimes.put(player.getUUID(), System.currentTimeMillis());
    }

    public void recordDeath(ServerPlayer player) {
        if (!active) return;
        Long spawnTime = spawnTimes.remove(player.getUUID());
        if (spawnTime != null) {
            long survived = System.currentTimeMillis() - spawnTime;
            getOrCreate(player).survivalTime += survived;
        }
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
        match.addProperty(
                "startedAt",
                java.time.LocalDateTime.now().minusSeconds(durationSec).format(fmt));
        match.addProperty("endedAt", java.time.LocalDateTime.now().format(fmt));

        JsonArray playersArr = new JsonArray();
        for (PlayerMatchStats ps : this.players.values()) {
            JsonObject p = new JsonObject();
            p.addProperty("uuid", ps.uuid);
            p.addProperty("team", ps.team);
            p.addProperty("kills", ps.kills);
            p.addProperty("deaths", ps.deaths);
            p.addProperty("score", ps.score);
            p.addProperty("vehicleKills", ps.vehicleKills);
            p.addProperty("captures", ps.captures);
            p.addProperty("revives", ps.revives);
            p.addProperty("damageDealt", ps.damageDealt);
            p.addProperty("healingDone", ps.healingDone);
            p.addProperty("suppliesDelivered", ps.suppliesDelivered);
            p.addProperty("longestKill", ps.longestKill);
            p.addProperty("role", ps.role != null ? ps.role : "");
            p.addProperty("squadId", ps.squadId);
            p.addProperty("wasSquadLeader", ps.wasSquadLeader);
            p.addProperty("vehiclesDestroyed", ps.vehiclesDestroyed);
            p.addProperty("airVehiclesDestroyed", ps.airVehiclesDestroyed);
            p.addProperty("teamKills", ps.teamKills);
            p.addProperty("survivalTime", ps.survivalTime);
            playersArr.add(p);
        }
        match.add("players", playersArr);

        // Сохранение результатов — в фоне: десятки HTTP-запросов не должны блокировать тик.
        // Экземпляр трекера после reset() больше не меняется, поэтому читать players из потока безопасно.
        final String winnerTeam = winner;
        Thread saver = new Thread(() -> persistResults(match, winnerTeam, durationSec), "PWPMatchStatsSave");
        saver.setDaemon(true);
        saver.start();

        scheduleServerShutdown(180);
        reset();
    }

    /** Отправка матча, наград и статистики игроков в core-service. Только фоновый поток. */
    private void persistResults(JsonObject match, String winner, int durationSec) {
        if (ModList.get().isLoaded("pwpcore")) {
            log.info("Saving match result to Core API...");
            try {
                JsonObject response = CoreApi.await(CoreApi.saveMatch(match));
                if (response != null) {
                    log.info("Match saved successfully");
                } else {
                    log.warn("Could not save match result (Core API may be down)");
                }

                for (PlayerMatchStats ps : this.players.values()) {
                    long xp = 0;
                    long coins = 0;

                    boolean isWin = ps.team.equals(winner);

                    try {
                        JsonObject rewardResult = CoreApi.await(CoreApi.calculateRewards(
                                ps.uuid,
                                ps.team,
                                winner,
                                ps.kills,
                                ps.assists,
                                ps.vehicleKills,
                                ps.captures,
                                ps.revives,
                                0,
                                durationSec / 60));

                        if (rewardResult != null && rewardResult.has("data")) {
                            JsonObject data = rewardResult.getAsJsonObject("data");
                            xp = data.get("xp").getAsLong();
                            coins = data.get("coins").getAsLong();
                        } else {
                            throw new Exception("API returned null, using fallback");
                        }
                    } catch (Exception e) {
                        log.warn(
                                "Награды для {} рассчитаны локально (core-service не ответил): {}",
                                ps.nickname,
                                e.getMessage());
                        xp = ps.kills * 50L
                                + ps.vehicleKills * 150L
                                + ps.captures * 100L
                                + ps.revives * 75L
                                + ps.assists * 25L
                                + durationSec / 60 * 10L;
                        coins = ps.kills * 10L + ps.vehicleKills * 30L + ps.captures * 25L + ps.revives * 15L;
                        if (isWin) {
                            xp += 200;
                            coins += 50;
                        } else {
                            xp += 100;
                            coins += 20;
                        }
                    }

                    sendRewards(ps.uuid, xp, coins, ps.assists);
                    CoreApi.await(CoreApi.addXp(ps.uuid, xp, "MATCH"));
                    CoreApi.await(CoreApi.addCurrency(ps.uuid, coins, "MATCH_REWARD"));
                    CoreApi.await(CoreApi.checkRank(ps.uuid));

                    JsonObject statsDelta = new JsonObject();
                    statsDelta.addProperty("kills", ps.kills);
                    statsDelta.addProperty("deaths", ps.deaths);
                    statsDelta.addProperty("wins", isWin ? 1 : 0);
                    statsDelta.addProperty("losses", isWin ? 0 : 1);
                    statsDelta.addProperty("playtimeSeconds", durationSec);
                    statsDelta.addProperty("revives", ps.revives);
                    statsDelta.addProperty("vehicleKills", ps.vehicleKills);
                    statsDelta.addProperty("captures", ps.captures);
                    statsDelta.addProperty("damageDealt", ps.damageDealt);
                    statsDelta.addProperty("healingDone", ps.healingDone);
                    statsDelta.addProperty("suppliesDelivered", ps.suppliesDelivered);
                    statsDelta.addProperty("longestKill", ps.longestKill);
                    statsDelta.addProperty("bestKillStreak", ps.bestKillStreak);
                    statsDelta.addProperty("matchesPlayed", 1);
                    statsDelta.addProperty("survivalTime", ps.survivalTime);
                    statsDelta.addProperty("vehiclesDestroyed", ps.vehiclesDestroyed);
                    statsDelta.addProperty("airVehiclesDestroyed", ps.airVehiclesDestroyed);
                    statsDelta.addProperty("teamKills", ps.teamKills);
                    JsonObject saveResult = CoreApi.await(CoreApi.saveStats(ps.uuid, statsDelta));
                    if (saveResult != null) {
                        log.info(
                                "Stats saved for {}: {}k/{}d/{}v/{}vd/{}ad/{}pt",
                                ps.nickname,
                                ps.kills,
                                ps.deaths,
                                ps.vehicleKills,
                                ps.vehiclesDestroyed,
                                ps.airVehiclesDestroyed,
                                durationSec);
                    }

                    log.info("{} earned {} XP and {} Coins", ps.nickname, xp, coins);
                }
            } catch (Exception e) {
                log.error("Failed to save match data: {}", e.getMessage(), e);
            }
        } else {
            log.info("pwp_core_client not installed, skipping match save");
        }
    }

    public static void scheduleServerShutdown(int delaySeconds) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            log.error("scheduleServerShutdown: no server!");
            return;
        }
        log.info("scheduleServerShutdown: scheduling in {}s", delaySeconds);
        new Thread(
                        () -> {
                            try {
                                log.info("Shutdown thread sleeping {}s...", delaySeconds);
                                Thread.sleep((long) delaySeconds * 1000L);
                                log.info("Shutdown delay complete, transferring players...");
                                server.execute(() -> {
                                    try {
                                        for (ServerPlayer player :
                                                server.getPlayerList().getPlayers()) {
                                            // Локальные игроки получают LAN-адрес лобби, внешние — домен
                                            String host = TransferHosts.resolveTransferHost(player);
                                            player.sendSystemMessage(
                                                    Component.translatable("pwpwarfare.match.return_to_lobby")
                                                            .withStyle(net.minecraft.ChatFormatting.YELLOW),
                                                    false);
                                            PacketHandler.INSTANCE.send(
                                                    PacketDistributor.PLAYER.with(() -> player),
                                                    new ConnectToServerPacket(host, WarfareConfig.LOBBY_PORT.get()));
                                        }
                                    } catch (Exception ex) {
                                        log.warn("Transfer failed, halting anyway: {}", ex.getMessage());
                                    }
                                });
                                // 2 с клиентам на переход — ждём ЗДЕСЬ, в фоновом потоке. Раньше
                                // Thread.sleep(2000) стоял внутри server.execute и вешал главный поток.
                                Thread.sleep(2000);
                                server.execute(() -> {
                                    try {
                                        server.halt(false);
                                    } catch (Exception ex) {
                                        log.error("Failed to halt server: {}", ex.getMessage());
                                    }
                                });
                            } catch (InterruptedException e) {
                                log.warn("Shutdown delay interrupted, halting immediately");
                                Thread.currentThread().interrupt();
                                try {
                                    server.halt(false);
                                } catch (Exception ignored) {
                                    log.warn("MatchStatsTracker.scheduleServerShutdown: исключение", ignored);
                                }
                            }
                        },
                        "PWPMatchShutdown")
                .start();
    }

    public long getStartedAt() {
        return startedAt;
    }

    public Map<String, PlayerMatchStats> getAllPlayers() {
        return players;
    }

    /** Награды игроку на экран победы (вызывается из фонового потока расчёта). */
    private static void sendRewards(String uuid, long xp, long coins, int assists) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.execute(() -> {
            try {
                ServerPlayer p = server.getPlayerList().getPlayer(java.util.UUID.fromString(uuid));
                if (p != null) {
                    // канал pwpwarfare (здешний импорт PacketHandler — канал core-client для переброса)
                    com.pigeostudios.pwp.warfare.network.PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> p), new RewardPackets.MatchRewards(xp, coins, assists));
                }
            } catch (IllegalArgumentException e) {
                log.warn("Награды: некорректный uuid {}", uuid);
            }
        });
    }
}

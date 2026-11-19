package com.pigeostudios.pwp.warfare.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.config.MapLayoutState;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.layout.Issue;
import com.pigeostudios.pwp.warfare.layout.StartGate;
import com.pigeostudios.pwp.warfare.match.AdminInput;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Серверные действия управления матчем — ЕДИНАЯ реализация для админ-панели (вкладка «Матч») и команд
 * {@code /pwpwarfare}. Каждый метод возвращает null при успехе или ключ перевода ошибки (раньше команды
 * при неверном вводе молча подставляли дефолт и писали «успех»). Права проверяет вызывающий.
 */
public final class MatchAdmin {
    private static final Logger LOG = LoggerFactory.getLogger(MatchAdmin.class);

    public static final String DATA_TYPE = "adminMatch";

    static final String E = "pwpwarfare.admin.match.error.";
    public static final String ERR_LAYOUT_BLOCKED = E + "layout_blocked";
    static final String ERR_ALREADY_STARTED = E + "already_started";
    static final String ERR_COUNTDOWN = E + "countdown_active";
    static final String ERR_NOTHING_RUNNING = E + "nothing_running";
    static final String ERR_NOT_WAITING = E + "not_waiting";
    static final String ERR_BAD_MODE = E + "bad_mode";
    static final String ERR_BAD_TEAM = E + "bad_team";
    static final String ERR_BAD_FACTION = E + "bad_faction";
    static final String ERR_BAD_TICKETS = E + "bad_tickets";
    static final String ERR_BAD_WAITING = E + "bad_waiting";

    private MatchAdmin() {}

    // ---------- старт и проверка раскладки ----------

    /** Решение «можно ли стартовать» по замечаниям валидатора и флагу конфига. */
    public static StartGate.Decision gate(boolean force) {
        List<Issue> issues = MapLayoutState.layout() == null ? null : MapLayoutState.issues();
        return StartGate.check(issues, WarfareConfig.BLOCK_START_ON_LAYOUT_ERRORS.get(), force);
    }

    /** Ручной старт отсчёта (панель/команда). */
    public static String start(ServerLevel level, boolean force) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (data.match.countdownActive) return ERR_COUNTDOWN;
        if (data.match.isGameStarted) return ERR_ALREADY_STARTED;
        if (gate(force).blocked()) return ERR_LAYOUT_BLOCKED;
        data.match.playedBlueSiren = false;
        data.match.playedRedSiren = false;
        clearWaitingTitles(level, data);
        return GameLogicEvents.startGameCountdown(level, force).blocked() ? ERR_LAYOUT_BLOCKED : null;
    }

    /** «Пропустить ожидание»: конец фазы ожидания сразу, со стартом отсчёта (та же проверка раскладки). */
    public static String skipWaiting(ServerLevel level, boolean force) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (!data.match.waitingActive) return ERR_NOT_WAITING;
        if (gate(force).blocked()) return ERR_LAYOUT_BLOCKED;
        return start(level, force);
    }

    /**
     * Старт заблокирован на автоматическом пути (конец ожидания, голосование «готовы»): игрокам с уровнем
     * ADMIN+ — сообщение в чат, в лог — список ошибок.
     */
    public static void notifyBlocked(ServerLevel level, StartGate.Decision d) {
        LOG.error(
                "[MapLayout] Старт матча ЗАБЛОКИРОВАН: {} ошибок раскладки",
                d.errors().size());
        for (Issue i : d.errors()) LOG.error("[MapLayout]   {}", i.format());
        Component msg = Component.translatable(
                        "pwpwarfare.admin.match.blocked_chat", d.errors().size())
                .withStyle(ChatFormatting.RED);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (PwpPermissions.has(p, AccessLevel.ADMIN)) p.sendSystemMessage(msg);
        }
    }

    private static void clearWaitingTitles(ServerLevel level, WarfareWorldData data) {
        if (!data.match.waitingActive) return;
        data.match.waitingActive = false;
        for (ServerPlayer p : level.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 0, 0));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("")));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
        }
    }

    // ---------- стоп / пауза ----------

    public static String stop(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        boolean running = data.match.isGameStarted
                || data.match.countdownActive
                || data.match.invasionSetupActive
                || data.match.waitingActive;
        if (!running) return ERR_NOTHING_RUNNING;
        boolean wasPlaying = data.match.isGameStarted;
        data.match.isGameStarted = false;
        // этап 3: остановленный бой = конец матча для правки «только на этот матч»
        if (wasPlaying) com.pigeostudios.pwp.warfare.server.map.MapLayoutService.onMatchEnded(level);
        data.match.invasionSetupActive = false;
        clearWaitingTitles(level, data);
        GameLogicEvents.cancelCountdown(level);
        data.setDirty();
        PacketHandler.sendToAllClients(level, data);
        return null;
    }

    /** Переключить паузу; возвращает новое состояние (true — пауза). */
    public static boolean togglePause(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.isPaused = !data.match.isPaused;
        data.setDirty();
        PacketHandler.sendToAllClients(level, data);
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam blueTeam = scoreboard.getPlayerTeam("Blue");
        PlayerTeam redTeam = scoreboard.getPlayerTeam("Red");
        boolean ff = !data.match.isPaused;
        if (blueTeam != null) blueTeam.setAllowFriendlyFire(ff);
        if (redTeam != null) redTeam.setAllowFriendlyFire(ff);
        for (ServerPlayer p : level.players()) {
            if (data.match.isPaused) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 999999, 10));
                p.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.translatable("pwpwarfare.admin.match.paused_subtitle")));
                p.connection.send(
                        new ClientboundSetTitleTextPacket(Component.translatable("pwpwarfare.admin.match.paused_title")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            } else {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 0, 0));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("")));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
            }
        }
        level.getServer()
                .getPlayerList()
                .broadcastSystemMessage(
                        Component.translatable(
                                        data.match.isPaused
                                                ? "pwpwarfare.admin.match.paused_chat"
                                                : "pwpwarfare.admin.match.resumed_chat")
                                .withStyle(data.match.isPaused ? ChatFormatting.RED : ChatFormatting.GREEN),
                        false);
        return data.match.isPaused;
    }

    // ---------- настройки матча ----------

    public static String setMode(ServerLevel level, String mode) {
        String clean = AdminInput.mode(mode);
        if (clean == null) return ERR_BAD_MODE;
        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.gameMode = clean;
        if (clean.equals(AdminInput.MODE_INVASION)) giveAllPointsToDefender(data);
        sync(level, data);
        return null;
    }

    public static String setDefender(ServerLevel level, String team) {
        String t = AdminInput.team(team);
        if (t == null) return ERR_BAD_TEAM;
        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.invasionDefender = t;
        if (AdminInput.MODE_INVASION.equals(data.match.gameMode)) giveAllPointsToDefender(data);
        sync(level, data);
        return null;
    }

    private static void giveAllPointsToDefender(WarfareWorldData data) {
        String defender = data.match.invasionDefender != null ? data.match.invasionDefender : "RED";
        for (WarfareWorldData.CapturePoint cp : data.match.capturePoints) {
            cp.owner = defender;
            cp.progress = 1.0F;
            cp.capturingTeam = "NONE";
            cp.invLocked = false;
        }
    }

    public static String setFaction(ServerLevel level, String team, String faction) {
        String t = AdminInput.team(team);
        if (t == null) return ERR_BAD_TEAM;
        String f = AdminInput.faction(faction);
        if (f == null) return ERR_BAD_FACTION;
        WarfareWorldData data = WarfareWorldData.get(level);
        if (t.equals("BLUE")) data.match.blueFaction = f;
        else data.match.redFaction = f;
        sync(level, data);
        return null;
    }

    public static String setTickets(ServerLevel level, String team, int amount) {
        String t = AdminInput.team(team);
        if (t == null) return ERR_BAD_TEAM;
        if (!AdminInput.ticketsValid(amount)) return ERR_BAD_TICKETS;
        WarfareWorldData data = WarfareWorldData.get(level);
        if (t.equals("BLUE")) {
            data.match.blueTickets = amount;
            if (amount > 50) data.match.playedBlueSiren = false;
        } else {
            data.match.redTickets = amount;
            if (amount > 50) data.match.playedRedSiren = false;
        }
        sync(level, data);
        return null;
    }

    /**
     * Длительность ожидания: идёт ожидание — меняет текущий таймер; {@code persist} (OWNER) — ещё и
     * дефолт в конфиге {@code waitingPhaseSeconds} «навсегда». Без ожидания и без persist — ошибка.
     */
    public static String setWaitingSeconds(ServerLevel level, int seconds, boolean persist) {
        if (!AdminInput.waitingValid(seconds)) return ERR_BAD_WAITING;
        WarfareWorldData data = WarfareWorldData.get(level);
        if (!data.match.waitingActive && !persist) return ERR_NOT_WAITING;
        if (data.match.waitingActive) {
            data.match.waitingTimer = seconds;
            sync(level, data);
        }
        if (persist) {
            WarfareConfig.WAITING_PHASE_SECONDS.set(seconds);
            WarfareConfig.SPEC.save();
        }
        return null;
    }

    private static void sync(ServerLevel level, WarfareWorldData data) {
        data.setDirty();
        PacketHandler.sendToAllClients(level, data);
    }

    // ---------- рестарт, победа, перезагрузка (19.11.2026) ----------

    static final String ERR_NOT_PLAYING = E + "not_playing";
    static final String ERR_MATCH_RUNNING = E + "match_running";
    static final String ERR_RETURNING = E + "returning_to_lobby";
    static final String ERR_NO_CONFIG = E + "no_config";
    static final String ERR_APPLY_FAILED = E + "apply_failed";
    static final String ERR_NO_CORE = E + "no_core";
    static final String ERR_NO_FACTIONS = E + "no_factions";
    /** Шаг «продлить ожидание». */
    public static final int EXTEND_SECONDS = 60;

    /** Итог последней перезагрузки map_config (строка вкладки «Матч»). */
    private static String lastReport = "";

    /** Матч в активной фазе (бой, отсчёт, подготовка Invasion) — перезагрузка конфига запрещена. */
    private static boolean fighting(WarfareWorldData data) {
        return data.match.isGameStarted || data.match.countdownActive || data.match.invasionSetupActive;
    }

    /**
     * Рестарт матча: идущий бой останавливается без итогов (статистика отбрасывается, в core не уходит),
     * игроки команд — в свой мейн без снаряжения (кит сброшен, роль выбирается заново), затем map_config
     * применяется заново → тикеты/точки из конфига, фаза ожидания, загрузка китов и техники.
     */
    public static String restart(ServerLevel level) {
        if (GameLogicEvents.returningToLobby()) return ERR_RETURNING;
        WarfareWorldData data = WarfareWorldData.get(level);
        if (fighting(data) || data.match.waitingActive) {
            String err = stop(level);
            if (err != null && !err.equals(ERR_NOTHING_RUNNING)) return err;
        }
        com.pigeostudios.pwp.warfare.stats.MatchStatsTracker.get().endMatch();
        data.match.isPaused = false;
        String dim = level.dimension().location().toString();
        for (ServerPlayer p : level.players()) {
            String team = p.getTeam() == null ? "" : p.getTeam().getName().toUpperCase(java.util.Locale.ROOT);
            net.minecraft.core.BlockPos spawn = team.equals("BLUE")
                    ? data.match.blueSpawns.get(dim)
                    : team.equals("RED") ? data.match.redSpawns.get(dim) : null;
            if (spawn == null) continue;
            if (!p.isCreative() && !p.isSpectator()) {
                p.getInventory().clearContent();
                com.pigeostudios.pwp.warfare.network.ResupplyHandler.clearCurios(p);
                p.inventoryMenu.broadcastChanges();
                var wd = com.pigeostudios.pwp.warfare.data.state.WarfareData.of(p);
                wd.putString(
                        com.pigeostudios.pwp.warfare.data.state.WarfareKeys.CURRENT_KIT,
                        com.pigeostudios.pwp.warfare.network.KitAccess.UNASSIGNED);
                wd.remove(com.pigeostudios.pwp.warfare.data.state.WarfareKeys.PENDING_KIT);
                com.pigeostudios.pwp.warfare.events.KitUtil.syncMyKit(p);
            }
            if (p.isAlive()) p.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }
        String err = reloadMapConfig(level);
        if (err != null) return err;
        level.getServer()
                .getPlayerList()
                .broadcastSystemMessage(
                        Component.translatable("pwpwarfare.admin.match.restarted_chat")
                                .withStyle(ChatFormatting.GOLD),
                        false);
        return null;
    }

    /** Продлить ожидание на {@link #EXTEND_SECONDS} (не выше {@code AdminInput.WAITING_MAX}). */
    public static String extendWaiting(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (!data.match.waitingActive) return ERR_NOT_WAITING;
        int t = Math.min(AdminInput.WAITING_MAX, data.match.waitingTimer + EXTEND_SECONDS);
        data.match.waitingTimer = t;
        // голосование «готовы» живёт столько же, сколько ожидание (этап 4)
        if (data.match.voteActive) data.match.voteTimer = t;
        sync(level, data);
        com.pigeostudios.pwp.warfare.staging.StagingPackets.broadcastState(level, data);
        return null;
    }

    /** Принудительная победа команды {@code team} — тот же путь, что окончание тикетов (итоги, награды). */
    public static String forceWin(ServerLevel level, String team) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (!data.match.isGameStarted) return ERR_NOT_PLAYING;
        String t = AdminInput.team(team);
        if (t == null) return ERR_BAD_TEAM;
        GameLogicEvents.forceVictory(level, data, t.equals("BLUE"));
        return null;
    }

    /**
     * Перезагрузить map_config без рестарта сервера (только пока бой не идёт): раскладка, тикеты, точки,
     * спавны, слоты техники, фаза ожидания, киты/техника из core. Отчёт — {@code lastReport} в adminMatch,
     * замечания валидатора — во вкладке «Карта».
     */
    public static String reloadMapConfig(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (fighting(data)) return ERR_MATCH_RUNNING;
        if (com.pigeostudios.pwp.warfare.WarfareMod.resolveMapConfigPath() == null) return ERR_NO_CONFIG;
        clearWaitingTitles(level, data);
        data.match.configApplied = false;
        // слоты техники добавляются заново (applyMapConfig их только дописывает) — как в LobbyHandoff
        data.vehicles.configVehicleSpawners.clear();
        data.vehicles.pendingVehicleSpawners.clear();
        com.pigeostudios.pwp.warfare.WarfareMod.applyMapConfig(level);
        com.pigeostudios.pwp.warfare.server.map.MapLayoutService.onServerStarted(level);
        if (!data.match.configApplied) return ERR_APPLY_FAILED;
        int errors = 0;
        int warns = 0;
        for (Issue i : MapLayoutState.issues()) {
            if (i.severity() == Issue.Severity.ERROR) errors++;
            else warns++;
        }
        lastReport = Component.translatable(
                        "pwpwarfare.admin.match.reload_report",
                        data.match.capturePoints.size(),
                        data.match.blueTickets,
                        data.match.redTickets,
                        data.vehicles.configVehicleSpawners.size(),
                        errors,
                        warns)
                .getString();
        LOG.info(
                "[MatchAdmin] map_config перезагружен: точек {}, тикеты {}/{}, слотов {}, ошибок {}, замечаний {}",
                data.match.capturePoints.size(),
                data.match.blueTickets,
                data.match.redTickets,
                data.vehicles.configVehicleSpawners.size(),
                errors,
                warns);
        sync(level, data);
        return null;
    }

    /** Перезагрузить киты и пулы техники фракций из core-service (асинхронно; статус — в adminMatch). */
    public static String reloadFactionData(ServerLevel level) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("pwpcore")) return ERR_NO_CORE;
        WarfareWorldData data = WarfareWorldData.get(level);
        if (data.match.blueFaction == null && data.match.redFaction == null) return ERR_NO_FACTIONS;
        com.pigeostudios.pwp.warfare.server.FactionDataLoader.start(level, data);
        return null;
    }

    // ---------- данные для вкладок «Матч» и «Карта» ----------

    /** JSON типа {@code adminMatch}: состояние матча + замечания раскладки. */
    public static JsonObject toJson(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        var m = data.match;
        JsonObject o = new JsonObject();
        AdminInput.Phase phase = AdminInput.phase(
                m.isPaused, m.countdownActive, m.invasionSetupActive, m.waitingActive, m.voteActive, m.isGameStarted);
        o.addProperty("phase", phase.name());
        int timer =
                switch (phase) {
                    case WAITING -> m.waitingTimer;
                    case COUNTDOWN -> (m.countdownTicks + 19) / 20;
                    case INVASION_SETUP -> m.invasionSetupTimer;
                    case READY_VOTE -> m.voteTimer;
                    default -> 0;
                };
        o.addProperty("timer", Math.max(0, timer));
        o.addProperty("paused", m.isPaused);
        o.addProperty("waiting", m.waitingActive);
        o.addProperty("mode", m.gameMode == null ? AdminInput.MODE_AAS : m.gameMode);
        o.addProperty("defender", m.invasionDefender == null ? "RED" : m.invasionDefender);
        o.addProperty("blueFaction", m.blueFaction);
        o.addProperty("redFaction", m.redFaction);
        o.addProperty("blueTickets", m.blueTickets);
        o.addProperty("redTickets", m.redTickets);
        o.addProperty("waitingDefault", WarfareConfig.WAITING_PHASE_SECONDS.get());
        o.addProperty("blockEnabled", WarfareConfig.BLOCK_START_ON_LAYOUT_ERRORS.get());
        JsonArray factions = new JsonArray();
        AdminInput.DEFAULT_FACTIONS.forEach(factions::add);
        o.add("factions", factions);
        o.add("issues", issuesJson());
        o.addProperty("layoutLoaded", MapLayoutState.layout() != null);
        o.addProperty("lastReport", lastReport);
        o.addProperty("returning", GameLogicEvents.returningToLobby());
        o.add("factionData", com.pigeostudios.pwp.warfare.server.FactionDataLoader.statusJson());
        return o;
    }

    public static JsonArray issuesJson() {
        JsonArray arr = new JsonArray();
        for (Issue i : MapLayoutState.issues()) {
            JsonObject e = new JsonObject();
            e.addProperty("severity", i.severity().name());
            e.addProperty("key", i.langKey());
            e.addProperty("subject", i.subject());
            e.addProperty("detail", i.detail());
            arr.add(e);
        }
        return arr;
    }
}

package com.pigeostudios.pwp.warfare.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.WarfareMod;
import com.pigeostudios.pwp.warfare.server.map.MapLayoutService;
import com.pigeostudios.pwp.warfare.server.map.MapPrerender;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.core.Heartbeat;
import com.pwp.api.lobby.MatchHandoff;
import com.pwp.api.lobby.MatchHandoff.Settings;
import com.pwp.api.lobby.MatchHandoff.Stage;
import com.pwp.api.lobby.MatchHandoff.Status;
import com.pwp.api.time.GameClock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Матч-сервер в цикле лобби (этап 5, канал — {@link MatchHandoff}).
 *
 * <ul>
 *   <li>Старт с готовым {@code lobby_settings.json} (обычный запуск) — настройки вливаются в map_config ДО
 *       {@code applyMapConfig}, дальше всё как раньше.
 *   <li>Предпрогрев ({@code lobby_await.flag} без настроек) — раскладка применяется сразу (граница → пререндер
 *       карты идёт), но фракции/киты/фаза ожидания откладываются. Когда лобби допишет настройки, они
 *       вливаются в map_config и {@code applyMapConfig} повторяется вживую (+ раскладка из БД через
 *       {@link MapLayoutService#onServerStarted}) — игроков на сервере ещё нет, лобби переносит их только
 *       после {@code settingsApplied}.
 *   <li>Раз в секунду — {@code match_status.json} (стадия, пререндер); то же уходит в heartbeat матча.
 * </ul>
 *
 * Работает только в папке {@code match_*} (матч-сервер, который поднимает лобби).
 */
public final class LobbyHandoff {
    private static final Logger LOG = LoggerFactory.getLogger(LobbyHandoff.class);
    private static final Path SETTINGS = Path.of(MatchHandoff.SETTINGS_FILE);
    private static final Path AWAIT = Path.of(MatchHandoff.AWAIT_FLAG);
    private static final Path STATUS = Path.of(MatchHandoff.STATUS_FILE);

    private static volatile boolean awaiting;
    private static volatile boolean settingsApplied;
    private static volatile String error;
    private static boolean enabled;
    private static int ticks;

    private LobbyHandoff() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(LobbyHandoff.class);
    }

    /** Ждём настроек лобби: фракции/киты/фаза ожидания отложены. */
    public static boolean isAwaitingSettings() {
        return awaiting;
    }

    /** Из {@code onServerStarting} до {@code applyMapConfig}. */
    public static void beforeApply() {
        String dir = Path.of("").toAbsolutePath().getFileName().toString();
        enabled = dir.startsWith("match_");
        awaiting = false;
        settingsApplied = false;
        error = null;
        if (!enabled) return;
        if (Files.isRegularFile(SETTINGS)) {
            settingsApplied = mergeSettingsIntoMapConfig();
            if (!settingsApplied) awaiting = true; // битый файл — ждём исправленного, статус ERROR
        } else if (Files.isRegularFile(AWAIT)) {
            awaiting = true;
            LOG.info("[LobbyHandoff] prewarm: waiting for {} from lobby", SETTINGS);
        } else {
            // запуск без лобби-цикла (старый лобби/ручной запуск) — считаем настройки уже в map_config
            settingsApplied = true;
        }
    }

    /** Влить файл настроек в map_config матча; false — файл битый/нет map_config (причина в {@link #error}). */
    private static boolean mergeSettingsIntoMapConfig() {
        try {
            JsonObject raw = JsonParser.parseString(Files.readString(SETTINGS, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            List<String> errors = new ArrayList<>();
            Settings s = Settings.fromJson(raw, errors);
            if (s == null) {
                LOG.error("[LobbyHandoff] {} rejected: bad fields {}", SETTINGS, errors);
                error = "bad_settings";
                return false;
            }
            Path cfg = WarfareMod.resolveMapConfigPath();
            if (cfg == null) {
                LOG.error("[LobbyHandoff] map_config.json not found — lobby settings cannot be applied");
                error = "no_map_config";
                return false;
            }
            JsonObject root = JsonParser.parseString(Files.readString(cfg, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            s.applyTo(root);
            Path tmp = cfg.resolveSibling(cfg.getFileName() + ".tmp");
            Files.writeString(tmp, root.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, cfg, StandardCopyOption.REPLACE_EXISTING);
            error = null;
            LOG.info(
                    "[LobbyHandoff] lobby settings applied to {}: {} vs {}, mode {}",
                    cfg,
                    s.blueFaction(),
                    s.redFaction(),
                    s.mode());
            return true;
        } catch (IOException | RuntimeException e) {
            LOG.error("[LobbyHandoff] failed to apply {}", SETTINGS, e);
            error = "bad_settings";
            return false;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled) return;
        if (++ticks % 20 != 0) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        ServerLevel level = server.overworld();
        if (level == null) return;
        if (awaiting && Files.isRegularFile(SETTINGS) && error == null) applyLive(level);
        else if (awaiting && error != null && Files.isRegularFile(SETTINGS)) retryIfRewritten(level);
        writeStatus();
    }

    private static long badFileStamp;

    /** Битый файл: повторить, только когда лобби перепишет его (по времени изменения). */
    private static void retryIfRewritten(ServerLevel level) {
        try {
            long stamp = Files.getLastModifiedTime(SETTINGS).toMillis();
            if (stamp == badFileStamp) return;
            badFileStamp = stamp;
            error = null;
            applyLive(level);
        } catch (IOException e) {
            LOG.warn("[LobbyHandoff] cannot stat {}", SETTINGS, e);
        }
    }

    /** Предпрогрев закончен: настройки пришли — повторное применение конфига вживую. */
    private static void applyLive(ServerLevel level) {
        if (!mergeSettingsIntoMapConfig()) {
            try {
                badFileStamp = Files.getLastModifiedTime(SETTINGS).toMillis();
            } catch (IOException e) {
                LOG.warn("[LobbyHandoff] cannot stat {}", SETTINGS, e);
            }
            return;
        }
        awaiting = false;
        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.configApplied = false;
        // слоты техники добавляются заново (applyMapConfig их только дописывает)
        data.vehicles.configVehicleSpawners.clear();
        data.vehicles.pendingVehicleSpawners.clear();
        WarfareMod.applyMapConfig(level);
        MapLayoutService.onServerStarted(level);
        settingsApplied = data.match.configApplied;
        if (!settingsApplied) error = "apply_failed";
        LOG.info("[LobbyHandoff] live re-apply after prewarm: {}", settingsApplied ? "ok" : "FAILED");
    }

    private static Status status() {
        int[] p = MapPrerender.progress();
        Stage stage = error != null ? Stage.ERROR : (awaiting ? Stage.AWAITING_SETTINGS : Stage.READY);
        return new Status(stage, settingsApplied && !awaiting, p[0], p[1], error);
    }

    private static void writeStatus() {
        try {
            Path tmp = STATUS.resolveSibling(STATUS.getFileName() + ".tmp");
            Files.writeString(tmp, status().toJson(GameClock.realMillis()).toString(), StandardCharsets.UTF_8);
            Files.move(tmp, STATUS, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            com.pwp.api.util.LogOnce.warn(LOG, "lobby-handoff-status", "[LobbyHandoff] cannot write " + STATUS, e);
        }
    }

    /** Для heartbeat матча ({@link MatchHeartbeatSender}): null — сервер не из цикла лобби. */
    public static Heartbeat withPrep(Heartbeat hb) {
        if (!enabled) return hb;
        Status st = status();
        return hb.prep(st.stage().name(), st.settingsApplied(), st.prerenderDone(), st.prerenderTotal());
    }
}

package com.pigeostudios.pwp.warfare.server.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.WarfareMod;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.config.MapLayoutState;
import com.pigeostudios.pwp.warfare.layout.Issue;
import com.pigeostudios.pwp.warfare.layout.LayoutDerive;
import com.pigeostudios.pwp.warfare.layout.LayoutTransfer;
import com.pigeostudios.pwp.warfare.layout.LayoutValidator;
import com.pigeostudios.pwp.warfare.layout.MapLayout;
import com.pigeostudios.pwp.warfare.layout.MapLayoutCodec;
import com.pigeostudios.pwp.warfare.layout.Team;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.core.CoreApi;
import com.pwp.api.util.ServerThread;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

/**
 * Применение раскладки карты из редактора (этап 3) — тот же результат, что {@code WarfareMod.applyMapConfig}
 * для частей раскладки (граница, мейны, точки, спавны мейна, слоты техники), но вживую, без перезапуска.
 *
 * <p>Источники по приоритету: правка «только на этот матч» ({@link
 * com.pigeostudios.pwp.warfare.world.MapEditRegistry}) → последняя версия «навсегда» из БD (core-service,
 * асинхронно) → map_config.json матча (сид/фолбэк, его же читает applyMapConfig на старте). Всё, что не
 * раскладка (фракции, тикеты, режим, паттерн, защитник), берётся из map_config матча — его переписывает
 * лобби, и версия из БД его не трогает ({@link LayoutTransfer#merge}).
 *
 * <p>Главный поток сервера никогда не ждёт HTTP: версия из БД применяется в колбэке {@code thenOnServer},
 * и только пока матч не начался (фаза ожидания) — иначе ждёт следующего матча.
 */
public final class MapLayoutService {
    private static final Logger LOG = LogUtils.getLogger();

    static final String E = "pwpwarfare.editor.error.";
    public static final String ERR_NOT_LOADED = E + "not_loaded";
    public static final String ERR_BAD_JSON = E + "bad_json";
    public static final String ERR_TOO_LARGE = E + "too_large";
    public static final String ERR_MATCH_RUNNING = E + "match_running";
    public static final String ERR_CORE = E + "core";

    /** Версия из БД, применённая на этом запуске (id; 0 — нет). */
    private static volatile long appliedVersionId;

    private MapLayoutService() {}

    public static void setAppliedVersionId(long id) {
        appliedVersionId = id;
    }

    public static long appliedVersionId() {
        return appliedVersionId;
    }

    /** Имя карты из map_config матча (ключ версий в БД); "" — раскладка не загружалась. */
    public static String mapName() {
        MapLayout base = MapLayoutState.explicit();
        if (base == null) return "";
        JsonObject src = base.source();
        JsonElement n = src.get("name");
        return n != null && n.isJsonPrimitive() ? n.getAsString() : "";
    }

    public static boolean coreAvailable() {
        return com.pwp.api.core.CoreApi.isConfigured();
    }

    /** Матч уже идёт (отсчёт, подготовка Invasion, бой) — точки на лету не меняем. */
    public static boolean matchRunning(WarfareWorldData data) {
        return data.match.isGameStarted || data.match.countdownActive || data.match.invasionSetupActive;
    }

    /** Старт сервера, после applyMapConfig: правка матча из SavedData, иначе — запрос версии из БД. */
    public static void onServerStarted(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (MapLayoutState.explicit() == null) return;
        if (data.mapEdit.hasMatchLayout()) {
            String err = apply(level, data.mapEdit.matchLayoutJson, "match edit by " + data.mapEdit.matchLayoutAuthor);
            if (err != null) LOG.warn("[MapLayout] match edit from SavedData not applied: {}", err);
            return;
        }
        String map = mapName();
        if (!coreAvailable() || map.isEmpty()) return;
        ServerThread.thenOnServer(CoreApi.getLatestMapLayout(map), level.getServer(), resp -> {
            JsonObject version = data(resp);
            if (version == null) {
                LOG.info("[MapLayout] no saved version for {} in core-service — using map_config.json", map);
                return;
            }
            WarfareWorldData now = WarfareWorldData.get(level);
            if (now.mapEdit.hasMatchLayout()) return; // админ уже правит матч — его правка главнее
            if (matchRunning(now)) {
                LOG.warn(
                        "[MapLayout] version {} for {} arrived after match start — applies next match",
                        id(version),
                        map);
                return;
            }
            String err = apply(level, str(version, "json"), "core-service version " + id(version));
            if (err != null) LOG.warn("[MapLayout] core-service version {} not applied: {}", id(version), err);
            else appliedVersionId = id(version);
        });
    }

    /** Конец матча: правка «только на этот матч» пропадает. */
    public static void onMatchEnded(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (!data.mapEdit.hasMatchLayout()) return;
        data.mapEdit.clear();
        data.setDirty();
        LOG.info("[MapLayout] match ended — match-only layout edit dropped");
    }

    /** Разбор JSON раскладки от клиента/БД; null — не JSON-объект. */
    public static MapLayout parse(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return null;
            return MapLayoutCodec.read(el.getAsJsonObject()).layout();
        } catch (RuntimeException e) {
            LOG.warn("[MapLayout] bad layout JSON: {}", e.toString());
            return null;
        }
    }

    /** Правка «только на этот матч» из редактора: запомнить в SavedData и применить. null — успех. */
    public static String applyForMatch(ServerLevel level, String json, String author) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (matchRunning(data)) return ERR_MATCH_RUNNING;
        String err = apply(level, json, "match edit by " + author);
        if (err != null) return err;
        data.mapEdit.matchLayoutJson = json;
        data.mapEdit.matchLayoutAuthor = author;
        data.setDirty();
        return null;
    }

    /**
     * Применить раскладку (только ключи раскладки) поверх map_config матча. null — успех, иначе ключ ошибки.
     * Главный поток.
     */
    public static String apply(ServerLevel level, String json, String sourceLabel) {
        MapLayout base = MapLayoutState.explicit();
        if (base == null) return ERR_NOT_LOADED;
        if (LayoutTransfer.checkSize(json) != null) return ERR_TOO_LARGE;
        MapLayout incoming = parse(json);
        if (incoming == null) return ERR_BAD_JSON;
        WarfareWorldData data = WarfareWorldData.get(level);
        if (matchRunning(data)) return ERR_MATCH_RUNNING;

        MapLayout merged = LayoutTransfer.merge(base, incoming);
        LayoutDerive.Resolved resolved = LayoutDerive.resolve(merged);
        MapLayout layout = resolved.layout();
        List<Issue> issues = new ArrayList<>(resolved.notes());
        issues.addAll(LayoutValidator.validate(layout));
        MapLayoutState.set(layout, issues, sourceLabel);
        MapLayoutState.setExplicit(merged);
        MapLayoutState.log("editor apply");

        // граница
        MapLayout.Border border = layout.border();
        if (border != null) {
            if (border.centerX() != null) data.match.mapCenterX = border.centerX();
            if (border.centerZ() != null) data.match.mapCenterZ = border.centerZ();
            if (border.sizeBlocks() != null) data.match.mapSizeBlocks = border.sizeBlocks();
            level.getWorldBorder().setCenter(data.match.mapCenterX, data.match.mapCenterZ);
            level.getWorldBorder().setSize(data.match.mapSizeBlocks);
        }

        // мейны
        data.structures.mainZones.clear();
        for (MapLayout.MainZoneDef z : layout.mainZones()) {
            data.structures.mainZones.add(
                    new WarfareWorldData.MainProtectionZone(z.team(), z.effShape(), WarfareMod.aabb(z.box())));
        }

        // точки (состояние захвата — заново, как на старте сервера)
        data.match.capturePoints.clear();
        for (MapLayout.CapturePointDef p : layout.points()) data.match.capturePoints.add(WarfareMod.toCapturePoint(p));
        if ("invasion".equals(data.match.gameMode)) {
            for (WarfareWorldData.CapturePoint cp : data.match.capturePoints) {
                cp.owner = data.match.invasionDefender;
                cp.progress = 1.0F;
                cp.capturingTeam = "NONE";
                cp.invLocked = false;
            }
        }

        // спавны мейна
        for (Team team : Team.values()) {
            MapLayout.TeamSpawn spawn = layout.spawn(team);
            if (spawn == null) continue;
            BlockPos pos = new BlockPos(spawn.x(), spawn.effY(), spawn.z());
            (team == Team.BLUE ? data.match.blueSpawns : data.match.redSpawns).put(spawn.effDimension(), pos);
        }

        // слоты техники: блоки старых слотов, которых больше нет, снимаются (если чанк загружен)
        Set<BlockPos> keep = new HashSet<>();
        for (MapLayout.VehicleSlot s : layout.vehicleSlots()) {
            keep.add(new BlockPos(s.pos().x(), s.pos().y(), s.pos().z()));
        }
        for (WarfareWorldData.ConfigVehicleSpawner old : data.vehicles.configVehicleSpawners) {
            BlockPos p = new BlockPos(old.x, old.y, old.z);
            if (keep.contains(p) || !level.isLoaded(p)) continue;
            if (level.getBlockState(p).is(ModBlocks.VEHICLE_SPAWNER_BLOCK.get())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        data.vehicles.configVehicleSpawners.clear();
        data.vehicles.pendingVehicleSpawners.clear();
        for (MapLayout.VehicleSlot s : layout.vehicleSlots()) {
            data.addConfigVehicleSpawner(
                    s.team().name(),
                    s.role(),
                    s.vehicleName(),
                    s.pos().x(),
                    s.pos().y(),
                    s.pos().z(),
                    s.effYaw(),
                    s.effFixed());
        }

        data.setDirty();
        PacketHandler.sendToAllClients(level, data);
        LOG.info(
                "[MapLayout] applied {}: {} points, {} mains, {} vehicle slots",
                sourceLabel,
                layout.points().size(),
                layout.mainZones().size(),
                layout.vehicleSlots().size());
        return null;
    }

    // ---------- разбор ответов core-service ----------

    /** {@code {data: {...}}} → объект data или null. */
    public static JsonObject data(JsonObject resp) {
        if (resp == null || !resp.has("data") || !resp.get("data").isJsonObject()) return null;
        return resp.getAsJsonObject("data");
    }

    public static long id(JsonObject v) {
        return v != null && v.has("id") && v.get("id").isJsonPrimitive()
                ? v.get("id").getAsLong()
                : 0L;
    }

    public static String str(JsonObject o, String k) {
        return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }
}

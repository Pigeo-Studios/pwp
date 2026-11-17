package com.pigeostudios.pwp.warfare.server.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.map.MapOverview;
import com.pigeostudios.pwp.warfare.map.MapPng;
import com.pigeostudios.pwp.warfare.map.MapTileMath;
import com.pigeostudios.pwp.warfare.map.MapTileQuery;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.network.data.DataRequests;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Тип запроса данных {@code mapTile} (единая система {@link DataRequests}): тайлы динамической
 * тактической карты, отрендеренные сервером из мира.
 *
 * <p>Параметры: {@code {"t": "x,z,hash;..."}} ({@link MapTileQuery}, до 6 тайлов). Ответ:
 * {@code {"dim": "...", "tiles": [{"x","z","s","h","png"}]}}, где {@code s} — {@code ok} (есть
 * {@code png} base64), {@code same} (хэш клиента актуален), {@code pending} (рендер идёт /
 * не влезло в ответ — повторить позже), {@code out} (вне играбельной зоны).
 *
 * <p>Кэш: память + диск {@code <мир>/data/pwp_map_tiles/<dim>/<x>_<z>.png}; тайл старше
 * {@link #TTL_MS} отдаётся как есть и перерисовывается в фоне. Рендер — один фоновый поток,
 * на главном потоке только снимок палитр ({@link MapTileRenderer#snapshot}).
 */
@Mod.EventBusSubscriber(modid = "pwpwarfare")
public final class MapTileService {
    private static final Logger LOG = LoggerFactory.getLogger(MapTileService.class);
    public static final String TYPE = "mapTile";

    /** Возраст тайла, после которого он перерисовывается в фоне. */
    static final long TTL_MS = TimeUnit.MINUTES.toMillis(5);
    /** Сколько новых рендеров запускает один запрос (остальные — pending). */
    static final int MAX_RENDERS_PER_REQUEST = 2;
    /** Сколько ждать готовых тайлов, прежде чем ответить pending. */
    static final long WAIT_MS = 6_000;
    /** Бюджет base64 в одном ответе (лимит DataResponse ~1 МБ). */
    static final int RESPONSE_BUDGET = 800_000;
    /** Без карты матча — тайлы только в этом радиусе от игрока. */
    static final int UNBOUNDED_RADIUS = 4096;
    /** Тайлов одного измерения в памяти. */
    static final int MEMORY_CAP = 512;

    record Entry(byte[] png, String hash, long renderedAt) {}

    private static final Map<String, Map<Long, Entry>> MEMORY = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Entry>> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PWP-MapTiles");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private MapTileService() {}

    public static void register() {
        DataRequests.register(TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer player, JsonObject params) {
                return handle(player, DataRequests.str(params, "t", ""));
            }
        });
        MapOverviewService.register();
    }

    /** Главный поток (fetch DataRequests). */
    static CompletableFuture<JsonObject> handle(ServerPlayer player, String query) {
        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();
        String dim = level.dimension().location().toString();
        double[] b = bounds(level, player);
        List<MapTileQuery.Ref> refs = MapTileQuery.parse(query);
        List<CompletableFuture<Entry>> futures = new ArrayList<>();
        int renders = 0;
        for (MapTileQuery.Ref r : refs) {
            if (!intersects(b, r.x(), r.z())) {
                futures.add(null);
                continue;
            }
            Entry e = memory(dim).get(MapTileMath.key(0, r.x(), r.z()));
            if (e != null) {
                if (System.currentTimeMillis() - e.renderedAt() > TTL_MS) startRender(level, server, dim, r, false);
                futures.add(CompletableFuture.completedFuture(e));
            } else if (renders < MAX_RENDERS_PER_REQUEST || IN_FLIGHT.containsKey(flightKey(dim, r))) {
                if (!IN_FLIGHT.containsKey(flightKey(dim, r))) renders++;
                futures.add(startRender(level, server, dim, r, true));
            } else {
                futures.add(CompletableFuture.completedFuture(null));
            }
        }
        CompletableFuture<?>[] all = futures.stream()
                .filter(f -> f != null)
                .map(f -> f.exceptionally(t -> null))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(all)
                .completeOnTimeout(null, WAIT_MS, TimeUnit.MILLISECONDS)
                .thenApplyAsync(v -> respond(dim, refs, futures), EXEC);
    }

    private static JsonObject respond(String dim, List<MapTileQuery.Ref> refs, List<CompletableFuture<Entry>> futures) {
        JsonObject out = new JsonObject();
        out.addProperty("dim", dim);
        JsonArray tiles = new JsonArray();
        int budget = RESPONSE_BUDGET;
        for (int i = 0; i < refs.size(); i++) {
            MapTileQuery.Ref r = refs.get(i);
            CompletableFuture<Entry> f = futures.get(i);
            JsonObject t = new JsonObject();
            t.addProperty("x", r.x());
            t.addProperty("z", r.z());
            Entry e = f == null ? null : f.getNow(null);
            if (f == null) {
                t.addProperty("s", "out");
            } else if (e == null) {
                t.addProperty("s", "pending");
            } else if (e.hash().equals(r.hash())) {
                t.addProperty("s", "same");
                t.addProperty("h", e.hash());
            } else {
                String b64 = Base64.getEncoder().encodeToString(e.png());
                if (b64.length() > budget) {
                    t.addProperty("s", "pending");
                } else {
                    budget -= b64.length();
                    t.addProperty("s", "ok");
                    t.addProperty("h", e.hash());
                    t.addProperty("png", b64);
                }
            }
            tiles.add(t);
        }
        out.add("tiles", tiles);
        return out;
    }

    /**
     * Запуск (или присоединение к) рендеру тайла. {@code tryDisk} — сначала дисковый кэш (холодный
     * старт сервера); свежий файл с диска возвращается без рендера.
     */
    private static CompletableFuture<Entry> startRender(
            ServerLevel level, MinecraftServer server, String dim, MapTileQuery.Ref r, boolean tryDisk) {
        String fk = flightKey(dim, r);
        CompletableFuture<Entry> existing = IN_FLIGHT.get(fk);
        if (existing != null) return existing;
        Path file = tileFile(server, dim, r.x(), r.z());
        long key = MapTileMath.key(0, r.x(), r.z());
        CompletableFuture<Entry> f = CompletableFuture.supplyAsync(() -> tryDisk ? readDisk(file) : null, EXEC)
                .thenCompose(disk -> {
                    if (disk != null && System.currentTimeMillis() - disk.renderedAt() <= TTL_MS) {
                        return CompletableFuture.completedFuture(disk);
                    }
                    // снимок — задачей главного потока (не блокирует текущий тик)
                    return renderFrom(
                            CompletableFuture.supplyAsync(() -> MapTileRenderer.snapshot(level, r.x(), r.z()), server),
                            file,
                            dim,
                            r.x(),
                            r.z(),
                            disk);
                })
                .whenComplete((e, t) -> {
                    if (e != null) put(dim, key, e);
                    IN_FLIGHT.remove(fk);
                });
        CompletableFuture<Entry> raced = IN_FLIGHT.putIfAbsent(fk, f);
        return raced != null ? raced : f;
    }

    /** Снимок → чтение NBT → рендер на фоне → диск. Ошибка — {@code fallback} (может быть null). */
    private static CompletableFuture<Entry> renderFrom(
            CompletableFuture<MapTileRenderer.Snapshot> snap, Path file, String dim, int tx, int tz, Entry fallback) {
        return snap.thenCompose(MapTileRenderer.Snapshot::whenRead)
                .thenApplyAsync(
                        s -> {
                            byte[] png = s.render();
                            Entry e = new Entry(png, MapPng.hash(png), System.currentTimeMillis());
                            writeDisk(file, png);
                            return e;
                        },
                        EXEC)
                .exceptionally(t -> {
                    LOG.warn("Рендер тайла карты {} {},{} не удался", dim, tx, tz, t);
                    return fallback;
                });
    }

    // ------------------------------------------------------------------ для пререндера (MapPrerender)

    /**
     * ФОНОВЫЙ поток: тайл на диске палитровый и не старше своего region-файла — кладётся в память
     * как свежий (renderedAt = сейчас), true. Иначе false — нужен рендер.
     */
    static boolean loadIfFresh(MinecraftServer server, ServerLevel level, String dim, int tx, int tz) {
        long key = MapTileMath.key(0, tx, tz);
        Entry mem = memory(dim).get(key);
        if (mem != null && MapPng.isIndexed(mem.png()) && System.currentTimeMillis() - mem.renderedAt() <= TTL_MS)
            return true;
        Path file = tileFile(server, dim, tx, tz);
        try {
            if (!Files.isRegularFile(file)) return false;
            long tileMtime = Files.getLastModifiedTime(file).toMillis();
            Path region = DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT))
                    .resolve("region")
                    .resolve("r." + MapOverview.regionOf(tx) + "." + MapOverview.regionOf(tz) + ".mca");
            long regionMtime = Files.isRegularFile(region)
                    ? Files.getLastModifiedTime(region).toMillis()
                    : -1;
            byte[] png = Files.readAllBytes(file);
            if (!MapOverview.cacheFresh(MapPng.isIndexed(png), tileMtime, regionMtime)) return false;
            MapPng.decode(png); // проверка целостности
            put(dim, key, new Entry(png, MapPng.hash(png), System.currentTimeMillis()));
            return true;
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("Кэш тайла карты {} не прочитан — будет перерисован", file, e);
            return false;
        }
    }

    /**
     * Пререндер одного тайла из снимка, сделанного вызывающим на ГЛАВНОМ потоке. Если тайл уже
     * рендерится по запросу игрока — присоединяется к нему.
     */
    static CompletableFuture<Entry> renderSnapshot(MinecraftServer server, String dim, MapTileRenderer.Snapshot snap) {
        MapTileQuery.Ref r = new MapTileQuery.Ref(snap.tx, snap.tz, "");
        String fk = flightKey(dim, r);
        CompletableFuture<Entry> existing = IN_FLIGHT.get(fk);
        if (existing != null) return existing;
        long key = MapTileMath.key(0, snap.tx, snap.tz);
        CompletableFuture<Entry> f = renderFrom(
                        CompletableFuture.completedFuture(snap),
                        tileFile(server, dim, snap.tx, snap.tz),
                        dim,
                        snap.tx,
                        snap.tz,
                        null)
                .whenComplete((e, t) -> {
                    if (e != null) put(dim, key, e);
                    IN_FLIGHT.remove(fk);
                });
        CompletableFuture<Entry> raced = IN_FLIGHT.putIfAbsent(fk, f);
        return raced != null ? raced : f;
    }

    /** Тайл в памяти (для обзора/прогресса); null — нет. */
    static Entry cached(String dim, int tx, int tz) {
        return memory(dim).get(MapTileMath.key(0, tx, tz));
    }

    static ExecutorService executor() {
        return EXEC;
    }

    /**
     * {minX, minZ, maxX, maxZ} играбельной зоны матча (граница мира ∩ квадрат карты); null — карта
     * матча не задана (пререндер и обзор не работают — неизвестно, что рендерить).
     */
    static double[] zoneBounds(ServerLevel level) {
        var match = WarfareWorldData.get(level).match;
        if (match.mapSizeBlocks <= 0) return null;
        WorldBorder wb = level.getWorldBorder();
        double h = match.mapSizeBlocks / 2.0;
        double minX = Math.max(wb.getMinX(), match.mapCenterX - h);
        double maxX = Math.min(wb.getMaxX(), match.mapCenterX + h);
        double minZ = Math.max(wb.getMinZ(), match.mapCenterZ - h);
        double maxZ = Math.min(wb.getMaxZ(), match.mapCenterZ + h);
        if (maxX <= minX || maxZ <= minZ) return null;
        return new double[] {minX, minZ, maxX, maxZ};
    }

    /** Диапазон тайлов уровня 0 зоны: {tx0, tz0, tx1, tz1} включительно. */
    static int[] tileRange(double[] b) {
        return new int[] {
            MapTileMath.tileOf(b[0], 0),
            MapTileMath.tileOf(b[1], 0),
            MapTileMath.tileOf(Math.nextDown(b[2]), 0),
            MapTileMath.tileOf(Math.nextDown(b[3]), 0)
        };
    }

    private static void put(String dim, long key, Entry e) {
        Map<Long, Entry> m = memory(dim);
        if (m.size() >= MEMORY_CAP && !m.containsKey(key)) {
            // простая защита памяти: выбросить самый старый
            Long oldest = null;
            long t = Long.MAX_VALUE;
            for (var en : m.entrySet()) {
                if (en.getValue().renderedAt() < t) {
                    t = en.getValue().renderedAt();
                    oldest = en.getKey();
                }
            }
            if (oldest != null) m.remove(oldest);
        }
        m.put(key, e);
    }

    private static Map<Long, Entry> memory(String dim) {
        return MEMORY.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());
    }

    private static String flightKey(String dim, MapTileQuery.Ref r) {
        return dim + "|" + r.x() + "|" + r.z();
    }

    /** {minX, minZ, maxX, maxZ}: граница мира ∩ квадрат карты матча (без карты — радиус от игрока). */
    static double[] bounds(ServerLevel level, ServerPlayer player) {
        WorldBorder wb = level.getWorldBorder();
        double minX = wb.getMinX(), minZ = wb.getMinZ(), maxX = wb.getMaxX(), maxZ = wb.getMaxZ();
        var match = WarfareWorldData.get(level).match;
        if (match.mapSizeBlocks > 0) {
            double h = match.mapSizeBlocks / 2.0;
            minX = Math.max(minX, match.mapCenterX - h);
            maxX = Math.min(maxX, match.mapCenterX + h);
            minZ = Math.max(minZ, match.mapCenterZ - h);
            maxZ = Math.min(maxZ, match.mapCenterZ + h);
        } else {
            minX = Math.max(minX, player.getX() - UNBOUNDED_RADIUS);
            maxX = Math.min(maxX, player.getX() + UNBOUNDED_RADIUS);
            minZ = Math.max(minZ, player.getZ() - UNBOUNDED_RADIUS);
            maxZ = Math.min(maxZ, player.getZ() + UNBOUNDED_RADIUS);
        }
        return new double[] {minX, minZ, maxX, maxZ};
    }

    static boolean intersects(double[] b, int tx, int tz) {
        long x0 = MapTileMath.tileMin(tx, 0), z0 = MapTileMath.tileMin(tz, 0);
        long x1 = x0 + MapTileMath.TILE, z1 = z0 + MapTileMath.TILE;
        return x1 > b[0] && x0 < b[2] && z1 > b[1] && z0 < b[3];
    }

    private static Path tileFile(MinecraftServer server, String dim, int tx, int tz) {
        String safeDim = dim.replaceAll("[^a-zA-Z0-9._-]", "_");
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("pwp_map_tiles")
                .resolve(safeDim)
                .resolve(tx + "_" + tz + ".png");
    }

    private static Entry readDisk(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            byte[] png = Files.readAllBytes(file);
            MapPng.decode(png); // проверка целостности
            return new Entry(
                    png, MapPng.hash(png), Files.getLastModifiedTime(file).toMillis());
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("Битый кэш тайла карты {} — будет перерисован", file, e);
            return null;
        }
    }

    private static void writeDisk(Path file, byte[] png) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, png);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.warn("Не удалось сохранить тайл карты {}", file, e);
        }
    }

    /** Сброс памяти при остановке сервера (одиночная игра: другой мир — другие тайлы); диск остаётся. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clearMemory();
    }

    public static void clearMemory() {
        MEMORY.clear();
        IN_FLIGHT.clear();
        EXEC.execute(MapOverviewService::clear);
    }
}

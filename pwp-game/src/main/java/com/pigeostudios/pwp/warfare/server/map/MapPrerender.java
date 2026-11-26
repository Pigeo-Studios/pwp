package com.pigeostudios.pwp.warfare.server.map;

import com.pigeostudios.pwp.warfare.map.MapOverview;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Фоновый пререндер ВСЕХ тайлов играбельной зоны матча, чтобы игроки в ожидании (лобби матча,
 * деплой) видели готовую карту, а не дорисовку по запросам.
 *
 * <p>Запуск: старт сервера и любая смена зоны матча (центр/размер карты, граница мира — проверка
 * раз в {@link #ZONE_CHECK_TICKS}; так ловятся применение map_config, команды и старт матча без
 * привязки к конкретному событию). Порядок — от горячих точек (центр, мейны, точки захвата).
 *
 * <p>Нагрузка: проверка свежести дискового кэша (палитровый PNG не старше region-файла) — на
 * фоновом потоке; снимок палитр — на главном, но не дольше {@link #TICK_BUDGET_NS} за тик и не
 * больше {@link #MAX_IN_FLIGHT} тайлов в работе; чанки не генерируются (как у запросов mapTile).
 */
@Mod.EventBusSubscriber(modid = "pwpwarfare")
public final class MapPrerender {
    private static final Logger LOG = LoggerFactory.getLogger(MapPrerender.class);

    /** Бюджет снимков на главном потоке за тик (2 мс из 50). */
    static final long TICK_BUDGET_NS = 2_000_000L;
    /** Тайлов одновременно в работе (проверка/рендер) — запросы игроков не ждут в очереди. */
    static final int MAX_IN_FLIGHT = 2;
    /** Как часто сверять зону матча (тики). */
    static final int ZONE_CHECK_TICKS = 100;
    /** Защита от гигантской зоны (64x64 тайла = 16384 блоков). */
    static final int MAX_TILES = 4096;

    private static final Deque<int[]> QUEUE = new ArrayDeque<>();
    private static final ConcurrentLinkedQueue<int[]> TO_SNAPSHOT = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger();
    private static final AtomicInteger DONE = new AtomicInteger();
    private static final AtomicInteger RENDERED = new AtomicInteger();
    private static String zoneSig;
    private static int total;
    private static volatile int generation;
    private static long startedMs;
    private static boolean finishLogged = true;
    private static int tickCounter;

    private MapPrerender() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        reset();
        tickCounter = ZONE_CHECK_TICKS; // проверить зону на первом же тике
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        reset();
    }

    private static void reset() {
        QUEUE.clear();
        TO_SNAPSHOT.clear();
        zoneSig = null;
        total = 0;
        generation++;
        IN_FLIGHT.set(0);
        DONE.set(0);
        RENDERED.set(0);
        finishLogged = true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        ServerLevel level = server.overworld();
        if (level == null) return;
        if (++tickCounter >= ZONE_CHECK_TICKS) {
            tickCounter = 0;
            checkZone(server, level);
        }
        String dim = level.dimension().location().toString();
        int gen = generation;
        // 1) проверка свежести на фоне (без главного потока)
        while (!QUEUE.isEmpty() && IN_FLIGHT.get() < MAX_IN_FLIGHT) {
            int[] t = QUEUE.poll();
            IN_FLIGHT.incrementAndGet();
            CompletableFuture.supplyAsync(
                            () -> MapTileService.loadIfFresh(server, level, dim, t[0], t[1]), MapTileService.executor())
                    .whenComplete((fresh, err) -> {
                        if (err != null) LOG.warn("Пререндер карты: проверка тайла {},{} упала", t[0], t[1], err);
                        if (gen != generation) return; // зона сменилась — счётчики уже сброшены
                        if (Boolean.TRUE.equals(fresh)) {
                            finishOne(false);
                        } else {
                            TO_SNAPSHOT.add(t); // снимок — на главном потоке в своём бюджете
                        }
                    });
        }
        // 2) снимки в бюджете тика
        long start = System.nanoTime();
        for (int[] t; System.nanoTime() - start < TICK_BUDGET_NS && (t = TO_SNAPSHOT.poll()) != null; ) {
            MapTileRenderer.Snapshot snap;
            try {
                snap = MapTileRenderer.snapshot(level, t[0], t[1]);
            } catch (RuntimeException e) {
                LOG.warn("Пререндер карты: снимок тайла {},{} не удался", t[0], t[1], e);
                finishOne(false);
                continue;
            }
            MapTileService.renderSnapshot(server, dim, snap).whenComplete((e, err) -> {
                if (gen == generation) finishOne(e != null);
            });
        }
        if (!finishLogged && total > 0 && DONE.get() >= total) {
            finishLogged = true;
            LOG.info(
                    "Пререндер карты завершён: {} тайлов ({} перерисовано, {} из кэша) за {} с",
                    total,
                    RENDERED.get(),
                    total - RENDERED.get(),
                    (GameClock.realMillis() - startedMs) / 1000);
        }
    }

    private static void finishOne(boolean rendered) {
        if (rendered) RENDERED.incrementAndGet();
        DONE.incrementAndGet();
        IN_FLIGHT.decrementAndGet();
    }

    private static void checkZone(MinecraftServer server, ServerLevel level) {
        double[] b = MapTileService.zoneBounds(level);
        String sig = b == null ? "none" : (long) b[0] + "," + (long) b[1] + "," + (long) b[2] + "," + (long) b[3];
        if (sig.equals(zoneSig)) return;
        zoneSig = sig;
        QUEUE.clear();
        TO_SNAPSHOT.clear();
        generation++;
        IN_FLIGHT.set(0); // задачи прошлого поколения счётчики не трогают (gen != generation)
        DONE.set(0);
        RENDERED.set(0);
        total = 0;
        if (b == null) {
            finishLogged = true;
            return;
        }
        int[] r = MapTileService.tileRange(b);
        long count = (long) (r[2] - r[0] + 1) * (r[3] - r[1] + 1);
        if (count > MAX_TILES) {
            LOG.warn("Пререндер карты пропущен: зона {} тайлов > {}", count, MAX_TILES);
            finishLogged = true;
            return;
        }
        QUEUE.addAll(MapOverview.order(r[0], r[1], r[2], r[3], hotspots(level)));
        total = QUEUE.size();
        startedMs = GameClock.realMillis();
        finishLogged = false;
        LOG.info("Пререндер карты: {} тайлов зоны {} (бюджет {} мс/тик)", total, sig, TICK_BUDGET_NS / 1_000_000.0);
    }

    /** Центр карты, мейны и точки захвата — их тайлы рендерятся первыми. */
    private static List<double[]> hotspots(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        List<double[]> out = new ArrayList<>();
        out.add(new double[] {data.match.mapCenterX, data.match.mapCenterZ});
        for (var z : data.structures.mainZones) add(out, z.area);
        for (var p : data.match.capturePoints) add(out, p.area);
        return out;
    }

    private static void add(List<double[]> out, AABB a) {
        if (a != null) out.add(new double[] {(a.minX + a.maxX) / 2, (a.minZ + a.maxZ) / 2});
    }

    /** Прогресс для обзора и отчёта лобби (этап 5): {готово, всего}; всего 0 — пререндер не идёт. */
    public static int[] progress() {
        return new int[] {Math.min(DONE.get(), total), total};
    }
}

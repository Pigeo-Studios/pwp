package com.pwp.core.metrics;

import com.pwp.core.db.DatabaseManager;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Метрики core-service и игровых серверов для дашборда ({@code /admin/dashboard}) — без внешних
 * зависимостей. Маршруты: число запросов, 5xx, p50/p95/max за последние {@link Route#WINDOW}
 * запросов. Пул Hikari, память JVM, аптайм. Серверы: последний heartbeat (онлайн, MSPT/TPS, heap).
 * Отдаёт {@code GET /api/v1/admin/metrics} (под общим verifyAdmin).
 */
public final class Metrics {
    private static final long STARTED = System.currentTimeMillis();
    private static final Map<String, Route> ROUTES = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> SERVERS = new ConcurrentHashMap<>();
    private static final String T0 = "metrics.t0";

    private Metrics() {}

    public static void install(Javalin app) {
        app.before(ctx -> ctx.attribute(T0, System.nanoTime()));
        app.after(Metrics::record);
        app.get("/api/v1/admin/metrics", ctx -> ctx.json(snapshot()));
    }

    private static void record(Context ctx) {
        Long t0 = ctx.attribute(T0);
        if (t0 == null) return;
        String path = ctx.endpointHandlerPath();
        if (path == null || path.isEmpty() || path.equals("*")) path = "(unmatched)";
        String key = ctx.method().name() + " " + path;
        ROUTES.computeIfAbsent(key, k -> new Route())
                .add((System.nanoTime() - t0) / 1_000_000.0, ctx.statusCode() >= 500);
    }

    /** Последний heartbeat игрового сервера (вызывает NetworkController). */
    public static void recordServer(String server, int online, double mspt, double tps, long heapUsedMb, long heapMaxMb) {
        if (server == null || server.isEmpty()) return;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("online", online);
        m.put("mspt", round(mspt));
        m.put("tps", round(tps));
        m.put("heapUsedMb", heapUsedMb);
        m.put("heapMaxMb", heapMaxMb);
        m.put("at", System.currentTimeMillis());
        SERVERS.put(server, m);
    }

    static Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        out.put("uptimeSec", (System.currentTimeMillis() - STARTED) / 1000);
        out.put("heapUsedMb", (rt.totalMemory() - rt.freeMemory()) >> 20);
        out.put("heapMaxMb", rt.maxMemory() >> 20);
        Map<String, Object> pool = new LinkedHashMap<>();
        try {
            HikariDataSource ds = DatabaseManager.getDataSource() instanceof HikariDataSource h ? h : null;
            HikariPoolMXBean mx = ds != null ? ds.getHikariPoolMXBean() : null;
            if (mx != null) {
                pool.put("active", mx.getActiveConnections());
                pool.put("idle", mx.getIdleConnections());
                pool.put("waiting", mx.getThreadsAwaitingConnection());
                pool.put("total", mx.getTotalConnections());
                pool.put("max", ds.getMaximumPoolSize());
            }
        } catch (RuntimeException e) {
            pool.put("error", e.getClass().getSimpleName());
        }
        out.put("dbPool", pool);
        List<Map<String, Object>> routes = new ArrayList<>();
        ROUTES.forEach((k, r) -> routes.add(r.toMap(k)));
        routes.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        out.put("routes", routes);
        long now = System.currentTimeMillis();
        Map<String, Object> servers = new LinkedHashMap<>();
        SERVERS.forEach((k, v) -> {
            Map<String, Object> m = new LinkedHashMap<>(v);
            m.put("ageSec", (now - (Long) v.get("at")) / 1000);
            servers.put(k, m);
        });
        out.put("servers", servers);
        return out;
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    static final class Route {
        static final int WINDOW = 512;
        final AtomicLong count = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
        private final double[] ring = new double[WINDOW];
        private int pos;
        private int filled;

        void add(double ms, boolean error) {
            count.incrementAndGet();
            if (error) errors.incrementAndGet();
            synchronized (this) {
                ring[pos] = ms;
                pos = (pos + 1) % WINDOW;
                if (filled < WINDOW) filled++;
            }
        }

        Map<String, Object> toMap(String key) {
            double[] copy;
            synchronized (this) {
                copy = Arrays.copyOf(ring, filled);
            }
            Arrays.sort(copy);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("route", key);
            m.put("count", count.get());
            m.put("errors5xx", errors.get());
            m.put("p50ms", copy.length == 0 ? 0 : round(copy[(int) (copy.length * 0.50)]));
            m.put("p95ms", copy.length == 0 ? 0 : round(copy[Math.min(copy.length - 1, (int) (copy.length * 0.95))]));
            m.put("maxMs", copy.length == 0 ? 0 : round(copy[copy.length - 1]));
            return m;
        }
    }
}

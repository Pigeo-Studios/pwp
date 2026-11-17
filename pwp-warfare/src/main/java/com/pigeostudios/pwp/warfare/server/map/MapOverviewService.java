package com.pigeostudios.pwp.warfare.server.map;

import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.map.MapOverview;
import com.pigeostudios.pwp.warfare.map.MapPng;
import com.pigeostudios.pwp.warfare.map.MapTileMath;
import com.pwp.coreclient.network.data.DataRequests;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Тип запроса данных {@code mapOverview}: вся играбельная зона одним грубым палитровым PNG
 * (сторона ≤ {@link MapOverview#MAX_SIDE}) из готовых тайлов памяти + прогресс готовности. Клиент
 * рисует его под детальными тайлами, пока те не пришли, и показывает «Загрузка карты N%».
 *
 * <p>Параметры: {@code {"h": "<хэш обзора клиента>"}}. Ответ: {@code {"dim", "s": ok|same|none,
 * "h", "png", "x0", "z0", "bpp", "pw", "ph", "ready", "total"}}; {@code none} — карта матча не
 * задана. Сборка — на фоновом потоке тайлов, не чаще {@link #MIN_REBUILD_MS}; уменьшенные копии
 * тайлов кэшируются по хэшу (PNG декодируется один раз).
 */
final class MapOverviewService {
    private static final Logger LOG = LoggerFactory.getLogger(MapOverviewService.class);
    static final String TYPE = "mapOverview";
    /** Пересборка обзора не чаще (пока идёт пререндер, тайлы прибывают постоянно). */
    static final long MIN_REBUILD_MS = 2_500;

    private record Small(String hash, int level, int[] px) {}

    // доступ только с фонового потока тайлов (MapTileService.executor — один поток)
    private static final Map<Long, Small> SMALL = new HashMap<>();
    private static String lastKey = "", lastSig = "", lastHash = "";
    private static byte[] lastPng;
    private static long lastBuildMs;

    private MapOverviewService() {}

    static void register() {
        DataRequests.register(TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer player, JsonObject params) {
                ServerLevel level = player.serverLevel();
                String dim = level.dimension().location().toString();
                double[] b = MapTileService.zoneBounds(level);
                String clientHash = DataRequests.str(params, "h", "");
                return CompletableFuture.supplyAsync(() -> build(dim, b, clientHash), MapTileService.executor());
            }
        });
    }

    /** Фоновый поток тайлов. */
    private static JsonObject build(String dim, double[] b, String clientHash) {
        JsonObject out = new JsonObject();
        out.addProperty("dim", dim);
        if (b == null) {
            out.addProperty("s", "none");
            return out;
        }
        int[] r = MapTileService.tileRange(b);
        int tw = r[2] - r[0] + 1, th = r[3] - r[1] + 1;
        if ((long) tw * th > MapPrerender.MAX_TILES) {
            out.addProperty("s", "none");
            return out;
        }
        int level = MapOverview.levelFor(tw, th, MapOverview.MAX_SIDE);
        int q = MapOverview.tilePx(level), pw = tw * q, ph = th * q;
        String key = dim + "|" + r[0] + "," + r[1] + "," + r[2] + "," + r[3] + "|" + level;
        StringBuilder sig = new StringBuilder();
        int ready = 0;
        for (int tz = r[1]; tz <= r[3]; tz++) {
            for (int tx = r[0]; tx <= r[2]; tx++) {
                MapTileService.Entry e = MapTileService.cached(dim, tx, tz);
                sig.append(e == null ? "-" : e.hash()).append(';');
                if (e != null) ready++;
            }
        }
        String s = MapPng.hash(sig.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        long now = System.currentTimeMillis();
        boolean sameKey = key.equals(lastKey) && lastPng != null;
        if (!sameKey || (!s.equals(lastSig) && now - lastBuildMs >= MIN_REBUILD_MS)) {
            try {
                lastPng = compose(dim, r, level, pw, ph);
                lastHash = MapPng.hash(lastPng);
                lastKey = key;
                lastSig = s;
                lastBuildMs = now;
            } catch (IllegalArgumentException ex) {
                LOG.warn("Обзор карты {} не собран", dim, ex);
                if (!sameKey) {
                    out.addProperty("s", "none");
                    return out;
                }
            }
        }
        out.addProperty("ready", ready);
        out.addProperty("total", tw * th);
        out.addProperty("h", lastHash);
        if (lastHash.equals(clientHash)) {
            out.addProperty("s", "same");
            return out;
        }
        out.addProperty("s", "ok");
        out.addProperty("png", Base64.getEncoder().encodeToString(lastPng));
        out.addProperty("x0", MapTileMath.tileMin(r[0], 0));
        out.addProperty("z0", MapTileMath.tileMin(r[1], 0));
        out.addProperty("bpp", 1 << level);
        out.addProperty("pw", pw);
        out.addProperty("ph", ph);
        return out;
    }

    private static byte[] compose(String dim, int[] r, int level, int pw, int ph) {
        int q = MapOverview.tilePx(level);
        int[] px = new int[pw * ph];
        for (int tz = r[1]; tz <= r[3]; tz++) {
            for (int tx = r[0]; tx <= r[2]; tx++) {
                MapTileService.Entry e = MapTileService.cached(dim, tx, tz);
                if (e == null) continue;
                long k = MapTileMath.key(0, tx, tz);
                Small sm = SMALL.get(k);
                if (sm == null || sm.level() != level || !sm.hash().equals(e.hash())) {
                    MapPng.Image img = MapPng.decode(e.png());
                    if (img.width() != MapTileMath.TILE || img.height() != MapTileMath.TILE) continue;
                    int[] small = new int[q * q];
                    MapOverview.blit(img.argb(), small, q, 0, 0, level);
                    sm = new Small(e.hash(), level, small);
                    SMALL.put(k, sm);
                }
                int ox = (tx - r[0]) * q, oz = (tz - r[1]) * q;
                for (int z = 0; z < q; z++) System.arraycopy(sm.px(), z * q, px, (oz + z) * pw + ox, q);
            }
        }
        if (SMALL.size() > 2 * MapPrerender.MAX_TILES) SMALL.clear();
        return MapPng.encodeIndexed(px, pw, ph);
    }

    /** Сброс при остановке сервера (вызывается на фоновом потоке тайлов). */
    static void clear() {
        SMALL.clear();
        lastKey = "";
        lastSig = "";
        lastHash = "";
        lastPng = null;
    }
}

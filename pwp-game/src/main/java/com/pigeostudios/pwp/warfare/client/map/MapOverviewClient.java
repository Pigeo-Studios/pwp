package com.pigeostudios.pwp.warfare.client.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.pigeostudios.pwp.warfare.client.gui.map.MapView;
import com.pigeostudios.pwp.warfare.map.MapOverview;
import com.pigeostudios.pwp.warfare.map.MapPng;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.network.data.DataRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Грубый обзор всей играбельной зоны (DataRequest {@code mapOverview}) — подложка под детальными
 * тайлами, пока те грузятся, и прогресс серверного пререндера. Кэш на диске рядом с тайлами
 * ({@code overview.png} + {@code overview.json}): при повторном входе обзор виден сразу.
 */
@OnlyIn(Dist.CLIENT)
final class MapOverviewClient {
    private static final Logger LOG = LoggerFactory.getLogger(MapOverviewClient.class);
    static final String TYPE = "mapOverview";

    /** Опрос, пока сервер пререндерит (обзор дополняется). */
    static final long POLL_LOADING_MS = 2_500;
    /** Опрос, когда всё готово (свежие воронки/постройки — их дорисует клиент). */
    static final long POLL_IDLE_MS = 60_000;
    /** Ответ потерян — повторить. */
    static final long TIMEOUT_MS = 10_000;

    private static final ConcurrentLinkedQueue<Runnable> MAIN = new ConcurrentLinkedQueue<>();
    private static DynamicTexture tex;
    private static ResourceLocation texId;
    private static long x0, z0;
    private static int bpp, pw, ph;
    private static String hash = "";
    private static int ready, total;
    private static boolean known, none, inFlight, listening;
    private static long sentAt, nextAt;
    private static String ctx;
    private static int texSeq;

    private MapOverviewClient() {}

    static void init() {
        if (listening) return;
        listening = true;
        ClientDataResponses.listen((type, json) -> {
            if (TYPE.equals(type)) onResponse(json);
        });
    }

    /** Тик клиента (из MapTileClient): смена контекста, применение, запрос. */
    static void tick(Minecraft mc, String context, Path cacheDir, long now) {
        if (!context.equals(ctx)) {
            clear(mc);
            ctx = context;
            loadDisk(context, cacheDir);
        }
        for (Runnable r; (r = MAIN.poll()) != null; ) r.run();
        if (inFlight && now - sentAt < TIMEOUT_MS) return;
        inFlight = false;
        if (now < nextAt) return;
        JsonObject params = new JsonObject();
        params.addProperty("h", hash);
        PacketHandler.INSTANCE.sendToServer(new DataRequest(TYPE, params.toString()));
        inFlight = true;
        sentAt = now;
        nextAt = now + POLL_LOADING_MS;
    }

    private static void onResponse(String json) {
        inFlight = false;
        JsonObject o;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return;
            o = el.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            LOG.warn("Битый ответ mapOverview", e);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().toString().equals(str(o, "dim"))) return;
        long now = System.currentTimeMillis();
        String s = str(o, "s");
        if (s.equals("none")) {
            none = true;
            nextAt = now + POLL_IDLE_MS;
            return;
        }
        none = false;
        ready = num(o, "ready");
        total = num(o, "total");
        nextAt = now + (ready < total ? POLL_LOADING_MS : POLL_IDLE_MS);
        if (!s.equals("ok")) return; // same
        String b64 = str(o, "png");
        String h = str(o, "h");
        long ox = (long) num(o, "x0"), oz = (long) num(o, "z0");
        int b = num(o, "bpp");
        String c = ctx;
        Path dir = MapTileClient.cacheDir();
        CompletableFuture.runAsync(
                () -> {
                    try {
                        byte[] png = Base64.getDecoder().decode(b64);
                        MapPng.Image img = MapPng.decode(png);
                        if (b <= 0 || b > (1 << MapOverview.MAX_LEVEL)) return;
                        if (dir != null) {
                            MapTileClient.writeFile(dir.resolve("overview.png"), png);
                            JsonObject meta = new JsonObject();
                            meta.addProperty("x0", ox);
                            meta.addProperty("z0", oz);
                            meta.addProperty("bpp", b);
                            meta.addProperty("h", h);
                            MapTileClient.writeFile(
                                    dir.resolve("overview.json"),
                                    meta.toString().getBytes(StandardCharsets.UTF_8));
                        }
                        MAIN.add(() -> apply(c, img, ox, oz, b, h));
                    } catch (IllegalArgumentException ex) {
                        LOG.warn("Битый обзор карты", ex);
                    }
                },
                Util.backgroundExecutor());
    }

    private static void loadDisk(String c, Path dir) {
        if (dir == null) return;
        CompletableFuture.runAsync(
                () -> {
                    Path png = dir.resolve("overview.png"), meta = dir.resolve("overview.json");
                    try {
                        if (!Files.isRegularFile(png) || !Files.isRegularFile(meta)) return;
                        JsonObject m = JsonParser.parseString(Files.readString(meta, StandardCharsets.UTF_8))
                                .getAsJsonObject();
                        MapPng.Image img = MapPng.decode(Files.readAllBytes(png));
                        long ox = (long) num(m, "x0"), oz = (long) num(m, "z0");
                        int b = num(m, "bpp");
                        if (b <= 0 || b > (1 << MapOverview.MAX_LEVEL)) return;
                        String h = str(m, "h");
                        MAIN.add(() -> {
                            if (!known) apply(c, img, ox, oz, b, h);
                        });
                    } catch (IOException | IllegalArgumentException | IllegalStateException | JsonParseException e) {
                        LOG.warn("Кэш обзора карты {} не прочитан — будет загружен заново", png, e);
                    }
                },
                Util.backgroundExecutor());
    }

    private static void apply(String c, MapPng.Image img, long ox, long oz, int b, String h) {
        if (!c.equals(ctx)) return;
        Minecraft mc = Minecraft.getInstance();
        if (tex != null && (pw != img.width() || ph != img.height())) closeTex(mc);
        if (tex == null) {
            tex = new DynamicTexture(img.width(), img.height(), false);
            texId = new ResourceLocation("pwpwarfare", "map_overview/" + (texSeq++));
            mc.getTextureManager().register(texId, tex);
        }
        NativeImage ni = tex.getPixels();
        if (ni == null) return;
        int[] px = img.argb();
        for (int z = 0; z < img.height(); z++) {
            for (int x = 0; x < img.width(); x++) {
                int col = px[z * img.width() + x];
                ni.setPixelRGBA(x, z, (col & 0xFF00FF00) | ((col >> 16) & 0xFF) | ((col & 0xFF) << 16));
            }
        }
        tex.upload();
        tex.setFilter(true, false);
        pw = img.width();
        ph = img.height();
        x0 = ox;
        z0 = oz;
        bpp = b;
        hash = h;
        known = true;
    }

    /** Подложка-обзор в области карты (ножницы ставит вызывающий). */
    static boolean draw(GuiGraphics g, MapView v) {
        if (!known || texId == null) return false;
        int sx0 = (int) Math.floor(v.sx(x0)), sy0 = (int) Math.floor(v.sy(z0));
        int sx1 = (int) Math.floor(v.sx(x0 + (long) pw * bpp)), sy1 = (int) Math.floor(v.sy(z0 + (long) ph * bpp));
        if (sx1 <= sx0 || sy1 <= sy0) return false;
        g.blit(texId, sx0, sy0, sx1 - sx0, sy1 - sy0, 0F, 0F, pw, ph, pw, ph);
        return true;
    }

    static boolean known() {
        return known;
    }

    /** Готовность серверного пререндера 0..100; неизвестно (нет ответа / нет карты) — -1. */
    static int serverPercent() {
        if (none || total <= 0) return -1;
        return MapOverview.percent(ready, total);
    }

    static void clear(Minecraft mc) {
        closeTex(mc);
        MAIN.clear();
        known = false;
        none = false;
        inFlight = false;
        hash = "";
        ready = 0;
        total = 0;
        nextAt = 0;
        ctx = null;
    }

    private static void closeTex(Minecraft mc) {
        if (texId != null) mc.getTextureManager().release(texId);
        tex = null;
        texId = null;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    private static int num(JsonObject o, String k) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : 0;
        } catch (NumberFormatException e) {
            LOG.warn("mapOverview: поле {} не число", k, e);
            return 0;
        }
    }
}

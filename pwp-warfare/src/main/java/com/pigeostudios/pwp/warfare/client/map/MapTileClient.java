package com.pigeostudios.pwp.warfare.client.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.WarfareUiSettings;
import com.pigeostudios.pwp.warfare.client.gui.map.MapView;
import com.pigeostudios.pwp.warfare.map.MapColumnSampler;
import com.pigeostudios.pwp.warfare.map.MapPng;
import com.pigeostudios.pwp.warfare.map.MapShading;
import com.pigeostudios.pwp.warfare.map.MapTileMath;
import com.pigeostudios.pwp.warfare.map.MapTileQuery;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.ui.layout.Rect;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Клиент динамической тактической карты: тайлы 256x256 блоков (1 px = 1 блок) с сервера
 * (DataRequest {@code mapTile}), дисковый кэш {@code config/pwp_map_cache/<сервер>/<dim>/}, мипы
 * для отдаления (уровни 1..3 собираются из тайлов уровня 0) и дорисовка из загруженных клиентом
 * чанков (свежие постройки/воронки видны сразу).
 *
 * <p>Текстуры — {@link DynamicTexture} на тайл; загрузка в GPU только у изменившихся тайлов.
 * Включается настройкой {@link WarfareUiSettings#MAP_WORLD} (по умолчанию выключено — до
 * проверки в игре); выключено или тайлов нет — {@code MapLayers.tiles} рисует старый PNG.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT)
public final class MapTileClient {
    private static final Logger LOG = LoggerFactory.getLogger(MapTileClient.class);
    private static final String TYPE = "mapTile";

    /** Ответ не пришёл (запрос отброшен антиспамом/потерян) — повторить через. */
    static final long REQUEST_TIMEOUT_MS = 10_000;
    /** Пауза между запросами (кулдаун DataRequests 300 мс). */
    static final long REQUEST_GAP_MS = 400;
    /** Тайл «рендерится» — спросить снова через. */
    static final long PENDING_RETRY_MS = 1_500;
    /** Перепроверка хэша тайла (как TTL сервера). */
    static final long REVERIFY_MS = 5 * 60_000;
    /** Карта не рисовалась дольше — запросы и дорисовка стоят. */
    static final long IDLE_MS = 3_000;
    /** Чанков дорисовки за тик. */
    static final int LIVE_CHUNKS_PER_TICK = 6;
    /** Загрузок текстур в GPU за кадр. */
    static final int UPLOADS_PER_FRAME = 6;
    /** Тайлов уровня 0 в памяти. */
    static final int MEMORY_CAP = 300;
    /** Тайлов в видимой области максимум (защита от бесконечной границы). */
    static final int VISIBLE_LIMIT = 1024;

    private static class Tex {
        final int[] px = new int[MapTileMath.PIXELS];
        DynamicTexture tex;
        ResourceLocation id;
        boolean dirty;

        ResourceLocation upload(Minecraft mc) {
            if (tex == null) {
                tex = new DynamicTexture(MapTileMath.TILE, MapTileMath.TILE, false);
                id = new ResourceLocation("pwpwarfare", "map_tile/" + (nextTexId++));
                mc.getTextureManager().register(id, tex);
                dirty = true;
            }
            if (dirty) {
                NativeImage img = tex.getPixels();
                if (img != null) {
                    for (int z = 0; z < MapTileMath.TILE; z++) {
                        for (int x = 0; x < MapTileMath.TILE; x++) {
                            int c = px[z * MapTileMath.TILE + x];
                            // ARGB -> ABGR (формат NativeImage)
                            img.setPixelRGBA(x, z, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
                        }
                    }
                    tex.upload();
                }
                dirty = false;
            }
            return id;
        }

        void close(Minecraft mc) {
            if (id != null) mc.getTextureManager().release(id);
            tex = null;
            id = null;
        }
    }

    private static final class Tile extends Tex {
        final int tx, tz;
        short[] heights;
        String hash = "";
        boolean hasData;
        boolean diskDone;
        long nextRequestAt;
        long verifiedAt;
        boolean out;
        int version;

        Tile(int tx, int tz) {
            this.tx = tx;
            this.tz = tz;
        }
    }

    private static final class Mip extends Tex {
        long builtVersion = Long.MIN_VALUE;
        boolean any;
    }

    private static final Map<Long, Tile> TILES = new HashMap<>();
    private static final Map<Long, Mip> MIPS = new HashMap<>();
    private static final ConcurrentLinkedQueue<Runnable> MAIN = new ConcurrentLinkedQueue<>();
    private static int nextTexId;
    private static String context;
    private static Path cacheDir;
    private static List<long[]> wanted = List.of();
    private static long lastDrawMs;
    private static boolean inFlight;
    private static long sentAt, lastResponseAt;
    private static int liveCursor;
    private static int liveRadius = -1;
    private static int[][] liveOrder = new int[0][];
    private static boolean listening;

    private MapTileClient() {}

    /** Подписка на ответы (вызов из ClientModEvents.onClientSetup). */
    public static void init() {
        if (listening) return;
        listening = true;
        ClientDataResponses.listen((type, json) -> {
            if (TYPE.equals(type)) onResponse(json);
        });
    }

    public static boolean enabled() {
        return WarfareUiSettings.MAP_WORLD.get();
    }

    /** Есть ли хоть один тайл с данными (для подсказки «нет карты»). */
    public static boolean hasAny() {
        if (!enabled()) return false;
        for (Tile t : TILES.values()) if (t.hasData) return true;
        return false;
    }

    // ------------------------------------------------------------------ рисование

    /**
     * Рисует тайлы в области карты (ножницы ставит вызывающий). false — рисовать нечего
     * (выключено/нет тайлов): вызывающий рисует фолбэк-PNG.
     */
    public static boolean draw(GuiGraphics g, MapView v) {
        if (!enabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        ensureContext(mc);
        lastDrawMs = System.currentTimeMillis();
        Rect a = v.area();
        double[] pw = MapView.playableWorld();
        double x0 = Math.max(pw[0], v.worldX(a.x())), x1 = Math.min(pw[2], v.worldX(a.right()));
        double z0 = Math.max(pw[1], v.worldZ(a.y())), z1 = Math.min(pw[3], v.worldZ(a.bottom()));
        wanted = MapTileMath.tilesIn(x0, z0, x1, z1, 0, v.centerX(), v.centerZ(), VISIBLE_LIMIT);
        for (long[] w : wanted) tile((int) w[0], (int) w[1]);
        double gs = Math.max(1.0, mc.getWindow().getGuiScale());
        int level = MapTileMath.levelFor(v.bpp() / gs);
        boolean smooth = v.bpp() / gs > 1.0;
        int[] uploads = {UPLOADS_PER_FRAME};
        boolean drew = false;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        if (level == 0) {
            for (long[] w : wanted) {
                Tile t = TILES.get(MapTileMath.key(0, (int) w[0], (int) w[1]));
                if (t == null || !t.hasData) continue;
                if (t.dirty && t.tex != null && uploads[0]-- <= 0) {
                    drew |= blit(g, v, t, t.tx, t.tz, 0, smooth); // старая текстура — до следующего кадра
                    continue;
                }
                t.upload(mc);
                drew |= blit(g, v, t, t.tx, t.tz, 0, smooth);
            }
        } else {
            int rebuilds = 1;
            for (long[] w : MapTileMath.tilesIn(x0, z0, x1, z1, level, v.centerX(), v.centerZ(), VISIBLE_LIMIT)) {
                int mx = (int) w[0], mz = (int) w[1];
                Mip m = MIPS.computeIfAbsent(MapTileMath.key(level, mx, mz), k -> new Mip());
                long ver = mipVersion(level, mx, mz);
                if (ver != m.builtVersion && (rebuilds-- > 0 || m.tex == null)) buildMip(m, level, mx, mz, ver);
                if (!m.any) continue;
                if (m.dirty && m.tex != null && uploads[0]-- <= 0) {
                    drew |= blit(g, v, m, mx, mz, level, true);
                    continue;
                }
                m.upload(mc);
                drew |= blit(g, v, m, mx, mz, level, true);
            }
        }
        RenderSystem.disableBlend();
        return drew;
    }

    private static boolean blit(GuiGraphics g, MapView v, Tex t, int tx, int tz, int level, boolean smooth) {
        if (t.id == null) return false;
        long wx = MapTileMath.tileMin(tx, level), wz = MapTileMath.tileMin(tz, level);
        int span = MapTileMath.span(level);
        int sx0 = (int) Math.floor(v.sx(wx)), sy0 = (int) Math.floor(v.sy(wz));
        int sx1 = (int) Math.floor(v.sx(wx + span)), sy1 = (int) Math.floor(v.sy(wz + span));
        if (sx1 <= sx0 || sy1 <= sy0) return false;
        t.tex.setFilter(smooth, false);
        int n = MapTileMath.TILE;
        g.blit(t.id, sx0, sy0, sx1 - sx0, sy1 - sy0, 0F, 0F, n, n, n, n);
        return true;
    }

    private static long mipVersion(int level, int mx, int mz) {
        int f = 1 << level;
        long ver = 0;
        for (int i = 0; i < f; i++) {
            for (int j = 0; j < f; j++) {
                Tile t = TILES.get(MapTileMath.key(0, mx * f + i, mz * f + j));
                if (t != null && t.hasData) ver = ver * 31 + t.version + 1;
                else ver = ver * 31;
            }
        }
        return ver;
    }

    private static void buildMip(Mip m, int level, int mx, int mz, long ver) {
        int f = 1 << level;
        boolean any = false;
        for (int i = 0; i < f; i++) {
            for (int j = 0; j < f; j++) {
                Tile t = TILES.get(MapTileMath.key(0, mx * f + i, mz * f + j));
                int[] src = t != null && t.hasData ? t.px : null;
                any |= src != null;
                MapTileMath.downsampleInto(src, m.px, i, j, f);
            }
        }
        m.any = any;
        m.builtVersion = ver;
        m.dirty = true;
    }

    // ------------------------------------------------------------------ тик: запросы и дорисовка

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !enabled()) {
            if (context != null) clearAll(mc);
            MAIN.clear();
            return;
        }
        ensureContext(mc);
        for (Runnable r; (r = MAIN.poll()) != null; ) r.run();
        long now = System.currentTimeMillis();
        if (now - lastDrawMs > IDLE_MS) return;
        sendRequest(now);
        sampleLive(mc, mc.level);
        evict();
    }

    private static Tile tile(int tx, int tz) {
        long k = MapTileMath.key(0, tx, tz);
        Tile t = TILES.get(k);
        if (t == null) {
            t = new Tile(tx, tz);
            TILES.put(k, t);
            loadDisk(t);
        }
        return t;
    }

    private static void sendRequest(long now) {
        if (inFlight && now - sentAt < REQUEST_TIMEOUT_MS) return;
        inFlight = false;
        if (now - lastResponseAt < REQUEST_GAP_MS || now - sentAt < REQUEST_GAP_MS) return;
        List<MapTileQuery.Ref> refs = new ArrayList<>();
        for (long[] w : wanted) {
            Tile t = TILES.get(MapTileMath.key(0, (int) w[0], (int) w[1]));
            if (t == null || !t.diskDone || t.out || now < t.nextRequestAt) continue;
            if (t.verifiedAt != 0 && now - t.verifiedAt < REVERIFY_MS) continue;
            refs.add(new MapTileQuery.Ref(t.tx, t.tz, t.hash));
            t.nextRequestAt = now + REQUEST_TIMEOUT_MS;
            if (refs.size() >= MapTileQuery.MAX_TILES) break;
        }
        if (refs.isEmpty()) return;
        JsonObject params = new JsonObject();
        params.addProperty("t", MapTileQuery.format(refs));
        PacketHandler.INSTANCE.sendToServer(new DataRequest(TYPE, params.toString()));
        inFlight = true;
        sentAt = now;
    }

    private static void onResponse(String json) {
        inFlight = false;
        lastResponseAt = System.currentTimeMillis();
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return;
            root = el.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            LOG.warn("Битый ответ mapTile", e);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !root.has("tiles") || !root.get("tiles").isJsonArray()) return;
        if (!mc.level.dimension().location().toString().equals(str(root, "dim"))) return;
        String ctx = context;
        long now = System.currentTimeMillis();
        for (JsonElement e : root.getAsJsonArray("tiles")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            if (!o.has("x") || !o.has("z")) continue;
            Tile t = TILES.get(
                    MapTileMath.key(0, o.get("x").getAsInt(), o.get("z").getAsInt()));
            if (t == null) continue;
            switch (str(o, "s")) {
                case "same" -> {
                    t.verifiedAt = now;
                    t.nextRequestAt = 0;
                }
                case "out" -> t.out = true;
                case "ok" -> {
                    String b64 = str(o, "png");
                    String hash = str(o, "h");
                    t.nextRequestAt = now + PENDING_RETRY_MS; // до применения
                    Path file = tileFile(t.tx, t.tz);
                    CompletableFuture.runAsync(
                            () -> {
                                try {
                                    byte[] png = Base64.getDecoder().decode(b64);
                                    MapPng.Image img = MapPng.decode(png);
                                    if (img.width() != MapTileMath.TILE || img.height() != MapTileMath.TILE) return;
                                    writeFile(file, png);
                                    MAIN.add(() -> applyServer(ctx, t, img.argb(), hash));
                                } catch (IllegalArgumentException ex) {
                                    LOG.warn("Битый тайл карты {},{}", t.tx, t.tz, ex);
                                }
                            },
                            Util.backgroundExecutor());
                }
                default -> t.nextRequestAt = now + PENDING_RETRY_MS; // pending
            }
        }
    }

    private static void applyServer(String ctx, Tile t, int[] argb, String hash) {
        if (!ctx.equals(context) || TILES.get(MapTileMath.key(0, t.tx, t.tz)) != t) return;
        System.arraycopy(argb, 0, t.px, 0, t.px.length);
        t.hash = hash;
        t.hasData = true;
        t.dirty = true;
        t.version++;
        t.verifiedAt = System.currentTimeMillis();
        t.nextRequestAt = 0;
        // дорисовка из загруженных чанков перекроет устаревшее на следующем проходе
        liveCursor = 0;
    }

    /** Дорисовка поверх серверных данных из чанков, загруженных клиентом (по кругу от игрока). */
    @SuppressWarnings("unchecked")
    private static void sampleLive(Minecraft mc, ClientLevel level) {
        if (mc.player == null) return;
        int r = Math.max(2, mc.options.getEffectiveRenderDistance());
        if (r != liveRadius) {
            liveRadius = r;
            List<int[]> order = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) order.add(new int[] {dx, dz});
            order.sort((p, q) -> Integer.compare(p[0] * p[0] + p[1] * p[1], q[0] * q[0] + q[1] * q[1]));
            liveOrder = order.toArray(new int[0][]);
            liveCursor = 0;
        }
        double[] pw = MapView.playableWorld();
        int pcx = mc.player.chunkPosition().x, pcz = mc.player.chunkPosition().z;
        int sections = level.getSectionsCount();
        int[] rgb = new int[256];
        short[] h = new short[256];
        byte[] d = new byte[256];
        int done = 0;
        for (int step = 0; step < liveOrder.length && done < LIVE_CHUNKS_PER_TICK; step++) {
            int[] off = liveOrder[liveCursor];
            liveCursor = (liveCursor + 1) % liveOrder.length;
            int cx = pcx + off[0], cz = pcz + off[1];
            double bx = cx * 16.0, bz = cz * 16.0;
            if (bx + 16 <= pw[0] || bx >= pw[2] || bz + 16 <= pw[1] || bz >= pw[3]) continue;
            LevelChunk c = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
            if (c == null || c instanceof EmptyLevelChunk) continue;
            done++;
            LevelChunkSection[] src = c.getSections();
            PalettedContainerRO<BlockState>[] secs = new PalettedContainerRO[sections];
            for (int k = 0; k < src.length && k < sections; k++) {
                if (src[k] != null && !src[k].hasOnlyAir()) secs[k] = src[k].getStates();
            }
            MapColumnSampler.sample(secs, level.getMinBuildHeight(), rgb, h, d, 16, 0, 0);
            applyLive(Math.floorDiv(cx, MapTileMath.CHUNKS), Math.floorDiv(cz, MapTileMath.CHUNKS), cx, cz, rgb, h, d);
        }
    }

    private static void applyLive(int tx, int tz, int cx, int cz, int[] rgb, short[] h, byte[] d) {
        Tile t = tile(tx, tz);
        if (t.heights == null) {
            t.heights = new short[MapTileMath.PIXELS];
            Arrays.fill(t.heights, MapShading.NO_HEIGHT);
        }
        int ox = Math.floorMod(cx, MapTileMath.CHUNKS) * 16, oz = Math.floorMod(cz, MapTileMath.CHUNKS) * 16;
        int w = MapTileMath.TILE;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                t.heights[(oz + z) * w + ox + x] =
                        rgb[z * 16 + x] == MapShading.NO_COLOR ? MapShading.NO_HEIGHT : h[z * 16 + x];
            }
        }
        boolean changed = false;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = (oz + z) * w + ox + x;
                int hn = oz + z > 0 ? t.heights[i - w] : MapShading.NO_HEIGHT;
                int hw = ox + x > 0 ? t.heights[i - 1] : MapShading.NO_HEIGHT;
                int p = MapShading.pixel(rgb[z * 16 + x], h[z * 16 + x], hn, hw, d[z * 16 + x] & 0xFF);
                if (t.px[i] != p) {
                    t.px[i] = p;
                    changed = true;
                }
            }
        }
        if (changed) {
            t.hasData = true;
            t.dirty = true;
            t.version++;
        }
    }

    // ------------------------------------------------------------------ кэш/контекст

    private static void ensureContext(Minecraft mc) {
        String dim = mc.level.dimension().location().toString();
        String ctx = serverId(mc) + "/" + safe(dim);
        if (ctx.equals(context)) return;
        clearAll(mc);
        context = ctx;
        cacheDir = FMLPaths.CONFIGDIR
                .get()
                .resolve("pwp_map_cache")
                .resolve(serverId(mc))
                .resolve(safe(dim));
    }

    private static String serverId(Minecraft mc) {
        String host;
        ServerData sd = mc.getCurrentServer();
        if (sd != null && sd.ip != null) {
            String ip = sd.ip;
            int colon = ip.lastIndexOf(':');
            host = colon > 0 && ip.indexOf(':') == colon ? ip.substring(0, colon) : ip;
        } else if (mc.getSingleplayerServer() != null) {
            host = "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            host = "unknown";
        }
        String map = ClientData.currentMapImage == null ? "" : ClientData.currentMapImage;
        return safe(host + "_" + map);
    }

    private static String safe(String s) {
        String r = s.replaceAll("[^a-zA-Z0-9._-]", "_");
        return r.length() > 64 ? r.substring(0, 64) : r;
    }

    private static Path tileFile(int tx, int tz) {
        return cacheDir.resolve(tx + "_" + tz + ".png");
    }

    private static void loadDisk(Tile t) {
        String ctx = context;
        Path file = tileFile(t.tx, t.tz);
        CompletableFuture.runAsync(
                () -> {
                    int[] px = null;
                    String hash = "";
                    try {
                        if (Files.isRegularFile(file)) {
                            byte[] png = Files.readAllBytes(file);
                            MapPng.Image img = MapPng.decode(png);
                            if (img.width() == MapTileMath.TILE && img.height() == MapTileMath.TILE) {
                                px = img.argb();
                                hash = MapPng.hash(png);
                            }
                        }
                    } catch (IOException | IllegalArgumentException e) {
                        LOG.warn("Битый кэш тайла карты {} — будет загружен заново", file, e);
                    }
                    int[] fpx = px;
                    String fh = hash;
                    MAIN.add(() -> {
                        if (!ctx.equals(context) || TILES.get(MapTileMath.key(0, t.tx, t.tz)) != t) return;
                        t.diskDone = true;
                        if (fpx != null && !t.hasData) {
                            System.arraycopy(fpx, 0, t.px, 0, t.px.length);
                            t.hash = fh;
                            t.hasData = true;
                            t.dirty = true;
                            t.version++;
                        } else if (fpx != null) {
                            t.hash = fh; // дорисовка уже легла поверх — хэш для сверки
                        }
                    });
                },
                Util.backgroundExecutor());
    }

    private static void writeFile(Path file, byte[] png) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, png);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.warn("Не удалось сохранить тайл карты {}", file, e);
        }
    }

    private static void evict() {
        if (TILES.size() <= MEMORY_CAP) return;
        Minecraft mc = Minecraft.getInstance();
        java.util.Set<Long> keep = new java.util.HashSet<>();
        for (long[] w : wanted) keep.add(MapTileMath.key(0, (int) w[0], (int) w[1]));
        Iterator<Map.Entry<Long, Tile>> it = TILES.entrySet().iterator();
        while (it.hasNext() && TILES.size() > MEMORY_CAP) {
            var e = it.next();
            if (keep.contains(e.getKey())) continue;
            e.getValue().close(mc);
            it.remove();
        }
    }

    private static void clearAll(Minecraft mc) {
        for (Tile t : TILES.values()) t.close(mc);
        for (Mip m : MIPS.values()) m.close(mc);
        TILES.clear();
        MIPS.clear();
        wanted = List.of();
        context = null;
        inFlight = false;
        liveCursor = 0;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }
}

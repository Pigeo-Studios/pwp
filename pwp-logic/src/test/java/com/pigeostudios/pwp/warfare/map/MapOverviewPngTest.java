package com.pigeostudios.pwp.warfare.map;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Палитровый PNG, обзор карты и правила пререндера. */
class MapOverviewPngTest {

    /** Синтетический тайл «как из мира»: несколько MapColor, холмы, вода, дыра (несгенерировано). */
    static int[] terrainTile(long seed) {
        return terrainTile(seed, 20);
    }

    /** {@code noiseEvery} — каждый N-й пиксель случайно меняет блок (цветы, тропинки); 0 — без шума. */
    static int[] terrainTile(long seed, int noiseEvery) {
        Random r = new Random(seed);
        int w = MapTileMath.TILE;
        int[] base = {0x7FB238, 0x667F33, 0x976D4D, 0x707070, 0xF7E9A3, 0x4040FF, 0x8F7748, 0x007C00};
        int[] rgb = new int[MapTileMath.PIXELS];
        short[] h = new short[MapTileMath.PIXELS];
        byte[] d = new byte[MapTileMath.PIXELS];
        double p1 = r.nextDouble() * 6, p2 = r.nextDouble() * 6;
        for (int z = 0; z < w; z++) {
            for (int x = 0; x < w; x++) {
                int i = z * w + x;
                double hh =
                        70 + 12 * Math.sin(x / 23.0 + p1) * Math.cos(z / 31.0 + p2) + 4 * Math.sin(x / 7.0 + z / 9.0);
                h[i] = (short) Math.round(hh);
                if (hh < 62) {
                    rgb[i] = base[5];
                    d[i] = (byte) Math.min(30, (int) (62 - hh));
                } else {
                    int k = ((x / 19) * 7 + (z / 23) * 3 + (noiseEvery > 0 && r.nextInt(noiseEvery) == 0 ? 1 : 0))
                            % base.length;
                    rgb[i] = k == 5 ? base[0] : base[k];
                }
                if (x > 200 && z > 220) rgb[i] = MapShading.NO_COLOR;
            }
        }
        int[] out = new int[MapTileMath.PIXELS];
        MapShading.render(rgb, h, d, w, w, out);
        return out;
    }

    @Test
    void indexedRoundTripIsLossless() {
        int[] px = terrainTile(1);
        Set<Integer> colors = new HashSet<>();
        for (int c : px) colors.add((c >>> 24) == 0 ? 0 : c);
        assertTrue(colors.size() <= MapPng.MAX_PALETTE, "цветов в тайле: " + colors.size());
        byte[] png = MapPng.encodeIndexed(px, 256, 256);
        assertTrue(MapPng.isIndexed(png));
        MapPng.Image img = MapPng.decode(png);
        for (int i = 0; i < px.length; i++) {
            int want = (px[i] >>> 24) == 0 ? 0 : px[i];
            int got = (img.argb()[i] >>> 24) == 0 ? 0 : img.argb()[i];
            assertEquals(want, got, "px " + i);
        }
    }

    @Test
    void indexedIsMuchSmallerThanRgba() {
        int[] px = terrainTile(7);
        int rgba = MapPng.encode(px, 256, 256).length;
        int idx = MapPng.encodeIndexed(px, 256, 256).length;
        assertTrue(idx * 2 < rgba, "rgba=" + rgba + " indexed=" + idx);
    }

    @Test
    void moreThan256ColorsMapToNearest() {
        int[] px = new int[64 * 64];
        for (int i = 0; i < px.length; i++) px[i] = 0xFF000000 | (i * 4099);
        px[0] = 0;
        px[1] = 0x80123456; // полупрозрачный
        byte[] png = MapPng.encodeIndexed(px, 64, 64);
        MapPng.Image img = MapPng.decode(png);
        Set<Integer> out = new HashSet<>();
        for (int c : img.argb()) out.add(c);
        assertTrue(out.size() <= 256);
        assertEquals(0, img.argb()[0] >>> 24);
        // ошибка квантования мала относительно размаха цветов
        long err = 0;
        for (int i = 2; i < px.length; i++) {
            int a = px[i], b = img.argb()[i];
            err += Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF))
                    + Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                    + Math.abs((a & 0xFF) - (b & 0xFF));
        }
        assertTrue(err / (px.length - 2) < 90, "средняя ошибка " + err / (px.length - 2));
    }

    @Test
    void rgbaStillDecodesAndIsNotIndexed() {
        int[] px = terrainTile(3);
        byte[] old = MapPng.encode(px, 256, 256);
        assertFalse(MapPng.isIndexed(old));
        assertArrayEquals(px, MapPng.decode(old).argb());
    }

    @Test
    void indexedTransparentOnly() {
        byte[] png = MapPng.encodeIndexed(new int[16], 4, 4);
        for (int c : MapPng.decode(png).argb()) assertEquals(0, c);
    }

    @Test
    void indexedRejectsBadIndex() {
        int[] px = {0xFF112233, 0xFF445566, 0xFF112233, 0xFF445566};
        byte[] png = MapPng.encodeIndexed(px, 2, 2);
        assertArrayEquals(px, MapPng.decode(png).argb());
    }

    @Test
    void overviewLevelFitsMaxSide() {
        assertEquals(0, MapOverview.levelFor(2, 2, 512));
        assertEquals(1, MapOverview.levelFor(3, 2, 512));
        assertEquals(3, MapOverview.levelFor(12, 12, 512)); // 3072 блоков -> 384 px
        assertTrue(12 * MapOverview.tilePx(3) <= 512);
        assertEquals(MapOverview.MAX_LEVEL, MapOverview.levelFor(100_000, 1, 512));
    }

    @Test
    void overviewBlitAverages() {
        int[] src = new int[MapTileMath.PIXELS];
        java.util.Arrays.fill(src, 0xFF204060);
        int lvl = 4, q = MapOverview.tilePx(lvl), dstW = q * 2;
        int[] dst = new int[dstW * q];
        MapOverview.blit(src, dst, dstW, 1, 0, lvl);
        MapOverview.blit(null, dst, dstW, 0, 0, lvl);
        assertEquals(0, dst[0]);
        assertEquals(0xFF204060, dst[q]);
        assertEquals(0xFF204060, dst[dstW * q - 1]);
    }

    @Test
    void orderNearestHotspotFirst() {
        List<int[]> o = MapOverview.order(-2, -2, 2, 2, List.of(new double[] {512 + 128, 512 + 128}));
        assertEquals(25, o.size());
        assertArrayEquals(new int[] {2, 2}, o.get(0));
        assertArrayEquals(new int[] {-2, -2}, o.get(24));
        List<int[]> c = MapOverview.order(-1, -1, 0, 0, List.of());
        assertEquals(4, c.size());
    }

    @Test
    void freshnessAndPercent() {
        assertFalse(MapOverview.cacheFresh(false, 100, 50)); // старый RGBA — перерисовать
        assertTrue(MapOverview.cacheFresh(true, 100, 50));
        assertFalse(MapOverview.cacheFresh(true, 40, 50)); // region новее тайла
        assertTrue(MapOverview.cacheFresh(true, 40, -1)); // region-файла нет
        assertFalse(MapOverview.cacheFresh(true, 0, -1)); // тайла нет
        assertEquals(0, MapOverview.regionOf(1));
        assertEquals(-1, MapOverview.regionOf(-1));
        assertEquals(-1, MapOverview.regionOf(-2));
        assertEquals(1, MapOverview.regionOf(2));
        assertEquals(50, MapOverview.percent(1, 2));
        assertEquals(100, MapOverview.percent(0, 0));
    }
}

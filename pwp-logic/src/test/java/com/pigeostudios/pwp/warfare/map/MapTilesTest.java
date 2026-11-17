package com.pigeostudios.pwp.warfare.map;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MapTilesTest {

    @Test
    void tileIndexFloorsNegative() {
        assertEquals(0, MapTileMath.tileOf(0, 0));
        assertEquals(0, MapTileMath.tileOf(255.9, 0));
        assertEquals(1, MapTileMath.tileOf(256, 0));
        assertEquals(-1, MapTileMath.tileOf(-0.1, 0));
        assertEquals(-1, MapTileMath.tileOf(-256, 0));
        assertEquals(-2, MapTileMath.tileOf(-257, 0));
        assertEquals(-1, MapTileMath.tileOf(-1, 3));
        assertEquals(-2048L, MapTileMath.tileMin(-1, 3));
    }

    @Test
    void keyRoundTrip() {
        int[][] cases = {{0, 0, 0}, {3, -5, 7}, {1, -(1 << 18), (1 << 18)}, {2, 12345, -54321}};
        for (int[] c : cases) {
            long k = MapTileMath.key(c[0], c[1], c[2]);
            assertEquals(c[0], MapTileMath.keyLevel(k));
            assertEquals(c[1], MapTileMath.keyX(k));
            assertEquals(c[2], MapTileMath.keyZ(k));
        }
        assertNotEquals(MapTileMath.key(0, 1, 0), MapTileMath.key(1, 1, 0));
    }

    @Test
    void levelForScale() {
        assertEquals(0, MapTileMath.levelFor(0.25));
        assertEquals(0, MapTileMath.levelFor(1.0));
        assertEquals(0, MapTileMath.levelFor(1.9));
        assertEquals(1, MapTileMath.levelFor(2.0));
        assertEquals(2, MapTileMath.levelFor(5));
        assertEquals(3, MapTileMath.levelFor(100));
        assertEquals(0, MapTileMath.levelFor(Double.NaN));
    }

    @Test
    void tilesInCoversRectNearestFirst() {
        List<long[]> t = MapTileMath.tilesIn(-1024, -1024, 1024, 1024, 0, 0, 0, 1000);
        assertEquals(64, t.size());
        long[] first = t.get(0);
        assertTrue(first[0] == 0 || first[0] == -1);
        // граница ровно на краю тайла не захватывает лишний
        assertEquals(1, MapTileMath.tilesIn(0, 0, 256, 256, 0, 0, 0, 10).size());
        // бесконечная граница — пусто (лимит)
        assertTrue(MapTileMath.tilesIn(-3e7, -3e7, 3e7, 3e7, 0, 0, 0, 500).isEmpty());
        assertTrue(MapTileMath.tilesIn(5, 5, 5, 9, 0, 0, 0, 10).isEmpty());
    }

    @Test
    void downsampleAveragesAndKeepsTransparent() {
        int[] src = new int[MapTileMath.PIXELS];
        // левая половина — красная, правая — прозрачная
        for (int z = 0; z < 256; z++) for (int x = 0; x < 128; x++) src[z * 256 + x] = 0xFFFF0000;
        // один синий пиксель в первом блоке 2x2
        src[0] = 0xFF0000FF;
        int[] dst = new int[MapTileMath.PIXELS];
        MapTileMath.downsampleInto(src, dst, 1, 0, 2);
        int c = dst[128]; // квадрант (1,0), первый пиксель
        assertEquals(0xFF, c >>> 24);
        assertEquals((255 * 3) / 4, (c >> 16) & 0xFF);
        assertEquals(255 / 4, c & 0xFF);
        assertEquals(0, dst[128 + 127]); // правая половина источника прозрачна
        assertEquals(0, dst[0]); // соседний квадрант не тронут
        MapTileMath.downsampleInto(null, dst, 1, 0, 2);
        assertEquals(0, dst[128]);
    }

    @Test
    void shadingSlopesAndWater() {
        int grass = 0x7FB238;
        int flat = MapShading.pixel(grass, 63, 63, 63, 0);
        int up = MapShading.pixel(grass, 66, 63, 63, 0);
        int down = MapShading.pixel(grass, 60, 63, 63, 0);
        assertEquals(0xFF000000 | grass, flat);
        assertTrue(((up >> 8) & 0xFF) > ((flat >> 8) & 0xFF));
        assertTrue(((down >> 8) & 0xFF) < ((flat >> 8) & 0xFF));
        // сосед неизвестен — как ровный
        assertEquals(flat, MapShading.pixel(grass, 63, MapShading.NO_HEIGHT, MapShading.NO_HEIGHT, 0));
        assertEquals(0, MapShading.pixel(MapShading.NO_COLOR, 63, 63, 63, 0));
        int water = 0x4040FF;
        int shallow = MapShading.pixel(water, 62, 62, 62, 1);
        int deep = MapShading.pixel(water, 62, 62, 62, 30);
        assertTrue((shallow & 0xFF) > (deep & 0xFF));
        assertEquals(0xFF, deep >>> 24);
    }

    @Test
    void renderUsesNeighbours() {
        int[] rgb = {0x808080, 0x808080, 0x808080, MapShading.NO_COLOR};
        short[] h = {63, 70, 63, 0};
        byte[] d = new byte[4];
        int[] out = new int[4];
        MapShading.render(rgb, h, d, 2, 2, out);
        assertEquals(MapShading.pixel(0x808080, 70, MapShading.NO_HEIGHT, 63, 0), out[1]);
        assertEquals(0, out[3]);
    }

    @Test
    void pngRoundTrip() {
        Random r = new Random(42);
        int[] px = new int[MapTileMath.PIXELS];
        for (int i = 0; i < px.length; i++) {
            px[i] = (i % 7 == 0) ? 0 : (0xFF000000 | (r.nextInt(4) * 0x404040) | (i & 0xFF));
        }
        byte[] png = MapPng.encode(px, 256, 256);
        MapPng.Image img = MapPng.decode(png);
        assertEquals(256, img.width());
        assertEquals(256, img.height());
        assertArrayEquals(px, img.argb());
        assertEquals(16, MapPng.hash(png).length());
        assertEquals(MapPng.hash(png), MapPng.hash(MapPng.encode(px, 256, 256)));
    }

    @Test
    void pngFlatTileIsSmall() {
        int[] px = new int[MapTileMath.PIXELS];
        java.util.Arrays.fill(px, 0xFF7FB238);
        assertTrue(MapPng.encode(px, 256, 256).length < 4096);
    }

    @Test
    void pngRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> MapPng.decode(new byte[10]));
        byte[] png = MapPng.encode(new int[4], 2, 2);
        png[1] = 'X';
        assertThrows(IllegalArgumentException.class, () -> MapPng.decode(png));
    }

    @Test
    void queryRoundTripAndValidation() {
        var refs = List.of(new MapTileQuery.Ref(-3, 4, "00ff"), new MapTileQuery.Ref(10, -20, ""));
        String s = MapTileQuery.format(refs);
        assertEquals(refs, MapTileQuery.parse(s));
        var bad = MapTileQuery.parse("1,2,ZZ;a,b,c;999999999,1,;1,2,ab;3,4;-,1");
        assertEquals(2, bad.size());
        assertEquals("", bad.get(0).hash());
        assertEquals(new MapTileQuery.Ref(3, 4, ""), bad.get(1));
        assertEquals(
                MapTileQuery.MAX_TILES,
                MapTileQuery.parse("1,1;2,2;3,3;4,4;5,5;6,6;7,7;8,8").size());
        assertTrue(MapTileQuery.parse(null).isEmpty());
    }
}

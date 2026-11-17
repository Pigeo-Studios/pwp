package com.pigeostudios.pwp.warfare.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Математика тайлов динамической тактической карты (без Minecraft).
 *
 * <p>Тайл уровня 0 — квадрат {@link #TILE} x {@link #TILE} блоков, 1 пиксель = 1 блок. Тайл
 * уровня L (мип) покрывает {@code TILE << L} блоков тем же числом пикселей (1 px = 2^L блоков) и
 * собирается из 2^L x 2^L тайлов уровня 0 усреднением ({@link #downsampleInto}).
 *
 * <p>Ключ тайла — {@code long}: уровень в старших битах, tx/tz по 28 бит со знаком.
 */
public final class MapTileMath {
    private MapTileMath() {}

    /** Сторона тайла в блоках (и пикселях) на уровне 0. */
    public static final int TILE = 256;
    /** Пикселей в тайле. */
    public static final int PIXELS = TILE * TILE;
    /** Максимальный уровень мипа (1 px = 8 блоков). */
    public static final int MAX_LEVEL = 3;
    /** Чанков по стороне тайла уровня 0. */
    public static final int CHUNKS = TILE / 16;

    private static final long MASK28 = (1L << 28) - 1;

    /** Индекс тайла уровня {@code level}, которому принадлежит мировая координата блока. */
    public static int tileOf(double block, int level) {
        return (int) Math.floor(block / (double) (TILE << level));
    }

    /** Мировая координата левого/верхнего края тайла. */
    public static long tileMin(int tile, int level) {
        return (long) tile * (TILE << level);
    }

    /** Блоков на сторону тайла уровня. */
    public static int span(int level) {
        return TILE << level;
    }

    /**
     * Уровень мипа для текущего масштаба: {@code blocksPerScreenPixel} — сколько блоков в одном
     * реальном пикселе экрана. Берём самый крупный уровень, у которого текстель не больше пикселя.
     */
    public static int levelFor(double blocksPerScreenPixel) {
        if (!(blocksPerScreenPixel > 1.0)) return 0;
        int l = (int) Math.floor(Math.log(blocksPerScreenPixel) / Math.log(2) + 1e-9);
        return Math.max(0, Math.min(MAX_LEVEL, l));
    }

    public static long key(int level, int tx, int tz) {
        return ((long) level << 56) | ((tx & MASK28) << 28) | (tz & MASK28);
    }

    public static int keyLevel(long key) {
        return (int) (key >>> 56);
    }

    public static int keyX(long key) {
        return signed28((key >>> 28) & MASK28);
    }

    public static int keyZ(long key) {
        return signed28(key & MASK28);
    }

    private static int signed28(long v) {
        return (int) ((v << 36) >> 36);
    }

    /**
     * Тайлы уровня, пересекающие прямоугольник мира [minX, maxX) x [minZ, maxZ), отсортированные по
     * расстоянию от (cx, cz) — ближние первыми. Пусто, если прямоугольник пуст или больше
     * {@code limit} тайлов (защита от бесконечной границы).
     */
    public static List<long[]> tilesIn(
            double minX, double minZ, double maxX, double maxZ, int level, double cx, double cz, int limit) {
        List<long[]> out = new ArrayList<>();
        if (!(maxX > minX) || !(maxZ > minZ)) return out;
        int x0 = tileOf(minX, level), x1 = tileOf(Math.nextDown(maxX), level);
        int z0 = tileOf(minZ, level), z1 = tileOf(Math.nextDown(maxZ), level);
        long n = ((long) x1 - x0 + 1) * ((long) z1 - z0 + 1);
        if (n <= 0 || n > limit) return out;
        double half = span(level) / 2.0;
        for (int tx = x0; tx <= x1; tx++) {
            for (int tz = z0; tz <= z1; tz++) {
                double dx = tileMin(tx, level) + half - cx;
                double dz = tileMin(tz, level) + half - cz;
                out.add(new long[] {tx, tz, (long) (dx * dx + dz * dz)});
            }
        }
        out.sort((a, b) -> Long.compare(a[2], b[2]));
        return out;
    }

    /**
     * Уменьшает тайл {@code src} (ARGB, TILE x TILE) в {@code factor} раз и пишет в квадрант
     * (qx, qz) тайла {@code dst} (размер квадранта TILE/factor). Прозрачные пиксели не участвуют в
     * среднем; если все прозрачные — результат прозрачный. {@code src == null} — квадрант
     * очищается.
     */
    public static void downsampleInto(int[] src, int[] dst, int qx, int qz, int factor) {
        int q = TILE / factor;
        int ox = qx * q, oz = qz * q;
        for (int z = 0; z < q; z++) {
            for (int x = 0; x < q; x++) {
                int di = (oz + z) * TILE + ox + x;
                if (src == null) {
                    dst[di] = 0;
                    continue;
                }
                long r = 0, g = 0, b = 0, a = 0;
                int n = 0;
                for (int sz = 0; sz < factor; sz++) {
                    int row = (z * factor + sz) * TILE + x * factor;
                    for (int sx = 0; sx < factor; sx++) {
                        int c = src[row + sx];
                        int ca = c >>> 24;
                        if (ca == 0) continue;
                        a += ca;
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        n++;
                    }
                }
                if (n == 0) {
                    dst[di] = 0;
                } else {
                    // альфа — по доле покрытых пикселей (край известной области мягкий)
                    int ra = (int) (a / (factor * factor));
                    dst[di] = (ra << 24) | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
                }
            }
        }
    }
}

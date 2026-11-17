package com.pigeostudios.pwp.warfare.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Обзор всей играбельной зоны одним изображением (грубый мип для «пока грузится») и порядок
 * пререндера тайлов. Обзор выровнен по сетке тайлов уровня 0: левый верхний угол — {@code
 * tileMin(tx0)}, 1 px = 2^level блоков.
 */
public final class MapOverview {
    private MapOverview() {}

    /** Максимальная сторона обзора в пикселях (512 — 1 ответ DataResponse с запасом). */
    public static final int MAX_SIDE = 512;
    /** Самый грубый уровень (1 px = 256 блоков, один пиксель на тайл). */
    public static final int MAX_LEVEL = 8;

    /** Уровень обзора: сторона {@code tiles * (256 >> level)} не больше {@code maxSide}. */
    public static int levelFor(int tilesW, int tilesH, int maxSide) {
        int level = 0;
        while (level < MAX_LEVEL && (long) Math.max(tilesW, tilesH) * (MapTileMath.TILE >> level) > maxSide) level++;
        return level;
    }

    /** Пикселей обзора на один тайл уровня 0. */
    public static int tilePx(int level) {
        return MapTileMath.TILE >> level;
    }

    /**
     * Уменьшенная копия тайла 256x256 в обзор шириной {@code dstW} на позицию (col,row) в тайлах.
     * {@code src == null} — пиксели прозрачные. Альфа — доля покрытых пикселей (как мипы).
     */
    public static void blit(int[] src, int[] dst, int dstW, int col, int row, int level) {
        int f = 1 << level, q = tilePx(level);
        int ox = col * q, oz = row * q;
        for (int z = 0; z < q; z++) {
            for (int x = 0; x < q; x++) {
                int di = (oz + z) * dstW + ox + x;
                if (src == null) {
                    dst[di] = 0;
                    continue;
                }
                long r = 0, g = 0, b = 0, a = 0;
                int n = 0;
                for (int sz = 0; sz < f; sz++) {
                    int rowOff = (z * f + sz) * MapTileMath.TILE + x * f;
                    for (int sx = 0; sx < f; sx++) {
                        int c = src[rowOff + sx];
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
                    int ra = (int) (a / ((long) f * f));
                    dst[di] = (ra << 24) | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
                }
            }
        }
    }

    /**
     * Порядок пререндера: все тайлы прямоугольника [tx0..tx1]x[tz0..tz1] (включительно) по
     * расстоянию от центра тайла до ближайшей «горячей» точки {x,z} (центр карты, мейны, точки
     * захвата). Без точек — по расстоянию до центра прямоугольника. Результат — {tx, tz}.
     */
    public static List<int[]> order(int tx0, int tz0, int tx1, int tz1, List<double[]> hot) {
        List<double[]> pts = new ArrayList<>(hot);
        if (pts.isEmpty()) {
            double cx = (MapTileMath.tileMin(tx0, 0) + MapTileMath.tileMin(tx1 + 1, 0)) / 2.0;
            double cz = (MapTileMath.tileMin(tz0, 0) + MapTileMath.tileMin(tz1 + 1, 0)) / 2.0;
            pts.add(new double[] {cx, cz});
        }
        List<double[]> scored = new ArrayList<>();
        for (int tz = tz0; tz <= tz1; tz++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                double mx = MapTileMath.tileMin(tx, 0) + MapTileMath.TILE / 2.0;
                double mz = MapTileMath.tileMin(tz, 0) + MapTileMath.TILE / 2.0;
                double best = Double.MAX_VALUE;
                for (double[] p : pts) {
                    double dx = mx - p[0], dz = mz - p[1];
                    best = Math.min(best, dx * dx + dz * dz);
                }
                scored.add(new double[] {tx, tz, best});
            }
        }
        scored.sort(Comparator.comparingDouble(s -> s[2]));
        List<int[]> out = new ArrayList<>(scored.size());
        for (double[] s : scored) out.add(new int[] {(int) s[0], (int) s[1]});
        return out;
    }

    /**
     * Дисковый тайл не нужно перерисовывать при пререндере: он палитровый (новый формат) и не
     * старше region-файла, в котором лежит ({@code regionMtime < 0} — region-файла нет, в мире там
     * ничего не сохранено). Незагруженные чанки меняются только с записью region-файла; загруженные
     * клиент дорисовывает сам, а сервер перерисует тайл по TTL при запросе.
     */
    public static boolean cacheFresh(boolean indexed, long tileMtime, long regionMtime) {
        if (!indexed || tileMtime <= 0) return false;
        return regionMtime < 0 || tileMtime >= regionMtime;
    }

    /** Region-файл (32x32 чанка = 512 блоков), в котором целиком лежит тайл уровня 0. */
    public static int regionOf(int tile) {
        return Math.floorDiv(tile, 2);
    }

    /** Доля готового в процентах 0..100 (0 из 0 = 100: нечего грузить). */
    public static int percent(int ready, int total) {
        if (total <= 0) return 100;
        return (int) Math.max(0, Math.min(100, (ready * 100L) / total));
    }
}

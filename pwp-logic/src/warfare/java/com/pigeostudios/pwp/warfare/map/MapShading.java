package com.pigeostudios.pwp.warfare.map;

/**
 * Цвет пикселя карты из колонки мира: базовый цвет MapColor верхнего блока + отмывка рельефа
 * (hillshade, свет с северо-запада) + лёгкий тон по абсолютной высоте; вода темнеет с глубиной.
 *
 * <p>Одна и та же функция используется сервером (рендер тайла) и клиентом (дорисовка из
 * загруженных чанков), поэтому пиксели совпадают.
 */
public final class MapShading {
    private MapShading() {}

    /** Высота неизвестна (соседняя колонка вне данных) — берётся высота самой колонки. */
    public static final short NO_HEIGHT = Short.MIN_VALUE;
    /** Базовый цвет неизвестен (чанк не сгенерирован/не загружен) — пиксель прозрачный. */
    public static final int NO_COLOR = -1;

    /** Уровень моря: тон по высоте нулевой на этой высоте. */
    static final int SEA_LEVEL = 63;
    /** Сила отмывки на блок перепада. */
    static final float SLOPE_GAIN = 0.07F;

    static final float SHADE_MIN = 0.62F;
    static final float SHADE_MAX = 1.22F;
    /** Тон по высоте: ±8% на ±96 блоков от уровня моря. */
    static final float ALT_GAIN = 0.08F / 96F;
    /** Ступеней яркости на единицу множителя (1/32 ~ 3%: глазу незаметно, палитра <= 256). */
    static final int SHADE_STEPS = 32;
    /** Глубина, на которой вода максимально тёмная. */
    static final int DEPTH_MAX = 24;
    /** Цвет глубокой воды (RGB). */
    static final int DEEP_WATER = 0x10264A;

    /**
     * ARGB пикселя. {@code rgb} — базовый цвет (или {@link #NO_COLOR}); {@code h} — высота
     * поверхности; {@code hNorth}/{@code hWest} — высоты соседей (z-1 / x-1) или
     * {@link #NO_HEIGHT}; {@code depth} — глубина воды над дном (0 — не вода).
     */
    public static int pixel(int rgb, int h, int hNorth, int hWest, int depth) {
        if (rgb == NO_COLOR) return 0;
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        if (depth > 0) {
            // вода: от базового цвета к тёмно-синему по глубине (корень — мелководье заметнее)
            float t = (float) Math.sqrt(Math.min(depth, DEPTH_MAX) / (float) DEPTH_MAX);
            float k = 0.15F + 0.75F * t;
            r = lerp(r, (DEEP_WATER >> 16) & 0xFF, k);
            g = lerp(g, (DEEP_WATER >> 8) & 0xFF, k);
            b = lerp(b, DEEP_WATER & 0xFF, k);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        int hn = hNorth == NO_HEIGHT ? h : hNorth;
        int hw = hWest == NO_HEIGHT ? h : hWest;
        float slope = (h - hn) + (h - hw);
        float f = 1F + slope * SLOPE_GAIN;
        f = Math.max(SHADE_MIN, Math.min(SHADE_MAX, f));
        f *= 1F + (h - SEA_LEVEL) * ALT_GAIN;
        // ступени яркости: цветов в тайле = MapColor x уровни — влезает в палитровый PNG
        f = Math.round(f * SHADE_STEPS) / (float) SHADE_STEPS;
        r = clamp(Math.round(r * f));
        g = clamp(Math.round(g * f));
        b = clamp(Math.round(b * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Рендер прямоугольника колонок шириной {@code w}: массивы {@code rgb}/{@code height}/
     * {@code depth} длиной w*h (строки по z), результат в {@code out} (ARGB).
     */
    public static void render(int[] rgb, short[] height, byte[] depth, int w, int h, int[] out) {
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                int i = z * w + x;
                int hn = z > 0 && rgb[i - w] != NO_COLOR ? height[i - w] : NO_HEIGHT;
                int hw = x > 0 && rgb[i - 1] != NO_COLOR ? height[i - 1] : NO_HEIGHT;
                out[i] = pixel(rgb[i], height[i], hn, hw, depth[i] & 0xFF);
            }
        }
    }

    private static int lerp(int a, int b, float t) {
        return clamp(Math.round(a + (b - a) * t));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }
}

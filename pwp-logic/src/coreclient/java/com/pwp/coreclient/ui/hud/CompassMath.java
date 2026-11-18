package com.pwp.coreclient.ui.hud;

/**
 * Математика компаса HUD (этап 3 редизайна HUD): азимут из yaw Minecraft, направление на цель,
 * положение на ленте, мягкий фейд краёв и кэш строк градусов. Чистая логика без Minecraft — БЕЗ
 * аллокаций в покадровых методах.
 *
 * <p>Азимут — как у настоящего компаса и карты: 0 = север (−Z), 90 = восток (+X), 180 = юг (+Z),
 * 270 = запад (−X). Yaw Minecraft: 0 = юг, 90 = запад, поэтому азимут = yaw + 180.
 */
public final class CompassMath {
    private CompassMath() {}

    /** Шаг подписанных делений ленты, градусы. */
    public static final int MAJOR_STEP = 15;
    /** Шаг мелких делений, градусы. */
    public static final int MINOR_STEP = 5;
    /** Шаг сторон света (N, NE, E…), градусы. */
    public static final int CARDINAL_STEP = 45;
    /** Сторон света (N, NE, E, SE, S, SW, W, NW — индекс = азимут / 45). */
    public static final int CARDINALS = 8;
    /** Доля полуширины ленты, с которой начинается фейд к краю. */
    public static final float FADE_START = 0.55F;

    /** Строки 0..359 (подписи делений) — создаются один раз. */
    private static final String[] DEGREES = new String[360];
    /** Строки 000..359 (показание азимута — постоянная ширина). */
    private static final String[] BEARINGS = new String[360];

    static {
        for (int i = 0; i < 360; i++) {
            DEGREES[i] = Integer.toString(i);
            BEARINGS[i] = (i < 10 ? "00" : i < 100 ? "0" : "") + i;
        }
    }

    /** Угол в [0, 360). */
    public static float wrap360(float deg) {
        if (Float.isNaN(deg) || Float.isInfinite(deg)) return 0F;
        float r = deg % 360F;
        if (r < 0F) r += 360F;
        return r >= 360F ? 0F : r;
    }

    /** Целый угол в [0, 360). */
    public static int wrap360(int deg) {
        int r = deg % 360;
        return r < 0 ? r + 360 : r;
    }

    /** Азимут взгляда по yaw Minecraft (с partialTick — плавно). */
    public static float bearing(float yaw) {
        return wrap360(yaw + 180F);
    }

    /** Азимут направления на цель по смещению {@code dx, dz} (мир); 0 при нулевом смещении. */
    public static float bearingTo(double dx, double dz) {
        if (dx == 0.0 && dz == 0.0) return 0F;
        return wrap360((float) Math.toDegrees(Math.atan2(dx, -dz)));
    }

    /** Кратчайшая разница {@code target − heading} в [−180, 180). */
    public static float delta(float target, float heading) {
        float d = wrap360(target - heading);
        return d >= 180F ? d - 360F : d;
    }

    /**
     * Смещение по X от центра ленты для угла {@code delta} (градусы от центра) при ширине ленты
     * {@code width} и видимом секторе {@code range} градусов.
     */
    public static float offset(float delta, float width, float range) {
        if (range <= 0F) return 0F;
        return delta * width / range;
    }

    /**
     * Прозрачность у края ленты: 1 в середине, плавно (smoothstep) к 0 на краю. {@code frac} —
     * |смещение| / полуширина (0 — центр, 1 — край).
     */
    public static float edgeFade(float frac) {
        float f = Math.abs(frac);
        if (Float.isNaN(f) || f >= 1F) return 0F;
        if (f <= FADE_START) return 1F;
        float t = (f - FADE_START) / (1F - FADE_START);
        return 1F - t * t * (3F - 2F * t);
    }

    /**
     * Первое деление с шагом {@code step} не левее {@code heading − halfRange} (может быть
     * отрицательным — нормализуйте {@link #wrap360(int)} при подписи). Цикл кадра: {@code for (d =
     * firstTick(...); d <= heading + halfRange; d += step)}.
     */
    public static int firstTick(float heading, float halfRange, int step) {
        if (step <= 0) return 0;
        return (int) Math.ceil((heading - halfRange) / step) * step;
    }

    /** Индекс стороны света 0..7 (N, NE, E, SE, S, SW, W, NW) или −1, если угол не кратен 45. */
    public static int cardinalIndex(int deg) {
        int d = wrap360(deg);
        return d % CARDINAL_STEP == 0 ? d / CARDINAL_STEP : -1;
    }

    /** Вид деления: 2 — подписанное (15°), 1 — мелкое (5°), 0 — без деления. */
    public static int tickKind(int deg) {
        int d = wrap360(deg);
        if (d % MAJOR_STEP == 0) return 2;
        return d % MINOR_STEP == 0 ? 1 : 0;
    }

    /** Подпись деления «15», «30»… — из кэша, без аллокации. */
    public static String degreeText(int deg) {
        return DEGREES[wrap360(deg)];
    }

    /** Показание азимута «005», «275» — из кэша, без аллокации. */
    public static String bearingText(float bearing) {
        int b = Math.round(wrap360(bearing));
        return BEARINGS[b >= 360 ? 0 : b];
    }
}

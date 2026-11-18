package com.pwp.coreclient.ui.hud;

/**
 * Геометрия сегментного кольца HUD у прицела (этап 5: кольцо канала дрона, кольцо HP цели):
 * сколько сегментов залито, доля ведущего сегмента, длина штриха по дуге, угол лопастей дрона.
 * Чистая логика без Minecraft, без аллокаций. Углы — радианы, 0 = «12 часов», по часовой стрелке
 * (экранная ось Y вниз).
 */
public final class RingMath {
    private RingMath() {}

    /** Полный оборот, радианы. */
    public static final double TAU = Math.PI * 2.0;

    /** Доля 0..1 (NaN/мусор — 0). */
    public static float clampFrac(float frac) {
        if (Float.isNaN(frac) || frac <= 0F) return 0F;
        return Math.min(1F, frac);
    }

    /** Сколько сегментов залито целиком при доле {@code frac} (0..segments). */
    public static int filled(float frac, int segments) {
        if (segments <= 0) return 0;
        // 1e-4 — запас на float: 0.75 * 4 не должно стать 2.9999
        return Math.min(segments, (int) Math.floor(clampFrac(frac) * segments + 1e-4));
    }

    /** Доля ведущего (частично залитого) сегмента 0..1; 0 — ведущего нет. */
    public static float partial(float frac, int segments) {
        if (segments <= 0) return 0F;
        float f = clampFrac(frac) * segments - filled(frac, segments);
        return f < 1e-3F ? 0F : Math.min(1F, f);
    }

    /** Шаг между сегментами, радианы. */
    public static float step(int segments) {
        return segments <= 0 ? 0F : (float) (TAU / segments);
    }

    /**
     * Длина штриха сегмента по дуге радиуса {@code radius}: длина дуги сегмента минус зазор, не
     * меньше 1.
     */
    public static int dash(int radius, int segments, int gap) {
        if (segments <= 0 || radius <= 0) return 1;
        double arc = TAU * radius / segments;
        return Math.max(1, (int) Math.round(arc - Math.max(0, gap)));
    }

    /**
     * Угол лопастей дрона (радианы, 0..TAU) по непрерывному времени {@code time} в тиках (тики мира
     * + доля кадра): оборот за {@code periodTicks}.
     */
    public static float spin(float time, float periodTicks) {
        if (periodTicks <= 0F || Float.isNaN(time) || Float.isInfinite(time)) return 0F;
        float t = time % periodTicks;
        if (t < 0F) t += periodTicks;
        return (float) (t / periodTicks * TAU);
    }

    /** Сколько секунд осталось при доле {@code frac} из {@code totalTicks} (вверх, не меньше 0). */
    public static int secondsLeft(float frac, int totalTicks) {
        int total = Math.max(1, totalTicks);
        return Math.max(0, (int) Math.ceil((1F - clampFrac(frac)) * total / 20F));
    }
}

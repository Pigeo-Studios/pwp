package com.pwp.coreclient.ui;

import java.util.function.LongSupplier;

/**
 * Экспоненциальное «догоняние» значения (сглаживание HUD) в РЕАЛЬНОМ времени.
 *
 * <p>Замена покадровых {@code x += (target - x) * k}: при 60 FPS даёт ровно ту же кривую, что и
 * старый код с коэффициентом {@code k} на кадр, но не ускоряется при 144 FPS и не тормозит при 30.
 * Формула: доля пройденного за {@code dt} мс = {@code 1 - (1 - k)^(dt / 16.667)}.
 *
 * <p>{@link Frame} — часы одного элемента HUD: {@code dtMs()} раз за кадр отдаёт время с прошлого
 * кадра, обрезанное до {@link #MAX_DT_MS} (после паузы/свёрнутого окна анимация не «прыгает» в
 * конец, а продолжается). Чистая логика — покрыта тестами (часы подменяются).
 */
public final class UIDamp {
    /** Длительность кадра при 60 FPS — эталон, под который подбирались старые коэффициенты. */
    public static final float FRAME_60_MS = 1000F / 60F;

    /** Максимальный шаг за кадр (мс). */
    public static final long MAX_DT_MS = 100L;

    private UIDamp() {}

    /**
     * Сдвиг {@code current} к {@code target}.
     *
     * @param ratePer60Fps доля пути за один кадр при 60 FPS (прежний покадровый коэффициент), 0..1
     * @param dtMs время с прошлого кадра, мс
     */
    public static float approach(float current, float target, float ratePer60Fps, long dtMs) {
        if (dtMs <= 0 || ratePer60Fps <= 0F) return current;
        if (ratePer60Fps >= 1F) return target;
        double keep = Math.pow(1.0 - ratePer60Fps, dtMs / (double) FRAME_60_MS);
        return (float) (target + (current - target) * keep);
    }

    /** Часы кадров одного элемента HUD. */
    public static final class Frame {
        private final LongSupplier clock;
        private long lastMs = Long.MIN_VALUE;

        public Frame() {
            this(UIAnim.REAL_TIME);
        }

        public Frame(LongSupplier clock) {
            this.clock = clock;
        }

        /** Время с прошлого вызова (мс), 0..{@link #MAX_DT_MS}; первый вызов — 0. */
        public long dtMs() {
            long now = clock.getAsLong();
            long dt = lastMs == Long.MIN_VALUE ? 0L : now - lastMs;
            lastMs = now;
            return Math.max(0L, Math.min(MAX_DT_MS, dt));
        }
    }
}

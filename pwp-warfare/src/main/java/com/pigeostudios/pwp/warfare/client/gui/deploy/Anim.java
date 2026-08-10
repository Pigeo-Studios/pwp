package com.pigeostudios.pwp.warfare.client.gui.deploy;

import net.minecraft.util.Mth;

/**
 * Мини-хелперы плавности для деплоя: ease-out кривые и состояние раскрытия панели.
 * Все анимации идут по реальному времени (System.currentTimeMillis) — плавность
 * не зависит от FPS, а при низком FPS не «телепортируется».
 */
public final class Anim {

    private Anim() {}

    /** Ease-out cubic: быстрое начало, мягкое завершение (стандарт для UI-раскрытий). */
    public static float easeOutCubic(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return 1f - (float) Math.pow(1f - t, 3);
    }

    /** Ease-in-out cubic: плавный старт и финиш (для кросс-фейдов). */
    public static float easeInOutCubic(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    /**
     * Состояние раскрытия панели: ведёт progress от 0 до 1 (и обратно) по времени.
     * Кадр держит направление, пока {@code open} не поменялось — плавная анимация
     * высоты/альфы без дёрганий при каждом кадре.
     */
    public static final class ExpandState {
        private long start = -1;
        private boolean dir;

        public float progress(boolean open, long now, long durationMs) {
            if (start < 0 || open != dir) {
                start = now;
                dir = open;
            }
            float t = (now - start) / (float) Math.max(1, durationMs);
            t = Mth.clamp(t, 0f, 1f);
            return dir ? easeOutCubic(t) : 1f - easeOutCubic(t);
        }

        /** Фактическая видимость: >0 — панель хоть как-то видна. */
        public boolean visible(boolean open, long now, long durationMs) {
            return progress(open, now, durationMs) > 0.01f;
        }

        public void reset() {
            start = -1;
        }
    }
}

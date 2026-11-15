package com.pwp.coreclient.ui;

import java.util.function.LongSupplier;

/**
 * Анимация значения UI-кита (фейд, слайд, hover) с easing. Время — РЕАЛЬНОЕ (мс), не тики:
 * анимация одинаково плавная при любом FPS и не «стоит», когда игра на паузе/тик просел.
 *
 * <p>Использование: {@code hover.to(hovered ? 1 : 0)} каждый кадр, затем {@code hover.value()}.
 * Смена цели посреди анимации продолжает её с текущего значения (без скачка).
 * Чистая логика — покрыта тестами (часы подменяются).
 */
public final class UIAnim {
    /** Функция сглаживания 0..1 → 0..1. */
    @FunctionalInterface
    public interface Easing {
        float apply(float t);
    }

    public static final Easing LINEAR = t -> t;
    public static final Easing EASE_OUT_CUBIC = t -> 1 - (float) Math.pow(1 - t, 3);
    public static final Easing EASE_IN_OUT_CUBIC =
            t -> t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;

    /** Системные часы UI (мс). */
    public static final LongSupplier REAL_TIME = () -> System.nanoTime() / 1_000_000L;

    private final long durationMs;
    private final Easing easing;
    private final LongSupplier clock;
    private float from;
    private float target;
    private long startMs;

    public UIAnim(float initial, long durationMs, Easing easing) {
        this(initial, durationMs, easing, REAL_TIME);
    }

    public UIAnim(float initial, long durationMs, Easing easing, LongSupplier clock) {
        this.from = initial;
        this.target = initial;
        this.durationMs = Math.max(1, durationMs);
        this.easing = easing;
        this.clock = clock;
        this.startMs = clock.getAsLong() - this.durationMs;
    }

    /** Новая цель; если цель та же — ничего не меняется. */
    public UIAnim to(float newTarget) {
        if (newTarget == target) return this;
        from = value();
        target = newTarget;
        startMs = clock.getAsLong();
        return this;
    }

    /** Мгновенно поставить значение без анимации. */
    public void snap(float value) {
        from = value;
        target = value;
        startMs = clock.getAsLong() - durationMs;
    }

    /** Текущее значение с учётом easing. */
    public float value() {
        float t = progress();
        return from + (target - from) * easing.apply(t);
    }

    /** Линейный прогресс 0..1 текущего перехода. */
    public float progress() {
        long elapsed = clock.getAsLong() - startMs;
        return Math.max(0F, Math.min(1F, (float) elapsed / durationMs));
    }

    public boolean done() {
        return progress() >= 1F;
    }

    public float target() {
        return target;
    }
}

package com.pwp.coreclient.ui.hud;

import com.pwp.coreclient.ui.UIAnim;
import com.pwp.coreclient.ui.UIDamp;
import java.util.function.LongSupplier;

/**
 * Полоса HP с «отложенным» уроном (трейл, как в Squad/HLL): при уроне заливка быстро падает к
 * новому значению, а светлый хвост держится на старом {@link #HOLD_MS} и потом плавно стекает. При
 * лечении заливка растёт мягче, хвоста нет. Всё в долях 0..1 и в РЕАЛЬНОМ времени ({@link
 * UIDamp}); при выключенных анимациях — сразу в цели. Без аллокаций в кадре.
 */
public final class DamageTrail {
    /** Сколько хвост стоит на месте после последнего урона. */
    public static final long HOLD_MS = 450L;
    /** Скорость падения заливки при уроне (доля за кадр при 60 FPS). */
    public static final float DROP_RATE = 0.35F;
    /** Скорость роста при лечении. */
    public static final float HEAL_RATE = 0.15F;
    /** Скорость стекания хвоста после паузы. */
    public static final float TRAIL_RATE = 0.08F;

    private static final float EPS = 0.001F;

    private final UIDamp.Frame frame;
    private final LongSupplier clock;
    private float value;
    private float trail;
    private float lastTarget;
    private long holdUntil = Long.MIN_VALUE;

    public DamageTrail(float initial) {
        this(initial, UIAnim.REAL_TIME);
    }

    public DamageTrail(float initial, LongSupplier clock) {
        this.clock = clock;
        this.frame = new UIDamp.Frame(clock);
        snap(initial);
    }

    /** Шаг кадра к доле {@code target} (0..1, NaN = 0). */
    public void update(float target) {
        float t = clamp(target);
        long dt = frame.dtMs();
        long now = clock.getAsLong();
        if (!UIAnim.animationsEnabled()) {
            value = t;
            trail = t;
            lastTarget = t;
            return;
        }
        if (t < lastTarget - EPS) {
            // новый урон: хвост остаётся на верхней границе, пауза отсчитывается заново
            trail = Math.max(trail, value);
            holdUntil = now + HOLD_MS;
        }
        lastTarget = t;
        value = UIDamp.approach(value, t, t < value ? DROP_RATE : HEAL_RATE, dt);
        if (Math.abs(value - t) < EPS) value = t;
        if (trail <= value) {
            trail = value;
        } else if (now >= holdUntil) {
            trail = UIDamp.approach(trail, value, TRAIL_RATE, dt);
            if (trail - value < EPS) trail = value;
        }
    }

    /** Мгновенно поставить значение без хвоста (респавн, первый кадр). */
    public void snap(float v) {
        float t = clamp(v);
        value = t;
        trail = t;
        lastTarget = t;
        holdUntil = Long.MIN_VALUE;
    }

    /** Текущая заливка 0..1. */
    public float value() {
        return value;
    }

    /** Верх хвоста 0..1 (≥ {@link #value()}); хвост рисуется между value и trail. */
    public float trail() {
        return trail;
    }

    private static float clamp(float v) {
        if (Float.isNaN(v)) return 0F;
        return Math.max(0F, Math.min(1F, v));
    }
}

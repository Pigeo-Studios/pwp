package com.pigeostudios.pwp.movement;

/**
 * Подавление, как в Squad (этап 17g): чужие пули рядом копят подавление 0..1, оно держится секунду и спадает.
 * Эффект на клиенте — затемнение краёв экрана ({@link #vignette}) и лёгкая раскачка прицела ({@link #sway}).
 * Шаг — тик (1/20 с). Чистое правило: что считать «пулей рядом», решает клиент.
 */
public final class Suppression {
    /** Одна пуля рядом. */
    public static final float PER_BULLET = 0.18F;
    /** Сколько тиков держится после последней пули. */
    public static final int HOLD_TICKS = 20;
    /** Спад за тик после удержания (полное — ~2.5 с). */
    public static final float DECAY = 1F / 50F;

    public static final float MAX_VIGNETTE = 0.65F;
    public static final float MAX_SWAY_DEG = 1.2F;

    private float value;
    private int hold;

    public void bullet() {
        value = Math.min(1F, value + PER_BULLET);
        hold = HOLD_TICKS;
    }

    public void tick() {
        if (hold > 0) {
            hold--;
            return;
        }
        value = Math.max(0F, value - DECAY);
    }

    public float value() {
        return value;
    }

    /** Непрозрачность затемнения краёв 0..{@link #MAX_VIGNETTE}. */
    public float vignette() {
        return MAX_VIGNETTE * value;
    }

    /** Амплитуда раскачки прицела, градусы. */
    public float sway() {
        return MAX_SWAY_DEG * value * value;
    }

    public void reset() {
        value = 0F;
        hold = 0;
    }
}

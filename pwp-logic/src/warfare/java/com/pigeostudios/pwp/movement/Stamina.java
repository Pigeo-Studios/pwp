package com.pigeostudios.pwp.movement;

/**
 * Выносливость (этап 15 ROADMAP), как в Squad: бег тратит, шаг и стоянка восстанавливают; на нуле бежать
 * нельзя, пока не восстановится {@link #RESUME}. Чем меньше выносливости, тем сильнее раскачка прицела
 * ({@link #sway}) — после рывка стрелять точно не получится. Значение 0..1, шаг — тик (1/20 с).
 */
public final class Stamina {
    /** Полный бег с полной выносливости — около 18 с. */
    public static final float SPRINT_COST = 1F / (20 * 18);
    /** Восстановление стоя/шагом — полное за ~12 с, в приседе быстрее. */
    public static final float REGEN = 1F / (20 * 12);

    public static final float REGEN_CROUCH = REGEN * 1.5F;
    /** Прыжок отнимает рывком. */
    public static final float JUMP_COST = 0.06F;
    /** После полного истощения бег снова разрешён с этого уровня. */
    public static final float RESUME = 0.25F;
    /** Раскачка прицела (градусы амплитуды) на нуле выносливости. */
    public static final float MAX_SWAY_DEG = 1.6F;

    private float value = 1F;
    private boolean exhausted;

    public float value() {
        return value;
    }

    public boolean canSprint() {
        return !exhausted && value > 0F;
    }

    /** Тик: бежит ли, присел ли, прыгнул ли в этот тик. */
    public void tick(boolean sprinting, boolean crouching, boolean jumped) {
        if (sprinting && canSprint()) value -= SPRINT_COST;
        else value += crouching ? REGEN_CROUCH : REGEN;
        if (jumped) value -= JUMP_COST;
        value = Math.max(0F, Math.min(1F, value));
        if (value <= 0F) exhausted = true;
        if (exhausted && value >= RESUME) exhausted = false;
    }

    /** Амплитуда раскачки прицела, градусы: 0 при ≥70% выносливости, растёт к нулю. */
    public float sway() {
        float k = Math.max(0F, (0.7F - value) / 0.7F);
        return MAX_SWAY_DEG * k * k;
    }

    public void reset() {
        value = 1F;
        exhausted = false;
    }
}

package com.pigeostudios.pwp.warfare.match;

/**
 * Чистые правила боя, у которых раньше не было логики вовсе: залпы артиллерии командира и
 * подъём раненого. Побочные эффекты (взрывы, сообщения, предметы) — у вызывающего.
 */
public final class CombatRules {
    private CombatRules() {}

    // ------------------------------------------------------------ артиллерия (как в Squad)
    /** Длительность удара целиком (тиков) — совпадает с {@code ActiveStrike.ticksLeft} по умолчанию. */
    public static final int STRIKE_TOTAL_TICKS = 400;
    /** Подлёт: от подтверждения до первого разрыва. */
    public static final int STRIKE_DELAY_TICKS = 100;
    /** Интервал между снарядами. */
    public static final int STRIKE_INTERVAL_TICKS = 10;

    /** Падает ли снаряд в этот тик удара. {@code ticksLeft} — сколько осталось ДО уменьшения. */
    public static boolean shellFires(int ticksLeft) {
        int elapsed = STRIKE_TOTAL_TICKS - ticksLeft;
        if (elapsed < STRIKE_DELAY_TICKS || ticksLeft <= 0) return false;
        return (elapsed - STRIKE_DELAY_TICKS) % STRIKE_INTERVAL_TICKS == 0;
    }

    /** Сколько снарядов даёт удар целиком (для подсказок/тестов). */
    public static int shellsPerStrike() {
        int n = 0;
        for (int left = STRIKE_TOTAL_TICKS; left > 0; left--) if (shellFires(left)) n++;
        return n;
    }

    /**
     * Смещение снаряда от центра: равномерно по кругу радиуса {@code radius}
     * ({@code u1, u2} — случайные числа из [0,1)). Возвращает {dx, dz}.
     */
    public static double[] shellOffset(double u1, double u2, double radius) {
        double r = radius * Math.sqrt(Math.max(0, Math.min(1, u1)));
        double a = 2 * Math.PI * u2;
        return new double[] {r * Math.cos(a), r * Math.sin(a)};
    }

    // ------------------------------------------------------------ подъём из нока
    /** Медик может поднять дальше этого расстояния (блоков) — канал рвётся. */
    public static final double REVIVE_MAX_DISTANCE = 3.0;

    /** Поднимать раненых может только медик (как в Squad); креатив — для тестов. */
    public static boolean canRevive(String appliedKit, boolean creative) {
        if (creative) return true;
        return appliedKit != null && appliedKit.trim().toLowerCase().startsWith("medic");
    }

    public enum ReviveStep {
        CONTINUE,
        DONE,
        CANCEL_DISTANCE,
        CANCEL_STATE
    }

    /**
     * Шаг канала подъёма. {@code stateOk} — раненый всё ещё в ноке, медик жив, не в ноке и держит
     * предмет подъёма.
     */
    public static ReviveStep reviveStep(long elapsedTicks, long requiredTicks, double distanceSq, boolean stateOk) {
        if (!stateOk) return ReviveStep.CANCEL_STATE;
        if (distanceSq > REVIVE_MAX_DISTANCE * REVIVE_MAX_DISTANCE) return ReviveStep.CANCEL_DISTANCE;
        return elapsedTicks >= requiredTicks ? ReviveStep.DONE : ReviveStep.CONTINUE;
    }

    /** Прогресс канала 0..100. */
    public static int revivePercent(long elapsedTicks, long requiredTicks) {
        if (requiredTicks <= 0) return 100;
        return (int) Math.max(0, Math.min(100, elapsedTicks * 100 / requiredTicks));
    }
}

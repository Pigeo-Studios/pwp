package com.pigeostudios.pwp.warfare.match;

/**
 * Лестница наказаний за тимкилл внутри матча (этап 10 ROADMAP). Прежнее поведение сохранено: 1+ —
 * предупреждение, {@code restrictAt}+ (было зашито 5) — 5 минут без выхода из мейна, {@code topAt}
 * ({@code teamkillSpectatorThreshold}) — наблюдатель. Новое: при {@code banMinutes > 0} на верхней ступени —
 * временный бан вместо наблюдателя (наблюдатель всё равно не может убивать, а сидеть им весь матч — не выход),
 * а на ступени ограничения — жалоба модераторам.
 */
public final class TeamkillLadder {
    public enum Step {
        WARN,
        RESTRICT,
        SPECTATE,
        BAN
    }

    public static final int RESTRICT_AT = 5;

    private TeamkillLadder() {}

    public static Step step(int teamKills, int topAt, int banMinutes) {
        int top = Math.max(RESTRICT_AT + 1, topAt);
        if (teamKills >= top) return banMinutes > 0 ? Step.BAN : Step.SPECTATE;
        if (teamKills >= RESTRICT_AT) return Step.RESTRICT;
        return Step.WARN;
    }

    /** Жалобу модераторам шлём один раз — при первом достижении ступени ограничения. */
    public static boolean autoReport(int teamKills) {
        return teamKills == RESTRICT_AT;
    }
}

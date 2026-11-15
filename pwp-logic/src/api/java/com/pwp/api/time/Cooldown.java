package com.pwp.api.time;

/**
 * Чистая логика кулдауна по абсолютной метке «до»: каждый старт ПЕРЕЗАПИСЫВАЕТ метку
 * ({@code now + duration}), а не прибавляет к ней — повторный старт не «копит» время.
 * Время передаётся снаружи (обычно {@link GameClock#worldTicks}, если метка сохраняется).
 */
public final class Cooldown {
    private Cooldown() {}

    /** Новая метка окончания кулдауна. */
    public static long start(long now, long duration) {
        return now + Math.max(0, duration);
    }

    /** true — кулдаун ещё идёт. */
    public static boolean active(long until, long now) {
        return until > now;
    }

    /** Сколько осталось (0, если кулдауна нет). */
    public static long remaining(long until, long now) {
        return Math.max(0, until - now);
    }

    /**
     * Метка, пережившая смену часов (например, сохранена от счётчика, который потом сбросился):
     * если осталось больше максимально возможного кулдауна — она битая, кулдаун снимается.
     */
    public static long sanitize(long until, long now, long maxDuration) {
        return until - now > maxDuration ? now : until;
    }

    /** Прошло ли {@code interval} с момента {@code last} (last &lt;= 0 — «никогда»). */
    public static boolean elapsed(long last, long now, long interval) {
        return last <= 0 || now - last >= interval;
    }
}

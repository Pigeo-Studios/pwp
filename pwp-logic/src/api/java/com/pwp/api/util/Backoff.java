package com.pwp.api.util;

/**
 * Экспоненциальная пауза между повторами: {@code base · 2^(attempt-1)}, не больше {@code max}. Единицы —
 * любые (тики, мс). Используется там, где внешний сервис недоступен долго (core-service на тест-стенде):
 * повтор каждые 10 с вечно спамил лог и нагружал сеть впустую.
 */
public final class Backoff {
    private Backoff() {}

    /**
     * Пауза перед следующей попыткой после {@code attempt} неудачных подряд (1 — первая неудача).
     * attempt &lt;= 0 — пауза {@code base}. Переполнения нет: степень ограничена.
     */
    public static long delay(int attempt, long base, long max) {
        if (base <= 0) return 0;
        long cap = Math.max(base, max);
        int shift = Math.max(0, Math.min(attempt - 1, 30));
        long d = base << shift;
        return d <= 0 || d > cap ? cap : d;
    }

    /**
     * Логировать ли неудачу громко (warn/error): первая и каждая степень двойки (1, 2, 4, 8…) — остальные
     * пишутся на уровне debug.
     */
    public static boolean loud(int attempt) {
        return attempt >= 1 && (attempt & (attempt - 1)) == 0;
    }
}

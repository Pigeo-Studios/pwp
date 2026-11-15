package com.pwp.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Ограничение частоты по ключу (обычно «игрок + тип события»): {@link #tryAcquire} разрешает не
 * чаще раза в {@code intervalMs}. Нужен для логов отказов клиентского ввода — злой клиент не
 * должен забивать лог сервера. Чистая логика без Forge, потокобезопасен.
 */
public final class RateLimiter {
    /** При превышении карта чистится от записей старше интервала. */
    static final int MAX_KEYS = 4096;

    private final long intervalMs;
    private final LongSupplier clockMs;
    private final Map<String, Long> last = new HashMap<>();

    public RateLimiter(long intervalMs) {
        this(intervalMs, System::currentTimeMillis);
    }

    public RateLimiter(long intervalMs, LongSupplier clockMs) {
        this.intervalMs = intervalMs;
        this.clockMs = clockMs;
    }

    /** true — событие по ключу можно обработать (логировать) сейчас. */
    public synchronized boolean tryAcquire(String key) {
        long now = clockMs.getAsLong();
        Long prev = last.get(key);
        if (prev != null && now - prev < intervalMs) return false;
        if (last.size() >= MAX_KEYS) last.values().removeIf(t -> now - t >= intervalMs);
        if (last.size() >= MAX_KEYS) last.clear();
        last.put(key, now);
        return true;
    }

    public long intervalMs() {
        return intervalMs;
    }
}

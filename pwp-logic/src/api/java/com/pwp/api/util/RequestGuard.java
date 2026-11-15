package com.pwp.api.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Антиспам клиентских запросов, которые вызывают core-service.
 *
 * <p>Ключ — (игрок, тип запроса). Запрос разрешён, если по этому ключу нет незавершённого
 * запроса и с момента прошлого старта прошёл кулдаун. Чистая логика без Forge — покрыта тестами.
 * Потокобезопасен: release вызывается из потока HTTP-клиента.
 */
public final class RequestGuard {
    private final long cooldownMs;
    private final LongSupplier clockMs;
    private final Map<Key, Long> lastStart = new ConcurrentHashMap<>();
    private final Map<Key, Boolean> inFlight = new ConcurrentHashMap<>();

    public RequestGuard(long cooldownMs) {
        this(cooldownMs, System::currentTimeMillis);
    }

    public RequestGuard(long cooldownMs, LongSupplier clockMs) {
        this.cooldownMs = cooldownMs;
        this.clockMs = clockMs;
    }

    /** Пытается занять слот; true — запрос можно запускать (тогда обязателен {@link #release}). */
    public boolean tryAcquire(UUID player, String type) {
        Key key = new Key(player, type);
        long now = clockMs.getAsLong();
        synchronized (this) {
            if (inFlight.containsKey(key)) return false;
            Long last = lastStart.get(key);
            if (last != null && now - last < cooldownMs) return false;
            inFlight.put(key, Boolean.TRUE);
            lastStart.put(key, now);
            return true;
        }
    }

    /** Освобождает слот после завершения запроса. */
    public void release(UUID player, String type) {
        inFlight.remove(new Key(player, type));
    }

    /** Забыть игрока целиком (выход с сервера) — чтобы карты не росли. */
    public void forget(UUID player) {
        lastStart.keySet().removeIf(k -> k.player.equals(player));
        inFlight.keySet().removeIf(k -> k.player.equals(player));
    }

    public boolean isInFlight(UUID player, String type) {
        return inFlight.containsKey(new Key(player, type));
    }

    private record Key(UUID player, String type) {}
}

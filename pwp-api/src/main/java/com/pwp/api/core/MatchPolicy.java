package com.pwp.api.core;

import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Кэш политики запуска матчей (core-service {@code /network/match-policy}).
 *
 * <p>Чтение неблокирующее: возвращается последнее известное значение, а если оно
 * старше {@link #TTL_MS} — в фоне запускается обновление. Раньше обновление шло
 * синхронным HTTP прямо из серверного тика (рассылка состояния лобби каждую секунду).
 * Пока ответа не было ни разу — матчи разрешены (как и раньше при недоступном core-service).
 */
public final class MatchPolicy {
    static final long TTL_MS = 15_000;

    private static volatile long checkedAt = 0;
    private static volatile boolean canStart = true;
    private static volatile String reason = null;
    private static final AtomicBoolean refreshing = new AtomicBoolean(false);

    private MatchPolicy() {}

    public static boolean canStartNewMatch() {
        refreshIfStale();
        return canStart;
    }

    public static String reason() {
        refreshIfStale();
        return reason;
    }

    static void invalidate() {
        checkedAt = 0;
    }

    private static void refreshIfStale() {
        if (System.currentTimeMillis() - checkedAt < TTL_MS) return;
        if (!refreshing.compareAndSet(false, true)) return;
        CoreApi.getMatchPolicy().whenComplete((resp, error) -> {
            try {
                apply(CoreApi.dataObject(resp));
            } finally {
                checkedAt = System.currentTimeMillis();
                refreshing.set(false);
            }
        });
    }

    /** Применяет ответ; null (core-service недоступен) оставляет последнее известное значение. */
    static void apply(JsonObject data) {
        if (data == null) return;
        if (data.has("canStartNewMatch")) canStart = data.get("canStartNewMatch").getAsBoolean();
        reason = data.has("reason") && !data.get("reason").isJsonNull() ? data.get("reason").getAsString() : null;
    }
}

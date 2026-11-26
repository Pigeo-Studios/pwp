package com.pigeostudios.pwp.warfare.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Запросы на вождение машины специалиста (этап 17a): раньше запрос был только строкой в чате лидера и
 * терялся; теперь он ждёт ответа {@link #TTL_MS} и виден лидеру в радиалке техники. Ключи — строки (UUID машины
 * и игрока), время — мс вызывающего (настенное).
 */
public final class DriveRequests {
    public static final long TTL_MS = 60_000L;

    public record Request(String playerId, String playerName, long until) {}

    private final Map<String, LinkedHashMap<String, Request>> byVehicle = new HashMap<>();

    /** Добавить или продлить запрос игрока на машину. */
    public synchronized void add(String vehicleId, String playerId, String playerName, long nowMs) {
        byVehicle
                .computeIfAbsent(vehicleId, k -> new LinkedHashMap<>())
                .put(playerId, new Request(playerId, playerName, nowMs + TTL_MS));
    }

    /** Живые запросы на машину, старые первыми; просроченные выкидываются. */
    public synchronized List<Request> pending(String vehicleId, long nowMs) {
        LinkedHashMap<String, Request> m = byVehicle.get(vehicleId);
        if (m == null) return List.of();
        m.values().removeIf(r -> r.until() <= nowMs);
        if (m.isEmpty()) byVehicle.remove(vehicleId);
        return m.isEmpty() ? List.of() : new ArrayList<>(m.values());
    }

    /** Есть ли живой запрос именно этого игрока. */
    public synchronized boolean has(String vehicleId, String playerId, long nowMs) {
        return pending(vehicleId, nowMs).stream().anyMatch(r -> r.playerId().equals(playerId));
    }

    /** Ответ дан (или отозван) — убрать. */
    public synchronized void remove(String vehicleId, String playerId) {
        LinkedHashMap<String, Request> m = byVehicle.get(vehicleId);
        if (m == null) return;
        m.remove(playerId);
        if (m.isEmpty()) byVehicle.remove(vehicleId);
    }

    /** Игрок вышел — его запросы больше не нужны. */
    public synchronized void forgetPlayer(String playerId) {
        for (Iterator<LinkedHashMap<String, Request>> it = byVehicle.values().iterator(); it.hasNext(); ) {
            LinkedHashMap<String, Request> m = it.next();
            m.remove(playerId);
            if (m.isEmpty()) it.remove();
        }
    }

    public synchronized int vehicles() {
        return byVehicle.size();
    }
}

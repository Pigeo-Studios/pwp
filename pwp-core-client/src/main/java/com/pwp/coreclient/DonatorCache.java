package com.pwp.coreclient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Клиентский кэш донат-тиров: uuid игрока -> тир (SILVER/GOLD/PLATINUM), наполняется из PacketDonatorTiers. */
public final class DonatorCache {

    private static final Map<UUID, String> TIERS = new ConcurrentHashMap<>();

    private DonatorCache() {}

    /** Полная замена списка (сервер шлёт всех донатеров лобби). */
    public static void apply(String[] uuids, String[] tiers) {
        TIERS.clear();
        int n = Math.min(uuids.length, tiers.length);
        for (int i = 0; i < n; i++) {
            String t = tiers[i];
            if (t == null || t.isEmpty() || "NONE".equalsIgnoreCase(t)) continue;
            try {
                TIERS.put(UUID.fromString(uuids[i]), t.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /** Тип донатера для игрока или null, если не донатер. */
    public static String get(UUID uuid) {
        return TIERS.get(uuid);
    }

    public static boolean isDonator(UUID uuid) {
        return TIERS.containsKey(uuid);
    }

    public static void clear() {
        TIERS.clear();
    }
}

package com.pwp.coreclient;

import com.pwp.coreclient.donor.DonorLevel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Клиентский кэш донат-тиров и ролей: uuid игрока -> уровень (SILVER/GOLD/PLATINUM/ADMIN/MODERATOR), из PacketDonatorTiers. */
public final class DonatorCache {

    private static final Map<UUID, String> TIERS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> ROLES = new ConcurrentHashMap<>();

    private DonatorCache() {}

    /** Полная замена списка (сервер шлёт всех донатеров и ролей лобби). */
    public static void apply(String[] uuids, String[] tiers, String[] roles) {
        TIERS.clear();
        ROLES.clear();
        int n = Math.min(Math.min(uuids.length, tiers.length), roles.length);
        for (int i = 0; i < n; i++) {
            UUID id;
            try {
                id = UUID.fromString(uuids[i]);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            String t = tiers[i] == null ? "" : tiers[i];
            if (!t.isEmpty() && !"NONE".equalsIgnoreCase(t)) TIERS.put(id, t.toUpperCase());
            String r = DonorLevel.normalizeRole(roles[i]);
            if (r != null) ROLES.put(id, r);
        }
    }

    /** Донат-тир игрока или null. */
    public static String get(UUID uuid) {
        return TIERS.get(uuid);
    }

    /** Нормализованная роль (ADMIN/MODERATOR) или null. */
    public static String getRole(UUID uuid) {
        return ROLES.get(uuid);
    }

    /** Итоговый уровень для отображения: роль приоритетнее тира (ADMIN > MODERATOR > тир). */
    public static String levelOf(UUID uuid) {
        String r = ROLES.get(uuid);
        if (r != null) return r;
        return TIERS.get(uuid);
    }

    public static boolean isDonator(UUID uuid) {
        return TIERS.containsKey(uuid);
    }

    public static boolean hasLevel(UUID uuid) {
        return TIERS.containsKey(uuid) || ROLES.containsKey(uuid);
    }

    public static void clear() {
        TIERS.clear();
        ROLES.clear();
    }
}

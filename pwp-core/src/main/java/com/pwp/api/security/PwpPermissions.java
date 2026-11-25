package com.pwp.api.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

/**
 * Единая проверка прав на админ-действия (редакторы китов/техники/скинов, отладка, админ-панель).
 *
 * <p>Уровень = max(op сервера, роль из БД core-service) — см. {@link AccessLevel}. Роль из БД
 * подгружается асинхронно при входе (pwp-core-client {@code AccessSync}) и кэшируется по UUID здесь;
 * пока не пришла или core-service недоступен — работает только op. {@code isCreative()} правом не
 * является. На клиенте op синхронизирует ванилла, роль из БД — пакет уровня; клиентская проверка —
 * только скрытие UI, сервер проверяет сам на каждом пакете.
 */
public final class PwpPermissions {
    /** Уровень op, нужный для админ-действий (как у /gamemode, /give). */
    public static final int ADMIN_LEVEL = 2;

    /** Роли из БД по UUID (только сервер; null — роль неизвестна). */
    private static final Map<UUID, AccessLevel> DB_ROLES = new ConcurrentHashMap<>();

    private PwpPermissions() {}

    /** Прежний вызов: ADMIN и выше (op 2+ или роль admin/owner). */
    public static boolean isAdmin(Player player) {
        return has(player, AccessLevel.ADMIN);
    }

    public static boolean has(Player player, AccessLevel required) {
        return player != null && level(player).atLeast(required);
    }

    /** Итоговый уровень игрока. */
    public static AccessLevel level(Player player) {
        if (player == null) return AccessLevel.PLAYER;
        return AccessLevel.combine(opLevel(player), DB_ROLES.get(player.getUUID()));
    }

    /** Уровень op 0–4 (ванильный permission level). */
    public static int opLevel(Player player) {
        for (int lvl = 4; lvl >= 1; lvl--) if (player.hasPermissions(lvl)) return lvl;
        return 0;
    }

    /** Роль из БД (null — забыть). Возврат: изменилась ли запись. */
    public static boolean setDbRole(UUID uuid, AccessLevel role) {
        AccessLevel prev = role == null ? DB_ROLES.remove(uuid) : DB_ROLES.put(uuid, role);
        return prev != role;
    }

    public static void forget(UUID uuid) {
        DB_ROLES.remove(uuid);
    }
}

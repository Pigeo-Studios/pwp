package com.pwp.api.security;

import java.util.Locale;

/**
 * Уровень доступа к админ-функциям PWP. Итог = max(уровень по op, роль игрока в БД core-service
 * {@code players.role}). Порядок констант = порядок прав (сравнение по {@link #ordinal()}); имена уходят
 * клиенту строкой — не переименовывать.
 *
 * <ul>
 *   <li>{@link #PLAYER} — обычный игрок (роль {@code user}, op 0–1);
 *   <li>{@link #MODERATOR} — модерация игроков онлайн (роль {@code support});
 *   <li>{@link #ADMIN} — матч, лобби, карта (роль {@code admin}, op 2–3);
 *   <li>{@link #OWNER} — всё + сохранение настроек «навсегда» (роль {@code owner}, op 4).
 * </ul>
 */
public enum AccessLevel {
    PLAYER,
    MODERATOR,
    ADMIN,
    OWNER;

    /** Уровень по op (permission level сервера 0–4). */
    public static AccessLevel fromOp(int opLevel) {
        if (opLevel >= 4) return OWNER;
        if (opLevel >= 2) return ADMIN;
        return PLAYER;
    }

    /** Роль из БД ({@code user/support/admin/owner}, регистр не важен); неизвестная/пустая — PLAYER. */
    public static AccessLevel fromRole(String role) {
        if (role == null) return PLAYER;
        return switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "owner" -> OWNER;
            case "admin" -> ADMIN;
            case "support", "moderator" -> MODERATOR;
            default -> PLAYER;
        };
    }

    /** Имя уровня от сервера ({@link #name()}); битое — PLAYER (клиент ничего лишнего не покажет). */
    public static AccessLevel parse(String name) {
        if (name == null) return PLAYER;
        for (AccessLevel l : values()) if (l.name().equalsIgnoreCase(name.trim())) return l;
        return PLAYER;
    }

    public static AccessLevel max(AccessLevel a, AccessLevel b) {
        if (a == null) return b == null ? PLAYER : b;
        if (b == null) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /** Итоговый уровень: op и роль из БД (null — роль неизвестна, например core-service недоступен). */
    public static AccessLevel combine(int opLevel, AccessLevel dbRole) {
        return max(fromOp(opLevel), dbRole);
    }

    public boolean atLeast(AccessLevel required) {
        return required == null || ordinal() >= required.ordinal();
    }
}

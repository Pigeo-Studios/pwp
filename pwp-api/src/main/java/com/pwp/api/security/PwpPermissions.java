package com.pwp.api.security;

import net.minecraft.world.entity.player.Player;

/**
 * Единая проверка прав на админ-действия (редакторы китов/техники/скинов, отладка).
 *
 * <p>Раньше права проверялись через {@code isCreative()} — креатив не является правом
 * (его можно получить, например, в одиночке/тестовом мире, а на клиенте он подделывается).
 * Источник истины — permission level сервера (op 2+). Ванилла синхронизирует уровень
 * клиенту, поэтому на клиенте метод годится для показа/скрытия UI, а сервер обязан
 * проверять сам на каждом пакете.
 */
public final class PwpPermissions {
    /** Уровень op, нужный для админ-действий (как у /gamemode, /give). */
    public static final int ADMIN_LEVEL = 2;

    private PwpPermissions() {}

    public static boolean isAdmin(Player player) {
        return player != null && player.hasPermissions(ADMIN_LEVEL);
    }
}

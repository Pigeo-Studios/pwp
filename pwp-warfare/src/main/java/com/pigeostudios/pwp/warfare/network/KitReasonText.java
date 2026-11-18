package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.match.KitLimits;
import java.util.Arrays;
import net.minecraft.network.chat.Component;

/**
 * Текст причины недоступности роли из строки {@code key|a|b} ({@link KitLimits.Reason#encoded}) —
 * на языке игрока (lang-ключи {@code pwpwarfare.kit.reason.*}). Старый/свободный текст показывается
 * как есть. Работает на обеих сторонах: сервер шлёт translatable, клиент переводит сам.
 */
public final class KitReasonText {
    /** Кита нет у команды игрока. */
    public static final String NOT_FOUND = KitLimits.Reason.KEY_PREFIX + "not_found";

    private KitReasonText() {}

    public static Component of(String reason) {
        String[] parts = KitLimits.Reason.decode(reason);
        if (parts == null) return Component.literal(reason == null ? "" : reason);
        Object[] args = Arrays.copyOfRange(parts, 1, parts.length);
        return Component.translatable(parts[0], args);
    }
}

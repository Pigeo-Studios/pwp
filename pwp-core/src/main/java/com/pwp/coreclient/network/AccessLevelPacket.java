package com.pwp.coreclient.network;

import com.pwp.api.net.Net;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S→C: уровень доступа игрока ({@code AccessLevel.name()}). Шлёт {@link AccessSync} при входе и при
 * смене op/роли. Клиент только прячет/показывает админ-UI; каждое действие проверяет сервер.
 */
public record AccessLevelPacket(String level) implements Net.S2C {
    public static void encode(AccessLevelPacket m, FriendlyByteBuf b) {
        b.writeUtf(m.level, 16);
    }

    public static AccessLevelPacket decode(FriendlyByteBuf b) {
        return new AccessLevelPacket(b.readUtf(16));
    }

    @Override
    public void handleClient() {
        com.pwp.coreclient.admin.AdminPanel.onLevel(level);
    }
}

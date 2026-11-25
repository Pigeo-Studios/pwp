package com.pwp.coreclient.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Пункт радиального/контекстного меню. Список пунктов строится провайдером действий объекта
 * (хаб, ящик, техника, игрок, постройка — этап 4) из DTO сервера; выполнение — пакет на сервер,
 * который сам проверяет право (клиентский {@code enabled} — только подсказка).
 *
 * @param icon текстура иконки или null
 * @param disabledReason почему недоступно (показывается в тултипе), null — доступно
 */
public record UIAction(Component label, ResourceLocation icon, String disabledReason, Runnable run) {
    public static UIAction of(Component label, Runnable run) {
        return new UIAction(label, null, null, run);
    }

    public static UIAction disabled(Component label, String reason) {
        return new UIAction(label, null, reason, null);
    }

    public boolean enabled() {
        return disabledReason == null && run != null;
    }
}

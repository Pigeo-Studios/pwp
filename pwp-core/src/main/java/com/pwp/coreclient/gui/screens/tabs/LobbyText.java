package com.pwp.coreclient.gui.screens.tabs;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Подписи лобби: имена фракций, время, флаги. Только форматирование данных сервера. */
public final class LobbyText {
    private LobbyText() {}

    /** Имя фракции по id (usa, russia…): ключ pwp_core.faction.<id>, иначе id заглавными, пусто — «—». */
    public static Component faction(String id) {
        if (id == null || id.isBlank()) return Component.literal("—");
        String key = "pwp_core.faction." + id.toLowerCase();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(id.toUpperCase());
    }

    /** «m:ss» (или «h:mm:ss») из секунд. */
    public static String clock(int seconds) {
        int s = Math.max(0, seconds);
        if (s >= 3600) return String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    /** Флаг фракции (текстуры pwp-warfare, 64x36); null — нет id. */
    public static ResourceLocation flag(String id) {
        return id != null && !id.isBlank()
                ? new ResourceLocation("pwpwarfare", "textures/gui/flags/" + id.toLowerCase() + ".png")
                : null;
    }

    public static final int FLAG_W = 64;
    public static final int FLAG_H = 36;
}

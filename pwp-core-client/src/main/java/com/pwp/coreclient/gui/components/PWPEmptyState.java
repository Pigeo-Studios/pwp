package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PWPEmptyState {

    private PWPEmptyState() {}

    public static void render(GuiGraphics gui, String title, String subtitle, int cx, int cy) {
        var font = PWPTheme.Fonts.display();
        gui.drawString(font, Component.literal(title), cx - font.width(title) / 2, cy - 8, PWPTheme.Colors.TEXT_SECONDARY, false);
        if (subtitle != null && !subtitle.isEmpty()) {
            gui.drawString(font, Component.literal(subtitle), cx - font.width(subtitle) / 2, cy + 8, PWPTheme.Colors.TEXT_DIM, false);
        }
    }

    public static void render(GuiGraphics gui, String title, int cx, int cy) {
        render(gui, title, null, cx, cy);
    }
}

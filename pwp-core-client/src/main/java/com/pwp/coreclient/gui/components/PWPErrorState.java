package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PWPErrorState {

    private PWPErrorState() {}

    public static void render(GuiGraphics gui, String title, String subtitle, int cx, int cy) {
        var font = PWPTheme.Fonts.display();
        String errTitle = "\u26A0 " + title;
        gui.drawString(font, Component.literal(errTitle), cx - font.width(errTitle) / 2, cy - 10, PWPTheme.Colors.DANGER, false);
        if (subtitle != null && !subtitle.isEmpty()) {
            gui.drawString(font, Component.literal(subtitle), cx - font.width(subtitle) / 2, cy + 6, PWPTheme.Colors.TEXT_SECONDARY, false);
        }
    }
}

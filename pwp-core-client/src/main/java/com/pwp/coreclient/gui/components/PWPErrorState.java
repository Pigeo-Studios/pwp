package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PWPErrorState {

    private PWPErrorState() {}

    public static void render(GuiGraphics gui, String title, String subtitle, int cx, int cy) {
        var font = Minecraft.getInstance().font;
        gui.drawCenteredString(font, Component.literal("\u26A0 " + title), cx, cy - 10, PWPTheme.Colors.DANGER);
        if (subtitle != null && !subtitle.isEmpty()) {
            gui.drawCenteredString(font, Component.literal(subtitle), cx, cy + 6, PWPTheme.Colors.TEXT_SECONDARY);
        }
    }
}

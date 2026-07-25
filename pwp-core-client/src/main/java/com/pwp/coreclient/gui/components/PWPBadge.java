package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PWPBadge {

    public enum Type {
        SUCCESS, DANGER, WARNING, INFO
    }

    private PWPBadge() {}

    public static void render(GuiGraphics gui, String text, int x, int y, Type type) {
        var font = PWPTheme.Fonts.display();
        int textW = font.width(text) + 8;
        int h = 14;

        int bgColor = switch (type) {
            case SUCCESS -> PWPTheme.Styles.Badge.BG_SUCCESS;
            case DANGER -> PWPTheme.Styles.Badge.BG_DANGER;
            case WARNING -> PWPTheme.Styles.Badge.BG_WARNING;
            case INFO -> PWPTheme.Styles.Badge.BG_INFO;
        };

        gui.fill(x, y, x + textW, y + h, bgColor);
        gui.drawCenteredString(font, Component.literal(text), x + textW / 2, y + 3, PWPTheme.Styles.Badge.TEXT);
    }
}

package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPIcons;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class PWPLayout {

    public static final float CONTENT_WIDTH_RATIO = 0.7F;
    public static final int CONTENT_MIN = 260;
    public static final int CONTENT_MAX = 640;

    private PWPLayout() {}

    public static int centerX(int screenWidth, int elementWidth) {
        return (screenWidth - elementWidth) / 2;
    }

    public static int contentWidth(int screenWidth) {
        return Mth.clamp((int) (screenWidth * CONTENT_WIDTH_RATIO), CONTENT_MIN, CONTENT_MAX);
    }

    public static int gridColumns(int contentWidth, int gap, int cardWidth) {
        return Math.max(1, (contentWidth + gap) / (cardWidth + gap));
    }

    public static void renderHeader(GuiGraphics gui, String title, int screenWidth) {
        renderHeader(gui, title, -1, screenWidth, PWPTheme.Spacing.MD);
    }

    public static void renderHeader(GuiGraphics gui, String title, int icon, int screenWidth) {
        renderHeader(gui, title, icon, screenWidth, PWPTheme.Spacing.MD);
    }

    public static void renderHeader(GuiGraphics gui, String title, int icon, int screenWidth, int topPadding) {
        var font = PWPTheme.Fonts.display();
        int cx = screenWidth / 2;
        int y = topPadding;

        if (icon >= 0) {
            PWPIcons.render(gui, icon, cx - font.width(title) / 2 - 20, y + 2);
        }

        gui.drawString(font, Component.literal(title), cx - font.width(title) / 2, y, PWPTheme.Colors.TEXT_ACCENT, false);

        int lineW = Math.min(120, font.width(title) + 40);
        gui.fill(cx - lineW / 2, y + 14, cx + lineW / 2, y + 15, PWPTheme.Colors.ACCENT);
    }

    public static void renderFooter(GuiGraphics gui, String text, int screenWidth, int screenHeight) {
        var font = PWPTheme.Fonts.display();
        int color = PWPTheme.Colors.TEXT_DIM;
        gui.drawString(font, Component.literal(text), screenWidth / 2 - font.width(text) / 2, screenHeight - 12, color, false);
    }

    public static void renderDivider(GuiGraphics gui, int x, int y, int width) {
        gui.fill(x, y, x + width, y + 1, PWPTheme.Colors.BORDER);
    }
}

package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PWPPanel {

    public enum Variant {
        SURFACE, SURFACE_DIM, ACCENT_BORDER
    }

    private PWPPanel() {}

    public static void render(GuiGraphics gui, int x, int y, int w, int h) {
        render(gui, x, y, w, h, Variant.SURFACE, false);
    }

    public static void render(GuiGraphics gui, int x, int y, int w, int h, Variant variant) {
        render(gui, x, y, w, h, variant, false);
    }

    public static void render(GuiGraphics gui, int x, int y, int w, int h, Variant variant, boolean elevated) {
        int bg, borderColor;
        switch (variant) {
            case SURFACE_DIM:
                bg = PWPTheme.Colors.SURFACE_DIM;
                borderColor = PWPTheme.Colors.BORDER;
                break;
            case ACCENT_BORDER:
                bg = PWPTheme.Colors.SURFACE;
                borderColor = PWPTheme.Colors.BORDER_ACCENT;
                break;
            default:
                bg = PWPTheme.Colors.SURFACE;
                borderColor = PWPTheme.Colors.BORDER;
                break;
        }

        int r = PWPTheme.Spacing.RADIUS_MEDIUM;

        if (elevated) {
            int shadowOffset = 3;
            RoundedRect.fill(gui, x, y + shadowOffset, w, h, r, PWPTheme.Styles.Panel.SHADOW);
        }

        RoundedRect.fill(gui, x, y, w, h, r, bg);
        RoundedRect.border(gui, x, y, w, h, r, 1, borderColor);
    }

    public static void renderWithTitle(GuiGraphics gui, int x, int y, int w, int h, String title, int titleColor) {
        render(gui, x, y, w, h);
        if (title != null && !title.isEmpty()) {
            var font = Minecraft.getInstance().font;
            int titleY = y + PWPTheme.Spacing.XS;
            gui.drawCenteredString(font, Component.literal(title), x + w / 2, titleY, titleColor);
            int dividerY = y + titleHeight() - PWPTheme.Spacing.XXS;
            gui.fill(x + PWPTheme.Spacing.SM, dividerY, x + w - PWPTheme.Spacing.SM, dividerY + 1, PWPTheme.Colors.BORDER);
        }
    }

    public static int titleHeight() {
        return PWPTheme.Spacing.XS + PWPTheme.Fonts.SIZE_NORMAL + PWPTheme.Spacing.XS;
    }
}

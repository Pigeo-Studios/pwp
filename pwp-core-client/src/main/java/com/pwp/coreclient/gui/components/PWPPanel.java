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
        render(gui, x, y, w, h, Variant.SURFACE);
    }

    public static void render(GuiGraphics gui, int x, int y, int w, int h, Variant variant) {
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

        int r = PWPTheme.Spacing.RADIUS_SMALL;

        gui.fill(x + r, y, x + w - r, y + h, bg);
        gui.fill(x, y + r, x + r, y + h - r, bg);
        gui.fill(x + w - r, y + r, x + w, y + h - r, bg);
        gui.fill(x + r, y + r, x + w - r, y + h - r, bg);

        gui.fill(x + r, y, x + w - r, y + 1, borderColor);
        gui.fill(x + r, y + h - 1, x + w - r, y + h, borderColor);
        gui.fill(x, y + r, x + 1, y + h - r, borderColor);
        gui.fill(x + w - 1, y + r, x + w, y + h - r, borderColor);
        gui.fill(x + r, y, x + r + 1, y + 1, borderColor);
        gui.fill(x + w - r - 1, y, x + w - r, y + 1, borderColor);
        gui.fill(x + r, y + h - 1, x + r + 1, y + h, borderColor);
        gui.fill(x + w - r - 1, y + h - 1, x + w - r, y + h, borderColor);
    }

    public static void renderWithTitle(GuiGraphics gui, int x, int y, int w, int h, String title, int titleColor) {
        render(gui, x, y, w, h);
        if (title != null && !title.isEmpty()) {
            var font = Minecraft.getInstance().font;
            gui.drawCenteredString(font, Component.literal(title), x + w / 2, y + 4, titleColor);
            gui.fill(x + 10, y + 14, x + w - 10, y + 15, PWPTheme.Colors.BORDER);
        }
    }

    public static int titleHeight() {
        return 18;
    }
}

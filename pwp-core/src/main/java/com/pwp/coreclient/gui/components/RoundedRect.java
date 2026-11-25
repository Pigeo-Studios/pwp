package com.pwp.coreclient.gui.components;

import net.minecraft.client.gui.GuiGraphics;

public final class RoundedRect {

    private RoundedRect() {}

    public static void fill(GuiGraphics gui, int x, int y, int w, int h, int radius, int color) {
        int r = clampRadius(radius, w, h);
        if (r <= 0) {
            gui.fill(x, y, x + w, y + h, color);
            return;
        }

        gui.fill(x, y + r, x + w, y + h - r, color);

        for (int i = 0; i < r; i++) {
            int inset = insetForRow(r, i);
            gui.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            gui.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void border(GuiGraphics gui, int x, int y, int w, int h, int radius, int thickness, int color) {
        int r = clampRadius(radius, w, h);
        int t = Math.max(1, thickness);

        if (r <= 0) {
            gui.fill(x, y, x + w, y + t, color);
            gui.fill(x, y + h - t, x + w, y + h, color);
            gui.fill(x, y, x + t, y + h, color);
            gui.fill(x + w - t, y, x + w, y + h, color);
            return;
        }

        gui.fill(x + r, y, x + w - r, y + t, color);
        gui.fill(x + r, y + h - t, x + w - r, y + h, color);
        gui.fill(x, y + r, x + t, y + h - r, color);
        gui.fill(x + w - t, y + r, x + w, y + h - r, color);

        for (int i = 0; i < r; i++) {
            int inset = insetForRow(r, i);
            int nextInset = insetForRow(r, Math.min(i + 1, r - 1));
            int arcThickness = Math.max(1, Math.min(t, Math.abs(nextInset - inset) + t));

            gui.fill(x + inset, y + i, x + Math.min(inset + arcThickness, w - inset), y + i + 1, color);
            gui.fill(x + w - inset - arcThickness, y + i, x + w - inset, y + i + 1, color);
            gui.fill(x + inset, y + h - 1 - i, x + Math.min(inset + arcThickness, w - inset), y + h - i, color);
            gui.fill(x + w - inset - arcThickness, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void glow(GuiGraphics gui, int x, int y, int w, int h, int radius, int spread, int glowColor) {
        fill(gui, x - spread, y - spread, w + spread * 2, h + spread * 2, radius + spread, glowColor);
    }

    private static int insetForRow(int r, int row) {
        double dy = r - row - 0.5;
        double dx = Math.sqrt(Math.max(0, (double) r * r - dy * dy));
        return (int) Math.round(r - dx);
    }

    private static int clampRadius(int radius, int w, int h) {
        return Math.max(0, Math.min(radius, Math.min(w, h) / 2));
    }
}

package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

public class PWPSkeleton {

    private static final long PERIOD_MS = 1200;

    private PWPSkeleton() {}

    public static void render(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        float t = (float) (ageMs % PERIOD_MS) / PERIOD_MS;
        float pulse = 0.3f + 0.2f * (float) Math.sin(t * Math.PI * 2);
        int alpha = Math.min(255, Math.max(40, (int) (pulse * 255)));
        int color = (alpha << 24) | (PWPTheme.Colors.SURFACE & 0x00FFFFFF);
        int r = PWPTheme.Spacing.RADIUS_SMALL;
        RoundedRect.fill(gui, x, y, w, h, r, color);
        RoundedRect.border(gui, x, y, w, h, r, 1, PWPTheme.Colors.BORDER);
    }

    public static void renderCard(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        render(gui, x, y, w, h, ageMs);

        int pad = 10;
        int lineH = 6;
        int lineGap = 4;
        int lineX = x + pad;
        int lineW = w - pad * 2;

        renderLine(gui, lineX, y + pad, lineW * 6 / 10, lineH, ageMs);
        renderLine(gui, lineX, y + pad + lineH + lineGap, lineW * 4 / 10, lineH, ageMs);
        renderLine(gui, lineX, y + pad + (lineH + lineGap) * 2, lineW, lineH, ageMs);
    }

    private static void renderLine(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        float t = (float) (ageMs % PERIOD_MS) / PERIOD_MS;
        float pulse = 0.15f + 0.1f * (float) Math.sin(t * Math.PI * 2);
        int alpha = Math.min(255, Math.max(20, (int) (pulse * 255)));
        int color = (alpha << 24) | (PWPTheme.Colors.SURFACE_LIGHT & 0x00FFFFFF);
        RoundedRect.fill(gui, x, y, w, h, 2, color);
    }
}

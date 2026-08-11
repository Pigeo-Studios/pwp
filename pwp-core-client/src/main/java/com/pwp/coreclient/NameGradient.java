package com.pwp.coreclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Градиентные имена донатеров: общий расчёт цвета по тиру + отрисовка в 2D-интерфейсах. */
public final class NameGradient {

    private NameGradient() {}

    // Пары (старт, конец) градиентов по тиру
    private static final int SILVER_START = 0xFF8E9AA6;
    private static final int SILVER_END   = 0xFFF3F6F9;
    private static final int GOLD_START   = 0xFFB8860B;
    private static final int GOLD_END     = 0xFFFFF3B0;
    private static final int PLATINUM_START = 0xFF6FB6D6;
    private static final int PLATINUM_END   = 0xFFF6FBFF;

    public static boolean isSupported(String tier) {
        if (tier == null) return false;
        return switch (tier.toUpperCase()) {
            case "SILVER", "GOLD", "PLATINUM" -> true;
            default -> false;
        };
    }

    /** Цвет символа индекса index из total, фаза анимации от времени. */
    public static int color(String tier, int index, int total, long timeMs) {
        if (tier == null) return 0xFFFFFFFF;
        int c0, c1;
        switch (tier.toUpperCase()) {
            case "SILVER" -> { c0 = SILVER_START; c1 = SILVER_END; }
            case "PLATINUM" -> { c0 = PLATINUM_START; c1 = PLATINUM_END; }
            default -> { c0 = GOLD_START; c1 = GOLD_END; }
        }
        // «живой» перелив: фаза двигается по строке
        double base = total > 1 ? (double) index / (total - 1) : 0.0;
        double phase = (timeMs % 1800L) / 1800.0;
        double t = base + phase;
        t -= Math.floor(t);
        return lerp(c0, c1, t);
    }

    /** Ширина градиентного текста (без draw) — для центрирования. */
    public static int width(Font font, String text) {
        return font.width(text);
    }

    /** Отрисовка градиентного текста в 2D-интерфейсе (GuiGraphics). Возвращает итоговую ширину. */
    public static int draw(GuiGraphics g, Font font, String text, int x, int y, String tier, boolean shadow) {
        if (text.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        int n = text.length();
        int cursor = x;
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(text.charAt(i));
            int w = font.width(ch);
            g.drawString(font, ch, cursor, y, color(tier, i, n, now), shadow);
            cursor += w;
        }
        return cursor - x;
    }

    /** Отрисовка с мягким свечением вокруг текста (для лидербордов). */
    public static int drawGlow(GuiGraphics g, Font font, String text, int x, int y, String tier) {
        int glow = 0x40FFFFFF;
        g.drawString(font, text, x - 1, y, glow, false);
        g.drawString(font, text, x + 1, y, glow, false);
        g.drawString(font, text, x, y - 1, glow, false);
        g.drawString(font, text, x, y + 1, glow, false);
        return draw(g, font, text, x, y, tier, true);
    }

    private static int lerp(int c0, int c1, double t) {
        int a0 = (c0 >> 24) & 0xFF, r0 = (c0 >> 16) & 0xFF, g0 = (c0 >> 8) & 0xFF, b0 = c0 & 0xFF;
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a = (int) Math.round(a0 + (a1 - a0) * t);
        int r = (int) Math.round(r0 + (r1 - r0) * t);
        int g = (int) Math.round(g0 + (g1 - g0) * t);
        int b = (int) Math.round(b0 + (b1 - b0) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

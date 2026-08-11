package com.pwp.coreclient;

import com.pwp.coreclient.donor.DonorLevel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Градиентные имена: цвет по пиксельной позиции от уровня (тир или роль) + отрисовка в 2D и надмиде. */
public final class NameGradient {

    private NameGradient() {}

    // Цикл «живого» перелива цвета по строке
    private static final long CYCLE_MS = 2800L;

    // Альфа дилатационного ореола (8 копий по ±1px)
    private static final int GLOW_ALPHA = 0x2A;

    /** Цвет по пиксельной позиции px внутри строки шириной totalWidth. Плавно, без ступеней по символам. */
    public static int colorAt(String level, double px, double totalWidth, long timeMs) {
        DonorLevel lvl = DonorLevel.byName(level);
        if (lvl == null) return 0xFFFFFFFF;
        double t = totalWidth > 1 ? px / totalWidth : 0.0;
        // Косинус-перелив вперёд-назад (0->1->0 плавно, производная 0 на краях) — вместо
        // пилообразного возврата, при котором в момент оборота цикла цвет резко прыгал
        double raw = (timeMs % CYCLE_MS) / (double) CYCLE_MS;
        double phase = 0.5 - 0.5 * Math.cos(raw * 2.0 * Math.PI);
        t = t + phase;
        t -= Math.floor(t);
        return grade(lvl.stops(), t);
    }

    /** Ширина градиентного текста (без draw) — для центрирования. */
    public static int width(Font font, String text) {
        return font.width(text);
    }

    /** Отрисовка градиентного текста в 2D-интерфейсе (GuiGraphics). Возвращает итоговую ширину. */
    public static int draw(GuiGraphics g, Font font, String text, int x, int y, String level, boolean shadow) {
        if (text.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        int total = font.width(text);
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int w = font.width(ch);
            g.drawString(font, ch, cursor, y, colorAt(level, cursor + w / 2.0 - x, total, now), shadow);
            cursor += w;
        }
        return cursor - x;
    }

    /** Отрисовка с мягким ореолом вокруг текста (для лидербордов). Ореол — дилатация на 8 направлений
     *  цветом уровня (низкая альфа, по ±1px), сверху градиент — без ванильной тени. */
    public static int drawGlow(GuiGraphics g, Font font, String text, int x, int y, String level) {
        if (text.isEmpty()) return 0;
        DonorLevel lvl = DonorLevel.byName(level);
        if (lvl == null) return draw(g, font, text, x, y, level, true);
        int glow = (lvl.glowColor() & 0xFFFFFF) | (GLOW_ALPHA << 24);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                g.drawString(font, text, x + dx, y + dy, glow, false);
            }
        }
        return draw(g, font, text, x, y, level, false);
    }

    private static int grade(int[] stops, double t) {
        if (stops.length >= 3) return triple(stops[0], stops[1], stops[2], t);
        return lerp(stops[0], stops[1], t);
    }

    private static int triple(int c0, int c1, int c2, double t) {
        if (t < 0.5) return lerp(c0, c1, t * 2.0);
        return lerp(c1, c2, (t - 0.5) * 2.0);
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

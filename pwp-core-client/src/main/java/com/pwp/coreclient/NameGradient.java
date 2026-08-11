package com.pwp.coreclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Градиентные имена: цвет по пиксельной позиции от уровня (тир или роль) + отрисовка в 2D и надмиде. */
public final class NameGradient {

    private NameGradient() {}

    // Цикл «живого» перелива цвета по строке
    private static final long CYCLE_MS = 2800L;

    // Палитры тиров (старт, конец) в ARGB
    private static final int SILVER_START   = 0xFF8E9AA6;
    private static final int SILVER_END     = 0xFFF3F6F9;
    private static final int GOLD_START     = 0xFFB8860B;
    private static final int GOLD_END       = 0xFFFFF3B0;
    private static final int PLATINUM_START = 0xFF6FB6D6;
    private static final int PLATINUM_END   = 0xFFF6FBFF;

    // Роли — ярче тиров: ADMIN огненный красный (3 стопа), MODERATOR сине-голубой
    private static final int MODERATOR_START = 0xFF2A5B8F;
    private static final int MODERATOR_END   = 0xFF9FD0FF;
    // Старт палитры яркий (0xFF5C0000 был почти чёрным — первый «сегмент» ника читался чёрной плашкой)
    private static final int[] ADMIN_PALETTE = {0xFFD50000, 0xFFFF4D4D, 0xFFFFD980};

    // Цвет мягкого свечения по уровню (зарезервировано)
    private static final int GLOW_WHITE      = 0x40FFFFFF;
    private static final int GLOW_ADMIN      = 0x40FF3030;
    private static final int GLOW_MODERATOR  = 0x4030A0FF;

    /** Нормализация роли из core-service (admin/owner/support/moderator) в display-уровень или null. */
    public static String normalizeRole(String role) {
        if (role == null) return null;
        return switch (role.trim().toLowerCase()) {
            case "admin", "owner" -> "ADMIN";
            case "support", "moderator" -> "MODERATOR";
            default -> null;
        };
    }

    /** Итоговый уровень для отображения: роль приоритетнее тира (ADMIN > MODERATOR > тир). */
    public static String resolve(String role, String tier) {
        String r = normalizeRole(role);
        if (r != null) return r;
        if (tier == null) return null;
        String t = tier.toUpperCase();
        return isSupported(t) ? t : null;
    }

    public static boolean isSupported(String level) {
        if (level == null) return false;
        return switch (level.toUpperCase()) {
            case "SILVER", "GOLD", "PLATINUM", "ADMIN", "MODERATOR" -> true;
            default -> false;
        };
    }

    /** Цвет по пиксельной позиции px внутри строки шириной totalWidth. Плавно, без ступеней по символам. */
    public static int colorAt(String level, double px, double totalWidth, long timeMs) {
        if (level == null) return 0xFFFFFFFF;
        double t = totalWidth > 1 ? px / totalWidth : 0.0;
        // Косинус-перелив вперёд-назад (0->1->0 плавно, производная 0 на краях) — вместо
        // пилообразного возврата, при котором в момент оборота цикла цвет резко прыгал
        // с выцветшего конца палитры на стартовый (у ADMIN — красный «вспыхивал» рывком)
        double raw = (timeMs % CYCLE_MS) / (double) CYCLE_MS;
        double phase = 0.5 - 0.5 * Math.cos(raw * 2.0 * Math.PI);
        t = t + phase;
        t -= Math.floor(t);
        return grade(level, t);
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

    /** Отрисовка с мягким свечением вокруг текста (для лидербордов). Цвет свечения зависит от уровня. */
    public static int drawGlow(GuiGraphics g, Font font, String text, int x, int y, String level) {
        // Раньше было 4 «призрачные» копии в ±1px (0x40 см. цвет) — они складывались и давали
        // эффект «двух элементов» ника. Оставляем один проход градиента с тенью — без наложения.
        return draw(g, font, text, x, y, level, true);
    }

    private static int grade(String level, double t) {
        switch (level) {
            case "SILVER":
                return lerp(SILVER_START, SILVER_END, t);
            case "GOLD":
                return lerp(GOLD_START, GOLD_END, t);
            case "PLATINUM":
                return lerp(PLATINUM_START, PLATINUM_END, t);
            case "MODERATOR":
                return lerp(MODERATOR_START, MODERATOR_END, t);
            case "ADMIN":
                return triple(ADMIN_PALETTE[0], ADMIN_PALETTE[1], ADMIN_PALETTE[2], t);
            default:
                return lerp(GOLD_START, GOLD_END, t);
        }
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

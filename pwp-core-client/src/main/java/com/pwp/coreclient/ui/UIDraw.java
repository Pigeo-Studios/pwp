package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Общие примитивы рисования UI-кита (поверх RoundedRect и PWPTheme). */
@OnlyIn(Dist.CLIENT)
public final class UIDraw {
    private UIDraw() {}

    /** Скруглённая плашка с обводкой (border = 0 — без обводки). */
    public static void box(GuiGraphics g, int x, int y, int w, int h, int radius, int bg, int border) {
        if (PWPTheme.Colors.alpha(bg) > 0) RoundedRect.fill(g, x, y, w, h, radius, bg);
        if (PWPTheme.Colors.alpha(border) > 0) RoundedRect.border(g, x, y, w, h, radius, 1, border);
    }

    /** Текст по центру прямоугольника с тенью. */
    public static void centered(GuiGraphics g, Font font, Component text, int cx, int cy, int color) {
        int tw = font.width(text);
        g.drawString(font, text, cx - tw / 2, cy - font.lineHeight / 2, color, true);
    }

    /** Текст, обрезанный многоточием по ширине. */
    public static Component ellipsize(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String s = text.getString();
        String dots = "…";
        int dw = font.width(dots);
        while (!s.isEmpty() && font.width(s) + dw > maxWidth) s = s.substring(0, s.length() - 1);
        return Component.literal(s + dots).withStyle(text.getStyle());
    }

    /** Цвет с прозрачностью, умноженной на alpha (0..1) — для фейдов. */
    public static int fade(int color, float alpha) {
        return PWPTheme.Colors.multiplyAlpha(color, Math.max(0F, Math.min(1F, alpha)));
    }
}

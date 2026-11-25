package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Общая «обвязка» экранов core-client: поля экрана, верхняя полоса с логотипом, нижняя
 * полоса, логотип целочисленного масштаба. Размеры — по {@link Fit.Density} из PWPTheme.
 */
public final class PWPChrome {
    private static final Component LOGO = Component.literal("PWP");

    private PWPChrome() {}

    public static Fit.Density density(int w, int h) {
        return Fit.Density.of(w, h);
    }

    /** Поле экрана (отступ контента от краёв). */
    public static int margin(int w) {
        return Fit.fraction(w, Spacing.SCREEN_MARGIN_FRACTION, Spacing.SCREEN_MARGIN_MIN, Spacing.SCREEN_MARGIN_MAX);
    }

    public static int topBarHeight(Fit.Density d) {
        return d.pick(Spacing.TOP_BAR_H_COMPACT, Spacing.TOP_BAR_H, Spacing.TOP_BAR_H_LARGE);
    }

    public static int footerHeight(Fit.Density d) {
        return d.pick(Spacing.FOOTER_H_COMPACT, Spacing.FOOTER_H, Spacing.FOOTER_H);
    }

    /** Масштаб логотипа главного меню. */
    public static int logoScale(Fit.Density d) {
        return d.pick(PWPTheme.Text.LOGO_SCALE_COMPACT, PWPTheme.Text.LOGO_SCALE, PWPTheme.Text.LOGO_SCALE_LARGE);
    }

    /** Ширина/высота логотипа при масштабе. */
    public static int logoWidth(Font font, int scale) {
        return font.width(LOGO) * scale;
    }

    public static int logoHeight(Font font, int scale) {
        return (font.lineHeight - 1) * scale;
    }

    /** Логотип «PWP» + акцентное подчёркивание; (x, y) — левый верх. */
    public static void logo(GuiGraphics g, Font font, int x, int y, int scale, float opacity) {
        UIDraw.textScaled(g, font, LOGO, x, y, scale, UIDraw.fade(PWPTheme.Text.LOGO, opacity), true);
    }

    /**
     * Верхняя полоса: логотип (масштаб 1–2) и подзаголовок проекта, если влезает. Возвращает
     * прямоугольник полосы — контент экрана начинается под ним.
     */
    public static Rect topBar(GuiGraphics g, Font font, int w, Fit.Density d, float opacity) {
        return topBar(g, font, w, d, opacity, null);
    }

    /** Верхняя полоса с текстом справа (например «ОНЛАЙН: 12»); подзаголовок его не перекрывает. */
    public static Rect topBar(GuiGraphics g, Font font, int w, Fit.Density d, float opacity, Component right) {
        Rect bar = Rect.of(0, 0, w, topBarHeight(d));
        UIBackdrop.bar(g, bar, true);
        int scale = d == Fit.Density.COMPACT ? 1 : 2;
        int m = margin(w);
        int lh = logoHeight(font, scale);
        int ly = bar.y() + (bar.h() - lh) / 2;
        logo(g, font, m, ly, scale, opacity);
        int sx = m + logoWidth(font, scale) + Spacing.SM;
        UIDraw.vline(g, sx, bar.y() + Spacing.XS, bar.bottom() - Spacing.XS, PWPTheme.Styles.Backdrop.BAR_LINE);
        sx += Spacing.SM;
        int ty = bar.y() + (bar.h() - font.lineHeight) / 2 + 1;
        int rightEdge = w - m;
        if (right != null) {
            Component r = UIDraw.ellipsize(font, right, Math.max(0, (w - 2 * m) / 3));
            int rw = font.width(r);
            UIDraw.text(g, font, r, rightEdge - rw, ty, UIDraw.fade(PWPTheme.Text.MUTED, opacity), false);
            rightEdge -= rw + Spacing.LG;
        }
        Component sub = UIDraw.ellipsize(font, Component.translatable("pwp_core.main_menu.subtitle"), rightEdge - sx);
        UIDraw.text(g, font, sub, sx, ty, UIDraw.fade(PWPTheme.Text.CAPTION, opacity), false);
        return bar;
    }

    /** Нижняя полоса: текст слева и справа, обрезка «…» без наложения. */
    public static Rect footer(GuiGraphics g, Font font, int w, int h, Fit.Density d, Component left, Component right) {
        Rect bar = Rect.of(0, h - footerHeight(d), w, footerHeight(d));
        UIBackdrop.bar(g, bar, false);
        Rect inner = bar.inset(margin(w), 0);
        int ty = bar.y() + (bar.h() - font.lineHeight) / 2 + 1;
        int rightW = right == null ? 0 : Math.min(font.width(right), inner.w() / 2);
        if (right != null) {
            Component r = UIDraw.ellipsize(font, right, rightW);
            UIDraw.text(g, font, r, inner.right() - font.width(r), ty, PWPTheme.Text.CAPTION, false);
        }
        if (left != null) {
            int leftW = inner.w() - rightW - (right == null ? 0 : Spacing.LG);
            UIDraw.text(g, font, UIDraw.ellipsize(font, left, leftW), inner.x(), ty, PWPTheme.Text.MUTED, false);
        }
        return bar;
    }
}

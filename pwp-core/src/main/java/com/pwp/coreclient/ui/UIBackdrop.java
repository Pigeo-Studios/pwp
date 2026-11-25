package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Атмосферный слой поверх фоновой картинки/мира: общее затемнение вертикальным градиентом,
 * тень от левого края (под колонку меню), шум, скан-лайны и виньетка. Экраны вызывают
 * {@link #overlay} сразу после картинки фона — сплошных плоских заливок на весь экран нет.
 */
@OnlyIn(Dist.CLIENT)
public final class UIBackdrop {
    private UIBackdrop() {}

    /** Какую сторону дополнительно затенить под контент. */
    public enum Shade {
        NONE,
        LEFT
    }

    public static void overlay(GuiGraphics g, int w, int h, Shade shade) {
        Rect screen = Rect.screen(w, h);
        // сила затемнения — настройка «Затемнение фона меню» (1 = как в теме)
        float dim = com.pwp.coreclient.settings.CoreUiSettings.BACKDROP_DIM.get();
        UIDraw.gradientV(
                g, screen, UIDraw.fade(Styles.Backdrop.SCRIM_TOP, dim), UIDraw.fade(Styles.Backdrop.SCRIM_BOTTOM, dim));
        if (shade == Shade.LEFT) {
            int sw = Math.round(w * Styles.Backdrop.SIDE_SHADE_FRACTION);
            UIDraw.gradientH(
                    g,
                    Rect.of(0, 0, sw, h),
                    UIDraw.fade(Styles.Backdrop.SIDE_SHADE, dim),
                    Colors.withAlpha(Styles.Backdrop.SIDE_SHADE, 0));
        }
        UIDraw.noise(g, screen, Styles.Backdrop.NOISE_TINT);
        UIDraw.scanlines(g, screen, Styles.Backdrop.SCANLINE_TINT);
        UIDraw.vignette(g, w, h, Styles.Backdrop.VIGNETTE, Styles.Backdrop.VIGNETTE_FRACTION);
    }

    /** Полоса шапки/подвала экрана: тёмный градиент + 1px линия со стороны контента. */
    public static void bar(GuiGraphics g, Rect r, boolean lineAtBottom) {
        if (lineAtBottom) {
            UIDraw.gradientV(g, r, Styles.Backdrop.BAR_TOP, Styles.Backdrop.BAR_BOTTOM);
            UIDraw.hline(g, r.x(), r.right(), r.bottom() - 1, Styles.Backdrop.BAR_LINE);
        } else {
            UIDraw.gradientV(g, r, Styles.Backdrop.BAR_BOTTOM, Styles.Backdrop.BAR_TOP);
            UIDraw.hline(g, r.x(), r.right(), r.y(), Styles.Backdrop.BAR_LINE);
        }
    }
}

package com.pigeostudios.pwp.warfare.client.gui.map;

import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * HUD тактической карты поверх всех слоёв: рамка со скобами, плашка «квадрат под курсором ·
 * зум», шкала масштаба (круглая длина под текущий зум) и роза «С» в правом верхнем углу.
 */
@OnlyIn(Dist.CLIENT)
public final class MapHud {
    private MapHud() {}

    /** Круглые длины шкалы масштаба, м. */
    private static final int[] SCALE_STEPS = {25, 50, 100, 200, 300, 500, 1000, 2000, 5000};

    public static void frame(GuiGraphics g, MapView v) {
        Rect a = v.area();
        UIDraw.outline(g, a.outset(1), PWPTheme.Styles.Glass.BORDER);
        UIDraw.frame(g, a.outset(1), PWPTheme.Styles.Glass.FRAME_TINT);
        // лёгкая внутренняя виньетка по краям карты (читаемость подписей)
        int band = Math.min(WTheme.MAP_INNER_SHADE, Math.min(a.w(), a.h()) / 6);
        int shade = WTheme.MAP_INNER_SHADE_COLOR;
        int clear = Colors.withAlpha(shade, 0);
        UIDraw.gradientV(g, a.top(band), shade, clear);
        UIDraw.gradientV(g, a.bottomStrip(band), clear, shade);
    }

    /** Плашка координат: квадрат под курсором (или «--») и зум. */
    public static void readout(GuiGraphics g, Font font, MapView v, int mx, int my) {
        Rect a = v.area();
        String kp = a.contains(mx, my) ? MapView.kp(v.worldX(mx), v.worldZ(my)) : "--";
        Component t = Component.literal(kp + "  ")
                .append(Component.translatable("gui.pwpwarfare.map.zoom", String.format("%.1f", v.bpp())));
        int lh = font.lineHeight + Spacing.XS;
        int w = font.width(t) + WTheme.ICON_SMALL + 3 * Spacing.XS;
        Rect r = Rect.of(
                a.x() + Spacing.XS + WTheme.MAP_LABEL_COL_W, a.y() + Spacing.XS + lh, Math.min(w, a.w() / 2), lh);
        UIDraw.rect(g, r, WTheme.MAP_LABEL_BG);
        UIDraw.outline(g, r, WTheme.MAP_LABEL_BORDER);
        WDraw.icon(
                g,
                WarfareIcon.TARGET,
                r.x() + Spacing.XS,
                r.centerY() - WTheme.ICON_SMALL / 2,
                WTheme.ICON_SMALL,
                Colors.ACCENT);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, t, r.w() - WTheme.ICON_SMALL - 2 * Spacing.XS),
                r.x() + 2 * Spacing.XS + WTheme.ICON_SMALL,
                r.y() + 3,
                WTheme.MAP_LABEL_TEXT,
                false);
    }

    /** Шкала масштаба внизу слева: длина — круглое число метров, ~[MIN..MAX] px. */
    public static void scaleBar(GuiGraphics g, Font font, MapView v) {
        Rect a = v.area();
        int meters = SCALE_STEPS[0];
        for (int s : SCALE_STEPS) {
            if (s / v.bpp() <= WTheme.MAP_SCALE_MAX_PX) meters = s;
        }
        int px = (int) Math.round(meters / v.bpp());
        if (px < WTheme.MAP_SCALE_MIN_PX || px > a.w() / 2) return;
        int x = a.x() + Spacing.SM;
        int y = a.bottom() - Spacing.SM - WTheme.BAR_H;
        Component t = Component.translatable("gui.pwpwarfare.map.scale", meters);
        Rect bg = Rect.of(
                x - Spacing.XS,
                y - font.lineHeight - Spacing.XS,
                Math.max(px, font.width(t)) + 2 * Spacing.XS,
                font.lineHeight + Spacing.XS + WTheme.BAR_H + Spacing.XS);
        UIDraw.rect(g, bg, WTheme.MAP_LABEL_BG);
        // полосатая шкала: половины разного цвета
        UIDraw.rect(g, Rect.of(x, y, px / 2, WTheme.BAR_H), WTheme.MAP_LABEL_TEXT);
        UIDraw.rect(g, Rect.of(x + px / 2, y, px - px / 2, WTheme.BAR_H), Colors.ACCENT);
        UIDraw.vline(g, x, y - Spacing.XXS, y + WTheme.BAR_H, WTheme.MAP_LABEL_TEXT);
        UIDraw.vline(g, x + px - 1, y - Spacing.XXS, y + WTheme.BAR_H, WTheme.MAP_LABEL_TEXT);
        UIDraw.text(g, font, t, x, y - font.lineHeight - 1, WTheme.MAP_LABEL_TEXT, true);
    }

    /** Роза «С» (север = −Z) в правом верхнем углу. */
    public static void compass(GuiGraphics g, Font font, MapView v) {
        Rect a = v.area();
        int s = Spacing.ICON_SIZE;
        int x = a.right() - s - Spacing.SM;
        int y = a.y() + Spacing.SM + font.lineHeight + Spacing.XS;
        Rect bg = Rect.of(x - Spacing.XXS, y - Spacing.XXS, s + 2 * Spacing.XXS, s + font.lineHeight + 2 * Spacing.XXS);
        UIDraw.rect(g, bg, WTheme.MAP_LABEL_BG);
        UIDraw.outline(g, bg, WTheme.MAP_LABEL_BORDER);
        WDraw.icon(g, WarfareIcon.COMPASS, x, y, s, WTheme.MAP_LABEL_TEXT);
        Component n = Component.translatable("gui.pwpwarfare.map.north");
        UIDraw.text(g, font, n, x + (s - font.width(n)) / 2, y + s + 1, Colors.ACCENT, true);
    }
}

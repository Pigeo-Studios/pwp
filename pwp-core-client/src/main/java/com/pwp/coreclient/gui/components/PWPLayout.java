package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPIcons;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Grid;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Легаси-хелперы раскладки (API прежний). Расчёты — через pwp-logic ({@link Fit}, {@link Grid}),
 * шапка/подвал/разделитель — в тактическом стиле UI-кита (заголовок с тенью и акцентным
 * подчёркиванием по ширине текста, обрезка «…» по ширине экрана). Новые экраны —
 * {@code ui.layout.Rect/Flow/Grid} и {@code PWPChrome}.
 */
public class PWPLayout {

    public static final float CONTENT_WIDTH_RATIO = 0.7F;
    public static final int CONTENT_MIN = 260;
    public static final int CONTENT_MAX = 640;

    private PWPLayout() {}

    public static int centerX(int screenWidth, int elementWidth) {
        return (screenWidth - elementWidth) / 2;
    }

    /** Ширина контента: 70% экрана в [260, 640], но не шире самого экрана с полями. */
    public static int contentWidth(int screenWidth) {
        return Fit.fraction(
                screenWidth, CONTENT_WIDTH_RATIO, CONTENT_MIN, Math.min(CONTENT_MAX, screenWidth - 2 * Spacing.MD));
    }

    public static int gridColumns(int contentWidth, int gap, int cardWidth) {
        return Grid.columnsFor(contentWidth, cardWidth, gap, Integer.MAX_VALUE);
    }

    public static void renderHeader(GuiGraphics gui, String title, int screenWidth) {
        renderHeader(gui, title, -1, screenWidth, PWPTheme.Spacing.MD);
    }

    public static void renderHeader(GuiGraphics gui, String title, int icon, int screenWidth) {
        renderHeader(gui, title, icon, screenWidth, PWPTheme.Spacing.MD);
    }

    public static void renderHeader(GuiGraphics gui, String title, int icon, int screenWidth, int topPadding) {
        var font = PWPTheme.Fonts.display();
        int iconW = icon >= 0 ? Spacing.ICON_SIZE + Spacing.XS : 0;
        Component t = UIDraw.ellipsize(font, Component.literal(title), screenWidth - 2 * Spacing.MD - iconW);
        int tw = font.width(t);
        int x = (screenWidth - tw - iconW) / 2;
        if (icon >= 0) PWPIcons.render(gui, icon, x, topPadding - 4);
        UIDraw.text(gui, font, t, x + iconW, topPadding, PWPTheme.Text.HEADING, true);
        UIDraw.accentBarH(
                gui,
                x + iconW,
                topPadding + font.lineHeight + Spacing.XXS,
                tw,
                Spacing.ACCENT_BAR,
                Styles.Control.ACCENT_BAR);
    }

    public static void renderFooter(GuiGraphics gui, String text, int screenWidth, int screenHeight) {
        var font = PWPTheme.Fonts.display();
        Component t = UIDraw.ellipsize(font, Component.literal(text), screenWidth - 2 * Spacing.MD);
        UIDraw.text(gui, font, t, screenWidth / 2 - font.width(t) / 2, screenHeight - 12, PWPTheme.Text.CAPTION, false);
    }

    public static void renderDivider(GuiGraphics gui, int x, int y, int width) {
        UIDraw.hline(gui, x, x + width, y, Styles.Glass.HEADER_DIVIDER);
    }
}

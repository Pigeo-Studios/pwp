package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Составные примитивы игрового UI поверх {@link UIDraw} (кит pwp-core-client): панели со
 * стеклом и шапкой, «плитки» с состояниями hover/selected/disabled, пилюли статуса, пустые
 * состояния, полоса прокрутки. Только GuiGraphics; цвета — {@link PWPTheme} / {@link WTheme}.
 */
@OnlyIn(Dist.CLIENT)
public final class WDraw {
    private WDraw() {}

    /** Иконка атласа warfare, окрашенная в {@code color}. */
    public static void icon(GuiGraphics g, WarfareIcon icon, int x, int y, int size, int color) {
        if (icon == null || size <= 0) return;
        int cell = WTheme.ICON_CELL;
        UIDraw.blitTinted(
                g,
                color,
                () -> g.blit(
                        WTheme.ICONS,
                        x,
                        y,
                        size,
                        size,
                        (float) icon.u(),
                        (float) icon.v(),
                        cell,
                        cell,
                        WTheme.ICONS_W,
                        WTheme.ICONS_H));
    }

    // ================================================================ панели

    /** Высота шапки панели для экрана данной высоты. */
    public static int headerHeight(int screenH) {
        return screenH < 300 ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
    }

    /**
     * Стеклянная панель с шапкой (иконка + заголовок + опциональный бейдж справа). Возвращает
     * область контента (под шапкой, с внутренним полем).
     */
    public static Rect panel(
            GuiGraphics g,
            Font font,
            Rect r,
            int headerH,
            WarfareIcon icon,
            Component title,
            Component badge,
            float o) {
        UIDraw.glass(g, r, o, false);
        if (title == null) return r.inset(Spacing.XS);
        Rect head = r.top(Math.min(headerH, r.h()));
        header(g, font, head, icon, title, badge, o);
        return r.inset(Spacing.XS, head.h() + Spacing.XS, Spacing.XS, Spacing.XS);
    }

    /** Шапка: тёмный градиент, акцентная полоса, иконка, заголовок «…», бейдж справа. */
    public static void header(
            GuiGraphics g, Font font, Rect head, WarfareIcon icon, Component title, Component badge, float o) {
        if (head.h() <= 0) return;
        UIDraw.gradientV(g, head, UIDraw.fade(Styles.Glass.HEADER_TOP, o), UIDraw.fade(Styles.Glass.HEADER_BOTTOM, o));
        UIDraw.accentBar(
                g, head.x(), head.y(), head.h(), Spacing.ACCENT_BAR, UIDraw.fade(Styles.Control.ACCENT_BAR, o));
        UIDraw.hline(g, head.x(), head.right(), head.bottom() - 1, UIDraw.fade(Styles.Glass.HEADER_DIVIDER, o));
        Rect inner = head.inset(Spacing.SM + Spacing.ACCENT_BAR, 0, Spacing.SM, 0);
        int right = inner.right();
        if (badge != null && !badge.getString().isEmpty()) {
            int bw = font.width(badge);
            UIDraw.text(
                    g,
                    font,
                    badge,
                    right - bw,
                    head.y() + (head.h() - font.lineHeight) / 2 + 1,
                    UIDraw.fade(PWPTheme.Text.MUTED, o),
                    false);
            right -= bw + Spacing.SM;
        }
        int tx = inner.x();
        int is = WTheme.ICON_ROW;
        if (icon != null && head.h() >= is) {
            icon(g, icon, tx, head.y() + (head.h() - is) / 2, is, UIDraw.fade(Colors.ACCENT, o));
            tx += is + Spacing.XS;
        }
        Component shown = UIDraw.ellipsize(font, title, right - tx);
        UIDraw.text(
                g,
                font,
                shown,
                tx,
                head.y() + (head.h() - font.lineHeight) / 2 + 1,
                UIDraw.fade(PWPTheme.Text.HEADING, o),
                true);
    }

    /** Подзаголовок секции внутри панели: акцентная засечка + текст + линия до правого края. */
    public static void section(GuiGraphics g, Font font, Rect row, Component title, int color, float o) {
        if (row.w() <= 0) return;
        int ty = row.y() + (row.h() - font.lineHeight) / 2 + 1;
        UIDraw.accentBar(g, row.x(), ty - 1, font.lineHeight - 1, Spacing.ACCENT_BAR, UIDraw.fade(color, o));
        int tx = row.x() + Spacing.ACCENT_BAR + Spacing.XS;
        Component t = UIDraw.ellipsize(font, title, row.right() - tx);
        UIDraw.text(g, font, t, tx, ty, UIDraw.fade(color, o), false);
        int lx = tx + font.width(t) + Spacing.XS;
        UIDraw.hline(g, lx, row.right(), ty + font.lineHeight / 2 - 1, UIDraw.fade(Styles.Glass.BORDER, o * 0.5F));
    }

    // ================================================================ плитки

    /**
     * Фон интерактивной плитки (строка списка, ячейка роли, карточка спавна) в стиле UIButton:
     * полупрозрачная заливка + блик + 1px обводка, акцентная полоса слева растёт при наведении
     * и выборе. {@code hover/selected} — значения анимаций 0..1, {@code enabled} — 0..1.
     */
    public static void tile(GuiGraphics g, Rect r, float hover, float selected, float enabled, float o) {
        if (r.w() <= 0 || r.h() <= 0) return;
        int bg = Colors.lerp(Styles.Control.BG, Styles.Control.BG_HOVER, hover);
        bg = Colors.lerp(bg, Styles.Control.BG_SELECTED, selected);
        bg = Colors.lerp(Styles.Control.BG_DISABLED, bg, enabled);
        int border = Colors.lerp(Styles.Control.BORDER, Styles.Control.BORDER_HOVER, hover);
        border = Colors.lerp(border, Styles.Control.BORDER_SELECTED, selected);
        border = Colors.lerp(Styles.Control.BORDER_DISABLED, border, enabled);
        UIDraw.rect(g, r, UIDraw.fade(bg, o));
        UIDraw.gradientV(
                g, r, UIDraw.fade(Styles.Control.SHEEN_TOP, o * enabled), UIDraw.fade(Styles.Control.SHEEN_BOTTOM, o));
        UIDraw.outline(g, r, UIDraw.fade(border, o));
        int bar = Math.round(Math.max(hover * Spacing.ACCENT_BAR, selected * Spacing.ACCENT_BAR_SELECTED) * enabled);
        if (bar > 0) UIDraw.accentBar(g, r.x(), r.y(), r.h(), bar, UIDraw.fade(Styles.Control.ACCENT_BAR, o));
    }

    /** Углублённая подложка (под иконку, поле ввода, превью). */
    public static void well(GuiGraphics g, Rect r, float o) {
        UIDraw.rect(g, r, UIDraw.fade(WTheme.WELL, o));
        UIDraw.outline(g, r, UIDraw.fade(WTheme.WELL_BORDER, o));
    }

    /** Поле ввода (подложка под EditBox без рамки): фокус подсвечивает обводку акцентом. */
    public static void field(GuiGraphics g, Rect r, boolean focused, float o) {
        UIDraw.rect(g, r, UIDraw.fade(WTheme.WELL, o));
        UIDraw.outline(g, r, UIDraw.fade(focused ? Colors.ACCENT_DIM : WTheme.WELL_BORDER, o));
        if (focused) UIDraw.hline(g, r.x(), r.right(), r.bottom() - 1, UIDraw.fade(Colors.ACCENT, o));
    }

    /** Ширина пилюли с текстом. */
    public static int pillWidth(Font font, Component text) {
        return font.width(text) + 2 * Spacing.XS;
    }

    /** Высота пилюли. */
    public static int pillHeight(Font font) {
        return font.lineHeight + Spacing.XXS;
    }

    /** Пилюля статуса: подложка цвета статуса низкой альфы, 1px обводка, текст цветом статуса. */
    public static int pill(GuiGraphics g, Font font, int x, int y, Component text, int color, float o) {
        int w = pillWidth(font, text);
        Rect r = Rect.of(x, y, w, pillHeight(font));
        UIDraw.rect(g, r, UIDraw.fade(Colors.withAlpha(color, WTheme.PILL_ALPHA), o));
        UIDraw.outline(g, r, UIDraw.fade(Colors.withAlpha(color, 0x80), o));
        UIDraw.text(g, font, text, x + Spacing.XS, y + 2, UIDraw.fade(color, o), false);
        return w;
    }

    /** Текст по вертикальному центру области с выравниванием и «…». Возвращает ширину. */
    public static int text(
            GuiGraphics g, Font font, Component text, Rect area, Align align, int color, boolean shadow, float o) {
        if (area.w() <= 0) return 0;
        Component t = UIDraw.ellipsize(font, text, area.w());
        int tw = font.width(t);
        int x = area.x() + align.offset(area.w(), tw);
        UIDraw.text(g, font, t, x, area.y() + (area.h() - font.lineHeight) / 2 + 1, UIDraw.fade(color, o), shadow);
        return tw;
    }

    /** Строки с переносом (не больше maxLines, хвост «…»). Возвращает занятую высоту. */
    public static int paragraph(GuiGraphics g, Font font, Component text, Rect area, int maxLines, int color, float o) {
        if (area.w() <= 0 || area.h() < font.lineHeight) return 0;
        int fit = Math.max(1, (area.h() + PWPTheme.Text.LINE_GAP) / (font.lineHeight + PWPTheme.Text.LINE_GAP));
        List<FormattedCharSequence> lines = UIDraw.wrap(font, text, area.w(), Math.min(maxLines, fit));
        UIDraw.lines(g, font, lines, area, false, UIDraw.fade(color, o), false);
        return UIDraw.linesHeight(font, lines.size());
    }

    // ================================================================ состояния

    /** Пустое состояние: иконка, заголовок и пояснение с переносом по центру области. */
    public static void emptyState(
            GuiGraphics g, Font font, Rect area, WarfareIcon icon, Component title, Component hint, float o) {
        if (area.w() <= 0 || area.h() < font.lineHeight || o <= 0F) return;
        int is = area.h() >= 3 * font.lineHeight + Spacing.ICON_SIZE_LG ? Spacing.ICON_SIZE_LG : Spacing.ICON_SIZE;
        boolean showIcon = icon != null && area.h() >= is + 2 * font.lineHeight;
        int maxW = Math.min(area.w() - 2 * Spacing.XS, Spacing.DIALOG_WIDTH_MIN);
        List<FormattedCharSequence> hintLines =
                hint == null ? List.of() : UIDraw.wrap(font, hint, Math.max(1, maxW), WTheme.EMPTY_HINT_LINES);
        int h = (showIcon ? is + Spacing.SM : 0)
                + font.lineHeight
                + (hintLines.isEmpty() ? 0 : Spacing.XS + UIDraw.linesHeight(font, hintLines.size()));
        int y = area.y() + Math.max(0, (area.h() - h) / 2);
        if (showIcon) {
            icon(g, icon, area.centerX() - is / 2, y, is, UIDraw.fade(PWPTheme.Text.CAPTION, o * 0.8F));
            y += is + Spacing.SM;
        }
        Component t = UIDraw.ellipsize(font, title, area.w());
        UIDraw.text(g, font, t, area.centerX() - font.width(t) / 2, y, UIDraw.fade(PWPTheme.Text.HEADING, o), true);
        y += font.lineHeight + Spacing.XS;
        UIDraw.lines(
                g,
                font,
                hintLines,
                Rect.of(area.x(), y, area.w(), Math.max(0, area.bottom() - y)),
                true,
                UIDraw.fade(PWPTheme.Text.MUTED, o),
                false);
    }

    // ================================================================ прочее

    /** Тонкая полоса прогресса: трек + заливка доли {@code frac} (0..1). */
    public static void progress(GuiGraphics g, Rect r, float frac, int track, int fill, float o) {
        UIDraw.rect(g, r, UIDraw.fade(track, o));
        int fw = Math.round(r.w() * Math.max(0F, Math.min(1F, frac)));
        if (fw > 0) UIDraw.rect(g, r.withWidth(fw), UIDraw.fade(fill, o));
    }

    /** Вертикальная полоса прокрутки у правого края области (только если контент не влезает). */
    public static void scrollbar(GuiGraphics g, Rect view, double content, double scroll, float o) {
        if (content <= view.h() || view.h() <= 0) return;
        Rect track = Rect.of(view.right() - WTheme.SCROLLBAR_W, view.y(), WTheme.SCROLLBAR_W, view.h());
        UIDraw.rect(g, track, UIDraw.fade(WTheme.SCROLL_TRACK, o));
        int th = UIMath.thumbHeight(content, view.h(), Spacing.SM);
        int ty = UIMath.thumbOffset(scroll, content, view.h(), th);
        UIDraw.rect(g, Rect.of(track.x(), track.y() + ty, track.w(), th), UIDraw.fade(WTheme.SCROLL_THUMB, o));
    }

    /** Горизонтальная пунктирная линия (dash/gap в px). */
    public static void dashedH(GuiGraphics g, int x1, int x2, int y, int dash, int gap, int color) {
        for (int x = x1; x < x2; x += dash + gap) g.fill(x, y, Math.min(x + dash, x2), y + 1, color);
    }

    /** Вертикальная пунктирная линия. */
    public static void dashedV(GuiGraphics g, int x, int y1, int y2, int dash, int gap, int color) {
        for (int y = y1; y < y2; y += dash + gap) g.fill(x, y, x + 1, Math.min(y + dash, y2), color);
    }

    /** Цвет команды по имени команды (BLUE/RED). */
    public static int teamColor(String team) {
        return team != null && team.toUpperCase().contains("BLUE") ? WTheme.TEAM_BLUE : WTheme.TEAM_RED;
    }
}

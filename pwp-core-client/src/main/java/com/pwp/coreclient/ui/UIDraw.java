package com.pwp.coreclient.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.gui.theme.PWPTheme.Textures;
import com.pwp.coreclient.ui.layout.NineSlice;
import com.pwp.coreclient.ui.layout.Rect;
import com.pwp.coreclient.ui.layout.TextFit;
import com.pwp.coreclient.ui.layout.Tiles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Примитивы рисования UI-кита. Только {@link GuiGraphics} (fill / fillGradient / blit /
 * drawString / pose), цвета — из {@link PWPTheme}. Все координаты целые (GUI scaled px):
 * 1px линии и стыки 9-slice остаются ровными на любом GUI scale.
 *
 * <p>Важные особенности ванили, которые здесь учтены:
 * <ul>
 *   <li>{@code Font.drawString} при альфе цвета &lt; 4 рисует текст НЕПРОЗРАЧНЫМ — {@link #text}
 *       такие кадры фейда пропускает;
 *   <li>{@code GuiGraphics.blit} сам не включает смешивание — полупрозрачные текстуры рисуются
 *       через {@link #blitTinted} (blend + shader color, затем сброс);
 *   <li>текстуры GUI по умолчанию GL_REPEAT: тайл рисуется ОДНИМ blit с UV за пределами
 *       текстуры ({@link #tile}) — повтор, а не растяжение.
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class UIDraw {
    private UIDraw() {}

    // ================================================================ базовые

    /** Скруглённая плашка с обводкой (border = 0 — без обводки). */
    public static void box(GuiGraphics g, int x, int y, int w, int h, int radius, int bg, int border) {
        if (Colors.alpha(bg) > 0) RoundedRect.fill(g, x, y, w, h, radius, bg);
        if (Colors.alpha(border) > 0) RoundedRect.border(g, x, y, w, h, radius, 1, border);
    }

    /** Прямоугольник (x, y, w, h). */
    public static void rect(GuiGraphics g, Rect r, int color) {
        if (Colors.alpha(color) == 0 || r.w() <= 0 || r.h() <= 0) return;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), color);
    }

    /** Обводка 1px строго внутри прямоугольника (углы не перекрываются — ровная альфа). */
    public static void outline(GuiGraphics g, Rect r, int color) {
        if (Colors.alpha(color) == 0 || r.w() <= 0 || r.h() <= 0) return;
        int t = Spacing.HAIRLINE;
        g.fill(r.x(), r.y(), r.right(), r.y() + t, color);
        if (r.h() > t) g.fill(r.x(), r.bottom() - t, r.right(), r.bottom(), color);
        if (r.h() > 2 * t) {
            g.fill(r.x(), r.y() + t, r.x() + t, r.bottom() - t, color);
            if (r.w() > t) g.fill(r.right() - t, r.y() + t, r.right(), r.bottom() - t, color);
        }
    }

    /** Горизонтальная 1px линия. */
    public static void hline(GuiGraphics g, int x1, int x2, int y, int color) {
        if (x2 > x1) g.fill(x1, y, x2, y + Spacing.HAIRLINE, color);
    }

    /** Вертикальная 1px линия. */
    public static void vline(GuiGraphics g, int x, int y1, int y2, int color) {
        if (y2 > y1) g.fill(x, y1, x + Spacing.HAIRLINE, y2, color);
    }

    // ================================================================ градиенты

    /** Вертикальный градиент: {@code top} сверху -> {@code bottom} снизу. */
    public static void gradientV(GuiGraphics g, Rect r, int top, int bottom) {
        if (r.w() <= 0 || r.h() <= 0 || (Colors.alpha(top) == 0 && Colors.alpha(bottom) == 0)) return;
        g.fillGradient(r.x(), r.y(), r.right(), r.bottom(), top, bottom);
    }

    /**
     * Горизонтальный градиент: {@code left} слева -> {@code right} справа. В GuiGraphics есть
     * только вертикальный — рисуем его в повёрнутой на -90° системе координат (одна квадра,
     * без полос и без сотен fill).
     */
    public static void gradientH(GuiGraphics g, Rect r, int left, int right) {
        if (r.w() <= 0 || r.h() <= 0 || (Colors.alpha(left) == 0 && Colors.alpha(right) == 0)) return;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(r.x(), r.bottom(), 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-90F));
        // локальные (u, v) -> экран (x + v, bottom - u); градиент идёт по v = по экранному X
        g.fillGradient(0, 0, r.h(), r.w(), left, right);
        pose.popPose();
    }

    /**
     * Виньетка экрана: четыре градиента от краёв внутрь на {@code fraction} меньшей стороны.
     * Углы затемняются сильнее (наложение) — как у настоящей виньетки. Текстурой не делается:
     * растянутая текстура при nearest-фильтрации GUI даёт ступени.
     */
    public static void vignette(GuiGraphics g, int w, int h, int edge, float fraction) {
        int band = Math.max(1, Math.round(Math.min(w, h) * fraction));
        int clear = Colors.withAlpha(edge, 0);
        gradientV(g, Rect.of(0, 0, w, band), edge, clear);
        gradientV(g, Rect.of(0, h - band, w, band), clear, edge);
        gradientH(g, Rect.of(0, 0, band, h), edge, clear);
        gradientH(g, Rect.of(w - band, 0, band, h), clear, edge);
    }

    // ================================================================ текстуры

    /** Выполнить рисование текстур с тинтом ARGB (shader color) и смешиванием. */
    public static void blitTinted(GuiGraphics g, int argb, Runnable draw) {
        if (Colors.alpha(argb) == 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(
                Colors.red(argb) / 255F,
                Colors.green(argb) / 255F,
                Colors.blue(argb) / 255F,
                Colors.alpha(argb) / 255F);
        draw.run();
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    /**
     * Тайловая текстура по области ОДНИМ blit с повтором (GL_REPEAT), узор прибит к экрану
     * (соседние панели стыкуются, при сдвиге панели узор не плывёт). Без растяжения.
     */
    public static void tile(GuiGraphics g, ResourceLocation tex, Rect r, int tileSize, int tint) {
        if (r.w() <= 0 || r.h() <= 0) return;
        int u = Tiles.anchoredOffset(r.x(), tileSize);
        int v = Tiles.anchoredOffset(r.y(), tileSize);
        blitTinted(g, tint, () -> g.blit(tex, r.x(), r.y(), (float) u, (float) v, r.w(), r.h(), tileSize, tileSize));
    }

    /** Тонкий шум поверх области. */
    public static void noise(GuiGraphics g, Rect r, int tint) {
        tile(g, Textures.NOISE, r, Textures.TILE_SIZE, tint);
    }

    /** Скан-лайны (каждая вторая GUI-строка) поверх области. */
    public static void scanlines(GuiGraphics g, Rect r, int tint) {
        tile(g, Textures.SCANLINES, r, Textures.TILE_SIZE, tint);
    }

    /**
     * 9-slice: углы 1:1, края/центр — повтором ({@link NineSlice.Mode#TILE}) или растяжением
     * однородных краёв ({@link NineSlice.Mode#STRETCH_EDGES}). Целые координаты кусков —
     * без щелей в 1px на любом GUI scale.
     */
    public static void nineSlice(
            GuiGraphics g,
            ResourceLocation tex,
            Rect dst,
            int texW,
            int texH,
            int corner,
            NineSlice.Mode mode,
            boolean center,
            int tint) {
        List<NineSlice.Piece> pieces = NineSlice.pieces(dst, texW, texH, corner, mode, center);
        if (pieces.isEmpty()) return;
        blitTinted(g, tint, () -> {
            for (NineSlice.Piece p : pieces) {
                if (p.oneToOne()) {
                    g.blit(tex, p.x(), p.y(), (float) p.u(), (float) p.v(), p.w(), p.h(), texW, texH);
                } else {
                    g.blit(tex, p.x(), p.y(), p.w(), p.h(), (float) p.u(), (float) p.v(), p.uw(), p.vh(), texW, texH);
                }
            }
        });
    }

    /** Мягкая тень под прямоугольником (9-slice shadow.png, выходит за край). */
    public static void shadow(GuiGraphics g, Rect r, float opacity) {
        Rect s = r.outset(Spacing.SHADOW_SPREAD).translate(0, Spacing.SHADOW_DROP);
        nineSlice(
                g,
                Textures.SHADOW,
                s,
                Textures.SHADOW_SIZE,
                Textures.SHADOW_SIZE,
                Spacing.SHADOW_CORNER,
                NineSlice.Mode.STRETCH_EDGES,
                false,
                fade(Styles.Glass.SHADOW_TINT, opacity));
    }

    /** Рамка с уголками-скобами (9-slice frame.png), центр не рисуется. */
    public static void frame(GuiGraphics g, Rect r, int tint) {
        nineSlice(
                g,
                Textures.FRAME,
                r,
                Textures.FRAME_SIZE,
                Textures.FRAME_SIZE,
                Spacing.FRAME_CORNER,
                NineSlice.Mode.STRETCH_EDGES,
                false,
                tint);
    }

    /** Иконка атласа, окрашенная в {@code color}. size 16 — 1:1, кратные — чётко. */
    public static void icon(GuiGraphics g, UIIcon icon, int x, int y, int size, int color) {
        if (icon == null || size <= 0) return;
        int cell = Textures.ICON_CELL;
        blitTinted(
                g,
                color,
                () -> g.blit(
                        Textures.ICONS,
                        x,
                        y,
                        size,
                        size,
                        (float) icon.u(),
                        (float) icon.v(),
                        cell,
                        cell,
                        Textures.ICONS_W,
                        Textures.ICONS_H));
    }

    // ================================================================ панели

    /**
     * «Стеклянная» панель: тень, полупрозрачная заливка, вертикальный градиент-блик, шум,
     * верхний 1px хайлайт и рамка со скобами в углах. Квадратные углы (тактический стиль).
     *
     * @param opacity 0..1 — для фейда появления
     * @param strong true — плотнее (для диалогов поверх яркого фона)
     */
    public static void glass(GuiGraphics g, Rect r, float opacity, boolean strong) {
        if (r.w() <= 0 || r.h() <= 0 || opacity <= 0F) return;
        shadow(g, r, opacity);
        rect(g, r, fade(strong ? Styles.Glass.BG_STRONG : Styles.Glass.BG, opacity));
        gradientV(g, r, fade(Styles.Glass.SHEEN_TOP, opacity), fade(Styles.Glass.SHEEN_BOTTOM, opacity));
        noise(g, r, fade(Styles.Glass.NOISE_TINT, opacity));
        hline(g, r.x() + 1, r.right() - 1, r.y() + 1, fade(Styles.Glass.HIGHLIGHT, opacity));
        frame(g, r, fade(Styles.Glass.FRAME_TINT, opacity));
    }

    /**
     * Шапка панели: тёмный градиент, акцентная полоса слева, иконка и заголовок (обрезка «…»),
     * 1px разделитель снизу. Возвращает область шапки.
     */
    public static void header(
            GuiGraphics g, Font font, Rect header, UIIcon icon, Component title, int titleColor, float opacity) {
        if (header.h() <= 0) return;
        gradientV(g, header, fade(Styles.Glass.HEADER_TOP, opacity), fade(Styles.Glass.HEADER_BOTTOM, opacity));
        accentBar(g, header.x(), header.y(), header.h(), Spacing.ACCENT_BAR, fade(Styles.Control.ACCENT_BAR, opacity));
        hline(g, header.x(), header.right(), header.bottom() - 1, fade(Styles.Glass.HEADER_DIVIDER, opacity));
        Rect inner = header.inset(Spacing.SM + Spacing.ACCENT_BAR, 0, Spacing.SM, 0);
        int textX = inner.x();
        if (icon != null) {
            int is = Spacing.ICON_SIZE;
            if (header.h() >= is) {
                icon(g, icon, inner.x(), header.y() + (header.h() - is) / 2, is, fade(titleColor, opacity));
                textX += is + Spacing.XS;
            }
        }
        int maxW = inner.right() - textX;
        Component shown = ellipsize(font, title, maxW);
        text(
                g,
                font,
                shown,
                textX,
                header.y() + (header.h() - font.lineHeight) / 2 + 1,
                fade(titleColor, opacity),
                true);
    }

    /** Вертикальная акцентная полоса (активный элемент). */
    public static void accentBar(GuiGraphics g, int x, int y, int h, int thickness, int color) {
        if (thickness <= 0 || h <= 0 || Colors.alpha(color) == 0) return;
        g.fill(x, y, x + thickness, y + h, color);
    }

    /** Горизонтальная акцентная полоса (подчёркивание вкладки). */
    public static void accentBarH(GuiGraphics g, int x, int y, int w, int thickness, int color) {
        if (thickness <= 0 || w <= 0 || Colors.alpha(color) == 0) return;
        g.fill(x, y, x + w, y + thickness, color);
    }

    // ================================================================ текст

    /** Текст с проверкой альфы (альфа &lt; 4 у ванильного шрифта = непрозрачный — пропускаем). */
    public static void text(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
        if (Colors.alpha(color) < 4) return;
        g.drawString(font, text, x, y, color, shadow);
    }

    public static void text(
            GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if (Colors.alpha(color) < 4) return;
        g.drawString(font, text, x, y, color, shadow);
    }

    /** Текст по центру прямоугольника с тенью. */
    public static void centered(GuiGraphics g, Font font, Component text, int cx, int cy, int color) {
        int tw = font.width(text);
        text(g, font, text, cx - tw / 2, cy - font.lineHeight / 2, color, true);
    }

    /**
     * Текст, увеличенный в ЦЕЛОЕ число раз (ванильный пиксельный шрифт остаётся чётким).
     * (x, y) — левый верх в обычных координатах.
     */
    public static void textScaled(
            GuiGraphics g, Font font, Component text, int x, int y, int scale, int color, boolean shadow) {
        if (Colors.alpha(color) < 4) return;
        int s = Math.max(1, scale);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(s, s, 1F);
        g.drawString(font, text, 0, 0, color, shadow);
        pose.popPose();
    }

    /** Текст, обрезанный многоточием по ширине (стиль сохраняется). */
    public static Component ellipsize(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String s = TextFit.ellipsize(text.getString(), maxWidth, font::width);
        return Component.literal(s).withStyle(text.getStyle());
    }

    /**
     * Перенос по ширине (ванильный {@code font.split} — учитывает стили), не больше
     * {@code maxLines} строк: лишний хвост сворачивается в последнюю строку с «…».
     */
    public static List<FormattedCharSequence> wrap(Font font, Component text, int maxWidth, int maxLines) {
        int w = Math.max(1, maxWidth);
        List<FormattedCharSequence> lines = font.split(text, w);
        if (maxLines <= 0 || lines.size() <= maxLines) return lines;
        List<String> plain = TextFit.wrap(text.getString(), w, maxLines, font::width);
        List<FormattedCharSequence> out = new ArrayList<>(plain.size());
        for (String line : plain) {
            out.add(Component.literal(line).withStyle(text.getStyle()).getVisualOrderText());
        }
        return out;
    }

    /** Высота блока из {@code lines} строк с межстрочным интервалом темы. */
    public static int linesHeight(Font font, int lines) {
        if (lines <= 0) return 0;
        return lines * font.lineHeight + (lines - 1) * PWPTheme.Text.LINE_GAP;
    }

    /** Нарисовать строки сверху вниз, выравнивая по центру (center=true) или слева от x. */
    public static void lines(
            GuiGraphics g,
            Font font,
            List<FormattedCharSequence> lines,
            Rect area,
            boolean center,
            int color,
            boolean shadow) {
        int y = area.y();
        for (FormattedCharSequence line : lines) {
            if (y + font.lineHeight > area.bottom() + 1) break; // не вылезать за область
            int x = center ? area.centerX() - font.width(line) / 2 : area.x();
            text(g, font, line, x, y, color, shadow);
            y += font.lineHeight + PWPTheme.Text.LINE_GAP;
        }
    }

    // ================================================================ цвет

    /** Цвет с прозрачностью, умноженной на alpha (0..1) — для фейдов. */
    public static int fade(int color, float alpha) {
        return Colors.multiplyAlpha(color, Math.max(0F, Math.min(1F, alpha)));
    }
}

package com.pwp.coreclient.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.ui.UIDraw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Примитивы HUD матча (этап 2): тёмная стеклянная плашка, полоса с делениями и хвостом урона,
 * текст с защитой альфы, иконка с тинтом. Только int-координаты и примитивы — БЕЗ аллокаций в
 * кадре (никаких лямбд/Rect/Component внутри). Цвета — {@link HudTheme}.
 *
 * <p>Масштаб HUD: элемент берёт свою точку из {@code HudZones} (GUI-пиксели), вызывает {@link
 * #begin} и дальше рисует в базовых единицах — шрифт, предметы и рамки масштабируются вместе.
 */
@OnlyIn(Dist.CLIENT)
public final class HudDraw {
    private HudDraw() {}

    // ================================================================ масштаб

    /** Начать элемент в точке (x, y) GUI-пикселей с масштабом HUD; парный {@link #end}. */
    public static void begin(GuiGraphics g, float x, float y, float scale) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0F);
        if (scale != 1F) pose.scale(scale, scale, 1F);
    }

    public static void end(GuiGraphics g) {
        g.pose().popPose();
    }

    // ================================================================ плашка

    /**
     * Подложка плашки: стекло (альфа × {@code o} × настройка «Фон HUD-плашек»), блик сверху и
     * рамка 1px внутри области.
     */
    public static void plateBg(GuiGraphics g, int x, int y, int w, int h, float o) {
        if (w <= 0 || h <= 0 || o <= 0F) return;
        float bg = o * Hud.plateOpacity();
        g.fill(x, y, x + w, y + h, UIDraw.fade(HudTheme.PLATE_BG, bg));
        g.fill(x + 1, y, x + w - 1, y + 1, UIDraw.fade(HudTheme.PLATE_HIGHLIGHT, bg));
        frame(g, x, y, w, h, UIDraw.fade(HudTheme.PLATE_BORDER, bg));
    }

    /** Плашка с акцентной полоской слева. */
    public static void plate(GuiGraphics g, int x, int y, int w, int h, int accent, float o) {
        plateBg(g, x, y, w, h, o);
        stripe(g, x, y, h, accent, o);
    }

    /** Акцентная полоска слева (не зависит от настройки фона — это сигнал, а не подложка). */
    public static void stripe(GuiGraphics g, int x, int y, int h, int accent, float o) {
        int c = UIDraw.fade(accent, o);
        if (Colors.alpha(c) == 0) return;
        g.fill(x, y, x + HudTheme.PLATE_STRIPE, y + h, c);
    }

    /** Рамка 1px по краю области (внутри). */
    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (Colors.alpha(color) == 0 || w <= 1 || h <= 1) return;
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    // ================================================================ полоса

    /**
     * Полоса с делениями: трек, хвост урона от {@code frac} до {@code trail} (светлый), заливка
     * {@code frac}, деления на {@code segments} частей (≤1 — без делений), рамка снаружи.
     * {@code trail} < frac — хвоста нет.
     */
    public static void bar(
            GuiGraphics g, int x, int y, int w, int h, float frac, float trail, int segments, int fill, float o) {
        if (w <= 0 || h <= 0 || o <= 0F) return;
        g.fill(x, y, x + w, y + h, UIDraw.fade(HudTheme.BAR_TRACK, o));
        int fw = width(w, frac);
        int tw = width(w, trail);
        if (tw > fw) g.fill(x + fw, y, x + tw, y + h, UIDraw.fade(HudTheme.BAR_TRAIL, o));
        if (fw > 0) g.fill(x, y, x + fw, y + h, UIDraw.fade(fill, o));
        if (segments > 1) {
            int tick = UIDraw.fade(HudTheme.BAR_TICK, o);
            for (int i = 1; i < segments; i++) {
                int tx = x + (int) ((long) w * i / segments);
                g.fill(tx, y, tx + 1, y + h, tick);
            }
        }
        int b = UIDraw.fade(HudTheme.BAR_BORDER, o);
        if (Colors.alpha(b) != 0) {
            g.fill(x - 1, y - 1, x + w + 1, y, b);
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, b);
            g.fill(x - 1, y, x, y + h, b);
            g.fill(x + w, y, x + w + 1, y + h, b);
        }
    }

    private static int width(int w, float frac) {
        if (Float.isNaN(frac) || frac <= 0F) return 0;
        return Math.round(w * Math.min(1F, frac));
    }

    // ================================================================ текст

    /** Строка с защитой альфы (&lt; 4 у ванильного шрифта = непрозрачный текст — пропускаем). */
    public static int text(GuiGraphics g, Font font, String text, int x, int y, int color, boolean shadow) {
        if (text == null || Colors.alpha(color) < 4) return 0;
        return g.drawString(font, text, x, y, color, shadow);
    }

    /** Строка, уменьшенная/увеличенная в {@code scale} раз; (x, y) — левый верх. */
    public static void textScaled(GuiGraphics g, Font font, String text, float x, float y, float scale, int color) {
        if (text == null || Colors.alpha(color) < 4) return;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0F);
        pose.scale(scale, scale, 1F);
        g.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }

    // ================================================================ иконка

    /**
     * Иконка из атласа с тинтом (без лямбды — без аллокации в кадре): ячейка {@code u,v} размера
     * {@code cell} атласа {@code texW x texH} в квадрат {@code size}.
     */
    public static void icon(
            GuiGraphics g,
            ResourceLocation atlas,
            int u,
            int v,
            int cell,
            int texW,
            int texH,
            int x,
            int y,
            int size,
            int color) {
        if (size <= 0 || Colors.alpha(color) == 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
                Colors.red(color) / 255F,
                Colors.green(color) / 255F,
                Colors.blue(color) / 255F,
                Colors.alpha(color) / 255F);
        g.blit(atlas, x, y, size, size, (float) u, (float) v, cell, cell, texW, texH);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }
}

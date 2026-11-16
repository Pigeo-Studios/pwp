package com.pigeostudios.pwp.warfare.client.gui.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Базовые слои тактической карты, рисуются СТРОГО по порядку (без наложения одинаковых слоёв —
 * нет мерцания/z-fighting):
 * <ol>
 *   <li>{@link #background} — подложка вне текстуры карты;
 *   <li>{@link #tiles} — текстура карты матча (сглаживание только на время blit);
 *   <li>{@link #grid} — сетка 300 (и 100 при приближении) только внутри играбельной зоны;
 *   <li>{@link #bounds} — ОДНА маска вне зоны из непересекающихся полос + градиентный край и
 *       светящаяся пунктирная линия границы;
 *   <li>{@link #labels} — подписи колонок A–Z сверху и строк 1–N слева (только квадраты зоны).
 * </ol>
 * HUD поверх (координаты, масштаб, компас, рамка) — {@link MapHud}.
 */
@OnlyIn(Dist.CLIENT)
public final class MapLayers {
    private MapLayers() {}

    private static final Map<String, ResourceLocation> MAP_TEXTURES = new HashMap<>();
    /** Размер текстуры карты (как раньше: 1024x1024 на весь квадрат карты матча). */
    private static final int MAP_TEX = 1024;

    public static void background(GuiGraphics g, MapView v) {
        UIDraw.rect(g, v.area(), WTheme.MAP_BG);
        UIDraw.noise(g, v.area(), WTheme.MAP_BG_NOISE);
    }

    public static ResourceLocation mapTexture() {
        String img = ClientData.currentMapImage;
        if (img == null || img.isEmpty()) img = "map1";
        return MAP_TEXTURES.computeIfAbsent(
                img, k -> new ResourceLocation("pwpwarfare", "textures/gui/maps/" + k + ".png"));
    }

    public static void tiles(GuiGraphics g, MapView v) {
        int s = ClientData.mapSizeBlocks;
        if (s <= 0) return;
        ResourceLocation tex = mapTexture();
        int x0 = (int) Math.floor(v.sx(ClientData.mapCenterX - s / 2.0));
        int y0 = (int) Math.floor(v.sy(ClientData.mapCenterZ - s / 2.0));
        int x1 = (int) Math.floor(v.sx(ClientData.mapCenterX + s / 2.0));
        int y1 = (int) Math.floor(v.sy(ClientData.mapCenterZ + s / 2.0));
        if (x1 <= x0 || y1 <= y0) return;
        var t = Minecraft.getInstance().getTextureManager().getTexture(tex);
        t.setFilter(true, false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(tex, x0, y0, x1 - x0, y1 - y0, 0, 0, MAP_TEX, MAP_TEX, MAP_TEX, MAP_TEX);
        t.setFilter(false, false);
    }

    /** Сетка внутри пересечения области карты и играбельной зоны. */
    public static void grid(GuiGraphics g, MapView v) {
        Rect clip = intersect(v.area(), v.playableScreen());
        if (clip.w() <= 0 || clip.h() <= 0) return;
        if (v.bpp() <= WTheme.MAP_SUBGRID_MAX_BPP) {
            float a =
                    (float) Math.min(1, Math.max(0, (WTheme.MAP_SUBGRID_MAX_BPP - v.bpp()) / WTheme.MAP_SUBGRID_FADE));
            lines(g, v, clip, MapView.SUB_CELL, UIDraw.fade(WTheme.MAP_GRID_MINOR, a), false);
        }
        lines(g, v, clip, MapView.CELL, WTheme.MAP_GRID_MAJOR, true);
    }

    private static void lines(GuiGraphics g, MapView v, Rect clip, long step, int color, boolean major) {
        long ox = MapView.gridOriginX();
        long oz = MapView.gridOriginZ();
        long xs = Math.floorDiv((long) v.worldX(clip.x()) - ox, step) * step + ox;
        for (long wx = xs; v.sx(wx) <= clip.right(); wx += step) {
            int x = (int) Math.floor(v.sx(wx));
            if (x < clip.x() || x >= clip.right()) continue;
            if (major) UIDraw.vline(g, x, clip.y(), clip.bottom(), color);
            else WDraw.dashedV(g, x, clip.y(), clip.bottom(), WTheme.MAP_DASH, WTheme.MAP_DASH, color);
        }
        long zs = Math.floorDiv((long) v.worldZ(clip.y()) - oz, step) * step + oz;
        for (long wz = zs; v.sy(wz) <= clip.bottom(); wz += step) {
            int y = (int) Math.floor(v.sy(wz));
            if (y < clip.y() || y >= clip.bottom()) continue;
            if (major) UIDraw.hline(g, clip.x(), clip.right(), y, color);
            else WDraw.dashedH(g, clip.x(), clip.right(), y, WTheme.MAP_DASH, WTheme.MAP_DASH, color);
        }
    }

    /**
     * Затемнение всего, что вне играбельной зоны, как граница в Borderlands-подобных играх:
     * сплошная маска + градиентная полоса у края + светящаяся пунктирная линия. Маска — четыре
     * НЕПЕРЕСЕКАЮЩИЕСЯ прямоугольника (верх/низ во всю ширину, лево/право между ними), полоса
     * градиента — так же; ничего не рисуется дважды.
     */
    public static void bounds(GuiGraphics g, MapView v, long timeMs) {
        Rect a = v.area();
        Rect p = v.playableScreen();
        int band = WTheme.MAP_EDGE_BAND;
        // внешняя граница полосы
        Rect outer = p.outset(band);
        int mask = WTheme.MAP_OUT_MASK;
        int edge = WTheme.MAP_OUT_EDGE;
        // 1) сплошная маска вне outer
        fillClipped(g, a, Rect.of(a.x(), a.y(), a.w(), outer.y() - a.y()), mask);
        fillClipped(g, a, Rect.of(a.x(), outer.bottom(), a.w(), a.bottom() - outer.bottom()), mask);
        fillClipped(g, a, Rect.of(a.x(), outer.y(), outer.x() - a.x(), outer.h()), mask);
        fillClipped(g, a, Rect.of(outer.right(), outer.y(), a.right() - outer.right(), outer.h()), mask);
        // 2) градиентная полоса между p и outer: от края (светлее) к маске
        gradClipped(g, a, Rect.of(outer.x(), outer.y(), outer.w(), band), mask, edge, true);
        gradClipped(g, a, Rect.of(outer.x(), p.bottom(), outer.w(), band), edge, mask, true);
        gradClipped(g, a, Rect.of(outer.x(), p.y(), band, p.h()), mask, edge, false);
        gradClipped(g, a, Rect.of(p.right(), p.y(), band, p.h()), edge, mask, false);
        // 3) линия границы: свечение снаружи + пунктир «бегущих муравьёв»
        if (p.w() <= 0 || p.h() <= 0) return;
        int glow = WTheme.MAP_BORDER_GLOW;
        g.enableScissor(a.x(), a.y(), a.right(), a.bottom());
        UIDraw.outline(g, p.outset(1), glow);
        UIDraw.outline(g, p.outset(2), Colors.multiplyAlpha(glow, 0.5F));
        UIDraw.outline(g, p, WTheme.MAP_BORDER_LINE);
        int period = WTheme.MAP_BORDER_DASH * 2;
        int phase = (int) ((timeMs / WTheme.MAP_BORDER_DASH_MS) % period);
        dashedOutline(g, p, WTheme.MAP_BORDER_DASH, phase, WTheme.MAP_BORDER_DASH_COLOR);
        g.disableScissor();
    }

    private static void dashedOutline(GuiGraphics g, Rect r, int dash, int phase, int color) {
        int period = dash * 2;
        for (int x = r.x() - phase; x < r.right(); x += period) {
            int x0 = Math.max(r.x(), x), x1 = Math.min(r.right(), x + dash);
            if (x1 > x0) {
                g.fill(x0, r.y(), x1, r.y() + 1, color);
                g.fill(x0, r.bottom() - 1, x1, r.bottom(), color);
            }
        }
        for (int y = r.y() - phase; y < r.bottom(); y += period) {
            int y0 = Math.max(r.y() + 1, y), y1 = Math.min(r.bottom() - 1, y + dash);
            if (y1 > y0) {
                g.fill(r.x(), y0, r.x() + 1, y1, color);
                g.fill(r.right() - 1, y0, r.right(), y1, color);
            }
        }
    }

    private static Rect intersect(Rect a, Rect b) {
        int x0 = Math.max(a.x(), b.x()), y0 = Math.max(a.y(), b.y());
        int x1 = Math.min(a.right(), b.right()), y1 = Math.min(a.bottom(), b.bottom());
        return Rect.of(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    private static void fillClipped(GuiGraphics g, Rect clip, Rect r, int color) {
        Rect c = intersect(clip, r);
        if (c.w() > 0 && c.h() > 0) UIDraw.rect(g, c, color);
    }

    /** Градиент в r, обрезанный clip: цвета пересчитываются по доле обрезки (без растяжения). */
    private static void gradClipped(GuiGraphics g, Rect clip, Rect r, int from, int to, boolean vertical) {
        Rect c = intersect(clip, r);
        if (c.w() <= 0 || c.h() <= 0) return;
        int len = vertical ? r.h() : r.w();
        if (len <= 0) return;
        float t0 = vertical ? (c.y() - r.y()) / (float) len : (c.x() - r.x()) / (float) len;
        float t1 = vertical ? (c.bottom() - r.y()) / (float) len : (c.right() - r.x()) / (float) len;
        int c0 = Colors.lerp(from, to, t0);
        int c1 = Colors.lerp(from, to, t1);
        if (vertical) UIDraw.gradientV(g, c, c0, c1);
        else UIDraw.gradientH(g, c, c0, c1);
    }

    /** Подписи колонок (сверху) и строк (слева) по центрам квадратов зоны. */
    public static void labels(GuiGraphics g, Font font, MapView v) {
        Rect a = v.area();
        Rect clip = intersect(a, v.playableScreen());
        if (clip.w() <= 0 || clip.h() <= 0) return;
        long ox = MapView.gridOriginX();
        long oz = MapView.gridOriginZ();
        int lh = font.lineHeight + Spacing.XXS;
        long c0 = Math.floorDiv((long) v.worldX(clip.x()) - ox, MapView.CELL);
        long c1 = Math.floorDiv((long) v.worldX(clip.right()) - ox, MapView.CELL);
        for (long i = c0; i <= c1; i++) {
            int px = (int) v.sx(ox + i * MapView.CELL + MapView.CELL / 2);
            String t = MapView.column(i);
            int bw = font.width(t) + 2 * Spacing.XS;
            if ("?".equals(t) || px - bw / 2 < clip.x() || px + bw / 2 > clip.right()) continue;
            label(g, font, Rect.of(px - bw / 2, a.y() + Spacing.XXS, bw, lh), t);
        }
        long r0 = Math.floorDiv((long) v.worldZ(clip.y()) - oz, MapView.CELL);
        long r1 = Math.floorDiv((long) v.worldZ(clip.bottom()) - oz, MapView.CELL);
        for (long i = r0; i <= r1; i++) {
            int py = (int) v.sy(oz + i * MapView.CELL + MapView.CELL / 2);
            String t = String.valueOf(i + 1);
            int bw = font.width(t) + 2 * Spacing.XS;
            if (i < 0 || py - lh / 2 < clip.y() + lh || py + lh / 2 > clip.bottom()) continue;
            label(g, font, Rect.of(a.x() + Spacing.XXS, py - lh / 2, bw, lh), t);
        }
    }

    private static void label(GuiGraphics g, Font font, Rect r, String t) {
        UIDraw.rect(g, r, WTheme.MAP_LABEL_BG);
        UIDraw.outline(g, r, WTheme.MAP_LABEL_BORDER);
        UIDraw.text(g, font, Component.literal(t), r.x() + Spacing.XS, r.y() + 2, WTheme.MAP_LABEL_TEXT, false);
    }

    /** Иконка-подсказка «нет карты» (неизвестный размер карты матча). */
    public static void noMap(GuiGraphics g, Font font, MapView v) {
        if (ClientData.mapSizeBlocks > 0) return;
        WDraw.emptyState(
                g, font, v.area(), WarfareIcon.MAP, Component.translatable("gui.pwpwarfare.map.no_data"), null, 1F);
    }
}

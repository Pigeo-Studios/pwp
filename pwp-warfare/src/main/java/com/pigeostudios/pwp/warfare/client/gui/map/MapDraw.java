package com.pigeostudios.pwp.warfare.client.gui.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/** Примитивы объектов карты: кольцо прогресса (точки захвата) и подпись-плашка у иконки. */
@OnlyIn(Dist.CLIENT)
public final class MapDraw {
    private MapDraw() {}

    /** Кольцевой сектор [a0, a1] в градусах от «вверх» по часовой. */
    public static void arc(GuiGraphics g, float cx, float cy, float r0, float r1, double a0, double a1, int argb) {
        if (Colors.alpha(argb) == 0 || r1 <= r0 || a1 <= a0) return;
        Matrix4f m = g.pose().last().pose();
        float al = Colors.alpha(argb) / 255F;
        float r = Colors.red(argb) / 255F;
        float gr = Colors.green(argb) / 255F;
        float b = Colors.blue(argb) / 255F;
        int steps = Math.max(6, (int) Math.ceil((a1 - a0) / 8.0));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            double ang = Math.toRadians(a0 + (a1 - a0) * i / steps);
            float sx = (float) Math.sin(ang);
            float sy = (float) -Math.cos(ang);
            buf.vertex(m, cx + sx * r1, cy + sy * r1, 0).color(r, gr, b, al).endVertex();
            buf.vertex(m, cx + sx * r0, cy + sy * r0, 0).color(r, gr, b, al).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    /** Кольцо прогресса: трек + заполненная доля {@code progress} цветом {@code color}. */
    public static void progressRing(GuiGraphics g, float cx, float cy, float radius, float progress, int color) {
        float r0 = radius - WTheme.MAP_RING_W;
        arc(g, cx, cy, r0, radius, 0, 360, WTheme.MAP_RING_TRACK);
        float p = Math.max(0F, Math.min(1F, progress));
        if (p > 0F) arc(g, cx, cy, r0, radius, 0, 360.0 * p, color);
    }

    /** Подпись-плашка по центру под точкой (x — центр, y — верх плашки). */
    public static void label(GuiGraphics g, Font font, Component text, int cx, int y, int color) {
        int w = font.width(text) + 2 * Spacing.XXS + 2;
        Rect r = Rect.of(cx - w / 2, y, w, font.lineHeight + 1);
        UIDraw.rect(g, r, WTheme.MAP_LABEL_BG);
        UIDraw.text(g, font, text, r.x() + Spacing.XXS + 1, r.y() + 1, color, false);
    }
}

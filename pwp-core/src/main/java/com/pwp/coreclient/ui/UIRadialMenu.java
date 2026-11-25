package com.pwp.coreclient.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Радиальное меню (как в Squad): кольцо секторов вокруг центра экрана, сектор под курсором
 * подсвечивается, клик/отпускание клавиши выполняет действие. Радиусы — доли меньшей стороны
 * экрана ({@code Styles.Radial}), поэтому меню одинаково выглядит на 720p и 1080p при любом GUI scale.
 *
 * <p>Это ОДНО меню для всех объектов: пункты даёт провайдер действий (этап 4 заменит им
 * 8 отдельных радиалок).
 */
@OnlyIn(Dist.CLIENT)
public class UIRadialMenu extends UIWidget {
    private static final int ARC_STEPS = 24;

    private final List<UIAction> actions = new ArrayList<>();
    private Component title = Component.empty();
    private final UIAnim open = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);
    private final List<UIAnim> sectorHover = new ArrayList<>();

    public UIRadialMenu(int screenW, int screenH) {
        super(0, 0, screenW, screenH);
    }

    public UIRadialMenu actions(Component title, List<UIAction> list) {
        this.title = title;
        actions.clear();
        actions.addAll(list);
        sectorHover.clear();
        for (int i = 0; i < list.size(); i++) {
            sectorHover.add(new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC));
        }
        open.snap(0F);
        open.to(1F);
        return this;
    }

    private int cx() {
        return x + width / 2;
    }

    private int cy() {
        return y + height / 2;
    }

    private float inner() {
        return Math.min(width, height) * Styles.Radial.INNER_RADIUS;
    }

    private float outer() {
        return Math.min(width, height) * Styles.Radial.OUTER_RADIUS * (0.85F + 0.15F * open.value());
    }

    /** Индекс сектора под курсором или -1. */
    public int hoveredIndex(double mx, double my) {
        return UIMath.radialSector(cx(), cy(), mx, my, actions.size(), inner(), outer() * 1.15);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float alpha = open.value();
        g.fill(x, y, x + width, y + height, UIDraw.fade(Styles.Radial.OVERLAY, alpha));
        int n = actions.size();
        if (n == 0) return;
        int hovered = hoveredIndex(mouseX, mouseY);
        double step = 360.0 / n;
        float r0 = inner();
        float r1 = outer();
        for (int i = 0; i < n; i++) {
            UIAction a = actions.get(i);
            float h =
                    sectorHover.get(i).to(i == hovered && a.enabled() ? 1F : 0F).value();
            int color = !a.enabled()
                    ? Styles.Radial.SECTOR_DISABLED
                    : com.pwp.coreclient.gui.theme.PWPTheme.Colors.lerp(
                            Styles.Radial.SECTOR, Styles.Radial.SECTOR_HOVER, h);
            double center = UIMath.sectorCenterDeg(i, n);
            double gap = Math.min(2.0, step * 0.05);
            float grow = r1 + h * Spacing.XS;
            arc(g, cx(), cy(), r0, grow, center - step / 2 + gap, center + step / 2 - gap, UIDraw.fade(color, alpha));
            if (h > 0.01F) {
                arc(
                        g,
                        cx(),
                        cy(),
                        grow - 2,
                        grow,
                        center - step / 2 + gap,
                        center + step / 2 - gap,
                        UIDraw.fade(Styles.Radial.SECTOR_ACTIVE_EDGE, alpha * h));
            }
            double rad = Math.toRadians(center);
            float mid = (r0 + r1) / 2;
            int lx = cx() + (int) Math.round(Math.sin(rad) * mid);
            int ly = cy() - (int) Math.round(Math.cos(rad) * mid);
            int textColor = a.enabled() ? Styles.Radial.TEXT : Styles.Radial.TEXT_DISABLED;
            int maxW = (int) Math.max(Spacing.XXL, (r1 - r0) * 1.4);
            if (a.icon() != null) {
                int s = Spacing.ICON_SIZE;
                g.blit(a.icon(), lx - s / 2, ly - s, 0, 0, s, s, s, s);
                UIDraw.centered(
                        g,
                        font(),
                        UIDraw.ellipsize(font(), a.label(), maxW),
                        lx,
                        ly + Spacing.XS,
                        UIDraw.fade(textColor, alpha));
            } else {
                UIDraw.centered(
                        g, font(), UIDraw.ellipsize(font(), a.label(), maxW), lx, ly, UIDraw.fade(textColor, alpha));
            }
        }
        // центр: заголовок или подсказка недоступного пункта
        int cr = Math.round(r0 - Spacing.XS);
        UIDraw.box(
                g,
                cx() - cr,
                cy() - cr,
                cr * 2,
                cr * 2,
                cr,
                UIDraw.fade(Styles.Radial.CENTER, alpha),
                UIDraw.fade(Styles.Radial.SECTOR_BORDER, alpha));
        Component center = title;
        if (hovered >= 0
                && !actions.get(hovered).enabled()
                && actions.get(hovered).disabledReason() != null) {
            center = Component.literal(actions.get(hovered).disabledReason());
        }
        UIDraw.centered(
                g,
                font(),
                UIDraw.ellipsize(font(), center, cr * 2 - Spacing.XS),
                cx(),
                cy() + 1,
                UIDraw.fade(Styles.Radial.TEXT, alpha));
    }

    /** Кольцевой сектор [a0, a1] (градусы от «вверх» по часовой) — треугольниками. */
    private static void arc(GuiGraphics g, int cx, int cy, float r0, float r1, double a0, double a1, int argb) {
        Matrix4f m = g.pose().last().pose();
        float a = ((argb >>> 24) & 0xFF) / 255F;
        float r = ((argb >> 16) & 0xFF) / 255F;
        float gr = ((argb >> 8) & 0xFF) / 255F;
        float b = (argb & 0xFF) / 255F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= ARC_STEPS; i++) {
            double ang = Math.toRadians(a0 + (a1 - a0) * i / ARC_STEPS);
            float sx = (float) Math.sin(ang);
            float sy = (float) -Math.cos(ang);
            buf.vertex(m, cx + sx * r1, cy + sy * r1, 0).color(r, gr, b, a).endVertex();
            buf.vertex(m, cx + sx * r0, cy + sy * r0, 0).color(r, gr, b, a).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    /** Выполнить пункт под курсором; true — выполнен (меню можно закрывать). */
    public boolean activate(double mx, double my) {
        int i = hoveredIndex(mx, my);
        if (i < 0) return false;
        UIAction a = actions.get(i);
        if (!a.enabled()) return false;
        a.run().run();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return button == 0 && activate(mx, my);
    }
}

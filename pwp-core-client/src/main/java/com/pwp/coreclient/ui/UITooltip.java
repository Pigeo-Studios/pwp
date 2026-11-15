package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Тултип UI-кита: появляется с задержкой после наведения, фейдится, не вылезает за экран.
 * {@link UILayer} сам показывает тултип виджета под курсором.
 */
@OnlyIn(Dist.CLIENT)
public final class UITooltip {
    /** Задержка показа, мс. */
    public static final long DELAY_MS = 350;
    /** Максимальная ширина текста до переноса. */
    public static final int MAX_WIDTH = 200;

    /** Содержимое: заголовок (акцентом) и строки описания. */
    public record Content(Component title, List<Component> lines) {
        public static Content of(Component title) {
            return new Content(title, List.of());
        }
    }

    private UITooltip() {}

    /** Рисует тултип у курсора с прозрачностью alpha (0..1). */
    public static void render(
            GuiGraphics g, Content content, int mouseX, int mouseY, int screenW, int screenH, float alpha) {
        if (content == null || alpha <= 0.01F) return;
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        List<FormattedCharSequence> body = content.lines().stream()
                .flatMap(c -> font.split(c, MAX_WIDTH).stream())
                .toList();
        int w = font.width(content.title());
        for (FormattedCharSequence s : body) w = Math.max(w, font.width(s));
        int pad = Spacing.SM;
        int lh = font.lineHeight + Spacing.XXS;
        int h = lh + body.size() * lh + (body.isEmpty() ? 0 : Spacing.XS);
        int bw = w + 2 * pad;
        int bh = h + 2 * pad - Spacing.XXS;
        int tx = UIMath.keepInside(mouseX + Spacing.MD, bw, screenW, Spacing.XS);
        int ty = UIMath.keepInside(mouseY + Spacing.MD, bh, screenH, Spacing.XS);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        UIDraw.box(
                g,
                tx,
                ty,
                bw,
                bh,
                Spacing.RADIUS_SMALL,
                UIDraw.fade(Styles.Tooltip.BG, alpha),
                UIDraw.fade(Styles.Tooltip.BORDER, alpha));
        int cy = ty + pad;
        g.drawString(
                font,
                content.title(),
                tx + pad,
                cy,
                UIDraw.fade(com.pwp.coreclient.gui.theme.PWPTheme.Colors.TEXT_ACCENT, alpha),
                false);
        cy += lh + (body.isEmpty() ? 0 : Spacing.XS);
        for (FormattedCharSequence s : body) {
            g.drawString(font, s, tx + pad, cy, UIDraw.fade(Styles.Tooltip.TEXT, alpha), false);
            cy += lh;
        }
        g.pose().popPose();
    }
}

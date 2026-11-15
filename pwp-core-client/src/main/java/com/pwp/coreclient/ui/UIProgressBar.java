package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Полоса прогресса (тикеты, XP, захват); значение плавно догоняет новое. */
@OnlyIn(Dist.CLIENT)
public class UIProgressBar extends UIWidget {
    private final UIAnim value = new UIAnim(0F, Animations.DURATION_SLOW, UIAnim.EASE_OUT_CUBIC);
    private int fillColor = Styles.Progress.FILL_XP;
    private Component label;

    public UIProgressBar(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /** Прогресс 0..1 (значения вне диапазона обрезаются). */
    public UIProgressBar progress(float progress, boolean animate) {
        float p = Math.max(0F, Math.min(1F, progress));
        if (animate) value.to(p);
        else value.snap(p);
        return this;
    }

    public UIProgressBar color(int fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    public UIProgressBar label(Component label) {
        this.label = label;
        return this;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UIDraw.box(g, x, y, width, height, Spacing.RADIUS_SMALL, Styles.Progress.BG, Colors.BORDER);
        int fw = Math.round((width - 2) * value.value());
        if (fw > 0) g.fill(x + 1, y + 1, x + 1 + fw, y + height - 1, fillColor);
        if (label != null && height >= font().lineHeight) {
            UIDraw.centered(g, font(), label, x + width / 2, y + height / 2 + 1, Colors.TEXT_PRIMARY);
        }
    }
}

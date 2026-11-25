package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Полоса прогресса в стиле индикатора загрузки (тонкий трек + акцентная заливка, квадратные
 * края); значение плавно догоняет новое. Подпись рисуется поверх, только если полоса выше
 * строки текста.
 */
@OnlyIn(Dist.CLIENT)
public class UIProgressBar extends UIWidget {
    private final UIAnim value = new UIAnim(0F, Animations.DURATION_SLOW, UIAnim.EASE_OUT_CUBIC);
    private int fillColor = Styles.Loader.FILL;
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
        Rect r = bounds();
        UIDraw.rect(g, r, UIDraw.fade(Styles.Loader.TRACK, opacity));
        int fw = Math.round(r.w() * value.value());
        if (fw > 0) {
            Rect fill = r.withWidth(fw);
            UIDraw.rect(g, fill, UIDraw.fade(fillColor, opacity));
            // светлая кромка сверху — «объём» как у кнопок
            UIDraw.hline(g, fill.x(), fill.right(), fill.y(), UIDraw.fade(Styles.Glass.HIGHLIGHT, opacity));
        }
        if (label != null && height >= font().lineHeight) {
            UIDraw.centered(
                    g,
                    font(),
                    UIDraw.ellipsize(font(), label, width),
                    x + width / 2,
                    y + height / 2 + 1,
                    UIDraw.fade(Colors.TEXT_PRIMARY, opacity));
        }
    }
}

package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Легаси-полоса прогресса (API прежний) в стиле индикатора загрузки UI-кита: трек + заливка
 * с хайлайтом; {@link #renderPulse} — бегущий туда-обратно блок. Новые экраны —
 * {@link com.pwp.coreclient.ui.UIProgressBar}.
 */
public class PWPProgressBar {

    private float currentProgress;
    private float targetProgress;

    public PWPProgressBar() {
        this.currentProgress = 0;
        this.targetProgress = 0;
    }

    public void setProgress(float progress) {
        this.targetProgress = Math.max(0, Math.min(1, progress));
    }

    public float getCurrentProgress() {
        return currentProgress;
    }

    public boolean isAnimating() {
        return Math.abs(currentProgress - targetProgress) > 0.001F;
    }

    public void render(GuiGraphics gui, int x, int y, int w, int h, long now) {
        render(gui, x, y, w, h, now, PWPTheme.Colors.ACCENT);
    }

    public void render(GuiGraphics gui, int x, int y, int w, int h, long now, int fillColor) {
        if (now > 0) {
            currentProgress += (targetProgress - currentProgress) * 0.1F;
            if (Math.abs(currentProgress - targetProgress) < 0.001F) {
                currentProgress = targetProgress;
            }
        }
        Rect r = Rect.of(x, y, w, h);
        UIDraw.rect(gui, r, Styles.Loader.TRACK);
        if (currentProgress > 0.01F) {
            Rect fill = r.withWidth(Math.max(2, Math.round(w * currentProgress)));
            UIDraw.rect(gui, fill, fillColor);
            if (h > 2) UIDraw.hline(gui, fill.x(), fill.right(), fill.y(), Styles.Glass.HIGHLIGHT);
        }
    }

    public static void renderPulse(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        Rect r = Rect.of(x, y, w, h);
        UIDraw.rect(gui, r, Styles.Loader.TRACK);
        int block = Math.max(w / PWPTheme.Spacing.LOADER_BLOCK_DIV, PWPTheme.Spacing.LG);
        int bx = x + Math.round((w - block) * UIMath.pingPong(ageMs, Animations.LOADER_PERIOD));
        UIDraw.rect(gui, Rect.of(bx, y, Math.min(block, w), h), Styles.Loader.FILL);
    }
}

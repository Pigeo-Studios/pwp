package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

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

        gui.fill(x, y, x + w, y + h, PWPTheme.Styles.Progress.BG);
        if (currentProgress > 0.01F) {
            int fillW = Math.max(2, (int) (w * currentProgress));
            gui.fill(x, y, x + fillW, y + h, fillColor);
        }
    }

    public static void renderPulse(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        gui.fill(x, y, x + w, y + h, PWPTheme.Styles.Progress.BG);

        long periodMs = 1600;
        long phase = ageMs % periodMs;
        float t = (float) phase / periodMs;
        if (t > 0.5F) t = 1.0F - t;

        int blockWidth = Math.max(w / 5, 20);
        int blockX = x + (int) ((w - blockWidth) * t * 2.0F);
        gui.fill(blockX, y, blockX + blockWidth, y + h, PWPTheme.Colors.ACCENT);
    }
}

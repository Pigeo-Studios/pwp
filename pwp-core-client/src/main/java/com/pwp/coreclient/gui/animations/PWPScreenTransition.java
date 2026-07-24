package com.pwp.coreclient.gui.animations;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public class PWPScreenTransition {

    public enum Type {
        FADE, FADE_ONLY, SLIDE_UP, SLIDE_DOWN
    }

    private final long startTime;
    private final long durationMs;
    private final Type type;

    public PWPScreenTransition(long startTime, long durationMs, Type type) {
        this.startTime = startTime;
        this.durationMs = durationMs;
        this.type = type;
    }

    public boolean isActive(long now) {
        return now - startTime < durationMs;
    }

    public float getProgress(long now) {
        float t = (float) (now - startTime) / durationMs;
        return Math.min(Math.max(t, 0), 1);
    }

    public void apply(GuiGraphics gui, long now) {
        float progress = getProgress(now);
        float eased = Easing.easeOutCubic(progress);

        switch (type) {
            case FADE:
            case FADE_ONLY:
                RenderSystem.setShaderColor(1, 1, 1, eased);
                break;
            case SLIDE_UP:
                RenderSystem.setShaderColor(1, 1, 1, eased);
                gui.pose().translate(0, (1 - eased) * 20, 0);
                break;
            case SLIDE_DOWN:
                RenderSystem.setShaderColor(1, 1, 1, eased);
                gui.pose().translate(0, -(1 - eased) * 20, 0);
                break;
        }
    }

    public void reset() {
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static void applyFade(GuiGraphics gui, long startTime, long now, long duration) {
        float t = (float) (now - startTime) / duration;
        float eased = Easing.easeOutCubic(Math.min(Math.max(t, 0), 1));
        RenderSystem.setShaderColor(1, 1, 1, eased);
    }

    public static void endFade() {
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static void applySlideUp(GuiGraphics gui, long startTime, long now, long duration, int offsetPx) {
        float t = (float) (now - startTime) / duration;
        float eased = Easing.easeOutCubic(Math.min(Math.max(t, 0), 1));
        float slide = (1 - eased) * offsetPx;
        RenderSystem.setShaderColor(1, 1, 1, eased);
        gui.pose().translate(0, slide, 0);
    }

    public static void endSlide() {
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static float pulseAlpha(long startTime, long periodMs) {
        long elapsed = (System.currentTimeMillis() - startTime) % periodMs;
        float t = (float) elapsed / periodMs;
        return 0.4F + 0.6F * (float) (Math.sin(t * Math.PI * 2) * 0.5 + 0.5);
    }
}

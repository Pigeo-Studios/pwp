package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Fit;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Фон меню/загрузки: случайная картинка из набора с кроссфейдом раз в 1–5 мин. Картинка
 * вписывается режимом «cover» (без искажения пропорций на 4:3/16:9/21:9), поверх —
 * атмосферный слой {@link UIBackdrop#overlay}.
 */
public class PWPRotatingBackground {

    private static final ResourceLocation[] BG_TEXTURES = {
        new ResourceLocation("pwp_core_client", "textures/gui/bg/bg_0.png"),
        new ResourceLocation("pwp_core_client", "textures/gui/bg/bg_1.png"),
        new ResourceLocation("pwp_core_client", "textures/gui/bg/bg_2.png")
    };
    /** Размер файлов bg_*.png (все одного размера). */
    private static final int BG_W = 1672;

    private static final int BG_H = 941;

    private static final long TRANSITION_DURATION = 1000;
    private static final long MIN_INTERVAL = 60000;
    private static final long MAX_INTERVAL = 300000;

    private static final Random RANDOM = new Random();

    private static int currentIndex = RANDOM.nextInt(BG_TEXTURES.length);
    private static int nextIndex = -1;
    private static long switchStartTime = System.currentTimeMillis();
    private static long nextSwitchDelay = getRandomDelay();
    private static boolean transitioning = false;

    private static long getRandomDelay() {
        return MIN_INTERVAL + RANDOM.nextLong(MAX_INTERVAL - MIN_INTERVAL + 1);
    }

    /** Картинка + стандартный атмосферный слой (без боковой тени). */
    public static void render(GuiGraphics gui, int x, int y, int w, int h) {
        render(gui, x, y, w, h, UIBackdrop.Shade.NONE);
    }

    public static void render(GuiGraphics gui, int x, int y, int w, int h, UIBackdrop.Shade shade) {
        renderImage(gui, x, y, w, h);
        UIBackdrop.overlay(gui, w, h, shade);
    }

    /** Только картинка (cover), с кроссфейдом смены. */
    public static void renderImage(GuiGraphics gui, int x, int y, int w, int h) {
        long now = System.currentTimeMillis();
        long elapsed = now - switchStartTime;

        if (!transitioning && elapsed >= nextSwitchDelay) {
            transitioning = true;
            switchStartTime = now;
            do {
                nextIndex = RANDOM.nextInt(BG_TEXTURES.length);
            } while (nextIndex == currentIndex);
        }

        int[] src = Fit.cover(BG_W, BG_H, w, h);
        if (transitioning) {
            long transitionElapsed = now - switchStartTime;
            float progress = Math.min(transitionElapsed / (float) TRANSITION_DURATION, 1.0f);
            blitCover(gui, BG_TEXTURES[currentIndex], x, y, w, h, src, 0xFFFFFFFF);
            blitCover(gui, BG_TEXTURES[nextIndex], x, y, w, h, src, UIDraw.fade(0xFFFFFFFF, progress));
            if (progress >= 1.0f) {
                currentIndex = nextIndex;
                nextIndex = -1;
                transitioning = false;
                switchStartTime = now;
                nextSwitchDelay = getRandomDelay();
            }
        } else {
            blitCover(gui, BG_TEXTURES[currentIndex], x, y, w, h, src, 0xFFFFFFFF);
        }
    }

    private static void blitCover(
            GuiGraphics gui, ResourceLocation tex, int x, int y, int w, int h, int[] src, int tint) {
        UIDraw.blitTinted(
                gui, tint, () -> gui.blit(tex, x, y, w, h, (float) src[0], (float) src[1], src[2], src[3], BG_W, BG_H));
    }
}

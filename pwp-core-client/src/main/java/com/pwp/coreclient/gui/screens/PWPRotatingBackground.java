package com.pwp.coreclient.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public class PWPRotatingBackground {

    private static final ResourceLocation[] BG_TEXTURES = {
        new ResourceLocation("pwp_core_client", "textures/gui/bg/bg_0.png"),
        new ResourceLocation("pwp_core_client", "textures/gui/bg/bg_1.png"),
        new ResourceLocation("pwp_core_client", "textures/gui/bg/bg_2.png")
    };

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

    public static void render(GuiGraphics gui, int x, int y, int w, int h) {
        long now = System.currentTimeMillis();
        long elapsed = now - switchStartTime;

        if (!transitioning && elapsed >= nextSwitchDelay) {
            transitioning = true;
            switchStartTime = now;
            do {
                nextIndex = RANDOM.nextInt(BG_TEXTURES.length);
            } while (nextIndex == currentIndex);
        }

        if (transitioning) {
            long transitionElapsed = now - switchStartTime;
            float progress = Math.min(transitionElapsed / (float) TRANSITION_DURATION, 1.0f);

            gui.setColor(1, 1, 1, 1.0f - progress);
            gui.blit(BG_TEXTURES[currentIndex], x, y, 0, 0, w, h, w, h);

            gui.setColor(1, 1, 1, progress);
            gui.blit(BG_TEXTURES[nextIndex], x, y, 0, 0, w, h, w, h);

            gui.setColor(1, 1, 1, 1);

            if (progress >= 1.0f) {
                currentIndex = nextIndex;
                nextIndex = -1;
                transitioning = false;
                switchStartTime = now;
                nextSwitchDelay = getRandomDelay();
            }
        } else {
            gui.blit(BG_TEXTURES[currentIndex], x, y, 0, 0, w, h, w, h);
        }

        // Dark gradient overlay for readability
        int gradientStart = h * 2 / 3;
        gui.fillGradient(0, gradientStart, w, h, 0x00000000, 0xAA000000);
    }
}

package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PWPUtils {

    private PWPUtils() {}

    public static void renderLogo(GuiGraphics gui, int cx, int y) {
        var font = PWPTheme.Fonts.display();
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(cx, y, 0);
        pose.scale(2.2f, 2.2f, 1f);
        gui.drawCenteredString(font, Component.literal("PWP"), 0, 0, PWPTheme.Colors.ACCENT);
        pose.popPose();
    }

    public static void renderSpinner(GuiGraphics gui, int cx, int y, long ageMs) {
        int radius = 3;
        int spacing = 12;
        long periodMs = 900;

        for (int i = 0; i < 3; i++) {
            long phaseMs = i * 150L;
            long t = (ageMs + phaseMs) % periodMs;
            float pulse = Easing.pulse((float) t / periodMs);
            float alpha = 0.3f + 0.7f * pulse;
            int color = withAlpha(PWPTheme.Colors.ACCENT, (int) (alpha * 255));

            int cxDot = cx + i * spacing - spacing;
            gui.fill(cxDot - radius, y - radius, cxDot + radius, y + radius, color);
        }
    }

    public static void renderProgressBar(GuiGraphics gui, int cx, int y, int barWidth, int barHeight, long ageMs) {
        int trackX = cx - barWidth / 2;
        gui.fill(trackX, y, trackX + barWidth, y + barHeight, PWPTheme.Colors.SURFACE_DIM);

        long periodMs = 1600;
        long phase = ageMs % periodMs;
        float t = (float) phase / periodMs;
        if (t > 0.5f) t = 1.0f - t;
        t = Easing.easeOutCubic(t * 2.0f) * 0.5f;

        int blockWidth = Math.max(barWidth / 5, 20);
        int blockX = trackX + (int) ((barWidth - blockWidth) * t * 2.0f);
        gui.fill(blockX, y, blockX + blockWidth, y + barHeight, PWPTheme.Colors.ACCENT);
    }

    public static void renderStatusText(GuiGraphics gui, String text, int cx, int y, long ageMs) {
        var font = PWPTheme.Fonts.display();
        long periodMs = 2400;
        float pulse = Easing.pulse((float) (ageMs % periodMs) / periodMs);
        float alpha = 0.7f + 0.3f * pulse;
        int color = withAlpha(PWPTheme.Colors.TEXT_PRIMARY, (int) (alpha * 255));
        gui.drawCenteredString(font, Component.literal(text), cx, y, color);
    }

    public static void renderBox(GuiGraphics gui, int cx, int y, int w, int h) {
        int x = cx - w / 2;
        RoundedRect.fill(gui, x, y, w, h, PWPTheme.Spacing.RADIUS_SMALL, 0xCC000000);
        RoundedRect.border(gui, x, y, w, h, PWPTheme.Spacing.RADIUS_SMALL, 1, 0x44FFFFFF);
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.min(255, Math.max(0, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}

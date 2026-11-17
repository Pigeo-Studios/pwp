package com.pigeostudios.pwp.limit.client;

import com.pigeostudios.pwp.limit.ModConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "pwplimit",
        value = {Dist.CLIENT})
public class ClientOverlay {
    private static float displayHealth = 20.0f;
    private static float lastActualHealth = 20.0f;
    private static float healthBarAlpha = 0.0f;
    // Таймер показа — в миллисекундах реального времени (раньше считал КАДРЫ:
    // 60 кадров = 1 с при 60 FPS, но 0.4 с при 144 FPS). Сглаживание — та же
    // кривая, что прежний покадровый lerp при 60 FPS, но не зависит от FPS.
    private static float healthDisplayTimerMs = 0.0f;
    private static final float DISPLAY_DURATION_MS = 1000.0f; // = 60 кадров при 60 FPS
    private static final float FADE_SPEED = 0.08f; // доля за кадр при 60 FPS
    private static final float HEALTH_LERP = 0.15f; // доля за кадр при 60 FPS
    private static final float FRAME_60_MS = 1000.0f / 60.0f;
    private static final long MAX_DT_MS = 100L;
    private static long lastFrameMs = -1L;

    @SubscribeEvent
    public static void onRenderBars(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        // F1 скрывает HUD (как общий гейт WHud.hudVisible в warfare; от warfare модуль не зависит)
        if (player == null || mc.screen != null || mc.options.hideGui) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int barWidth = 81;
        int barHeight = 7;
        int x = 10;
        int y = mc.getWindow().getGuiScaledHeight() - 32;
        long nowMs = Util.getMillis();
        long dtMs = lastFrameMs < 0L ? 0L : Math.max(0L, Math.min(MAX_DT_MS, nowMs - lastFrameMs));
        lastFrameMs = nowMs;

        // HP BAR
        float currentHealth = player.getHealth();
        float maxHealth = Math.max(player.getMaxHealth(), 1.0f);

        // Detect health change
        if (Math.abs(currentHealth - lastActualHealth) > 0.001f) {
            lastActualHealth = currentHealth;
            healthDisplayTimerMs = DISPLAY_DURATION_MS;
        }

        // Check CD activity
        boolean cdActive = false;
        long lastJump = 0;
        long elapsed = 0;
        long cooldownMs = 0;
        if (ModConfig.ENABLE_JUMP_COOLDOWN.get()) {
            Long storedJump = ClientEvents.sprintJumpCooldown.get(player.getUUID());
            if (storedJump != null) {
                cooldownMs = (long) (ModConfig.JUMP_COOLDOWN_SECONDS.get() * 1000.0);
                elapsed = System.currentTimeMillis() - storedJump;
                if (elapsed < cooldownMs) {
                    cdActive = true;
                } else {
                    ClientEvents.sprintJumpCooldown.remove(player.getUUID());
                }
            }
        }

        // Reset timer if CD active
        if (cdActive) {
            healthDisplayTimerMs = DISPLAY_DURATION_MS;
        }

        // Update timer
        if (healthDisplayTimerMs > 0) {
            healthDisplayTimerMs -= dtMs;
        }

        // Smooth display health lerp
        displayHealth = approach(displayHealth, currentHealth, HEALTH_LERP, dtMs);

        // Alpha animation
        float targetAlpha = healthDisplayTimerMs > 0 ? 1.0f : 0.0f;
        healthBarAlpha = approach(healthBarAlpha, targetAlpha, FADE_SPEED, dtMs);

        float renderAlpha = healthBarAlpha;
        if (renderAlpha < 0.01f) {
            return;
        }

        int a = Mth.clamp((int) (renderAlpha * 255.0f), 0, 255);

        int filled = (int) (barWidth * Mth.clamp(displayHealth / maxHealth, 0.0f, 1.0f));

        // Frame
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, (a << 24) | 0x1E222A);
        // Background
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, (a << 24) | 0x181C24);
        // Fill
        if (filled > 0) {
            int fillColor = displayHealth / maxHealth > 0.3f ? 0xFF3D7A40 : 0xFFA53D3D;
            guiGraphics.fill(x, y, x + filled, y + barHeight, (a << 24) | (fillColor & 0x00FFFFFF));
        }

        // JUMP COOLDOWN BAR (only if active)
        if (!cdActive) {
            return;
        }
        float progress = (float) elapsed / (float) cooldownMs;
        int cdFilled = (int) (barWidth * progress);
        int cdY = y + barHeight + 2;

        guiGraphics.fill(x, cdY, x + barWidth, cdY + 2, (a << 24) | 0x181C24);
        if (cdFilled > 0) {
            guiGraphics.fill(x, cdY, x + cdFilled, cdY + 2, (a << 24) | 0xC8812A);
        }
    }

    /** Экспоненциальное сглаживание в реальном времени: rate — доля за кадр при 60 FPS. */
    private static float approach(float current, float target, float rate, long dtMs) {
        if (dtMs <= 0L) return current;
        double keep = Math.pow(1.0 - rate, dtMs / (double) FRAME_60_MS);
        return (float) (target + (current - target) * keep);
    }
}

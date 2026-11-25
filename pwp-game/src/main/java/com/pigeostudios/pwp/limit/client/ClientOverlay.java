package com.pigeostudios.pwp.limit.client;

import com.pwp.coreclient.ui.hud.Hud;
import com.pwp.coreclient.ui.hud.HudVitals;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "pwpwarfare",
        value = {Dist.CLIENT})
public class ClientOverlay {
    private static float lastActualHealth = 20.0f;
    private static float healthBarAlpha = 0.0f;
    // Таймер показа — в миллисекундах реального времени (раньше считал КАДРЫ:
    // 60 кадров = 1 с при 60 FPS, но 0.4 с при 144 FPS). Сглаживание — та же
    // кривая, что прежний покадровый lerp при 60 FPS, но не зависит от FPS.
    private static float healthDisplayTimerMs = 0.0f;
    // 1.6 с (было 1 с): успевает пройти пауза и стекание хвоста урона (DamageTrail)
    private static final float DISPLAY_DURATION_MS = 1600.0f;
    private static final float FADE_SPEED = 0.08f; // доля за кадр при 60 FPS
    private static final float FRAME_60_MS = 1000.0f / 60.0f;
    private static final long MAX_DT_MS = 100L;
    private static long lastFrameMs = -1L;

    /** Вид HP/брони — HUD-кит core-client (низ-лево рядом с хотбаром, хвост урона). */
    private static final HudVitals VITALS = new HudVitals();
    /** Броня 0..20 (ванильная шкала; больше — полная полоса). */
    private static final float ARMOR_FULL = 20.0f;

    // HIGH: HP занимает НИЖНЕЕ место стека низ-лево раньше БК warfare (NORMAL)
    @SubscribeEvent(priority = EventPriority.HIGH)
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
        if (LimitsConfigCache.isJumpCooldownEnabled()) {
            Long storedJump = ClientEvents.sprintJumpCooldown.get(player.getUUID());
            if (storedJump != null) {
                cooldownMs = (long) (LimitsConfigCache.getJumpCooldownSeconds() * 1000.0);
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

        // Alpha animation
        float targetAlpha = healthDisplayTimerMs > 0 ? 1.0f : 0.0f;
        healthBarAlpha = approach(healthBarAlpha, targetAlpha, FADE_SPEED, dtMs);

        float cooldown = cdActive ? (float) elapsed / (float) cooldownMs : -1.0f;
        float armor = Mth.clamp(player.getArmorValue() / ARMOR_FULL, 0.0f, 1.0f);
        // место в стеке занимается и при alpha 0 — БК над HP не прыгает при показе/скрытии
        VITALS.render(
                guiGraphics,
                mc.font,
                Hud.zones(),
                currentHealth / maxHealth,
                armor,
                cooldown,
                healthBarAlpha < 0.01f ? 0.0f : healthBarAlpha);
    }

    /** Экспоненциальное сглаживание в реальном времени: rate — доля за кадр при 60 FPS. */
    private static float approach(float current, float target, float rate, long dtMs) {
        if (dtMs <= 0L) return current;
        double keep = Math.pow(1.0 - rate, dtMs / (double) FRAME_60_MS);
        return (float) (target + (current - target) * keep);
    }
}

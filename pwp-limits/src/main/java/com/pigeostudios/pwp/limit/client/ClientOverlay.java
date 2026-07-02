package com.pigeostudios.pwp.limit.client;

import com.pigeostudios.pwp.limit.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="pwplimit", value={Dist.CLIENT})
public class ClientOverlay {
    private static float displayHealth = 20.0f;
    private static float lastActualHealth = 20.0f;
    private static float healthBarAlpha = 0.0f;
    private static float prevHealthBarAlpha = 0.0f;
    private static float healthDisplayTimer = 0.0f;
    private static final float DISPLAY_DURATION = 60.0f;
    private static final float FADE_SPEED = 0.08f;

    @SubscribeEvent
    public static void onRenderBars(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int barWidth = 81;
        int barHeight = 7;
        int x = 10;
        int y = mc.getWindow().getGuiScaledHeight() - 32;
        float partialTick = event.getPartialTick();

        // HP BAR
        float currentHealth = player.getHealth();
        float maxHealth = Math.max(player.getMaxHealth(), 1.0f);

        // Detect health change
        if (Math.abs(currentHealth - lastActualHealth) > 0.001f) {
            lastActualHealth = currentHealth;
            healthDisplayTimer = DISPLAY_DURATION;
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
            healthDisplayTimer = DISPLAY_DURATION;
        }

        // Update timer
        if (healthDisplayTimer > 0) {
            healthDisplayTimer--;
        }

        // Smooth display health lerp
        displayHealth = Mth.lerp(0.15f, displayHealth, currentHealth);

        // Alpha animation
        prevHealthBarAlpha = healthBarAlpha;
        float targetAlpha = healthDisplayTimer > 0 ? 1.0f : 0.0f;
        healthBarAlpha = Mth.lerp(FADE_SPEED, healthBarAlpha, targetAlpha);

        float renderAlpha = Mth.lerp(partialTick, prevHealthBarAlpha, healthBarAlpha);
        if (renderAlpha < 0.01f) {
            return;
        }

        int a = Mth.clamp((int) (renderAlpha * 255.0f), 0, 255);

        int filled = (int) (barWidth * Mth.clamp(displayHealth / maxHealth, 0.0f, 1.0f));

        // Frame
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, (a << 24) | 0x555555);
        // Background
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, (a << 24) | 0x222222);
        // Fill
        if (filled > 0) {
            guiGraphics.fill(x, y, x + filled, y + barHeight, (a << 24) | 0xDDDDDD);
        }

        // JUMP COOLDOWN BAR (only if active)
        if (!cdActive) {
            return;
        }
        float progress = (float) elapsed / (float) cooldownMs;
        int cdFilled = (int) (barWidth * progress);
        int cdY = y + barHeight + 2;

        guiGraphics.fill(x, cdY, x + barWidth, cdY + 2, (a << 24) | 0x333333);
        if (cdFilled > 0) {
            guiGraphics.fill(x, cdY, x + cdFilled, cdY + 2, (a << 24) | 0x999999);
        }
    }
}

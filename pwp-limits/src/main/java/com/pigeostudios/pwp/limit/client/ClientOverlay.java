package com.pigeostudios.pwp.limit.client;

import com.alrex.parcool.api.Stamina;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "pwplimit", value = {Dist.CLIENT})
public class ClientOverlay {
    private static float displayHealth = 20.0f;
    private static float lastActualHealth = 20.0f;
    private static float healthBarAlpha = 0.0f;
    private static float prevHealthBarAlpha = 0.0f;
    private static float healthDisplayTimer = 0.0f;
    private static final float DISPLAY_DURATION = 60.0f;
    private static final float FADE_SPEED = 0.08f;

    // Стамина ParCool (Squad-полоска над HP-баром, видна только при неполной стамине)
    private static float displayStamina = 0.0f;
    private static float staminaBarAlpha = 0.0f;
    private static float prevStaminaBarAlpha = 0.0f;

    // Палитра CD-градиента (белый -> оранжевый -> красный по мере убывания остатка)
    private static final int CD_WHITE = 0xFFFFFF;
    private static final int CD_ORANGE = 0xC8812A;
    private static final int CD_RED = 0xA53D3D;

    @SubscribeEvent
    public static void onRenderBars(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || mc.options.hideGui) {
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
        int elapsedTicks = 0;
        int cooldownTicks = 0;
        if (LimitsConfigCache.isJumpCooldownEnabled()) {
            Integer lastJumpTick = ClientEvents.sprintJumpCooldown.get(player.getUUID());
            if (lastJumpTick != null) {
                cooldownTicks = (int) (LimitsConfigCache.getJumpCooldownSeconds() * 20.0);
                elapsedTicks = player.tickCount - lastJumpTick;
                if (elapsedTicks < cooldownTicks) {
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
        if (renderAlpha >= 0.01f) {
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
        }

        // STAMINA BAR (ParCool) — над HP-баром: y = hpY - barHeight, белая заливка
        int staminaY = y - barHeight;
        renderStaminaBar(guiGraphics, player, x, staminaY, barWidth, partialTick);

        // JUMP COOLDOWN BAR — над полоской стамины, градиент по остатку КД
        int cdY = staminaY - 4;
        renderJumpCooldownBar(guiGraphics, x, cdY, barWidth, cdActive, elapsedTicks, cooldownTicks);
    }

    // Стамина ParCool: видна только пока стамина неполная (трата на ClimbUp/Vault),
    // значение плавное (lerp), восстановление видно как медленный рост белой полосы.
    // Данные — публичный API com.alrex.parcool.api.Stamina (синхронизируется клиенту сам).
    private static void renderStaminaBar(GuiGraphics guiGraphics, LocalPlayer player, int x, int y, int barWidth, float partialTick) {
        int[] stamina = readParCoolStamina(player);
        if (stamina == null) {
            return;
        }
        int value = stamina[0];
        int maxValue = Math.max(stamina[1], 1);
        float ratio = Mth.clamp((float) value / (float) maxValue, 0.0f, 1.0f);

        displayStamina = Mth.lerp(0.15f, displayStamina, value);
        prevStaminaBarAlpha = staminaBarAlpha;
        staminaBarAlpha = Mth.lerp(FADE_SPEED, staminaBarAlpha, ratio < 1.0f ? 1.0f : 0.0f);
        float alpha = Mth.lerp(partialTick, prevStaminaBarAlpha, staminaBarAlpha);
        if (alpha < 0.01f) {
            return;
        }

        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        int filled = (int) (barWidth * Mth.clamp(displayStamina / (float) maxValue, 0.0f, 1.0f));

        // Background
        guiGraphics.fill(x, y, x + barWidth, y + 2, (a << 24) | 0x181C24);
        // Fill (белая, как стамина в Squad)
        if (filled > 0) {
            guiGraphics.fill(x, y, x + filled, y + 2, (a << 24) | 0xFFFFFF);
        }
    }

    // Полоска КД спринт-прыжка над стаминой: показывает ОСТАТОК (убывает).
    // Цвет — плавный градиент по остатку: >66% белый, 33-66% оранжевый, <33% красный.
    private static void renderJumpCooldownBar(GuiGraphics guiGraphics, int x, int y, int barWidth, boolean cdActive, int elapsedTicks, int cooldownTicks) {
        if (!cdActive || cooldownTicks <= 0) {
            return;
        }
        float remaining = 1.0f - (float) elapsedTicks / (float) cooldownTicks;
        int filled = (int) (barWidth * Mth.clamp(remaining, 0.0f, 1.0f));

        // Background
        guiGraphics.fill(x, y, x + barWidth, y + 2, 0xFF181C24);
        // Fill
        if (filled > 0) {
            guiGraphics.fill(x, y, x + filled, y + 2, cdGradientColor(remaining));
        }
    }

    private static int[] readParCoolStamina(LocalPlayer player) {
        try {
            Stamina stamina = Stamina.get(player);
            if (stamina == null) {
                return null;
            }
            return new int[]{stamina.getValue(), stamina.getMaxValue()};
        } catch (Throwable t) {
            // ParCool отсутствует (например, dev-сборка без мода) — полоска просто не рисуется
            return null;
        }
    }

    private static int cdGradientColor(float ratio) {
        if (ratio > 0.66f) {
            return lerpColor(CD_ORANGE, CD_WHITE, (ratio - 0.66f) / 0.34f);
        }
        if (ratio > 0.33f) {
            return lerpColor(CD_RED, CD_ORANGE, (ratio - 0.33f) / 0.33f);
        }
        return CD_RED;
    }

    private static int lerpColor(int from, int to, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int r1 = (from >> 16) & 0xFF;
        int g1 = (from >> 8) & 0xFF;
        int b1 = from & 0xFF;
        int r2 = (to >> 16) & 0xFF;
        int g2 = (to >> 8) & 0xFF;
        int b2 = to & 0xFF;
        int r = (int) Mth.lerp(t, r1, r2);
        int g = (int) Mth.lerp(t, g1, g2);
        int b = (int) Mth.lerp(t, b1, b2);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}

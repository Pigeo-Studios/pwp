package com.pwp.coreclient.gui.hud;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PWPHudOverlay {

    private static final long FLASH_DURATION_MS = 200;

    private float lastHealth = -1;
    private long healthFlashStart;
    private boolean healthFlashActive;
    private float lastFood = -1;
    private long foodFlashStart;
    private boolean foodFlashActive;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new PWPHudOverlay());
    }

    private PWPHudOverlay() {}

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        // Health bar (bottom-left)
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthRatio = Math.max(0, health / maxHealth);

        if (lastHealth < 0) lastHealth = health;
        if (Math.abs(health - lastHealth) > 0.5F) {
            healthFlashActive = true;
            healthFlashStart = now;
        }
        lastHealth = health;

        renderBar(gui, 10, h - 20, 80, 6, healthRatio,
            PWPTheme.Colors.SUCCESS, PWPTheme.Colors.DANGER, now, healthFlashActive, healthFlashStart);

        String healthText = String.format("%.0f", health);
        gui.drawString(mc.font, healthText, 12, h - 28, PWPTheme.Colors.TEXT_PRIMARY);

        // Armor bar (below health)
        float armor = player.getArmorValue();
        float armorRatio = Math.min(1, armor / 20.0F);
        if (armorRatio > 0.01F) {
            renderBar(gui, 10, h - 12, 80, 4, armorRatio, PWPTheme.Colors.INFO, 0, now, false, 0);
        }

        // Food bar (bottom-right)
        int food = player.getFoodData().getFoodLevel();
        float foodRatio = food / 20.0F;

        renderBar(gui, w - 90, h - 16, 80, 6, foodRatio,
            PWPTheme.Colors.WARNING, 0, now, foodFlashActive, foodFlashStart);

        // Damage vignette
        if (player.hurtTime > 0) {
            float intensity = Math.min(1, player.hurtTime / 10.0F);
            int alpha = (int) (intensity * 60);
            int color = (alpha << 24) | 0xFF0000;
            gui.fill(0, 0, w, h, color);
        }
    }

    private void renderBar(GuiGraphics gui, int x, int y, int w, int h, float ratio,
                           int fillColor, int warnColor, long now, boolean flashActive, long flashStart) {
        gui.fill(x, y, x + w, y + h, 0x66000000);

        int displayColor = fillColor;
        if (warnColor != 0 && ratio < 0.3F) {
            displayColor = warnColor;
        }

        if (ratio > 0.01F) {
            int fillW = Math.max(1, (int) (w * ratio));
            gui.fill(x, y, x + fillW, y + h, displayColor);
        }

        // Flash overlay
        if (flashActive) {
            long elapsed = now - flashStart;
            if (elapsed < FLASH_DURATION_MS) {
                float flashAlpha = 1.0F - (float) elapsed / FLASH_DURATION_MS;
                int flash = ((int) (flashAlpha * 180) << 24) | 0xFFFFFF;
                gui.fill(x, y, x + w, y + h, flash);
            } else {
                if (flashActive) flashActive = false;
            }
        }
    }
}

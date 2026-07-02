package com.pigeostudios.pwp.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Обработчик событий мода PWP Movement
// Управляет наклоном камеры, оглушением, усталостью и кастомным HUD
public class MovementHandler {
    // Поля: усталость при спринте, наклон камеры, оглушение при приземлении, анимация хотбара
    private float fatigue = 0.0f;
    private float currentTilt = 0.0f;
    private float prevTilt = 0.0f;
    private float landingStun = 0.0f;
    private float swayTime = 0.0f;
    private boolean wasInAir = false;
    private int hotbarTimer = 0;
    private int lastSelectedSlot = -1;
    private int lastInvChange = 0;
    private float hotbarAlpha = 0.0f;
    private float prevHotbarAlpha = 0.0f;
    private float hotbarSlide = 0.0f;
    private float prevHotbarSlide = 0.0f;
    private float[] slotH = new float[9];
    private float[] prevSlotH = new float[9];

    // onClientTick: обновление наклона камеры, замедление в воздухе,
    // оглушение приземления, усталость спринта, анимация хотбара
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        boolean shouldShow;
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        this.prevTilt = this.currentTilt;
        this.currentTilt = Mth.lerp(0.12f, this.currentTilt, -player.xxa * 1.33f);
        if (player.onGround()) {
            if (this.wasInAir) {
                this.landingStun = 0.65f;
                this.wasInAir = false;
            }
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.95, 1.0, 0.95));
        } else {
            this.wasInAir = true;
        }
        if (this.landingStun > 0.01f) {
            float slow = 1.0f - this.landingStun * 0.45f;
            player.setDeltaMovement(player.getDeltaMovement().multiply(slow, 1.0, slow));
            this.landingStun *= 0.88f;
        }
        this.fatigue = player.isSprinting() && player.zza != 0.0f
                ? Math.min(this.fatigue + 0.012f, 1.0f)
                : Math.max(this.fatigue - 0.008f, 0.0f);
        int currentSlot = player.getInventory().selected;
        if (currentSlot != this.lastSelectedSlot) {
            this.hotbarTimer = 50;
            this.lastSelectedSlot = currentSlot;
        }
        int invChange = player.getInventory().getTimesChanged();
        if (invChange != this.lastInvChange) {
            this.lastInvChange = invChange;
            if (this.hotbarTimer <= 0) {
                this.hotbarTimer = 50;
            }
        }
        if (this.hotbarTimer > 0) {
            --this.hotbarTimer;
        }
        this.prevHotbarAlpha = this.hotbarAlpha;
        this.prevHotbarSlide = this.hotbarSlide;
        boolean bl = shouldShow = this.hotbarTimer > 0 || player.isSpectator();
        if (shouldShow) {
            this.hotbarSlide = Mth.lerp(0.35f, this.hotbarSlide, 1.0f);
            this.hotbarAlpha = Mth.lerp(0.35f, this.hotbarAlpha, 1.0f);
        } else {
            this.hotbarSlide = Mth.lerp(0.12f, this.hotbarSlide, 0.0f);
            this.hotbarAlpha = Mth.lerp(0.12f, this.hotbarAlpha, 0.0f);
        }
        for (int i = 0; i < 9; ++i) {
            this.prevSlotH[i] = this.slotH[i];
            float targetH = i == currentSlot ? 10.0f : 0.0f;
            this.slotH[i] = Mth.lerp(0.4f, this.slotH[i], targetH);
        }
    }

    // onRenderGuiPost: отрисовка кастомного хотбара с анимацией
    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float pt = event.getPartialTick();
        float renderAlpha = Mth.lerp(pt, this.prevHotbarAlpha, this.hotbarAlpha);
        float renderSlide = Mth.lerp(pt, this.prevHotbarSlide, this.hotbarSlide);
        if (renderAlpha <= 0.001f) {
            return;
        }
        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int slotSize = 22;
        int gap = 4;
        int totalWidth = 9 * (slotSize + gap);
        int startX = (screenWidth - totalWidth) / 2;
        float baseY = (float) (screenHeight + 40) - renderSlide * 72.0f;
        int alpha = (int) (renderAlpha * 200.0f);
        int color = alpha << 24;
        for (int i = 0; i < 9; ++i) {
            int x = startX + i * (slotSize + gap);
            float currentH = Mth.lerp(pt, this.prevSlotH[i], this.slotH[i]);
            float y = baseY - currentH;
            gui.fill(x, (int) y, x + slotSize, (int) y + slotSize, color);
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            gui.renderItem(stack, x + 3, (int) y + 3);
            gui.renderItemDecorations(mc.font, stack, x + 3, (int) y + 3);
        }
    }

    // onCameraSetup: наклон камеры и тряска от усталости
    @SubscribeEvent
    public void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        float pt = (float) event.getPartialTick();
        event.setRoll(event.getRoll()
                + Mth.lerp(pt, this.prevTilt, this.currentTilt));
        this.swayTime += pt * 0.035f;
        if (this.fatigue > 0.01f) {
            float amp = this.fatigue * 0.8f;
            event.setYaw(event.getYaw()
                    + (float) (Math.sin(this.swayTime)
                            + Math.sin(this.swayTime * 0.45f)) * amp);
            event.setPitch(event.getPitch()
                    + (float) (Math.cos(this.swayTime * 0.65f)
                            + Math.cos(this.swayTime * 0.25f)) * (amp * 0.5f));
        }
    }

    // onFOVUpdate: изменение FOV от скорости
    @SubscribeEvent
    public void onFOVUpdate(ViewportEvent.ComputeFov event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && p.isSprinting()) {
            event.setFOV(event.getFOV()
                    + p.getDeltaMovement().length() * 12.0f);
        }
    }
}

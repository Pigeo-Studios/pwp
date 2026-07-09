/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 */
package com.example.aas.client.gui;

import com.example.aas.client.ClientPlacementHandler;
import com.example.aas.item.RallyItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StaticGunRadialScreen
extends Screen {
    static private final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("aas", "textures/gui/radial_sector_4.png");
    private final Screen parentScreen;
    private boolean isSwitching = false;

    public StaticGunRadialScreen(Screen parent) {
        super((Component)Component.literal((String)"Static Guns"));
        this.parentScreen = parent;
    }

    public boolean isPauseScreen() {
        return false;
    }

    protected void init() {
        super.init();
        this.triggerRadioAnim("deploy");
    }

    public void onClose() {
        if (!this.isSwitching) {
            this.triggerRadioAnim("close");
        }
        super.onClose();
    }

    private void triggerRadioAnim(String animName) {
        ItemStack stack;
        Item item;
        if (this.minecraft.player != null && (item = (stack = this.minecraft.player.getMainHandItem()).getItem()) instanceof RallyItem) {
            RallyItem radio = (RallyItem)item;
            long instanceId = stack.getOrCreateTag().getLong("GeckoLibID");
            radio.triggerAnim((Entity)this.minecraft.player, instanceId, "RadioController", animName);
        }
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        int selected = -1;
        if (distance > 10.0) {
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0.0) {
                angle += 360.0;
            }
            selected = angle >= 45.0 && angle < 135.0 ? 1 : (angle >= 135.0 && angle < 225.0 ? 2 : (angle >= 225.0 && angle < 315.0 ? 3 : 0));
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PoseStack pose = gui.pose();
        int size = 95;
        for (int i = 0; i < 4; ++i) {
            pose.pushPose();
            pose.translate((float)centerX, (float)centerY, 0.0f);
            float rot = 0.0f;
            if (i == 0) {
                rot = 90.0f;
            }
            if (i == 1) {
                rot = 180.0f;
            }
            if (i == 2) {
                rot = -90.0f;
            }
            if (i == 3) {
                rot = 0.0f;
            }
            pose.mulPose(Axis.ZP.rotationDegrees(rot));
            boolean isSelected = i == selected;
            float scale = isSelected ? 1.15f : 1.0f;
            pose.scale(scale, scale, 1.0f);
            pose.translate((float)(-size) / 2.0f, (float)(-size), 0.0f);
            if (isSelected) {
                RenderSystem.setShaderColor(0.4f, 1.0f, 0.4f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.blit(SECTOR_TEXTURE, 0, 0, 0.0f, 0.0f, size, size, size, size);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.drawLabel(gui, "M2 Browning", centerX + 60, centerY, selected == 0);
        this.drawLabel(gui, "Mortar", centerX, centerY + 60, selected == 1);
        this.drawLabel(gui, "AGS-30", centerX - 60, centerY, selected == 2);
        this.drawLabel(gui, "TOW", centerX, centerY - 60, selected == 3);
    }

    private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected) {
        int color = selected ? -16711936 : -1;
        int width = this.font.width(text);
        gui.drawString(this.font, text, x - width / 2, y - 4, color, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerY;
        double dy;
        int centerX;
        double dx;
        double dist;
        if (button == 0 && (dist = Math.sqrt((dx = mouseX - (double)(centerX = this.width / 2)) * dx + (dy = mouseY - (double)(centerY = this.height / 2)) * dy)) > 10.0) {
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0.0) {
                angle += 360.0;
            }
            int selected = 0;
            selected = angle >= 45.0 && angle < 135.0 ? 1 : (angle >= 135.0 && angle < 225.0 ? 2 : (angle >= 225.0 && angle < 315.0 ? 3 : 0));
            if (selected == 0) {
                ClientPlacementHandler.startPlacing(20);
            }
            if (selected == 1) {
                ClientPlacementHandler.startPlacing(22);
            }
            if (selected == 2) {
                ClientPlacementHandler.startPlacing(21);
            }
            if (selected == 3) {
                ClientPlacementHandler.startPlacing(23);
            }
            this.onClose();
            return true;
        }
        if (button == 1) {
            Minecraft.getInstance().setScreen(this.parentScreen);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}


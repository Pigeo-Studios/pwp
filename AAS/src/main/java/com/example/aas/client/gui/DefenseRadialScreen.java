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
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRadioAction;
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

public class DefenseRadialScreen
extends Screen {
    static private final ResourceLocation SECTOR_TEXTURE_5 = new ResourceLocation("aas", "textures/gui/radial_sector_5.png");
    private final Screen parentScreen;
    private boolean isSwitching = false;

    public DefenseRadialScreen(Screen parent) {
        super((Component)Component.literal((String)"Defense Menu"));
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
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0.0) {
            angle += 360.0;
        }
        int selected = -1;
        if (distance > 10.0) {
            double shiftedAngle = angle + 36.0;
            if (shiftedAngle >= 360.0) {
                shiftedAngle -= 360.0;
            }
            selected = (int)(shiftedAngle / 72.0);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PoseStack pose = gui.pose();
        int size = 95;
        for (int i = 0; i < 5; ++i) {
            pose.pushPose();
            pose.translate((float)centerX, (float)centerY, 0.0f);
            pose.mulPose(Axis.ZP.rotationDegrees((float)(i * 72)));
            boolean isSelected = i == selected;
            float scale = isSelected ? 1.15f : 1.0f;
            pose.scale(scale, scale, 1.0f);
            pose.translate((float)(-size) / 2.0f, (float)(-size), 0.0f);
            if (isSelected) {
                RenderSystem.setShaderColor(0.4f, 1.0f, 0.4f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.blit(SECTOR_TEXTURE_5, 0, 0, 0.0f, 0.0f, size, size, size, size);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            pose.popPose();
        }
        this.drawLabel(gui, "1x1", centerX, centerY - 75, selected == 0);
        this.drawLabel(gui, "2x2", centerX + 70, centerY - 25, selected == 1);
        this.drawLabel(gui, "3x3", centerX + 45, centerY + 65, selected == 2);
        this.drawLabel(gui, "WIRE", centerX - 45, centerY + 65, selected == 3);
        this.drawLabel(gui, "HUB", centerX - 70, centerY - 25, selected == 4);
    }

    private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected) {
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate((float)x, (float)y, 0.0f);
        float scale = selected ? 1.1f : 0.9f;
        pose.scale(scale, scale, 1.0f);
        int color = selected ? -16711936 : -1;
        int width = this.font.width(text);
        gui.drawString(this.font, text, -width / 2, -4, color, true);
        pose.popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerY;
        double dy;
        int centerX;
        double dx;
        double distance;
        if (button == 0 && (distance = Math.sqrt((dx = mouseX - (double)(centerX = this.width / 2)) * dx + (dy = mouseY - (double)(centerY = this.height / 2)) * dy)) > 10.0) {
            int sector;
            int actionId;
            double shiftedAngle;
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
            if (angle < 0.0) {
                angle += 360.0;
            }
            if ((shiftedAngle = angle + 36.0) >= 360.0) {
                shiftedAngle -= 360.0;
            }
            if ((actionId = 10 + (sector = (int)(shiftedAngle / 72.0))) == 10) {
                ClientPlacementHandler.startPlacing(10);
                this.onClose();
            } else if (actionId == 11) {
                ClientPlacementHandler.startPlacing(11);
                this.onClose();
            } else if (actionId == 12) {
                ClientPlacementHandler.startPlacing(12);
                this.onClose();
            } else if (actionId == 13) {
                ClientPlacementHandler.startPlacing(13);
                this.onClose();
            } else {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketRadioAction(actionId));
                this.onClose();
            }
            return true;
        }
        if (button == 1) {
            Minecraft.getInstance().setScreen(this.parentScreen);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}


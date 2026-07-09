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
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 */
package com.example.aas.client.gui;

import com.example.aas.client.ClientData;
import com.example.aas.client.gui.DefenseRadialScreen;
import com.example.aas.client.gui.StaticGunRadialScreen;
import com.example.aas.item.RallyItem;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRadioAction;
import com.example.aas.world.AASWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RadioRadialScreen
extends Screen {
    static private final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("aas", "textures/gui/radial_sector.png");
    private boolean isSwitching = false;

    public RadioRadialScreen() {
        super((Component)Component.literal((String)"Radio Menu"));
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
        int rallyColor;
        this.renderBackground(gui);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean rallyOnCooldown = false;
        long secondsLeft = 0L;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            String pName = player.getScoreboardName();
            AASWorldData.Squad mySquad = null;
            for (AASWorldData.Squad s : ClientData.clientSquads) {
                if (!s.members.contains(pName)) continue;
                mySquad = s;
                break;
            }
            if (mySquad != null) {
                long cooldownEnd = mySquad.nextRallyAvailableTick;
                long gameTime = player.level().getGameTime();
                if (gameTime < cooldownEnd && !player.isCreative()) {
                    rallyOnCooldown = true;
                    secondsLeft = (cooldownEnd - gameTime) / 20L;
                }
            }
        }
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0.0) {
            angle += 360.0;
        }
        int selected = -1;
        if (distance > 10.0) {
            if (angle > 300.0 || angle <= 60.0) {
                selected = 0;
            } else if (angle > 60.0 && angle <= 180.0) {
                selected = 1;
            } else if (angle > 180.0 && angle <= 300.0) {
                selected = 2;
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PoseStack pose = gui.pose();
        int size = 95;
        for (int i = 0; i < 3; ++i) {
            pose.pushPose();
            pose.translate((float)centerX, (float)centerY, 0.0f);
            pose.mulPose(Axis.ZP.rotationDegrees((float)(i * 120)));
            boolean isSelected = i == selected;
            float scale = isSelected ? 1.15f : 1.0f;
            pose.scale(scale, scale, 1.0f);
            pose.translate((float)(-size) / 2.0f, (float)(-size), 0.0f);
            if (i == 0 && rallyOnCooldown) {
                RenderSystem.setShaderColor(1.0f, 0.4f, 0.4f, 1.0f);
            } else if (isSelected) {
                RenderSystem.setShaderColor(0.4f, 1.0f, 0.4f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.blit(SECTOR_TEXTURE, 0, 0, 0.0f, 0.0f, size, size, size, size);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            pose.popPose();
        }
        Object rallyText = "RALLY POINT";
        int n = rallyColor = selected == 0 ? -16711936 : -1;
        if (rallyOnCooldown) {
            rallyText = "WAIT: " + secondsLeft + "s";
            rallyColor = -43691;
        }
        this.drawLabel(gui, (String)rallyText, centerX, centerY - 70, selected == 0, rallyColor);
        this.drawLabel(gui, "DEFENSES", centerX + 60, centerY + 35, selected == 1, selected == 1 ? -16711936 : -1);
        this.drawLabel(gui, "STATIC GUN", centerX - 60, centerY + 35, selected == 2, selected == 2 ? -16711936 : -1);
    }

    private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected, int color) {
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate((float)x, (float)y, 0.0f);
        float textScale = selected ? 1.1f : 0.9f;
        pose.scale(textScale, textScale, 1.0f);
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
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
            if (angle < 0.0) {
                angle += 360.0;
            }
            int action = -1;
            if (angle > 300.0 || angle <= 60.0) {
                action = 0;
            } else if (angle > 60.0 && angle <= 180.0) {
                action = 1;
            } else if (angle > 180.0 && angle <= 300.0) {
                action = 2;
            }
            if (action == 0) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketRadioAction(0));
                this.onClose();
            }
            if (action == 1) {
                this.isSwitching = true;
                Minecraft.getInstance().setScreen((Screen)new DefenseRadialScreen(this));
            } else if (action == 2) {
                this.isSwitching = true;
                Minecraft.getInstance().setScreen((Screen)new StaticGunRadialScreen(this));
            }
            if (action != -1) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}


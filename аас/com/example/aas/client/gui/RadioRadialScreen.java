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
    private static final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("aas", "textures/gui/radial_sector.png");
    private boolean isSwitching = false;

    public RadioRadialScreen() {
        super((Component)Component.m_237113_((String)"Radio Menu"));
    }

    public boolean m_7043_() {
        return false;
    }

    protected void m_7856_() {
        super.m_7856_();
        this.triggerRadioAnim("deploy");
    }

    public void m_7379_() {
        if (!this.isSwitching) {
            this.triggerRadioAnim("close");
        }
        super.m_7379_();
    }

    private void triggerRadioAnim(String animName) {
        ItemStack stack;
        Item item;
        if (this.f_96541_.f_91074_ != null && (item = (stack = this.f_96541_.f_91074_.m_21205_()).m_41720_()) instanceof RallyItem) {
            RallyItem radio = (RallyItem)item;
            long instanceId = stack.m_41784_().m_128454_("GeckoLibID");
            radio.triggerAnim((Entity)this.f_96541_.f_91074_, instanceId, "RadioController", animName);
        }
    }

    public void m_88315_(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int rallyColor;
        this.m_280273_(gui);
        int centerX = this.f_96543_ / 2;
        int centerY = this.f_96544_ / 2;
        boolean rallyOnCooldown = false;
        long secondsLeft = 0L;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player != null) {
            String pName = player.m_6302_();
            AASWorldData.Squad mySquad = null;
            for (AASWorldData.Squad s : ClientData.clientSquads) {
                if (!s.members.contains(pName)) continue;
                mySquad = s;
                break;
            }
            if (mySquad != null) {
                long cooldownEnd = mySquad.nextRallyAvailableTick;
                long gameTime = player.m_9236_().m_46467_();
                if (gameTime < cooldownEnd && !player.m_7500_()) {
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
        PoseStack pose = gui.m_280168_();
        int size = 95;
        for (int i = 0; i < 3; ++i) {
            pose.m_85836_();
            pose.m_252880_((float)centerX, (float)centerY, 0.0f);
            pose.m_252781_(Axis.f_252403_.m_252977_((float)(i * 120)));
            boolean isSelected = i == selected;
            float scale = isSelected ? 1.15f : 1.0f;
            pose.m_85841_(scale, scale, 1.0f);
            pose.m_252880_((float)(-size) / 2.0f, (float)(-size), 0.0f);
            if (i == 0 && rallyOnCooldown) {
                RenderSystem.setShaderColor((float)1.0f, (float)0.4f, (float)0.4f, (float)1.0f);
            } else if (isSelected) {
                RenderSystem.setShaderColor((float)0.4f, (float)1.0f, (float)0.4f, (float)1.0f);
            } else {
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            }
            gui.m_280163_(SECTOR_TEXTURE, 0, 0, 0.0f, 0.0f, size, size, size, size);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            pose.m_85849_();
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
        PoseStack pose = gui.m_280168_();
        pose.m_85836_();
        pose.m_252880_((float)x, (float)y, 0.0f);
        float textScale = selected ? 1.1f : 0.9f;
        pose.m_85841_(textScale, textScale, 1.0f);
        int width = this.f_96547_.m_92895_(text);
        gui.m_280056_(this.f_96547_, text, -width / 2, -4, color, true);
        pose.m_85849_();
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        int centerY;
        double dy;
        int centerX;
        double dx;
        double distance;
        if (button == 0 && (distance = Math.sqrt((dx = mouseX - (double)(centerX = this.f_96543_ / 2)) * dx + (dy = mouseY - (double)(centerY = this.f_96544_ / 2)) * dy)) > 10.0) {
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
                this.m_7379_();
            }
            if (action == 1) {
                this.isSwitching = true;
                Minecraft.m_91087_().m_91152_((Screen)new DefenseRadialScreen(this));
            } else if (action == 2) {
                this.isSwitching = true;
                Minecraft.m_91087_().m_91152_((Screen)new StaticGunRadialScreen(this));
            }
            if (action != -1) {
                return true;
            }
        }
        return super.m_6375_(mouseX, mouseY, button);
    }
}


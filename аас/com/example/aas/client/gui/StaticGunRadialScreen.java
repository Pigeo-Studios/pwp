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
    private static final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("aas", "textures/gui/radial_sector_4.png");
    private final Screen parentScreen;
    private boolean isSwitching = false;

    public StaticGunRadialScreen(Screen parent) {
        super((Component)Component.m_237113_((String)"Static Guns"));
        this.parentScreen = parent;
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
        this.m_280273_(gui);
        int centerX = this.f_96543_ / 2;
        int centerY = this.f_96544_ / 2;
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
        PoseStack pose = gui.m_280168_();
        int size = 95;
        for (int i = 0; i < 4; ++i) {
            pose.m_85836_();
            pose.m_252880_((float)centerX, (float)centerY, 0.0f);
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
            pose.m_252781_(Axis.f_252403_.m_252977_(rot));
            boolean isSelected = i == selected;
            float scale = isSelected ? 1.15f : 1.0f;
            pose.m_85841_(scale, scale, 1.0f);
            pose.m_252880_((float)(-size) / 2.0f, (float)(-size), 0.0f);
            if (isSelected) {
                RenderSystem.setShaderColor((float)0.4f, (float)1.0f, (float)0.4f, (float)1.0f);
            } else {
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            }
            gui.m_280163_(SECTOR_TEXTURE, 0, 0, 0.0f, 0.0f, size, size, size, size);
            pose.m_85849_();
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.drawLabel(gui, "M2 Browning", centerX + 60, centerY, selected == 0);
        this.drawLabel(gui, "Mortar", centerX, centerY + 60, selected == 1);
        this.drawLabel(gui, "AGS-30", centerX - 60, centerY, selected == 2);
        this.drawLabel(gui, "TOW", centerX, centerY - 60, selected == 3);
    }

    private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected) {
        int color = selected ? -16711936 : -1;
        int width = this.f_96547_.m_92895_(text);
        gui.m_280056_(this.f_96547_, text, x - width / 2, y - 4, color, true);
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        int centerY;
        double dy;
        int centerX;
        double dx;
        double dist;
        if (button == 0 && (dist = Math.sqrt((dx = mouseX - (double)(centerX = this.f_96543_ / 2)) * dx + (dy = mouseY - (double)(centerY = this.f_96544_ / 2)) * dy)) > 10.0) {
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
            this.m_7379_();
            return true;
        }
        if (button == 1) {
            Minecraft.m_91087_().m_91152_(this.parentScreen);
            return true;
        }
        return super.m_6375_(mouseX, mouseY, button);
    }
}


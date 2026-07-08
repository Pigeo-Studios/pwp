/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Button$OnPress
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 */
package com.example.aas.client.gui;

import com.example.aas.config.AASConfig;
import com.example.aas.network.PacketDownedAction;
import com.example.aas.network.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class DownedScreen
extends Screen {
    private static final ResourceLocation HEARTBEAT_ICON = new ResourceLocation("aas", "textures/gui/heartbeat.png");
    private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("aas", "textures/misc/vignette.png");
    private Button callMedicButton;
    private final long screenOpenTime = System.currentTimeMillis();
    private long lastMedicCallTime = 0L;

    public DownedScreen() {
        super((Component)Component.m_237113_((String)"Incapacitated"));
    }

    protected void m_7856_() {
        int cx = this.f_96543_ / 2;
        int bottomY = this.f_96544_ - 50;
        this.m_142416_((GuiEventListener)new SquadButton(cx - 130, bottomY, 120, 24, (Component)Component.m_237113_((String)"GIVE UP"), b -> {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketDownedAction(1));
            this.m_7379_();
        }));
        this.callMedicButton = (Button)this.m_142416_((GuiEventListener)new SquadButton(cx + 10, bottomY, 120, 24, (Component)Component.m_237113_((String)"CALL MEDIC"), b -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastMedicCallTime >= 15000L) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketDownedAction(0));
                this.lastMedicCallTime = currentTime;
            }
        }));
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        this.renderVignette(gui);
        int cx = this.f_96543_ / 2;
        int baseY = this.f_96544_ - 150;
        int boxW = 320;
        int boxH = 135;
        this.renderSquadFrame(gui, cx - boxW / 2, baseY, boxW, boxH);
        RenderSystem.enableBlend();
        gui.m_280163_(HEARTBEAT_ICON, cx - 18, baseY + 10, 0.0f, 0.0f, 36, 36, 36, 36);
        String allyStatus = this.getAllyDistanceStatus();
        gui.m_280137_(this.f_96547_, allyStatus, cx, baseY + 55, 0xFFFFFF);
        int maxSeconds = (Integer)AASConfig.MAX_DOWNED_TIME_SECONDS.get();
        long remainingBleedout = (long)maxSeconds - (System.currentTimeMillis() - this.screenOpenTime) / 1000L;
        if (remainingBleedout < 0L) {
            remainingBleedout = 0L;
        }
        String bleedText = "Bleeding out in: " + remainingBleedout + " s";
        gui.m_280137_(this.f_96547_, bleedText, cx, baseY + 72, 0xAAAAAA);
        long remainingCooldown = 15000L - (System.currentTimeMillis() - this.lastMedicCallTime);
        if (remainingCooldown > 0L) {
            this.callMedicButton.m_93666_((Component)Component.m_237113_((String)("CALL MEDIC " + (remainingCooldown / 1000L + 1L))));
            this.callMedicButton.f_93623_ = false;
        } else {
            this.callMedicButton.m_93666_((Component)Component.m_237113_((String)"CALL MEDIC"));
            this.callMedicButton.f_93623_ = true;
        }
        super.m_88315_(gui, mx, my, pt);
    }

    private String getAllyDistanceStatus() {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return "NO NEARBY ALLIES";
        }
        double minDistance = Double.MAX_VALUE;
        Player closestAlly = null;
        boolean found = false;
        for (Player other : mc.f_91073_.m_6907_()) {
            double dist;
            if (other == mc.f_91074_ || other.m_5833_() || !other.m_6084_() || mc.f_91074_.m_5647_() == null || other.m_5647_() != mc.f_91074_.m_5647_() || !((dist = (double)mc.f_91074_.m_20270_((Entity)other)) < minDistance)) continue;
            minDistance = dist;
            closestAlly = other;
            found = true;
        }
        if (!found || minDistance > 250.0) {
            return "NO NEARBY ALLIES";
        }
        String allyName = closestAlly.m_6302_();
        return String.format("Closest Ally: %d m (%s)", (int)minDistance, allyName);
    }

    private void renderSquadFrame(GuiGraphics gui, int x, int y, int w, int h) {
        gui.m_280509_(x, y, x + w, y + h, -1728053248);
        gui.m_280637_(x, y, w, h, 0x44FFFFFF);
        gui.m_280637_(x + 2, y + 2, w - 4, h - 4, -1140850689);
    }

    private void renderVignette(GuiGraphics gui) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)0.8f, (float)0.0f, (float)0.0f, (float)0.05f);
        gui.m_280163_(VIGNETTE_TEXTURE, 0, 0, 0.0f, 0.0f, this.f_96543_, this.f_96544_, this.f_96543_, this.f_96544_);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)0.8f);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
    }

    public boolean m_6913_() {
        return false;
    }

    private static class SquadButton
    extends Button {
        public SquadButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, f_252438_);
        }

        protected void m_87963_(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
            int borderColor;
            if (!this.f_93624_) {
                return;
            }
            int n = borderColor = this.m_274382_() ? -1 : -6710887;
            if (!this.f_93623_) {
                borderColor = -12303292;
            }
            gui.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, -871296751);
            gui.m_280637_(this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_, borderColor);
            int textColor = this.f_93623_ ? -1 : -8947849;
            gui.m_280653_(Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + (this.f_93619_ - 8) / 2, textColor);
            if (this.f_93623_ && this.m_274382_()) {
                gui.m_280509_(this.m_252754_(), this.m_252907_() + this.f_93619_ - 2, this.m_252754_() + 2, this.m_252907_() + this.f_93619_, -1);
            }
        }
    }
}


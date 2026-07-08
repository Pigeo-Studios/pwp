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
 *  net.minecraft.util.Mth
 */
package com.example.aas.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VictoryScreen
extends Screen {
    private final String winnerName;
    private final String winnerFaction;
    private final String subText;
    private final boolean isBlueWinner;
    private final long openTime;
    private SquadButton continueButton;
    private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("aas", "textures/gui/flags/ukraine.png");
    private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("aas", "textures/gui/flags/russia.png");
    private static final ResourceLocation FLAG_USA = new ResourceLocation("aas", "textures/gui/flags/usa.png");
    private static final ResourceLocation FLAG_NATO = new ResourceLocation("aas", "textures/gui/flags/nato.png");
    private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
    private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("aas", "textures/gui/flags/redfor.png");
    private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("aas", "textures/gui/flags/insurgency.png");
    private static final ResourceLocation FLAG_PMC = new ResourceLocation("aas", "textures/gui/flags/pmc.png");

    public VictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner) {
        super((Component)Component.m_237113_((String)"Victory Screen"));
        this.winnerName = winnerName;
        this.winnerFaction = winnerFaction;
        this.subText = subText;
        this.isBlueWinner = isBlueWinner;
        this.openTime = System.currentTimeMillis();
    }

    protected void m_7856_() {
        int cx = this.f_96543_ / 2;
        int cy = this.f_96544_ / 2;
        this.continueButton = new SquadButton(cx - 70, cy + 65, 140, 24, (Component)Component.m_237113_((String)"CONTINUE"), b -> this.m_7379_());
        this.continueButton.f_93623_ = false;
        this.m_142416_((GuiEventListener)this.continueButton);
    }

    public void m_88315_(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - this.openTime;
        float bgAlpha = Mth.m_14036_((float)((float)elapsed / 1500.0f), (float)0.0f, (float)1.0f);
        float contentAlpha = Mth.m_14036_((float)((float)(elapsed - 1500L) / 2000.0f), (float)0.0f, (float)1.0f);
        int topAlpha = (int)(bgAlpha * 100.0f);
        int bottomAlpha = (int)(bgAlpha * 140.0f);
        int topBg = topAlpha << 24 | 0;
        int bottomBg = bottomAlpha << 24 | 0;
        gui.m_280024_(0, 0, this.f_96543_, this.f_96544_, topBg, bottomBg);
        if (contentAlpha > 0.01f) {
            int cx = this.f_96543_ / 2;
            int cy = this.f_96544_ / 2;
            int alphaInt = (int)(contentAlpha * 255.0f);
            int frameColor = alphaInt << 24 | 0xFFFFFF;
            ResourceLocation flagTex = this.getFlagTexture(this.winnerFaction);
            int flagW = 128;
            int flagH = 72;
            int flagX = cx - flagW / 2;
            int flagY = cy - 95;
            if (flagTex != null) {
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)contentAlpha);
                RenderSystem.enableBlend();
                gui.m_280163_(flagTex, flagX, flagY, 0.0f, 0.0f, flagW, flagH, flagW, flagH);
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                gui.m_280637_(flagX - 1, flagY - 1, flagW + 2, flagH + 2, frameColor);
            } else {
                int fallbackBase = this.isBlueWinner ? 0x3366CC : 0xCC3333;
                gui.m_280509_(flagX, flagY, flagX + flagW, flagY + flagH, alphaInt << 24 | fallbackBase);
                gui.m_280637_(flagX - 1, flagY - 1, flagW + 2, flagH + 2, frameColor);
            }
            RenderSystem.enableBlend();
            int titleColor = alphaInt << 24 | 0xFFFFFF;
            gui.m_280168_().m_85836_();
            gui.m_280168_().m_252880_((float)cx, (float)(cy - 5), 0.0f);
            gui.m_280168_().m_85841_(2.0f, 2.0f, 1.0f);
            gui.m_280137_(this.f_96547_, this.winnerName + " WINS!", 0, 0, titleColor);
            gui.m_280168_().m_85849_();
            int subColor = alphaInt << 24 | 0xAAAAAA;
            gui.m_280137_(this.f_96547_, this.subText, cx, cy + 25, subColor);
            RenderSystem.disableBlend();
        }
        this.continueButton.currentAlpha = contentAlpha;
        if (contentAlpha >= 1.0f && !this.continueButton.f_93623_) {
            this.continueButton.f_93623_ = true;
        }
        super.m_88315_(gui, mouseX, mouseY, partialTick);
    }

    public boolean m_7043_() {
        return false;
    }

    private ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equals("none")) {
            return null;
        }
        switch (faction.toLowerCase()) {
            case "ukraine": {
                return FLAG_UKRAINE;
            }
            case "russia": {
                return FLAG_RUSSIA;
            }
            case "usa": {
                return FLAG_USA;
            }
            case "nato": {
                return FLAG_NATO;
            }
            case "bluefor": {
                return FLAG_BLUEFOR;
            }
            case "redfor": {
                return FLAG_REDFOR;
            }
            case "insurgency": {
                return FLAG_INSURGENCY;
            }
            case "pmc": {
                return FLAG_PMC;
            }
        }
        return null;
    }

    private static class SquadButton
    extends Button {
        public float currentAlpha = 0.0f;

        public SquadButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, f_252438_);
        }

        protected void m_87963_(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
            int borderBase;
            if (!this.f_93624_ || this.currentAlpha <= 0.02f) {
                return;
            }
            int a = (int)(this.currentAlpha * 255.0f);
            int bgCol = a << 24 | 0x111111;
            int n = borderBase = this.m_274382_() ? 0xFFFFFF : 0x999999;
            if (!this.f_93623_) {
                borderBase = 0x444444;
            }
            int borderCol = a << 24 | borderBase;
            RenderSystem.enableBlend();
            gui.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, bgCol);
            gui.m_280637_(this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_, borderCol);
            int textBase = this.f_93623_ ? 0xFFFFFF : 0x777777;
            int textCol = a << 24 | textBase;
            gui.m_280653_(Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + (this.f_93619_ - 8) / 2, textCol);
            if (this.f_93623_ && this.m_274382_()) {
                gui.m_280509_(this.m_252754_(), this.m_252907_() + this.f_93619_ - 2, this.m_252754_() + 2, this.m_252907_() + this.f_93619_, a << 24 | 0xFFFFFF);
            }
            RenderSystem.disableBlend();
        }
    }
}


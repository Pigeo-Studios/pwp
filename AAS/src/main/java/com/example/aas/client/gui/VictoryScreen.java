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
    static private final ResourceLocation FLAG_UKRAINE = new ResourceLocation("aas", "textures/gui/flags/ukraine.png");
    static private final ResourceLocation FLAG_RUSSIA = new ResourceLocation("aas", "textures/gui/flags/russia.png");
    static private final ResourceLocation FLAG_USA = new ResourceLocation("aas", "textures/gui/flags/usa.png");
    static private final ResourceLocation FLAG_NATO = new ResourceLocation("aas", "textures/gui/flags/nato.png");
    static private final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
    static private final ResourceLocation FLAG_REDFOR = new ResourceLocation("aas", "textures/gui/flags/redfor.png");
    static private final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("aas", "textures/gui/flags/insurgency.png");
    static private final ResourceLocation FLAG_PMC = new ResourceLocation("aas", "textures/gui/flags/pmc.png");

    public VictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner) {
        super((Component)Component.literal((String)"Victory Screen"));
        this.winnerName = winnerName;
        this.winnerFaction = winnerFaction;
        this.subText = subText;
        this.isBlueWinner = isBlueWinner;
        this.openTime = System.currentTimeMillis();
    }

    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.continueButton = new SquadButton(cx - 70, cy + 65, 140, 24, (Component)Component.literal((String)"CONTINUE"), b -> this.onClose());
        this.continueButton.active = false;
        this.addRenderableWidget((GuiEventListener)this.continueButton);
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - this.openTime;
        float bgAlpha = Mth.clamp((float)((float)elapsed / 1500.0f), 0.0f, 1.0f);
        float contentAlpha = Mth.clamp((float)((float)(elapsed - 1500L) / 2000.0f), 0.0f, 1.0f);
        int topAlpha = (int)(bgAlpha * 100.0f);
        int bottomAlpha = (int)(bgAlpha * 140.0f);
        int topBg = topAlpha << 24 | 0;
        int bottomBg = bottomAlpha << 24 | 0;
        gui.fillGradient(0, 0, this.width, this.height, topBg, bottomBg);
        if (contentAlpha > 0.01f) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int alphaInt = (int)(contentAlpha * 255.0f);
            int frameColor = alphaInt << 24 | 0xFFFFFF;
            ResourceLocation flagTex = this.getFlagTexture(this.winnerFaction);
            int flagW = 128;
            int flagH = 72;
            int flagX = cx - flagW / 2;
            int flagY = cy - 95;
            if (flagTex != null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float)contentAlpha);
                RenderSystem.enableBlend();
                gui.blit(flagTex, flagX, flagY, 0.0f, 0.0f, flagW, flagH, flagW, flagH);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                gui.renderOutline(flagX - 1, flagY - 1, flagW + 2, flagH + 2, frameColor);
            } else {
                int fallbackBase = this.isBlueWinner ? 0x3366CC : 0xCC3333;
                gui.fill(flagX, flagY, flagX + flagW, flagY + flagH, alphaInt << 24 | fallbackBase);
                gui.renderOutline(flagX - 1, flagY - 1, flagW + 2, flagH + 2, frameColor);
            }
            RenderSystem.enableBlend();
            int titleColor = alphaInt << 24 | 0xFFFFFF;
            gui.pose().pushPose();
            gui.pose().translate((float)cx, (float)(cy - 5), 0.0f);
            gui.pose().scale(2.0f, 2.0f, 1.0f);
            gui.drawCenteredString(this.font, this.winnerName + " WINS!", 0, 0, titleColor);
            gui.pose().popPose();
            int subColor = alphaInt << 24 | 0xAAAAAA;
            gui.drawCenteredString(this.font, this.subText, cx, cy + 25, subColor);
            RenderSystem.disableBlend();
        }
        this.continueButton.currentAlpha = contentAlpha;
        if (contentAlpha >= 1.0f && !this.continueButton.active) {
            this.continueButton.active = true;
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }

    public boolean isPauseScreen() {
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
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
            int borderBase;
            if (!this.visible || this.currentAlpha <= 0.02f) {
                return;
            }
            int a = (int)(this.currentAlpha * 255.0f);
            int bgCol = a << 24 | 0x111111;
            int n = borderBase = this.isHovered() ? 0xFFFFFF : 0x999999;
            if (!this.active) {
                borderBase = 0x444444;
            }
            int borderCol = a << 24 | borderBase;
            RenderSystem.enableBlend();
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgCol);
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderCol);
            int textBase = this.active ? 0xFFFFFF : 0x777777;
            int textCol = a << 24 | textBase;
            gui.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textCol);
            if (this.active && this.isHovered()) {
                gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height, a << 24 | 0xFFFFFF);
            }
            RenderSystem.disableBlend();
        }
    }
}


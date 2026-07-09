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
    static private final ResourceLocation HEARTBEAT_ICON = new ResourceLocation("aas", "textures/gui/heartbeat.png");
    static private final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("aas", "textures/misc/vignette.png");
    private Button callMedicButton;
    private final long screenOpenTime = System.currentTimeMillis();
    private long lastMedicCallTime = 0L;

    public DownedScreen() {
        super((Component)Component.literal((String)"Incapacitated"));
    }

    protected void init() {
        int cx = this.width / 2;
        int bottomY = this.height - 50;
        this.addRenderableWidget((GuiEventListener)new SquadButton(cx - 130, bottomY, 120, 24, (Component)Component.literal((String)"GIVE UP"), b -> {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketDownedAction(1));
            this.onClose();
        }));
        this.callMedicButton = (Button)this.addRenderableWidget((GuiEventListener)new SquadButton(cx + 10, bottomY, 120, 24, (Component)Component.literal((String)"CALL MEDIC"), b -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastMedicCallTime >= 15000L) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketDownedAction(0));
                this.lastMedicCallTime = currentTime;
            }
        }));
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        this.renderVignette(gui);
        int cx = this.width / 2;
        int baseY = this.height - 150;
        int boxW = 320;
        int boxH = 135;
        this.renderSquadFrame(gui, cx - boxW / 2, baseY, boxW, boxH);
        RenderSystem.enableBlend();
        gui.blit(HEARTBEAT_ICON, cx - 18, baseY + 10, 0.0f, 0.0f, 36, 36, 36, 36);
        String allyStatus = this.getAllyDistanceStatus();
        gui.drawCenteredString(this.font, allyStatus, cx, baseY + 55, 0xFFFFFF);
        int maxSeconds = (Integer)AASConfig.MAX_DOWNED_TIME_SECONDS.get();
        long remainingBleedout = (long)maxSeconds - (System.currentTimeMillis() - this.screenOpenTime) / 1000L;
        if (remainingBleedout < 0L) {
            remainingBleedout = 0L;
        }
        String bleedText = "Bleeding out in: " + remainingBleedout + " s";
        gui.drawCenteredString(this.font, bleedText, cx, baseY + 72, 0xAAAAAA);
        long remainingCooldown = 15000L - (System.currentTimeMillis() - this.lastMedicCallTime);
        if (remainingCooldown > 0L) {
            this.callMedicButton.setMessage((Component)Component.literal((String)("CALL MEDIC " + (remainingCooldown / 1000L + 1L))));
            this.callMedicButton.active = false;
        } else {
            this.callMedicButton.setMessage((Component)Component.literal((String)"CALL MEDIC"));
            this.callMedicButton.active = true;
        }
        super.render(gui, mx, my, pt);
    }

    private String getAllyDistanceStatus() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return "NO NEARBY ALLIES";
        }
        double minDistance = Double.MAX_VALUE;
        Player closestAlly = null;
        boolean found = false;
        for (Player other : mc.level.players()) {
            double dist;
            if (other == mc.player || other.isSpectator() || !other.isAlive() || mc.player.getTeam() == null || other.getTeam() != mc.player.getTeam() || !((dist = (double)mc.player.distanceTo((Entity)other)) < minDistance)) continue;
            minDistance = dist;
            closestAlly = other;
            found = true;
        }
        if (!found || minDistance > 250.0) {
            return "NO NEARBY ALLIES";
        }
        String allyName = closestAlly.getScoreboardName();
        return String.format("Closest Ally: %d m (%s)", (int)minDistance, allyName);
    }

    private void renderSquadFrame(GuiGraphics gui, int x, int y, int w, int h) {
        gui.fill(x, y, x + w, y + h, -1728053248);
        gui.renderOutline(x, y, w, h, 0x44FFFFFF);
        gui.renderOutline(x + 2, y + 2, w - 4, h - 4, -1140850689);
    }

    private void renderVignette(GuiGraphics gui) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.8f, 0.0f, 0.0f, 0.05f);
        gui.blit(VIGNETTE_TEXTURE, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static class SquadButton
    extends Button {
        public SquadButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
            int borderColor;
            if (!this.visible) {
                return;
            }
            int n = borderColor = this.isHovered() ? -1 : -6710887;
            if (!this.active) {
                borderColor = -12303292;
            }
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -871296751);
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
            int textColor = this.active ? -1 : -8947849;
            gui.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
            if (this.active && this.isHovered()) {
                gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height, -1);
            }
        }
    }
}


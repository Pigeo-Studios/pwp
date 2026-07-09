/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.math.Axis
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package com.example.aas.client.gui;

import com.example.aas.client.AASDeathScreen;
import com.example.aas.client.gui.SquadSelectionScreen;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSquadMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SquadMarkerRadialScreen
extends Screen {
    static private final ResourceLocation SECTOR_3 = new ResourceLocation("aas", "textures/gui/radial_sector.png");
    static private final ResourceLocation SECTOR_4 = new ResourceLocation("aas", "textures/gui/radial_sector_4.png");
    private final int targetX;
    private final int targetZ;
    private int currentLayer = 1;

    public SquadMarkerRadialScreen(int x, int z) {
        super((Component)Component.literal((String)"Markers"));
        this.targetX = x;
        this.targetZ = z;
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (!this.minecraft.player.isAlive()) {
            gui.fill(0, 0, this.width, this.height, -16777216);
        } else {
            this.renderBackground(gui);
        }
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (this.currentLayer == 0) {
            this.renderMainLayer(gui, dx, dy, dist, centerX, centerY);
        } else {
            this.renderSquadLayer(gui, dx, dy, dist, centerX, centerY);
        }
    }

    private void renderMainLayer(GuiGraphics gui, double dx, double dy, double dist, int cx, int cy) {
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0.0) {
            angle += 360.0;
        }
        int selected = -1;
        if (dist > 10.0) {
            selected = angle > 300.0 || angle <= 60.0 ? 0 : (angle > 60.0 && angle <= 180.0 ? 1 : 2);
        }
        for (int i = 0; i < 3; ++i) {
            gui.pose().pushPose();
            gui.pose().translate((float)cx, (float)cy, 0.0f);
            gui.pose().mulPose(Axis.ZP.rotationDegrees((float)(i * 120)));
            if (i == selected) {
                RenderSystem.setShaderColor(0.4f, 1.0f, 0.4f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.pose().translate(-47.5, -95.0, 0.0);
            gui.blit(SECTOR_3, 0, 0, 0.0f, 0.0f, 95, 95, 95, 95);
            gui.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.drawLabel(gui, "TEAM", cx, cy - 70, selected == 0, -11141291);
        this.drawLabel(gui, "ENEMY", cx + 60, cy + 30, selected == 1, -43691);
        this.drawLabel(gui, "SQUAD", cx - 60, cy + 30, selected == 2, -171);
    }

    private void renderSquadLayer(GuiGraphics gui, double dx, double dy, double dist, int cx, int cy) {
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0.0) {
            angle += 360.0;
        }
        int selected = -1;
        if (dist > 10.0) {
            selected = angle >= 45.0 && angle < 135.0 ? 1 : (angle >= 135.0 && angle < 225.0 ? 2 : (angle >= 225.0 && angle < 315.0 ? 3 : 0));
        }
        for (int i = 0; i < 4; ++i) {
            gui.pose().pushPose();
            gui.pose().translate((float)cx, (float)cy, 0.0f);
            float rot = i == 0 ? 90.0f : (i == 1 ? 180.0f : (i == 2 ? -90.0f : 0.0f));
            gui.pose().mulPose(Axis.ZP.rotationDegrees(rot));
            if (i == selected) {
                RenderSystem.setShaderColor(0.4f, 1.0f, 0.4f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.pose().translate(-47.5, -95.0, 0.0);
            gui.blit(SECTOR_4, 0, 0, 0.0f, 0.0f, 95, 95, 95, 95);
            gui.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.drawLabel(gui, "MOVE", cx + 60, cy, selected == 0, -11141291);
        this.drawLabel(gui, "ATTACK", cx, cy + 60, selected == 1, -22016);
        this.drawLabel(gui, "DEFEND", cx - 60, cy, selected == 2, -11184641);
        this.drawLabel(gui, "BUILD", cx, cy - 60, selected == 3, -43521);
    }

    private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean sel, int color) {
        gui.drawCenteredString(this.font, text, x, y - 4, sel ? -1 : color);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        double dx = mx - (double)cx;
        double dy = my - (double)cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (button == 0 && dist > 10.0) {
            if (this.currentLayer == 0) {
                int sel;
                double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
                if (angle < 0.0) {
                    angle += 360.0;
                }
                int n = angle > 300.0 || angle <= 60.0 ? 0 : (sel = angle > 60.0 && angle <= 180.0 ? 1 : 2);
                if (sel == 2) {
                    this.currentLayer = 1;
                    return true;
                }
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadMarker(this.targetX, this.targetZ, sel + 4));
                if (!this.minecraft.player.isAlive()) {
                    this.minecraft.setScreen((Screen)new AASDeathScreen(null, false));
                } else {
                    this.minecraft.setScreen((Screen)new SquadSelectionScreen());
                }
            } else {
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                if (angle < 0.0) {
                    angle += 360.0;
                }
                int type = angle >= 45.0 && angle < 135.0 ? 1 : (angle >= 135.0 && angle < 225.0 ? 2 : (angle >= 225.0 && angle < 315.0 ? 3 : 0));
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadMarker(this.targetX, this.targetZ, type));
                if (!this.minecraft.player.isAlive()) {
                    this.minecraft.setScreen((Screen)new AASDeathScreen(null, false));
                } else {
                    this.minecraft.setScreen((Screen)new SquadSelectionScreen());
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    public boolean isPauseScreen() {
        return false;
    }
}


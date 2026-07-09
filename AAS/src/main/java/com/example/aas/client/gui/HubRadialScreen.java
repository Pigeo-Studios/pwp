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
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package com.example.aas.client.gui;

import com.example.aas.block.HubBlockEntity;
import com.example.aas.client.ClientData;
import com.example.aas.config.AASConfig;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRequestAmmo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

public class HubRadialScreen
extends Screen {
    static private final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("aas", "textures/gui/radial_sector_5.png");
    private final BlockPos hubPos;
    private int cdAmmo = 0;
    private int cdAGS = 0;
    private int cdM2 = 0;
    private int cdMortar = 0;
    private int cdTOW = 0;
    private int materials = 0;

    public HubRadialScreen(BlockPos pos) {
        super((Component)Component.literal((String)"Hub Supply"));
        this.hubPos = pos;
    }

    public boolean isPauseScreen() {
        return false;
    }

    private void updateData() {
        BlockEntity be;
        long elapsed = System.currentTimeMillis() - ClientData.lastFobResupplyTime;
        this.cdAmmo = elapsed < 60000L && !Minecraft.getInstance().player.isCreative() ? (int)((60000L - elapsed) / 50L) : 0;
        if (Minecraft.getInstance().level != null && (be = Minecraft.getInstance().level.getBlockEntity(this.hubPos)) instanceof HubBlockEntity) {
            HubBlockEntity hub = (HubBlockEntity)be;
            this.cdAGS = hub.cooldownAGS;
            this.cdM2 = hub.cooldownM2;
            this.cdMortar = hub.cooldownMortar;
            this.cdTOW = hub.cooldownTOW;
            this.materials = hub.getMaterials();
        }
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        this.updateData();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        String matText = "Mats: " + this.materials;
        gui.drawCenteredString(this.font, matText, centerX, centerY + 5, -22016);
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        int selected = -1;
        if (distance > 10.0) {
            double shiftedAngle;
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
            if (angle < 0.0) {
                angle += 360.0;
            }
            if ((shiftedAngle = angle + 36.0) >= 360.0) {
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
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            boolean onCooldown = false;
            boolean noMats = false;
            if (i == 0) {
                if (this.cdAmmo > 0) {
                    onCooldown = true;
                }
                if (this.materials < (Integer)AASConfig.HUB_RESUPPLY_COST.get()) {
                    noMats = true;
                }
            } else if (i == 1) {
                if (this.cdAGS > 0) {
                    onCooldown = true;
                }
                if (this.materials < 20) {
                    noMats = true;
                }
            } else if (i == 2) {
                if (this.cdM2 > 0) {
                    onCooldown = true;
                }
                if (this.materials < 15) {
                    noMats = true;
                }
            } else if (i == 3) {
                if (this.cdMortar > 0) {
                    onCooldown = true;
                }
                if (this.materials < 20) {
                    noMats = true;
                }
            } else if (i == 4) {
                if (this.cdTOW > 0) {
                    onCooldown = true;
                }
                if (this.materials < 50) {
                    noMats = true;
                }
            }
            if (onCooldown || noMats) {
                r = 1.0f;
                g = 0.4f;
                b = 0.4f;
            } else if (isSelected) {
                r = 0.4f;
                g = 1.0f;
                b = 0.4f;
            }
            RenderSystem.setShaderColor((float)r, (float)g, (float)b, 1.0f);
            gui.blit(SECTOR_TEXTURE, 0, 0, 0.0f, 0.0f, size, size, size, size);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            pose.popPose();
        }
        this.drawLabel(gui, "Resupply (" + String.valueOf(AASConfig.HUB_RESUPPLY_COST.get()) + ")", centerX, centerY - 75, selected == 0, this.cdAmmo);
        this.drawLabel(gui, "AGS-30 (20)", centerX + 70, centerY - 25, selected == 1, this.cdAGS);
        this.drawLabel(gui, "M2 (15)", centerX + 45, centerY + 65, selected == 2, this.cdM2);
        this.drawLabel(gui, "Mortar (20)", centerX - 45, centerY + 65, selected == 3, this.cdMortar);
        this.drawLabel(gui, "TOW (50)", centerX - 70, centerY - 25, selected == 4, this.cdTOW);
    }

    private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected, int cooldownTicks) {
        int color;
        int n = color = selected ? -16711936 : -1;
        if (cooldownTicks > 0) {
            text = cooldownTicks / 20 + "s";
            color = -43691;
        }
        int width = this.font.width((String)text);
        gui.drawString(this.font, (String)text, x - width / 2, y - 4, color, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerY;
        double dy;
        int centerX;
        double dx;
        double dist;
        if (button == 0 && (dist = Math.sqrt((dx = mouseX - (double)(centerX = this.width / 2)) * dx + (dy = mouseY - (double)(centerY = this.height / 2)) * dy)) > 10.0) {
            int sector;
            double shiftedAngle;
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
            if (angle < 0.0) {
                angle += 360.0;
            }
            if ((shiftedAngle = angle + 36.0) >= 360.0) {
                shiftedAngle -= 360.0;
            }
            if ((sector = (int)(shiftedAngle / 72.0)) == 0) {
                if (this.cdAmmo > 0) {
                    return true;
                }
                if (this.materials >= (Integer)AASConfig.HUB_RESUPPLY_COST.get() || Minecraft.getInstance().player.isCreative()) {
                    ClientData.lastFobResupplyTime = System.currentTimeMillis();
                }
            }
            PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestAmmo(this.hubPos, sector));
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}


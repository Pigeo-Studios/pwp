/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package com.example.aas.client.gui;

import com.example.aas.client.AASDeathScreen;
import com.example.aas.client.gui.MapMarkerGridScreen;
import com.example.aas.client.gui.SquadMarkerRadialScreen;
import com.example.aas.client.gui.SquadSelectionScreen;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSquadMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TacticalMapRadialScreen
extends Screen {
    private final int wx;
    private final int wz;
    static private final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("aas", "textures/gui/radial_sector.png");

    public TacticalMapRadialScreen(int x, int z) {
        super((Component)Component.literal((String)"Tactical Menu"));
        this.wx = x;
        this.wz = z;
    }

    public boolean isPauseScreen() {
        return false;
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
            if (isSelected) {
                RenderSystem.setShaderColor(0.4f, 1.0f, 0.4f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.blit(SECTOR_TEXTURE, 0, 0, 0.0f, 0.0f, size, size, size, size);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.drawLabel(gui, "TEAM", centerX, centerY - 70, selected == 0);
        this.drawLabel(gui, "ENEMY", centerX + 60, centerY + 30, selected == 1);
        this.drawLabel(gui, "SQUAD", centerX - 60, centerY + 30, selected == 2);
        int cx = this.width / 2;
        int cy = this.height / 2;
        boolean hoverCenter = distance < 20.0;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (hoverCenter) {
            RenderSystem.setShaderColor(0.8f, 0.8f, 0.8f, 1.0f);
        }
        gui.blit(new ResourceLocation("aas", "textures/gui/map_icons/player_circle.png"), cx - 12, cy - 12, 0.0f, 0.0f, 24, 24, 24, 24);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected) {
        int color = selected ? -16711936 : -1;
        gui.drawCenteredString(this.font, text, x, y - 4, color);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            int centerX = this.width / 2;
            double dx = mx - (double)centerX;
            int centerY = this.height / 2;
            double dy = my - (double)centerY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 20.0) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadMarker(this.wx, this.wz, 6));
                if (!this.minecraft.player.isAlive()) {
                    this.minecraft.setScreen((Screen)new AASDeathScreen(null, false));
                } else {
                    this.minecraft.setScreen((Screen)new SquadSelectionScreen());
                }
                return true;
            }
            if (dist < 10.0) {
                return false;
            }
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
                this.openGrid("Team Markers", this.getTeamMarkers());
            } else if (action == 1) {
                this.openGrid("Enemy Markers", this.getEnemyMarkers());
            } else if (action == 2) {
                this.minecraft.setScreen((Screen)new SquadMarkerRadialScreen(this.wx, this.wz));
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void openGrid(String title, Map<String, ResourceLocation> markers) {
        this.minecraft.setScreen((Screen)new MapMarkerGridScreen(this.wx, this.wz, title, markers));
    }

    private Map<String, ResourceLocation> getTeamMarkers() {
        LinkedHashMap<String, ResourceLocation> m = new LinkedHashMap<String, ResourceLocation>();
        m.put("Supply Request", new ResourceLocation("aas", "textures/gui/map_icons/supply_request_marker.png"));
        m.put("Artillery Request", new ResourceLocation("aas", "textures/gui/map_icons/artillery_request_marker.png"));
        return m;
    }

    private Map<String, ResourceLocation> getEnemyMarkers() {
        LinkedHashMap<String, ResourceLocation> m = new LinkedHashMap<String, ResourceLocation>();
        m.put("Infantry", new ResourceLocation("aas", "textures/gui/map_icons/infantry_marker.png"));
        m.put("Sniper", new ResourceLocation("aas", "textures/gui/map_icons/sniper_marker.png"));
        m.put("HAT", new ResourceLocation("aas", "textures/gui/map_icons/hat_marker.png"));
        m.put("Mortar", new ResourceLocation("aas", "textures/gui/map_icons/mortar_marker.png"));
        m.put("TOW", new ResourceLocation("aas", "textures/gui/map_icons/tow_marker.png"));
        m.put("Enemy HUB", new ResourceLocation("aas", "textures/gui/map_icons/hub_marker.png"));
        m.put("Enemy Rally", new ResourceLocation("aas", "textures/gui/map_icons/rally_marker.png"));
        m.put("Combat Vehicle", new ResourceLocation("aas", "textures/gui/map_icons/combat_vehicle_marker.png"));
        m.put("Infantry Vehicle", new ResourceLocation("aas", "textures/gui/map_icons/infantry_vehicle_marker.png"));
        m.put("APC", new ResourceLocation("aas", "textures/gui/map_icons/apc_marker.png"));
        m.put("Tank", new ResourceLocation("aas", "textures/gui/map_icons/tank_marker.png"));
        m.put("Helicopter", new ResourceLocation("aas", "textures/gui/map_icons/helicopter_marker.png"));
        m.put("CAS Heli", new ResourceLocation("aas", "textures/gui/map_icons/cas_helicopter_marker.png"));
        m.put("CAS Fighter", new ResourceLocation("aas", "textures/gui/map_icons/cas_fighter_marker.png"));
        m.put("Mobile ZU", new ResourceLocation("aas", "textures/gui/map_icons/mobile_zu_marker.png"));
        m.put("Supply Truck", new ResourceLocation("aas", "textures/gui/map_icons/supply_truck_marker.png"));
        return m;
    }
}


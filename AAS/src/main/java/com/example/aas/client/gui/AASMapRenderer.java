/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  com.mojang.blaze3d.platform.InputConstants$Key
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.lwjgl.glfw.GLFW
 */
package com.example.aas.client.gui;

import com.example.aas.client.ClientData;
import com.example.aas.client.ModKeyBindings;
import com.example.aas.client.gui.TacticalMapRadialScreen;
import com.example.aas.config.AASConfig;
import com.example.aas.network.MapPlayerInfo;
import com.example.aas.world.AASWorldData;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class AASMapRenderer
implements AutoCloseable {
    static private final Map<String, ResourceLocation> MAP_ICONS_CACHE = new HashMap<String, ResourceLocation>();
    private int mapX;
    private int mapY;
    private int mapSize;
    public String selectedSpawnId = "";
    private double mapPanX = 0.0;
    private double mapPanZ = 0.0;
    private boolean isDraggingMap = false;
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;
    static private final ResourceLocation MARKER_MOVE = new ResourceLocation("aas", "textures/gui/map_icons/marker_move.png");
    static private final ResourceLocation MARKER_ATTACK = new ResourceLocation("aas", "textures/gui/map_icons/marker_attack.png");
    static private final ResourceLocation MARKER_DEFEND = new ResourceLocation("aas", "textures/gui/map_icons/marker_defend.png");
    static private final ResourceLocation MARKER_BUILD = new ResourceLocation("aas", "textures/gui/map_icons/marker_build.png");
    static private final ResourceLocation FLAG_NEUTRAL = new ResourceLocation("aas", "textures/gui/flags/neutral.png");
    static private final ResourceLocation ICON_CIRCLE = new ResourceLocation("aas", "textures/gui/map_icons/player_circle.png");
    static private final ResourceLocation ICON_PLUS = new ResourceLocation("aas", "textures/gui/map_icons/medic_plus.png");
    static private final ResourceLocation ICON_PLAYER_SELF = new ResourceLocation("aas", "textures/gui/map_icons/player_self.png");
    static private final ResourceLocation MAP_GRID_TEXTURE = new ResourceLocation("aas", "textures/gui/map_grid.png");
    static private final ResourceLocation HUB_ICON = new ResourceLocation("aas", "textures/gui/map_icons/hub_icon.png");
    static private final ResourceLocation RALLY_ICON = new ResourceLocation("aas", "textures/gui/map_icons/rally_icon.png");
    static private final ResourceLocation MAIN_BASE_ICON = new ResourceLocation("aas", "textures/gui/map_icons/main_base.png");
    static private final ResourceLocation ICON_OBJ_ATTACK = new ResourceLocation("aas", "textures/gui/map_icons/objective_attack.png");
    static private final ResourceLocation ICON_OBJ_DEFEND = new ResourceLocation("aas", "textures/gui/map_icons/objective_defend.png");
    static private final ResourceLocation HUB_SELECTED_ICON = new ResourceLocation("aas", "textures/gui/map_icons/hub_icon_selected.png");
    static private final ResourceLocation RALLY_SELECTED_ICON = new ResourceLocation("aas", "textures/gui/map_icons/rally_icon_selected.png");
    static private final ResourceLocation MAIN_SELECTED_ICON = new ResourceLocation("aas", "textures/gui/map_icons/main_base_selected.png");
    static private final ResourceLocation MATS_ICON = new ResourceLocation("aas", "textures/gui/mats_icon.png");
    static private final Map<String, ResourceLocation> VEHICLE_ICONS = new HashMap<String, ResourceLocation>();

    private ResourceLocation getCurrentMapTexture() {
        String img = ClientData.currentMapImage;
        if (img == null || img.isEmpty()) {
            img = "map1";
        }
        return MAP_ICONS_CACHE.computeIfAbsent(img, k -> new ResourceLocation("aas", "textures/gui/maps/" + k + ".png"));
    }

    private double getMapScale() {
        return ClientData.mapScale;
    }

    public int getMapX() {
        return this.mapX;
    }

    public int getMapY() {
        return this.mapY;
    }

    public int getMapSize() {
        return this.mapSize;
    }

    private ResourceLocation getMarkerIcon(String type) {
        String path = type.toLowerCase().replace("enemy ", "").replace(" ", "_");
        return new ResourceLocation("aas", "textures/gui/map_icons/" + path + "_marker.png");
    }

    private void setFilter(ResourceLocation tex, boolean smooth) {
        Minecraft.getInstance().getTextureManager().getTexture(tex).setFilter(smooth, false);
    }

    public void init(int x, int y, int size) {
        this.mapX = x;
        this.mapY = y;
        this.mapSize = size;
    }

    private void drawSquadNumber(GuiGraphics gui, Font font, String text, int x, int y, int color) {
        gui.drawString(font, text, x - 1, y, -16777216, false);
        gui.drawString(font, text, x + 1, y, -16777216, false);
        gui.drawString(font, text, x, y - 1, -16777216, false);
        gui.drawString(font, text, x, y + 1, -16777216, false);
        gui.drawString(font, text, x, y, color, false);
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) {
            return;
        }
        double currentScale = ClientData.mapScale;
        double cx = localPlayer.getX() + this.mapPanX;
        double cz = localPlayer.getZ() + this.mapPanZ;
        gui.fillGradient(this.mapX, this.mapY, this.mapX + this.mapSize, this.mapY + this.mapSize, -15064016, -16448251);
        gui.enableScissor(this.mapX, this.mapY, this.mapX + this.mapSize, this.mapY + this.mapSize);
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate((double)this.mapX + (double)this.mapSize / 2.0, (double)this.mapY + (double)this.mapSize / 2.0, 0.0);
        float scale = 1.0f / (float)currentScale;
        pose.scale(scale, scale, 1.0f);
        int s = ClientData.mapSizeBlocks;
        float drawX = (float)((double)ClientData.mapCenterX - (double)s / 2.0 - cx);
        float drawY = (float)((double)ClientData.mapCenterZ - (double)s / 2.0 - cz);
        ResourceLocation dynamicMapTexture = this.getCurrentMapTexture();
        this.setFilter(dynamicMapTexture, true);
        RenderSystem.setShaderTexture(0, (ResourceLocation)dynamicMapTexture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        gui.blit(dynamicMapTexture, (int)drawX, (int)drawY, s, s, 0.0f, 0.0f, 1024, 1024, 1024, 1024);
        this.setFilter(dynamicMapTexture, false);
        this.setFilter(MAP_GRID_TEXTURE, true);
        RenderSystem.setShaderTexture(0, (ResourceLocation)MAP_GRID_TEXTURE);
        RenderSystem.enableBlend();
        gui.blit(MAP_GRID_TEXTURE, (int)drawX, (int)drawY, s, s, 0.0f, 0.0f, 1024, 1024, 1024, 1024);
        this.setFilter(MAP_GRID_TEXTURE, false);
        pose.popPose();
        this.renderLatticeLines(gui, mc, cx, cz, currentScale);
        this.renderOverlays(gui, mc, cx, cz, currentScale);
        this.renderMainBases(gui, mc, cx, cz, currentScale);
        this.renderArtilleryZones(gui, cx, cz, currentScale);
        this.renderStructures(gui, mc, cx, cz, currentScale);
        this.renderVehicles(gui, mc, cx, cz, currentScale);
        this.renderAllPlayers(gui, mc, localPlayer, cx, cz, currentScale);
        this.renderSquadMarkerLogic(gui, mc, cx, cz, currentScale);
        this.renderTacticalMarkers(gui, mc, cx, cz, currentScale);
        this.renderSquadRhombusMarkers(gui, mc, cx, cz, currentScale);
        this.renderSquadPings(gui, cx, cz, currentScale);
        gui.disableScissor();
    }

    private void renderOverlays(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        if (ClientData.allCapturePoints == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        boolean blinkOn = System.currentTimeMillis() / 400L % 2L == 0L;
        for (AASWorldData.CapturePoint cp : ClientData.allCapturePoints) {
            Vec3 center = cp.area.getCenter();
            double dx = (center.x - cx) / bpp;
            int pX = (int)((double)(this.mapX + this.mapSize / 2) + dx);
            double dy = (center.z - cz) / bpp;
            int pY = (int)((double)(this.mapY + this.mapSize / 2) + dy);
            if (!this.isPointOnMap(pX, pY)) continue;
            String owner = cp.owner.toUpperCase();
            String capTeam = cp.capturingTeam.toUpperCase();
            float progress = cp.progress;
            String teamToRender = owner;
            float alpha = 1.0f;
            if (owner.equals("NEUTRAL")) {
                if (!capTeam.equals("NONE") && !capTeam.equals("NEUTRAL")) {
                    if (blinkOn) {
                        teamToRender = capTeam;
                        alpha = 0.1f + progress * 0.9f;
                    } else {
                        teamToRender = "NEUTRAL";
                        alpha = 1.0f;
                    }
                } else {
                    teamToRender = "NEUTRAL";
                    alpha = 1.0f;
                }
            } else if (progress < 1.0f) {
                teamToRender = owner;
                alpha = 0.1f + progress * 0.9f;
            } else {
                teamToRender = owner;
                alpha = 1.0f;
            }
            ResourceLocation flagTex = FLAG_NEUTRAL;
            int tintColor = -1;
            boolean useTint = false;
            if (teamToRender.equals("BLUE")) {
                flagTex = this.getFlagTexture(ClientData.BLUE_FACTION);
                if (flagTex == null) {
                    flagTex = FLAG_NEUTRAL;
                    tintColor = -11184641;
                    useTint = true;
                }
            } else if (teamToRender.equals("RED") && (flagTex = this.getFlagTexture(ClientData.RED_FACTION)) == null) {
                flagTex = FLAG_NEUTRAL;
                tintColor = -43691;
                useTint = true;
            }
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            if (useTint) {
                r = (float)(tintColor >> 16 & 0xFF) / 255.0f;
                g = (float)(tintColor >> 8 & 0xFF) / 255.0f;
                b = (float)(tintColor & 0xFF) / 255.0f;
            }
            RenderSystem.setShaderColor((float)r, (float)g, (float)b, (float)alpha);
            this.setFilter(flagTex, true);
            gui.blit(flagTex, pX - 8, pY - 4, 16, 9, 0.0f, 0.0f, 64, 36, 64, 36);
            this.setFilter(flagTex, false);
            int textColor = 0xFFFFFF;
            gui.pose().pushPose();
            gui.pose().translate((float)pX, (float)(pY + 7), 101.0f);
            float textScale = 0.6f;
            gui.pose().scale(textScale, textScale, 1.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.drawCenteredString(mc.font, cp.name, 0, 0, textColor);
            gui.pose().popPose();
        }
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderAllPlayers(GuiGraphics gui, Minecraft mc, LocalPlayer self, double cx, double cz, double bpp) {
        String myName = self.getScoreboardName();
        int myInternalSquadId = -1;
        String myTeamForNums = this.getPlayerTeamStrict(mc);
        boolean isBlueForNums = myTeamForNums != null && myTeamForNums.contains("BLUE");
        int teamCMDIdForNums = isBlueForNums ? ClientData.blueCMDId : ClientData.redCMDId;
        List teamSquadsForNums = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeamForNums)).collect(Collectors.toList());
        teamSquadsForNums.sort((s1, s2) -> {
            if (s1.id == teamCMDIdForNums && teamCMDIdForNums != -1) {
                return -1;
            }
            if (s2.id == teamCMDIdForNums && teamCMDIdForNums != -1) {
                return 1;
            }
            return Integer.compare(s1.id, s2.id);
        });
        HashMap<Integer, Integer> idToDisplayNum = new HashMap<Integer, Integer>();
        for (int i = 0; i < teamSquadsForNums.size(); ++i) {
            AASWorldData.Squad s3 = (AASWorldData.Squad)teamSquadsForNums.get(i);
            idToDisplayNum.put(s3.id, i + 1);
            if (!s3.members.contains(myName)) continue;
            myInternalSquadId = s3.id;
        }
        boolean isShowNicksHeld = this.isShowNicknamesHeld();
        long currentTime = mc.level.getGameTime();
        boolean amIMedic = "Medic".equalsIgnoreCase(ClientData.myCurrentKit);
        HashMap<Integer, List> vehicleGroups = new HashMap<Integer, List>();
        for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
            if (info.inVehicle) {
                vehicleGroups.computeIfAbsent(info.vehicleId, k -> new ArrayList()).add(info);
                continue;
            }
            double dx = (info.x - cx) / bpp;
            int sx = (int)((double)(this.mapX + this.mapSize / 2) + dx);
            double dy = (info.z - cz) / bpp;
            int sy = (int)((double)(this.mapY + this.mapSize / 2) + dy);
            if (!this.isPointOnMap(sx, sy)) continue;
            if (isShowNicksHeld && !info.name.equals(myName) && !info.isDowned) {
                gui.pose().pushPose();
                gui.pose().translate((float)sx, (float)(sy - 8), 450.0f);
                gui.pose().scale(0.6f, 0.6f, 1.0f);
                int nickColor = info.squadId != -1 && info.squadId == myInternalSquadId ? -11141291 : -1;
                gui.drawCenteredString(mc.font, (Component)Component.literal((String)info.name), 0, 0, nickColor);
                gui.pose().popPose();
            }
            if (info.name.equals(myName)) continue;
            if (info.isDowned) {
                if (amIMedic || currentTime - info.lastShoutTime < 60L) {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    gui.blit(ICON_PLUS, sx - 4, sy - 4, 8, 8, 0.0f, 0.0f, 16, 16, 16, 16);
                }
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                float r = 0.2f;
                float g = 0.6f;
                float b = 1.0f;
                if (info.squadId != -1 && info.squadId == myInternalSquadId) {
                    r = 0.0f;
                    g = 1.0f;
                    b = 0.0f;
                }
                RenderSystem.setShaderColor((float)r, (float)g, (float)b, 1.0f);
                gui.blit(ICON_CIRCLE, sx - 3, sy - 3, 6, 6, 0.0f, 0.0f, 16, 16, 16, 16);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            Integer displayNum = (Integer)idToDisplayNum.get(info.squadId);
            if (info.isDowned || !info.isLeader || info.squadId == -1 || displayNum == null) continue;
            String numStr = String.valueOf(displayNum);
            int textColor = info.squadId == myInternalSquadId ? -11141291 : -11184641;
            gui.pose().pushPose();
            gui.pose().translate((float)sx, (float)sy, 350.0f);
            gui.pose().scale(0.5f, 0.5f, 1.0f);
            int tw = mc.font.width(numStr);
            this.drawSquadNumber(gui, mc.font, numStr, -(tw / 2), -4, textColor);
            gui.pose().popPose();
        }
        if (isShowNicksHeld) {
            for (List group : vehicleGroups.values()) {
                if (group.isEmpty()) continue;
                group.sort(Comparator.comparingInt(p -> p.seatIndex));
                MapPlayerInfo driver = (MapPlayerInfo)group.get(0);
                double dx = (driver.x - cx) / bpp;
                double dy = (driver.z - cz) / bpp;
                int sx = (int)((double)(this.mapX + this.mapSize / 2) + dx);
                int sy = (int)((double)(this.mapY + this.mapSize / 2) + dy);
                if (!this.isPointOnMap(sx, sy)) continue;
                int yOffset = sy - 10 - (group.size() - 1) * 8;
                for (MapPlayerInfo pInfo : group) {
                    gui.pose().pushPose();
                    gui.pose().translate((float)sx, (float)yOffset, 450.0f);
                    gui.pose().scale(0.6f, 0.6f, 1.0f);
                    int nickColor = -1;
                    if (pInfo.squadId != -1 && pInfo.squadId == myInternalSquadId) {
                        nickColor = -11141291;
                    }
                    if (pInfo.name.equals(myName)) {
                        nickColor = -171;
                    }
                    gui.drawCenteredString(mc.font, (Component)Component.literal((String)pInfo.name), 0, 0, nickColor);
                    gui.pose().popPose();
                    yOffset += 8;
                }
            }
        }
        this.renderSelf(gui, self, cx, cz, bpp, myInternalSquadId);
    }

    private void renderSelf(GuiGraphics gui, LocalPlayer self, double cx, double cz, double bpp, int mySquadId) {
        double myDy;
        int mySy;
        if (!self.isAlive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean inVehicle = self.getVehicle() != null;
        double myDx = (self.getX() - cx) / bpp;
        int mySx = (int)((double)(this.mapX + this.mapSize / 2) + myDx);
        if (this.isPointOnMap(mySx, mySy = (int)((double)(this.mapY + this.mapSize / 2) + (myDy = (self.getZ() - cz) / bpp))) && !inVehicle) {
            gui.pose().pushPose();
            gui.pose().translate((float)mySx, (float)mySy, 300.0f);
            gui.pose().mulPose(Axis.ZP.rotationDegrees(self.getYRot() + 180.0f));
            if (mySquadId != -1) {
                RenderSystem.setShaderColor(0.0f, 1.0f, 0.0f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.blit(ICON_PLAYER_SELF, -5, -5, 10, 10, 0.0f, 0.0f, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.pose().popPose();
            if (this.isShowNicknamesHeld()) {
                gui.pose().pushPose();
                gui.pose().translate((float)mySx, (float)(mySy - 10), 450.0f);
                gui.pose().scale(0.6f, 0.6f, 1.0f);
                int myColor = mySquadId != -1 ? -11141291 : -171;
                gui.drawCenteredString(mc.font, (Component)Component.literal((String)self.getScoreboardName()), 0, 0, myColor);
                gui.pose().popPose();
            }
        }
    }

    private boolean isPointOnMap(int x, int y) {
        return x >= this.mapX && x <= this.mapX + this.mapSize && y >= this.mapY && y <= this.mapY + this.mapSize;
    }

    private ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equalsIgnoreCase("none")) {
            return null;
        }
        return new ResourceLocation("aas", "textures/gui/flags/" + faction.toLowerCase() + ".png");
    }

    public void centerOnPlayer() {
        this.mapPanX = 0.0;
        this.mapPanZ = 0.0;
    }

    public double getCenterX(LocalPlayer player) {
        return player.getX() + this.mapPanX;
    }

    public double getCenterZ(LocalPlayer player) {
        return player.getZ() + this.mapPanZ;
    }

    public double getBlocksPerPixel() {
        return ClientData.mapScale;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.isMouseOver(mouseX, mouseY)) {
            ClientData.zoomMap(delta);
            return true;
        }
        return false;
    }

    private void renderVehicles(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        if (ClientData.clientVehicles == null || ClientData.clientVehicles.isEmpty()) {
            return;
        }
        PoseStack pose = gui.pose();
        String myTeam = "NEUTRAL";
        if (mc.player.getTeam() != null) {
            String name = mc.player.getTeam().getName().toUpperCase();
            if (name.contains("BLUE")) {
                myTeam = "BLUE";
            } else if (name.contains("RED")) {
                myTeam = "RED";
            }
        }
        boolean isObserver = mc.player.isCreative() || mc.player.isSpectator();
        for (AASWorldData.VehicleRecord record : ClientData.clientVehicles) {
            double dy;
            int screenY;
            double dx;
            int screenX;
            if (!record.team.equalsIgnoreCase(myTeam) && !isObserver || !this.isPointOnMap(screenX = (int)((double)(this.mapX + this.mapSize / 2) + (dx = (record.x - cx) / bpp)), screenY = (int)((double)(this.mapY + this.mapSize / 2) + (dy = (record.z - cz) / bpp)))) continue;
            ResourceLocation icon = VEHICLE_ICONS.getOrDefault(record.type, VEHICLE_ICONS.get("DEFAULT"));
            this.setFilter(icon, true);
            pose.pushPose();
            pose.translate((float)screenX, (float)screenY, 150.0f);
            if (!record.type.equals("Mine")) {
                pose.mulPose(Axis.ZP.rotationDegrees(record.yaw + 180.0f));
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            gui.blit(icon, -6, -6, 12, 12, 0.0f, 0.0f, 16, 16, 16, 16);
            pose.popPose();
            this.setFilter(icon, false);
        }
    }

    private void renderLatticeLines(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        if (ClientData.allCapturePoints == null || ClientData.allCapturePoints.isEmpty()) {
            return;
        }
        if (mc.level == null) {
            return;
        }
        String currentDim = mc.level.dimension().location().toString();
        ArrayList<AASWorldData.CapturePoint> sortedPoints = new ArrayList<AASWorldData.CapturePoint>(ClientData.allCapturePoints);
        sortedPoints.sort(Comparator.comparingInt(p -> p.bluePriority));
        ArrayList<Vec3> path = new ArrayList<Vec3>();
        if (ClientData.blueSpawns.containsKey(currentDim)) {
            BlockPos bPos = ClientData.blueSpawns.get(currentDim);
            path.add(new Vec3((double)bPos.getX() + 0.5, (double)bPos.getY(), (double)bPos.getZ() + 0.5));
        }
        for (AASWorldData.CapturePoint cp : sortedPoints) {
            path.add(cp.area.getCenter());
        }
        if (ClientData.redSpawns.containsKey(currentDim)) {
            BlockPos rPos = ClientData.redSpawns.get(currentDim);
            path.add(new Vec3((double)rPos.getX() + 0.5, (double)rPos.getY(), (double)rPos.getZ() + 0.5));
        }
        int lineColor = 0x66FFFFFF;
        for (int i = 0; i < path.size() - 1; ++i) {
            Vec3 p1 = (Vec3)path.get(i);
            Vec3 p2 = (Vec3)path.get(i + 1);
            int x1 = (int)((double)(this.mapX + this.mapSize / 2) + (p1.x - cx) / bpp);
            int y1 = (int)((double)(this.mapY + this.mapSize / 2) + (p1.z - cz) / bpp);
            int x2 = (int)((double)(this.mapX + this.mapSize / 2) + (p2.x - cx) / bpp);
            int y2 = (int)((double)(this.mapY + this.mapSize / 2) + (p2.z - cz) / bpp);
            this.drawSolidLine(gui, x1, y1, x2, y2, lineColor);
        }
    }

    private void renderMainBases(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        boolean isFriendly;
        BlockPos pos;
        if (mc.level == null) {
            return;
        }
        String currentDim = mc.level.dimension().location().toString();
        String playerTeam = this.getPlayerTeamStrict(mc);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (ClientData.blueSpawns.containsKey(currentDim)) {
            pos = ClientData.blueSpawns.get(currentDim);
            isFriendly = playerTeam.equals("BLUE");
            this.drawMainBaseIcon(gui, mc, pos, cx, cz, bpp, ClientData.BLUE_FACTION, -13408564, isFriendly);
        }
        if (ClientData.redSpawns.containsKey(currentDim)) {
            pos = ClientData.redSpawns.get(currentDim);
            isFriendly = playerTeam.equals("RED");
            this.drawMainBaseIcon(gui, mc, pos, cx, cz, bpp, ClientData.RED_FACTION, -3394765, isFriendly);
        }
    }

    private void drawMainBaseIcon(GuiGraphics gui, Minecraft mc, BlockPos pos, double cx, double cz, double bpp, String faction, int fallbackColor, boolean isFriendly) {
        int pY;
        int pX = (int)((double)(this.mapX + this.mapSize / 2) + ((double)pos.getX() + 0.5 - cx) / bpp);
        if (!this.isPointOnMap(pX, pY = (int)((double)(this.mapY + this.mapSize / 2) + ((double)pos.getZ() + 0.5 - cz) / bpp))) {
            return;
        }
        ResourceLocation icon = isFriendly && "MAIN".equals(this.selectedSpawnId) ? MAIN_SELECTED_ICON : MAIN_BASE_ICON;
        ResourceLocation flagTex = this.getFlagTexture(faction);
        if (flagTex != null && !faction.equals("none")) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            this.setFilter(flagTex, true);
            gui.blit(flagTex, pX - 8, pY - 4, 16, 9, 0.0f, 0.0f, 64, 36, 64, 36);
            this.setFilter(flagTex, false);
        } else {
            gui.fill(pX - 8, pY - 4, pX + 8, pY + 5, fallbackColor);
        }
        this.setFilter(icon, true);
        gui.pose().pushPose();
        gui.pose().translate((float)pX, (float)pY, 150.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        gui.blit(icon, -6, -6, 12, 12, 0.0f, 0.0f, 16, 16, 16, 16);
        gui.pose().popPose();
        this.setFilter(icon, false);
    }

    private void drawSolidLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0f) {
            return;
        }
        float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();
        gui.pose().translate((float)x1, (float)y1, 0.0f);
        gui.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        gui.fill(0, 0, (int)len, 1, color);
        gui.pose().popPose();
    }

    private void drawSmoothCircle(GuiGraphics gui, float cx, float cy, float radius, int color) {
        if (radius <= 0.0f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::m_172811_);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        Matrix4f matrix = gui.pose().last().pose();
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int segments = 128;
        for (int i = 0; i <= segments; ++i) {
            float angle = (float)i * ((float)Math.PI * 2) / (float)segments;
            float x = cx + Mth.cos((float)angle) * radius;
            float y = cy + Mth.sin((float)angle) * radius;
            bufferbuilder.vertex(matrix, x, y, 0.0f).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
    }

    private void renderArtilleryZones(GuiGraphics gui, double cx, double cz, double bpp) {
        if (ClientData.activeStrikes == null || ClientData.activeStrikes.isEmpty()) {
            return;
        }
        float radius = ((Integer)AASConfig.ART_STRIKE_RADIUS.get()).floatValue();
        for (AASWorldData.ActiveStrike strike : ClientData.activeStrikes) {
            double dx = ((double)strike.pos.getX() + 0.5 - cx) / bpp;
            double dy = ((double)strike.pos.getZ() + 0.5 - cz) / bpp;
            float sx = (float)((double)this.mapX + (double)this.mapSize / 2.0 + dx);
            float sy = (float)((double)this.mapY + (double)this.mapSize / 2.0 + dy);
            this.drawSmoothCircle(gui, sx, sy, radius / (float)bpp, -65536);
        }
    }

    private void renderStructures(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        float sy;
        float sx;
        double dy;
        double dx;
        PoseStack pose = gui.pose();
        String myTeam = this.getPlayerTeamStrict(mc);
        String myName = mc.player.getScoreboardName();
        boolean isObserver = mc.player.isCreative() || mc.player.isSpectator();
        double minHubDist = ((Integer)AASConfig.MIN_HUB_DISTANCE.get()).intValue();
        double buildRad = ((Integer)AASConfig.HUB_BUILD_RADIUS.get()).intValue();
        for (AASWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.constructed || !hub.team.equalsIgnoreCase(myTeam) && !isObserver) continue;
            dx = ((double)hub.pos.getX() + 0.5 - cx) / bpp;
            dy = ((double)hub.pos.getZ() + 0.5 - cz) / bpp;
            sx = (float)((double)this.mapX + (double)this.mapSize / 2.0 + dx);
            sy = (float)((double)this.mapY + (double)this.mapSize / 2.0 + dy);
            pose.pushPose();
            pose.translate(0.0f, 0.0f, 50.0f);
            this.drawSmoothCircle(gui, sx, sy, (float)(minHubDist / bpp), 0x60FFFFFF);
            int teamCircleColor = hub.team.equalsIgnoreCase("BLUE") ? -2141891073 : -2130750123;
            this.drawSmoothCircle(gui, sx, sy, (float)(buildRad / bpp), teamCircleColor);
            pose.popPose();
            if (!this.isPointOnMap((int)sx, (int)sy)) continue;
            String hubId = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
            ResourceLocation icon = hubId.equals(this.selectedSpawnId) ? HUB_SELECTED_ICON : HUB_ICON;
            this.setFilter(icon, true);
            pose.pushPose();
            pose.translate(sx, sy, 160.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.blit(icon, -6, -6, 12, 12, 0.0f, 0.0f, 16, 16, 16, 16);
            pose.popPose();
            this.setFilter(icon, false);
            String matValue = String.valueOf(hub.materials);
            int textWidth = mc.font.width(matValue);
            int iconSize = 8;
            int gap = 2;
            float totalWidth = iconSize + gap + textWidth;
            gui.pose().pushPose();
            gui.pose().translate(sx, sy - 15.0f, 500.0f);
            gui.pose().scale(0.8f, 0.8f, 1.0f);
            float startX = -totalWidth / 2.0f;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            this.setFilter(MATS_ICON, true);
            gui.blit(MATS_ICON, (int)startX, -4, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
            this.setFilter(MATS_ICON, false);
            gui.drawString(mc.font, matValue, (int)startX + iconSize + gap, -4, -22016, true);
            gui.pose().popPose();
        }
        for (AASWorldData.Squad squad : ClientData.clientSquads) {
            if (squad.rallyPos == null || !squad.team.equalsIgnoreCase(myTeam) && !isObserver || !this.isPointOnMap((int)(sx = (float)((double)this.mapX + (double)this.mapSize / 2.0 + (dx = ((double)squad.rallyPos.getX() + 0.5 - cx) / bpp))), (int)(sy = (float)((double)this.mapY + (double)this.mapSize / 2.0 + (dy = ((double)squad.rallyPos.getZ() + 0.5 - cz) / bpp))))) continue;
            boolean isMySquad = squad.members.contains(myName);
            ResourceLocation icon = isMySquad && "RALLY".equals(this.selectedSpawnId) ? RALLY_SELECTED_ICON : RALLY_ICON;
            this.setFilter(icon, true);
            pose.pushPose();
            pose.translate(sx, sy, 170.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.blit(icon, -5, -5, 10, 10, 0.0f, 0.0f, 16, 16, 16, 16);
            pose.popPose();
            this.setFilter(icon, false);
        }
    }

    private void renderSquadMarkerLogic(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        boolean withDash;
        boolean canSee;
        boolean isCharlie;
        if (mc.player == null) {
            return;
        }
        String myName = mc.player.getScoreboardName();
        AASWorldData.Squad mySquad = null;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            mySquad = s;
            break;
        }
        if (mySquad == null) {
            return;
        }
        boolean isSL = mySquad.leader.equals(myName);
        boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
        boolean bl = isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
        if (mySquad.marker != null && mySquad.marker.type != 6) {
            this.drawMapMarkerAndLine(gui, mc, cx, cz, bpp, mySquad.marker, this.getSquadMarkerIcon(mySquad.marker.type), this.getSquadMarkerColor(mySquad.marker.type), true);
        }
        if (mySquad.bravoMarker != null && mySquad.bravoMarker.type != 6) {
            boolean bl2 = canSee = isSL || isBravo;
            if (canSee) {
                withDash = isSL || isBravo;
                this.drawMapMarkerAndLine(gui, mc, cx, cz, bpp, mySquad.bravoMarker, this.getBravoMarkerIcon(mySquad.bravoMarker.type), -65281, withDash);
            }
        }
        if (mySquad.charlieMarker != null && mySquad.charlieMarker.type != 6) {
            boolean bl3 = canSee = isSL || isCharlie;
            if (canSee) {
                withDash = isSL || isCharlie;
                this.drawMapMarkerAndLine(gui, mc, cx, cz, bpp, mySquad.charlieMarker, this.getCharlieMarkerIcon(mySquad.charlieMarker.type), -16711766, withDash);
            }
        }
        if (mySquad.bravoMarker != null && mySquad.bravoMarker.type != 6 && !isSL && !isBravo) {
            this.drawMapMarkerAndLine(gui, mc, cx, cz, bpp, mySquad.bravoMarker, this.getBravoMarkerIcon(mySquad.bravoMarker.type), -65281, false);
        }
        if (mySquad.charlieMarker != null && mySquad.charlieMarker.type != 6 && !isSL && !isCharlie) {
            this.drawMapMarkerAndLine(gui, mc, cx, cz, bpp, mySquad.charlieMarker, this.getCharlieMarkerIcon(mySquad.charlieMarker.type), -16711766, false);
        }
    }

    private void drawMapMarkerAndLine(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp, AASWorldData.SquadMarker m, ResourceLocation icon, int color, boolean withDash) {
        int mx = (int)((double)(this.mapX + this.mapSize / 2) + ((double)m.x - cx) / bpp);
        int my = (int)((double)(this.mapY + this.mapSize / 2) + ((double)m.z - cz) / bpp);
        int px = (int)((double)(this.mapX + this.mapSize / 2) + (mc.player.getX() - cx) / bpp);
        int py = (int)((double)(this.mapY + this.mapSize / 2) + (mc.player.getZ() - cz) / bpp);
        if (withDash) {
            this.drawDashedLine(gui, px, py, mx, my, color);
        }
        if (this.isPointOnMap(mx, my)) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.blit(icon, mx - 6, my - 6, 0.0f, 0.0f, 12, 12, 12, 12);
            double distance = Math.sqrt(mc.player.distanceToSqr((double)m.x, mc.player.getY(), (double)m.z));
            String distText = (int)distance + "m";
            gui.pose().pushPose();
            gui.pose().translate((float)mx, (float)(my + 8), 600.0f);
            gui.pose().scale(0.8f, 0.8f, 1.0f);
            int textWidth = mc.font.width(distText);
            gui.drawString(mc.font, distText, -(textWidth / 2), 0, color, true);
            gui.pose().popPose();
        }
    }

    private int getSquadMarkerColor(int type) {
        switch (type) {
            case 1: {
                return -22016;
            }
            case 2: {
                return -11184641;
            }
            case 3: {
                return -43521;
            }
            case 4: {
                return -1;
            }
            case 5: {
                return -65536;
            }
        }
        return -11141291;
    }

    private ResourceLocation getSquadMarkerIcon(int type) {
        switch (type) {
            case 1: {
                return MARKER_ATTACK;
            }
            case 2: {
                return MARKER_DEFEND;
            }
            case 3: {
                return MARKER_BUILD;
            }
            case 5: {
                return MARKER_ATTACK;
            }
        }
        return MARKER_MOVE;
    }

    private ResourceLocation getBravoMarkerIcon(int type) {
        switch (type) {
            case 1: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_attack_bravo.png");
            }
            case 2: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_defend_bravo.png");
            }
            case 3: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_build_bravo.png");
            }
        }
        return new ResourceLocation("aas", "textures/gui/map_icons/marker_move_bravo.png");
    }

    private ResourceLocation getCharlieMarkerIcon(int type) {
        switch (type) {
            case 1: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_attack_charlie.png");
            }
            case 2: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_defend_charlie.png");
            }
            case 3: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_build_charlie.png");
            }
        }
        return new ResourceLocation("aas", "textures/gui/map_icons/marker_move_charlie.png");
    }

    private void drawDashedLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 5.0) {
            return;
        }
        int i = 0;
        while ((double)i < len) {
            double t = (double)i / len;
            int lx = (int)((double)x1 + (double)dx * t);
            int ly = (int)((double)y1 + (double)dy * t);
            if (this.isPointOnMap(lx, ly)) {
                gui.fill(lx, ly, lx + 2, ly + 2, color);
            }
            i += 4;
        }
    }

    private String getPlayerTeamStrict(Minecraft mc) {
        if (mc.player != null && mc.player.getTeam() != null) {
            String name = mc.player.getTeam().getName().toUpperCase();
            if (name.contains("BLUE")) {
                return "BLUE";
            }
            if (name.contains("RED")) {
                return "RED";
            }
            return name;
        }
        return "NEUTRAL";
    }

    private void renderSquadPings(GuiGraphics gui, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        String myName = mc.player.getScoreboardName();
        AASWorldData.Squad mySquad = null;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            mySquad = s;
            break;
        }
        if (mySquad != null) {
            boolean isCharlie;
            long time = mc.level.getGameTime();
            boolean isSL = mySquad.leader.equals(myName);
            boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
            boolean bl = isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
            if (mySquad.pingPos != null && time < mySquad.pingExpiry) {
                this.drawPingOnMap(gui, mySquad.pingPos, new ResourceLocation("aas", "textures/gui/map_icons/ping_eye.png"), cx, cz, bpp);
            }
            if (mySquad.bravoPingPos != null && time < mySquad.bravoPingExpiry && (isSL || isBravo)) {
                this.drawPingOnMap(gui, mySquad.bravoPingPos, new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_bravo.png"), cx, cz, bpp);
            }
            if (mySquad.charliePingPos != null && time < mySquad.charliePingExpiry && (isSL || isCharlie)) {
                this.drawPingOnMap(gui, mySquad.charliePingPos, new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_charlie.png"), cx, cz, bpp);
            }
        }
    }

    private void drawPingOnMap(GuiGraphics gui, BlockPos pos, ResourceLocation icon, double cx, double cz, double bpp) {
        double dz;
        int py;
        double dx = ((double)pos.getX() + 0.5 - cx) / bpp;
        int px = (int)((double)(this.mapX + this.mapSize / 2) + dx);
        if (this.isPointOnMap(px, py = (int)((double)(this.mapY + this.mapSize / 2) + (dz = ((double)pos.getZ() + 0.5 - cz) / bpp)))) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.blit(icon, px - 6, py - 6, 0.0f, 0.0f, 12, 12, 12, 12);
        }
    }

    private void renderTacticalMarkers(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        String myTeam = this.getPlayerTeamStrict(mc);
        long currentTime = mc.level.getGameTime();
        float totalLifetime = 3600.0f;
        for (AASWorldData.MapMarker m : ClientData.activeMarkers) {
            double dy;
            int sy;
            long timeLeft;
            if (!m.team.equalsIgnoreCase(myTeam) || (timeLeft = m.expiryTick - currentTime) <= 0L) continue;
            float alpha = Mth.clamp((float)((float)timeLeft / totalLifetime), 0.0f, 1.0f);
            double dx = ((double)m.pos.getX() - cx) / bpp;
            int sx = (int)((double)(this.mapX + this.mapSize / 2) + dx);
            if (!this.isPointOnMap(sx, sy = (int)((double)(this.mapY + this.mapSize / 2) + (dy = ((double)m.pos.getZ() - cz) / bpp)))) continue;
            ResourceLocation icon = this.getMarkerIcon(m.type);
            this.setFilter(icon, true);
            gui.pose().pushPose();
            gui.pose().translate((float)sx, (float)sy, 120.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float)alpha);
            gui.blit(icon, -8, -8, 16, 16, 0.0f, 0.0f, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.pose().popPose();
            this.setFilter(icon, false);
        }
    }

    private void renderSquadRhombusMarkers(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
        long time = mc.level.getGameTime();
        String myName = mc.player.getScoreboardName();
        String myTeam = this.getPlayerTeamStrict(mc);
        boolean amISquadLeader = this.isSquadLeaderOrFTL(mc.player);
        boolean isBlueForNums = myTeam != null && myTeam.contains("BLUE");
        int teamCMDIdForNums = isBlueForNums ? ClientData.blueCMDId : ClientData.redCMDId;
        List teamSquadsForNums = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeam)).collect(Collectors.toList());
        teamSquadsForNums.sort((s1, s2) -> {
            if (s1.id == teamCMDIdForNums && teamCMDIdForNums != -1) {
                return -1;
            }
            if (s2.id == teamCMDIdForNums && teamCMDIdForNums != -1) {
                return 1;
            }
            return Integer.compare(s1.id, s2.id);
        });
        for (AASWorldData.Squad squad : ClientData.clientSquads) {
            boolean canSee;
            if (!squad.team.equalsIgnoreCase(myTeam)) continue;
            boolean bl = canSee = squad.members.contains(myName) || amISquadLeader;
            if (!canSee) continue;
            for (AASWorldData.SquadMarker rm : squad.rhombusMarkers) {
                long timeLeft = rm.expiryTick - time;
                if (timeLeft <= 0L) continue;
                float alpha = Mth.clamp((float)((float)timeLeft / 3600.0f), 0.1f, 1.0f);
                int mx = (int)((double)(this.mapX + this.mapSize / 2) + ((double)rm.x - cx) / bpp);
                int my = (int)((double)(this.mapY + this.mapSize / 2) + ((double)rm.z - cz) / bpp);
                if (!this.isPointOnMap(mx, my)) continue;
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float)alpha);
                gui.blit(new ResourceLocation("aas", "textures/gui/map_icons/squad_rhombus.png"), mx - 8, my - 8, 0.0f, 0.0f, 16, 16, 16, 16);
                gui.pose().pushPose();
                gui.pose().translate((float)mx, (float)my, 500.0f);
                gui.pose().scale(0.5f, 0.5f, 1.0f);
                int displayNum = 0;
                for (int i = 0; i < teamSquadsForNums.size(); ++i) {
                    if (((AASWorldData.Squad)teamSquadsForNums.get((int)i)).id != squad.id) continue;
                    displayNum = i + 1;
                    break;
                }
                String numStr = String.valueOf(displayNum);
                int whiteWithAlpha = (int)(alpha * 255.0f) << 24 | 0xFFFFFF;
                this.drawSquadNumber(gui, mc.font, numStr, -(mc.font.width(numStr) / 2), -4, whiteWithAlpha);
                gui.pose().popPose();
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            if (button == 0) {
                this.isDraggingMap = true;
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                return true;
            }
            if (button == 1) {
                return true;
            }
        }
        return false;
    }

    public void mouseReleased(int button) {
        if (button == 0) {
            this.isDraggingMap = false;
        }
    }

    private void handleMapRightClick(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!this.isSquadLeaderOrFTL(mc.player)) {
            mc.player.displayClientMessage((Component)Component.literal((String)"Only SL and FTLs can place markers!").withStyle(ChatFormatting.RED), true);
            return;
        }
        double bpp = this.getBlocksPerPixel();
        double centerX = this.getCenterX(mc.player);
        double centerZ = this.getCenterZ(mc.player);
        int targetX = (int)(centerX + (mouseX - ((double)this.mapX + (double)this.mapSize / 2.0)) * bpp);
        int targetZ = (int)(centerZ + (mouseY - ((double)this.mapY + (double)this.mapSize / 2.0)) * bpp);
        mc.setScreen((Screen)new TacticalMapRadialScreen(targetX, targetZ));
    }

    private boolean isSquadLeaderOrFTL(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        String pName = player.getScoreboardName();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(pName) && !s.bravoLeader.equals(pName) && !s.charlieLeader.equals(pName)) continue;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingMap && button == 0) {
            this.mapPanX -= (mouseX - this.lastMouseX) * ClientData.mapScale;
            this.mapPanZ -= (mouseY - this.lastMouseY) * ClientData.mapScale;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= (double)this.mapX && mouseX <= (double)(this.mapX + this.mapSize) && mouseY >= (double)this.mapY && mouseY <= (double)(this.mapY + this.mapSize);
    }

    private boolean isShowNicknamesHeld() {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        InputConstants.Key nickKey = ModKeyBindings.SHOW_NICKNAMES_KEY.getKey();
        if (nickKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton((long)window, (int)nickKey.getValue()) == 1;
        }
        return InputConstants.isKeyDown((long)window, (int)nickKey.getValue());
    }

    @Override
    public void close() {
    }

    static {
        VEHICLE_ICONS.put("APC", new ResourceLocation("aas", "textures/gui/map_icons/apc.png"));
        VEHICLE_ICONS.put("TANK", new ResourceLocation("aas", "textures/gui/map_icons/tank.png"));
        VEHICLE_ICONS.put("HELICOPTER", new ResourceLocation("aas", "textures/gui/map_icons/helicopter.png"));
        VEHICLE_ICONS.put("CAS Helicopter", new ResourceLocation("aas", "textures/gui/map_icons/cas_helicopter.png"));
        VEHICLE_ICONS.put("CAS Fighter", new ResourceLocation("aas", "textures/gui/map_icons/cas_fighter.png"));
        VEHICLE_ICONS.put("Combat Vehicle", new ResourceLocation("aas", "textures/gui/map_icons/combat_vehicle.png"));
        VEHICLE_ICONS.put("Infantry Vehicle", new ResourceLocation("aas", "textures/gui/map_icons/infantry_vehicle.png"));
        VEHICLE_ICONS.put("Supply Truck", new ResourceLocation("aas", "textures/gui/map_icons/supply_truck.png"));
        VEHICLE_ICONS.put("Supply Helicopter", new ResourceLocation("aas", "textures/gui/map_icons/supply_helicopter.png"));
        VEHICLE_ICONS.put("DEFAULT", new ResourceLocation("aas", "textures/gui/map_icons/default.png"));
        VEHICLE_ICONS.put("Static ZU", new ResourceLocation("aas", "textures/gui/map_icons/static_zu.png"));
        VEHICLE_ICONS.put("Mobile ZU", new ResourceLocation("aas", "textures/gui/map_icons/mobile_zu.png"));
        VEHICLE_ICONS.put("BOAT", new ResourceLocation("aas", "textures/gui/map_icons/boat.png"));
        VEHICLE_ICONS.put("Motorcycle", new ResourceLocation("aas", "textures/gui/map_icons/motorcycle.png"));
        VEHICLE_ICONS.put("Light Supply", new ResourceLocation("aas", "textures/gui/map_icons/light_supply.png"));
        VEHICLE_ICONS.put("Mine", new ResourceLocation("aas", "textures/gui/map_icons/skull_marker.png"));
    }
}


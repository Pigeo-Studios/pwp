package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

// Рендер тактической карты на экране отряда
// Отображает текстуру карты, игроков, точки захвата, маркеры и технику
public class WarfareMapRenderer implements AutoCloseable {
   private static final Map<String, ResourceLocation> MAP_ICONS_CACHE = new HashMap<>();
   private static final ResourceLocation MATS_ICON = new ResourceLocation("pwpwarfare", "textures/gui/mats_icon.png");
   public String selectedSpawnId = "";
   private int mapX;
   private int mapY;
   private int mapSize;
   private double mapPanX = 0.0;
   private double mapPanZ = 0.0;
   private boolean isDraggingMap = false;
   private double lastMouseX = 0.0;
   private double lastMouseY = 0.0;
   private static final ResourceLocation MARKER_MOVE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move.png");
   private static final ResourceLocation MARKER_ATTACK = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_attack.png");
   private static final ResourceLocation MARKER_DEFEND = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_defend.png");
   private static final ResourceLocation MARKER_BUILD = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_build.png");
   private static final ResourceLocation FLAG_NEUTRAL = new ResourceLocation("pwpwarfare", "textures/gui/flags/neutral.png");
   private static final ResourceLocation ICON_CIRCLE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/player_circle.png");
   private static final ResourceLocation ICON_PLUS = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/medic_plus.png");
   private static final ResourceLocation ICON_PLAYER_SELF = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/player_self.png");
   private static final ResourceLocation MAP_GRID_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/map_grid.png");
   private static final ResourceLocation HUB_ICON = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_icon.png");
   private static final ResourceLocation HUB_ICON_SELECTED = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_icon_selected.png");
   private static final ResourceLocation RALLY_ICON = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_icon.png");
   private static final ResourceLocation RALLY_ICON_SELECTED = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_icon_selected.png");
   private static final ResourceLocation MAIN_BASE_ICON = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/main_base.png");
   private static final ResourceLocation MAIN_BASE_ICON_SELECTED = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/main_base_selected.png");
   private static final ResourceLocation ICON_OBJ_ATTACK = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/objective_attack.png");
   private static final ResourceLocation ICON_OBJ_DEFEND = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/objective_defend.png");
   private static final Map<String, ResourceLocation> VEHICLE_ICONS = new HashMap<>();

   private ResourceLocation getCurrentMapTexture() {
      String img = ClientData.currentMapImage;
      if (img == null || img.isEmpty()) {
         img = "map1";
      }

      return MAP_ICONS_CACHE.computeIfAbsent(img, k -> new ResourceLocation("pwpwarfare", "textures/gui/maps/" + k + ".png"));
   }

   private double getMapScale() {
      return ClientData.mapScale;
   }

   private ResourceLocation getMarkerIcon(String type) {
      String path = type.toLowerCase().replace("enemy ", "").replace(" ", "_");
      return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + path + "_marker.png");
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
      if (localPlayer != null) {
         double currentScale = ClientData.mapScale;
         double cx = localPlayer.getX() + this.mapPanX;
         double cz = localPlayer.getZ() + this.mapPanZ;
         gui.fillGradient(this.mapX, this.mapY, this.mapX + this.mapSize, this.mapY + this.mapSize, -15064016, -16448251);
         gui.enableScissor(this.mapX, this.mapY, this.mapX + this.mapSize, this.mapY + this.mapSize);
         PoseStack pose = gui.pose();
         pose.pushPose();
         pose.translate(this.mapX + this.mapSize / 2.0, this.mapY + this.mapSize / 2.0, 0.0);
         float scale = 1.0F / (float)currentScale;
         pose.scale(scale, scale, 1.0F);
         int s = ClientData.mapSizeBlocks;
         float drawX = (float)(ClientData.mapCenterX - s / 2.0 - cx);
         float drawY = (float)(ClientData.mapCenterZ - s / 2.0 - cz);
         ResourceLocation dynamicMapTexture = this.getCurrentMapTexture();
         this.setFilter(dynamicMapTexture, true);
         RenderSystem.setShaderTexture(0, dynamicMapTexture);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.blit(dynamicMapTexture, (int)drawX, (int)drawY, s, s, 0.0F, 0.0F, 1024, 1024, 1024, 1024);
         this.setFilter(dynamicMapTexture, false);
         this.setFilter(MAP_GRID_TEXTURE, true);
         RenderSystem.setShaderTexture(0, MAP_GRID_TEXTURE);
         RenderSystem.enableBlend();
         gui.blit(MAP_GRID_TEXTURE, (int)drawX, (int)drawY, s, s, 0.0F, 0.0F, 1024, 1024, 1024, 1024);
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
   }

   private void renderOverlays(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
      if (ClientData.allCapturePoints != null) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         boolean blinkOn = System.currentTimeMillis() / 400L % 2L == 0L;

         for (WarfareWorldData.CapturePoint cp : ClientData.allCapturePoints) {
            Vec3 center = cp.area.getCenter();
            double dx = (center.x - cx) / bpp;
            double dy = (center.z - cz) / bpp;
            int pX = (int)(this.mapX + this.mapSize / 2 + dx);
            int pY = (int)(this.mapY + this.mapSize / 2 + dy);
            if (this.isPointOnMap(pX, pY)) {
               String owner = cp.owner.toUpperCase();
               String capTeam = cp.capturingTeam.toUpperCase();
               float progress = cp.progress;
               float alpha = 1.0F;
               String teamToRender;
               if (owner.equals("NEUTRAL")) {
                  if (capTeam.equals("NONE") || capTeam.equals("NEUTRAL")) {
                     teamToRender = "NEUTRAL";
                     alpha = 1.0F;
                  } else if (blinkOn) {
                     teamToRender = capTeam;
                     alpha = 0.1F + progress * 0.9F;
                  } else {
                     teamToRender = "NEUTRAL";
                     alpha = 1.0F;
                  }
               } else if (progress < 1.0F) {
                  teamToRender = owner;
                  alpha = 0.1F + progress * 0.9F;
               } else {
                  teamToRender = owner;
                  alpha = 1.0F;
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
               } else if (teamToRender.equals("RED")) {
                  flagTex = this.getFlagTexture(ClientData.RED_FACTION);
                  if (flagTex == null) {
                     flagTex = FLAG_NEUTRAL;
                     tintColor = -43691;
                     useTint = true;
                  }
               }

               float r = 1.0F;
               float g = 1.0F;
               float b = 1.0F;
               if (useTint) {
                  r = (tintColor >> 16 & 0xFF) / 255.0F;
                  g = (tintColor >> 8 & 0xFF) / 255.0F;
                  b = (tintColor & 0xFF) / 255.0F;
               }

               RenderSystem.setShaderColor(r, g, b, alpha);
               this.setFilter(flagTex, true);
               gui.blit(flagTex, pX - 8, pY - 4, 16, 9, 0.0F, 0.0F, 64, 36, 64, 36);
               this.setFilter(flagTex, false);
               int textColor = 16777215;
               gui.pose().pushPose();
               gui.pose().translate(pX, pY + 7, 101.0F);
               float textScale = 0.6F;
               gui.pose().scale(textScale, textScale, 1.0F);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               gui.drawCenteredString(mc.font, cp.name, 0, 0, textColor);
               gui.pose().popPose();
            }
         }

         RenderSystem.enableDepthTest();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void renderAllPlayers(GuiGraphics gui, Minecraft mc, LocalPlayer self, double cx, double cz, double bpp) {
      String myName = self.getScoreboardName();
      int myInternalSquadId = -1;
      String myTeamForNums = this.getPlayerTeamStrict(mc);
      boolean isBlueForNums = myTeamForNums != null && myTeamForNums.contains("BLUE");
      int teamCMDIdForNums = isBlueForNums ? ClientData.blueCMDId : ClientData.redCMDId;
      List<WarfareWorldData.Squad> teamSquadsForNums = ClientData.clientSquads
         .stream()
         .filter(s -> s.team.equalsIgnoreCase(myTeamForNums))
         .collect(Collectors.toList());
      teamSquadsForNums.sort((s1, s2) -> {
         if (s1.id == teamCMDIdForNums && teamCMDIdForNums != -1) {
            return -1;
         } else {
            return s2.id == teamCMDIdForNums && teamCMDIdForNums != -1 ? 1 : Integer.compare(s1.id, s2.id);
         }
      });
      Map<Integer, Integer> idToDisplayNum = new HashMap<>();

      for (int i = 0; i < teamSquadsForNums.size(); i++) {
         WarfareWorldData.Squad s = teamSquadsForNums.get(i);
         idToDisplayNum.put(s.id, i + 1);
         if (s.members.contains(myName)) {
            myInternalSquadId = s.id;
         }
      }

      boolean isShowNicksHeld = this.isShowNicknamesHeld();
      long currentTime = mc.level.getGameTime();
      boolean amIMedic = "Medic".equalsIgnoreCase(ClientData.myCurrentKit);
      Map<Integer, List<MapPlayerInfo>> vehicleGroups = new HashMap<>();

      for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
         if (info.inVehicle) {
            vehicleGroups.computeIfAbsent(info.vehicleId, k -> new ArrayList<>()).add(info);
         } else {
            double dx = (info.x - cx) / bpp;
            double dy = (info.z - cz) / bpp;
            int sx = (int)(this.mapX + this.mapSize / 2 + dx);
            int sy = (int)(this.mapY + this.mapSize / 2 + dy);
            if (this.isPointOnMap(sx, sy)) {
               if (isShowNicksHeld && !info.name.equals(myName) && !info.isDowned) {
                  gui.pose().pushPose();
                  gui.pose().translate(sx, sy - 8, 450.0F);
                  gui.pose().scale(0.6F, 0.6F, 1.0F);
                  int nickColor = info.squadId != -1 && info.squadId == myInternalSquadId ? -11141291 : -1;
                  gui.drawCenteredString(mc.font, Component.literal(info.name), 0, 0, nickColor);
                  gui.pose().popPose();
               }

               if (!info.name.equals(myName)) {
                  if (info.isDowned) {
                     if (amIMedic || currentTime - info.lastShoutTime < 60L) {
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        gui.blit(ICON_PLUS, sx - 4, sy - 4, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
                     }
                  } else {
                     RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                     float r = 0.2F;
                     float g = 0.6F;
                     float b = 1.0F;
                     if (info.squadId != -1 && info.squadId == myInternalSquadId) {
                        r = 0.0F;
                        g = 1.0F;
                        b = 0.0F;
                     }

                     RenderSystem.setShaderColor(r, g, b, 1.0F);
                     gui.blit(ICON_CIRCLE, sx - 3, sy - 3, 6, 6, 0.0F, 0.0F, 16, 16, 16, 16);
                     RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  }

                  Integer displayNum = idToDisplayNum.get(info.squadId);
                  if (!info.isDowned && info.isLeader && info.squadId != -1 && displayNum != null) {
                     String numStr = String.valueOf(displayNum);
                     int textColor = info.squadId == myInternalSquadId ? -11141291 : -11184641;
                     gui.pose().pushPose();
                     gui.pose().translate(sx, sy, 350.0F);
                     gui.pose().scale(0.5F, 0.5F, 1.0F);
                     int tw = mc.font.width(numStr);
                     this.drawSquadNumber(gui, mc.font, numStr, -(tw / 2), -4, textColor);
                     gui.pose().popPose();
                  }
               }
            }
         }
      }

      if (isShowNicksHeld) {
         for (List<MapPlayerInfo> group : vehicleGroups.values()) {
            if (!group.isEmpty()) {
               group.sort(Comparator.comparingInt(p -> p.seatIndex));
               MapPlayerInfo driver = group.get(0);
               double dx = (driver.x - cx) / bpp;
               double dy = (driver.z - cz) / bpp;
               int sx = (int)(this.mapX + this.mapSize / 2 + dx);
               int sy = (int)(this.mapY + this.mapSize / 2 + dy);
               if (this.isPointOnMap(sx, sy)) {
                  int yOffset = sy - 10 - (group.size() - 1) * 8;

                  for (MapPlayerInfo pInfo : group) {
                     gui.pose().pushPose();
                     gui.pose().translate(sx, yOffset, 450.0F);
                     gui.pose().scale(0.6F, 0.6F, 1.0F);
                     int nickColor = -1;
                     if (pInfo.squadId != -1 && pInfo.squadId == myInternalSquadId) {
                        nickColor = -11141291;
                     }

                     if (pInfo.name.equals(myName)) {
                        nickColor = -171;
                     }

                     gui.drawCenteredString(mc.font, Component.literal(pInfo.name), 0, 0, nickColor);
                     gui.pose().popPose();
                     yOffset += 8;
                  }
               }
            }
         }
      }

      this.renderSelf(gui, self, cx, cz, bpp, myInternalSquadId);
   }

   private void renderSelf(GuiGraphics gui, LocalPlayer self, double cx, double cz, double bpp, int mySquadId) {
      Minecraft mc = Minecraft.getInstance();
      boolean inVehicle = self.getVehicle() != null;
      double myDx = (self.getX() - cx) / bpp;
      double myDy = (self.getZ() - cz) / bpp;
      int mySx = (int)(this.mapX + this.mapSize / 2 + myDx);
      int mySy = (int)(this.mapY + this.mapSize / 2 + myDy);
      if (this.isPointOnMap(mySx, mySy) && !inVehicle) {
         gui.pose().pushPose();
         gui.pose().translate(mySx, mySy, 300.0F);
         gui.pose().mulPose(Axis.ZP.rotationDegrees(self.getYRot() + 180.0F));
         if (mySquadId != -1) {
            RenderSystem.setShaderColor(0.0F, 1.0F, 0.0F, 1.0F);
         } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gui.blit(ICON_PLAYER_SELF, -5, -5, 10, 10, 0.0F, 0.0F, 16, 16, 16, 16);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.pose().popPose();
         if (this.isShowNicknamesHeld()) {
            gui.pose().pushPose();
            gui.pose().translate(mySx, mySy - 10, 450.0F);
            gui.pose().scale(0.6F, 0.6F, 1.0F);
            int myColor = mySquadId != -1 ? -11141291 : -171;
            gui.drawCenteredString(mc.font, Component.literal(self.getScoreboardName()), 0, 0, myColor);
            gui.pose().popPose();
         }
      }
   }

   private boolean isPointOnMap(int x, int y) {
      return x >= this.mapX && x <= this.mapX + this.mapSize && y >= this.mapY && y <= this.mapY + this.mapSize;
   }

   private ResourceLocation getFlagTexture(String faction) {
      return faction != null && !faction.equalsIgnoreCase("none") ? new ResourceLocation("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png") : null;
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
      } else {
         return false;
      }
   }

   private void renderVehicles(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
      if (ClientData.clientVehicles != null && !ClientData.clientVehicles.isEmpty()) {
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

         for (WarfareWorldData.VehicleRecord record : ClientData.clientVehicles) {
            if (record.team.equalsIgnoreCase(myTeam) || isObserver) {
               double dx = (record.x - cx) / bpp;
               double dy = (record.z - cz) / bpp;
               int screenX = (int)(this.mapX + this.mapSize / 2 + dx);
               int screenY = (int)(this.mapY + this.mapSize / 2 + dy);
               if (this.isPointOnMap(screenX, screenY)) {
                  ResourceLocation icon = VEHICLE_ICONS.getOrDefault(record.type, VEHICLE_ICONS.get("DEFAULT"));
                  this.setFilter(icon, true);
                   pose.pushPose();
                   pose.translate(screenX, screenY, 150.0F);
                   if (!record.type.equals("Mine")) {
                      pose.mulPose(Axis.ZP.rotationDegrees(record.yaw + 180.0F));
                   }
                   RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                   RenderSystem.enableBlend();
                   gui.blit(icon, -6, -6, 12, 12, 0.0F, 0.0F, 16, 16, 16, 16);
                  pose.popPose();
                  this.setFilter(icon, false);
               }
            }
         }
      }
   }

    private void renderLatticeLines(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
       if (ClientData.allCapturePoints != null && !ClientData.allCapturePoints.isEmpty()) {
          if (mc.level != null) {
             String currentDim = mc.level.dimension().location().toString();
             boolean isRed = mc.player != null && mc.player.getTeam() != null
                && mc.player.getTeam().getName().equalsIgnoreCase("Red");
             List<WarfareWorldData.CapturePoint> sortedPoints = new ArrayList<>(ClientData.allCapturePoints);
             if (isRed) {
                sortedPoints.sort(Comparator.comparingInt(p -> p.redPriority));
             } else {
                sortedPoints.sort(Comparator.comparingInt(p -> p.bluePriority));
             }
             List<Vec3> path = new ArrayList<>();
             if (isRed) {
                if (ClientData.redSpawns.containsKey(currentDim)) {
                   BlockPos rPos = ClientData.redSpawns.get(currentDim);
                   path.add(new Vec3(rPos.getX() + 0.5, rPos.getY(), rPos.getZ() + 0.5));
                }
             } else {
                if (ClientData.blueSpawns.containsKey(currentDim)) {
                   BlockPos bPos = ClientData.blueSpawns.get(currentDim);
                   path.add(new Vec3(bPos.getX() + 0.5, bPos.getY(), bPos.getZ() + 0.5));
                }
             }

             for (WarfareWorldData.CapturePoint cp : sortedPoints) {
                path.add(cp.area.getCenter());
             }

             if (isRed) {
                if (ClientData.blueSpawns.containsKey(currentDim)) {
                   BlockPos bPos = ClientData.blueSpawns.get(currentDim);
                   path.add(new Vec3(bPos.getX() + 0.5, bPos.getY(), bPos.getZ() + 0.5));
                }
             } else {
                if (ClientData.redSpawns.containsKey(currentDim)) {
                   BlockPos rPos = ClientData.redSpawns.get(currentDim);
                   path.add(new Vec3(rPos.getX() + 0.5, rPos.getY(), rPos.getZ() + 0.5));
                }
             }

             int lineColor = 1728053247;

             for (int i = 0; i < path.size() - 1; i++) {
                Vec3 p1 = path.get(i);
                Vec3 p2 = path.get(i + 1);
                int x1 = (int)(this.mapX + this.mapSize / 2 + (p1.x - cx) / bpp);
                int y1 = (int)(this.mapY + this.mapSize / 2 + (p1.z - cz) / bpp);
                int x2 = (int)(this.mapX + this.mapSize / 2 + (p2.x - cx) / bpp);
                int y2 = (int)(this.mapY + this.mapSize / 2 + (p2.z - cz) / bpp);
                this.drawSolidLine(gui, x1, y1, x2, y2, lineColor);
             }
          }
       }
    }

   private void renderMainBases(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
      if (mc.level != null) {
         String currentDim = mc.level.dimension().location().toString();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         if (ClientData.blueSpawns.containsKey(currentDim)) {
            BlockPos pos = ClientData.blueSpawns.get(currentDim);
            this.drawMainBaseIcon(gui, mc, pos, cx, cz, bpp, ClientData.BLUE_FACTION, -13408564);
         }

         if (ClientData.redSpawns.containsKey(currentDim)) {
            BlockPos pos = ClientData.redSpawns.get(currentDim);
            this.drawMainBaseIcon(gui, mc, pos, cx, cz, bpp, ClientData.RED_FACTION, -3394765);
         }
      }
   }

   private void drawMainBaseIcon(GuiGraphics gui, Minecraft mc, BlockPos pos, double cx, double cz, double bpp, String faction, int fallbackColor) {
      int pX = (int)(this.mapX + this.mapSize / 2 + (pos.getX() + 0.5 - cx) / bpp);
      int pY = (int)(this.mapY + this.mapSize / 2 + (pos.getZ() + 0.5 - cz) / bpp);
      if (this.isPointOnMap(pX, pY)) {
         ResourceLocation flagTex = this.getFlagTexture(faction);
         if (flagTex != null && !faction.equals("none")) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.setFilter(flagTex, true);
            gui.blit(flagTex, pX - 8, pY - 4, 16, 9, 0.0F, 0.0F, 64, 36, 64, 36);
            this.setFilter(flagTex, false);
         } else {
            gui.fill(pX - 8, pY - 4, pX + 8, pY + 5, fallbackColor);
         }

         boolean isSelected = this.selectedSpawnId.equals("MAIN");
         ResourceLocation mainTex = isSelected ? MAIN_BASE_ICON_SELECTED : MAIN_BASE_ICON;
         this.setFilter(mainTex, true);
         gui.pose().pushPose();
         gui.pose().translate(pX, pY, 150.0F);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.blit(mainTex, -6, -6, 12, 12, 0.0F, 0.0F, 16, 16, 16, 16);
         gui.pose().popPose();
         this.setFilter(mainTex, false);
         gui.pose().pushPose();
         gui.pose().translate(pX, pY + 7, 151.0F);
         float textScale = 0.6F;
         gui.pose().scale(textScale, textScale, 1.0F);
         gui.drawCenteredString(mc.font, "MAIN", 0, 0, -1);
         gui.pose().popPose();
      }
   }

   private void drawSolidLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(len < 1.0F)) {
         float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         gui.pose().pushPose();
         gui.pose().translate(x1, y1, 0.0F);
         gui.pose().mulPose(Axis.ZP.rotationDegrees(angle));
         gui.fill(0, 0, (int)len, 1, color);
         gui.pose().popPose();
      }
   }

   private void drawSmoothCircle(GuiGraphics gui, float cx, float cy, float radius, int color) {
      if (!(radius <= 0.0F)) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder bufferbuilder = tesselator.getBuilder();
         Matrix4f matrix = gui.pose().last().pose();
         float a = (color >> 24 & 0xFF) / 255.0F;
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         bufferbuilder.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
         int segments = 128;

         for (int i = 0; i <= segments; i++) {
            float angle = i * (float) (Math.PI * 2) / segments;
            float x = cx + Mth.cos(angle) * radius;
            float y = cy + Mth.sin(angle) * radius;
            bufferbuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
         }

         tesselator.end();
         RenderSystem.disableBlend();
      }
   }

   private void renderArtilleryZones(GuiGraphics gui, double cx, double cz, double bpp) {
      if (ClientData.activeStrikes != null && !ClientData.activeStrikes.isEmpty()) {
         float radius = ((Integer)WarfareConfig.ART_STRIKE_RADIUS.get()).floatValue();

         for (WarfareWorldData.ActiveStrike strike : ClientData.activeStrikes) {
            double dx = (strike.pos.getX() + 0.5 - cx) / bpp;
            double dy = (strike.pos.getZ() + 0.5 - cz) / bpp;
            float sx = (float)(this.mapX + this.mapSize / 2.0 + dx);
            float sy = (float)(this.mapY + this.mapSize / 2.0 + dy);
            this.drawSmoothCircle(gui, sx, sy, radius / (float)bpp, -65536);
         }
      }
   }

   private void renderStructures(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
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
      double minHubDist = ((Integer)WarfareConfig.MIN_HUB_DISTANCE.get()).intValue();
      double buildRad = ((Integer)WarfareConfig.HUB_BUILD_RADIUS.get()).intValue();

      for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
         if (hub.constructed && (hub.team.equalsIgnoreCase(myTeam) || isObserver)) {
            double dx = (hub.pos.getX() + 0.5 - cx) / bpp;
            double dy = (hub.pos.getZ() + 0.5 - cz) / bpp;
            float sx = (float)(this.mapX + this.mapSize / 2.0 + dx);
            float sy = (float)(this.mapY + this.mapSize / 2.0 + dy);
            pose.pushPose();
            pose.translate(0.0F, 0.0F, 50.0F);
            this.drawSmoothCircle(gui, sx, sy, (float)(minHubDist / bpp), 1627389951);
            int teamCircleColor = hub.team.equalsIgnoreCase("BLUE") ? -2141891073 : -2130750123;
            this.drawSmoothCircle(gui, sx, sy, (float)(buildRad / bpp), teamCircleColor);
            pose.popPose();
            if (this.isPointOnMap((int)sx, (int)sy)) {
                String hubPayload = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
                boolean isSelected = this.selectedSpawnId.equals(hubPayload);
                ResourceLocation hubTex = isSelected ? HUB_ICON_SELECTED : HUB_ICON;
                this.setFilter(hubTex, true);
                pose.pushPose();
                pose.translate(sx, sy, 160.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                 gui.blit(hubTex, -6, -6, 12, 12, 0.0F, 0.0F, 16, 16, 16, 16);
                 pose.popPose();
                 this.setFilter(hubTex, false);
                 String matsText = String.valueOf(hub.materials);
                 int matsW = mc.font.width(matsText);
                 int iconSize = 8;
                 int matsX = (int)sx + 8;
                 int matsY = (int)sy - 2;
                 pose.pushPose();
                 pose.translate(0.0F, 0.0F, 165.0F);
                 RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                 this.setFilter(MATS_ICON, true);
                 gui.blit(MATS_ICON, matsX, matsY, iconSize, iconSize, 0.0F, 0.0F, 16, 16, 16, 16);
                 this.setFilter(MATS_ICON, false);
                 gui.drawString(mc.font, matsText, matsX + iconSize + 2, matsY + 1, -22016, false);
                 pose.popPose();
              }
         }
      }

      for (WarfareWorldData.Squad squad : ClientData.clientSquads) {
         if (squad.rallyPos != null && (squad.team.equalsIgnoreCase(myTeam) || isObserver)) {
            double dx = (squad.rallyPos.getX() + 0.5 - cx) / bpp;
            double dy = (squad.rallyPos.getZ() + 0.5 - cz) / bpp;
            float sx = (float)(this.mapX + this.mapSize / 2.0 + dx);
            float sy = (float)(this.mapY + this.mapSize / 2.0 + dy);
            if (this.isPointOnMap((int)sx, (int)sy)) {
                boolean isSelected = this.selectedSpawnId.equals("RALLY");
                ResourceLocation rallyTex = isSelected ? RALLY_ICON_SELECTED : RALLY_ICON;
                this.setFilter(rallyTex, true);
                pose.pushPose();
                pose.translate(sx, sy, 170.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                gui.blit(rallyTex, -5, -5, 10, 10, 0.0F, 0.0F, 16, 16, 16, 16);
                pose.popPose();
                this.setFilter(rallyTex, false);
             }
         }
      }
   }

   private void renderSquadMarkerLogic(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
      if (mc.player != null) {
         String myName = mc.player.getScoreboardName();
         WarfareWorldData.Squad mySquad = null;

         for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) {
               mySquad = s;
               break;
            }
         }

          if (mySquad != null) {
             if (mySquad.marker != null && mySquad.marker.type != 6) {
                boolean withDash = mySquad.marker.type != 0;
                this.drawMapMarkerAndLine(
                   gui, mc, cx, cz, bpp, mySquad.marker, this.getSquadMarkerIcon(mySquad.marker.type), this.getSquadMarkerColor(mySquad.marker.type), withDash
                );
             }

             if (mySquad.bravoMarker != null && mySquad.bravoMarker.type != 6) {
                boolean withDash = mySquad.bravoMarker.type != 0;
                this.drawMapMarkerAndLine(gui, mc, cx, cz, bpp, mySquad.bravoMarker, this.getBravoMarkerIcon(mySquad.bravoMarker.type), -65281, withDash);
             }

             if (mySquad.charlieMarker != null && mySquad.charlieMarker.type != 6) {
                boolean withDash = mySquad.charlieMarker.type != 0;
                this.drawMapMarkerAndLine(
                   gui, mc, cx, cz, bpp, mySquad.charlieMarker, this.getCharlieMarkerIcon(mySquad.charlieMarker.type), -16711766, withDash
                );
             }
          }
      }
   }

   private void drawMapMarkerAndLine(
      GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp, WarfareWorldData.SquadMarker m, ResourceLocation icon, int color, boolean withDash
   ) {
      int mx = (int)(this.mapX + this.mapSize / 2 + (m.x - cx) / bpp);
      int my = (int)(this.mapY + this.mapSize / 2 + (m.z - cz) / bpp);
      int px = (int)(this.mapX + this.mapSize / 2 + (mc.player.getX() - cx) / bpp);
      int py = (int)(this.mapY + this.mapSize / 2 + (mc.player.getZ() - cz) / bpp);
      if (withDash) {
         this.drawDashedLine(gui, px, py, mx, my, color);
      }

      if (this.isPointOnMap(mx, my)) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.blit(icon, mx - 6, my - 6, 0.0F, 0.0F, 12, 12, 12, 12);
         double distance = Math.sqrt(mc.player.distanceToSqr(m.x, mc.player.getY(), m.z));
         String distText = (int)distance + "m";
         gui.pose().pushPose();
         gui.pose().translate(mx, my + 8, 600.0F);
         gui.pose().scale(0.8F, 0.8F, 1.0F);
         int textWidth = mc.font.width(distText);
         gui.drawString(mc.font, distText, -(textWidth / 2), 0, color, true);
         gui.pose().popPose();
      }
   }

   private int getSquadMarkerColor(int type) {
      switch (type) {
         case 1:
            return -22016;
         case 2:
            return -11184641;
         case 3:
            return -43521;
         case 4:
            return -1;
         case 5:
            return -65536;
         default:
            return -11141291;
      }
   }

   private ResourceLocation getSquadMarkerIcon(int type) {
      switch (type) {
         case 1:
            return MARKER_ATTACK;
         case 2:
            return MARKER_DEFEND;
         case 3:
            return MARKER_BUILD;
         case 4:
         default:
            return MARKER_MOVE;
         case 5:
            return MARKER_ATTACK;
      }
   }

   private ResourceLocation getBravoMarkerIcon(int type) {
      switch (type) {
         case 1:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_attack_bravo.png");
         case 2:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_defend_bravo.png");
         case 3:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_build_bravo.png");
         default:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move_bravo.png");
      }
   }

   private ResourceLocation getCharlieMarkerIcon(int type) {
      switch (type) {
         case 1:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_attack_charlie.png");
         case 2:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_defend_charlie.png");
         case 3:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_build_charlie.png");
         default:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move_charlie.png");
      }
   }

   private void drawDashedLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
      int dx = x2 - x1;
      int dy = y2 - y1;
      double len = Math.sqrt(dx * dx + dy * dy);
      if (!(len < 5.0)) {
         for (int i = 0; i < len; i += 4) {
            double t = i / len;
            int lx = (int)(x1 + dx * t);
            int ly = (int)(y1 + dy * t);
            if (this.isPointOnMap(lx, ly)) {
               gui.fill(lx, ly, lx + 2, ly + 2, color);
            }
         }
      }
   }

   private String getPlayerTeamStrict(Minecraft mc) {
      if (mc.player != null && mc.player.getTeam() != null) {
         String name = mc.player.getTeam().getName().toUpperCase();
         if (name.contains("BLUE")) {
            return "BLUE";
         } else {
            return name.contains("RED") ? "RED" : name;
         }
      } else {
         return "NEUTRAL";
      }
   }

   private void renderSquadPings(GuiGraphics gui, double cx, double cz, double bpp) {
      Minecraft mc = Minecraft.getInstance();
      String myName = mc.player.getScoreboardName();
      WarfareWorldData.Squad mySquad = null;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) {
            mySquad = s;
            break;
         }
      }

      if (mySquad != null) {
         long time = mc.level.getGameTime();
         boolean isSL = mySquad.leader.equals(myName);
         boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
         boolean isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
         if (mySquad.pingPos != null && time < mySquad.pingExpiry) {
            this.drawPingOnMap(gui, mySquad.pingPos, new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye.png"), cx, cz, bpp);
         }

         if (mySquad.bravoPingPos != null && time < mySquad.bravoPingExpiry && (isSL || isBravo)) {
            this.drawPingOnMap(gui, mySquad.bravoPingPos, new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_bravo.png"), cx, cz, bpp);
         }

         if (mySquad.charliePingPos != null && time < mySquad.charliePingExpiry && (isSL || isCharlie)) {
            this.drawPingOnMap(gui, mySquad.charliePingPos, new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_charlie.png"), cx, cz, bpp);
         }
      }
   }

   private void drawPingOnMap(GuiGraphics gui, BlockPos pos, ResourceLocation icon, double cx, double cz, double bpp) {
      double dx = (pos.getX() + 0.5 - cx) / bpp;
      double dz = (pos.getZ() + 0.5 - cz) / bpp;
      int px = (int)(this.mapX + this.mapSize / 2 + dx);
      int py = (int)(this.mapY + this.mapSize / 2 + dz);
      if (this.isPointOnMap(px, py)) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.blit(icon, px - 6, py - 6, 0.0F, 0.0F, 12, 12, 12, 12);
      }
   }

   private void renderTacticalMarkers(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
      String myTeam = this.getPlayerTeamStrict(mc);
      long currentTime = mc.level.getGameTime();
      float totalLifetime = 3600.0F;

      for (WarfareWorldData.MapMarker m : ClientData.activeMarkers) {
         if (m.team.equalsIgnoreCase(myTeam)) {
            long timeLeft = m.expiryTick - currentTime;
            if (timeLeft > 0L) {
               float alpha = Mth.clamp((float)timeLeft / totalLifetime, 0.0F, 1.0F);
               double dx = (m.pos.getX() - cx) / bpp;
               double dy = (m.pos.getZ() - cz) / bpp;
               int sx = (int)(this.mapX + this.mapSize / 2 + dx);
               int sy = (int)(this.mapY + this.mapSize / 2 + dy);
               if (this.isPointOnMap(sx, sy)) {
                  ResourceLocation icon = this.getMarkerIcon(m.type);
                  this.setFilter(icon, true);
                  gui.pose().pushPose();
                  gui.pose().translate(sx, sy, 120.0F);
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                  gui.blit(icon, -8, -8, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  gui.pose().popPose();
                  this.setFilter(icon, false);
               }
            }
         }
      }
   }

   private void renderSquadRhombusMarkers(GuiGraphics gui, Minecraft mc, double cx, double cz, double bpp) {
      long time = mc.level.getGameTime();
      String myName = mc.player.getScoreboardName();
      String myTeam = this.getPlayerTeamStrict(mc);
      boolean amISquadLeader = this.isSquadLeaderOrFTL(mc.player);
      boolean isBlueForNums = myTeam != null && myTeam.contains("BLUE");
      int teamCMDIdForNums = isBlueForNums ? ClientData.blueCMDId : ClientData.redCMDId;
      List<WarfareWorldData.Squad> teamSquadsForNums = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeam)).collect(Collectors.toList());
      teamSquadsForNums.sort((s1, s2) -> {
         if (s1.id == teamCMDIdForNums && teamCMDIdForNums != -1) {
            return -1;
         } else {
            return s2.id == teamCMDIdForNums && teamCMDIdForNums != -1 ? 1 : Integer.compare(s1.id, s2.id);
         }
      });

      for (WarfareWorldData.Squad squad : ClientData.clientSquads) {
         if (squad.team.equalsIgnoreCase(myTeam)) {
            boolean canSee = squad.members.contains(myName) || amISquadLeader;
            if (canSee) {
               for (WarfareWorldData.SquadMarker rm : squad.rhombusMarkers) {
                  long timeLeft = rm.expiryTick - time;
                  if (timeLeft > 0L) {
                     float alpha = Mth.clamp((float)timeLeft / 3600.0F, 0.1F, 1.0F);
                     int mx = (int)(this.mapX + this.mapSize / 2 + (rm.x - cx) / bpp);
                     int my = (int)(this.mapY + this.mapSize / 2 + (rm.z - cz) / bpp);
                     if (this.isPointOnMap(mx, my)) {
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                        gui.blit(new ResourceLocation("pwpwarfare", "textures/gui/map_icons/squad_rhombus.png"), mx - 8, my - 8, 0.0F, 0.0F, 16, 16, 16, 16);
                        gui.pose().pushPose();
                        gui.pose().translate(mx, my, 500.0F);
                        gui.pose().scale(0.5F, 0.5F, 1.0F);
                        int displayNum = 0;

                        for (int i = 0; i < teamSquadsForNums.size(); i++) {
                           if (teamSquadsForNums.get(i).id == squad.id) {
                              displayNum = i + 1;
                              break;
                           }
                        }

                        String numStr = String.valueOf(displayNum);
                        int whiteWithAlpha = (int)(alpha * 255.0F) << 24 | 16777215;
                        this.drawSquadNumber(gui, mc.font, numStr, -(mc.font.width(numStr) / 2), -4, whiteWithAlpha);
                        gui.pose().popPose();
                     }
                  }
               }
            }
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
      if (mc.player != null) {
         if (!this.isSquadLeaderOrFTL(mc.player)) {
            mc.player.displayClientMessage(Component.translatable("gui.pwpwarfare.map_marker.error").withStyle(ChatFormatting.RED), true);
         } else {
            double bpp = this.getBlocksPerPixel();
            double centerX = this.getCenterX(mc.player);
            double centerZ = this.getCenterZ(mc.player);
            int targetX = (int)(centerX + (mouseX - (this.mapX + this.mapSize / 2.0)) * bpp);
            int targetZ = (int)(centerZ + (mouseY - (this.mapY + this.mapSize / 2.0)) * bpp);
            mc.setScreen(new TacticalMapRadialScreen(targetX, targetZ, mc.screen));
         }
      }
   }

   private boolean isSquadLeaderOrFTL(LocalPlayer player) {
      if (player == null) {
         return false;
      }

      String pName = player.getScoreboardName();

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.leader.equals(pName) || s.bravoLeader.equals(pName) || s.charlieLeader.equals(pName)) {
            return true;
         }
      }

      return false;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.isDraggingMap && button == 0) {
         this.mapPanX = this.mapPanX - (mouseX - this.lastMouseX) * ClientData.mapScale;
         this.mapPanZ = this.mapPanZ - (mouseY - this.lastMouseY) * ClientData.mapScale;
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         return true;
      } else {
         return false;
      }
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return mouseX >= this.mapX && mouseX <= this.mapX + this.mapSize && mouseY >= this.mapY && mouseY <= this.mapY + this.mapSize;
   }

   private boolean isShowNicknamesHeld() {
      Minecraft mc = Minecraft.getInstance();
      long window = mc.getWindow().getWindow();
      Key nickKey = ModKeyBindings.SHOW_NICKNAMES_KEY.getKey();
      return nickKey.getType() == Type.MOUSE ? GLFW.glfwGetMouseButton(window, nickKey.getValue()) == 1 : InputConstants.isKeyDown(window, nickKey.getValue());
   }

   @Override
   public void close() {
   }

   static {
      VEHICLE_ICONS.put("APC", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/apc.png"));
      VEHICLE_ICONS.put("TANK", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/tank.png"));
      VEHICLE_ICONS.put("HELICOPTER", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/helicopter.png"));
      VEHICLE_ICONS.put("CAS Helicopter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_helicopter.png"));
      VEHICLE_ICONS.put("CAS Fighter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_fighter.png"));
      VEHICLE_ICONS.put("Combat Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/combat_vehicle.png"));
      VEHICLE_ICONS.put("Infantry Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/infantry_vehicle.png"));
      VEHICLE_ICONS.put("Supply Truck", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_truck.png"));
      VEHICLE_ICONS.put("Supply Helicopter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_helicopter.png"));
      VEHICLE_ICONS.put("DEFAULT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/default.png"));
      VEHICLE_ICONS.put("Static ZU", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/static_zu.png"));
      VEHICLE_ICONS.put("Mobile ZU", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/mobile_zu.png"));
      VEHICLE_ICONS.put("BOAT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/boat.png"));
      VEHICLE_ICONS.put("Motorcycle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/motorcycle.png"));
      VEHICLE_ICONS.put("Light Supply", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/light_supply.png"));
      VEHICLE_ICONS.put("Mine", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/skull_marker.png"));
   }
}

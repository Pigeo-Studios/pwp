package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.block.AGSConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlock;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlockEntity;
import com.pigeostudios.pwp.warfare.block.HubBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.M2ConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.MainSupplyBlock;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.WallBlock;
import com.pigeostudios.pwp.warfare.block.WallBlockEntity;
import com.pigeostudios.pwp.warfare.client.gui.SquadMapRenderer;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Post;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import com.pwp.coreclient.gui.theme.PWPTheme;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
// Р С›РЎРѓР Р…Р С•Р Р†Р Р…Р С•Р в„– Р С•Р Р†Р ВµРЎР‚Р В»Р ВµР в„– Р СР С•Р Т‘Р В°, Р С•РЎвЂљР С•Р В±РЎР‚Р В°Р В¶Р В°РЎР‹РЎвЂ°Р С‘Р в„– HUD: Р С”Р С•Р СР С—Р В°РЎРѓ, Р В·Р В°РЎвЂ¦Р Р†Р В°РЎвЂљ РЎвЂљР С•РЎвЂЎР ВµР С”,
// РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”РЎС“, РЎвЂљР С‘Р С”Р ВµРЎвЂљРЎвЂ№, РЎС“Р Р†Р ВµР Т‘Р С•Р СР В»Р ВµР Р…Р С‘РЎРЏ, РЎР‚Р В°РЎвЂ Р С‘РЎР‹ Р С‘ Р Т‘РЎР‚РЎС“Р С–Р С‘Р Вµ РЎРЊР В»Р ВµР СР ВµР Р…РЎвЂљРЎвЂ№ Р С‘Р Р…РЎвЂљР ВµРЎР‚РЎвЂћР ВµР в„–РЎРѓР В°
public class WarfareOverlay {
   private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
   private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
   private static final ResourceLocation FLAG_USA = new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
   private static final ResourceLocation FLAG_NATO = new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
   private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
   private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
   private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
   private static final ResourceLocation FLAG_PMC = new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");
   private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("pwpwarfare", "textures/misc/vignette.png");
   private static final ResourceLocation ARROW_TEX = new ResourceLocation("pwpwarfare", "textures/gui/capture_arrow.png");
   private static final ResourceLocation BUILD_ICON = new ResourceLocation("pwpwarfare", "textures/gui/build_icon.png");
   private static final ResourceLocation DIG_ICON = new ResourceLocation("pwpwarfare", "textures/gui/dig_icon.png");
   private static final ResourceLocation SHOVEL_ICON = new ResourceLocation("pwpwarfare", "textures/gui/shovel_icon.png");
   private static final ResourceLocation ICON_PLUS = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/medic_plus.png");
   private static final ResourceLocation ICON_PING = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye.png");
   private static final ResourceLocation MOVE_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move.png");
   private static final ResourceLocation MOVE_TEX_BRAVO = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move_bravo.png");
   private static final ResourceLocation PING_TEX_BRAVO = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_bravo.png");
   private static final ResourceLocation MOVE_TEX_CHARLIE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move_charlie.png");
   private static final ResourceLocation PING_TEX_CHARLIE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_charlie.png");
    private static SquadMapRenderer HUD_SIDE_MAP;
   private static final ResourceLocation MOUSE_LEFT = new ResourceLocation("pwpwarfare", "textures/gui/dig_icon.png");
   private static final ResourceLocation MOUSE_RIGHT = new ResourceLocation("pwpwarfare", "textures/gui/build_icon.png");
   private static final ResourceLocation ICON_ROTATE = new ResourceLocation("pwpwarfare", "textures/gui/icon_rotate.png");
   private static final ResourceLocation ICON_CONFIRM = new ResourceLocation("pwpwarfare", "textures/gui/icon_confirm.png");
   private static final ResourceLocation VOICE_ICON_TEX = new ResourceLocation("pwpwarfare", "textures/gui/voice_icon.png");
    private static final ResourceLocation VOICE_ICON_RADIO_TEX = new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_radio.png");
    private static final ResourceLocation VOICE_ICON_STAR_TEX = new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_star.png");
    private static final Map<String, ResourceLocation> SQUADCALC_ICONS = new HashMap<>();

    private static ResourceLocation squadCalcIcon(String type, String team) {
        String s = "enemy".equals(team) ? "_r" : "team".equals(team) ? "_y" : "_g";
        return SQUADCALC_ICONS.computeIfAbsent(type + s, k -> new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + k + ".png"));
    }

    @SubscribeEvent
    public static void onRenderOverlay(Post event) {
      if (event.getOverlay() == VanillaGuiOverlay.CHAT_PANEL.type()) {
         GuiGraphics gui = event.getGuiGraphics();
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            renderTickets(gui, mc, width);
            renderVotePanel(gui, mc, height);
            if (ClientData.allCapturePoints != null) {
               for (WarfareWorldData.CapturePoint cp : ClientData.allCapturePoints) {
                  if (mc.player.getBoundingBox().intersects(cp.area)) {
                     renderCapturePoint(gui, mc, width, height);
                     break;
                  }
               }
            }

            renderBuildProgress(gui, mc, width, height);
            renderHubMaterials(gui, mc, width, height);
            renderVehicleAmmo(gui, mc, width, height);
            renderSupplyTruckInfo(gui, mc, width, height);
            renderPlacementHints(gui, mc, width, height);
            renderCaptureNotifications(gui, mc, width, height);
            renderCompass(gui, mc, width, event.getPartialTick());
            renderCMDVotePanel(gui, mc, width);
            renderArtStrikeRequest(gui, mc, width);
            if (mc.player.getPersistentData().getBoolean("WARFARE_IsDowned")) {
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.setShaderColor(0.5F, 0.0F, 0.0F, 0.9F);
               gui.blit(VIGNETTE_TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.depthMask(true);
               RenderSystem.enableDepthTest();
            }

            String currentKit = mc.player.getPersistentData().getString("WARFARE_CurrentKit");
            if ("Medic".equalsIgnoreCase(currentKit)) {
               renderMedicUI(gui, mc, width);
            }

            renderRadioSpeakers(gui, mc, height);
            renderVoiceSpeakers(gui, mc, height);
            if (ClientData.isMapOpen || ClientData.mapTransition > 0.0F) {
               renderSideMap(gui, mc, width, height, event.getPartialTick());
            }
         }
      }
   }

   private static void renderCompass(GuiGraphics gui, Minecraft mc, int screenWidth, float partialTick) {
      if (!mc.options.hideGui) {
         int configVal = (Integer)WarfareConfig.COMPASS_SCALE.get();
         float scaleFactor = 1.0F;
         if (configVal == 1) {
            scaleFactor = 0.6666667F;
         } else if (configVal == 3) {
            scaleFactor = 1.5F;
         }

         float centerX = screenWidth / 2.0F;
         float y = 8.0F;
         float baseWidth = 320.0F;
         float scaledWidth = baseWidth * scaleFactor;
         float visibleRange = 70.0F;
         float pixelsPerDegree = scaledWidth / visibleRange;
         gui.pose().pushPose();
         gui.pose().translate(centerX, y, 0.0F);
         gui.pose().scale(scaleFactor, scaleFactor, 1.0F);
         gui.pose().translate(-centerX, -y, 0.0F);
         gui.pose().pushPose();
         gui.pose().translate(centerX, y + 2.0F, 100.0F);
         gui.pose().scale(0.8F, 0.8F, 1.0F);
         drawOutlinedString(gui, mc, "\u25BC", -(PWPTheme.Fonts.display().width("\u25BC") / 2), 0, -1);
         gui.pose().popPose();
         float playerYaw = (mc.player.getViewYRot(partialTick) % 360.0F + 360.0F) % 360.0F;
         String bearing = String.valueOf((int)playerYaw);
         gui.pose().pushPose();
         gui.pose().translate(centerX, y + 26.0F, 100.0F);
         drawOutlinedString(gui, mc, bearing, -(PWPTheme.Fonts.display().width(bearing) / 2), 0, -1);
         gui.pose().popPose();
         gui.enableScissor((int)(centerX - scaledWidth / 2.0F), 0, (int)(centerX + scaledWidth / 2.0F), screenHeight());

         for (int i = (int)(playerYaw - visibleRange / 2.0F) - 1; i <= (int)(playerYaw + visibleRange / 2.0F) + 1; i++) {
            int degree = (i % 360 + 360) % 360;
            float xPos = centerX + (i - playerYaw) * pixelsPerDegree;
            float diffFromCenter = Math.abs(xPos - centerX);
            float edgeFading = 1.0F - (float)Math.pow(diffFromCenter / (scaledWidth / 2.0F), 2.0);
            if (!(edgeFading <= 0.02F)) {
               int alpha = (int)(edgeFading * 255.0F);
               if (degree % 15 == 0) {
                  drawSmoothLine(gui, xPos, y + 10.0F, 1.2F, 6.0F, alpha << 24 | 16777215);
                  String label = getDirectionLabel(degree);
                  int txtCol = !Character.isDigit(label.charAt(0)) ? alpha << 24 | 8965375 : alpha << 24 | 16777215;
                  gui.pose().pushPose();
                  gui.pose().translate(xPos, y + 18.0F, 0.0F);
                  gui.pose().scale(0.75F, 0.75F, 1.0F);
                  drawOutlinedStringWithAlpha(gui, mc, label, -(PWPTheme.Fonts.display().width(label) / 2), 0, txtCol, alpha);
                  gui.pose().popPose();
               } else if (degree % 5 == 0) {
                  drawSmoothLine(gui, xPos, y + 12.0F, 1.0F, 4.0F, alpha << 24 | 16777215);
               } else {
                  drawSmoothLine(gui, xPos, y + 14.0F, 0.8F, 1.5F, alpha << 24 | 16777215);
               }
            }
         }

         renderMarkersOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
         renderDownedOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
         renderSquadPingOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
         renderSquadMarkersOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
         gui.disableScissor();
         gui.pose().popPose();
      }
   }

   private static void renderSquadMarkersOnCompass(
      GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange
   ) {
      String myName = mc.player.getScoreboardName();
      WarfareWorldData.Squad mySquad = null;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) {
            mySquad = s;
            break;
         }
      }

      if (mySquad != null) {
         boolean isSL = mySquad.leader.equals(myName);
         if (!mySquad.bravoMembers.contains(myName) && !mySquad.bravoLeader.equals(myName)) {
            boolean isBravo = false;
         } else {
            boolean isBravo = true;
         }

         if (!mySquad.charlieMembers.contains(myName) && !mySquad.charlieLeader.equals(myName)) {
            boolean isCharlie = false;
         } else {
            boolean isCharlie = true;
         }

         if (mySquad.marker != null && mySquad.marker.type == 0) {
            drawSquadCompassMarker(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.marker, MOVE_TEXTURE);
         }

         if (mySquad.bravoMarker != null && mySquad.bravoMarker.type == 0) {
            drawSquadCompassMarker(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.bravoMarker, MOVE_TEX_BRAVO);
         }

         if (mySquad.charlieMarker != null && mySquad.charlieMarker.type == 0) {
            drawSquadCompassMarker(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.charlieMarker, MOVE_TEX_CHARLIE);
         }
      }
   }

   private static void drawSquadCompassMarker(
      GuiGraphics gui, Minecraft mc, float pYaw, float cX, float y, float ppd, float wPix, float vRange, WarfareWorldData.SquadMarker m, ResourceLocation icon
   ) {
      double dist = Math.sqrt(mc.player.distanceToSqr(m.x + 0.5, mc.player.getY(), m.z + 0.5));
      if (!(dist > 5000.0)) {
         double dx = m.x + 0.5 - mc.player.getX();
         double dz = m.z + 0.5 - mc.player.getZ();
         float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         float diff = Mth.wrapDegrees(angle - pYaw);
         if (Math.abs(diff) < vRange / 2.0F + 5.0F) {
            float xPos = cX + diff * ppd;
            float edgeFading = 1.0F - (float)Math.pow(Math.abs(xPos - cX) / (wPix / 2.0F), 4.0);
            renderCompassIcon(gui, icon, xPos, y - 2.0F, 14, Math.max(0.0F, edgeFading));
         }
      }
   }

   private static ResourceLocation getSquadMarkerIcon(int type) {
      switch (type) {
         case 1:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_attack.png");
         case 2:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_defend.png");
         case 3:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_build.png");
         case 4:
         default:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move.png");
         case 5:
            return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_attack.png");
      }
   }

   private static void renderMarkersOnCompass(
      GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange
   ) {
      String myTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";

       for (WarfareWorldData.MapMarker m : ClientData.activeMarkers) {
          if (m.team.equalsIgnoreCase(myTeam) || mc.player.isCreative()) {
             double dist = Math.sqrt(mc.player.distanceToSqr(m.pos.getX() + 0.5, mc.player.getY(), m.pos.getZ() + 0.5));
             if (!(dist > 250.0)) {
                double dx = m.pos.getX() + 0.5 - mc.player.getX();
                double dz = m.pos.getZ() + 0.5 - mc.player.getZ();
                float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                float diff = Mth.wrapDegrees(angle - playerYaw);
                if (Math.abs(diff) < visibleRange / 2.0F + 5.0F) {
                   float xPos = centerX + diff * pixelsPerDegree;
                   float distAlpha = 1.0F;
                   if (dist > 150.0) {
                      distAlpha = 0.35F;
                   } else if (dist > 50.0) {
                      distAlpha = 0.7F;
                   }

                   float edgeFading = 1.0F - (float)Math.pow(Math.abs(xPos - centerX) / (widthInPixels / 2.0F), 4.0);
                   float finalAlpha = distAlpha * Math.max(0.0F, edgeFading);
                   if (finalAlpha > 0.05F) {
                      ResourceLocation icon = getMarkerIconLocal(m.type);
                      renderCompassIcon(gui, icon, xPos, y - 2.0F, 12, finalAlpha);
                   }
                }
             }
          }
       }

       long now = mc.level.getGameTime();
       for (var m : com.pigeostudios.pwp.warfare.client.MarkerClientCache.getAll()) {
          double dist = Math.sqrt(mc.player.distanceToSqr(m.pos.getX() + 0.5, mc.player.getY(), m.pos.getZ() + 0.5));
          if (dist > 300.0) continue;
          double dx = m.pos.getX() + 0.5 - mc.player.getX();
          double dz = m.pos.getZ() + 0.5 - mc.player.getZ();
          float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
          float diff = Mth.wrapDegrees(angle - playerYaw);
          if (Math.abs(diff) >= visibleRange / 2.0F + 5.0F) continue;
          float xPos = centerX + diff * pixelsPerDegree;
          float edgeFading = 1.0F - (float)Math.pow(Math.abs(xPos - centerX) / (widthInPixels / 2.0F), 4.0);
          float lifeAlpha = Math.max(0, (6000 - (now - m.createdAt)) / 6000f);
          float finalAlpha = lifeAlpha * Math.max(0.0F, edgeFading);
          if (finalAlpha > 0.05F) {
             renderCompassIcon(gui, squadCalcIcon(m.iconType, m.team), xPos, y - 2.0F, 12, finalAlpha);
          }
       }
    }

   private static void renderDownedOnCompass(
      GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange
   ) {
      if (ClientData.mapPlayers != null) {
         String myName = mc.player.getScoreboardName();
         String myTeam = "NEUTRAL";
         if (mc.player.getTeam() != null) {
            myTeam = mc.player.getTeam().getName().toUpperCase();
         }

         for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
            if (!info.name.equals(myName) && info.isDowned && !myTeam.equals("NEUTRAL") && info.team.equalsIgnoreCase(myTeam)) {
               double dx = info.x - mc.player.getX();
               double dz = info.z - mc.player.getZ();
               double dist = Math.sqrt(dx * dx + dz * dz);
               if (!(dist > 50.0)) {
                  float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                  float diff = Mth.wrapDegrees(angle - playerYaw);
                  if (Math.abs(diff) < visibleRange / 2.0F + 5.0F) {
                     float xPos = centerX + diff * pixelsPerDegree;
                     float edgeFading = 1.0F - (float)Math.pow(Math.abs(xPos - centerX) / (widthInPixels / 2.0F), 4.0);
                     float blink = 0.8F + (float)Math.sin(System.currentTimeMillis() / 200.0) * 0.2F;
                     float finalAlpha = Math.max(0.0F, edgeFading) * blink;
                     if (finalAlpha > 0.05F) {
                        renderCompassIcon(gui, ICON_PLUS, xPos, y - 2.0F, 10, finalAlpha);
                     }
                  }
               }
            }
         }
      }
   }

   private static void renderSquadPingOnCompass(
      GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange
   ) {
      String myName = mc.player.getScoreboardName();
      WarfareWorldData.Squad mySquad = null;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) {
            mySquad = s;
            break;
         }
      }

      if (mySquad != null) {
         boolean isSL = mySquad.leader.equals(myName);
         boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
         boolean isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
         long time = mc.level.getGameTime();
         if (mySquad.pingPos != null && time < mySquad.pingExpiry) {
            drawCompassPing(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.pingPos, ICON_PING);
         }

         if (mySquad.bravoPingPos != null && time < mySquad.bravoPingExpiry && (isSL || isBravo)) {
            drawCompassPing(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.bravoPingPos, PING_TEX_BRAVO);
         }

         if (mySquad.charliePingPos != null && time < mySquad.charliePingExpiry && (isSL || isCharlie)) {
            drawCompassPing(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.charliePingPos, PING_TEX_CHARLIE);
         }
      }
   }

   private static void drawCompassPing(
      GuiGraphics gui,
      Minecraft mc,
      float playerYaw,
      float centerX,
      float y,
      float pixelsPerDegree,
      float widthInPixels,
      float visibleRange,
      BlockPos pingPos,
      ResourceLocation icon
   ) {
      double dx = pingPos.getX() + 0.5 - mc.player.getX();
      double dz = pingPos.getZ() + 0.5 - mc.player.getZ();
      float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float diff = Mth.wrapDegrees(angle - playerYaw);
      if (Math.abs(diff) < visibleRange / 2.0F + 5.0F) {
         float xPos = centerX + diff * pixelsPerDegree;
         float edgeFading = 1.0F - (float)Math.pow(Math.abs(xPos - centerX) / (widthInPixels / 2.0F), 4.0);
         float blink = 0.8F + (float)Math.sin((float)mc.level.getGameTime() * 0.4F) * 0.2F;
         float finalAlpha = Math.max(0.0F, edgeFading) * blink;
         if (finalAlpha > 0.05F) {
            renderCompassIcon(gui, icon, xPos, y - 2.0F, 12, finalAlpha);
         }
      }
   }

   private static void renderCompassIcon(GuiGraphics gui, ResourceLocation icon, float x, float y, int size, float alpha) {
      gui.pose().pushPose();
      gui.pose().translate(x - size / 2.0F, y - size / 2.0F, 50.0F);
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      gui.blit(icon, 0, 0, 0.0F, 0.0F, size, size, size, size);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      gui.pose().popPose();
   }

   private static ResourceLocation getMarkerIconLocal(String type) {
      String path = type.toLowerCase().replace("enemy ", "").replace(" ", "_");
      return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + path + "_marker.png");
   }

   private static int screenHeight() {
      return Minecraft.getInstance().getWindow().getGuiScaledHeight();
   }

   private static void drawSmoothLine(GuiGraphics gui, float x, float y, float width, float height, int color) {
      gui.pose().pushPose();
      gui.pose().translate(x - width / 2.0F, y, 0.0F);
      gui.fill(0, 0, Math.max(1, (int)width), (int)height, color);
      gui.pose().popPose();
   }

   private static void drawOutlinedStringWithAlpha(GuiGraphics gui, Minecraft mc, String text, int x, int y, int color, int alpha) {
      int black = alpha << 24;
      gui.drawString(PWPTheme.Fonts.display(), text, x - 1, y, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x + 1, y, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x, y - 1, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x, y + 1, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x, y, color, false);
   }

   private static String getDirectionLabel(int degree) {
      switch (degree) {
         case 0:
            return "S";
         case 45:
            return "SW";
         case 90:
            return "W";
         case 135:
            return "NW";
         case 180:
            return "N";
         case 225:
            return "NE";
         case 270:
            return "E";
         case 315:
            return "SE";
         default:
            return String.valueOf(degree);
      }
   }

   private static void renderCMDVotePanel(GuiGraphics gui, Minecraft mc, int width) {
      String myTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
      if (!myTeam.equals("NEUTRAL")) {
         boolean isBlue = myTeam.equals("BLUE");
         boolean active = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
         if (active) {
            String candidateName = isBlue ? ClientData.blueCmdCandidateName : ClientData.redCmdCandidateName;
            Map<UUID, Boolean> currentVotes = isBlue ? ClientData.blueCmdVotes : ClientData.redCmdVotes;
            List<MapPlayerInfo> slPlayers = ClientData.mapPlayers
               .values()
               .stream()
               .filter(infox -> infox.team.equalsIgnoreCase(myTeam))
               .filter(infox -> infox.isLeader && !infox.name.equals(candidateName))
               .toList();
            int headerH = 32;
            int rowH = 12;
            int footerH = 15;
            int pWidth = 150;
            int pHeight = headerH + slPlayers.size() * rowH + footerH;
            int x = width - pWidth - 10;
            int y = 70;
            gui.fill(x, y, x + pWidth, y + pHeight, -1442840576);
            gui.renderOutline(x, y, pWidth, pHeight, -11141291);
            gui.drawCenteredString(PWPTheme.Fonts.display(), "CMD VOTE: " + candidateName, x + pWidth / 2, y + 5, -10496);
            gui.pose().pushPose();
            gui.pose().scale(0.75F, 0.75F, 1.0F);
            int sx = (int)((x + pWidth / 2) / 0.75F);
            gui.drawCenteredString(PWPTheme.Fonts.display(), "Needs 50% SL votes to pass", sx, (int)((y + 16) / 0.75F), -5592406);
            gui.pose().popPose();
            gui.fill(x + 5, y + 28, x + pWidth - 5, y + 29, 1442840575);
            int curY = y + headerH;

            for (MapPlayerInfo info : slPlayers) {
               Boolean vote = currentVotes.get(info.uuid);
               String icon = "\u25CB";
               int iconCol = -5592406;
               if (vote != null) {
                  icon = vote ? "\u2714" : "\u2718";
                  iconCol = vote ? -11141291 : -43691;
               }

               gui.drawString(PWPTheme.Fonts.display(), icon, x + 8, curY, iconCol, true);
               int nameCol = info.name.equals(mc.player.getScoreboardName()) ? -171 : -1;
               gui.drawString(PWPTheme.Fonts.display(), info.name, x + 22, curY, nameCol, true);
               curY += rowH;
            }

            if (!mc.player.getScoreboardName().equals(candidateName)) {
               gui.drawCenteredString(PWPTheme.Fonts.display(), "F7: YES | F8: NO", x + pWidth / 2, y + pHeight - 12, -4473925);
            } else {
               gui.drawCenteredString(PWPTheme.Fonts.display(), "WAITING FOR VOTES", x + pWidth / 2, y + pHeight - 12, -11141291);
            }
         }
      }
   }

   private static void renderArtStrikeRequest(GuiGraphics gui, Minecraft mc, int width) {
      if (mc.player != null) {
         String myTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
         if (!myTeam.equals("NEUTRAL")) {
            int timer = myTeam.equals("BLUE") ? ClientData.blueArtTimer : ClientData.redArtTimer;
            String requester = myTeam.equals("BLUE") ? ClientData.blueArtReqName : ClientData.redArtReqName;
            BlockPos pos = myTeam.equals("BLUE") ? ClientData.blueArtPos : ClientData.redArtPos;
            if (timer > 0 && !requester.isEmpty() && pos != null) {
               int mySquadId = mc.player.getPersistentData().getInt("WARFARE_SquadID");
               boolean isSL = mc.player.getPersistentData().getBoolean("WARFARE_IsSquadLeader");
               int teamCmdId = myTeam.equals("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
               if (mc.player.isCreative() || isSL && mySquadId != -1 && mySquadId == teamCmdId) {
                  int x = width - 160;
                  int y = 140;
                  gui.fill(x, y, x + 150, y + 45, -1442840576);
                  gui.renderOutline(x, y, 150, 45, -43691);
                  gui.drawCenteredString(PWPTheme.Fonts.display(), "ARTILLERY REQUEST", x + 75, y + 5, -43691);
                  gui.pose().pushPose();
                  gui.pose().scale(0.8F, 0.8F, 1.0F);
                  int sx = (int)((x + 75) / 0.8F);
                  gui.drawCenteredString(PWPTheme.Fonts.display(), "From: " + requester, sx, (int)((y + 18) / 0.8F), -1);
                  gui.drawCenteredString(PWPTheme.Fonts.display(), "Pos: " + pos.getX() + ", " + pos.getZ(), sx, (int)((y + 28) / 0.8F), -5592406);
                  gui.pose().popPose();
                  gui.drawCenteredString(PWPTheme.Fonts.display(), "PgUp: CONFIRM | PgDn: DENY", x + 75, y + 35, -171);
                  float progress = Math.max(0.0F, timer / 200.0F);
                  gui.fill(x + 5, y + 43, x + 5 + (int)(140.0F * progress), y + 44, -1);
               }
            }
         }
      }
   }

   private static void renderCaptureNotifications(GuiGraphics gui, Minecraft mc, int screenWidth, int screenHeight) {
      if (!ClientData.captureNotifications.isEmpty()) {
         long now = System.currentTimeMillis();
         int flagW = 54;
         int flagH = 30;
         int x = (screenWidth - flagW) / 2;
         int y = 65;

         for (ClientData.CaptureNotification note : new ArrayList<>(ClientData.captureNotifications)) {
            long elapsed = now - note.startTime;
            long displayTime = note.duration + 2000L;
            long fadeTime = 1000L;
            if (elapsed <= displayTime + fadeTime) {
               float overallAlpha = 1.0F;
               if (elapsed > displayTime) {
                  overallAlpha = 1.0F - (float)(elapsed - displayTime) / (float)fadeTime;
               }

               overallAlpha = Mth.clamp(overallAlpha, 0.0F, 1.0F);
               float progress = Math.min(1.0F, (float)elapsed / (float)note.duration);
               int teamColor = note.team.equalsIgnoreCase("BLUE") ? -13408564 : -3394765;
               boolean showTeamFlag = note.isNeutralized ? progress < 1.0F : progress >= 1.0F;
               int bgAlpha = (int)(overallAlpha * 170.0F) << 24;
               RenderSystem.enableBlend();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, overallAlpha);
               if (showTeamFlag) {
                  String flagTeam = note.team;
                  if (note.isNeutralized) {
                     flagTeam = note.team.equalsIgnoreCase("BLUE") ? "RED" : "BLUE";
                  }

                  String faction = flagTeam.equalsIgnoreCase("BLUE") ? ClientData.BLUE_FACTION : ClientData.RED_FACTION;
                  ResourceLocation tex = getFlagTexture(faction);
                  if (tex != null) {
                     gui.blit(tex, x, y, flagW, flagH, 0.0F, 0.0F, 64, 36, 64, 36);
                  } else {
                     renderSolidWithAlpha(gui, x, y, flagW, flagH, teamColor, overallAlpha);
                  }
               } else {
                  renderSolidWithAlpha(gui, x, y, flagW, flagH, -1, overallAlpha);
               }

               renderNotificationArrows(gui, x, y, flagW, flagH, teamColor, overallAlpha);
               renderWrappingLine(gui, x, y, flagW, flagH, progress, teamColor, overallAlpha);
               String teamDisplayName = note.team.equalsIgnoreCase("BLUE") ? ClientData.customBlueName : ClientData.customRedName;
               String status = note.isNeutralized ? " neutralized " : " captured ";
               String msg = (teamDisplayName + status + note.pointName).toUpperCase();
               int textAlpha = (int)(overallAlpha * 255.0F) << 24;
               int textColor = textAlpha | 16777215;
               gui.pose().pushPose();
               gui.pose().translate(x + flagW / 2.0F, y + flagH + 8, 50.0F);
               gui.pose().scale(0.9F, 0.9F, 1.0F);
               gui.drawCenteredString(PWPTheme.Fonts.display(), msg, 0, 0, textColor);
               gui.pose().popPose();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               break;
            }

            ClientData.captureNotifications.remove(note);
         }
      }
   }

   private static void renderNotificationArrows(GuiGraphics gui, int x, int y, int w, int h, int color, float overallAlpha) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float pulse = 0.7F + (float)Math.sin(System.currentTimeMillis() / 120.0) * 0.3F;
      RenderSystem.setShaderColor(r, g, b, overallAlpha * pulse);
      int arrowSize = 12;
      int centerY = y + h / 2 - arrowSize / 2;

      for (int i = 0; i < 3; i++) {
         gui.blit(ARROW_TEX, x - 18 - i * 10, centerY, 0.0F, 0.0F, arrowSize, arrowSize, arrowSize, arrowSize);
         gui.pose().pushPose();
         int rx = x + w + 18 + i * 10;
         gui.pose().translate(rx + arrowSize / 2.0, centerY + arrowSize / 2.0, 0.0);
         gui.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
         gui.blit(ARROW_TEX, -arrowSize / 2, -arrowSize / 2, 0.0F, 0.0F, arrowSize, arrowSize, arrowSize, arrowSize);
         gui.pose().popPose();
      }
   }

   private static void renderSolidWithAlpha(GuiGraphics gui, int x, int y, int w, int h, int color, float alpha) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = (int)(alpha * 255.0F);
      gui.fill(x, y, x + w, y + h, a << 24 | r << 16 | g << 8 | b);
   }

   private static void renderWrappingLine(GuiGraphics gui, int x, int y, int w, int h, float progress, int color, float alpha) {
      float totalLen = w + 2 + h + 2 + w + 2 + h + 2;
      float cur = totalLen * progress;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = (int)(alpha * 255.0F);
      int finalColor = a << 24 | r << 16 | g << 8 | b;
      float s1 = h + 2;
      float d1 = Math.min(cur, s1);
      if (d1 > 0.0F) {
         gui.fill(x - 2, (int)(y + h + 2 - d1), x, y + h + 2, finalColor);
      }

      float s2 = w + 2;
      if (cur > s1) {
         float d2 = Math.min(cur - s1, s2);
         gui.fill(x - 2, y - 2, (int)(x - 2 + d2), y, finalColor);
      }

      float s3 = h + 2;
      if (cur > s1 + s2) {
         float d3 = Math.min(cur - s1 - s2, s3);
         gui.fill(x + w, y - 2, x + w + 2, (int)(y - 2 + d3), finalColor);
      }

      float s4 = w + 2;
      if (cur > s1 + s2 + s3) {
         float d4 = Math.min(cur - (s1 + s2 + s3), s4);
         gui.fill((int)(x + w + 2 - d4), y + h, x + w + 2, y + h + 2, finalColor);
      }
   }

   private static void renderPlacementHints(GuiGraphics gui, Minecraft mc, int width, int height) {
      if (ClientPlacementHandler.isPlacing()) {
         int uiWidth = 140;
         int uiHeight = 44;
         int xStart = (width - uiWidth) / 2;
         int yStart = height - uiHeight - 60;
         int goldLight = -10496;
         gui.fill(xStart, yStart, xStart + uiWidth, yStart + uiHeight, -1879048192);
         gui.fill(xStart, yStart, xStart + 2, yStart + uiHeight, goldLight);
         gui.drawCenteredString(PWPTheme.Fonts.display(), "Build Mode", xStart + uiWidth / 2, yStart + 4, goldLight);
         RenderSystem.enableBlend();
         int row1Y = yStart + 16;
         gui.blit(MOUSE_LEFT, xStart + 8, row1Y, 0.0F, 0.0F, 12, 12, 12, 12);
         gui.drawString(PWPTheme.Fonts.display(), "Rotate", xStart + 26, row1Y + 2, -1, true);
         gui.blit(ICON_ROTATE, xStart + uiWidth - 20, row1Y, 0.0F, 0.0F, 12, 12, 12, 12);
         int row2Y = yStart + 30;
         gui.blit(MOUSE_RIGHT, xStart + 8, row2Y, 0.0F, 0.0F, 12, 12, 12, 12);
         gui.drawString(PWPTheme.Fonts.display(), "Confirm", xStart + 26, row2Y + 2, -1, true);
         gui.blit(ICON_CONFIRM, xStart + uiWidth - 20, row2Y, 0.0F, 0.0F, 12, 12, 12, 12);
         RenderSystem.disableBlend();
      }
   }

   private static void renderDownedUI(GuiGraphics gui, Minecraft mc, int width) {
      String currentKit = mc.player.getPersistentData().getString("WARFARE_CurrentKit");
      boolean amIMedic = "Medic".equalsIgnoreCase(ClientData.myCurrentKit);
      long now = mc.level.getGameTime();
      int yOffset = 60;

      for (Integer id : ClientData.DOWNED_PLAYERS) {
         if (mc.level.getEntity(id) instanceof Player downed
            && downed != mc.player
            && mc.player.getTeam() != null
            && downed.getTeam() != null
            && mc.player.getTeam().isAlliedTo(downed.getTeam())) {
            long lastShout = downed.getPersistentData().getLong("WARFARE_LastMedicShoutTimeMS");
            boolean isShouting = now - lastShout < 3000L;
            if (amIMedic || isShouting) {
               int distance = (int)mc.player.distanceTo(downed);
               if (distance < 150) {
                  String name = downed.getScoreboardName();
                  String text = "\u271A " + name + " [" + distance + "m]";
                  int x = width - PWPTheme.Fonts.display().width(text) - 10;
                  int bgColor = isShouting ? -1426128896 : -2136342528;
                  gui.fill(x - 2, yOffset - 1, width - 5, yOffset + 9, bgColor);
                  gui.drawString(PWPTheme.Fonts.display(), text, x, yOffset, 16777215, false);
                  yOffset += 12;
               }
            }
         }
      }
   }

   private static void renderRadioSpeakers(GuiGraphics gui, Minecraft mc, int height) {
      if (!ClientData.RADIO_SPEAKERS.isEmpty()) {
         long now = System.currentTimeMillis();
         int yOffset = height / 2 - 40;
         int xOffset = 5;

         for (Entry<String, Long> entry : new ArrayList<>(ClientData.RADIO_SPEAKERS.entrySet())) {
            if (now - entry.getValue() > 1000L) {
               ClientData.RADIO_SPEAKERS.remove(entry.getKey());
            } else {
               String speakerName = entry.getKey();
               int color = -256;
               ResourceLocation icon = isCMD(speakerName) ? VOICE_ICON_STAR_TEX : VOICE_ICON_RADIO_TEX;
               int textWidth = PWPTheme.Fonts.display().width(speakerName) + 15;
               gui.fill(xOffset, yOffset - 2, xOffset + 5 + textWidth, yOffset + 10, Integer.MIN_VALUE);
               RenderSystem.enableBlend();
               float r = (color >> 16 & 0xFF) / 255.0F;
               float g = (color >> 8 & 0xFF) / 255.0F;
               float b = (color & 0xFF) / 255.0F;
               RenderSystem.setShaderColor(r, g, b, 1.0F);
               gui.blit(icon, xOffset + 3, yOffset, 0.0F, 0.0F, 8, 8, 8, 8);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               gui.drawString(PWPTheme.Fonts.display(), speakerName, xOffset + 14, yOffset, color, false);
               yOffset += 14;
            }
         }
      }
   }

   private static void renderVoiceSpeakers(GuiGraphics gui, Minecraft mc, int height) {
      if (!ClientData.SQUAD_SPEAKERS.isEmpty()) {
         long now = System.currentTimeMillis();
         int radioLines = ClientData.RADIO_SPEAKERS.size();
         int yOffset = height / 2 - 40 + radioLines * 14;
         int xOffset = 5;

         for (Entry<String, Long> entry : new ArrayList<>(ClientData.SQUAD_SPEAKERS.entrySet())) {
            if (now - entry.getValue() > 1000L) {
               ClientData.SQUAD_SPEAKERS.remove(entry.getKey());
            } else {
               String speakerName = entry.getKey();
               int color = -11141291;
               ResourceLocation icon = isCMD(speakerName) ? VOICE_ICON_STAR_TEX : VOICE_ICON_TEX;
               int textWidth = PWPTheme.Fonts.display().width(speakerName) + 15;
               gui.fill(xOffset, yOffset - 2, xOffset + 5 + textWidth, yOffset + 10, Integer.MIN_VALUE);
               RenderSystem.enableBlend();
               float r = (color >> 16 & 0xFF) / 255.0F;
               float g = (color >> 8 & 0xFF) / 255.0F;
               float b = (color & 0xFF) / 255.0F;
               RenderSystem.setShaderColor(r, g, b, 1.0F);
               gui.blit(icon, xOffset + 3, yOffset, 0.0F, 0.0F, 8, 8, 8, 8);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               gui.drawString(PWPTheme.Fonts.display(), speakerName, xOffset + 14, yOffset, color, false);
               yOffset += 14;
            }
         }
      }
   }

   private static boolean isCMD(String playerName) {
      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(playerName)) {
            int cmdid = s.team.equalsIgnoreCase("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
            return s.id == cmdid;
         }
      }
      return false;
   }

   private static void renderHubMaterials(GuiGraphics gui, Minecraft mc, int width, int height) {
      if (mc.hitResult != null) {
         int mats = -1;
         String team = "NEUTRAL";
         if (mc.hitResult.getType() == Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult)mc.hitResult;
            BlockPos pos = hit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            if (mc.level.getBlockEntity(pos) instanceof HubBlockEntity hub) {
               if (state.hasProperty(HubBlock.CONSTRUCTED) && !(Boolean)state.getValue(HubBlock.CONSTRUCTED)) {
                  return;
               }

               mats = hub.getMaterials();
               team = hub.getTeam();
            }
         } else if (mc.hitResult.getType() == Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult)mc.hitResult;
            if (hit.getEntity() instanceof SupplyCrateEntity crate) {
               mats = crate.getMaterials();
               team = crate.getTeamOwner();
            }
         }

         if (mats != -1) {
            int teamColor = -1;
            if (team.equalsIgnoreCase("BLUE")) {
               teamColor = -11184641;
            } else if (team.equalsIgnoreCase("RED")) {
               teamColor = -43691;
            }

            String text = "Materials: " + mats;
            int textWidth = PWPTheme.Fonts.display().width(text);
            int textX = (width - textWidth) / 2;
            int textY = height - 70;
            PoseStack pose = gui.pose();
            pose.pushPose();
            pose.translate(width / 2.0F, textY - 12, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
            gui.fill(-5, -5, 5, 5, -16777216);
            gui.fill(-4, -4, 4, 4, teamColor);
            pose.popPose();
            drawOutlinedString(gui, mc, text, textX, textY, -22016);
         }
      }
   }

   private static void renderBuildProgress(GuiGraphics gui, Minecraft mc, int width, int height) {
      ItemStack mainHand = mc.player.getMainHandItem();
      ItemStack offHand = mc.player.getOffhandItem();
      boolean holdingTool = mainHand.getItem() == ModItems.ENTRENCHING_TOOL.get() || offHand.getItem() == ModItems.ENTRENCHING_TOOL.get();
      if (holdingTool) {
         if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)mc.hitResult;
            BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            if (be != null) {
               float progress = -1.0F;
               String structureTeam = "NEUTRAL";
               boolean finished = false;
               if (be instanceof HubBlockEntity hub) {
                  progress = hub.getPercentage();
                  structureTeam = hub.getTeam();
                  finished = (Boolean)state.getValue(HubBlock.CONSTRUCTED);
               } else if (be instanceof WallBlockEntity wall) {
                  progress = wall.getPercentage();
                  structureTeam = wall.getTeam();
                  finished = (Boolean)state.getValue(WallBlock.CONSTRUCTED);
               } else if (be instanceof BarbedWireBlockEntity wire) {
                  progress = wire.getPercentage();
                  structureTeam = wire.getTeam();
                  finished = (Boolean)state.getValue(BarbedWireBlock.CONSTRUCTED);
               } else if (be instanceof M2ConstructionBlockEntity m2) {
                  progress = m2.getPercentage();
                  structureTeam = m2.getTeam();
               } else if (be instanceof AGSConstructionBlockEntity ags) {
                  progress = ags.getPercentage();
                  structureTeam = ags.getTeam();
               } else if (be instanceof MortarConstructionBlockEntity mortar) {
                  progress = mortar.getPercentage();
                  structureTeam = mortar.getTeam();
               } else if (be instanceof TOWConstructionBlockEntity tow) {
                  progress = tow.getPercentage();
                  structureTeam = tow.getTeam();
               }

                 if (!(progress < 0.0F)) {
                    String playerTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName() : "NEUTRAL";
                    boolean isEnemy = !structureTeam.equals("NEUTRAL") && !structureTeam.equalsIgnoreCase(playerTeam) && !mc.player.isCreative();
                    boolean dismantling = false;
                    if (be instanceof WallBlockEntity w) {
                       dismantling = w.isDismantling();
                    } else if (be instanceof BarbedWireBlockEntity w) {
                       dismantling = w.isDismantling();
                    } else if (be instanceof HubBlockEntity h) {
                       dismantling = h.isDismantling();
                    }

                    boolean showDestroy = finished || dismantling;
                    int uiWidth = 120;
                    int uiHeight = (!showDestroy) ? 54 : 65;
                    int xStart = width / 2 - uiWidth / 2;
                    int yStart = height - 110;
                    int goldLight = -10496;
                    int goldDark = -4026112;
                    gui.fill(xStart, yStart, xStart + uiWidth, yStart + uiHeight, -1879048192);
                    gui.fill(xStart, yStart, xStart + 2, yStart + uiHeight, goldLight);
                    RenderSystem.enableBlend();
                    if (showDestroy) {
                       gui.blit(DIG_ICON, xStart + 8, yStart + 8, 0.0F, 0.0F, 12, 12, 12, 12);
                       gui.drawString(PWPTheme.Fonts.display(), "Destroy", xStart + 26, yStart + 10, -1, true);
                    } else {
                       gui.blit(BUILD_ICON, xStart + 8, yStart + 8, 0.0F, 0.0F, 12, 12, 12, 12);
                       gui.drawString(PWPTheme.Fonts.display(), "Build", xStart + 26, yStart + 10, -1, true);
                    }

                    int barY = yStart + (showDestroy ? 28 : 24);
                    int barWidth = 85;
                    gui.blit(SHOVEL_ICON, xStart + 8, yStart + (showDestroy ? 25 : 21), 0.0F, 0.0F, 12, 12, 12, 12);
                    gui.fill(xStart + 26, barY, xStart + 26 + barWidth, barY + 5, 1090519039);
                    int currentBarWidth = (int)(barWidth * progress);
                    if (currentBarWidth > 0) {
                       gui.fillGradient(xStart + 26, barY, xStart + 26 + currentBarWidth, barY + 5, goldDark, goldLight);
                    }

                    if (dismantling) {
                       gui.drawCenteredString(PWPTheme.Fonts.display(), isEnemy ? "Destroying..." : "Dismantling...", xStart + uiWidth / 2, yStart + 52, isEnemy ? -43691 : -256);
                    } else if (finished) {
                       gui.drawCenteredString(PWPTheme.Fonts.display(), isEnemy ? "Enemy structure" : "Structure finished", xStart + uiWidth / 2, yStart + 52, isEnemy ? -43691 : -256);
                    } else if (isEnemy) {
                       gui.drawCenteredString(PWPTheme.Fonts.display(), "Enemy structure", xStart + uiWidth / 2, yStart + 52, -43691);
                    }

                    RenderSystem.disableBlend();
                 }
            }
         }
      }
   }

   private static void renderTickets(GuiGraphics gui, Minecraft mc, int width) {
      if (mc.player.isCreative() || mc.player.isSpectator()) {
         int boxWidth = 32;
         int boxHeight = 18;
         int gap = 6;
         int topOffset = 5;
         int centerX = width / 2;
         boolean blinkOn = System.currentTimeMillis() / 500L % 2L == 0L;
         int normalWhite = -1;
         int alarmRed = -43691;
         int blueX = centerX - boxWidth - gap / 2;
         ResourceLocation blueFlag = getFlagTexture(ClientData.BLUE_FACTION);
         gui.fill(blueX - 1, topOffset - 1, blueX + boxWidth + 1, topOffset + boxHeight + 1, -16777216);
         if (blueFlag != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            gui.blit(blueFlag, blueX, topOffset, boxWidth, boxHeight, 0.0F, 0.0F, boxWidth, boxHeight, boxWidth, boxHeight);
         } else {
            gui.fill(blueX, topOffset, blueX + boxWidth, topOffset + boxHeight, -869046580);
         }

         String blueText = String.valueOf(ClientData.BLUE_TICKETS);
         int blueTextColor = ClientData.blueBleeding ? (blinkOn ? alarmRed : normalWhite) : normalWhite;
         int blueTextX = blueX + (boxWidth - PWPTheme.Fonts.display().width(blueText)) / 2;
         int blueTextY = topOffset + (boxHeight - 8) / 2;
         drawOutlinedString(gui, mc, blueText, blueTextX, blueTextY, blueTextColor);
         int redX = centerX + gap / 2;
         ResourceLocation redFlag = getFlagTexture(ClientData.RED_FACTION);
         gui.fill(redX - 1, topOffset - 1, redX + boxWidth + 1, topOffset + boxHeight + 1, -16777216);
         if (redFlag != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            gui.blit(redFlag, redX, topOffset, boxWidth, boxHeight, 0.0F, 0.0F, boxWidth, boxHeight, boxWidth, boxHeight);
         } else {
            gui.fill(redX, topOffset, redX + boxWidth, topOffset + boxHeight, -859032781);
         }

          String redText = String.valueOf(ClientData.RED_TICKETS);
          int redTextColor = ClientData.redBleeding ? (blinkOn ? alarmRed : normalWhite) : normalWhite;
          int redTextX = redX + (boxWidth - PWPTheme.Fonts.display().width(redText)) / 2;
          int redTextY = topOffset + (boxHeight - 8) / 2;
          drawOutlinedString(gui, mc, redText, redTextX, redTextY, redTextColor);
       }
    }

   private static void drawOutlinedString(GuiGraphics gui, Minecraft mc, String text, int x, int y, int color) {
      int black = -16777216;
      gui.drawString(PWPTheme.Fonts.display(), text, x - 1, y, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x + 1, y, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x, y - 1, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x, y + 1, black, false);
      gui.drawString(PWPTheme.Fonts.display(), text, x, y, color, false);
   }

   private static void renderCapturePoint(GuiGraphics gui, Minecraft mc, int width, int height) {
      if (ClientData.isInsidePoint) {
         int flagW = 48;
         int flagH = 27;
         int xStart = width - flagW - 15;
         int yStart = 20;
         int teamColor = -1;
         String faction = "none";
         if (ClientData.pointOwner.equals("BLUE")) {
            teamColor = -13408564;
            faction = ClientData.BLUE_FACTION;
         } else if (ClientData.pointOwner.equals("RED")) {
            teamColor = -3394765;
            faction = ClientData.RED_FACTION;
         }

         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         gui.pose().pushPose();
         gui.pose().scale(0.8F, 0.8F, 1.0F);
         int scaledX = (int)((xStart + flagW / 2) / 0.8F);
         int scaledY = (int)((yStart - 9) / 0.8F);
         gui.drawCenteredString(PWPTheme.Fonts.display(), ClientData.pointName, scaledX, scaledY, -1);
         gui.pose().popPose();
         int barStartX = xStart - 1;
         gui.fill(barStartX, yStart - 1, xStart + flagW + 1, yStart + flagH + 1, -16777216);
         ResourceLocation flagTexture = getFlagTexture(faction);
         if (flagTexture != null && !faction.equals("none")) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.blit(flagTexture, xStart, yStart, flagW, flagH, 0.0F, 0.0F, 64, 36, 64, 36);
         } else {
            gui.fill(xStart, yStart, xStart + flagW, yStart + flagH, teamColor);
         }

         int barsY = yStart + flagH + 6;
         int segH = 4;
         int gap = 1;
         int[] segmentWidths = new int[]{12, 12, 12, 11};
         int currentX = barStartX;

         for (int i = 0; i < 4; i++) {
            int sW = segmentWidths[i];
            gui.fill(currentX, barsY, currentX + sW, barsY + segH, -1876811230);
            float threshold = i * 0.25F;
            if (ClientData.pointProgress > threshold) {
               float boxFill = Math.min(1.0F, (ClientData.pointProgress - threshold) / 0.25F);
               int fillW = (int)(sW * boxFill);
               int fillColor = teamColor;
               if (ClientData.pointOwner.equals("NEUTRAL")) {
                  if (ClientData.pointCapturingTeam.equals("BLUE")) {
                     fillColor = -13408564;
                  } else if (ClientData.pointCapturingTeam.equals("RED")) {
                     fillColor = -3394765;
                  }
               }

               gui.fill(currentX, barsY, currentX + fillW, barsY + segH, fillColor);
            }

            currentX += sW + gap;
         }

         int rate = ClientData.pointCaptureRate;
         if (rate != 0) {
            int absRate = Math.min(Math.abs(rate), 4);
            boolean isForward = rate > 0;
            String capTeam = ClientData.pointCapturingTeam;
            int arrowCol = -1;
            if (capTeam.equalsIgnoreCase("BLUE")) {
               arrowCol = -13408564;
            } else if (capTeam.equalsIgnoreCase("RED")) {
               arrowCol = -3394765;
            }

            float ar = (arrowCol >> 16 & 0xFF) / 255.0F;
            float ag = (arrowCol >> 8 & 0xFF) / 255.0F;
            float ab = (arrowCol & 0xFF) / 255.0F;
            double baseX = isForward ? barStartX + 5 : barStartX + 45;
            double arrowY = barsY + segH / 2.0;

            for (int j = 0; j < absRate; j++) {
               gui.pose().pushPose();
               double offsetX = isForward ? j * 6 : -j * 6;
               gui.pose().translate(baseX + offsetX, arrowY, 10.0);
               gui.pose().scale(1.05F, 1.05F, 1.0F);
               if (!isForward) {
                  gui.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
               }

               RenderSystem.setShaderColor(ar, ag, ab, 1.0F);
               gui.blit(ARROW_TEX, -6, -6, 0.0F, 0.0F, 12, 12, 12, 12);
               gui.pose().popPose();
            }
         }

         if (ClientData.isLocked) {
            gui.pose().pushPose();
            gui.pose().scale(0.7F, 0.7F, 1.0F);
            int lockX = (int)(barStartX / 0.71F);
            int lockY = (int)((barsY + 10) / 0.7F);
            gui.drawString(PWPTheme.Fonts.display(), "BLOCKED", lockX, lockY, -43691, true);
            if (!ClientData.nextObjectiveName.isEmpty()) {
               gui.drawString(PWPTheme.Fonts.display(), "Need: " + ClientData.nextObjectiveName, lockX, lockY + 10, -3355444, true);
            }

            gui.pose().popPose();
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static void renderVotePanel(GuiGraphics gui, Minecraft mc, int width) {
      float speed = 0.05F;
      ClientData.voteTransition = Mth.lerp(speed, ClientData.voteTransition, ClientData.voteActive ? 1.0F : 0.0F);
      if (!(ClientData.voteTransition <= 0.001F)) {
         int xPos = (int)Mth.lerp(ClientData.voteTransition, -180.0F, 10.0F);
         int yPos = 60;
         String myTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
         List<MapPlayerInfo> teamPlayers = ClientData.mapPlayers.values().stream().filter(infox -> infox.team.equalsIgnoreCase(myTeam)).toList();
         int panelWidth = 165;
         int rowHeight = 12;
         int headerHeight = 32;
         int statusHeight = 28;
         int footerHeight = 15;
         int panelHeight = headerHeight + statusHeight + teamPlayers.size() * rowHeight + footerHeight;
         gui.pose().pushPose();
         gui.pose().translate(0.0F, 0.0F, 500.0F);
         gui.fill(xPos, yPos, xPos + panelWidth, yPos + panelHeight, -1442840576);
         gui.renderOutline(xPos, yPos, panelWidth, panelHeight, -1);
         gui.drawCenteredString(PWPTheme.Fonts.display(), "VOTE TO START", xPos + panelWidth / 2, yPos + 5, -10496);
         int seconds = Math.max(0, ClientData.voteTimer);
         String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
         gui.drawCenteredString(PWPTheme.Fonts.display(), timeStr, xPos + panelWidth / 2, yPos + 16, -1);
         gui.fill(xPos + 5, yPos + 28, xPos + panelWidth - 5, yPos + 29, 1442840575);
         String blueName = ClientData.customBlueName;
         String redName = ClientData.customRedName;
         int blueColor = ClientData.blueReady ? -11141291 : -43691;
         int redColor = ClientData.redReady ? -11141291 : -43691;
         gui.drawString(PWPTheme.Fonts.display(), blueName + ": " + (ClientData.blueReady ? "READY" : "WAITING"), xPos + 8, yPos + 32, blueColor, true);
         gui.drawString(PWPTheme.Fonts.display(), redName + ": " + (ClientData.redReady ? "READY" : "WAITING"), xPos + 8, yPos + 44, redColor, true);
         int currentY = yPos + headerHeight + statusHeight;

         for (MapPlayerInfo info : teamPlayers) {
            Boolean vote = ClientData.votes.get(info.uuid);
            String icon = "\u25CB";
            int iconColor = -5592406;
            if (vote != null) {
               icon = vote ? "\u2714" : "\u2718";
               iconColor = vote ? -11141291 : -43691;
            }

            gui.drawString(PWPTheme.Fonts.display(), icon, xPos + 8, currentY, iconColor, true);
            int nameColor = info.name.equals(mc.player.getScoreboardName()) ? -171 : -1;
            gui.drawString(PWPTheme.Fonts.display(), info.name, xPos + 22, currentY, nameColor, true);
            currentY += rowHeight;
         }

         gui.drawCenteredString(PWPTheme.Fonts.display(), "F9: YES | F10: NO", xPos + panelWidth / 2, yPos + panelHeight - 12, -4473925);
         gui.pose().popPose();
      }
   }

   private static void renderProgressBars(GuiGraphics gui, Minecraft mc, int centerX, int y, int teamColor) {
      int barsTotalWidth = 80;
      int barHeight = 4;
      int barsStartX = centerX - barsTotalWidth / 2;
      int barGap = 2;
      int singleBarWidth = (barsTotalWidth - barGap * 3) / 4;

      for (int i = 0; i < 4; i++) {
         int currentBarX = barsStartX + i * (singleBarWidth + barGap);
         gui.fill(currentBarX, y, currentBarX + singleBarWidth, y + barHeight, -12303292);
         float threshold = i * 0.25F;
         if (ClientData.pointProgress > threshold) {
            float fillAmount = Math.min(1.0F, (ClientData.pointProgress - threshold) / 0.25F);
            int fillWidth = (int)(singleBarWidth * fillAmount);
            int finalBarColor = teamColor;
            if (ClientData.pointOwner.equals("NEUTRAL")) {
               if (ClientData.pointCapturingTeam.equals("BLUE")) {
                  finalBarColor = -13408564;
               } else if (ClientData.pointCapturingTeam.equals("RED")) {
                  finalBarColor = -3394765;
               }
            }

            gui.fill(currentBarX, y, currentBarX + fillWidth, y + barHeight, finalBarColor);
         }
      }
   }

   private static void renderCaptureArrows(GuiGraphics gui, Minecraft mc, int centerX, int y) {
      int rate = ClientData.pointCaptureRate;
      if (rate != 0) {
         int absRate = Math.min(Math.abs(rate), 4);
         boolean isForward = rate > 0;
         String capTeam = ClientData.pointCapturingTeam;
         int color = -1;
         if (capTeam.equalsIgnoreCase("BLUE")) {
            color = -13408564;
         } else if (capTeam.equalsIgnoreCase("RED")) {
            color = -3394765;
         }

         RenderSystem.enableBlend();
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         RenderSystem.setShaderColor(r, g, b, 1.0F);
         int arrowSize = 12;
         int barsStartX = centerX - 40;
         int segmentWidth = 20;

         for (int i = 0; i < absRate; i++) {
            int segmentIndex = isForward ? i : 3 - i;
            int xPos = barsStartX + segmentIndex * segmentWidth + 2;
            int yPos = y - 4;
            if (isForward) {
               gui.blit(ARROW_TEX, xPos, yPos, 0.0F, 0.0F, arrowSize, arrowSize, arrowSize, arrowSize);
            } else {
               gui.pose().pushPose();
               gui.pose().translate(xPos + arrowSize / 2.0, yPos + arrowSize / 2.0, 0.0);
               gui.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
               gui.blit(ARROW_TEX, -arrowSize / 2, -arrowSize / 2, 0.0F, 0.0F, arrowSize, arrowSize, arrowSize, arrowSize);
               gui.pose().popPose();
            }
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static void renderSupplyTruckInfo(GuiGraphics gui, Minecraft mc, int width, int height) {
      Entity ridingEntity = mc.player.getVehicle();
      if (ridingEntity != null) {
         Entity supplyTruck = getSupplyTruckEntity(ridingEntity);
         if (supplyTruck != null) {
            int crates = supplyTruck.getPersistentData().getInt("WARFARE_SupplyAmmo");
            int maxCrates = (Integer)WarfareConfig.SUPPLY_TRUCK_CRATES.get();
            boolean isCharging = false;
            if (crates < maxCrates) {
               BlockPos vPos = supplyTruck.blockPosition();

               for (BlockPos pos : BlockPos.betweenClosed(vPos.offset(-5, -2, -5), vPos.offset(5, 2, 5))) {
                  if (mc.level.getBlockState(pos).getBlock() instanceof MainSupplyBlock) {
                     isCharging = true;
                     break;
                  }
               }
            }

            int color = -1;
            if (isCharging) {
               if (System.currentTimeMillis() / 250L % 2L == 0L) {
                  color = -16711936;
               } else {
                  color = -256;
               }
            } else if (crates == 0) {
               color = -43691;
            }

            String text = "Supplies: " + crates + " / " + maxCrates;
            int textWidth = PWPTheme.Fonts.display().width(text);
            int x = width - textWidth - 10;
            int y = height - 25;
            drawOutlinedString(gui, mc, text, x, y, color);
            if (isCharging) {
               String reloadText = "RELOADING...";
               drawOutlinedString(gui, mc, reloadText, width - PWPTheme.Fonts.display().width(reloadText) - 10, y - 10, -11141291);
            }
         }
      }
   }

   private static Entity getSupplyTruckEntity(Entity entity) {
      if (entity.getPersistentData().getBoolean("WARFARE_IsSupplyTruck")) {
         return entity;
      }

      Entity parent = entity.getVehicle();
      return parent != null && parent.getPersistentData().getBoolean("WARFARE_IsSupplyTruck") ? parent : null;
   }

   private static void renderVehicleAmmo(GuiGraphics gui, Minecraft mc, int width, int height) {
      Entity vehicle = mc.player.getVehicle();
      int currentAmmo = -1;
      int maxAmmo = -1;
      if (vehicle instanceof M2BrowningEntity m2) {
         currentAmmo = m2.getAmmoCount();
         maxAmmo = 200;
      } else if (vehicle instanceof AGS30Entity ags) {
         currentAmmo = ags.getAmmoCount();
         maxAmmo = 30;
      }

      if (currentAmmo != -1) {
         String text = "Ammo: " + currentAmmo + " / " + maxAmmo;
         int x = 10;
         int y = height - 40;
         int color = currentAmmo == 0 ? -43691 : -1;
         drawOutlinedString(gui, mc, text, x, y, color);
      }
   }

   private static void renderMedicUI(GuiGraphics gui, Minecraft mc, int width) {
      int yOffset = 60;

      for (Integer id : ClientData.DOWNED_PLAYERS) {
         if (mc.level.getEntity(id) instanceof Player downed && downed != mc.player) {
            int distance = (int)mc.player.distanceTo(downed);
            if (distance < 100) {
               String name = downed.getScoreboardName();
               String text = "\u271A " + name + " [" + distance + "m]";
               int x = width - PWPTheme.Fonts.display().width(text) - 10;
               gui.fill(x - 2, yOffset - 1, width - 5, yOffset + 9, -2130771968);
               gui.drawString(PWPTheme.Fonts.display(), text, x, yOffset, 16777215, false);
               yOffset += 12;
            }
         }
      }
   }

   private static void renderSideMap(GuiGraphics gui, Minecraft mc, int screenWidth, int screenHeight, float partialTick) {
      if (HUD_SIDE_MAP == null) {
          HUD_SIDE_MAP = new SquadMapRenderer();
      }

      float speed = 0.12F;
      float target = ClientData.isMapOpen ? 1.0F : 0.0F;
      ClientData.mapTransition = Mth.lerp(speed, ClientData.mapTransition, target);
      if (!ClientData.isMapOpen && ClientData.mapTransition < 0.001F) {
         ClientData.mapTransition = 0.0F;
      } else {
         int mapSize = (int)((screenHeight - 60) / 1.3F);
         int topBarHeight = 35;
         int sidePadding = 15;
         int bottomPadding = 5;
         int containerWidth = mapSize + sidePadding * 2;
         int containerHeight = mapSize + topBarHeight + bottomPadding;
         float hiddenX = screenWidth + 20.0F;
         float visibleX = (float)screenWidth - containerWidth - 10.0F;
         int xPos = (int)Mth.lerp(ClientData.mapTransition, hiddenX, visibleX);
         int yPos = (screenHeight - containerHeight) / 2;
         gui.fill(xPos, yPos, xPos + containerWidth, yPos + containerHeight, -1442840576);
         renderMinimapStatus(gui, mc, xPos, yPos, containerWidth, topBarHeight);
         HUD_SIDE_MAP.init(xPos + sidePadding, yPos + topBarHeight, mapSize);
         HUD_SIDE_MAP.render(gui, -1, -1, partialTick);
      }
   }

   private static void renderMinimapStatus(GuiGraphics gui, Minecraft mc, int x, int y, int containerWidth, int topBarHeight) {
      String team = "NEUTRAL";
      if (mc.player.getTeam() != null) {
         team = mc.player.getTeam().getName().toUpperCase();
      }

      int tickets = team.equals("BLUE") ? ClientData.BLUE_TICKETS : (team.equals("RED") ? ClientData.RED_TICKETS : 0);
      String faction = team.equals("BLUE") ? ClientData.BLUE_FACTION : (team.equals("RED") ? ClientData.RED_FACTION : "none");
      String tText = String.valueOf(tickets);
      int flagW = 22;
      int flagH = 13;
      int iconSize = 12;
      int textW = PWPTheme.Fonts.display().width(tText);
      int gap = 6;
      int totalContentWidth = flagW + gap + iconSize + gap + textW;
      int startX = x + containerWidth / 2 - totalContentWidth / 2;
      int contentY = y + topBarHeight / 2 - flagH / 2;
      ResourceLocation flagTex = getFlagTexture(faction);
      if (flagTex != null) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.blit(flagTex, startX, contentY, flagW, flagH, 0.0F, 0.0F, 64, 36, 64, 36);
      }

      ResourceLocation ticketIcon = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");
      int iconX = startX + flagW + gap;
      RenderSystem.enableBlend();
      gui.blit(ticketIcon, iconX, contentY, iconSize, iconSize, 0.0F, 0.0F, 16, 16, 16, 16);
      int textX = iconX + iconSize + gap;
      gui.drawString(PWPTheme.Fonts.display(), tText, textX, contentY + 2, -1, true);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static ResourceLocation getFlagTexture(String faction) {
      if (faction == null) {
         return null;
      }

      switch (faction.toLowerCase()) {
         case "ukraine":
            return FLAG_UKRAINE;
         case "russia":
            return FLAG_RUSSIA;
         case "usa":
            return FLAG_USA;
         case "nato":
            return FLAG_NATO;
         case "bluefor":
            return FLAG_BLUEFOR;
         case "redfor":
            return FLAG_REDFOR;
         case "insurgency":
            return FLAG_INSURGENCY;
         case "pmc":
            return FLAG_PMC;
         default:
            return null;
      }
   }
}

package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSquadMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.pwp.coreclient.gui.theme.PWPTheme;

// Р Р°РґРёР°Р»СЊРЅРѕРµ РјРµРЅСЋ С‚Р°РєС‚РёС‡РµСЃРєРѕР№ РєР°СЂС‚С‹
// Р’С‹Р±РѕСЂ С‚РёРїР° РјР°СЂРєРµСЂР°: РєРѕРјР°РЅРґРЅС‹Р№, РІСЂР°Р¶РµСЃРєРёР№ РёР»Рё РѕС‚СЂСЏРґРЅС‹Р№
public class TacticalMapRadialScreen extends Screen {
   private final int wx;
   private final int wz;
   private final Screen previousScreen;
   private static final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/radial_sector.png");

   public TacticalMapRadialScreen(int x, int z, Screen previousScreen) {
      super(Component.translatable("gui.pwpwarfare.radial.tactical"));
      this.wx = x;
      this.wz = z;
      this.previousScreen = previousScreen;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
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

      for (int i = 0; i < 3; i++) {
         pose.pushPose();
         pose.translate(centerX, centerY, 0.0F);
         pose.mulPose(Axis.ZP.rotationDegrees(i * 120));
         boolean isSelected = i == selected;
         float scale = isSelected ? 1.15F : 1.0F;
         pose.scale(scale, scale, 1.0F);
         pose.translate(-size / 2.0F, -size, 0.0F);
         if (isSelected) {
            RenderSystem.setShaderColor(0.4F, 1.0F, 0.4F, 1.0F);
         } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gui.blit(SECTOR_TEXTURE, 0, 0, 0.0F, 0.0F, size, size, size, size);
         pose.popPose();
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.drawLabel(gui, "TEAM", centerX, centerY - 70, selected == 0);
      this.drawLabel(gui, "ENEMY", centerX + 60, centerY + 30, selected == 1);
      this.drawLabel(gui, "SQUAD", centerX - 60, centerY + 30, selected == 2);
      int cx = this.width / 2;
      int cy = this.height / 2;
      boolean hoverCenter = distance < 20.0;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (hoverCenter) {
         RenderSystem.setShaderColor(0.8F, 0.8F, 0.8F, 1.0F);
      }

      gui.blit(new ResourceLocation("pwpwarfare", "textures/gui/map_icons/player_circle.png"), cx - 12, cy - 12, 0.0F, 0.0F, 24, 24, 24, 24);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected) {
      int color = selected ? -16711936 : -1;
      gui.drawCenteredString(PWPTheme.Fonts.display(), text, x, y - 4, color);
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (btn == 0) {
         int centerX = this.width / 2;
         int centerY = this.height / 2;
         double dx = mx - centerX;
         double dy = my - centerY;
         double dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < 20.0) {
             PacketHandler.INSTANCE.sendToServer(new PacketSquadMarker(this.wx, this.wz, 6));
             this.minecraft.setScreen(this.previousScreen);
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
            this.openGrid("gui.pwpwarfare.radial.team_markers", this.getTeamMarkers());
         } else if (action == 1) {
            this.openGrid("gui.pwpwarfare.radial.enemy_markers", this.getEnemyMarkers());
          } else if (action == 2) {
             this.minecraft.setScreen(new SquadMarkerRadialScreen(this.wx, this.wz, this.previousScreen));
          }

         return true;
      } else {
         return super.mouseClicked(mx, my, btn);
      }
   }

   private void openGrid(String title, Map<String, ResourceLocation> markers) {
      this.minecraft.setScreen(new MapMarkerGridScreen(this.wx, this.wz, title, markers, this.previousScreen));
   }

   private Map<String, ResourceLocation> getTeamMarkers() {
      Map<String, ResourceLocation> m = new LinkedHashMap<>();
      m.put("Supply Request", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_request_marker.png"));
      m.put("Artillery Request", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/artillery_request_marker.png"));
      return m;
   }

   private Map<String, ResourceLocation> getEnemyMarkers() {
      Map<String, ResourceLocation> m = new LinkedHashMap<>();
      m.put("Infantry", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/infantry_marker.png"));
      m.put("Sniper", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/sniper_marker.png"));
      m.put("HAT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hat_marker.png"));
      m.put("Mortar", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/mortar_marker.png"));
      m.put("TOW", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/tow_marker.png"));
      m.put("Enemy HUB", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_marker.png"));
      m.put("Enemy Rally", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_marker.png"));
      m.put("Combat Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/combat_vehicle_marker.png"));
      m.put("Infantry Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/infantry_vehicle_marker.png"));
      m.put("APC", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/apc_marker.png"));
      m.put("Tank", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/tank_marker.png"));
      m.put("Helicopter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/helicopter_marker.png"));
      m.put("CAS Heli", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_helicopter_marker.png"));
      m.put("CAS Fighter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_fighter_marker.png"));
      m.put("Mobile ZU", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/mobile_zu_marker.png"));
      m.put("Supply Truck", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_truck_marker.png"));
      return m;
   }
}

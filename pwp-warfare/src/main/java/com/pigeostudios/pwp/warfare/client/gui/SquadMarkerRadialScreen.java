package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSquadMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Радиальное меню выбора типа маркера отряда
// Атака, защита, стройка, перемещение — для командиров отделений
public class SquadMarkerRadialScreen extends Screen {
   private static final ResourceLocation SECTOR_3 = new ResourceLocation("pwpwarfare", "textures/gui/radial_sector.png");
   private static final ResourceLocation SECTOR_4 = new ResourceLocation("pwpwarfare", "textures/gui/radial_sector_4.png");
   private final int targetX;
   private final int targetZ;
   private final Screen previousScreen;
   private int currentLayer = 1;

   public SquadMarkerRadialScreen(int x, int z, Screen previousScreen) {
      super(Component.translatable("gui.pwpwarfare.radial.markers"));
      this.targetX = x;
      this.targetZ = z;
      this.previousScreen = previousScreen;
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
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
         if (angle > 300.0 || angle <= 60.0) {
            selected = 0;
         } else if (angle > 60.0 && angle <= 180.0) {
            selected = 1;
         } else {
            selected = 2;
         }
      }

      for (int i = 0; i < 3; i++) {
         gui.pose().pushPose();
         gui.pose().translate(cx, cy, 0.0F);
         gui.pose().mulPose(Axis.ZP.rotationDegrees(i * 120));
         if (i == selected) {
            RenderSystem.setShaderColor(0.4F, 1.0F, 0.4F, 1.0F);
         } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gui.pose().translate(-47.5, -95.0, 0.0);
         gui.blit(SECTOR_3, 0, 0, 0.0F, 0.0F, 95, 95, 95, 95);
         gui.pose().popPose();
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
         if (angle >= 45.0 && angle < 135.0) {
            selected = 1;
         } else if (angle >= 135.0 && angle < 225.0) {
            selected = 2;
         } else if (angle >= 225.0 && angle < 315.0) {
            selected = 3;
         } else {
            selected = 0;
         }
      }

      for (int i = 0; i < 4; i++) {
         gui.pose().pushPose();
         gui.pose().translate(cx, cy, 0.0F);
         float rot = i == 0 ? 90.0F : (i == 1 ? 180.0F : (i == 2 ? -90.0F : 0.0F));
         gui.pose().mulPose(Axis.ZP.rotationDegrees(rot));
         if (i == selected) {
            RenderSystem.setShaderColor(0.4F, 1.0F, 0.4F, 1.0F);
         } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gui.pose().translate(-47.5, -95.0, 0.0);
         gui.blit(SECTOR_4, 0, 0, 0.0F, 0.0F, 95, 95, 95, 95);
         gui.pose().popPose();
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
      double dx = mx - cx;
      double dy = my - cy;
      double dist = Math.sqrt(dx * dx + dy * dy);
      if (button == 0 && dist > 10.0) {
         if (this.currentLayer == 0) {
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
            if (angle < 0.0) {
               angle += 360.0;
            }

            int sel = angle > 300.0 || angle <= 60.0 ? 0 : (angle > 60.0 && angle <= 180.0 ? 1 : 2);
            if (sel == 2) {
               this.currentLayer = 1;
               return true;
            }

            PacketHandler.INSTANCE.sendToServer(new PacketSquadMarker(this.targetX, this.targetZ, sel + 4));
             this.minecraft.setScreen(this.previousScreen);
          } else {
             double angle = Math.toDegrees(Math.atan2(dy, dx));
             if (angle < 0.0) {
                angle += 360.0;
             }

             int type = angle >= 45.0 && angle < 135.0 ? 1 : (angle >= 135.0 && angle < 225.0 ? 2 : (angle >= 225.0 && angle < 315.0 ? 3 : 0));
             PacketHandler.INSTANCE.sendToServer(new PacketSquadMarker(this.targetX, this.targetZ, type));
             this.minecraft.setScreen(this.previousScreen);
          }

         return true;
      } else {
         return super.mouseClicked(mx, my, button);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}

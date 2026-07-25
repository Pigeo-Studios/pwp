package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestCrateAmmo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.pwp.coreclient.gui.theme.PWPTheme;

// Р Р°РґРёР°Р»СЊРЅРѕРµ РјРµРЅСЋ РґР»СЏ РІР·Р°РёРјРѕРґРµР№СЃС‚РІРёСЏ СЃ СЏС‰РёРєРѕРј СЃРЅР°Р±Р¶РµРЅРёСЏ
// РџРѕР·РІРѕР»СЏРµС‚ РїРѕРїРѕР»РЅРёС‚СЊ Р±РѕРµРїСЂРёРїР°СЃС‹ РёР»Рё Р·Р°Р±СЂР°С‚СЊ РјР°С‚РµСЂРёР°Р»С‹
public class CrateRadialScreen extends Screen {
   private static final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/radial_sector_5.png");
   private final int entityId;
   private int cdAmmo = 0;
   private int materials = 0;

   public CrateRadialScreen(int entityId) {
      super(Component.translatable("gui.pwpwarfare.radial.crate_supply"));
      this.entityId = entityId;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void updateData() {
      long elapsed = System.currentTimeMillis() - ClientData.lastFobResupplyTime;
      if (elapsed < 60000L && !Minecraft.getInstance().player.isCreative()) {
         this.cdAmmo = (int)((60000L - elapsed) / 50L);
      } else {
         this.cdAmmo = 0;
      }

      if (Minecraft.getInstance().level != null) {
         if (Minecraft.getInstance().level.getEntity(this.entityId) instanceof SupplyCrateEntity crate) {
            this.materials = crate.getMaterials();
         } else {
            this.onClose();
         }
      }
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
      this.updateData();
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      String matText = "Mats: " + this.materials + " / " + WarfareConfig.SUPPLY_CRATE_MATERIALS.get();
      gui.drawCenteredString(PWPTheme.Fonts.display(), matText, centerX, centerY + 5, -22016);
      double dx = mouseX - centerX;
      double dy = mouseY - centerY;
      double distance = Math.sqrt(dx * dx + dy * dy);
      int selected = -1;
      if (distance > 10.0) {
         double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
         if (angle < 0.0) {
            angle += 360.0;
         }

         double shiftedAngle = angle + 36.0;
         if (shiftedAngle >= 360.0) {
            shiftedAngle -= 360.0;
         }

         selected = (int)(shiftedAngle / 72.0);
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      PoseStack pose = gui.pose();
      int size = 95;

      for (int i = 0; i < 5; i++) {
         pose.pushPose();
         pose.translate(centerX, centerY, 0.0F);
         pose.mulPose(Axis.ZP.rotationDegrees(i * 72));
         boolean isSelected = i == selected;
         float scale = isSelected ? 1.15F : 1.0F;
         pose.scale(scale, scale, 1.0F);
         pose.translate(-size / 2.0F, -size, 0.0F);
         float r = 1.0F;
         float g = 1.0F;
         float b = 1.0F;
         boolean onCooldown = false;
         boolean noMats = false;
         if (i == 0) {
            if (this.cdAmmo > 0) {
               onCooldown = true;
            }

            if (this.materials < (Integer)WarfareConfig.HUB_RESUPPLY_COST.get()) {
               noMats = true;
            }
          } else if (i == 1) {
             onCooldown = true;
          } else if (i == 2) {
             onCooldown = true;
          } else if (i == 3) {
            if (this.materials < 20) {
               noMats = true;
            }
         } else if (i == 4 && this.materials < 50) {
            noMats = true;
         }

         if (onCooldown || noMats) {
            r = 1.0F;
            g = 0.4F;
            b = 0.4F;
         } else if (isSelected) {
            r = 0.4F;
            g = 1.0F;
            b = 0.4F;
         }

         RenderSystem.setShaderColor(r, g, b, 1.0F);
         gui.blit(SECTOR_TEXTURE, 0, 0, 0.0F, 0.0F, size, size, size, size);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         pose.popPose();
      }

      this.drawLabel(gui, "Resupply (" + WarfareConfig.HUB_RESUPPLY_COST.get() + ")", centerX, centerY - 75, selected == 0, this.cdAmmo);
      this.drawLabel(gui, "AGS-30 (20)", centerX + 70, centerY - 25, selected == 1, 0);
      this.drawLabel(gui, "M2 (15)", centerX + 45, centerY + 65, selected == 2, 0);
      this.drawLabel(gui, "Mortar (20)", centerX - 45, centerY + 65, selected == 3, 0);
      this.drawLabel(gui, "TOW (50)", centerX - 70, centerY - 25, selected == 4, 0);
   }

   private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected, int cooldownTicks) {
      int color = selected ? -16711936 : -1;
      if (cooldownTicks > 0) {
         text = cooldownTicks / 20 + "s";
         color = -43691;
      }

      int width = PWPTheme.Fonts.display().width(text);
      gui.drawString(PWPTheme.Fonts.display(), text, x - width / 2, y - 4, color, true);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int centerX = this.width / 2;
         int centerY = this.height / 2;
         double dx = mouseX - centerX;
         double dy = mouseY - centerY;
         double dist = Math.sqrt(dx * dx + dy * dy);
         if (dist > 10.0) {
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
            if (angle < 0.0) {
               angle += 360.0;
            }

            double shiftedAngle = angle + 36.0;
            if (shiftedAngle >= 360.0) {
               shiftedAngle -= 360.0;
            }

            int sector = (int)(shiftedAngle / 72.0);
             if (sector == 0) {
                if (this.cdAmmo > 0) {
                   return true;
                }

                if (this.materials >= (Integer)WarfareConfig.HUB_RESUPPLY_COST.get() || Minecraft.getInstance().player.isCreative()) {
                   ClientData.lastFobResupplyTime = System.currentTimeMillis();
                }
             }

             if (sector == 1 || sector == 2) {
                return true;
             }

             PacketHandler.INSTANCE.sendToServer(new PacketRequestCrateAmmo(this.entityId, sector));
            this.onClose();
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }
}

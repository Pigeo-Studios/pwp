package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.item.RallyItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.pwp.coreclient.gui.theme.PWPTheme;

// Р Р°РґРёР°Р»СЊРЅРѕРµ РјРµРЅСЋ СЃС‚Р°С†РёРѕРЅР°СЂРЅРѕРіРѕ РѕСЂСѓР¶РёСЏ
// РџРѕР·РІРѕР»СЏРµС‚ СЂР°Р·РјРµСЃС‚РёС‚СЊ M2 Browning, РјРёРЅРѕРјС‘С‚, РђР“РЎ-30 РёР»Рё РџРўР Рљ TOW
public class StaticGunRadialScreen extends Screen {
   private static final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/radial_sector_4.png");
   private final Screen parentScreen;
   private boolean isSwitching = false;

   public StaticGunRadialScreen(Screen parent) {
      super(Component.translatable("gui.pwpwarfare.radial.static_guns"));
      this.parentScreen = parent;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      this.triggerRadioAnim("deploy");
   }

   public void onClose() {
      if (!this.isSwitching) {
         this.triggerRadioAnim("close");
      }

      super.onClose();
   }

   private void triggerRadioAnim(String animName) {
      if (this.minecraft.player != null) {
         ItemStack stack = this.minecraft.player.getMainHandItem();
         if (stack.getItem() instanceof RallyItem radio) {
            long instanceId = stack.getOrCreateTag().getLong("GeckoLibID");
            radio.triggerAnim(this.minecraft.player, instanceId, "RadioController", animName);
         }
      }
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      double dx = mouseX - centerX;
      double dy = mouseY - centerY;
      double distance = Math.sqrt(dx * dx + dy * dy);
      int selected = -1;
      if (distance > 10.0) {
         double angle = Math.toDegrees(Math.atan2(dy, dx));
         if (angle < 0.0) {
            angle += 360.0;
         }

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

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      PoseStack pose = gui.pose();
      int size = 95;

      for (int i = 0; i < 4; i++) {
         pose.pushPose();
         pose.translate(centerX, centerY, 0.0F);
         float rot = 0.0F;
         if (i == 0) {
            rot = 90.0F;
         }

         if (i == 1) {
            rot = 180.0F;
         }

         if (i == 2) {
            rot = -90.0F;
         }

         if (i == 3) {
            rot = 0.0F;
         }

         pose.mulPose(Axis.ZP.rotationDegrees(rot));
         boolean isSelected = i == selected;
         float scale = isSelected ? 1.15F : 1.0F;
         pose.scale(scale, scale, 1.0F);
          pose.translate(-size / 2.0F, -size, 0.0F);
          boolean disabled = i == 0 || i == 2;
          if (disabled) {
             RenderSystem.setShaderColor(1.0F, 0.4F, 0.4F, 1.0F);
          } else if (isSelected) {
             RenderSystem.setShaderColor(0.4F, 1.0F, 0.4F, 1.0F);
          } else {
             RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
          }

          gui.blit(SECTOR_TEXTURE, 0, 0, 0.0F, 0.0F, size, size, size, size);
          pose.popPose();
       }

       RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
       this.drawLabel(gui, "M2 Browning", centerX + 60, centerY, false);
       this.drawLabel(gui, "Mortar", centerX, centerY + 60, selected == 1);
       this.drawLabel(gui, "AGS-30", centerX - 60, centerY, false);
       this.drawLabel(gui, "TOW", centerX, centerY - 60, selected == 3);
   }

   private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected) {
      int color = selected ? -16711936 : -1;
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
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0.0) {
               angle += 360.0;
            }

            int selected = 0;
            byte var17;
            if (angle >= 45.0 && angle < 135.0) {
               var17 = 1;
            } else if (angle >= 135.0 && angle < 225.0) {
               var17 = 2;
            } else if (angle >= 225.0 && angle < 315.0) {
               var17 = 3;
            } else {
               var17 = 0;
            }

             if (var17 == 1) {
                ClientPlacementHandler.startPlacing(22);
             }

             if (var17 == 3) {
                ClientPlacementHandler.startPlacing(23);
             }

            this.onClose();
            return true;
         }
      }

      if (button == 1) {
         Minecraft.getInstance().setScreen(this.parentScreen);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }
}

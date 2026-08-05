package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.item.RallyItem;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRadioAction;
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

// Р Р°РґРёР°Р»СЊРЅРѕРµ РјРµРЅСЋ РѕР±РѕСЂРѕРЅРёС‚РµР»СЊРЅС‹С… СЃРѕРѕСЂСѓР¶РµРЅРёР№
// РџРѕР·РІРѕР»СЏРµС‚ СЂР°Р·РјРµС‰Р°С‚СЊ СЃС‚РµРЅС‹, РєРѕР»СЋС‡СѓСЋ РїСЂРѕРІРѕР»РѕРєСѓ Рё С…Р°Р±
public class DefenseRadialScreen extends Screen {
   private static final ResourceLocation SECTOR_TEXTURE_5 = new ResourceLocation("pwpwarfare", "textures/gui/radial_sector_5.png");
   private final Screen parentScreen;
   private boolean isSwitching = false;

   public DefenseRadialScreen(Screen parent) {
      super(Component.translatable("gui.pwpwarfare.radial.defense"));
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
      double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
      if (angle < 0.0) {
         angle += 360.0;
      }

      int selected = -1;
      if (distance > 10.0) {
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
         if (isSelected) {
            RenderSystem.setShaderColor(0.4F, 1.0F, 0.4F, 1.0F);
         } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gui.blit(SECTOR_TEXTURE_5, 0, 0, 0.0F, 0.0F, size, size, size, size);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         pose.popPose();
      }

      this.drawLabel(gui, "СТЕНЫ", centerX, centerY - 75, selected == 0);
      this.drawLabel(gui, "БУНКЕР", centerX + 70, centerY - 25, selected == 1);
      this.drawLabel(gui, "СТАНЦИЯ ТЕХНИКИ", centerX + 45, centerY + 65, selected == 2);
      this.drawLabel(gui, "КОЛЮЧКА", centerX - 45, centerY + 65, selected == 3);
      this.drawLabel(gui, "ФОБ", centerX - 70, centerY - 25, selected == 4);
   }

   private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected) {
      PoseStack pose = gui.pose();
      pose.pushPose();
      pose.translate(x, y, 0.0F);
      float scale = selected ? 1.1F : 0.9F;
      pose.scale(scale, scale, 1.0F);
      int color = selected ? -16711936 : -1;
      int width = PWPTheme.Fonts.display().width(text);
      gui.drawString(PWPTheme.Fonts.display(), text, -width / 2, -4, color, true);
      pose.popPose();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int centerX = this.width / 2;
         int centerY = this.height / 2;
         double dx = mouseX - centerX;
         double dy = mouseY - centerY;
         double distance = Math.sqrt(dx * dx + dy * dy);
         if (distance > 10.0) {
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
               this.isSwitching = true;
               Minecraft.getInstance().setScreen(new WallRadialScreen(this));
            } else if (sector == 1) {
               ClientPlacementHandler.startPlacing(17);
               this.onClose();
            } else if (sector == 2) {
               ClientPlacementHandler.startPlacing(18);
               this.onClose();
            } else if (sector == 3) {
               ClientPlacementHandler.startPlacing(13);
               this.onClose();
            } else {
               PacketHandler.INSTANCE.sendToServer(new PacketRadioAction(14));
               this.onClose();
            }

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

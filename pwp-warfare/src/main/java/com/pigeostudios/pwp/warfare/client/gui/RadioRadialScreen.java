package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.item.RallyItem;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRadioAction;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.pwp.coreclient.gui.theme.PWPTheme;

// Р Р°РґРёР°Р»СЊРЅРѕРµ РјРµРЅСЋ СЂР°РґРёРѕСЃС‚Р°РЅС†РёРё РєРѕРјР°РЅРґРёСЂР° РѕС‚СЂСЏРґР°
// РџРѕР·РІРѕР»СЏРµС‚ СѓСЃС‚Р°РЅРѕРІРёС‚СЊ С‚РѕС‡РєСѓ СЃР±РѕСЂР°, РѕР±РѕСЂРѕРЅРёС‚РµР»СЊРЅС‹Рµ СЃРѕРѕСЂСѓР¶РµРЅРёСЏ Рё СЃС‚Р°С†РёРѕРЅР°СЂРЅРѕРµ РѕСЂСѓР¶РёРµ
public class RadioRadialScreen extends Screen {
   private static final ResourceLocation SECTOR_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/radial_sector.png");
   private boolean isSwitching = false;

   public RadioRadialScreen() {
      super(Component.translatable("gui.pwpwarfare.radial.radio"));
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
      boolean rallyOnCooldown = false;
      long secondsLeft = 0L;
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         String pName = player.getScoreboardName();
         WarfareWorldData.Squad mySquad = null;

         for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(pName)) {
               mySquad = s;
               break;
            }
         }

         if (mySquad != null) {
            long cooldownEnd = mySquad.nextRallyAvailableTick;
            long gameTime = player.level().getGameTime();
            if (gameTime < cooldownEnd && !player.isCreative()) {
               rallyOnCooldown = true;
               secondsLeft = (cooldownEnd - gameTime) / 20L;
            }
         }
      }

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
         if (i == 0 && rallyOnCooldown) {
            RenderSystem.setShaderColor(1.0F, 0.4F, 0.4F, 1.0F);
         } else if (isSelected) {
            RenderSystem.setShaderColor(0.4F, 1.0F, 0.4F, 1.0F);
         } else {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gui.blit(SECTOR_TEXTURE, 0, 0, 0.0F, 0.0F, size, size, size, size);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         pose.popPose();
      }

      String rallyText = "ТОЧКА СБОРА";
      int rallyColor = selected == 0 ? -16711936 : -1;
      if (rallyOnCooldown) {
         rallyText = "ОЖИДАНИЕ: " + secondsLeft + "с";
         rallyColor = -43691;
      }

      this.drawLabel(gui, rallyText, centerX, centerY - 70, selected == 0, rallyColor);
      this.drawLabel(gui, "ОБОРОНА", centerX + 60, centerY + 35, selected == 1, selected == 1 ? -16711936 : -1);
      this.drawLabel(gui, "УСТАНОВКИ", centerX - 60, centerY + 35, selected == 2, selected == 2 ? -16711936 : -1);
   }

   private void drawLabel(GuiGraphics gui, String text, int x, int y, boolean selected, int color) {
      PoseStack pose = gui.pose();
      pose.pushPose();
      pose.translate(x, y, 0.0F);
      float textScale = selected ? 1.1F : 0.9F;
      pose.scale(textScale, textScale, 1.0F);
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

            int action = -1;
            if (angle > 300.0 || angle <= 60.0) {
               action = 0;
            } else if (angle > 60.0 && angle <= 180.0) {
               action = 1;
            } else if (angle > 180.0 && angle <= 300.0) {
               action = 2;
            }

            if (action == 0) {
               PacketHandler.INSTANCE.sendToServer(new PacketRadioAction(0));
               this.onClose();
            }

            if (action == 1) {
               this.isSwitching = true;
               Minecraft.getInstance().setScreen(new DefenseRadialScreen(this));
            } else if (action == 2) {
               this.isSwitching = true;
               Minecraft.getInstance().setScreen(new StaticGunRadialScreen(this));
            }

            if (action != -1) {
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }
}

package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.PacketDownedAction;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

// Экран состояния нокаута (подбит/ранен)
// Показывает время до истечения крови, кнопки сдачи и вызова медика
public class DownedScreen extends Screen {
   private static final ResourceLocation HEARTBEAT_ICON = new ResourceLocation("pwpwarfare", "textures/gui/heartbeat.png");
   private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("pwpwarfare", "textures/misc/vignette.png");
   private Button callMedicButton;
   private final long screenOpenTime;
   private long lastMedicCallTime = 0L;

   public DownedScreen() {
      super(Component.translatable("gui.pwpwarfare.downed.title"));
      this.screenOpenTime = System.currentTimeMillis();
   }

   protected void init() {
      int cx = this.width / 2;
      int bottomY = this.height - 50;
      this.addRenderableWidget(new DownedScreen.SquadButton(cx - 130, bottomY, 120, 24, Component.translatable("gui.pwpwarfare.downed.give_up"), b -> {
         PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(1));
         this.onClose();
      }));
      this.callMedicButton = (Button)this.addRenderableWidget(new DownedScreen.SquadButton(cx + 10, bottomY, 120, 24, Component.translatable("gui.pwpwarfare.downed.call_medic"), b -> {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastMedicCallTime >= 15000L) {
            PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(0));
            this.lastMedicCallTime = currentTime;
         }
      }));
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderVignette(gui);
      int cx = this.width / 2;
      int baseY = this.height - 150;
      int boxW = 320;
      int boxH = 135;
      this.renderSquadFrame(gui, cx - boxW / 2, baseY, boxW, boxH);
      RenderSystem.enableBlend();
      gui.blit(HEARTBEAT_ICON, cx - 18, baseY + 10, 0.0F, 0.0F, 36, 36, 36, 36);
      Component allyStatus = this.getAllyDistanceStatus();
      gui.drawCenteredString(this.font, allyStatus, cx, baseY + 55, 16777215);
      int maxSeconds = (Integer)WarfareConfig.MAX_DOWNED_TIME_SECONDS.get();
      long remainingBleedout = maxSeconds - (System.currentTimeMillis() - this.screenOpenTime) / 1000L;
      if (remainingBleedout < 0L) {
         remainingBleedout = 0L;
      }

      Component bleedText = Component.translatable("gui.pwpwarfare.downed.bleeding_out", remainingBleedout);
      gui.drawCenteredString(this.font, bleedText, cx, baseY + 72, 11184810);
      long remainingCooldown = 15000L - (System.currentTimeMillis() - this.lastMedicCallTime);
      if (remainingCooldown > 0L) {
         this.callMedicButton.setMessage(Component.translatable("gui.pwpwarfare.downed.call_medic_cooldown", remainingCooldown / 1000L + 1L));
         this.callMedicButton.active = false;
      } else {
         this.callMedicButton.setMessage(Component.translatable("gui.pwpwarfare.downed.call_medic"));
         this.callMedicButton.active = true;
      }

      super.render(gui, mx, my, pt);
   }

   private Component getAllyDistanceStatus() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         double minDistance = Double.MAX_VALUE;
         Player closestAlly = null;
         boolean found = false;

         for (Player other : mc.level.players()) {
            if (other != mc.player && !other.isSpectator() && other.isAlive() && mc.player.getTeam() != null && other.getTeam() == mc.player.getTeam()) {
               double dist = mc.player.distanceTo(other);
               if (dist < minDistance) {
                  minDistance = dist;
                  closestAlly = other;
                  found = true;
               }
            }
         }

         if (found && !(minDistance > 250.0)) {
            String allyName = closestAlly.getScoreboardName();
            return Component.translatable("gui.pwpwarfare.downed.closest_ally", (int)minDistance, allyName);
         } else {
            return Component.translatable("gui.pwpwarfare.downed.no_allies");
         }
      } else {
         return Component.translatable("gui.pwpwarfare.downed.no_allies");
      }
   }

   private void renderSquadFrame(GuiGraphics gui, int x, int y, int w, int h) {
      gui.fill(x, y, x + w, y + h, -1728053248);
      gui.renderOutline(x, y, w, h, 1157627903);
      gui.renderOutline(x + 2, y + 2, w - 4, h - 4, -1140850689);
   }

   private void renderVignette(GuiGraphics gui) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(0.8F, 0.0F, 0.0F, 0.05F);
      gui.blit(VIGNETTE_TEXTURE, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.8F);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private static class SquadButton extends Button {
      public SquadButton(int x, int y, int width, int height, Component message, OnPress onPress) {
         super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
         if (this.visible) {
            int borderColor = this.isHovered() ? -1 : -6710887;
            if (!this.active) {
               borderColor = -12303292;
            }

            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -871296751);
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
            int textColor = this.active ? -1 : -8947849;
            gui.drawCenteredString(
               Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor
            );
            if (this.active && this.isHovered()) {
               gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height, -1);
            }
         }
      }
   }
}

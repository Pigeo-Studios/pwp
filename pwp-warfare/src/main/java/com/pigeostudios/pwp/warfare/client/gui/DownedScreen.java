package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.PacketDownedAction;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

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
      int bottomY = this.height - 55;
      this.addRenderableWidget(new DownedButton(cx - 130, bottomY, 120, 24, Component.translatable("gui.pwpwarfare.downed.give_up"), b -> {
         PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(1));
         this.onClose();
      }, PWPTheme.Colors.DANGER));
      this.callMedicButton = this.addRenderableWidget(new DownedButton(cx + 10, bottomY, 120, 24, Component.translatable("gui.pwpwarfare.downed.call_medic"), b -> {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastMedicCallTime >= 15000L) {
            PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(0));
            this.lastMedicCallTime = currentTime;
         }
      }, PWPTheme.Colors.SUCCESS));
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderVignette(gui);
      int cx = this.width / 2;
      int baseY = this.height - 155;
      int boxW = 340;
      int boxH = 130;
      this.renderSquadFrame(gui, cx - boxW / 2, baseY, boxW, boxH);
      RenderSystem.enableBlend();

      gui.blit(HEARTBEAT_ICON, cx - 18, baseY + 12, 0.0F, 0.0F, 36, 36, 36, 36);

      Component allyStatus = this.getAllyDistanceStatus();
      gui.drawCenteredString(this.font, allyStatus, cx, baseY + 58, PWPTheme.Colors.TEXT_PRIMARY);

      int maxSeconds = (Integer)WarfareConfig.MAX_DOWNED_TIME_SECONDS.get();
      long remainingBleedout = maxSeconds - (System.currentTimeMillis() - this.screenOpenTime) / 1000L;
      if (remainingBleedout < 0L) {
         remainingBleedout = 0L;
      }

      // Bleedout progress bar
      int barW = 280;
      int barH = 6;
      int barX = cx - barW / 2;
      int barY = baseY + 75;
      float pct = (float) remainingBleedout / maxSeconds;
      gui.fill(barX, barY, barX + barW, barY + barH, PWPTheme.Styles.Progress.BG);
      int fillColor = pct > 0.3f ? PWPTheme.Colors.SUCCESS : (pct > 0.15f ? PWPTheme.Colors.WARNING : PWPTheme.Colors.DANGER);
      gui.fill(barX, barY, barX + (int)(barW * pct), barY + barH, fillColor);

      Component bleedText = Component.translatable("gui.pwpwarfare.downed.bleeding_out", remainingBleedout);
      gui.drawCenteredString(this.font, bleedText, cx, baseY + 85, PWPTheme.Colors.TEXT_SECONDARY);

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
      gui.fill(x, y, x + w, y + h, 0xCC0E1117);
      gui.renderOutline(x, y, w, h, PWPTheme.Colors.BORDER_ACCENT);
      gui.fill(x + 1, y + 1, x + w - 1, y + 2, PWPTheme.Colors.ACCENT_DIM);
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

   private static class DownedButton extends Button {
      private final int accentColor;
      private float hoverAnim;
      private long lastTick;

      public DownedButton(int x, int y, int width, int height, Component message, OnPress onPress, int accent) {
         super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
         this.accentColor = accent;
         this.hoverAnim = 0.0F;
         this.lastTick = System.currentTimeMillis();
      }

      protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
         if (!this.visible) return;

         long now = System.currentTimeMillis();
         float dt = Math.min((now - lastTick) / 50.0F, 4.0F);
         lastTick = now;

         boolean hovered = this.isHovered() && this.active;
         float target = hovered ? 1.0F : 0.0F;
         if (target > hoverAnim) {
            hoverAnim = Math.min(hoverAnim + 0.15F * dt, target);
         } else {
            hoverAnim = Math.max(hoverAnim - 0.12F * dt, target);
         }

         int bg = lerpColor(PWPTheme.Styles.Button.DARK_BG, accentColor, hoverAnim * 0.3F);
         int border = this.active ? (hovered ? accentColor : PWPTheme.Colors.BORDER) : PWPTheme.Colors.TEXT_DIM;
         if (!this.active) border = PWPTheme.Colors.TEXT_DIM;

         gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
         gui.renderOutline(this.getX(), this.getY(), this.width, this.height, border);
         int textColor = this.active ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM;
         gui.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
            this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
         if (this.active && hovered) {
            gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height, accentColor);
         }
      }

      private int lerpColor(int from, int to, float t) {
         if (t <= 0) return from;
         if (t >= 1) return to;
         int a1 = (from >> 24) & 0xFF, r1 = (from >> 16) & 0xFF, g1 = (from >> 8) & 0xFF, b1 = from & 0xFF;
         int a2 = (to >> 24) & 0xFF, r2 = (to >> 16) & 0xFF, g2 = (to >> 8) & 0xFF, b2 = to & 0xFF;
         return ((int)(a1 + (a2 - a1) * t) << 24) | ((int)(r1 + (r2 - r1) * t) << 16) | ((int)(g1 + (g2 - g1) * t) << 8) | (int)(b1 + (b2 - b1) * t);
      }
   }
}

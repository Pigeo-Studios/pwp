package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestAmmo;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class HubAmmoScreen extends Screen {
   private final BlockPos hubPos;
   private final int boxSize = 60;
   private final int gap = 20;
   private int cachedCdAGS = 0;
   private int cachedCdM2 = 0;

   public HubAmmoScreen(BlockPos pos) {
      super(Component.translatable("gui.pwpwarfare.radial.hub_supply"));
      this.hubPos = pos;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void updateCooldowns() {
      this.cachedCdAGS = 1;
      this.cachedCdM2 = 1;
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
      this.updateCooldowns();
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int leftX = centerX - 60 - 10;
      int leftY = centerY - 30;
      boolean hoverLeft = mouseX >= leftX && mouseX <= leftX + 60 && mouseY >= leftY && mouseY <= leftY + 60;
      int colorLeft = this.cachedCdAGS > 0 ? PWPTheme.Colors.DANGER : (hoverLeft ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY);

      this.renderBox(gui, leftX, leftY, 60, colorLeft, "AGS-30", this.cachedCdAGS);
      int rightX = centerX + 10;
      int rightY = centerY - 30;
      boolean hoverRight = mouseX >= rightX && mouseX <= rightX + 60 && mouseY >= rightY && mouseY <= rightY + 60;
      int colorRight = this.cachedCdM2 > 0 ? PWPTheme.Colors.DANGER : (hoverRight ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY);

      this.renderBox(gui, rightX, rightY, 60, colorRight, "M2 Ammo", this.cachedCdM2);
      super.render(gui, mouseX, mouseY, partialTick);
   }

   private void renderBox(GuiGraphics gui, int x, int y, int size, int color, String label, int cooldown) {
      gui.fill(x - 2, y - 2, x + size + 2, y + size + 2, PWPTheme.Colors.BORDER);
      gui.fill(x, y, x + size, y + size, PWPTheme.Colors.SURFACE_LIGHT);
      gui.renderOutline(x, y, size, size, color);
      int labelWidth = this.font.width(label);
      gui.drawString(this.font, label, x + (size - labelWidth) / 2, y + size / 2 - 10, color, true);
      if (cooldown > 0) {
         String time = cooldown / 20 + "s";
         int timeW = this.font.width(time);
         gui.drawString(this.font, time, x + (size - timeW) / 2, y + size / 2 + 5, PWPTheme.Colors.TEXT_DIM, true);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.updateCooldowns();
         int centerX = this.width / 2;
         int centerY = this.height / 2;
         int leftX = centerX - 60 - 10;
         int rightX = centerX + 10;
         int boxY = centerY - 30;
         if (mouseX >= leftX && mouseX <= leftX + 60 && mouseY >= boxY && mouseY <= boxY + 60 && this.cachedCdAGS == 0) {
            PacketHandler.INSTANCE.sendToServer(new PacketRequestAmmo(this.hubPos, 0));
            this.onClose();
            return true;
         }
         if (mouseX >= rightX && mouseX <= rightX + 60 && mouseY >= boxY && mouseY <= boxY + 60 && this.cachedCdM2 == 0) {
            PacketHandler.INSTANCE.sendToServer(new PacketRequestAmmo(this.hubPos, 1));
            this.onClose();
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }
}

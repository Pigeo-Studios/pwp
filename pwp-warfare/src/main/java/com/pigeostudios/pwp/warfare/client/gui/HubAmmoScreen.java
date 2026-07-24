package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestAmmo;
import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class HubAmmoScreen extends Screen {
   private final BlockPos hubPos;
   private int cachedCdAGS;
   private int cachedCdM2;

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

   @Override
   protected void init() {
      int panelW = 180;
      int panelH = 100;
      int cx = PWPLayout.centerX(width, panelW);
      int cy = (height - panelH) / 2;

      addRenderableWidget(new PWPButton(
         cx + 10, cy + 40, 70, 40,
         Component.literal("AGS-30"),
         b -> { PacketHandler.INSTANCE.sendToServer(new PacketRequestAmmo(this.hubPos, 0)); this.onClose(); },
         PWPButton.Style.PRIMARY
      ));

      addRenderableWidget(new PWPButton(
         cx + panelW - 80, cy + 40, 70, 40,
         Component.literal("M2\nAmmo"),
         b -> { PacketHandler.INSTANCE.sendToServer(new PacketRequestAmmo(this.hubPos, 1)); this.onClose(); },
         PWPButton.Style.PRIMARY
      ));
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
      this.updateCooldowns();

      int panelW = 180;
      int panelH = 100;
      int cx = PWPLayout.centerX(width, panelW);
      int cy = (height - panelH) / 2;

      PWPPanel.render(gui, cx, cy, panelW, panelH);
      gui.drawCenteredString(font, title, width / 2, cy + 8, PWPTheme.Colors.TEXT_PRIMARY);

      if (cachedCdAGS > 0) {
         String time = cachedCdAGS / 20 + "s";
         gui.drawCenteredString(font, Component.literal(time), cx + 45, cy + 85, PWPTheme.Colors.DANGER);
      }
      if (cachedCdM2 > 0) {
         String time = cachedCdM2 / 20 + "s";
         gui.drawCenteredString(font, Component.literal(time), cx + panelW - 45, cy + 85, PWPTheme.Colors.DANGER);
      }

      super.render(gui, mouseX, mouseY, partialTick);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }
}

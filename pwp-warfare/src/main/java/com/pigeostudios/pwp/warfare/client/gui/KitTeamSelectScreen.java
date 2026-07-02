package com.pigeostudios.pwp.warfare.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Экран выбора команды для редактирования наборов экипировки
public class KitTeamSelectScreen extends Screen {
   public KitTeamSelectScreen() {
      super(Component.translatable("gui.pwpwarfare.kit_team_select.title"));
   }

   protected void init() {
      int cx = this.width / 2;
      int cy = this.height / 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.pwpwarfare.kit_team_select.blue"), b -> this.minecraft.setScreen(new KitListScreen("BLUE")))
            .bounds(cx - 105, cy - 10, 100, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.pwpwarfare.kit_team_select.red"), b -> this.minecraft.setScreen(new KitListScreen("RED")))
            .bounds(cx + 5, cy - 10, 100, 20)
            .build()
      );
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
      super.render(gui, mx, my, pt);
   }
}

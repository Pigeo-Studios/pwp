package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMapMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Экран сетки тактических маркеров для размещения на карте
// Отображает доступные маркеры в виде сетки для выбора
public class MapMarkerGridScreen extends Screen {
   private final int worldX;
   private final int worldZ;
   private final Map<String, ResourceLocation> markers;
   private static final int COLS = 4;
   private static final int CELL_WIDTH = 90;
   private static final int CELL_HEIGHT = 90;
   private static final int BTN_WIDTH = 80;

   public MapMarkerGridScreen(int x, int z, String titleKey, Map<String, ResourceLocation> markers) {
      super(Component.translatable(titleKey));
      this.worldX = x;
      this.worldZ = z;
      this.markers = markers;
   }

   protected void init() {
      int totalWidth = 360;
      int startX = (this.width - totalWidth) / 2;
      int startY = 40;
      int i = 0;

      for (String type : this.markers.keySet()) {
         int row = i / 4;
         int col = i % 4;
         int x = startX + col * 90 + 5;
         int y = startY + row * 90;
          this.addRenderableWidget(Button.builder(Component.translatable("pwpwarfare.marker." + type.toLowerCase().replace(" ", "_")), b -> {
            PacketHandler.INSTANCE.sendToServer(new PacketPlaceMapMarker(this.worldX, this.worldZ, type));
            this.minecraft.setScreen(new SquadSelectionScreen());
         }).bounds(x, y + 45, 80, 20).build());
         i++;
      }
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
      int totalWidth = 360;
      int startX = (this.width - totalWidth) / 2;
      int startY = 40;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      int i = 0;

      for (ResourceLocation icon : this.markers.values()) {
         int row = i / 4;
         int col = i % 4;
         int iconX = startX + col * 90 + 29;
         int iconY = startY + row * 90 + 5;
         gui.blit(icon, iconX, iconY, 0.0F, 0.0F, 32, 32, 32, 32);
         i++;
      }

      super.render(gui, mx, my, pt);
   }

   public boolean isPauseScreen() {
      return false;
   }
}

package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMapMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Экран выбора тактического маркера для установки на карте
public class MapMarkerSelectionScreen extends Screen {
   private final BlockPos targetPos;
   private static final Map<String, ResourceLocation> MARKERS = new LinkedHashMap<>();

   public MapMarkerSelectionScreen(BlockPos pos) {
      super(Component.translatable("gui.pwpwarfare.map_marker_select.title"));
      this.targetPos = pos;
   }

   protected void init() {
      int startX = (this.width - 340) / 2;
      int startY = 40;
      int i = 0;

      for (String type : MARKERS.keySet()) {
         int row = i / 4;
         int col = i % 4;
         int x = startX + col * 85;
         int y = startY + row * 80;
          this.addRenderableWidget(Button.builder(Component.translatable("pwpwarfare.marker." + type.toLowerCase().replace(" ", "_")), b -> {
            PacketHandler.INSTANCE.sendToServer(new PacketPlaceMapMarker(this.targetPos.getX(), this.targetPos.getZ(), type));
            this.onClose();
         }).bounds(x, y + 35, 80, 20).build());
         i++;
      }
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
      int i = 0;
      int startX = (this.width - 340) / 2;
      int startY = 40;

      for (ResourceLocation icon : MARKERS.values()) {
         int row = i / 4;
         int col = i % 4;
         int iconX = startX + col * 85 + 24;
         int iconY = startY + row * 80;
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.blit(icon, iconX, iconY, 0.0F, 0.0F, 32, 32, 32, 32);
         i++;
      }

      super.render(gui, mx, my, pt);
   }

   static {
      MARKERS.put("Infantry", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/infantry_marker.png"));
      MARKERS.put("Sniper", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/sniper_marker.png"));
      MARKERS.put("HAT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hat_marker.png"));
      MARKERS.put("APC", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/apc_marker.png"));
      MARKERS.put("Tank", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/tank_marker.png"));
      MARKERS.put("Enemy HUB", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_marker.png"));
      MARKERS.put("Enemy Rally", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_marker.png"));
      MARKERS.put("Supply Request", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_request_marker.png"));
   }
}

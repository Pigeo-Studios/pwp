package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.WarfareClipboard;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenKitEditor;
import com.pigeostudios.pwp.warfare.network.PacketPasteKit;
import com.pigeostudios.pwp.warfare.network.PacketPasteTeam;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Экран списка наборов экипировки для выбранной команды
// Позволяет копировать, вставлять и открывать редактор каждого набора
public class KitListScreen extends Screen {
   private final String team;
   private static final int COLUMNS = 4;
   private static final int ROW_HEIGHT = 60;
   private static final int BUTTON_WIDTH = 90;

   public KitListScreen(String team) {
      super(Component.translatable("gui.pwpwarfare.kit_list.title_format", team));
      this.team = team;
   }

   protected void init() {
      int startX = (this.width - 440) / 2;
      int startY = 40;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.pwpwarfare.kit_list.copy_all"), b -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitData(this.team, "ALL")))
            .bounds(startX, 10, 80, 20)
            .build()
      );
      Button pasteAllBtn = Button.builder(Component.translatable("gui.pwpwarfare.kit_list.paste_all"), b -> {
         if (WarfareClipboard.teamKitsData != null) {
            PacketHandler.INSTANCE.sendToServer(new PacketPasteTeam(this.team, WarfareClipboard.teamKitsData));
         }
      }).bounds(startX + 85, 10, 80, 20).build();
      pasteAllBtn.active = WarfareClipboard.teamKitsData != null;
      this.addRenderableWidget(pasteAllBtn);

      for (int i = 0; i < WarfareWorldData.KIT_NAMES.length; i++) {
         String kitName = WarfareWorldData.KIT_NAMES[i];
         int row = i / 4;
         int col = i % 4;
         int x = startX + col * 110;
         int y = startY + row * 60 + 26;
         this.addRenderableWidget(
            Button.builder(Component.literal(kitName), b -> PacketHandler.INSTANCE.sendToServer(new PacketOpenKitEditor(this.team, kitName)))
               .bounds(x, y, 70, 20)
               .build()
         );
         this.addRenderableWidget(
            Button.builder(Component.translatable("gui.pwpwarfare.kit_list.copy"), b -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitData(this.team, kitName)))
               .bounds(x + 72, y, 15, 20)
               .build()
         );
          Button pBtn = Button.builder(Component.translatable("gui.pwpwarfare.kit_list.paste"), b -> {
            if (WarfareClipboard.kitData != null) {
               PacketHandler.INSTANCE.sendToServer(new PacketPasteKit(this.team, kitName, WarfareClipboard.kitData));
            }
         }).bounds(x + 89, y, 15, 20).build();
         pBtn.active = WarfareClipboard.kitData != null;
         this.addRenderableWidget(pBtn);
      }
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);
      int startX = (this.width - 440) / 2;
      int startY = 40;

      for (int i = 0; i < WarfareWorldData.KIT_NAMES.length; i++) {
         String kitName = WarfareWorldData.KIT_NAMES[i];
         int row = i / 4;
         int col = i % 4;
         String iconPath = kitName.toLowerCase().replace(" ", "_").replace("-", "_");
         ResourceLocation iconLoc = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + iconPath + ".png");
         int iconX = startX + col * 110 + 35 - 12;
         int iconY = startY + row * 60;
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         gui.blit(iconLoc, iconX, iconY, 0.0F, 0.0F, 24, 24, 24, 24);
      }

      super.render(gui, mx, my, pt);
      if (mx > (this.width - 440) / 2) {
      }
   }
}

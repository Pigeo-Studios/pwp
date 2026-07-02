package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Экран выбора класса (набора экипировки) игроком
// Отображает доступные наборы с иконками и кнопкой предпросмотра
public class PlayerKitSelectScreen extends Screen {
   private final List<PacketOpenPlayerKitMenu.KitDTO> kits;
   private static final int COLUMNS = 4;
   private static final int BUTTON_WIDTH = 90;
   private static final int ROW_HEIGHT = 60;
   private static final Component EYE_ICON = Component.literal("\ud83d\udc41");

   public PlayerKitSelectScreen(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
      super(Component.translatable("gui.pwpwarfare.kit_select.title"));
      this.kits = kits;
   }

   protected void init() {
      int startX = (this.width - 400) / 2;
      int startY = 40;
      int i = 0;

      for (PacketOpenPlayerKitMenu.KitDTO kit : this.kits) {
         int row = i / 4;
         int col = i % 4;
         int x = startX + col * 100;
         int y = startY + row * 60 + 26;
         Button btn = Button.builder(Component.literal(kit.name), b -> {
            PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kit.name));
            this.onClose();
         }).bounds(x, y, 70, 20).build();
         btn.active = kit.available;
         this.addRenderableWidget(btn);
         this.addRenderableWidget(
            Button.builder(EYE_ICON, b -> this.minecraft.setScreen(new KitPreviewScreen(this, kit.name, kit.items)))
               .bounds(x + 90 - 18, y, 20, 20)
               .build()
         );
         i++;
      }
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);
      int startX = (this.width - 400) / 2;
      int startY = 40;

      for (int i = 0; i < this.kits.size(); i++) {
         PacketOpenPlayerKitMenu.KitDTO kit = this.kits.get(i);
         int row = i / 4;
         int col = i % 4;
         String iconName = kit.name.toLowerCase().replace(" ", "_").replace("-", "_");
         ResourceLocation iconLoc = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + iconName + ".png");
         int iconX = startX + col * 100 + 45 - 12;
         int iconY = startY + row * 60;
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, kit.available ? 1.0F : 0.4F);
         gui.blit(iconLoc, iconX, iconY, 0.0F, 0.0F, 24, 24, 24, 24);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      super.render(gui, mx, my, pt);

      for (Renderable widget : this.renderables) {
         if (widget instanceof Button btn && btn.isHovered() && !btn.active) {
            for (PacketOpenPlayerKitMenu.KitDTO kit : this.kits) {
               if (btn.getMessage().getString().equals(kit.name)) {
                  gui.renderTooltip(this.font, Component.literal(kit.reason), mx, my);
                  break;
               }
            }
         }
      }
   }
}

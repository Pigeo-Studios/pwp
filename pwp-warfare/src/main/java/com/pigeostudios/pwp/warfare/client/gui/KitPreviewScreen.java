package com.pigeostudios.pwp.warfare.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// Экран предпросмотра набора экипировки
// Показывает содержимое инвентаря набора до его выбора
public class KitPreviewScreen extends Screen {
   private final Screen parent;
   private final String kitName;
   private final List<ItemStack> items;

   public KitPreviewScreen(Screen parent, String kitName, List<ItemStack> items) {
      super(Component.translatable("gui.pwpwarfare.kit_preview.format", kitName));
      this.parent = parent;
      this.kitName = kitName;
      this.items = items;
   }

   protected void init() {
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.pwpwarfare.kit_preview.back"), b -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 - 40, this.height - 30, 80, 20)
            .build()
      );
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
      int startX = this.width / 2 - 81;
      int startY = 50;

      for (int i = 0; i < 27; i++) {
         int x = startX + i % 9 * 18;
         int y = startY + i / 9 * 18;
         this.drawSlot(gui, x, y, this.items.get(i + 9));
      }

      for (int i = 0; i < 9; i++) {
         int x = startX + i * 18;
         int y = startY + 60;
         this.drawSlot(gui, x, y, this.items.get(i));
      }

      for (int i = 0; i < 5; i++) {
         int x = startX + i * 18;
         int y = startY + 85;
         this.drawSlot(gui, x, y, this.items.get(i + 36));
      }

      super.render(gui, mx, my, pt);
   }

   private void drawSlot(GuiGraphics gui, int x, int y, ItemStack stack) {
      gui.fill(x, y, x + 17, y + 17, 1358954495);
      gui.renderFakeItem(stack, x + 1, y + 1);
      gui.renderItemDecorations(this.font, stack, x + 1, y + 1);
   }
}

package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSaveKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// Экран редактора наборов экипировки
// Позволяет настроить предметы, лимиты и флаги пополнения для каждого набора
public class KitEditorScreen extends AbstractContainerScreen<KitEditorMenu> {
    private EditBox maxTeamBox;
    private EditBox maxSquadBox;
    private EditBox minPlayersBox;
    private boolean keepContainerOpen = false;

    public KitEditorScreen(KitEditorMenu menu, Inventory inv, Component title) {
       super(menu, inv, title);
       this.imageWidth = 176;
       this.imageHeight = 262;
       this.inventoryLabelY = 168;
       this.titleLabelY = 4;
    }

    @Override
    public void removed() {
        if (!keepContainerOpen) {
            super.removed();
        }
    }

    protected void init() {
       super.init();
       this.keepContainerOpen = false;
       int x = this.leftPos;
      int y = this.topPos;
      this.addRenderableWidget(Button.builder(((KitEditorMenu)this.menu).isLeaderOnly ? Component.translatable("gui.pwpwarfare.kit_editor.leader_only") : Component.translatable("gui.pwpwarfare.kit_editor.leader_only_off"), b -> {
         ((KitEditorMenu)this.menu).isLeaderOnly = !((KitEditorMenu)this.menu).isLeaderOnly;
         b.setMessage(((KitEditorMenu)this.menu).isLeaderOnly ? Component.translatable("gui.pwpwarfare.kit_editor.leader_only") : Component.translatable("gui.pwpwarfare.kit_editor.leader_only_off"));
      }).bounds(x + 8, y + 14, 90, 18).build());
      this.maxTeamBox = new EditBox(this.font, x + 144, y + 14, 24, 14, Component.empty());
      this.maxTeamBox.setValue(String.valueOf(((KitEditorMenu)this.menu).maxPerTeam));
      this.addRenderableWidget(this.maxTeamBox);
      this.maxSquadBox = new EditBox(this.font, x + 144, y + 32, 24, 14, Component.empty());
      this.maxSquadBox.setValue(String.valueOf(((KitEditorMenu)this.menu).maxPerSquad));
      this.addRenderableWidget(this.maxSquadBox);
      this.minPlayersBox = new EditBox(this.font, x + 144, y + 50, 24, 14, Component.empty());
      this.minPlayersBox.setValue(String.valueOf(((KitEditorMenu)this.menu).minSquadPlayers));
      this.addRenderableWidget(this.minPlayersBox);
      this.addRenderableWidget(Button.builder(Component.translatable("gui.pwpwarfare.kit_editor.save"), b -> this.saveKit()).bounds(x + 120, y + 148, 48, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("gui.pwpwarfare.kit_editor.skins"), b -> {
          keepContainerOpen = true;
          Minecraft.getInstance().setScreen(new KitSkinSelectScreen((KitEditorMenu) this.menu, this));
       }).bounds(x + 74, y + 148, 44, 20).build());
   }

   private void saveKit() {
      try {
         ((KitEditorMenu)this.menu).maxPerTeam = Integer.parseInt(this.maxTeamBox.getValue());
      } catch (Exception var4) {
      }

      try {
         ((KitEditorMenu)this.menu).maxPerSquad = Integer.parseInt(this.maxSquadBox.getValue());
      } catch (Exception var3) {
      }

      try {
         ((KitEditorMenu)this.menu).minSquadPlayers = Integer.parseInt(this.minPlayersBox.getValue());
      } catch (Exception var2) {
      }

      PacketHandler.INSTANCE
         .sendToServer(
            new PacketSaveKit(
               ((KitEditorMenu)this.menu).team,
               ((KitEditorMenu)this.menu).kitName,
               ((KitEditorMenu)this.menu).isLeaderOnly,
               ((KitEditorMenu)this.menu).maxPerTeam,
               ((KitEditorMenu)this.menu).maxPerSquad,
               ((KitEditorMenu)this.menu).minSquadPlayers,
               ((KitEditorMenu)this.menu).resupplyFlags,
               ((KitEditorMenu)this.menu).saveNbtFlags,
               ((KitEditorMenu)this.menu).slotSkins
            )
         );
      this.minecraft.player.displayClientMessage(Component.translatable("gui.pwpwarfare.kit_editor.saved"), true);
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      super.render(gui, mx, my, pt);
      this.renderTooltip(gui, mx, my);
      int x = this.leftPos;
      int y = this.topPos;
      int labelColor = 13421772;
      gui.drawString(this.font, Component.translatable("gui.pwpwarfare.kit_editor.max_team"), x + 98, y + 17, labelColor, false);
      gui.drawString(this.font, Component.translatable("gui.pwpwarfare.kit_editor.max_squad"), x + 103, y + 35, labelColor, false);
      gui.drawString(this.font, Component.translatable("gui.pwpwarfare.kit_editor.min_players"), x + 103, y + 53, labelColor, false);
      gui.pose().pushPose();
      gui.pose().scale(0.9F, 0.9F, 1.0F);
      int scaledX = (int)((x + 8) / 0.9F);
      int scaledY = (int)((y + 36) / 0.9F);
      gui.drawString(this.font, Component.translatable("gui.pwpwarfare.kit_editor.toggle_resupply"), scaledX, scaledY, 8454016, false);
      gui.drawString(this.font, Component.translatable("gui.pwpwarfare.kit_editor.save_nbt"), scaledX, scaledY + 10, 8454143, false);
      gui.pose().popPose();

      for (int i = 0; i < ((KitEditorMenu)this.menu).slots.size(); i++) {
         Slot slot = (Slot)((KitEditorMenu)this.menu).slots.get(i);
         if (slot.container == ((KitEditorMenu)this.menu).kitInventory) {
            int idx = slot.getContainerSlot();
            if (idx >= 0 && idx < 49) {
               if (((KitEditorMenu)this.menu).saveNbtFlags[idx]) {
                  gui.fill(
                     this.leftPos + slot.x,
                     this.topPos + slot.y,
                     this.leftPos + slot.x + 16,
                     this.topPos + slot.y + 16,
                     1610612991
                  );
               } else if (((KitEditorMenu)this.menu).resupplyFlags[idx]) {
                  gui.fill(
                     this.leftPos + slot.x,
                     this.topPos + slot.y,
                     this.leftPos + slot.x + 16,
                     this.topPos + slot.y + 16,
                     1627389696
                  );
               }
            }
         }
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 2) {
         Slot slot = this.hoveredSlot;
         if (slot != null && slot.container == ((KitEditorMenu)this.menu).kitInventory) {
            int idx = slot.getContainerSlot();
            if (idx >= 0 && idx < 49) {
               if (hasShiftDown()) {
                  ((KitEditorMenu)this.menu).saveNbtFlags[idx] = !((KitEditorMenu)this.menu).saveNbtFlags[idx];
               } else {
                  ((KitEditorMenu)this.menu).resupplyFlags[idx] = !((KitEditorMenu)this.menu).resupplyFlags[idx];
               }

               return true;
            }
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   protected void renderBg(GuiGraphics gui, float pt, int mx, int my) {
      gui.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -13421773);
      gui.fill(this.leftPos - 42, this.topPos + 62, this.leftPos - 2, this.topPos + 142, -14540254);
      gui.renderOutline(this.leftPos - 42, this.topPos + 62, 40, 80, -16777216);

      for (Slot slot : ((KitEditorMenu)this.menu).slots) {
         gui.fill(
            this.leftPos + slot.x - 1,
            this.topPos + slot.y - 1,
            this.leftPos + slot.x + 17,
            this.topPos + slot.y + 17,
            -16777216
         );
         gui.fill(
            this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, -7631989
         );
      }
   }
}

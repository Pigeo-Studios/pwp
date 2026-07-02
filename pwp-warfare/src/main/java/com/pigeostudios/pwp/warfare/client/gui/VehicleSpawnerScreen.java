package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.menu.VehicleSpawnerMenu;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketUpdateSpawner;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Экран настройки спавнера техники
// Позволяет задать ID техники, время возрождения, начальную задержку и угол
public class VehicleSpawnerScreen extends AbstractContainerScreen<VehicleSpawnerMenu> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/spawner_gui.png");
   private EditBox vehicleIdField;
   private EditBox respawnTimeField;
   private EditBox initialTimeField;
   private EditBox yawField;

   public VehicleSpawnerScreen(VehicleSpawnerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 196;
      this.imageHeight = 245;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   protected void init() {
      super.init();
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      this.yawField = new EditBox(this.font, x + 35, y + 40, 40, 14, Component.translatable("gui.pwpwarfare.vehicle_spawner.yaw"));
      this.yawField.setValue(String.valueOf((int)((VehicleSpawnerMenu)this.menu).blockEntity.vehicleYaw));
      this.yawField.setFilter(s -> s.matches("-?\\d*"));
      this.yawField.setResponder(val -> {
         try {
            ((VehicleSpawnerMenu)this.menu).blockEntity.vehicleYaw = Float.parseFloat(val);
            this.sendUpdatePacket();
         } catch (Exception var3x) {
         }
      });
      this.addRenderableWidget(this.yawField);
      this.vehicleIdField = new EditBox(this.font, x + 35, y + 20, 80, 14, Component.translatable("gui.pwpwarfare.vehicle_spawner.vehicle_id"));
      this.vehicleIdField.setMaxLength(64);
      this.vehicleIdField.setValue(((VehicleSpawnerMenu)this.menu).blockEntity.vehicleIdString);
      this.vehicleIdField.setBordered(true);
      this.vehicleIdField.setResponder(val -> this.sendUpdatePacket());
      this.addRenderableWidget(this.vehicleIdField);
      int rightCenterX = x + 155;
      int btnY_Respawn = y + 18;
      int btnY_Initial = y + 48;
      this.addRenderableWidget(
         Button.builder(Component.literal("-"), b -> this.adjustTimer(false, -5)).bounds(rightCenterX - 42, btnY_Respawn, 15, 16).build()
      );
      this.respawnTimeField = new EditBox(this.font, rightCenterX - 25, btnY_Respawn + 1, 46, 14, Component.translatable("gui.pwpwarfare.vehicle_spawner.respawn_time"));
      this.respawnTimeField.setValue(String.valueOf(((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings));
      this.respawnTimeField.setResponder(val -> this.onTimeFieldChanged(val, false));
      this.addRenderableWidget(this.respawnTimeField);
      this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.adjustTimer(false, 5)).bounds(rightCenterX + 23, btnY_Respawn, 15, 16).build());
      this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.adjustTimer(true, -5)).bounds(rightCenterX - 42, btnY_Initial, 15, 16).build());
      this.initialTimeField = new EditBox(this.font, rightCenterX - 25, btnY_Initial + 1, 46, 14, Component.translatable("gui.pwpwarfare.vehicle_spawner.initial_time"));
      this.initialTimeField.setValue(String.valueOf(((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings));
      this.initialTimeField.setResponder(val -> this.onTimeFieldChanged(val, true));
      this.addRenderableWidget(this.initialTimeField);
      this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.adjustTimer(true, 5)).bounds(rightCenterX + 23, btnY_Initial, 15, 16).build());
      this.addRenderableWidget(Button.builder(Component.literal("↺"), b -> this.adjustYaw(-45.0F)).bounds(x + 77, y + 40, 18, 14).build());
      this.addRenderableWidget(Button.builder(Component.literal("↻"), b -> this.adjustYaw(45.0F)).bounds(x + 97, y + 40, 18, 14).build());
   }

   private void onTimeFieldChanged(String value, boolean isInitial) {
      if (!value.isEmpty()) {
         try {
            int time = Integer.parseInt(value);
            if (time < 0) {
               time = 0;
            }

            if (isInitial) {
               ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings = time;
            } else {
               ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings = time;
            }

            this.sendUpdatePacket();
         } catch (NumberFormatException var4) {
         }
      }
   }

   private void adjustTimer(boolean isInitial, int change) {
      if (isInitial) {
         int newVal = Math.max(0, ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings + change);
         ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings = newVal;
         this.initialTimeField.setValue(String.valueOf(newVal));
      } else {
         int newVal = Math.max(0, ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings + change);
         ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings = newVal;
         this.respawnTimeField.setValue(String.valueOf(newVal));
      }

      this.sendUpdatePacket();
   }

   private void sendUpdatePacket() {
      if (((VehicleSpawnerMenu)this.menu).blockEntity.vehicleIdString == null) {
         ((VehicleSpawnerMenu)this.menu).blockEntity.vehicleIdString = "";
      }

      PacketHandler.INSTANCE
         .sendToServer(
            new PacketUpdateSpawner(
               ((VehicleSpawnerMenu)this.menu).blockEntity.getBlockPos(),
               ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings,
               ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings,
               this.vehicleIdField.getValue(),
               ((VehicleSpawnerMenu)this.menu).blockEntity.vehicleYaw
            )
         );
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
      super.render(gui, mouseX, mouseY, partialTick);
      this.renderTooltip(gui, mouseX, mouseY);
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      gui.drawString(this.font, "ID:", x + 10, y + 23, 11184810, false);
      gui.drawCenteredString(this.font, "Mod", x + 23, y + 35, 5636095);
      int txtX = x + 155;
      gui.drawCenteredString(this.font, "Respawn (s)", txtX, y + 8, 11184810);
      gui.drawCenteredString(this.font, "Initial (s)", txtX, y + 38, 11184810);
      gui.drawString(this.font, "Items (32)", x + 26, y + 64, 5635925, false);
   }

   protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      gui.drawString(this.font, "Yaw:", x + 10, y + 43, 11184810, false);
      gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
      gui.blit(TEXTURE, x + 14, y + 44, 79, 19, 18, 18);
      RenderSystem.disableBlend();
   }

   private void adjustYaw(float amount) {
      float current = 0.0F;

      try {
         current = Float.parseFloat(this.yawField.getValue());
      } catch (Exception var4) {
      }

      float next = (current + amount) % 360.0F;
      if (next < 0.0F) {
         next += 360.0F;
      }

      this.yawField.setValue(String.valueOf((int)next));
   }
}

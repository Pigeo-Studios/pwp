package com.pigeostudios.pwp.warfare.menu;

import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

// Меню спавнера техники
// Отображает инвентарь техники и позволяет его редактировать
public class VehicleSpawnerMenu extends AbstractContainerMenu {
   public final VehicleSpawnerBlockEntity blockEntity;

   public VehicleSpawnerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
      this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
   }

   public VehicleSpawnerMenu(int id, Inventory inv, BlockEntity entity) {
      super((MenuType)ModMenuTypes.VEHICLE_SPAWNER_MENU.get(), id);
      this.blockEntity = (VehicleSpawnerBlockEntity)entity;
      this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 0, 15, 45));
      int gridStartX = 26;
      int gridStartY = 75;

      for (int row = 0; row < 4; row++) {
         for (int col = 0; col < 8; col++) {
            this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 1 + col + row * 8, gridStartX + col * 18, gridStartY + row * 18));
         }
      }

      this.addPlayerInventory(inv, 160);
      this.addPlayerHotbar(inv, 218);
   }

   private void addPlayerInventory(Inventory playerInventory, int startY) {
      for (int i = 0; i < 3; i++) {
         for (int l = 0; l < 9; l++) {
            this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 17 + l * 18, startY + i * 18));
         }
      }
   }

   private void addPlayerHotbar(Inventory playerInventory, int startY) {
      for (int i = 0; i < 9; i++) {
         this.addSlot(new Slot(playerInventory, i, 17 + i * 18, startY));
      }
   }

   public ItemStack quickMoveStack(Player playerIn, int index) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      return stillValid(
         ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos()), player, (Block)ModBlocks.VEHICLE_SPAWNER_BLOCK.get()
      );
   }
}

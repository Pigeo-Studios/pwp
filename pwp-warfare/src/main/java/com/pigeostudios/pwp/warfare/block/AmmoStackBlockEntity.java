package com.pigeostudios.pwp.warfare.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Сущность блока стопки боеприпасов (общая для АГС и М2)
// Хранит список количеств патронов в каждой коробке
public class AmmoStackBlockEntity extends BlockEntity {
   // Список количества патронов в каждой коробке
   private final List<Integer> ammoCounts = new ArrayList<>();

   public AmmoStackBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.AMMO_STACK_BE.get(), pos, state);
   }

   // Добавляет коробку с патронами (макс. 4)
   public void addAmmoBox(int amount) {
      if (this.ammoCounts.size() < 4) {
         this.ammoCounts.add(amount);
         this.setChanged();
         if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
         }
      }
   }

   // Удаляет и возвращает верхнюю коробку с патронами
   public int removeTopAmmoBox() {
      if (!this.ammoCounts.isEmpty()) {
         int amount = this.ammoCounts.remove(this.ammoCounts.size() - 1);
         this.setChanged();
         if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
         }

         return amount;
      } else {
         return 0;
      }
   }

   // Возвращает копию списка количеств патронов
   public List<Integer> getAmmoCounts() {
      return new ArrayList<>(this.ammoCounts);
   }

   // Очищает все коробки с патронами
   public void clear() {
      this.ammoCounts.clear();
      this.setChanged();
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putIntArray("AmmoCounts", this.ammoCounts.stream().mapToInt(i -> i).toArray());
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.ammoCounts.clear();
      if (tag.contains("AmmoCounts")) {
         int[] loadedArray = tag.getIntArray("AmmoCounts");

         for (int i : loadedArray) {
            this.ammoCounts.add(i);
         }
      }
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag);
      return tag;
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}

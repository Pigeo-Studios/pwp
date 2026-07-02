package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Сущность блока стены
// Управляет прогрессом строительства и связями между соседними стенами
public class WallBlockEntity extends BlockEntity {
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";
   private List<BlockPos> linkedWalls = new ArrayList<>();
   private boolean isMultiWall = false;
   private boolean isUpdatingLinked = false;

   public WallBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.WALL_BE.get(), pos, state);
   }

   // Возвращает максимальный прогресс в зависимости от количества связанных стен
   public int getMaxProgress() {
      int size = this.linkedWalls.size();
      if (size > 5) {
         return 1200;
      } else {
         return size <= 0 && !this.isMultiWall ? 300 : 600;
      }
   }

   public void setTeam(String team) {
      this.teamOwner = team;
      this.setChanged();
   }

   // Устанавливает список связанных стен (мульти-стена)
   public void setLinkedWalls(List<BlockPos> links) {
      this.linkedWalls = new ArrayList<>(links);
      this.isMultiWall = true;
      this.setChanged();
   }

   public String getTeam() {
      return this.teamOwner;
   }

   public float getPercentage() {
      return (float)this.currentProgress / this.getMaxProgress();
   }

   public void addProgress() {
      if (!this.isUpdatingLinked) {
         this.performAddProgress();
         this.propagateToLinks(false, 0);
      }
   }

   public void addCreativeProgress(int amount) {
      if (!this.isUpdatingLinked) {
         this.performCreativeAdd(amount);
         this.propagateToLinks(true, amount);
      }
   }

   // Распространяет прогресс строительства на связанные стены
   private void propagateToLinks(boolean isCreative, int amount) {
      if (this.isMultiWall && !this.linkedWalls.isEmpty() && this.level != null) {
         this.isUpdatingLinked = true;

         for (BlockPos linkPos : this.linkedWalls) {
            if (!linkPos.equals(this.worldPosition) && this.level.isLoaded(linkPos) && this.level.getBlockEntity(linkPos) instanceof WallBlockEntity linkedWall) {
               if (isCreative) {
                  linkedWall.performCreativeAdd(amount);
               } else {
                  linkedWall.performAddProgress();
               }
            }
         }

         this.isUpdatingLinked = false;
      }
   }

   private void performAddProgress() {
      if (this.currentProgress < this.getMaxProgress()) {
         this.activeDiggers++;
         this.setChanged();
      }
   }

   private void performCreativeAdd(int amount) {
      if (this.currentProgress < this.getMaxProgress()) {
         this.currentProgress += amount;
         if (this.currentProgress >= this.getMaxProgress()) {
            this.currentProgress = this.getMaxProgress();
         }

         this.setChanged();
      }
   }

   // Тик строительства - обновляет прогресс, завершает по достижении максимума
   public static void tick(Level level, BlockPos pos, BlockState state, WallBlockEntity entity) {
      if (!level.isClientSide) {
         if (!(Boolean)state.getValue(WallBlock.CONSTRUCTED)) {
            if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
               if (entity.activeDiggers > 0) {
                  float speed;
                  if (entity.activeDiggers == 1) {
                     speed = 1.0F;
                  } else if (entity.activeDiggers == 2) {
                     speed = 1.34F;
                  } else if (entity.activeDiggers == 3) {
                     speed = 2.0F;
                  } else {
                     speed = 4.0F;
                  }

                  float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                  speed *= multiplier;
                  entity.currentProgress = entity.currentProgress + (int)Math.ceil(speed);
               }

               int max = entity.getMaxProgress();
               if (entity.currentProgress >= max) {
                  entity.currentProgress = max;
                  level.setBlock(pos, (BlockState)state.setValue(WallBlock.CONSTRUCTED, true), 3);
                  if (!level.isClientSide) {
                     ((ServerLevel)level)
                        .sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 25, 0.6, 0.4, 0.6, 0.05);
                  }
               }

               if (level.getGameTime() % 5L == 0L || entity.currentProgress >= max) {
                  level.sendBlockUpdated(pos, state, state, 3);
               }
            }

            entity.activeDiggers = 0;
         }
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("BuildProgress", this.currentProgress);
      tag.putString("TeamOwner", this.teamOwner);
      tag.putBoolean("IsMultiWall", this.isMultiWall);
      ListTag list = new ListTag();

      for (BlockPos p : this.linkedWalls) {
         list.add(LongTag.valueOf(p.asLong()));
      }

      tag.put("LinkedWalls", list);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.currentProgress = tag.getInt("BuildProgress");
      if (tag.contains("TeamOwner")) {
         this.teamOwner = tag.getString("TeamOwner");
      }

      if (tag.contains("IsMultiWall")) {
         this.isMultiWall = tag.getBoolean("IsMultiWall");
      }

      this.linkedWalls.clear();
      if (tag.contains("LinkedWalls")) {
         for (Tag t : tag.getList("LinkedWalls", 4)) {
            this.linkedWalls.add(BlockPos.of(((LongTag)t).getAsLong()));
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

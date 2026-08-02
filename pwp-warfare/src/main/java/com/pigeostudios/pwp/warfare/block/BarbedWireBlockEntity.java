package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
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

// Сущность блока колючей проволоки
// Управляет прогрессом строительства, демонтажа и связями между соседними блоками проволоки
public class BarbedWireBlockEntity extends BlockEntity {
   // Максимальный прогресс строительства (1200 тиков)
   public static final int MAX_PROGRESS = 1200;
   // Текущий прогресс строительства
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";
   private List<BlockPos> linkedWires = new ArrayList<>();
   private boolean isMultiWire = false;
   private boolean isUpdatingLinked = false;
   private int dismantleProgress = 0;
   private float dismantleMultiplier = 1.0F;

   public BarbedWireBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.WIRE_BE.get(), pos, state);
   }

   public void setTeam(String team) {
      this.teamOwner = team;
      this.setChanged();
   }

   // Устанавливает список связанных блоков проволоки (мульти-проволока)
   public void setLinkedWires(List<BlockPos> links) {
      this.linkedWires = new ArrayList<>(links);
      this.isMultiWire = true;
      this.setChanged();
   }

   public String getTeam() {
      return this.teamOwner;
   }

   public float getPercentage() {
      boolean constructed = this.level != null && this.level.getBlockState(this.worldPosition).hasProperty(BarbedWireBlock.CONSTRUCTED)
         && (Boolean)this.level.getBlockState(this.worldPosition).getValue(BarbedWireBlock.CONSTRUCTED);
      if (constructed && this.dismantleProgress > 0) {
         return (float)this.dismantleProgress / 1200.0F;
      }
      if (constructed) {
         return 1.0F;
      }
      return this.currentProgress / 1200.0F;
   }

   public boolean isDismantling() {
      return this.dismantleProgress > 0;
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

   public void addDismantleProgress(boolean isEnemy) {
      if (this.dismantleProgress < 1200) {
         this.activeDiggers++;
         this.dismantleMultiplier = isEnemy ? 0.5F : 1.0F;
         if (!this.isUpdatingLinked) {
            this.propagateDismantleToLinks(isEnemy);
         }
         this.setChanged();
      }
   }

   public void addCreativeDismantleProgress() {
      this.dismantleProgress = Math.min(this.dismantleProgress + 50, 1200);
      if (!this.isUpdatingLinked) {
         this.propagateCreativeDismantleToLinks();
      }
      this.setChanged();
   }

   private void propagateCreativeDismantleToLinks() {
      if (this.isMultiWire && !this.linkedWires.isEmpty() && this.level != null) {
         for (BlockPos linkPos : this.linkedWires) {
            if (!linkPos.equals(this.worldPosition) && this.level.isLoaded(linkPos) && this.level.getBlockEntity(linkPos) instanceof BarbedWireBlockEntity linkedWire) {
               linkedWire.dismantleProgress = Math.min(linkedWire.dismantleProgress + 50, 1200);
               linkedWire.setChanged();
            }
         }
      }
   }

   // Распространяет прогресс строительства на связанные блоки проволоки
   private void propagateToLinks(boolean isCreative, int amount) {
      if (this.isMultiWire && !this.linkedWires.isEmpty() && this.level != null) {
         this.isUpdatingLinked = true;

         for (BlockPos linkPos : this.linkedWires) {
            if (!linkPos.equals(this.worldPosition) && this.level.isLoaded(linkPos) && this.level.getBlockEntity(linkPos) instanceof BarbedWireBlockEntity linkedWire
               )
             {
               if (isCreative) {
                  linkedWire.performCreativeAdd(amount);
               } else {
                  linkedWire.performAddProgress();
               }
            }
         }

         this.isUpdatingLinked = false;
      }
   }

   private void performAddProgress() {
      if (this.currentProgress < 1200) {
         this.activeDiggers++;
         this.setChanged();
      }
   }

   private void performCreativeAdd(int amount) {
      this.currentProgress += amount;
      if (this.currentProgress >= 1200) {
         this.currentProgress = 1200;
      }

      this.setChanged();
   }

   private void propagateDismantleToLinks(boolean isEnemy) {
      if (this.isMultiWire && !this.linkedWires.isEmpty() && this.level != null) {
         for (BlockPos linkPos : this.linkedWires) {
            if (!linkPos.equals(this.worldPosition) && this.level.isLoaded(linkPos) && this.level.getBlockEntity(linkPos) instanceof BarbedWireBlockEntity linkedWire) {
               linkedWire.activeDiggers++;
               linkedWire.dismantleMultiplier = isEnemy ? 0.5F : 1.0F;
               linkedWire.setChanged();
            }
         }
      }
   }

   // Тик строительства и демонтажа - обновляет прогресс, завершает по достижении MAX_PROGRESS
   public static void tick(Level level, BlockPos pos, BlockState state, BarbedWireBlockEntity entity) {
      if (!level.isClientSide) {
          boolean constructed = (Boolean)state.getValue(BarbedWireBlock.CONSTRUCTED);
          if (!constructed) {
             if (state.getValue(BarbedWireBlock.BUILD_STAGE) == 0) {
                level.setBlock(pos, (BlockState)state.setValue(BarbedWireBlock.BUILD_STAGE, 1), 3);
             }

             if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
                if (entity.activeDiggers > 0) {
                   float speed = entity.activeDiggers >= 3 ? 2.0F : 1.0F;
                   float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                   speed *= multiplier;
                   entity.currentProgress = entity.currentProgress + (int)Math.ceil(speed);
                }

                if (entity.currentProgress >= 1200) {
                   entity.currentProgress = 1200;
                   level.setBlock(pos, (BlockState)((BlockState)state.setValue(BarbedWireBlock.CONSTRUCTED, true)).setValue(BarbedWireBlock.BUILD_STAGE, 2), 3);
                   if (!level.isClientSide) {
                      ((ServerLevel)level)
                         .sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.5, 0.3, 0.5, 0.03);
                   }
                } else {
                   int newStage = entity.currentProgress >= 600 ? 2 : 1;
                   if (state.getValue(BarbedWireBlock.BUILD_STAGE) != newStage) {
                      level.setBlock(pos, (BlockState)state.setValue(BarbedWireBlock.BUILD_STAGE, newStage), 3);
                   }
                }

                if (level.getGameTime() % 5L == 0L || entity.currentProgress >= 1200) {
                   level.sendBlockUpdated(pos, state, state, 3);
                }
             }

             entity.activeDiggers = 0;
          } else {
            if (entity.activeDiggers > 0 || entity.dismantleProgress > 0) {
               if (entity.activeDiggers > 0) {
                  float speed = entity.activeDiggers >= 3 ? 2.0F : 1.0F;
                  float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                  speed *= multiplier;
                  speed *= entity.dismantleMultiplier;
                  entity.dismantleProgress = entity.dismantleProgress + (int)Math.ceil(speed);
                  entity.setChanged();
               }

               if (entity.dismantleProgress >= 1200) {
                  entity.dismantleProgress = 1200;
                  entity.handleDismantleComplete(level, pos, state);
               }

               if (level.getGameTime() % 5L == 0L || entity.dismantleProgress >= 1200) {
                  level.sendBlockUpdated(pos, state, state, 3);
               }
            }

            entity.activeDiggers = 0;
            entity.dismantleMultiplier = 1.0F;
         }
      }
   }

   private void handleDismantleComplete(Level level, BlockPos pos, BlockState state) {
      if (level.getBlockState(pos).isAir()) return;
      if (this.isMultiWire && !this.linkedWires.isEmpty()) {
         for (BlockPos linkPos : this.linkedWires) {
            if (!linkPos.equals(this.worldPosition) && level.getBlockEntity(linkPos) instanceof BarbedWireBlockEntity linked) {
               linked.dismantleProgress = 1200;
               level.destroyBlock(linkPos, false);
            }
         }
      }

      this.returnMaterials(level, pos);
      level.destroyBlock(pos, false);
   }

   private void returnMaterials(Level level, BlockPos pos) {
      if (this.dismantleMultiplier >= 1.0F && level instanceof ServerLevel serverLevel) {
         int refund = 12;
         WarfareWorldData data = WarfareWorldData.get(serverLevel);
         BlockPos nearestHub = null;
         double nearestDist = Double.MAX_VALUE;

         for (WarfareWorldData.HubInfo h : data.hubs) {
            if (h.team.equalsIgnoreCase(this.teamOwner) && level.getBlockEntity(h.pos) instanceof HubBlockEntity hub) {
               double dist = h.pos.distSqr(pos);
               if (dist < nearestDist) {
                  nearestDist = dist;
                  nearestHub = h.pos;
               }
            }
         }

         if (nearestHub != null && level.getBlockEntity(nearestHub) instanceof HubBlockEntity hub) {
            hub.addMaterials(refund);
         }
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("BuildProgress", this.currentProgress);
      tag.putString("TeamOwner", this.teamOwner);
      tag.putBoolean("IsMultiWire", this.isMultiWire);
      tag.putInt("DismantleProgress", this.dismantleProgress);
      ListTag list = new ListTag();

      for (BlockPos p : this.linkedWires) {
         list.add(LongTag.valueOf(p.asLong()));
      }

      tag.put("LinkedWires", list);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.currentProgress = tag.getInt("BuildProgress");
      if (tag.contains("TeamOwner")) {
         this.teamOwner = tag.getString("TeamOwner");
      }

      if (tag.contains("IsMultiWire")) {
         this.isMultiWire = tag.getBoolean("IsMultiWire");
      }

      if (tag.contains("DismantleProgress")) {
         this.dismantleProgress = tag.getInt("DismantleProgress");
      }

      this.linkedWires.clear();
      if (tag.contains("LinkedWires")) {
         for (Tag t : tag.getList("LinkedWires", 4)) {
            this.linkedWires.add(BlockPos.of(((LongTag)t).getAsLong()));
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

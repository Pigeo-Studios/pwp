package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

// Сущность блока стены
// Управляет прогрессом строительства, демонтажа и связями между соседними стенами
public class WallBlockEntity extends BlockEntity {
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";
   private List<BlockPos> linkedWalls = new ArrayList<>();
   private boolean isMultiWall = false;
   private boolean isUpdatingLinked = false;
   private int dismantleProgress = 0;
   private float dismantleMultiplier = 1.0F;
   // Блок, в который стена превратится после достройки (например, камо-нет входа бункера)
   private String transformTo = "";
   private int transformDir = 2;

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

   // Задаёт блок, в который стена превратится после завершения строительства
   public void setTransformTo(String blockId, int directionIndex) {
      this.transformTo = blockId;
      this.transformDir = directionIndex;
      this.setChanged();
   }

   public float getPercentage() {
      boolean constructed = this.level != null && this.level.getBlockState(this.worldPosition).hasProperty(WallBlock.CONSTRUCTED)
         && (Boolean)this.level.getBlockState(this.worldPosition).getValue(WallBlock.CONSTRUCTED);
      if (constructed && this.dismantleProgress > 0) {
         return (float)this.dismantleProgress / this.getMaxProgress();
      }
      if (constructed) {
         return 1.0F;
      }
      return (float)this.currentProgress / this.getMaxProgress();
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
      if (this.dismantleProgress < this.getMaxProgress()) {
         this.activeDiggers++;
         this.dismantleMultiplier = isEnemy ? 0.5F : 1.0F;
         if (!this.isUpdatingLinked) {
            this.propagateDismantleToLinks(isEnemy);
         }
         this.setChanged();
      }
   }

   public void addCreativeDismantleProgress() {
      this.dismantleProgress = Math.min(this.dismantleProgress + 50, this.getMaxProgress());
      if (!this.isUpdatingLinked) {
         this.propagateCreativeDismantleToLinks();
      }
      this.setChanged();
   }

   private void propagateCreativeDismantleToLinks() {
      if (this.isMultiWall && !this.linkedWalls.isEmpty() && this.level != null) {
         for (BlockPos linkPos : this.linkedWalls) {
            if (!linkPos.equals(this.worldPosition) && this.level.isLoaded(linkPos) && this.level.getBlockEntity(linkPos) instanceof WallBlockEntity linkedWall) {
               linkedWall.dismantleProgress = Math.min(linkedWall.dismantleProgress + 50, linkedWall.getMaxProgress());
               linkedWall.setChanged();
            }
         }
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

   private void propagateDismantleToLinks(boolean isEnemy) {
      if (this.isMultiWall && !this.linkedWalls.isEmpty() && this.level != null) {
         for (BlockPos linkPos : this.linkedWalls) {
            if (!linkPos.equals(this.worldPosition) && this.level.isLoaded(linkPos) && this.level.getBlockEntity(linkPos) instanceof WallBlockEntity linkedWall) {
               linkedWall.activeDiggers++;
               linkedWall.dismantleMultiplier = isEnemy ? 0.5F : 1.0F;
               linkedWall.setChanged();
            }
         }
      }
   }

   // Тик строительства и демонтажа - обновляет прогресс, завершает по достижении максимума
   public static void tick(Level level, BlockPos pos, BlockState state, WallBlockEntity entity) {
      if (!level.isClientSide) {
          boolean constructed = (Boolean)state.getValue(WallBlock.CONSTRUCTED);
          if (!constructed) {
             if (state.getValue(WallBlock.BUILD_STAGE) == 0) {
                level.setBlock(pos, (BlockState)state.setValue(WallBlock.BUILD_STAGE, 1), 3);
             }

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
                   if (!entity.transformTo.isEmpty()) {
                      Block target = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entity.transformTo));
                      if (target != null) {
                         BlockState finalState = target.defaultBlockState();
                         if (finalState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                            finalState = finalState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from2DDataValue(entity.transformDir));
                         }
                         level.setBlock(pos, finalState, 3);
                      }
                   } else {
                      level.setBlock(pos, (BlockState)((BlockState)state.setValue(WallBlock.CONSTRUCTED, true)).setValue(WallBlock.BUILD_STAGE, 2), 3);
                   }

                   if (!level.isClientSide) {
                      ((ServerLevel)level)
                         .sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 25, 0.6, 0.4, 0.6, 0.05);
                   }
                } else {
                   int newStage = entity.currentProgress >= max / 2 ? 2 : 1;
                   if (state.getValue(WallBlock.BUILD_STAGE) != newStage) {
                      level.setBlock(pos, (BlockState)state.setValue(WallBlock.BUILD_STAGE, newStage), 3);
                   }
                }

                if (level.getGameTime() % 5L == 0L || entity.currentProgress >= max) {
                   level.sendBlockUpdated(pos, state, state, 3);
                }
             }

             entity.activeDiggers = 0;
          } else {
            if (entity.activeDiggers > 0 || entity.dismantleProgress > 0) {
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
                  speed *= entity.dismantleMultiplier;
                  entity.dismantleProgress = entity.dismantleProgress + (int)Math.ceil(speed);
                  entity.setChanged();
               }

               int max = entity.getMaxProgress();
               if (entity.dismantleProgress >= max) {
                  entity.dismantleProgress = max;
                  entity.handleDismantleComplete(level, pos, state);
               }

               if (level.getGameTime() % 5L == 0L || entity.dismantleProgress >= max) {
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
      if (this.isMultiWall && !this.linkedWalls.isEmpty()) {
         for (BlockPos linkPos : this.linkedWalls) {
            if (!linkPos.equals(this.worldPosition) && level.getBlockEntity(linkPos) instanceof WallBlockEntity linked) {
               linked.dismantleProgress = linked.getMaxProgress();
               level.destroyBlock(linkPos, false);
            }
         }
      }

      this.returnMaterials(level, pos);
      level.destroyBlock(pos, false);
   }

   private void returnMaterials(Level level, BlockPos pos) {
      if (this.dismantleMultiplier >= 1.0F && level instanceof ServerLevel serverLevel) {
         int refund = this.getRefundAmount();
         if (refund <= 0) return;

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

   private int getRefundAmount() {
      if (this.isMultiWall && !this.linkedWalls.isEmpty()) {
         int totalBlocks = this.linkedWalls.size();
         if (totalBlocks >= 9) return 7;
         if (totalBlocks >= 4) return 5;
      }
      return 2;
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("BuildProgress", this.currentProgress);
      tag.putString("TeamOwner", this.teamOwner);
      tag.putBoolean("IsMultiWall", this.isMultiWall);
      tag.putInt("DismantleProgress", this.dismantleProgress);
      tag.putString("TransformTo", this.transformTo);
      tag.putInt("TransformDir", this.transformDir);
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

      if (tag.contains("DismantleProgress")) {
         this.dismantleProgress = tag.getInt("DismantleProgress");
      }

      if (tag.contains("TransformTo")) {
         this.transformTo = tag.getString("TransformTo");
      }

      if (tag.contains("TransformDir")) {
         this.transformDir = tag.getInt("TransformDir");
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

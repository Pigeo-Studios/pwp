package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Сущность блюпринта РЭБ. Прогресс копания: большой РЭБ ~25с (500 тиков),
// мини ~12.5с (250 тиков) — значения из WarfareConfig. По завершении
// блок спавнит сущность uncomplicatedfpv:reb / reb_mini.
public class RebConstructionBlockEntity extends BlockEntity {
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";

   public RebConstructionBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.REB_CONSTRUCTION_BE.get(), pos, state);
   }

   private int maxProgress() {
      boolean mini = this.getBlockState().hasProperty(RebConstructionBlock.MINI)
         && (Boolean)this.getBlockState().getValue(RebConstructionBlock.MINI);
      return mini ? (Integer)WarfareConfig.REB_MINI_BUILD_TIME_TICKS.get() : (Integer)WarfareConfig.REB_BUILD_TIME_TICKS.get();
   }

   public void setTeam(String team) {
      this.teamOwner = team;
      this.setChanged();
   }

   public String getTeam() {
      return this.teamOwner;
   }

   public float getPercentage() {
      int max = this.maxProgress();
      return max <= 0 ? 1.0F : this.currentProgress / (float)max;
   }

   public void addProgress() {
      if (this.currentProgress < this.maxProgress()) {
         this.activeDiggers++;
         this.setChanged();
      }
   }

   public void addCreativeProgress(int amount) {
      this.currentProgress += amount;
      int max = this.maxProgress();
      if (this.currentProgress >= max) {
         this.currentProgress = max;
      }
      this.setChanged();
   }

   // Тик строительства: прогресс от копающих игроков, завершение -> спавн РЭБ.
   public static void tick(Level level, BlockPos pos, BlockState state, RebConstructionBlockEntity entity) {
      if (!level.isClientSide) {
         if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            if (entity.activeDiggers > 0) {
               float speed = entity.activeDiggers >= 2 ? 2.0F : 1.0F;
               entity.currentProgress = entity.currentProgress + (int)Math.ceil(speed);
            }

            if (entity.currentProgress >= entity.maxProgress() && state.getBlock() instanceof RebConstructionBlock block) {
               ((ServerLevel)level).sendParticles(
                  ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 25, 0.6, 0.4, 0.6, 0.05
               );
               block.finishConstruction((ServerLevel)level, pos, state);
            }

            if (level.getGameTime() % 10L == 0L) {
               level.sendBlockUpdated(pos, state, state, 3);
            }
         }

         entity.activeDiggers = 0;
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("BuildProgress", this.currentProgress);
      tag.putString("TeamOwner", this.teamOwner);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.currentProgress = tag.getInt("BuildProgress");
      if (tag.contains("TeamOwner")) {
         this.teamOwner = tag.getString("TeamOwner");
      }
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag);
      return tag;
   }
}

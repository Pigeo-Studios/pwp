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

// Сущность блока строительства TOW
// Отслеживает прогресс строительства и количество копающих игроков
public class TOWConstructionBlockEntity extends BlockEntity {
   // Максимальный прогресс строительства (2400 тиков)
   public static final int MAX_PROGRESS = 2400;
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";

   public TOWConstructionBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.TOW_CONSTRUCTION_BE.get(), pos, state);
   }

   public void setTeam(String team) {
      this.teamOwner = team;
      this.setChanged();
   }

   public String getTeam() {
      return this.teamOwner;
   }

   public float getPercentage() {
      return this.currentProgress / 2400.0F;
   }

   public void addProgress() {
      if (this.currentProgress < 2400) {
         this.activeDiggers++;
         this.setChanged();
      }
   }

   public void addCreativeProgress(int amount) {
      this.currentProgress += amount;
      if (this.currentProgress >= 2400) {
         this.currentProgress = 2400;
      }

      this.setChanged();
   }

   // Тик строительства - обновляет прогресс, завершает по достижении MAX_PROGRESS
   public static void tick(Level level, BlockPos pos, BlockState state, TOWConstructionBlockEntity entity) {
      if (!level.isClientSide) {
         if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            if (entity.activeDiggers > 0) {
               float speed = entity.activeDiggers >= 2 ? 2.0F : 1.0F;
               float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
               speed *= multiplier;
               entity.currentProgress = entity.currentProgress + (int)Math.ceil(speed);
            }

            if (entity.currentProgress >= 2400 && state.getBlock() instanceof TOWConstructionBlock block) {
               ((ServerLevel)level)
                  .sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.7, 0.4, 0.7, 0.05);
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

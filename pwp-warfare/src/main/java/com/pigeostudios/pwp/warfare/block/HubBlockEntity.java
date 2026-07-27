package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

// Сущность блока передовой операционной базы
// Управляет прогрессом строительства, ресурсами и кулдаунами спауна техники
public class HubBlockEntity extends BlockEntity {
   // Максимальный прогресс строительства (2400 тиков)
   public static final int MAX_PROGRESS = 2400;
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";
   // Количество строительных материалов (до 3000)
   private int constructionMaterials = 200;
   // Кулдаун спауна АГС-30 (тики)
   public int cooldownAGS = 0;
   // Кулдаун спауна M2 Browning (тики)
   public int cooldownM2 = 0;
   // Кулдаун спауна миномёта (тики)
   public int cooldownMortar = 0;
   // Кулдаун спауна TOW (тики)
   public int cooldownTOW = 0;
   public boolean wasDismantled = false;
   private int dismantleProgress = 0;
   private float dismantleMultiplier = 1.0F;
   private Object clientSoundRef = null;

   public HubBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.HUB_BE.get(), pos, state);
   }

   public void addProgress() {
      if (this.currentProgress < 2400) {
         this.activeDiggers++;
      }
   }

   public void addCreativeProgress(int amount) {
      if (this.currentProgress < 2400) {
         this.currentProgress += amount;
         if (this.currentProgress >= 2400) {
            this.currentProgress = 2400;
         }
      }
   }

   public void addDismantleProgress(boolean isEnemy) {
      if (this.dismantleProgress < 2400) {
         this.activeDiggers++;
         this.dismantleMultiplier = isEnemy ? 0.5F : 1.0F;
         this.setChanged();
      }
   }

   public void addCreativeDismantleProgress() {
      this.dismantleProgress = Math.min(this.dismantleProgress + 50, 2400);
      this.setChanged();
   }

   public void setTeam(String team) {
      this.teamOwner = team;
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public String getTeam() {
      return this.teamOwner;
   }

   // Устанавливает кулдаун для указанного типа техники (1=AGS, 2=M2, 3=Mortar, 4=TOW)
   public void setCooldown(int type, int ticks) {
      if (type == 1) {
         this.cooldownAGS = ticks;
      }

      if (type == 2) {
         this.cooldownM2 = ticks;
      }

      if (type == 3) {
         this.cooldownMortar = ticks;
      }

      if (type == 4) {
         this.cooldownTOW = ticks;
      }

      this.setChanged();
   }

   // Возвращает количество строительных материалов
   public int getMaterials() {
      return this.constructionMaterials;
   }

   // Тратит указанное количество материалов на постройку
   public void consumeMaterials(int amount) {
      this.constructionMaterials = Math.max(0, this.constructionMaterials - amount);
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.syncHubMaterials();
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public void addMaterials(int amount) {
      this.constructionMaterials += amount;
      if (this.constructionMaterials > 3000) {
         this.constructionMaterials = 3000;
      }

      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.syncHubMaterials();
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   private void syncHubMaterials() {
      if (this.level instanceof ServerLevel serverLevel) {
         WarfareWorldData data = WarfareWorldData.get(serverLevel);
         for (WarfareWorldData.HubInfo h : data.hubs) {
            if (h.pos.equals(this.worldPosition)) {
               h.materials = this.constructionMaterials;
               data.setDirty();
               break;
            }
         }
      }
   }

   // Тик хаба: обновляет кулдауны, прогресс строительства и звуки на клиенте
   public static void tick(Level level, BlockPos pos, BlockState state, HubBlockEntity entity) {
      boolean needsSync = false;
      if (entity.cooldownAGS > 0) {
         entity.cooldownAGS--;
         if (entity.cooldownAGS == 0) {
            needsSync = true;
         }
      }

      if (entity.cooldownM2 > 0) {
         entity.cooldownM2--;
         if (entity.cooldownM2 == 0) {
            needsSync = true;
         }
      }

      if (entity.cooldownMortar > 0) {
         entity.cooldownMortar--;
         if (entity.cooldownMortar == 0) {
            needsSync = true;
         }
      }

      if (entity.cooldownTOW > 0) {
         entity.cooldownTOW--;
         if (entity.cooldownTOW == 0) {
            needsSync = true;
         }
      }

      if (!level.isClientSide && needsSync) {
         level.sendBlockUpdated(pos, state, state, 3);
      }

      if (level.isClientSide) {
         if ((Boolean)state.getValue(HubBlock.CONSTRUCTED)) {
            entity.handleSoundClient();
         }
      } else if (!(Boolean)state.getValue(HubBlock.CONSTRUCTED)) {
         if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            if (entity.activeDiggers > 0) {
               float speed = entity.activeDiggers == 1 ? 1.0F : (entity.activeDiggers == 2 ? 1.34F : (entity.activeDiggers == 3 ? 2.0F : 4.0F));
               float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
               speed *= multiplier;
               entity.currentProgress = entity.currentProgress + (int)Math.ceil(speed);
            }

            if (entity.currentProgress >= 2400) {
               entity.currentProgress = 2400;
               level.setBlock(pos, (BlockState)state.setValue(HubBlock.CONSTRUCTED, true), 3);
               if (!level.isClientSide) {
                  ((ServerLevel)level)
                     .sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 50, 1.2, 0.5, 1.2, 0.05);
                  WarfareWorldData data = WarfareWorldData.get((ServerLevel)level);

                  for (WarfareWorldData.HubInfo h : data.hubs) {
                     if (h.pos.equals(pos)) {
                        h.constructed = true;
                        data.setDirty();
                        PacketHandler.sendToAllClients((ServerLevel)level, data);
                        break;
                     }
                  }
               }
            }

            if (level.getGameTime() % 5L == 0L || entity.currentProgress >= 2400) {
               level.sendBlockUpdated(pos, state, state, 3);
            }
         }

         entity.activeDiggers = 0;
      } else {
         if (entity.activeDiggers > 0) {
            float speed = entity.activeDiggers == 1 ? 1.0F : (entity.activeDiggers == 2 ? 1.34F : (entity.activeDiggers == 3 ? 2.0F : 4.0F));
            float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
            speed *= multiplier;
            speed *= entity.dismantleMultiplier;
            entity.dismantleProgress = entity.dismantleProgress + (int)Math.ceil(speed);
            entity.setChanged();

            if (entity.dismantleProgress >= 2400) {
               entity.dismantleProgress = 2400;
               entity.handleDismantleComplete(level, pos, state);
            }

            if (level.getGameTime() % 5L == 0L || entity.dismantleProgress >= 2400) {
               level.sendBlockUpdated(pos, state, state, 3);
            }
         }

         entity.activeDiggers = 0;
         entity.dismantleMultiplier = 1.0F;
      }
   }

   private void handleSoundClient() {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.clientSoundRef = ClientHooks.playHubSound(this, this.clientSoundRef));
   }

   private void handleDismantleComplete(Level level, BlockPos pos, BlockState state) {
      if (level.getBlockState(pos).isAir()) return;
      if (level instanceof ServerLevel serverLevel) {
         WarfareWorldData data = WarfareWorldData.get(serverLevel);
         String hubTeam = this.getTeam();

         if (this.dismantleMultiplier >= 1.0F) {
            this.wasDismantled = true;
            if (!hubTeam.equals("NEUTRAL")) {
               int penalty = 10;
               if (hubTeam.equalsIgnoreCase("BLUE")) {
                  data.blueTickets = Math.max(0, data.blueTickets - penalty);
                  broadcastMessage(serverLevel, "BLUE player dismantled Friendly FOB! (-10 Tickets)", ChatFormatting.BLUE);
               } else if (hubTeam.equalsIgnoreCase("RED")) {
                  data.redTickets = Math.max(0, data.redTickets - penalty);
                  broadcastMessage(serverLevel, "RED player dismantled Friendly FOB! (-10 Tickets)", ChatFormatting.RED);
               }
               data.setDirty();
               PacketHandler.sendToAllClients(serverLevel, data);
            }
         }
      }

      level.destroyBlock(pos, false);
   }

   private static void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
      level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(text).withStyle(color), false);
   }

   public void setRemoved() {
      if (this.level != null && this.level.isClientSide) {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.stopHubSound(this.clientSoundRef));
      }

      super.setRemoved();
   }

   public float getPercentage() {
      boolean constructed = this.level != null && this.level.getBlockState(this.worldPosition).hasProperty(HubBlock.CONSTRUCTED)
         && (Boolean)this.level.getBlockState(this.worldPosition).getValue(HubBlock.CONSTRUCTED);
      if (constructed && this.dismantleProgress > 0) {
         return (float)this.dismantleProgress / 2400.0F;
      }
      if (constructed) {
         return 1.0F;
      }
      return this.currentProgress / 2400.0F;
   }

   public boolean isDismantling() {
      return this.dismantleProgress > 0;
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("BuildProgress", this.currentProgress);
      tag.putString("TeamOwner", this.teamOwner);
      tag.putInt("Materials", this.constructionMaterials);
      tag.putInt("CooldownAGS", this.cooldownAGS);
      tag.putInt("CooldownM2", this.cooldownM2);
      tag.putInt("CooldownMortar", this.cooldownMortar);
      tag.putInt("CooldownTOW", this.cooldownTOW);
      tag.putInt("DismantleProgress", this.dismantleProgress);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.currentProgress = tag.getInt("BuildProgress");
      if (tag.contains("TeamOwner")) {
         this.teamOwner = tag.getString("TeamOwner");
      }

      if (tag.contains("Materials")) {
         this.constructionMaterials = tag.getInt("Materials");
      }

      if (tag.contains("CooldownAGS")) {
         this.cooldownAGS = tag.getInt("CooldownAGS");
      }

      if (tag.contains("CooldownM2")) {
         this.cooldownM2 = tag.getInt("CooldownM2");
      }

      if (tag.contains("CooldownMortar")) {
         this.cooldownMortar = tag.getInt("CooldownMortar");
      }

      if (tag.contains("CooldownTOW")) {
         this.cooldownTOW = tag.getInt("CooldownTOW");
      }

      if (tag.contains("DismantleProgress")) {
         this.dismantleProgress = tag.getInt("DismantleProgress");
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

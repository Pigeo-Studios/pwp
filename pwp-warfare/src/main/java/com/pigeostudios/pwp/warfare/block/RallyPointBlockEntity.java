package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSyncSquads;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;

// Сущность блока точки сбора
// Управляет временем жизни точки сбора и привязкой к отряду
public class RallyPointBlockEntity extends BlockEntity {
   // Флаг естественного истечения времени (не уничтожен врагом)
   public boolean isDecay = false;
   private int squadId = -1;
   private long expiryTick = -1L;
   private Object clientSoundRef = null;

   public RallyPointBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.RALLY_BE.get(), pos, state);
   }

   // Устанавливает ID отряда, к которому привязана точка
   public void setSquadId(int id) {
      this.squadId = id;
      this.setChanged();
   }

   public int getSquadId() {
      return this.squadId;
   }

   public void setExpiryTick(long tick) {
      this.expiryTick = tick;
      this.setChanged();
   }

   // Очищает данные точки сбора из списка отряда
   public void cleanupData(ServerLevel level) {
      if (this.squadId != -1) {
         WarfareWorldData data = WarfareWorldData.get(level);
         boolean changed = false;

         for (WarfareWorldData.Squad s : data.squads) {
            if (s.id == this.squadId) {
               if (s.rallyPos != null && s.rallyPos.equals(this.worldPosition)) {
                  s.rallyPos = null;
                  s.rallyExpiryTick = -1L;
                  changed = true;
               }
               break;
            }
         }

         if (changed) {
            data.setDirty();
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncSquads(data.squads));
         }
      }
   }

   // Проверяет, не истекло ли время жизни точки сбора
    public void checkExpiry(Level level, BlockPos pos) {
       if (this.expiryTick != -1L) {
          if (level.getGameTime() >= this.expiryTick) {
             this.isDecay = true;
             cleanupData((ServerLevel)level);
             level.removeBlock(pos, false);
          }
       }
    }

   public void handleSoundClient() {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.clientSoundRef = ClientHooks.playRallySound(this, this.clientSoundRef));
   }

   public void setRemoved() {
      if (this.level != null && this.level.isClientSide) {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.stopRallySound(this.clientSoundRef));
      }

      super.setRemoved();
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("SquadID", this.squadId);
      tag.putBoolean("IsDecay", this.isDecay);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.squadId = tag.getInt("SquadID");
      this.isDecay = tag.getBoolean("IsDecay");
   }
}

package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

// Сущность станции техники
// Строится лопатой (стадии 0-2), после постройки раз в секунду пополняет боезапас
// M2/AGS и лоудаут техники в радиусе 10 блоков, чинит повреждённые предметы.
// Готовая станция разбирается лопатой: при демонтаже стадии идут в обратном порядке,
// по завершении возвращается 50 материалов в ближайший хаб команды
public class VehicleStationBlockEntity extends BlockEntity {
   public static final int MAX_PROGRESS = 3000;
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";
   private int dismantleProgress = 0;
   private float dismantleMultiplier = 1.0F;
   private Object clientSoundRef = null;

   public VehicleStationBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.VEHICLE_STATION_BE.get(), pos, state);
   }

   public void addProgress() {
      if (this.currentProgress < MAX_PROGRESS) {
         this.activeDiggers++;
         this.setChanged();
      }
   }

   public void addCreativeProgress(int amount) {
      this.currentProgress = Math.min(MAX_PROGRESS, this.currentProgress + amount);
      this.setChanged();
   }

   public void addDismantleProgress(boolean isEnemy) {
      if (this.dismantleProgress < MAX_PROGRESS) {
         this.activeDiggers++;
         this.dismantleMultiplier = isEnemy ? 0.5F : 1.0F;
         this.setChanged();
      }
   }

   public void addCreativeDismantleProgress() {
      this.dismantleProgress = Math.min(this.dismantleProgress + 50, MAX_PROGRESS);
      this.setChanged();
   }

   public float getPercentage() {
      boolean constructed = this.level != null && this.level.getBlockState(this.worldPosition).hasProperty(VehicleStationBlock.CONSTRUCTED)
         && (Boolean)this.level.getBlockState(this.worldPosition).getValue(VehicleStationBlock.CONSTRUCTED);
      if (constructed && this.dismantleProgress > 0) {
         return (float)this.dismantleProgress / (float)MAX_PROGRESS;
      }
      if (constructed) {
         return 1.0F;
      }
      return this.currentProgress / (float)MAX_PROGRESS;
   }

   public boolean isDismantling() {
      return this.dismantleProgress > 0;
   }

   public String getTeam() {
      return this.teamOwner;
   }

   public void setTeam(String team) {
      this.teamOwner = team == null || team.isBlank() ? "NEUTRAL" : team.trim().toUpperCase();
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public static void tick(Level level, BlockPos pos, BlockState state, VehicleStationBlockEntity entity) {
      if (level.isClientSide) {
         if ((Boolean)state.getValue(VehicleStationBlock.CONSTRUCTED)) {
            entity.handleSoundClient();
         }
         return;
      }

      if (!(Boolean)state.getValue(VehicleStationBlock.CONSTRUCTED)) {
         tickConstruction(level, pos, state, entity);
         return;
      }

      if (entity.activeDiggers > 0 || entity.dismantleProgress > 0) {
         if (entity.activeDiggers > 0) {
            float speed = entity.activeDiggers == 1 ? 1.0F : (entity.activeDiggers == 2 ? 1.34F : (entity.activeDiggers == 3 ? 2.0F : 4.0F));
            float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
            speed *= multiplier;
            speed *= entity.dismantleMultiplier;
            entity.dismantleProgress = entity.dismantleProgress + (int)Math.ceil(speed);
            entity.setChanged();
         }

         if (entity.dismantleProgress >= MAX_PROGRESS) {
            entity.dismantleProgress = MAX_PROGRESS;
            entity.handleDismantleComplete(level, pos, state);
         } else {
            // Стадии при разборке идут в обратном порядке
            int newStage = entity.dismantleProgress >= (MAX_PROGRESS * 2) / 3 ? 0 : (entity.dismantleProgress >= MAX_PROGRESS / 3 ? 1 : 2);
            if (state.getValue(VehicleStationBlock.BUILD_STAGE) != newStage) {
               level.setBlock(pos, (BlockState)state.setValue(VehicleStationBlock.BUILD_STAGE, newStage), 3);
            }
         }

         if (level.getGameTime() % 5L == 0L || entity.dismantleProgress >= MAX_PROGRESS) {
            level.sendBlockUpdated(pos, state, state, 3);
         }
      }

      entity.activeDiggers = 0;
      entity.dismantleMultiplier = 1.0F;

      if (level.getGameTime() % 20L != 0L) {
         return;
      }

      tickResupply(level, pos, entity);
   }

   // Строительство станции лопатой со сменой стадий
   private static void tickConstruction(Level level, BlockPos pos, BlockState state, VehicleStationBlockEntity entity) {
      if (state.getValue(VehicleStationBlock.BUILD_STAGE) == 0) {
         level.setBlock(pos, (BlockState)state.setValue(VehicleStationBlock.BUILD_STAGE, 1), 3);
      }

      if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
         if (entity.activeDiggers > 0) {
            float speed = entity.activeDiggers == 1 ? 1.0F : (entity.activeDiggers == 2 ? 1.34F : (entity.activeDiggers == 3 ? 2.0F : 4.0F));
            float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
            entity.currentProgress += (int)Math.ceil(speed * multiplier);
         }

         if (entity.currentProgress >= MAX_PROGRESS) {
            entity.currentProgress = MAX_PROGRESS;
            level.setBlock(pos, (BlockState)((BlockState)state.setValue(VehicleStationBlock.CONSTRUCTED, true)).setValue(VehicleStationBlock.BUILD_STAGE, 2), 3);
         } else {
            int stage = entity.currentProgress >= MAX_PROGRESS / 2 ? 2 : 1;
            if (state.getValue(VehicleStationBlock.BUILD_STAGE) != stage) {
               level.setBlock(pos, (BlockState)state.setValue(VehicleStationBlock.BUILD_STAGE, stage), 3);
            }
         }

         level.sendBlockUpdated(pos, state, state, 3);
      }

      entity.activeDiggers = 0;
   }

   // Пополнение боезапаса техники в радиусе 10 блоков
   private static void tickResupply(Level level, BlockPos pos, VehicleStationBlockEntity entity) {
      if (!(Boolean)level.getBlockState(pos).getValue(VehicleStationBlock.CONSTRUCTED)) {
         return;
      }

      boolean stationHasTeam = entity.teamOwner != null && !entity.teamOwner.isBlank() && !entity.teamOwner.equals("NEUTRAL");
      AABB area = new AABB(pos).inflate(10.0);
      List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, area);

      for (Player p : nearbyPlayers) {
         Entity vehicle = p.getVehicle();
         if (vehicle == null) {
            continue;
         }

         if (vehicle instanceof SupplyCrateEntity) {
            continue;
         }

         String vTeam;
         if (vehicle.getPersistentData().contains("WARFARE_VehicleTeam")) {
            vTeam = vehicle.getPersistentData().getString("WARFARE_VehicleTeam");
         } else {
            vTeam = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "";
            if (!vTeam.isBlank()) {
               vehicle.getPersistentData().putString("WARFARE_VehicleTeam", vTeam);
            }
         }

         if (vTeam == null || vTeam.isBlank()) {
            sendActionBar(p, Component.translatable("pwpwarfare.message.station_no_vehicle_team"), ChatFormatting.RED);
            continue;
         }

         vTeam = vTeam.trim();
         if (!stationHasTeam) {
            sendActionBar(p, Component.translatable("pwpwarfare.message.station_no_team"), ChatFormatting.RED);
            continue;
         }

         if (!vTeam.equalsIgnoreCase(entity.teamOwner)) {
            sendActionBar(p, Component.translatable("pwpwarfare.message.station_wrong_team", entity.teamOwner), ChatFormatting.RED);
            continue;
         }

         boolean didAmmoUpdate = false;
         int shownCurrent = -1;
         int shownMax = -1;

         if (vehicle instanceof M2BrowningEntity m2) {
            int cur = m2.getAmmoCount();
            shownMax = 200;
            if (cur < shownMax) {
               m2.setAmmoCount(Math.min(shownMax, cur + 5));
               didAmmoUpdate = true;
            }
            shownCurrent = m2.getAmmoCount();
         } else if (vehicle instanceof AGS30Entity ags) {
            int cur = ags.getAmmoCount();
            shownMax = 30;
            if (cur < shownMax) {
               ags.setAmmoCount(Math.min(shownMax, cur + 1));
               didAmmoUpdate = true;
            }
            shownCurrent = ags.getAmmoCount();
         }

         LoadoutResult loadoutResult = resupplyFromLoadout(vehicle);
         if (loadoutResult != null && loadoutResult.targetTotal > 0) {
            didAmmoUpdate = didAmmoUpdate || loadoutResult.changed;
            if (shownMax == -1) {
               shownCurrent = loadoutResult.currentTotal;
               shownMax = loadoutResult.targetTotal;
            }
         }

         boolean[] repairedMags = new boolean[]{false};
         if (loadoutResult == null) {
            vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
               for (int i = 0; i < h.getSlots(); i++) {
                  ItemStack s = h.getStackInSlot(i);
                  if (s.isEmpty() || !s.isDamaged()) {
                     continue;
                  }
                  s.setDamageValue(0);
                  repairedMags[0] = true;
               }
            });
         }

         if (shownMax != -1) {
            if (shownCurrent < shownMax) {
               sendActionBar(p, Component.translatable("pwpwarfare.message.station_resupplying", shownCurrent, shownMax), ChatFormatting.YELLOW);
               continue;
            }
            sendActionBar(p, Component.translatable("pwpwarfare.message.station_ammo_full"), ChatFormatting.GREEN);
            continue;
         }

         if (didAmmoUpdate || repairedMags[0]) {
            sendActionBar(p, Component.translatable("pwpwarfare.message.station_resupplying_generic"), ChatFormatting.YELLOW);
            continue;
         }

         sendActionBar(p, Component.translatable("pwpwarfare.message.station_ammo_full"), ChatFormatting.GREEN);
      }
   }

   // Восстановление лоудаута техники из сохранённого при спавне списка
   private static LoadoutResult resupplyFromLoadout(Entity vehicle) {
      if (!vehicle.getPersistentData().contains("WARFARE_InitialLoadout")) {
         return null;
      }

      ListTag loadoutTag = vehicle.getPersistentData().getList("WARFARE_InitialLoadout", 10);
      if (loadoutTag.isEmpty()) {
         return null;
      }

      IItemHandler rawHandler = vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
      if (!(rawHandler instanceof IItemHandlerModifiable handler)) {
         return null;
      }

      LoadoutResult result = new LoadoutResult();
      for (int i = 0; i < loadoutTag.size(); i++) {
         CompoundTag itemTag = loadoutTag.getCompound(i);
         int slot = itemTag.getByte("Slot") & 0xFF;
         if (slot >= handler.getSlots()) {
            continue;
         }

         ItemStack targetStack = ItemStack.of(itemTag);
         if (targetStack.isEmpty()) {
            continue;
         }

         int targetCount = targetStack.getCount();
         result.targetTotal += targetCount;
         ItemStack currentStack = handler.getStackInSlot(slot);
         if (!currentStack.isEmpty() && !ItemStack.isSameItem(currentStack, targetStack)) {
            result.currentTotal += currentStack.getCount();
            continue;
         }

         int currentCount = currentStack.isEmpty() ? 0 : currentStack.getCount();
         if (currentCount >= targetCount) {
            result.currentTotal += currentCount;
            continue;
         }

         int deficit = targetCount - currentCount;
         int step = Math.max(1, (int)Math.ceil(targetCount * 0.01));
         int toAdd = Math.min(deficit, step);
         ItemStack newStack = targetStack.copy();
         newStack.setCount(currentCount + toAdd);
         handler.setStackInSlot(slot, newStack);
         result.changed = true;
         result.currentTotal += currentCount + toAdd;
      }

      return result;
   }

   // Завершение разборки: возврат 50 материалов в ближайший хаб команды и удаление
   private void handleDismantleComplete(Level level, BlockPos pos, BlockState state) {
      if (level.getBlockState(pos).isAir()) return;
      if (this.dismantleMultiplier >= 1.0F && level instanceof ServerLevel serverLevel) {
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
            hub.addMaterials(50);
         }
      }

      level.destroyBlock(pos, false);
   }

   private static void sendActionBar(Player player, Component msg, ChatFormatting color) {
      player.displayClientMessage(msg.copy().withStyle(color), true);
   }

   private void handleSoundClient() {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.clientSoundRef = ClientHooks.playStationSound(this, this.clientSoundRef));
   }

   public void setRemoved() {
      if (this.level != null && this.level.isClientSide) {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.stopStationSound(this.clientSoundRef));
      }

      super.setRemoved();
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("BuildProgress", this.currentProgress);
      tag.putString("TeamOwner", this.teamOwner);
      tag.putInt("DismantleProgress", this.dismantleProgress);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.currentProgress = tag.getInt("BuildProgress");
      if (tag.contains("TeamOwner")) {
         String loaded = tag.getString("TeamOwner");
         this.teamOwner = loaded == null || loaded.isBlank() ? "NEUTRAL" : loaded.trim().toUpperCase();
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

   private static class LoadoutResult {
      boolean changed;
      int currentTotal;
      int targetTotal;
   }
}

package com.pigeostudios.pwp.warfare.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.FactionVehicleData;
import com.pigeostudios.pwp.warfare.item.SupplyTruckMarkerItem;
import com.pigeostudios.pwp.warfare.item.VehicleMarkerItem;
import com.pigeostudios.pwp.warfare.menu.VehicleSpawnerMenu;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

// РЎСѓС‰РЅРѕСЃС‚СЊ Р±Р»РѕРєР° СЃРїР°СѓРЅРµСЂР° С‚РµС…РЅРёРєРё
// РЈРїСЂР°РІР»СЏРµС‚ С‚Р°Р№РјРµСЂР°РјРё РІРѕР·СЂРѕР¶РґРµРЅРёСЏ, РёРЅРІРµРЅС‚Р°СЂС‘Рј Рё СЃРїР°СѓРЅРѕРј С‚СЂР°РЅСЃРїРѕСЂС‚РЅС‹С… СЃСЂРµРґСЃС‚РІ
public class VehicleSpawnerBlockEntity extends BlockEntity implements MenuProvider {
   // РРЅРІРµРЅС‚Р°СЂСЊ СЃРїР°СѓРЅРµСЂР° (33 СЃР»РѕС‚Р°: 0 - РјР°СЂРєРµСЂ РўРЎ, 1-32 - СЃРѕРґРµСЂР¶РёРјРѕРµ)
   public final ItemStackHandler inventory = new ItemStackHandler(33) {
      protected void onContentsChanged(int slot) {
         VehicleSpawnerBlockEntity.this.setChanged();
      }
   };
    public float vehicleYaw = 0.0F;
    public int respawnTimeSettings = 60;
    public int initialTimeSettings = 60;
    public String vehicleIdString = "";
    public String vehicleName = "";
    public long targetSpawnTick = 0L;
    public boolean hasSpawnedOnce = false;
    private UUID lastVehicleUUID = null;
   private int loadTimer = 60;

   public VehicleSpawnerBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.VEHICLE_SPAWNER_BE.get(), pos, state);
   }

    public void onLoad() {
       super.onLoad();
       this.loadTimer = 60;
    }

    public void loadDefaultsFromFactionVehicle(FactionVehicleData veh) {
       if (veh == null) return;
       if (!veh.vehicleId.isEmpty()) this.vehicleIdString = veh.vehicleId;
       this.vehicleYaw = veh.yaw;
       this.respawnTimeSettings = veh.respawnTime;
       this.initialTimeSettings = veh.initialTime;
       // slot 0 = modifier/marker
       if (!veh.inventory.get(0).isEmpty()) {
          this.inventory.setStackInSlot(0, veh.inventory.get(0).copy());
       }
       // slots 1-32 = vehicle contents
       for (int i = 1; i < 33 && i < veh.inventory.size(); i++) {
          if (!veh.inventory.get(i).isEmpty()) {
             this.inventory.setStackInSlot(i, veh.inventory.get(i).copy());
          }
       }
       this.setChanged();
       syncToClient();
    }

   // РўРёРє СЃРїР°СѓРЅРµСЂР°: СѓРїСЂР°РІР»СЏРµС‚ С‚Р°Р№РјРµСЂР°РјРё РІРѕР·СЂРѕР¶РґРµРЅРёСЏ Рё СЃРїР°СѓРЅРёС‚ С‚РµС…РЅРёРєСѓ
   public static void tick(Level level, BlockPos pos, BlockState state, VehicleSpawnerBlockEntity be) {
      if (!level.isClientSide) {
         if (be.loadTimer > 0) {
            be.loadTimer--;
         } else {
            WarfareWorldData data = WarfareWorldData.get((ServerLevel)level);
            if (data.isGameStarted) {
               long currentTick = level.getGameTime();
               boolean vehicleExistsGlobally = data.markedVehicles.stream().anyMatch(v -> v.spawnerPos != null && v.spawnerPos.equals(pos));
               if (vehicleExistsGlobally) {
                  if (be.targetSpawnTick != 0L) {
                     be.targetSpawnTick = 0L;
                     be.setChanged();
                     be.syncToClient();
                  }
               } else {
                  if (be.lastVehicleUUID != null) {
                     be.lastVehicleUUID = null;
                     int delay = be.respawnTimeSettings * 20;
                     be.targetSpawnTick = currentTick + delay;
                     be.setChanged();
                     be.syncToClient();
                  }

                  if (be.targetSpawnTick == 0L && !be.hasSpawnedOnce) {
                     int delay = be.initialTimeSettings * 20;
                     be.targetSpawnTick = currentTick + delay;
                     be.setChanged();
                     be.syncToClient();
                  }

                  if (be.targetSpawnTick != 0L && currentTick >= be.targetSpawnTick) {
                     be.spawnVehicle();
                     be.targetSpawnTick = 0L;
                     be.setChanged();
                     be.syncToClient();
                  }
               }
            } else {
               if (be.hasSpawnedOnce || be.targetSpawnTick != 0L || be.lastVehicleUUID != null) {
                  be.hasSpawnedOnce = false;
                  be.targetSpawnTick = 0L;
                  be.lastVehicleUUID = null;
                  be.setChanged();
                  be.syncToClient();
               }
            }
         }
      }
   }

   // РЎРѕР·РґР°С‘С‚ С‚СЂР°РЅСЃРїРѕСЂС‚РЅРѕРµ СЃСЂРµРґСЃС‚РІРѕ СЃ РЅР°СЃС‚СЂРѕР№РєР°РјРё РёР· РёРЅРІРµРЅС‚Р°СЂСЏ СЃРїР°СѓРЅРµСЂР°
   private void spawnVehicle() {
      if (this.level != null && !this.level.isClientSide) {
         if (this.vehicleIdString != null && !this.vehicleIdString.trim().isEmpty()) {
            ResourceLocation resLoc = ResourceLocation.tryParse(this.vehicleIdString);
            if (resLoc != null) {
               EntityType<?> type = (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(resLoc);
               if (type != null) {
                  Entity entity = type.create(this.level);
                  if (entity != null) {
                     entity.setPos(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.5, this.worldPosition.getZ() + 0.5);
                     entity.getPersistentData().putLong("WARFARE_SpawnerPos", this.worldPosition.asLong());
                     entity.setYRot(this.vehicleYaw);
                     entity.setYHeadRot(this.vehicleYaw);
                     String vTeam = "NEUTRAL";
                     String vType = "DEFAULT";
                     int penalty = 0;
                     int maxMats = 0;
                     ItemStack modifierStack = this.inventory.getStackInSlot(0);
                     if (!modifierStack.isEmpty()) {
                        if (modifierStack.getItem() instanceof VehicleMarkerItem marker) {
                           vTeam = marker.getTeam();
                           vType = marker.getType();
                           penalty = marker.getPenalty();
                           maxMats = marker.getMaxMats();
                        } else if (modifierStack.getItem() instanceof SupplyTruckMarkerItem supply) {
                           vTeam = supply.getTeam();
                           vType = supply.getVehicleType();
                           penalty = supply.getPenalty();
                           maxMats = supply.getMaxMats();
                           entity.getPersistentData().putBoolean("WARFARE_IsSupplyTruck", true);
                           entity.getPersistentData().putInt("WARFARE_SupplyAmmo", (Integer)WarfareConfig.SUPPLY_TRUCK_CRATES.get());
                        }
                     }

entity.getPersistentData().putString("WARFARE_VehicleTeam", vTeam);
                      entity.getPersistentData().putString("WARFARE_VehicleType", vType);
                      entity.getPersistentData().putBoolean("WARFARE_FreshVehicle", true);
                     entity.getPersistentData().putInt("WARFARE_TicketPenalty", penalty);
                     if (maxMats > 0) {
                        entity.getPersistentData().putInt("WARFARE_VehicleMaxMats", maxMats);
                        entity.getPersistentData().putInt("WARFARE_VehicleMats", maxMats);
                     }

                     WarfareWorldData worldData = WarfareWorldData.get((ServerLevel)this.level);
                     worldData.markedVehicles.removeIf(v -> v.spawnerPos != null && v.spawnerPos.equals(this.worldPosition));
                     worldData.markedVehicles
                        .add(
                           new WarfareWorldData.VehicleRecord(
                              entity.getUUID(), vTeam, vType, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), this.worldPosition
                           )
                        );
                     worldData.setDirty();
                     PacketHandler.sendToAllClients((ServerLevel)this.level, worldData);
                     entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                        for (int i = 0; i < 32; i++) {
                           ItemStack contentStack = this.inventory.getStackInSlot(i + 1);
                           if (!contentStack.isEmpty() && handler instanceof IItemHandlerModifiable modifiable) {
                              modifiable.setStackInSlot(i, contentStack.copy());
                           }
                        }
                     });
                     if (entity instanceof LivingEntity living) {
                        living.setHealth(living.getMaxHealth());
                     }

                     entity.getPersistentData().putLong("WARFARE_SpawnGraceTick", this.level.getGameTime());
                     this.level.addFreshEntity(entity);
                     this.lastVehicleUUID = entity.getUUID();
                     this.hasSpawnedOnce = true;
                     this.setChanged();
                  }
               }
            }
         }
      }
   }

   private void insertItemIntoSlot(IItemHandler handler, int slot, ItemStack stack) {
      if (slot < handler.getSlots()) {
         if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
         } else {
            handler.insertItem(slot, stack, false);
         }
      }
   }

   // РЎРёРЅС…СЂРѕРЅРёР·РёСЂСѓРµС‚ РґР°РЅРЅС‹Рµ СЃРїР°СѓРЅРµСЂР° СЃ РєР»РёРµРЅС‚РѕРј
   public void syncToClient() {
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.contains("Inventory")) {
         this.inventory.deserializeNBT(tag.getCompound("Inventory"));
      }

      if (this.inventory.getSlots() < 33) {
         this.inventory.setSize(33);
      }

      this.respawnTimeSettings = tag.getInt("RespawnTime");
      this.initialTimeSettings = tag.getInt("InitialTime");
      this.targetSpawnTick = tag.getLong("TargetSpawnTick");
      this.hasSpawnedOnce = tag.getBoolean("HasSpawnedOnce");
      this.vehicleIdString = tag.getString("VehicleID");
      this.vehicleName = tag.getString("VehicleName");
      this.vehicleYaw = tag.getFloat("VehicleYaw");
      if (tag.hasUUID("LastVehicle")) {
         this.lastVehicleUUID = tag.getUUID("LastVehicle");
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.put("Inventory", this.inventory.serializeNBT());
      tag.putInt("RespawnTime", this.respawnTimeSettings);
      tag.putInt("InitialTime", this.initialTimeSettings);
      tag.putLong("TargetSpawnTick", this.targetSpawnTick);
      tag.putBoolean("HasSpawnedOnce", this.hasSpawnedOnce);
      tag.putString("VehicleID", this.vehicleIdString);
      tag.putString("VehicleName", this.vehicleName);
      tag.putFloat("VehicleYaw", this.vehicleYaw);
      if (this.lastVehicleUUID != null) {
         tag.putUUID("LastVehicle", this.lastVehicleUUID);
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

   public Component getDisplayName() {
      return Component.literal("Vehicle Spawner Config");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
      return new VehicleSpawnerMenu(id, playerInv, this);
   }
}

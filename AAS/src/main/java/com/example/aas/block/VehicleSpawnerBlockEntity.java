/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.common.capabilities.ForgeCapabilities
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.IItemHandlerModifiable
 *  net.minecraftforge.items.ItemStackHandler
 *  net.minecraftforge.registries.ForgeRegistries
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import com.example.aas.config.AASConfig;
import com.example.aas.item.SupplyTruckMarkerItem;
import com.example.aas.item.VehicleMarkerItem;
import com.example.aas.menu.VehicleSpawnerMenu;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.item.Item;
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

public class VehicleSpawnerBlockEntity
extends BlockEntity
implements MenuProvider {
    final public ItemStackHandler inventory = new ItemStackHandler(33){

        protected void onContentsChanged(int slot) {
            VehicleSpawnerBlockEntity.this.setChanged();
        }
    };
    public float vehicleYaw = 0.0f;
    public int respawnTimeSettings = 60;
    public int initialTimeSettings = 60;
    public String vehicleIdString = "";
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

    public static void tick(Level level, BlockPos pos, BlockState state, VehicleSpawnerBlockEntity be) {
        int delay;
        if (level.isClientSide) {
            return;
        }
        if (be.loadTimer > 0) {
            --be.loadTimer;
            return;
        }
        AASWorldData data = AASWorldData.get((ServerLevel)level);
        if (!data.isGameStarted) {
            if (be.hasSpawnedOnce || be.targetSpawnTick != 0L || be.lastVehicleUUID != null) {
                be.hasSpawnedOnce = false;
                be.targetSpawnTick = 0L;
                be.lastVehicleUUID = null;
                be.setChanged();
                be.syncToClient();
            }
            return;
        }
        long currentTick = level.getGameTime();
        boolean vehicleExistsGlobally = data.markedVehicles.stream().anyMatch(v -> v.spawnerPos != null && v.spawnerPos.equals((Object)pos));
        if (vehicleExistsGlobally) {
            if (be.targetSpawnTick != 0L) {
                be.targetSpawnTick = 0L;
                be.setChanged();
                be.syncToClient();
            }
            return;
        }
        if (be.lastVehicleUUID != null) {
            be.lastVehicleUUID = null;
            delay = be.respawnTimeSettings * 20;
            be.targetSpawnTick = currentTick + (long)delay;
            be.setChanged();
            be.syncToClient();
        }
        if (be.targetSpawnTick == 0L && !be.hasSpawnedOnce) {
            delay = be.initialTimeSettings * 20;
            be.targetSpawnTick = currentTick + (long)delay;
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

    private void spawnVehicle() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.vehicleIdString == null || this.vehicleIdString.trim().isEmpty()) {
            return;
        }
        ResourceLocation resLoc = ResourceLocation.tryParse((String)this.vehicleIdString);
        if (resLoc == null) {
            return;
        }
        EntityType type = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(resLoc);
        if (type == null) {
            return;
        }
        Entity entity = type.create(this.level);
        if (entity != null) {
            entity.setPos((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.5, (double)this.worldPosition.getZ() + 0.5);
            entity.getPersistentData().putLong("AAS_SpawnerPos", this.worldPosition.asLong());
            entity.setYRot(this.vehicleYaw);
            entity.setYHeadRot(this.vehicleYaw);
            String vTeam = "NEUTRAL";
            String vType = "DEFAULT";
            int penalty = 0;
            int maxMats = 0;
            ItemStack modifierStack = this.inventory.getStackInSlot(0);
            if (!modifierStack.isEmpty()) {
                Item item = modifierStack.getItem();
                if (item instanceof VehicleMarkerItem) {
                    VehicleMarkerItem marker = (VehicleMarkerItem)item;
                    vTeam = marker.getTeam();
                    vType = marker.getType();
                    penalty = marker.getPenalty();
                    maxMats = marker.getMaxMats();
                } else {
                    item = modifierStack.getItem();
                    if (item instanceof SupplyTruckMarkerItem) {
                        SupplyTruckMarkerItem supply = (SupplyTruckMarkerItem)item;
                        vTeam = supply.getTeam();
                        vType = supply.getVehicleType();
                        penalty = supply.getPenalty();
                        maxMats = supply.getMaxMats();
                        entity.getPersistentData().putBoolean("AAS_IsSupplyTruck", true);
                        entity.getPersistentData().putInt("AAS_SupplyAmmo", ((Integer)AASConfig.SUPPLY_TRUCK_CRATES.get()).intValue());
                    }
                }
            }
            entity.getPersistentData().putString("AAS_VehicleTeam", vTeam);
            entity.getPersistentData().putString("AAS_VehicleType", vType);
            entity.getPersistentData().putInt("AAS_TicketPenalty", penalty);
            if (maxMats > 0) {
                entity.getPersistentData().putInt("AAS_VehicleMaxMats", maxMats);
                entity.getPersistentData().putInt("AAS_VehicleMats", maxMats);
            }
            ListTag loadoutTag = new ListTag();
            for (int i = 0; i < 32; ++i) {
                ItemStack contentStack = this.inventory.getStackInSlot(i + 1);
                if (contentStack.isEmpty()) continue;
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte)i);
                contentStack.save(itemTag);
                loadoutTag.add((Object)itemTag);
            }
            entity.getPersistentData().put("AAS_InitialLoadout", (Tag)loadoutTag);
            AASWorldData worldData = AASWorldData.get((ServerLevel)this.level);
            worldData.markedVehicles.removeIf(v -> v.spawnerPos != null && v.spawnerPos.equals((Object)this.worldPosition));
            worldData.markedVehicles.add(new AASWorldData.VehicleRecord(entity.getUUID(), vTeam, vType, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), this.worldPosition));
            worldData.setDirty();
            PacketHandler.sendToAllClients((ServerLevel)this.level, worldData);
            entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                for (int i = 0; i < 32; ++i) {
                    ItemStack contentStack = this.inventory.getStackInSlot(i + 1);
                    if (contentStack.isEmpty() || !(handler instanceof IItemHandlerModifiable)) continue;
                    IItemHandlerModifiable modifiable = (IItemHandlerModifiable)handler;
                    modifiable.setStackInSlot(i, contentStack.copy());
                }
            });
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                living.setHealth(living.getMaxHealth());
            }
            entity.getPersistentData().putLong("AAS_SpawnGraceTick", this.level.getGameTime());
            this.level.addFreshEntity(entity);
            this.lastVehicleUUID = entity.getUUID();
            this.hasSpawnedOnce = true;
            this.setChanged();
        }
    }

    private void insertItemIntoSlot(IItemHandler handler, int slot, ItemStack stack) {
        if (slot < handler.getSlots()) {
            if (handler instanceof IItemHandlerModifiable) {
                IItemHandlerModifiable modifiable = (IItemHandlerModifiable)handler;
                modifiable.setStackInSlot(slot, stack);
            } else {
                handler.insertItem(slot, stack, false);
            }
        }
    }

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
        this.vehicleYaw = tag.getFloat("VehicleYaw");
        if (tag.hasUUID("LastVehicle")) {
            this.lastVehicleUUID = tag.getUUID("LastVehicle");
        }
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", (Tag)this.inventory.serializeNBT());
        tag.putInt("RespawnTime", this.respawnTimeSettings);
        tag.putInt("InitialTime", this.initialTimeSettings);
        tag.putLong("TargetSpawnTick", this.targetSpawnTick);
        tag.putBoolean("HasSpawnedOnce", this.hasSpawnedOnce);
        tag.putString("VehicleID", this.vehicleIdString);
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
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public Component getDisplayName() {
        return Component.literal((String)"Vehicle Spawner Config");
    }

    @Nullable
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new VehicleSpawnerMenu(id, playerInv, this);
    }
}


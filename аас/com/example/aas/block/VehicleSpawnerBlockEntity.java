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
    public final ItemStackHandler inventory = new ItemStackHandler(33){

        protected void onContentsChanged(int slot) {
            VehicleSpawnerBlockEntity.this.m_6596_();
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
        if (level.f_46443_) {
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
                be.m_6596_();
                be.syncToClient();
            }
            return;
        }
        long currentTick = level.m_46467_();
        boolean vehicleExistsGlobally = data.markedVehicles.stream().anyMatch(v -> v.spawnerPos != null && v.spawnerPos.equals((Object)pos));
        if (vehicleExistsGlobally) {
            if (be.targetSpawnTick != 0L) {
                be.targetSpawnTick = 0L;
                be.m_6596_();
                be.syncToClient();
            }
            return;
        }
        if (be.lastVehicleUUID != null) {
            be.lastVehicleUUID = null;
            delay = be.respawnTimeSettings * 20;
            be.targetSpawnTick = currentTick + (long)delay;
            be.m_6596_();
            be.syncToClient();
        }
        if (be.targetSpawnTick == 0L && !be.hasSpawnedOnce) {
            delay = be.initialTimeSettings * 20;
            be.targetSpawnTick = currentTick + (long)delay;
            be.m_6596_();
            be.syncToClient();
        }
        if (be.targetSpawnTick != 0L && currentTick >= be.targetSpawnTick) {
            be.spawnVehicle();
            be.targetSpawnTick = 0L;
            be.m_6596_();
            be.syncToClient();
        }
    }

    private void spawnVehicle() {
        if (this.f_58857_ == null || this.f_58857_.f_46443_) {
            return;
        }
        if (this.vehicleIdString == null || this.vehicleIdString.trim().isEmpty()) {
            return;
        }
        ResourceLocation resLoc = ResourceLocation.m_135820_((String)this.vehicleIdString);
        if (resLoc == null) {
            return;
        }
        EntityType type = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(resLoc);
        if (type == null) {
            return;
        }
        Entity entity = type.m_20615_(this.f_58857_);
        if (entity != null) {
            entity.m_6034_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 1.5, (double)this.f_58858_.m_123343_() + 0.5);
            entity.getPersistentData().m_128356_("AAS_SpawnerPos", this.f_58858_.m_121878_());
            entity.m_146922_(this.vehicleYaw);
            entity.m_5616_(this.vehicleYaw);
            String vTeam = "NEUTRAL";
            String vType = "DEFAULT";
            int penalty = 0;
            int maxMats = 0;
            ItemStack modifierStack = this.inventory.getStackInSlot(0);
            if (!modifierStack.m_41619_()) {
                Item item = modifierStack.m_41720_();
                if (item instanceof VehicleMarkerItem) {
                    VehicleMarkerItem marker = (VehicleMarkerItem)item;
                    vTeam = marker.getTeam();
                    vType = marker.getType();
                    penalty = marker.getPenalty();
                    maxMats = marker.getMaxMats();
                } else {
                    item = modifierStack.m_41720_();
                    if (item instanceof SupplyTruckMarkerItem) {
                        SupplyTruckMarkerItem supply = (SupplyTruckMarkerItem)item;
                        vTeam = supply.getTeam();
                        vType = supply.getVehicleType();
                        penalty = supply.getPenalty();
                        maxMats = supply.getMaxMats();
                        entity.getPersistentData().m_128379_("AAS_IsSupplyTruck", true);
                        entity.getPersistentData().m_128405_("AAS_SupplyAmmo", ((Integer)AASConfig.SUPPLY_TRUCK_CRATES.get()).intValue());
                    }
                }
            }
            entity.getPersistentData().m_128359_("AAS_VehicleTeam", vTeam);
            entity.getPersistentData().m_128359_("AAS_VehicleType", vType);
            entity.getPersistentData().m_128405_("AAS_TicketPenalty", penalty);
            if (maxMats > 0) {
                entity.getPersistentData().m_128405_("AAS_VehicleMaxMats", maxMats);
                entity.getPersistentData().m_128405_("AAS_VehicleMats", maxMats);
            }
            ListTag loadoutTag = new ListTag();
            for (int i = 0; i < 32; ++i) {
                ItemStack contentStack = this.inventory.getStackInSlot(i + 1);
                if (contentStack.m_41619_()) continue;
                CompoundTag itemTag = new CompoundTag();
                itemTag.m_128344_("Slot", (byte)i);
                contentStack.m_41739_(itemTag);
                loadoutTag.add((Object)itemTag);
            }
            entity.getPersistentData().m_128365_("AAS_InitialLoadout", (Tag)loadoutTag);
            AASWorldData worldData = AASWorldData.get((ServerLevel)this.f_58857_);
            worldData.markedVehicles.removeIf(v -> v.spawnerPos != null && v.spawnerPos.equals((Object)this.f_58858_));
            worldData.markedVehicles.add(new AASWorldData.VehicleRecord(entity.m_20148_(), vTeam, vType, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), entity.m_146908_(), this.f_58858_));
            worldData.m_77762_();
            PacketHandler.sendToAllClients((ServerLevel)this.f_58857_, worldData);
            entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                for (int i = 0; i < 32; ++i) {
                    ItemStack contentStack = this.inventory.getStackInSlot(i + 1);
                    if (contentStack.m_41619_() || !(handler instanceof IItemHandlerModifiable)) continue;
                    IItemHandlerModifiable modifiable = (IItemHandlerModifiable)handler;
                    modifiable.setStackInSlot(i, contentStack.m_41777_());
                }
            });
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                living.m_21153_(living.m_21233_());
            }
            entity.getPersistentData().m_128356_("AAS_SpawnGraceTick", this.f_58857_.m_46467_());
            this.f_58857_.m_7967_(entity);
            this.lastVehicleUUID = entity.m_20148_();
            this.hasSpawnedOnce = true;
            this.m_6596_();
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
        if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
        }
    }

    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        if (tag.m_128441_("Inventory")) {
            this.inventory.deserializeNBT(tag.m_128469_("Inventory"));
        }
        if (this.inventory.getSlots() < 33) {
            this.inventory.setSize(33);
        }
        this.respawnTimeSettings = tag.m_128451_("RespawnTime");
        this.initialTimeSettings = tag.m_128451_("InitialTime");
        this.targetSpawnTick = tag.m_128454_("TargetSpawnTick");
        this.hasSpawnedOnce = tag.m_128471_("HasSpawnedOnce");
        this.vehicleIdString = tag.m_128461_("VehicleID");
        this.vehicleYaw = tag.m_128457_("VehicleYaw");
        if (tag.m_128403_("LastVehicle")) {
            this.lastVehicleUUID = tag.m_128342_("LastVehicle");
        }
    }

    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128365_("Inventory", (Tag)this.inventory.serializeNBT());
        tag.m_128405_("RespawnTime", this.respawnTimeSettings);
        tag.m_128405_("InitialTime", this.initialTimeSettings);
        tag.m_128356_("TargetSpawnTick", this.targetSpawnTick);
        tag.m_128379_("HasSpawnedOnce", this.hasSpawnedOnce);
        tag.m_128359_("VehicleID", this.vehicleIdString);
        tag.m_128350_("VehicleYaw", this.vehicleYaw);
        if (this.lastVehicleUUID != null) {
            tag.m_128362_("LastVehicle", this.lastVehicleUUID);
        }
    }

    public CompoundTag m_5995_() {
        CompoundTag tag = new CompoundTag();
        this.m_183515_(tag);
        return tag;
    }

    public Packet<ClientGamePacketListener> m_58483_() {
        return ClientboundBlockEntityDataPacket.m_195640_((BlockEntity)this);
    }

    public Component m_5446_() {
        return Component.m_237113_((String)"Vehicle Spawner Config");
    }

    @Nullable
    public AbstractContainerMenu m_7208_(int id, Inventory playerInv, Player player) {
        return new VehicleSpawnerMenu(id, playerInv, this);
    }
}


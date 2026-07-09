/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.DamageTypeTags
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.MoverType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.AABB
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.common.capabilities.ForgeCapabilities
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.IItemHandlerModifiable
 *  net.minecraftforge.network.NetworkHooks
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.example.aas.entity;

import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.VehicleSpawnerBlockEntity;
import com.example.aas.client.ClientHooks;
import com.example.aas.config.AASConfig;
import com.example.aas.entity.ModEntities;
import com.example.aas.world.AASWorldData;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

public class SupplyCrateEntity
extends Entity {
    static private final EntityDataAccessor<Integer> MATERIALS = SynchedEntityData.defineId(SupplyCrateEntity.class, (EntityDataSerializer)EntityDataSerializers.INT);
    static private final EntityDataAccessor<String> TEAM_OWNER = SynchedEntityData.defineId(SupplyCrateEntity.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private boolean hasResupplied = false;
    private UUID ownerId = null;
    private UUID currentTargetUUID = null;

    public SupplyCrateEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public SupplyCrateEntity(Level level, double x, double y, double z, String team, UUID ownerId) {
        this((EntityType)ModEntities.SUPPLY_CRATE.get(), level);
        this.setPos(x, y, z);
        this.setTeamOwner(team);
        this.setMaterials((Integer)AASConfig.SUPPLY_CRATE_MATERIALS.get());
        this.ownerId = ownerId;
    }

    protected void defineSynchedData() {
        this.entityData.define(MATERIALS, (Object)50);
        this.entityData.define(TEAM_OWNER, (Object)"NEUTRAL");
    }

    public int getMaterials() {
        return (Integer)this.entityData.get(MATERIALS);
    }

    public void setMaterials(int amount) {
        this.entityData.set(MATERIALS, (Object)amount);
    }

    public String getTeamOwner() {
        return (String)this.entityData.get(TEAM_OWNER);
    }

    public void setTeamOwner(String team) {
        this.entityData.set(TEAM_OWNER, (Object)team);
    }

    public boolean isAttackable() {
        return true;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.getEntity() instanceof Player) {
            this.destroyCrate();
            return true;
        }
        return false;
    }

    public void destroyCrate() {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.sendParticles((ParticleOptions)ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
        }
        this.discard();
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.openCrateMenu(this.getId()));
        }
        return InteractionResult.sidedSuccess((boolean)this.level().isClientSide);
    }

    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.getMaterials() <= 0) {
            this.destroyCrate();
            return;
        }
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        float friction = 0.98f;
        if (this.onGround()) {
            friction = 0.7f;
        }
        this.setDeltaMovement(this.getDeltaMovement().scale((double)friction));
        if (this.level().isClientSide) {
            return;
        }
        if (!this.hasResupplied && this.tickCount > 20 && this.tickCount % 20 == 0 && this.onGround()) {
            this.supplyNearbyHub();
            if (!this.isRemoved()) {
                this.supplyNearbyVehicle();
            }
        }
        if (this.tickCount > 6000) {
            this.discard();
        }
    }

    private void supplyNearbyVehicle() {
        ServerLevel level = (ServerLevel)this.level();
        if (this.currentTargetUUID != null) {
            Entity target = level.getEntity(this.currentTargetUUID);
            if (target == null || !target.isAlive() || (double)target.distanceTo((Entity)this) > 15.0) {
                if (target != null) {
                    target.getPersistentData().putInt("AAS_CrateTimer", 0);
                }
                this.currentTargetUUID = null;
                return;
            }
            this.processVehicleLogic(target);
            return;
        }
        AABB searchArea = this.getBoundingBox().inflate(10.0);
        List vehicles = level.getEntities((Entity)this, searchArea, e -> e.isAlive() && e.getPersistentData().contains("AAS_VehicleTeam"));
        vehicles.sort((e1, e2) -> {
            boolean p2;
            boolean p1 = !e1.getPassengers().isEmpty();
            boolean bl = p2 = !e2.getPassengers().isEmpty();
            if (p1 && !p2) {
                return -1;
            }
            if (!p1 && p2) {
                return 1;
            }
            return 0;
        });
        for (Entity vehicle : vehicles) {
            String vTeam = vehicle.getPersistentData().getString("AAS_VehicleTeam");
            if (!this.getTeamOwner().equals("NEUTRAL") && !vTeam.isEmpty() && !vTeam.equalsIgnoreCase(this.getTeamOwner())) continue;
            boolean needsService = false;
            if (vehicle.getPersistentData().contains("AAS_SpawnerPos")) {
                LivingEntity living;
                if (vehicle instanceof LivingEntity && (living = (LivingEntity)vehicle).getHealth() < living.getMaxHealth()) {
                    needsService = true;
                }
                if (!vehicle.getPersistentData().getBoolean("AAS_IsSupplyTruck")) {
                    needsService = true;
                }
            }
            if (!needsService) continue;
            this.currentTargetUUID = vehicle.getUUID();
            vehicle.getPersistentData().putInt("AAS_CrateTimer", 0);
            SupplyCrateEntity.sendMessageToPassengers(vehicle, "Connecting to Supply Crate...", ChatFormatting.YELLOW);
            return;
        }
    }

    private void processVehicleLogic(Entity vehicle) {
        int timer = vehicle.getPersistentData().getInt("AAS_CrateTimer");
        int timeToWait = 30;
        if (++timer >= timeToWait) {
            boolean actionDone = false;
            if (vehicle.getPersistentData().contains("AAS_SpawnerPos")) {
                if (vehicle instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity)vehicle;
                    living.setHealth(living.getMaxHealth());
                }
                AtomicBoolean rearmSuccess = new AtomicBoolean(false);
                vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(vehInv -> {
                    Item batteryItem;
                    int i;
                    if (vehInv instanceof IItemHandlerModifiable) {
                        IItemHandlerModifiable modifiable = (IItemHandlerModifiable)vehInv;
                        for (i = 0; i < vehInv.getSlots(); ++i) {
                            modifiable.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                    if (vehicle.getPersistentData().contains("AAS_InitialLoadout")) {
                        ListTag loadoutTag = vehicle.getPersistentData().getList("AAS_InitialLoadout", 10);
                        for (i = 0; i < loadoutTag.size(); ++i) {
                            CompoundTag itemTag = loadoutTag.getCompound(i);
                            int slot = itemTag.getByte("Slot") & 0xFF;
                            if (slot >= vehInv.getSlots()) continue;
                            SupplyCrateEntity.insertItem(vehInv, slot, ItemStack.of((CompoundTag)itemTag));
                        }
                        rearmSuccess.set(true);
                    } else {
                        BlockEntity be;
                        long spawnerPosLong = vehicle.getPersistentData().getLong("AAS_SpawnerPos");
                        BlockPos spawnerPos = BlockPos.of((long)spawnerPosLong);
                        if (this.level().isLoaded(spawnerPos) && (be = this.level().getBlockEntity(spawnerPos)) instanceof VehicleSpawnerBlockEntity) {
                            VehicleSpawnerBlockEntity spawner = (VehicleSpawnerBlockEntity)be;
                            for (int i2 = 0; i2 < 32 && i2 < vehInv.getSlots(); ++i2) {
                                ItemStack sourceStack = spawner.inventory.getStackInSlot(i2 + 1);
                                if (sourceStack.isEmpty()) continue;
                                SupplyCrateEntity.insertItem(vehInv, i2, sourceStack.copy());
                            }
                            rearmSuccess.set(true);
                        }
                    }
                    if (rearmSuccess.get() && (batteryItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "large_battery"))) != null) {
                        for (i = 0; i < vehInv.getSlots(); ++i) {
                            if (!vehInv.getStackInSlot(i).isEmpty()) continue;
                            SupplyCrateEntity.insertItem(vehInv, i, new ItemStack((ItemLike)batteryItem));
                            break;
                        }
                    }
                });
                if (rearmSuccess.get()) {
                    vehicle.getPersistentData().putLong("AAS_NextSupplyTime", this.level().getGameTime() + 600L);
                    SupplyCrateEntity.sendMessageToPassengers(vehicle, "Vehicle Repaired & Rearmed by Crate!", ChatFormatting.GREEN);
                    actionDone = true;
                } else {
                    SupplyCrateEntity.sendMessageToPassengers(vehicle, "[Error] Rearm failed! Spawner chunk is unloaded (Old Vehicle).", ChatFormatting.RED);
                    actionDone = false;
                }
            }
            if (actionDone) {
                Level level = this.level();
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel)level;
                    serverLevel.sendParticles((ParticleOptions)ParticleTypes.HAPPY_VILLAGER, vehicle.getX(), vehicle.getY() + 1.5, vehicle.getZ(), 20, 1.0, 1.0, 1.0, 0.1);
                }
                vehicle.getPersistentData().putInt("AAS_CrateTimer", 0);
                this.hasResupplied = true;
                this.destroyCrate();
            } else {
                vehicle.getPersistentData().putInt("AAS_CrateTimer", 0);
                this.currentTargetUUID = null;
            }
        } else {
            vehicle.getPersistentData().putInt("AAS_CrateTimer", timer);
            SupplyCrateEntity.sendMessageToPassengers(vehicle, "Resupplying: " + (timeToWait - timer) + "s", ChatFormatting.AQUA);
        }
    }

    private static void insertItem(IItemHandler handler, int slot, ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable) {
            IItemHandlerModifiable modifiable = (IItemHandlerModifiable)handler;
            modifiable.setStackInSlot(slot, stack);
        } else {
            handler.insertItem(slot, stack, false);
        }
    }

    private static void sendMessageToPassengers(Entity vehicle, String msg, ChatFormatting color) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (!(passenger instanceof Player)) continue;
            Player player = (Player)passenger;
            player.displayClientMessage((Component)Component.literal((String)msg).withStyle(color), true);
        }
    }

    private void supplyNearbyHub() {
        if (this.getTeamOwner().equals("NEUTRAL")) {
            return;
        }
        ServerLevel currentLevel = (ServerLevel)this.level();
        AASWorldData data = AASWorldData.get(currentLevel);
        BlockPos myPos = this.blockPosition();
        double searchRadiusSq = 7500.0;
        String currentDim = currentLevel.dimension().location().toString();
        for (AASWorldData.HubInfo hubInfo : data.hubs) {
            Player player;
            HubBlockEntity hub;
            String hubTeam;
            BlockEntity be;
            BlockPos hubPos;
            if (hubInfo.dimension != null && !hubInfo.dimension.equals(currentDim) || !hubInfo.constructed || !((hubPos = hubInfo.pos).distSqr((Vec3i)myPos) <= searchRadiusSq) || !currentLevel.isLoaded(hubPos) || !((be = currentLevel.getBlockEntity(hubPos)) instanceof HubBlockEntity) || !(hubTeam = (hub = (HubBlockEntity)be).getTeam()).equalsIgnoreCase(this.getTeamOwner()) && !hubTeam.equals("NEUTRAL")) continue;
            if (hubTeam.equals("NEUTRAL")) {
                hub.setTeam(this.getTeamOwner());
                hubInfo.team = this.getTeamOwner();
                data.setDirty();
            }
            hub.addMaterials(this.getMaterials());
            this.hasResupplied = true;
            if (this.ownerId != null && (player = currentLevel.getPlayerByUUID(this.ownerId)) != null) {
                ChatFormatting color = this.getTeamOwner().equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
                player.displayClientMessage((Component)Component.literal((String)("FOB Resupplied! (+" + this.getMaterials() + " Mats)")).withStyle(color), true);
            }
            currentLevel.sendParticles((ParticleOptions)ParticleTypes.HAPPY_VILLAGER, (double)hubPos.getX() + 0.5, (double)hubPos.getY() + 1.5, (double)hubPos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.1);
            this.destroyCrate();
            return;
        }
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public boolean isPickable() {
        return true;
    }

    public boolean isPushable() {
        return false;
    }

    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setTeamOwner(tag.getString("Team"));
        this.setMaterials(tag.getInt("Materials"));
        this.hasResupplied = tag.getBoolean("HasResupplied");
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.currentTargetUUID = tag.getUUID("Target");
        }
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Team", this.getTeamOwner());
        tag.putInt("Materials", this.getMaterials());
        tag.putBoolean("HasResupplied", this.hasResupplied);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        if (this.currentTargetUUID != null) {
            tag.putUUID("Target", this.currentTargetUUID);
        }
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}


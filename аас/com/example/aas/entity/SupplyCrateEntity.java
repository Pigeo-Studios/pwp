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
    private static final EntityDataAccessor<Integer> MATERIALS = SynchedEntityData.m_135353_(SupplyCrateEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    private static final EntityDataAccessor<String> TEAM_OWNER = SynchedEntityData.m_135353_(SupplyCrateEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135030_);
    private boolean hasResupplied = false;
    private UUID ownerId = null;
    private UUID currentTargetUUID = null;

    public SupplyCrateEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.f_19811_ = true;
    }

    public SupplyCrateEntity(Level level, double x, double y, double z, String team, UUID ownerId) {
        this((EntityType)ModEntities.SUPPLY_CRATE.get(), level);
        this.m_6034_(x, y, z);
        this.setTeamOwner(team);
        this.setMaterials((Integer)AASConfig.SUPPLY_CRATE_MATERIALS.get());
        this.ownerId = ownerId;
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(MATERIALS, (Object)50);
        this.f_19804_.m_135372_(TEAM_OWNER, (Object)"NEUTRAL");
    }

    public int getMaterials() {
        return (Integer)this.f_19804_.m_135370_(MATERIALS);
    }

    public void setMaterials(int amount) {
        this.f_19804_.m_135381_(MATERIALS, (Object)amount);
    }

    public String getTeamOwner() {
        return (String)this.f_19804_.m_135370_(TEAM_OWNER);
    }

    public void setTeamOwner(String team) {
        this.f_19804_.m_135381_(TEAM_OWNER, (Object)team);
    }

    public boolean m_6097_() {
        return true;
    }

    public boolean m_6469_(DamageSource source, float amount) {
        if (this.m_9236_().f_46443_ || this.m_213877_()) {
            return false;
        }
        if (source.m_269533_(DamageTypeTags.f_268415_) || source.m_7639_() instanceof Player) {
            this.destroyCrate();
            return true;
        }
        return false;
    }

    public void destroyCrate() {
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.m_8767_((ParticleOptions)ParticleTypes.f_123777_, this.m_20185_(), this.m_20186_() + 0.5, this.m_20189_(), 5, 0.2, 0.2, 0.2, 0.05);
        }
        this.m_146870_();
    }

    public InteractionResult m_6096_(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (this.m_9236_().f_46443_) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.openCrateMenu(this.m_19879_()));
        }
        return InteractionResult.m_19078_((boolean)this.m_9236_().f_46443_);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_ && this.getMaterials() <= 0) {
            this.destroyCrate();
            return;
        }
        if (!this.m_20068_()) {
            this.m_20256_(this.m_20184_().m_82520_(0.0, -0.04, 0.0));
        }
        this.m_6478_(MoverType.SELF, this.m_20184_());
        float friction = 0.98f;
        if (this.m_20096_()) {
            friction = 0.7f;
        }
        this.m_20256_(this.m_20184_().m_82490_((double)friction));
        if (this.m_9236_().f_46443_) {
            return;
        }
        if (!this.hasResupplied && this.f_19797_ > 20 && this.f_19797_ % 20 == 0 && this.m_20096_()) {
            this.supplyNearbyHub();
            if (!this.m_213877_()) {
                this.supplyNearbyVehicle();
            }
        }
        if (this.f_19797_ > 6000) {
            this.m_146870_();
        }
    }

    private void supplyNearbyVehicle() {
        ServerLevel level = (ServerLevel)this.m_9236_();
        if (this.currentTargetUUID != null) {
            Entity target = level.m_8791_(this.currentTargetUUID);
            if (target == null || !target.m_6084_() || (double)target.m_20270_((Entity)this) > 15.0) {
                if (target != null) {
                    target.getPersistentData().m_128405_("AAS_CrateTimer", 0);
                }
                this.currentTargetUUID = null;
                return;
            }
            this.processVehicleLogic(target);
            return;
        }
        AABB searchArea = this.m_20191_().m_82400_(10.0);
        List vehicles = level.m_6249_((Entity)this, searchArea, e -> e.m_6084_() && e.getPersistentData().m_128441_("AAS_VehicleTeam"));
        vehicles.sort((e1, e2) -> {
            boolean p2;
            boolean p1 = !e1.m_20197_().isEmpty();
            boolean bl = p2 = !e2.m_20197_().isEmpty();
            if (p1 && !p2) {
                return -1;
            }
            if (!p1 && p2) {
                return 1;
            }
            return 0;
        });
        for (Entity vehicle : vehicles) {
            String vTeam = vehicle.getPersistentData().m_128461_("AAS_VehicleTeam");
            if (!this.getTeamOwner().equals("NEUTRAL") && !vTeam.isEmpty() && !vTeam.equalsIgnoreCase(this.getTeamOwner())) continue;
            boolean needsService = false;
            if (vehicle.getPersistentData().m_128441_("AAS_SpawnerPos")) {
                LivingEntity living;
                if (vehicle instanceof LivingEntity && (living = (LivingEntity)vehicle).m_21223_() < living.m_21233_()) {
                    needsService = true;
                }
                if (!vehicle.getPersistentData().m_128471_("AAS_IsSupplyTruck")) {
                    needsService = true;
                }
            }
            if (!needsService) continue;
            this.currentTargetUUID = vehicle.m_20148_();
            vehicle.getPersistentData().m_128405_("AAS_CrateTimer", 0);
            SupplyCrateEntity.sendMessageToPassengers(vehicle, "Connecting to Supply Crate...", ChatFormatting.YELLOW);
            return;
        }
    }

    private void processVehicleLogic(Entity vehicle) {
        int timer = vehicle.getPersistentData().m_128451_("AAS_CrateTimer");
        int timeToWait = 30;
        if (++timer >= timeToWait) {
            boolean actionDone = false;
            if (vehicle.getPersistentData().m_128441_("AAS_SpawnerPos")) {
                if (vehicle instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity)vehicle;
                    living.m_21153_(living.m_21233_());
                }
                AtomicBoolean rearmSuccess = new AtomicBoolean(false);
                vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(vehInv -> {
                    Item batteryItem;
                    int i;
                    if (vehInv instanceof IItemHandlerModifiable) {
                        IItemHandlerModifiable modifiable = (IItemHandlerModifiable)vehInv;
                        for (i = 0; i < vehInv.getSlots(); ++i) {
                            modifiable.setStackInSlot(i, ItemStack.f_41583_);
                        }
                    }
                    if (vehicle.getPersistentData().m_128441_("AAS_InitialLoadout")) {
                        ListTag loadoutTag = vehicle.getPersistentData().m_128437_("AAS_InitialLoadout", 10);
                        for (i = 0; i < loadoutTag.size(); ++i) {
                            CompoundTag itemTag = loadoutTag.m_128728_(i);
                            int slot = itemTag.m_128445_("Slot") & 0xFF;
                            if (slot >= vehInv.getSlots()) continue;
                            SupplyCrateEntity.insertItem(vehInv, slot, ItemStack.m_41712_((CompoundTag)itemTag));
                        }
                        rearmSuccess.set(true);
                    } else {
                        BlockEntity be;
                        long spawnerPosLong = vehicle.getPersistentData().m_128454_("AAS_SpawnerPos");
                        BlockPos spawnerPos = BlockPos.m_122022_((long)spawnerPosLong);
                        if (this.m_9236_().m_46749_(spawnerPos) && (be = this.m_9236_().m_7702_(spawnerPos)) instanceof VehicleSpawnerBlockEntity) {
                            VehicleSpawnerBlockEntity spawner = (VehicleSpawnerBlockEntity)be;
                            for (int i2 = 0; i2 < 32 && i2 < vehInv.getSlots(); ++i2) {
                                ItemStack sourceStack = spawner.inventory.getStackInSlot(i2 + 1);
                                if (sourceStack.m_41619_()) continue;
                                SupplyCrateEntity.insertItem(vehInv, i2, sourceStack.m_41777_());
                            }
                            rearmSuccess.set(true);
                        }
                    }
                    if (rearmSuccess.get() && (batteryItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "large_battery"))) != null) {
                        for (i = 0; i < vehInv.getSlots(); ++i) {
                            if (!vehInv.getStackInSlot(i).m_41619_()) continue;
                            SupplyCrateEntity.insertItem(vehInv, i, new ItemStack((ItemLike)batteryItem));
                            break;
                        }
                    }
                });
                if (rearmSuccess.get()) {
                    vehicle.getPersistentData().m_128356_("AAS_NextSupplyTime", this.m_9236_().m_46467_() + 600L);
                    SupplyCrateEntity.sendMessageToPassengers(vehicle, "Vehicle Repaired & Rearmed by Crate!", ChatFormatting.GREEN);
                    actionDone = true;
                } else {
                    SupplyCrateEntity.sendMessageToPassengers(vehicle, "[Error] Rearm failed! Spawner chunk is unloaded (Old Vehicle).", ChatFormatting.RED);
                    actionDone = false;
                }
            }
            if (actionDone) {
                Level level = this.m_9236_();
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel)level;
                    serverLevel.m_8767_((ParticleOptions)ParticleTypes.f_123748_, vehicle.m_20185_(), vehicle.m_20186_() + 1.5, vehicle.m_20189_(), 20, 1.0, 1.0, 1.0, 0.1);
                }
                vehicle.getPersistentData().m_128405_("AAS_CrateTimer", 0);
                this.hasResupplied = true;
                this.destroyCrate();
            } else {
                vehicle.getPersistentData().m_128405_("AAS_CrateTimer", 0);
                this.currentTargetUUID = null;
            }
        } else {
            vehicle.getPersistentData().m_128405_("AAS_CrateTimer", timer);
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
        for (Entity passenger : vehicle.m_20197_()) {
            if (!(passenger instanceof Player)) continue;
            Player player = (Player)passenger;
            player.m_5661_((Component)Component.m_237113_((String)msg).m_130940_(color), true);
        }
    }

    private void supplyNearbyHub() {
        if (this.getTeamOwner().equals("NEUTRAL")) {
            return;
        }
        ServerLevel currentLevel = (ServerLevel)this.m_9236_();
        AASWorldData data = AASWorldData.get(currentLevel);
        BlockPos myPos = this.m_20183_();
        double searchRadiusSq = 7500.0;
        String currentDim = currentLevel.m_46472_().m_135782_().toString();
        for (AASWorldData.HubInfo hubInfo : data.hubs) {
            Player player;
            HubBlockEntity hub;
            String hubTeam;
            BlockEntity be;
            BlockPos hubPos;
            if (hubInfo.dimension != null && !hubInfo.dimension.equals(currentDim) || !hubInfo.constructed || !((hubPos = hubInfo.pos).m_123331_((Vec3i)myPos) <= searchRadiusSq) || !currentLevel.m_46749_(hubPos) || !((be = currentLevel.m_7702_(hubPos)) instanceof HubBlockEntity) || !(hubTeam = (hub = (HubBlockEntity)be).getTeam()).equalsIgnoreCase(this.getTeamOwner()) && !hubTeam.equals("NEUTRAL")) continue;
            if (hubTeam.equals("NEUTRAL")) {
                hub.setTeam(this.getTeamOwner());
                hubInfo.team = this.getTeamOwner();
                data.m_77762_();
            }
            hub.addMaterials(this.getMaterials());
            this.hasResupplied = true;
            if (this.ownerId != null && (player = currentLevel.m_46003_(this.ownerId)) != null) {
                ChatFormatting color = this.getTeamOwner().equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
                player.m_5661_((Component)Component.m_237113_((String)("FOB Resupplied! (+" + this.getMaterials() + " Mats)")).m_130940_(color), true);
            }
            currentLevel.m_8767_((ParticleOptions)ParticleTypes.f_123748_, (double)hubPos.m_123341_() + 0.5, (double)hubPos.m_123342_() + 1.5, (double)hubPos.m_123343_() + 0.5, 20, 0.5, 0.5, 0.5, 0.1);
            this.destroyCrate();
            return;
        }
    }

    public boolean m_5829_() {
        return false;
    }

    public boolean m_6087_() {
        return true;
    }

    public boolean m_6094_() {
        return false;
    }

    protected void m_7378_(CompoundTag tag) {
        this.setTeamOwner(tag.m_128461_("Team"));
        this.setMaterials(tag.m_128451_("Materials"));
        this.hasResupplied = tag.m_128471_("HasResupplied");
        if (tag.m_128403_("Owner")) {
            this.ownerId = tag.m_128342_("Owner");
        }
        if (tag.m_128403_("Target")) {
            this.currentTargetUUID = tag.m_128342_("Target");
        }
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128359_("Team", this.getTeamOwner());
        tag.m_128405_("Materials", this.getMaterials());
        tag.m_128379_("HasResupplied", this.hasResupplied);
        if (this.ownerId != null) {
            tag.m_128362_("Owner", this.ownerId);
        }
        if (this.currentTargetUUID != null) {
            tag.m_128362_("Target", this.currentTargetUUID);
        }
    }

    public Packet<ClientGamePacketListener> m_5654_() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}


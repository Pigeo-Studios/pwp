/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraftforge.common.capabilities.ForgeCapabilities
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.IItemHandlerModifiable
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import com.example.aas.block.VehicleSpawnerBlockEntity;
import com.example.aas.config.AASConfig;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

public class MainSupplyBlockEntity
extends BlockEntity {
    private int checkTimer = 0;

    public MainSupplyBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.MAIN_SUPPLY_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MainSupplyBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        ++entity.checkTimer;
        if (entity.checkTimer < 20) {
            return;
        }
        entity.checkTimer = 0;
        if (((Boolean)AASConfig.MAIN_SUPPLY_HEALING.get()).booleanValue()) {
            int healRad = (Integer)AASConfig.MAIN_SUPPLY_HEAL_RADIUS.get();
            AABB healArea = new AABB(pos).inflate((double)healRad);
            List players = level.getEntitiesOfClass(Player.class, healArea);
            for (Player p : players) {
                if (!p.isAlive() || p.isSpectator()) continue;
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
            }
        }
        AABB searchArea = new AABB(pos).inflate(15.0);
        List nearbyEntities = level.getEntitiesOfClass(Entity.class, searchArea);
        nearbyEntities.sort((e1, e2) -> {
            boolean p1HasPlayer = e1.getPassengers().stream().anyMatch(p -> p instanceof Player);
            boolean p2HasPlayer = e2.getPassengers().stream().anyMatch(p -> p instanceof Player);
            if (p1HasPlayer && !p2HasPlayer) {
                return -1;
            }
            if (!p1HasPlayer && p2HasPlayer) {
                return 1;
            }
            return 0;
        });
        long currentTime = level.getGameTime();
        for (Entity vehicle : nearbyEntities) {
            StringBuilder statusMessage = new StringBuilder();
            boolean showActionBar = false;
            long lastSeenTime = vehicle.getPersistentData().getLong("AAS_LastSupplyTime");
            if (vehicle.getPersistentData().contains("AAS_VehicleMaxMats")) {
                int maxMats = vehicle.getPersistentData().getInt("AAS_VehicleMaxMats");
                vehicle.getPersistentData().putInt("AAS_VehicleMats", maxMats);
            }
            if (currentTime - lastSeenTime > 200L) {
                vehicle.getPersistentData().putInt("AAS_RepairTimer", 0);
                vehicle.getPersistentData().putInt("AAS_TruckReloadTimer", 0);
            }
            vehicle.getPersistentData().putLong("AAS_LastSupplyTime", currentTime);
            if (vehicle.getPersistentData().getBoolean("AAS_IsSupplyTruck")) {
                int maxCrates;
                int currentAmmo = vehicle.getPersistentData().getInt("AAS_SupplyAmmo");
                if (currentAmmo < (maxCrates = ((Integer)AASConfig.SUPPLY_TRUCK_CRATES.get()).intValue())) {
                    int supplyTimer = vehicle.getPersistentData().getInt("AAS_TruckReloadTimer");
                    if (++supplyTimer >= 15) {
                        vehicle.getPersistentData().putInt("AAS_SupplyAmmo", currentAmmo + 1);
                        vehicle.getPersistentData().putInt("AAS_TruckReloadTimer", 0);
                        MainSupplyBlockEntity.sendChatMessageToPassengers(vehicle, "[Supply] +1 Crate Loaded (" + (currentAmmo + 1) + "/" + maxCrates + ")", ChatFormatting.GOLD);
                        MainSupplyBlockEntity.spawnEffects(level, vehicle);
                    } else {
                        vehicle.getPersistentData().putInt("AAS_TruckReloadTimer", supplyTimer);
                        statusMessage.append(ChatFormatting.YELLOW).append("Loading Crate: ").append(15 - supplyTimer).append("s  ");
                        showActionBar = true;
                    }
                } else {
                    vehicle.getPersistentData().putInt("AAS_TruckReloadTimer", 0);
                }
            }
            if (vehicle.getPersistentData().contains("AAS_SpawnerPos")) {
                long nextSupplyTime = vehicle.getPersistentData().getLong("AAS_NextSupplyTime");
                if (currentTime < nextSupplyTime) {
                    long secondsLeft = (nextSupplyTime - currentTime) / 20L;
                    vehicle.getPersistentData().putInt("AAS_RepairTimer", 0);
                    statusMessage.append(ChatFormatting.RED).append("Rearm Cooldown: ").append(secondsLeft).append("s");
                    showActionBar = true;
                } else {
                    int repairTimer = vehicle.getPersistentData().getInt("AAS_RepairTimer");
                    if (++repairTimer >= 30) {
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
                            } else {
                                for (i = 0; i < vehInv.getSlots(); ++i) {
                                    vehInv.extractItem(i, 64, false);
                                }
                            }
                            if (vehicle.getPersistentData().contains("AAS_InitialLoadout")) {
                                ListTag loadoutTag = vehicle.getPersistentData().getList("AAS_InitialLoadout", 10);
                                for (i = 0; i < loadoutTag.size(); ++i) {
                                    CompoundTag itemTag = loadoutTag.getCompound(i);
                                    int slot = itemTag.getByte("Slot") & 0xFF;
                                    if (slot >= vehInv.getSlots()) continue;
                                    MainSupplyBlockEntity.insertItem(vehInv, slot, ItemStack.of((CompoundTag)itemTag));
                                }
                                rearmSuccess.set(true);
                            } else {
                                BlockEntity be;
                                long spawnerPosLong = vehicle.getPersistentData().getLong("AAS_SpawnerPos");
                                BlockPos spawnerPos = BlockPos.of((long)spawnerPosLong);
                                if (level.isLoaded(spawnerPos) && (be = level.getBlockEntity(spawnerPos)) instanceof VehicleSpawnerBlockEntity) {
                                    VehicleSpawnerBlockEntity spawner = (VehicleSpawnerBlockEntity)be;
                                    for (int i2 = 0; i2 < 32 && i2 < vehInv.getSlots(); ++i2) {
                                        ItemStack sourceStack = spawner.inventory.getStackInSlot(i2 + 1);
                                        if (sourceStack.isEmpty()) continue;
                                        MainSupplyBlockEntity.insertItem(vehInv, i2, sourceStack.copy());
                                    }
                                    rearmSuccess.set(true);
                                }
                            }
                            if (rearmSuccess.get() && (batteryItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "large_battery"))) != null) {
                                for (i = 0; i < vehInv.getSlots(); ++i) {
                                    if (!vehInv.getStackInSlot(i).isEmpty()) continue;
                                    MainSupplyBlockEntity.insertItem(vehInv, i, new ItemStack((ItemLike)batteryItem));
                                    break;
                                }
                            }
                        });
                        vehicle.getPersistentData().putInt("AAS_RepairTimer", 0);
                        if (rearmSuccess.get()) {
                            vehicle.getPersistentData().putLong("AAS_NextSupplyTime", currentTime + 6000L);
                            MainSupplyBlockEntity.sendChatMessageToPassengers(vehicle, "[Base] Vehicle Fully Rearmed & Repaired!", ChatFormatting.GREEN);
                            MainSupplyBlockEntity.spawnEffects(level, vehicle);
                        } else {
                            MainSupplyBlockEntity.sendChatMessageToPassengers(vehicle, "[Error] Rearm failed! Spawner chunk is unloaded (Old Vehicle).", ChatFormatting.RED);
                        }
                    } else {
                        vehicle.getPersistentData().putInt("AAS_RepairTimer", repairTimer);
                        statusMessage.append(ChatFormatting.AQUA).append("Rearming: ").append(30 - repairTimer).append("s");
                        showActionBar = true;
                    }
                }
            }
            if (!showActionBar || statusMessage.length() <= 0) continue;
            for (Entity passenger : vehicle.getPassengers()) {
                if (!(passenger instanceof Player)) continue;
                Player player = (Player)passenger;
                player.displayClientMessage((Component)Component.literal((String)statusMessage.toString()), true);
            }
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

    private static void sendActionBarToPassengers(Entity vehicle, String msg, ChatFormatting color) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (!(passenger instanceof Player)) continue;
            Player player = (Player)passenger;
            player.displayClientMessage((Component)Component.literal((String)msg).withStyle(color), true);
        }
    }

    private static void sendChatMessageToPassengers(Entity vehicle, String msg, ChatFormatting color) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (!(passenger instanceof Player)) continue;
            Player player = (Player)passenger;
            player.displayClientMessage((Component)Component.literal((String)msg).withStyle(color), false);
        }
    }

    private static void spawnEffects(Level level, Entity vehicle) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.sendParticles((ParticleOptions)ParticleTypes.HAPPY_VILLAGER, vehicle.getX(), vehicle.getY() + 1.5, vehicle.getZ(), 10, 1.0, 1.0, 1.0, 0.1);
        }
    }
}


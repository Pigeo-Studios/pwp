package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketVehicleStatus;
import com.pwp.api.time.GameClock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

// РЎСѓС‰РЅРѕСЃС‚СЊ Р±Р»РѕРєР° РіР»Р°РІРЅРѕРіРѕ
// СЃРЅР°Р±Р¶РµРЅРёСЏ
// Р›РµС‡РёС‚ РёРіСЂРѕРєРѕРІ, РїРѕРїРѕР»РЅСЏРµС‚
// Р±РѕРµРїСЂРёРїР°СЃС‹ РіСЂСѓР·РѕРІРёРєРѕРІ Рё
// РїРµСЂРµРІРѕРѕСЂСѓР¶Р°РµС‚/СЂРµРјРѕРЅС‚РёСЂСѓРµС‚
// С‚РµС…РЅРёРєСѓ
public class MainSupplyBlockEntity extends BlockEntity {
    // РўР°Р№РјРµСЂ РїСЂРѕРІРµСЂРєРё (РєР°Р¶РґС‹Рµ 20 С‚РёРєРѕРІ)
    private int checkTimer = 0;

    public MainSupplyBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType) ModBlocks.MAIN_SUPPLY_BE.get(), pos, state);
    }

    // РўРёРє СЃРЅР°Р±Р¶РµРЅРёСЏ: Р»РµС‡РµРЅРёРµ РёРіСЂРѕРєРѕРІ,
    // РїРѕРїРѕР»РЅРµРЅРёРµ РіСЂСѓР·РѕРІРёРєРѕРІ, СЂРµРјРѕРЅС‚
    // С‚РµС…РЅРёРєРё
    public static void tick(Level level, BlockPos pos, BlockState state, MainSupplyBlockEntity entity) {
        if (!level.isClientSide) {
            entity.checkTimer++;
            if (entity.checkTimer >= 20) {
                entity.checkTimer = 0;
                if ((Boolean) WarfareConfig.MAIN_SUPPLY_HEALING.get()) {
                    int healRad = (Integer) WarfareConfig.MAIN_SUPPLY_HEAL_RADIUS.get();
                    AABB healArea = new AABB(pos).inflate(healRad);

                    for (Player p : level.getEntitiesOfClass(Player.class, healArea)) {
                        if (p.isAlive() && !p.isSpectator()) {
                            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
                        }
                    }
                }

                AABB searchArea = new AABB(pos).inflate(15.0);
                List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class, searchArea);
                nearbyEntities.sort((e1, e2) -> {
                    boolean p1HasPlayer = e1.getPassengers().stream().anyMatch(px -> px instanceof Player);
                    boolean p2HasPlayer = e2.getPassengers().stream().anyMatch(px -> px instanceof Player);
                    if (p1HasPlayer && !p2HasPlayer) {
                        return -1;
                    } else {
                        return !p1HasPlayer && p2HasPlayer ? 1 : 0;
                    }
                });
                long currentTime = GameClock.worldTicks(level);

                for (Entity vehicle : nearbyEntities) {
                    int statusKind = 0;
                    int statusSeconds = 0;
                    long lastSeenTime = WarfareData.of(vehicle).getLong(WarfareKeys.LAST_SUPPLY_TIME);
                    if (WarfareData.of(vehicle).contains(WarfareKeys.VEHICLE_MAX_MATS)) {
                        int maxMats = WarfareData.of(vehicle).getInt(WarfareKeys.VEHICLE_MAX_MATS);
                        WarfareData.of(vehicle).putInt(WarfareKeys.VEHICLE_MATS, maxMats);
                    }

                    if (currentTime - lastSeenTime > 200L) {
                        WarfareData.of(vehicle).putInt(WarfareKeys.REPAIR_TIMER, 0);
                        WarfareData.of(vehicle).putInt(WarfareKeys.TRUCK_RELOAD_TIMER, 0);
                    }

                    WarfareData.of(vehicle).putLong(WarfareKeys.LAST_SUPPLY_TIME, currentTime);
                    if (WarfareData.of(vehicle).getBoolean(WarfareKeys.IS_SUPPLY_TRUCK)) {
                        int currentAmmo = WarfareData.of(vehicle).getInt(WarfareKeys.SUPPLY_AMMO);
                        int maxCrates = (Integer) WarfareConfig.SUPPLY_TRUCK_CRATES.get();
                        if (currentAmmo < maxCrates) {
                            int supplyTimer = WarfareData.of(vehicle).getInt(WarfareKeys.TRUCK_RELOAD_TIMER);
                            if (++supplyTimer >= 15) {
                                WarfareData.of(vehicle).putInt(WarfareKeys.SUPPLY_AMMO, currentAmmo + 1);
                                WarfareData.of(vehicle).putInt(WarfareKeys.TRUCK_RELOAD_TIMER, 0);
                                sendChatMessageToPassengers(
                                        vehicle,
                                        Component.translatable(
                                                "pwpwarfare.msg.supply.crate_loaded", currentAmmo + 1, maxCrates),
                                        ChatFormatting.GOLD);
                                spawnEffects(level, vehicle);
                            } else {
                                WarfareData.of(vehicle).putInt(WarfareKeys.TRUCK_RELOAD_TIMER, supplyTimer);
                                statusKind = PacketVehicleStatus.KIND_LOADING_CRATE;
                                statusSeconds = 15 - supplyTimer;
                            }
                        } else {
                            WarfareData.of(vehicle).putInt(WarfareKeys.TRUCK_RELOAD_TIMER, 0);
                        }
                    }

                    if (WarfareData.of(vehicle).contains(WarfareKeys.SPAWNER_POS)) {
                        long nextSupplyTime = WarfareData.of(vehicle).getLong(WarfareKeys.NEXT_SUPPLY_TIME);
                        if (currentTime < nextSupplyTime) {
                            long secondsLeft = (nextSupplyTime - currentTime) / 20L;
                            WarfareData.of(vehicle).putInt(WarfareKeys.REPAIR_TIMER, 0);
                            statusKind = PacketVehicleStatus.KIND_REARM_COOLDOWN;
                            statusSeconds = (int) secondsLeft;
                        } else {
                            int repairTimer = WarfareData.of(vehicle).getInt(WarfareKeys.REPAIR_TIMER);
                            if (++repairTimer >= 30) {
                                if (vehicle instanceof LivingEntity living) {
                                    living.setHealth(living.getMaxHealth());
                                }

                                long spawnerPosLong = WarfareData.of(vehicle).getLong(WarfareKeys.SPAWNER_POS);
                                BlockPos spawnerPos = BlockPos.of(spawnerPosLong);
                                if (level.isLoaded(spawnerPos)
                                        && level.getBlockEntity(spawnerPos)
                                                instanceof VehicleSpawnerBlockEntity spawner) {
                                    vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER)
                                            .ifPresent(vehInv -> {
                                                if (vehInv instanceof IItemHandlerModifiable modifiable) {
                                                    for (int i = 0; i < vehInv.getSlots(); i++) {
                                                        modifiable.setStackInSlot(i, ItemStack.EMPTY);
                                                    }
                                                }

                                                int slotsToCopy = 32;

                                                for (int i = 0; i < slotsToCopy && i < vehInv.getSlots(); i++) {
                                                    ItemStack sourceStack = spawner.inventory.getStackInSlot(i + 1);
                                                    if (!sourceStack.isEmpty()) {
                                                        insertItem(vehInv, i, sourceStack.copy());
                                                    }
                                                }

                                                Item batteryItem = (Item) ForgeRegistries.ITEMS.getValue(
                                                        new ResourceLocation("superbwarfare", "large_battery_pack"));
                                                if (batteryItem != null) {
                                                    for (int i = 0; i < vehInv.getSlots(); i++) {
                                                        if (vehInv.getStackInSlot(i)
                                                                .isEmpty()) {
                                                            ItemStack battery = new ItemStack(batteryItem);
                                                            // Полный заряд (ёмкость large_battery_pack = 20 000 000),
                                                            // иначе техника встанет без энергии
                                                            battery.getOrCreateTag()
                                                                    .putInt("Energy", 20000000);
                                                            insertItem(vehInv, i, battery);
                                                            break;
                                                        }
                                                    }
                                                }
                                            });
                                }

                                WarfareData.of(vehicle).putInt(WarfareKeys.REPAIR_TIMER, 0);
                                WarfareData.of(vehicle).putLong(WarfareKeys.NEXT_SUPPLY_TIME, currentTime + 600L);
                                sendChatMessageToPassengers(
                                        vehicle,
                                        Component.translatable("pwpwarfare.msg.supply.vehicle_restored"),
                                        ChatFormatting.GREEN);
                                spawnEffects(level, vehicle);
                            } else {
                                WarfareData.of(vehicle).putInt(WarfareKeys.REPAIR_TIMER, repairTimer);
                                statusKind = PacketVehicleStatus.KIND_REARMING;
                                statusSeconds = 30 - repairTimer;
                            }
                        }
                    }

                    if (statusKind != 0) {
                        sendVehicleStatus(vehicle, statusKind, statusSeconds);
                    }
                }
            }
        }
    }

    private static void insertItem(IItemHandler handler, int slot, ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
        } else {
            handler.insertItem(slot, stack, false);
        }
    }

    private static void sendVehicleStatus(Entity vehicle, int kind, int secondsLeft) {
        PacketVehicleStatus pkt = new PacketVehicleStatus(vehicle.getId(), kind, secondsLeft);
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof ServerPlayer sp) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), pkt);
            }
        }
    }

    private static void sendChatMessageToPassengers(Entity vehicle, Component msg, ChatFormatting color) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof Player player) {
                player.displayClientMessage(msg.copy().withStyle(color), false);
            }
        }
    }

    // РЎРїР°СѓРЅРёС‚ С‡Р°СЃС‚РёС†С‹ СЌС„С„РµРєС‚Р°
    // РїРѕРїРѕР»РЅРµРЅРёСЏ
    private static void spawnEffects(Level level, Entity vehicle) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    vehicle.getX(),
                    vehicle.getY() + 1.5,
                    vehicle.getZ(),
                    10,
                    1.0,
                    1.0,
                    1.0,
                    0.1);
        }
    }
}

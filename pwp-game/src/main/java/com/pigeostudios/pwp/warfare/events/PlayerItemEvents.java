package com.pigeostudios.pwp.warfare.events;

import static com.pigeostudios.pwp.warfare.events.GameLogicEvents.*;
import static com.pigeostudios.pwp.warfare.events.PlayerLifecycleEvents.*;
import static com.pigeostudios.pwp.warfare.events.VehicleLossEvents.*;

import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Предметы и мир игрока в матче: выброс предметов, эффекты и проверка водителя каждый тик, лимит мин миномёта, установка и ломание блоков, снаряды и взрывы, вход сущностей в мир.
 *
 * <p>Вынесено из {@link GameLogicEvents} без изменения кода (этап 13 ROADMAP).
 */
@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
public final class PlayerItemEvents {
    private PlayerItemEvents() {}

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() != null && !event.getPlayer().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            if (!player.isCreative()) {
                WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                if (data.match.isGameStarted) {
                    boolean preventAll = false;

                    try {
                        preventAll = (Boolean) WarfareConfig.PREVENT_ALL_ITEM_DROPS.get();
                    } catch (Exception ex) {
                        com.pwp.api.util.LogOnce.warn(
                                LOGGER, "GameLogicEvents:156", "GameLogicEvents.onItemToss: исключение", ex);
                    }

                    if (preventAll) {
                        event.setCanceled(true);
                        player.getInventory().add(event.getEntity().getItem());
                        player.displayClientMessage(
                                Component.translatable("pwpwarfare.msg.item.drop_disabled")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    } else if (isHeavyItem(event.getEntity().getItem().getItem())) {
                        event.setCanceled(true);
                        player.getInventory().add(event.getEntity().getItem());
                        player.displayClientMessage(
                                Component.translatable("pwpwarfare.msg.item.drop_heavy")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickEffects(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide) {
            if (event.player.tickCount % 10 == 0) {
                ServerPlayer player = (ServerPlayer) event.player;
                WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                if (data.match.isGameStarted && !player.isCreative()) {
                    boolean hasHeavyItem = false;

                    for (ItemStack stack : player.getInventory().items) {
                        if (!stack.isEmpty() && isHeavyItem(stack.getItem())) {
                            hasHeavyItem = true;
                            break;
                        }
                    }

                    if (!hasHeavyItem) {
                        for (ItemStack stack : player.getInventory().offhand) {
                            if (!stack.isEmpty() && isHeavyItem(stack.getItem())) {
                                hasHeavyItem = true;
                                break;
                            }
                        }
                    }

                    if (hasHeavyItem) {
                        player.addEffect(
                                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, true));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickDriveCheck(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide) {
            if ((Boolean) WarfareConfig.REQUIRE_SPECIALIST_TO_DRIVE.get()) {
                ServerPlayer player = (ServerPlayer) event.player;
                Entity vehicle = player.getVehicle();
                if (vehicle == null) {
                    WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                    WarfareData.of(player).remove(WarfareKeys.BOARDING_GRACE);
                    WarfareData.of(player).remove(WarfareKeys.LAST_VEHICLE_ID);
                } else {
                    int lastVehicleId = WarfareData.of(player).getInt(WarfareKeys.LAST_VEHICLE_ID);
                    if (lastVehicleId != vehicle.getId()) {
                        WarfareData.of(player).putInt(WarfareKeys.LAST_VEHICLE_ID, vehicle.getId());
                        WarfareData.of(player).putInt(WarfareKeys.BOARDING_GRACE, 0);
                        WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                    } else {
                        if (vehicle.getFirstPassenger() != player) {
                            WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                        } else if (!player.isCreative() && !player.isSpectator()) {
                            String vType = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TYPE);
                            if (vType.isEmpty() || vType.equals("DEFAULT")) {
                                WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                                return;
                            }
                            String pKit = KitUtil.getEffectiveKit(player);
                            if (!com.pigeostudios.pwp.warfare.vehicles.CrewRules.hasCorrectKit(vType, pKit)) {
                                player.stopRiding();
                                player.displayClientMessage(
                                        Component.translatable("pwpwarfare.msg.vehicle.crew_kit_required")
                                                .withStyle(ChatFormatting.RED),
                                        true);
                            } else {
                                WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerItemLogic(PlayerTickEvent event) {}

    static boolean isHeavyItem(Item item) {
        if (item == ModItems.AGS_AMMO.get()) {
            return true;
        }

        if (item == ModItems.M2_AMMO.get()) {
            return true;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) {
            String path = id.getPath();
            if (path.equals("mortar_shell")) {
                return true;
            }

            if (path.equals("medium_anti_ground_missile")) {
                return true;
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onBlockPlaced(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlacedBlock().getBlock() instanceof com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlock))
            return;
        net.minecraft.world.level.block.entity.BlockEntity be =
                ((net.minecraft.world.level.LevelAccessor) event.getLevel()).getBlockEntity(event.getPos());
        if (be instanceof VehicleSpawnerBlockEntity spawner) {
            net.minecraft.server.level.ServerLevel sLevel = (net.minecraft.server.level.ServerLevel) event.getLevel();
            WarfareWorldData data = WarfareWorldData.get(sLevel);
            data.vehicles.spawnerInfos.removeIf(s -> s.pos != null && s.pos.equals(event.getPos()));
            WarfareWorldData.SpawnerInfo info = spawner.getSpawnerInfo();
            if (info != null) data.vehicles.spawnerInfos.add(info);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getState().getBlock() instanceof com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlock)) return;
        net.minecraft.server.level.ServerLevel sLevel = (net.minecraft.server.level.ServerLevel) event.getLevel();
        WarfareWorldData data = WarfareWorldData.get(sLevel);
        data.vehicles.spawnerInfos.removeIf(s -> s.pos != null && s.pos.equals(event.getPos()));
    }

    @SubscribeEvent
    public static void onProjectileImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit) {
            Entity hitEntity = hit.getEntity();
            if (WarfareData.of(hitEntity).contains(WarfareKeys.TICKET_PENALTY)) {
                Entity projectile = event.getEntity();
                if (projectile instanceof net.minecraft.world.entity.projectile.Projectile proj) {
                    Entity owner = proj.getOwner();
                    if (owner instanceof ServerPlayer shooter) {
                        WarfareData.of(hitEntity).putString(WarfareKeys.LAST_DAMAGER, shooter.getStringUUID());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        if (!(level instanceof ServerLevel serverLevel)) return;

        WarfareWorldData data = WarfareWorldData.get(serverLevel);
        if (data.structures.mainZones.isEmpty()) return;

        event.getAffectedBlocks().removeIf(pos -> data.structures.mainZones.stream()
                .anyMatch(zone -> zone.isInside(Vec3.atCenterOf(pos))));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        Entity entity = event.getEntity();
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (rl != null
                && (rl.toString().equals("superbwarfare:tm_62") || rl.toString().equals("superbwarfare:claymore"))) {
            ServerLevel serverLevel = (ServerLevel) event.getLevel();
            WarfareWorldData data = WarfareWorldData.get(serverLevel);
            if (data.vehicles.markedVehicles.stream().anyMatch(v -> v.uuid.equals(entity.getUUID()))) return;
            String team = "NEUTRAL";
            if (WarfareData.of(entity).contains(WarfareKeys.VEHICLE_TEAM)) {
                team = WarfareData.of(entity).getString(WarfareKeys.VEHICLE_TEAM);
            } else {
                Player owner = serverLevel.getNearestPlayer(entity, 10.0);
                if (owner != null && owner.getTeam() != null) {
                    team = owner.getTeam().getName().toUpperCase();
                    WarfareData.of(entity).putString(WarfareKeys.VEHICLE_TEAM, team);
                    WarfareData.of(entity).putString(WarfareKeys.VEHICLE_TYPE, "Mine");
                }
            }
            if (!team.equals("NEUTRAL")) {
                data.vehicles.markedVehicles.add(new WarfareWorldData.VehicleRecord(
                        entity.getUUID(),
                        team,
                        "Mine",
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        entity.getYRot(),
                        null,
                        0));
                data.setDirty();
                PacketHandler.sendToAllClients(serverLevel, data);
            }
        }
    }
}

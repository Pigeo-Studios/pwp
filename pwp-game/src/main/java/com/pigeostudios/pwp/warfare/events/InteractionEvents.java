package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.block.AGSAmmoStackBlock;
import com.pigeostudios.pwp.warfare.block.AmmoStackBlockEntity;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlock;
import com.pigeostudios.pwp.warfare.block.HubBlock;
import com.pigeostudios.pwp.warfare.block.M2AmmoStackBlock;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.MortarShellStackBlock;
import com.pigeostudios.pwp.warfare.block.TOWMissileStackBlock;
import com.pigeostudios.pwp.warfare.block.WallBlock;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.item.AGSAmmoItem;
import com.pigeostudios.pwp.warfare.item.M2AmmoItem;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags.Blocks;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Open;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
// Обработчик взаимодействий игрока с миром
// Управляет использованием блоков, установкой конструкций и взаимодействием с техникой
public class InteractionEvents {
    private static final Set<String> ALLOWED_INVENTORY_ENTITIES = Set.of(
            "superbwarfare:mortar",
            "fpvdrone:drone",
            "superbwarfare:drone",
            "uncomplicatedfpv:fpv_drone",
            "uncomplicatedfpv:mavic_drone_no_drop",
            "uncomplicatedfpv:mavic_drone_with_drop",
            "uncomplicatedfpv:reb",
            "uncomplicatedfpv:reb_mini",
            "pwpwarfare:supply_crate",
            "vvp:mi8_mtv3",
            "vvp:mi8",
            "vvp:mi8_amtsh");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGlobalEntityInteract(EntityInteract event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            Player player = event.getEntity();
            if (!player.isCreative() && !player.isSpectator()) {
                if (player.isShiftKeyDown()) {
                    Entity target = event.getTarget();
                    ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                    if (entityKey != null && ALLOWED_INVENTORY_ENTITIES.contains(entityKey.toString())) {
                        return;
                    }

                    if (!event.getLevel().isClientSide && WarfareData.of(target).contains(WarfareKeys.VEHICLE_TEAM)) {
                        String vType = WarfareData.of(target).getString(WarfareKeys.VEHICLE_TYPE);
                        String vTeam = WarfareData.of(target).getString(WarfareKeys.VEHICLE_TEAM);
                        String pTeam =
                                player.getTeam() != null ? player.getTeam().getName() : "";
                        if (!vType.equalsIgnoreCase("Static ZU") && vTeam.equalsIgnoreCase(pTeam)) {
                            ServerPlayer sPlayer = (ServerPlayer) player;
                            int currentMats = WarfareData.of(target).getInt(WarfareKeys.VEHICLE_MATS);
                            int cost = (Integer) WarfareConfig.HUB_RESUPPLY_COST.get();
                            if (currentMats < cost) {
                                sPlayer.displayClientMessage(
                                        Component.translatable("pwpwarfare.msg.vehicle.not_enough_mats", currentMats)
                                                .withStyle(ChatFormatting.RED),
                                        true);
                                event.setCanceled(true);
                                event.setCancellationResult(InteractionResult.SUCCESS);
                                return;
                            }

                            String pendingKit = WarfareData.of(sPlayer).getString(WarfareKeys.PENDING_KIT);
                            String currentKit = WarfareData.of(sPlayer).getString(WarfareKeys.CURRENT_KIT);
                            boolean hasPending = !pendingKit.isEmpty();
                            String targetKitName = hasPending ? pendingKit : currentKit;
                            if (targetKitName != null
                                    && !targetKitName.isEmpty()
                                    && !targetKitName.equals("Unassigned")) {
                                WarfareWorldData data = WarfareWorldData.get(sPlayer.serverLevel());
                                if (hasPending) {
                                    ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                                    if (!sPlayer.isCreative()) {
                                        WarfareData.of(target).putInt(WarfareKeys.VEHICLE_MATS, currentMats - cost);
                                    }

                                    sPlayer.displayClientMessage(
                                            Component.translatable("pwpwarfare.msg.vehicle.kit_new", currentMats - cost)
                                                    .withStyle(ChatFormatting.GREEN),
                                            true);
                                    sPlayer.level()
                                            .playSound(
                                                    null,
                                                    sPlayer.blockPosition(),
                                                    SoundEvents.ITEM_PICKUP,
                                                    SoundSource.PLAYERS,
                                                    1.0F,
                                                    1.0F);
                                } else {
                                    WarfareWorldData.KitInfo kit = pTeam.equalsIgnoreCase("BLUE")
                                            ? data.blueKits.get(currentKit)
                                            : data.redKits.get(currentKit);
                                    if (kit != null) {
                                        if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                                            if (!sPlayer.isCreative()) {
                                                WarfareData.of(target)
                                                        .putInt(WarfareKeys.VEHICLE_MATS, currentMats - cost);
                                            }

                                            sPlayer.displayClientMessage(
                                                    Component.translatable(
                                                                    "pwpwarfare.msg.vehicle.kit_resupplied",
                                                                    currentMats - cost)
                                                            .withStyle(ChatFormatting.GREEN),
                                                    true);
                                            sPlayer.level()
                                                    .playSound(
                                                            null,
                                                            sPlayer.blockPosition(),
                                                            SoundEvents.ITEM_PICKUP,
                                                            SoundSource.PLAYERS,
                                                            1.0F,
                                                            1.0F);
                                        } else {
                                            sPlayer.displayClientMessage(
                                                    Component.translatable(
                                                                    "pwpwarfare.msg.vehicle.ammo_full", currentMats)
                                                            .withStyle(ChatFormatting.YELLOW),
                                                    true);
                                        }
                                    }
                                }

                                event.setCanceled(true);
                                event.setCancellationResult(InteractionResult.SUCCESS);
                                return;
                            }

                            sPlayer.displayClientMessage(
                                    Component.translatable("pwpwarfare.msg.kit.not_equipped")
                                            .withStyle(ChatFormatting.RED),
                                    true);
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS);
                            return;
                        }
                    }

                    if ((Boolean) WarfareConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()) {
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        if (event.getLevel().isClientSide) {
                            player.displayClientMessage(
                                    Component.translatable("pwpwarfare.msg.inventory.disabled")
                                            .withStyle(ChatFormatting.RED),
                                    true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onContainerOpen(Open event) {
        Player player = event.getEntity();
        if ((Boolean) WarfareConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()
                && !player.isCreative()
                && player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            ResourceLocation vehicleKey = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
            if (vehicleKey != null && ALLOWED_INVENTORY_ENTITIES.contains(vehicleKey.toString())) {
                return;
            }

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.server.execute(serverPlayer::closeContainer);
            } else {
                player.closeContainer();
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(LeftClickBlock event) {
        if ((Boolean) WarfareConfig.PREVENT_BLOCK_BREAKING.get()) {
            Player player = event.getEntity();
            if (player.isCreative()) {
                return;
            }

            Level level = event.getLevel();
            boolean isStarted = false;
            if (level instanceof ServerLevel serverLevel) {
                WarfareWorldData data = WarfareWorldData.get(serverLevel);
                isStarted = data.match.isGameStarted || data.match.waitingActive;
            } else if (level.isClientSide) {
                isStarted = ClientData.isGameStarted;
            }

            if (isStarted) {
                BlockState state = level.getBlockState(event.getPos());
                if (!isBlockWhitelisted(state)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(EntityPlaceEvent event) {
        if (event.getPlacedBlock().is((Block) ModBlocks.GAME_START_TRIGGER.get())
                && event.getLevel() instanceof ServerLevel level) {
            WarfareWorldData.get(level).structures.triggerBlocks.add(event.getPos());
            WarfareWorldData.get(level).setDirty();
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakEvent event) {
        if (event.getState().is((Block) ModBlocks.GAME_START_TRIGGER.get())
                && event.getLevel() instanceof ServerLevel level) {
            WarfareWorldData.get(level).structures.triggerBlocks.remove(event.getPos());
            WarfareWorldData.get(level).setDirty();
        }

        if ((Boolean) WarfareConfig.PREVENT_BLOCK_BREAKING.get()) {
            Player player = event.getPlayer();
            if (!player.isCreative() && event.getLevel() instanceof ServerLevel serverLevel) {
                WarfareWorldData data = WarfareWorldData.get(serverLevel);
                boolean isStarted = data.match.isGameStarted || data.match.waitingActive;
                if (isStarted && !isBlockWhitelisted(event.getState())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean isBlockWhitelisted(BlockState state) {
        if (state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.LEAVES)) {
            return true;
        } else if (state.hasProperty(WallBlock.CONSTRUCTED) && !(Boolean) state.getValue(WallBlock.CONSTRUCTED)) {
            return true;
        } else if (state.hasProperty(BarbedWireBlock.CONSTRUCTED)
                && !(Boolean) state.getValue(BarbedWireBlock.CONSTRUCTED)) {
            return true;
        } else if (state.hasProperty(HubBlock.CONSTRUCTED) && !(Boolean) state.getValue(HubBlock.CONSTRUCTED)) {
            return true;
        } else if (!state.is((Block) ModBlocks.M2_CONSTRUCTION_BLOCK.get())
                && !state.is((Block) ModBlocks.AGS_CONSTRUCTION_BLOCK.get())
                && !state.is((Block) ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get())
                && !state.is((Block) ModBlocks.TOW_CONSTRUCTION_BLOCK.get())
                && !state.is((Block) ModBlocks.REB_CONSTRUCTION_BLOCK.get())) {
            boolean isDefense =
                    state.is((Block) ModBlocks.WALL_BLOCK.get()) || state.is((Block) ModBlocks.BARBED_WIRE_BLOCK.get());
            boolean allowDefenses = (Boolean) WarfareConfig.ALLOW_BREAKING_DEFENSES.get();
            return state.is((Block) ModBlocks.BLUE_RALLY_BLOCK.get())
                    || state.is((Block) ModBlocks.RED_RALLY_BLOCK.get())
                    || state.is((Block) ModBlocks.AMMO_BAG_BLOCK.get())
                    || isDefense && allowDefenses
                    || state.is(Blocks.GLASS)
                    || state.is(Blocks.GLASS_PANES)
                    || state.is(BlockTags.IMPERMEABLE);
        } else {
            return true;
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            Player player = event.getEntity();
            Level level = event.getLevel();
            ItemStack heldItem = event.getItemStack();
            BlockPos clickedPos = event.getPos();
            Direction face = event.getFace();
            BlockState clickedState = level.getBlockState(clickedPos);
            if (clickedState.is((Block) ModBlocks.MAIN_SUPPLY_BLOCK.get())) {
                if (!level.isClientSide) {
                    ServerPlayer sPlayer = (ServerPlayer) player;
                    WarfareWorldData data = WarfareWorldData.get(sPlayer.serverLevel());
                    // в стейджинге склад работает (кит выдаётся уже при выборе роли, этап 4)
                    if (!data.match.isGameStarted
                            && !com.pigeostudios.pwp.warfare.staging.StagingService.isStaging(data)
                            && !sPlayer.isCreative()) {
                        sPlayer.sendSystemMessage(Component.translatable("pwpwarfare.message.match_not_started")
                                .withStyle(ChatFormatting.RED));
                        event.setCanceled(true);
                        return;
                    }

                    if (data.match.invasionSetupActive && !sPlayer.isCreative()) {
                        String pTeam = sPlayer.getTeam() != null
                                ? sPlayer.getTeam().getName().toUpperCase()
                                : "";
                        if (!pTeam.equalsIgnoreCase(data.match.invasionDefender)) {
                            sPlayer.sendSystemMessage(Component.translatable("pwpwarfare.msg.main_supply.setup")
                                    .withStyle(ChatFormatting.RED));
                            event.setCanceled(true);
                            return;
                        }
                    }

                    long lastMainUse = WarfareData.of(sPlayer).getLong(WarfareKeys.LAST_MAIN_RESUPPLY);
                    long currentTime = GameClock.worldTicks(sPlayer.level());
                    if (!sPlayer.isCreative() && currentTime < lastMainUse + 1200L) {
                        long secondsLeft = (lastMainUse + 1200L - currentTime) / 20L;
                        sPlayer.displayClientMessage(
                                Component.translatable("pwpwarfare.msg.main_supply.cooldown", secondsLeft)
                                        .withStyle(ChatFormatting.RED),
                                true);
                        event.setCanceled(true);
                        return;
                    }

                    if (WarfareData.of(sPlayer).contains(WarfareKeys.PENDING_KIT)) {
                        ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                        WarfareData.of(sPlayer).putLong(WarfareKeys.LAST_MAIN_RESUPPLY, currentTime);
                    } else {
                        String kitName = WarfareData.of(sPlayer).getString(WarfareKeys.CURRENT_KIT);
                        if (!kitName.isEmpty() && sPlayer.getTeam() != null) {
                            String t = sPlayer.getTeam().getName().toUpperCase();
                            WarfareWorldData.KitInfo kit =
                                    t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                            if (kit != null) {
                                if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                                    sPlayer.sendSystemMessage(Component.translatable("pwpwarfare.msg.kit.resupplied")
                                            .withStyle(ChatFormatting.GREEN));
                                    sPlayer.level()
                                            .playSound(
                                                    null,
                                                    sPlayer.blockPosition(),
                                                    SoundEvents.ITEM_PICKUP,
                                                    SoundSource.PLAYERS,
                                                    1.0F,
                                                    1.0F);
                                    WarfareData.of(sPlayer).putLong(WarfareKeys.LAST_MAIN_RESUPPLY, currentTime);
                                } else {
                                    sPlayer.sendSystemMessage(Component.translatable("pwpwarfare.msg.kit.full")
                                            .withStyle(ChatFormatting.YELLOW));
                                }
                            }
                        }
                    }
                }

                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else if (face != null) {
                BlockPos placePos = clickedPos.relative(face);
                if (MortarShellStackBlock.isMortarItem(heldItem)) {
                    if (clickedState.getBlock() == ModBlocks.MORTAR_SHELL_STACK_BLOCK.get()) {
                        return;
                    }

                    if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos)) {
                        BlockState newState = (BlockState) ((Block) ModBlocks.MORTAR_SHELL_STACK_BLOCK.get())
                                .defaultBlockState()
                                .setValue(
                                        MortarShellStackBlock.FACING,
                                        player.getDirection().getOpposite());
                        if (newState.canSurvive(level, placePos)) {
                            if (!level.isClientSide) {
                                level.setBlock(placePos, newState, 3);
                                level.playSound(
                                        null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                if (!player.isCreative()) {
                                    heldItem.shrink(1);
                                }
                            }

                            player.swing(event.getHand());
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS);
                        }
                    }
                } else if (TOWMissileStackBlock.isTOWItem(heldItem)) {
                    if (clickedState.getBlock() == ModBlocks.TOW_MISSILE_STACK_BLOCK.get()) {
                        return;
                    }

                    if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos)) {
                        BlockState newState = (BlockState) ((Block) ModBlocks.TOW_MISSILE_STACK_BLOCK.get())
                                .defaultBlockState()
                                .setValue(
                                        TOWMissileStackBlock.FACING,
                                        player.getDirection().getOpposite());
                        if (newState.canSurvive(level, placePos)) {
                            if (!level.isClientSide) {
                                level.setBlock(placePos, newState, 3);
                                level.playSound(
                                        null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                if (!player.isCreative()) {
                                    heldItem.shrink(1);
                                }
                            }

                            player.swing(event.getHand());
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS);
                        }
                    }
                } else if (heldItem.getItem() == ModItems.AGS_AMMO.get()) {
                    if (clickedState.getBlock() == ModBlocks.AGS_AMMO_STACK_BLOCK.get()) {
                        return;
                    }

                    if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos)) {
                        BlockState newState = (BlockState) ((Block) ModBlocks.AGS_AMMO_STACK_BLOCK.get())
                                .defaultBlockState()
                                .setValue(
                                        AGSAmmoStackBlock.FACING,
                                        player.getDirection().getOpposite());
                        if (newState.canSurvive(level, placePos)) {
                            if (!level.isClientSide) {
                                level.setBlock(placePos, newState, 3);
                                if (level.getBlockEntity(placePos) instanceof AmmoStackBlockEntity ammoBe) {
                                    int ammo = AGSAmmoItem.getAmmo(heldItem);
                                    ammoBe.addAmmoBox(ammo);
                                }

                                level.playSound(
                                        null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                if (!player.isCreative()) {
                                    heldItem.shrink(1);
                                }
                            }

                            player.swing(event.getHand());
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS);
                        }
                    }
                } else if (heldItem.getItem() == ModItems.M2_AMMO.get()) {
                    if (clickedState.getBlock() == ModBlocks.M2_AMMO_STACK_BLOCK.get()) {
                        return;
                    }

                    if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos)) {
                        BlockState newState = (BlockState) ((Block) ModBlocks.M2_AMMO_STACK_BLOCK.get())
                                .defaultBlockState()
                                .setValue(
                                        M2AmmoStackBlock.FACING,
                                        player.getDirection().getOpposite());
                        if (newState.canSurvive(level, placePos)) {
                            if (!level.isClientSide) {
                                level.setBlock(placePos, newState, 3);
                                if (level.getBlockEntity(placePos) instanceof AmmoStackBlockEntity ammoBe) {
                                    int ammo = M2AmmoItem.getAmmo(heldItem);
                                    ammoBe.addAmmoBox(ammo);
                                }

                                level.playSound(
                                        null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                if (!player.isCreative()) {
                                    heldItem.shrink(1);
                                }
                            }

                            player.swing(event.getHand());
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGlobalVehicleInteract(EntityInteract event) {
        Player player = event.getEntity();
        if ((Boolean) WarfareConfig.PREVENT_ENEMY_VEHICLE_ENTRY.get()) {
            if (!player.isCreative() && !player.isSpectator()) {
                Entity target = event.getTarget();
                if (WarfareData.of(target).contains(WarfareKeys.VEHICLE_TEAM)) {
                    String vTeam = WarfareData.of(target).getString(WarfareKeys.VEHICLE_TEAM);
                    if (!vTeam.isEmpty() && !vTeam.equalsIgnoreCase("NEUTRAL")) {
                        String pTeam = player.getTeam() != null
                                ? player.getTeam().getName().toUpperCase()
                                : "NEUTRAL";
                        if (!vTeam.equalsIgnoreCase(pTeam)) {
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.FAIL);
                            player.displayClientMessage(
                                    Component.translatable("pwpwarfare.msg.vehicle.enemy")
                                            .withStyle(ChatFormatting.RED),
                                    true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMountVehicle(EntityMountEvent event) {
        if (!event.isMounting()) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) return;

        if (!(Boolean) WarfareConfig.REQUIRE_SL_PERMISSION_TO_DRIVE.get()) return;
        if (player.isCreative() || player.isSpectator()) return;

        Entity vehicle = event.getEntityBeingMounted();
        if (!WarfareData.of(vehicle).contains(WarfareKeys.VEHICLE_TEAM)) return;

        String vType = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TYPE);
        if (!com.pigeostudios.pwp.warfare.vehicles.CrewRules.isSpecialist(vType)) return;

        if (!vehicle.getPassengers().isEmpty()) return;

        String pKit = KitUtil.getEffectiveKit(player);
        if (!com.pigeostudios.pwp.warfare.vehicles.CrewRules.hasCorrectKit(vType, pKit)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("pwpwarfare.msg.vehicle.crew_kit_required_this")
                            .withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        int squadId = WarfareData.of(player).getInt(WarfareKeys.SQUAD_ID);
        if (squadId == 0) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("pwpwarfare.msg.squad.not_in_squad").withStyle(ChatFormatting.RED), true);
            return;
        }

        boolean isSL = WarfareData.of(player).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
        boolean isFresh = WarfareData.of(vehicle).getBoolean(WarfareKeys.FRESH_VEHICLE);
        if (!isFresh) return;

        UUID vehicleUUID = vehicle.getUUID();
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());

        if (isSL || data.hasApprovedDriver(vehicleUUID, player.getUUID())) {
            WarfareData.of(vehicle).remove(WarfareKeys.FRESH_VEHICLE);
            return;
        }

        long cooldownUntil = WarfareData.of(player).getLong(WarfareKeys.driveRequestCooldown(vehicle.getId()));
        if (cooldownUntil > GameClock.worldTicks(player.level())) {
            long remaining = (cooldownUntil - GameClock.worldTicks(player.level())) / 20L;
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("pwpwarfare.msg.vehicle.request_cooldown", remaining)
                            .withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        event.setCanceled(true);
        Component requestMsg = Component.translatable("pwpwarfare.msg.vehicle.need_sl_permission")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("pwpwarfare.msg.vehicle.send_request")
                        .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                                .withClickEvent(
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vrequest " + vehicle.getId()))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("pwpwarfare.msg.vehicle.send_request_hover")))));
        player.sendSystemMessage(requestMsg);
    }

    /**
     * Закрепление техники за отрядом, как в Squad (этап 10c, {@code community.vehicleClaim}): садится ВОДИТЕЛЕМ
     * игрок отряда — машина за этим отрядом; водить её может только он, пока отряд жив (модераторы — всегда).
     * Пассажиром — можно всем, кроме машины с замком «только мой отряд» (этап 17a, радиалка техники).
     * Закрепление вручную (радиалка) действует и при выключенном авто-закреплении. Решение —
     * {@link com.pigeostudios.pwp.warfare.vehicles.VehicleClaim#decideSeat}. Идёт после проверки кита/разрешения
     * лидера (HIGHEST), отменённую посадку не трогает.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMountClaim(EntityMountEvent event) {
        if (!event.isMounting() || event.getLevel().isClientSide()) return;
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) return;
        boolean autoClaim = com.pwp.api.ServerRole.settings().community().vehicleClaim();
        Entity vehicle = event.getEntityBeingMounted();
        if (!WarfareData.of(vehicle).contains(WarfareKeys.VEHICLE_TEAM)) return;
        if (player.isSpectator()) return;
        boolean driver = vehicle.getPassengers().isEmpty();

        String team = player.getTeam() == null ? "" : player.getTeam().getName();
        int claim = WarfareData.of(vehicle).getInt(WarfareKeys.VEHICLE_CLAIM_SQUAD);
        if (!autoClaim && claim == 0) return; // авто-закрепление выключено, вручную не закрепляли
        String claimTeam = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_CLAIM_TEAM);
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        WarfareWorldData.Squad owner = claim == 0
                ? null
                : data.squadRegistry.squads.stream()
                        .filter(s -> s.id == claim && s.team.equalsIgnoreCase(claimTeam))
                        .findFirst()
                        .orElse(null);
        int mySquad = WarfareData.of(player).getInt(WarfareKeys.SQUAD_ID);
        // id отрядов уникальны только внутри команды: чужая команда не совпадает никогда
        int mine = claimTeam.isEmpty() || claimTeam.equalsIgnoreCase(team) ? mySquad : -1;
        // isCreative() правом не является (AGENTS.md): водить чужую закреплённую машину — только модератор+
        boolean staff = com.pwp.api.security.PwpPermissions.has(player, com.pwp.api.security.AccessLevel.MODERATOR);
        boolean locked = WarfareData.of(vehicle).getBoolean(WarfareKeys.VEHICLE_LOCKED);
        var decision = com.pigeostudios.pwp.warfare.vehicles.VehicleClaim.decideSeat(
                claim, owner != null, mine, staff, locked, driver);
        if (decision == com.pigeostudios.pwp.warfare.vehicles.VehicleClaim.Decision.CLAIM && !autoClaim) return;
        switch (decision) {
            case DENY -> {
                event.setCanceled(true);
                com.pigeostudios.pwp.warfare.util.PlayerFeedback.deny(
                        player,
                        driver ? "pwpwarfare.msg.vehicle.claimed" : "pwpwarfare.vehicle_menu.locked_deny",
                        owner == null ? "?" : owner.name);
            }
            case CLAIM -> {
                WarfareData.of(vehicle).putInt(WarfareKeys.VEHICLE_CLAIM_SQUAD, mySquad);
                WarfareData.of(vehicle).putString(WarfareKeys.VEHICLE_CLAIM_TEAM, team);
                String squadName = data.squadRegistry.squads.stream()
                        .filter(s -> s.id == mySquad && s.team.equalsIgnoreCase(team))
                        .map(s -> s.name)
                        .findFirst()
                        .orElse("#" + mySquad);
                com.pigeostudios.pwp.warfare.util.PlayerFeedback.info(
                        player, "pwpwarfare.msg.vehicle.claimed_by_you", squadName);
            }
            case ALLOW -> {}
        }
    }
}

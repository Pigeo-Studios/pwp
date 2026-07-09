/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraftforge.common.Tags$Blocks
 *  net.minecraftforge.event.entity.player.PlayerContainerEvent$Open
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickBlock
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.minecraftforge.event.level.BlockEvent$BreakEvent
 *  net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.example.aas.events;

import com.example.aas.block.AGSAmmoStackBlock;
import com.example.aas.block.AmmoStackBlockEntity;
import com.example.aas.block.BarbedWireBlock;
import com.example.aas.block.HubBlock;
import com.example.aas.block.M2AmmoStackBlock;
import com.example.aas.block.ModBlocks;
import com.example.aas.block.MortarShellStackBlock;
import com.example.aas.block.TOWMissileStackBlock;
import com.example.aas.block.WallBlock;
import com.example.aas.client.ClientData;
import com.example.aas.config.AASConfig;
import com.example.aas.item.AGSAmmoItem;
import com.example.aas.item.M2AmmoItem;
import com.example.aas.item.ModItems;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.world.AASWorldData;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid="aas", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class InteractionEvents {
    static private final Set<String> ALLOWED_INVENTORY_ENTITIES = Set.of("superbwarfare:mortar", "fpvdrone:drone", "superbwarfare:drone", "wrbdrones:fpv_drone", "wrbdrones:mavic_drone_no_drop", "wrbdrones:mavic_drone_with_drop", "aas:supply_crate", "vvp:mi8_mtv3", "vvp:mi8", "vvp:mi8_amtsh");

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onGlobalEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            Entity target = event.getTarget();
            ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey((Object)target.getType());
            if (entityKey != null && ALLOWED_INVENTORY_ENTITIES.contains(entityKey.toString())) {
                return;
            }
            if (!event.getLevel().isClientSide && target.getPersistentData().contains("AAS_VehicleTeam")) {
                String pTeam;
                String vType = target.getPersistentData().getString("AAS_VehicleType");
                String vTeam = target.getPersistentData().getString("AAS_VehicleTeam");
                String string = pTeam = player.getTeam() != null ? player.getTeam().getName() : "";
                if (!vType.equalsIgnoreCase("Static ZU") && vTeam.equalsIgnoreCase(pTeam)) {
                    String targetKitName;
                    int cost;
                    ServerPlayer sPlayer = (ServerPlayer)player;
                    int currentMats = target.getPersistentData().getInt("AAS_VehicleMats");
                    if (currentMats < (cost = ((Integer)AASConfig.HUB_RESUPPLY_COST.get()).intValue())) {
                        sPlayer.displayClientMessage((Component)Component.literal((String)("Not enough Materials in Vehicle! (" + currentMats + ")")).withStyle(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        return;
                    }
                    String pendingKit = sPlayer.getPersistentData().getString("AAS_PendingKit");
                    String currentKit = sPlayer.getPersistentData().getString("AAS_CurrentKit");
                    boolean hasPending = !pendingKit.isEmpty();
                    String string2 = targetKitName = hasPending ? pendingKit : currentKit;
                    if (targetKitName == null || targetKitName.isEmpty() || targetKitName.equals("Unassigned")) {
                        sPlayer.displayClientMessage((Component)Component.literal((String)"No Kit equipped!").withStyle(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        return;
                    }
                    AASWorldData data = AASWorldData.get(sPlayer.serverLevel());
                    if (hasPending) {
                        ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                        if (!sPlayer.isCreative()) {
                            target.getPersistentData().putInt("AAS_VehicleMats", currentMats - cost);
                        }
                        sPlayer.displayClientMessage((Component)Component.literal((String)("New Kit Equipped! Vehicle Mats: " + (currentMats - cost))).withStyle(ChatFormatting.GREEN), true);
                        sPlayer.level().playSound(null, sPlayer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    } else {
                        AASWorldData.KitInfo kit;
                        AASWorldData.KitInfo kitInfo = kit = pTeam.equalsIgnoreCase("BLUE") ? data.blueKits.get(currentKit) : data.redKits.get(currentKit);
                        if (kit != null) {
                            if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                                if (!sPlayer.isCreative()) {
                                    target.getPersistentData().putInt("AAS_VehicleMats", currentMats - cost);
                                }
                                sPlayer.displayClientMessage((Component)Component.literal((String)("Kit Resupplied! Vehicle Mats: " + (currentMats - cost))).withStyle(ChatFormatting.GREEN), true);
                                sPlayer.level().playSound(null, sPlayer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                            } else {
                                sPlayer.displayClientMessage((Component)Component.literal((String)("Ammo already full! Vehicle Mats: " + currentMats)).withStyle(ChatFormatting.YELLOW), true);
                            }
                        }
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
            if (((Boolean)AASConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()).booleanValue()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (event.getLevel().isClientSide) {
                    player.displayClientMessage((Component)Component.literal((String)"Inventory access is disabled!").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        if (((Boolean)AASConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()).booleanValue() && !player.isCreative() && player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            ResourceLocation vehicleKey = ForgeRegistries.ENTITY_TYPES.getKey((Object)vehicle.getType());
            if (vehicleKey != null && ALLOWED_INVENTORY_ENTITIES.contains(vehicleKey.toString())) {
                return;
            }
            event.setCanceled(true);
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                serverPlayer.server.execute(() -> ((ServerPlayer)serverPlayer).closeContainer());
            } else {
                player.closeContainer();
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (((Boolean)AASConfig.PREVENT_BLOCK_BREAKING.get()).booleanValue()) {
            BlockState state;
            Player player = event.getEntity();
            if (player.isCreative()) {
                return;
            }
            Level level = event.getLevel();
            boolean isStarted = false;
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                isStarted = AASWorldData.get((ServerLevel)serverLevel).isGameStarted;
            } else if (level.isClientSide) {
                isStarted = ClientData.isGameStarted;
            }
            if (isStarted && !InteractionEvents.isBlockWhitelisted(state = level.getBlockState(event.getPos()))) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor levelAccessor;
        if (event.getPlacedBlock().is((Block)ModBlocks.GAME_START_TRIGGER.get()) && (levelAccessor = event.getLevel()) instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor;
            AASWorldData.get((ServerLevel)level).triggerBlocks.add(event.getPos());
            AASWorldData.get(level).setDirty();
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor levelAccessor;
        Player player;
        LevelAccessor levelAccessor2;
        if (event.getState().is((Block)ModBlocks.GAME_START_TRIGGER.get()) && (levelAccessor2 = event.getLevel()) instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor2;
            AASWorldData.get((ServerLevel)level).triggerBlocks.remove(event.getPos());
            AASWorldData.get(level).setDirty();
        }
        if (((Boolean)AASConfig.PREVENT_BLOCK_BREAKING.get()).booleanValue() && !(player = event.getPlayer()).isCreative() && (levelAccessor = event.getLevel()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)levelAccessor;
            boolean isStarted = AASWorldData.get((ServerLevel)serverLevel).isGameStarted;
            if (isStarted && !InteractionEvents.isBlockWhitelisted(event.getState())) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean isBlockWhitelisted(BlockState state) {
        if (state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS) || state.is(BlockTags.LEAVES)) {
            return true;
        }
        if (state.hasProperty((Property)WallBlock.CONSTRUCTED) && !((Boolean)state.getValue((Property)WallBlock.CONSTRUCTED)).booleanValue()) {
            return true;
        }
        if (state.hasProperty((Property)BarbedWireBlock.CONSTRUCTED) && !((Boolean)state.getValue((Property)BarbedWireBlock.CONSTRUCTED)).booleanValue()) {
            return true;
        }
        if (state.hasProperty((Property)HubBlock.CONSTRUCTED) && !((Boolean)state.getValue((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
            return true;
        }
        if (state.is((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()) || state.is((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()) || state.is((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()) || state.is((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get())) {
            return true;
        }
        boolean isDefense = state.is((Block)ModBlocks.WALL_BLOCK.get()) || state.is((Block)ModBlocks.BARBED_WIRE_BLOCK.get());
        boolean allowDefenses = (Boolean)AASConfig.ALLOW_BREAKING_DEFENSES.get();
        return state.is((Block)ModBlocks.HUB_BLOCK.get()) || state.is((Block)ModBlocks.BLUE_RALLY_BLOCK.get()) || state.is((Block)ModBlocks.RED_RALLY_BLOCK.get()) || state.is((Block)ModBlocks.AMMO_BAG_BLOCK.get()) || isDefense && allowDefenses || state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(BlockTags.IMPERMEABLE);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();
        BlockPos clickedPos = event.getPos();
        Direction face = event.getFace();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.is((Block)ModBlocks.MAIN_SUPPLY_BLOCK.get())) {
            if (!level.isClientSide) {
                ServerPlayer sPlayer = (ServerPlayer)player;
                AASWorldData data = AASWorldData.get(sPlayer.serverLevel());
                if (!data.isGameStarted && !sPlayer.isCreative()) {
                    sPlayer.sendSystemMessage((Component)Component.literal((String)"Game hasn't started yet!").withStyle(ChatFormatting.RED));
                    event.setCanceled(true);
                    return;
                }
                long lastMainUse = sPlayer.getPersistentData().getLong("AAS_LastMainResupply");
                long currentTime = sPlayer.level().getGameTime();
                if (!sPlayer.isCreative() && currentTime < lastMainUse + 1200L) {
                    long secondsLeft = (lastMainUse + 1200L - currentTime) / 20L;
                    sPlayer.displayClientMessage((Component)Component.literal((String)("Main Supply Cooldown: " + secondsLeft + "s")).withStyle(ChatFormatting.RED), true);
                    event.setCanceled(true);
                    return;
                }
                if (sPlayer.getPersistentData().contains("AAS_PendingKit")) {
                    ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                    sPlayer.getPersistentData().putLong("AAS_LastMainResupply", currentTime);
                } else {
                    String kitName = sPlayer.getPersistentData().getString("AAS_CurrentKit");
                    if (!kitName.isEmpty() && sPlayer.getTeam() != null) {
                        AASWorldData.KitInfo kit;
                        String t = sPlayer.getTeam().getName().toUpperCase();
                        AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                        if (kit != null) {
                            if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                                sPlayer.sendSystemMessage((Component)Component.literal((String)"Kit Resupplied!").withStyle(ChatFormatting.GREEN));
                                sPlayer.level().playSound(null, sPlayer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                                sPlayer.getPersistentData().putLong("AAS_LastMainResupply", currentTime);
                            } else {
                                sPlayer.sendSystemMessage((Component)Component.literal((String)"Kit is already full!").withStyle(ChatFormatting.YELLOW));
                            }
                        }
                    }
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (face == null) {
            return;
        }
        BlockPos placePos = clickedPos.relative(face);
        if (MortarShellStackBlock.isMortarItem(heldItem)) {
            BlockState newState;
            if (clickedState.getBlock() == ModBlocks.MORTAR_SHELL_STACK_BLOCK.get()) {
                return;
            }
            if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos) && (newState = (BlockState)((Block)ModBlocks.MORTAR_SHELL_STACK_BLOCK.get()).defaultBlockState().setValue((Property)MortarShellStackBlock.FACING, (Comparable)player.getDirection().getOpposite())).canSurvive((LevelReader)level, placePos)) {
                if (!level.isClientSide) {
                    level.setBlock(placePos, newState, 3);
                    level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
                player.swing(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (TOWMissileStackBlock.isTOWItem(heldItem)) {
            BlockState newState;
            if (clickedState.getBlock() == ModBlocks.TOW_MISSILE_STACK_BLOCK.get()) {
                return;
            }
            if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos) && (newState = (BlockState)((Block)ModBlocks.TOW_MISSILE_STACK_BLOCK.get()).defaultBlockState().setValue((Property)TOWMissileStackBlock.FACING, (Comparable)player.getDirection().getOpposite())).canSurvive((LevelReader)level, placePos)) {
                if (!level.isClientSide) {
                    level.setBlock(placePos, newState, 3);
                    level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
                player.swing(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (heldItem.getItem() == ModItems.AGS_AMMO.get()) {
            BlockState newState;
            if (clickedState.getBlock() == ModBlocks.AGS_AMMO_STACK_BLOCK.get()) {
                return;
            }
            if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos) && (newState = (BlockState)((Block)ModBlocks.AGS_AMMO_STACK_BLOCK.get()).defaultBlockState().setValue((Property)AGSAmmoStackBlock.FACING, (Comparable)player.getDirection().getOpposite())).canSurvive((LevelReader)level, placePos)) {
                if (!level.isClientSide) {
                    level.setBlock(placePos, newState, 3);
                    BlockEntity be = level.getBlockEntity(placePos);
                    if (be instanceof AmmoStackBlockEntity) {
                        AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
                        int ammo = AGSAmmoItem.getAmmo(heldItem);
                        ammoBe.addAmmoBox(ammo);
                    }
                    level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
                player.swing(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (heldItem.getItem() == ModItems.M2_AMMO.get()) {
            BlockState newState;
            if (clickedState.getBlock() == ModBlocks.M2_AMMO_STACK_BLOCK.get()) {
                return;
            }
            if (level.getBlockState(placePos).canBeReplaced() && !level.isOutsideBuildHeight(placePos) && (newState = (BlockState)((Block)ModBlocks.M2_AMMO_STACK_BLOCK.get()).defaultBlockState().setValue((Property)M2AmmoStackBlock.FACING, (Comparable)player.getDirection().getOpposite())).canSurvive((LevelReader)level, placePos)) {
                if (!level.isClientSide) {
                    level.setBlock(placePos, newState, 3);
                    BlockEntity be = level.getBlockEntity(placePos);
                    if (be instanceof AmmoStackBlockEntity) {
                        AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
                        int ammo = M2AmmoItem.getAmmo(heldItem);
                        ammoBe.addAmmoBox(ammo);
                    }
                    level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
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

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onGlobalVehicleInteract(PlayerInteractEvent.EntityInteract event) {
        String vTeam;
        Player player = event.getEntity();
        if (!((Boolean)AASConfig.PREVENT_ENEMY_VEHICLE_ENTRY.get()).booleanValue()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        Entity target = event.getTarget();
        if (target.getPersistentData().contains("AAS_VehicleTeam") && !(vTeam = target.getPersistentData().getString("AAS_VehicleTeam")).isEmpty() && !vTeam.equalsIgnoreCase("NEUTRAL")) {
            String pTeam;
            String string = pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            if (!vTeam.equalsIgnoreCase(pTeam)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                if (event.getLevel().isClientSide) {
                    player.displayClientMessage((Component)Component.literal((String)"Access Denied: Enemy Vehicle!").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }
}


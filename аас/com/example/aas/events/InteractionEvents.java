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
    private static final Set<String> ALLOWED_INVENTORY_ENTITIES = Set.of("superbwarfare:mortar", "fpvdrone:drone", "superbwarfare:drone", "wrbdrones:fpv_drone", "wrbdrones:mavic_drone_no_drop", "wrbdrones:mavic_drone_with_drop", "aas:supply_crate", "vvp:mi8_mtv3", "vvp:mi8", "vvp:mi8_amtsh");

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onGlobalEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (player.m_7500_() || player.m_5833_()) {
            return;
        }
        if (player.m_6144_()) {
            Entity target = event.getTarget();
            ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey((Object)target.m_6095_());
            if (entityKey != null && ALLOWED_INVENTORY_ENTITIES.contains(entityKey.toString())) {
                return;
            }
            if (!event.getLevel().f_46443_ && target.getPersistentData().m_128441_("AAS_VehicleTeam")) {
                String pTeam;
                String vType = target.getPersistentData().m_128461_("AAS_VehicleType");
                String vTeam = target.getPersistentData().m_128461_("AAS_VehicleTeam");
                String string = pTeam = player.m_5647_() != null ? player.m_5647_().m_5758_() : "";
                if (!vType.equalsIgnoreCase("Static ZU") && vTeam.equalsIgnoreCase(pTeam)) {
                    String targetKitName;
                    int cost;
                    ServerPlayer sPlayer = (ServerPlayer)player;
                    int currentMats = target.getPersistentData().m_128451_("AAS_VehicleMats");
                    if (currentMats < (cost = ((Integer)AASConfig.HUB_RESUPPLY_COST.get()).intValue())) {
                        sPlayer.m_5661_((Component)Component.m_237113_((String)("Not enough Materials in Vehicle! (" + currentMats + ")")).m_130940_(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        return;
                    }
                    String pendingKit = sPlayer.getPersistentData().m_128461_("AAS_PendingKit");
                    String currentKit = sPlayer.getPersistentData().m_128461_("AAS_CurrentKit");
                    boolean hasPending = !pendingKit.isEmpty();
                    String string2 = targetKitName = hasPending ? pendingKit : currentKit;
                    if (targetKitName == null || targetKitName.isEmpty() || targetKitName.equals("Unassigned")) {
                        sPlayer.m_5661_((Component)Component.m_237113_((String)"No Kit equipped!").m_130940_(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        return;
                    }
                    AASWorldData data = AASWorldData.get(sPlayer.m_284548_());
                    if (hasPending) {
                        ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                        if (!sPlayer.m_7500_()) {
                            target.getPersistentData().m_128405_("AAS_VehicleMats", currentMats - cost);
                        }
                        sPlayer.m_5661_((Component)Component.m_237113_((String)("New Kit Equipped! Vehicle Mats: " + (currentMats - cost))).m_130940_(ChatFormatting.GREEN), true);
                        sPlayer.m_9236_().m_5594_(null, sPlayer.m_20183_(), SoundEvents.f_12019_, SoundSource.PLAYERS, 1.0f, 1.0f);
                    } else {
                        AASWorldData.KitInfo kit;
                        AASWorldData.KitInfo kitInfo = kit = pTeam.equalsIgnoreCase("BLUE") ? data.blueKits.get(currentKit) : data.redKits.get(currentKit);
                        if (kit != null) {
                            if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                                if (!sPlayer.m_7500_()) {
                                    target.getPersistentData().m_128405_("AAS_VehicleMats", currentMats - cost);
                                }
                                sPlayer.m_5661_((Component)Component.m_237113_((String)("Kit Resupplied! Vehicle Mats: " + (currentMats - cost))).m_130940_(ChatFormatting.GREEN), true);
                                sPlayer.m_9236_().m_5594_(null, sPlayer.m_20183_(), SoundEvents.f_12019_, SoundSource.PLAYERS, 1.0f, 1.0f);
                            } else {
                                sPlayer.m_5661_((Component)Component.m_237113_((String)("Ammo already full! Vehicle Mats: " + currentMats)).m_130940_(ChatFormatting.YELLOW), true);
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
                if (event.getLevel().f_46443_) {
                    player.m_5661_((Component)Component.m_237113_((String)"Inventory access is disabled!").m_130940_(ChatFormatting.RED), true);
                }
            }
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        if (((Boolean)AASConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()).booleanValue() && !player.m_7500_() && player.m_20202_() != null) {
            Entity vehicle = player.m_20202_();
            ResourceLocation vehicleKey = ForgeRegistries.ENTITY_TYPES.getKey((Object)vehicle.m_6095_());
            if (vehicleKey != null && ALLOWED_INVENTORY_ENTITIES.contains(vehicleKey.toString())) {
                return;
            }
            event.setCanceled(true);
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                serverPlayer.f_8924_.execute(() -> ((ServerPlayer)serverPlayer).m_6915_());
            } else {
                player.m_6915_();
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (((Boolean)AASConfig.PREVENT_BLOCK_BREAKING.get()).booleanValue()) {
            BlockState state;
            Player player = event.getEntity();
            if (player.m_7500_()) {
                return;
            }
            Level level = event.getLevel();
            boolean isStarted = false;
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                isStarted = AASWorldData.get((ServerLevel)serverLevel).isGameStarted;
            } else if (level.f_46443_) {
                isStarted = ClientData.isGameStarted;
            }
            if (isStarted && !InteractionEvents.isBlockWhitelisted(state = level.m_8055_(event.getPos()))) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor levelAccessor;
        if (event.getPlacedBlock().m_60713_((Block)ModBlocks.GAME_START_TRIGGER.get()) && (levelAccessor = event.getLevel()) instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor;
            AASWorldData.get((ServerLevel)level).triggerBlocks.add(event.getPos());
            AASWorldData.get(level).m_77762_();
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor levelAccessor;
        Player player;
        LevelAccessor levelAccessor2;
        if (event.getState().m_60713_((Block)ModBlocks.GAME_START_TRIGGER.get()) && (levelAccessor2 = event.getLevel()) instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor2;
            AASWorldData.get((ServerLevel)level).triggerBlocks.remove(event.getPos());
            AASWorldData.get(level).m_77762_();
        }
        if (((Boolean)AASConfig.PREVENT_BLOCK_BREAKING.get()).booleanValue() && !(player = event.getPlayer()).m_7500_() && (levelAccessor = event.getLevel()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)levelAccessor;
            boolean isStarted = AASWorldData.get((ServerLevel)serverLevel).isGameStarted;
            if (isStarted && !InteractionEvents.isBlockWhitelisted(event.getState())) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean isBlockWhitelisted(BlockState state) {
        if (state.m_204336_(BlockTags.f_278394_) || state.m_204336_(BlockTags.f_13041_) || state.m_204336_(BlockTags.f_13073_) || state.m_204336_(BlockTags.f_13035_)) {
            return true;
        }
        if (state.m_61138_((Property)WallBlock.CONSTRUCTED) && !((Boolean)state.m_61143_((Property)WallBlock.CONSTRUCTED)).booleanValue()) {
            return true;
        }
        if (state.m_61138_((Property)BarbedWireBlock.CONSTRUCTED) && !((Boolean)state.m_61143_((Property)BarbedWireBlock.CONSTRUCTED)).booleanValue()) {
            return true;
        }
        if (state.m_61138_((Property)HubBlock.CONSTRUCTED) && !((Boolean)state.m_61143_((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
            return true;
        }
        if (state.m_60713_((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()) || state.m_60713_((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()) || state.m_60713_((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()) || state.m_60713_((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get())) {
            return true;
        }
        boolean isDefense = state.m_60713_((Block)ModBlocks.WALL_BLOCK.get()) || state.m_60713_((Block)ModBlocks.BARBED_WIRE_BLOCK.get());
        boolean allowDefenses = (Boolean)AASConfig.ALLOW_BREAKING_DEFENSES.get();
        return state.m_60713_((Block)ModBlocks.HUB_BLOCK.get()) || state.m_60713_((Block)ModBlocks.BLUE_RALLY_BLOCK.get()) || state.m_60713_((Block)ModBlocks.RED_RALLY_BLOCK.get()) || state.m_60713_((Block)ModBlocks.AMMO_BAG_BLOCK.get()) || isDefense && allowDefenses || state.m_204336_(Tags.Blocks.GLASS) || state.m_204336_(Tags.Blocks.GLASS_PANES) || state.m_204336_(BlockTags.f_13049_);
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
        BlockState clickedState = level.m_8055_(clickedPos);
        if (clickedState.m_60713_((Block)ModBlocks.MAIN_SUPPLY_BLOCK.get())) {
            if (!level.f_46443_) {
                ServerPlayer sPlayer = (ServerPlayer)player;
                AASWorldData data = AASWorldData.get(sPlayer.m_284548_());
                if (!data.isGameStarted && !sPlayer.m_7500_()) {
                    sPlayer.m_213846_((Component)Component.m_237113_((String)"Game hasn't started yet!").m_130940_(ChatFormatting.RED));
                    event.setCanceled(true);
                    return;
                }
                long lastMainUse = sPlayer.getPersistentData().m_128454_("AAS_LastMainResupply");
                long currentTime = sPlayer.m_9236_().m_46467_();
                if (!sPlayer.m_7500_() && currentTime < lastMainUse + 1200L) {
                    long secondsLeft = (lastMainUse + 1200L - currentTime) / 20L;
                    sPlayer.m_5661_((Component)Component.m_237113_((String)("Main Supply Cooldown: " + secondsLeft + "s")).m_130940_(ChatFormatting.RED), true);
                    event.setCanceled(true);
                    return;
                }
                if (sPlayer.getPersistentData().m_128441_("AAS_PendingKit")) {
                    ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                    sPlayer.getPersistentData().m_128356_("AAS_LastMainResupply", currentTime);
                } else {
                    String kitName = sPlayer.getPersistentData().m_128461_("AAS_CurrentKit");
                    if (!kitName.isEmpty() && sPlayer.m_5647_() != null) {
                        AASWorldData.KitInfo kit;
                        String t = sPlayer.m_5647_().m_5758_().toUpperCase();
                        AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                        if (kit != null) {
                            if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                                sPlayer.m_213846_((Component)Component.m_237113_((String)"Kit Resupplied!").m_130940_(ChatFormatting.GREEN));
                                sPlayer.m_9236_().m_5594_(null, sPlayer.m_20183_(), SoundEvents.f_12019_, SoundSource.PLAYERS, 1.0f, 1.0f);
                                sPlayer.getPersistentData().m_128356_("AAS_LastMainResupply", currentTime);
                            } else {
                                sPlayer.m_213846_((Component)Component.m_237113_((String)"Kit is already full!").m_130940_(ChatFormatting.YELLOW));
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
        BlockPos placePos = clickedPos.m_121945_(face);
        if (MortarShellStackBlock.isMortarItem(heldItem)) {
            BlockState newState;
            if (clickedState.m_60734_() == ModBlocks.MORTAR_SHELL_STACK_BLOCK.get()) {
                return;
            }
            if (level.m_8055_(placePos).m_247087_() && !level.m_151570_(placePos) && (newState = (BlockState)((Block)ModBlocks.MORTAR_SHELL_STACK_BLOCK.get()).m_49966_().m_61124_((Property)MortarShellStackBlock.FACING, (Comparable)player.m_6350_().m_122424_())).m_60710_((LevelReader)level, placePos)) {
                if (!level.f_46443_) {
                    level.m_7731_(placePos, newState, 3);
                    level.m_5594_(null, placePos, SoundEvents.f_12065_, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!player.m_7500_()) {
                        heldItem.m_41774_(1);
                    }
                }
                player.m_6674_(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (TOWMissileStackBlock.isTOWItem(heldItem)) {
            BlockState newState;
            if (clickedState.m_60734_() == ModBlocks.TOW_MISSILE_STACK_BLOCK.get()) {
                return;
            }
            if (level.m_8055_(placePos).m_247087_() && !level.m_151570_(placePos) && (newState = (BlockState)((Block)ModBlocks.TOW_MISSILE_STACK_BLOCK.get()).m_49966_().m_61124_((Property)TOWMissileStackBlock.FACING, (Comparable)player.m_6350_().m_122424_())).m_60710_((LevelReader)level, placePos)) {
                if (!level.f_46443_) {
                    level.m_7731_(placePos, newState, 3);
                    level.m_5594_(null, placePos, SoundEvents.f_12065_, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!player.m_7500_()) {
                        heldItem.m_41774_(1);
                    }
                }
                player.m_6674_(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (heldItem.m_41720_() == ModItems.AGS_AMMO.get()) {
            BlockState newState;
            if (clickedState.m_60734_() == ModBlocks.AGS_AMMO_STACK_BLOCK.get()) {
                return;
            }
            if (level.m_8055_(placePos).m_247087_() && !level.m_151570_(placePos) && (newState = (BlockState)((Block)ModBlocks.AGS_AMMO_STACK_BLOCK.get()).m_49966_().m_61124_((Property)AGSAmmoStackBlock.FACING, (Comparable)player.m_6350_().m_122424_())).m_60710_((LevelReader)level, placePos)) {
                if (!level.f_46443_) {
                    level.m_7731_(placePos, newState, 3);
                    BlockEntity be = level.m_7702_(placePos);
                    if (be instanceof AmmoStackBlockEntity) {
                        AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
                        int ammo = AGSAmmoItem.getAmmo(heldItem);
                        ammoBe.addAmmoBox(ammo);
                    }
                    level.m_5594_(null, placePos, SoundEvents.f_12065_, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!player.m_7500_()) {
                        heldItem.m_41774_(1);
                    }
                }
                player.m_6674_(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (heldItem.m_41720_() == ModItems.M2_AMMO.get()) {
            BlockState newState;
            if (clickedState.m_60734_() == ModBlocks.M2_AMMO_STACK_BLOCK.get()) {
                return;
            }
            if (level.m_8055_(placePos).m_247087_() && !level.m_151570_(placePos) && (newState = (BlockState)((Block)ModBlocks.M2_AMMO_STACK_BLOCK.get()).m_49966_().m_61124_((Property)M2AmmoStackBlock.FACING, (Comparable)player.m_6350_().m_122424_())).m_60710_((LevelReader)level, placePos)) {
                if (!level.f_46443_) {
                    level.m_7731_(placePos, newState, 3);
                    BlockEntity be = level.m_7702_(placePos);
                    if (be instanceof AmmoStackBlockEntity) {
                        AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
                        int ammo = M2AmmoItem.getAmmo(heldItem);
                        ammoBe.addAmmoBox(ammo);
                    }
                    level.m_5594_(null, placePos, SoundEvents.f_12065_, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!player.m_7500_()) {
                        heldItem.m_41774_(1);
                    }
                }
                player.m_6674_(event.getHand());
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
        if (player.m_7500_() || player.m_5833_()) {
            return;
        }
        Entity target = event.getTarget();
        if (target.getPersistentData().m_128441_("AAS_VehicleTeam") && !(vTeam = target.getPersistentData().m_128461_("AAS_VehicleTeam")).isEmpty() && !vTeam.equalsIgnoreCase("NEUTRAL")) {
            String pTeam;
            String string = pTeam = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
            if (!vTeam.equalsIgnoreCase(pTeam)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                if (event.getLevel().f_46443_) {
                    player.m_5661_((Component)Component.m_237113_((String)"Access Denied: Enemy Vehicle!").m_130940_(ChatFormatting.RED), true);
                }
            }
        }
    }
}


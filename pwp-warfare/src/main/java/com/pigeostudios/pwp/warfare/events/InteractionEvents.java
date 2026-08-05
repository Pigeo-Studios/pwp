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
import com.pigeostudios.pwp.warfare.item.AGSAmmoItem;
import com.pigeostudios.pwp.warfare.item.M2AmmoItem;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
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
// РћР±СЂР°Р±РѕС‚С‡РёРє РІР·Р°РёРјРѕРґРµР№СЃС‚РІРёР№ РёРіСЂРѕРєР° СЃ РјРёСЂРѕРј
// РЈРїСЂР°РІР»СЏРµС‚ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРµРј Р±Р»РѕРєРѕРІ, СѓСЃС‚Р°РЅРѕРІРєРѕР№ РєРѕРЅСЃС‚СЂСѓРєС†РёР№ Рё РІР·Р°РёРјРѕРґРµР№СЃС‚РІРёРµРј СЃ С‚РµС…РЅРёРєРѕР№
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
      "vvp:mi8_amtsh"
   );

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

               if (!event.getLevel().isClientSide && target.getPersistentData().contains("WARFARE_VehicleTeam")) {
                  String vType = target.getPersistentData().getString("WARFARE_VehicleType");
                  String vTeam = target.getPersistentData().getString("WARFARE_VehicleTeam");
                  String pTeam = player.getTeam() != null ? player.getTeam().getName() : "";
                  if (!vType.equalsIgnoreCase("Static ZU") && vTeam.equalsIgnoreCase(pTeam)) {
                     ServerPlayer sPlayer = (ServerPlayer)player;
                     int currentMats = target.getPersistentData().getInt("WARFARE_VehicleMats");
                     int cost = (Integer)WarfareConfig.HUB_RESUPPLY_COST.get();
                     if (currentMats < cost) {
                        sPlayer.displayClientMessage(Component.literal("Недостаточно материалов в технике! (" + currentMats + ")").withStyle(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        return;
                     }

                     String pendingKit = sPlayer.getPersistentData().getString("WARFARE_PendingKit");
                     String currentKit = sPlayer.getPersistentData().getString("WARFARE_CurrentKit");
                     boolean hasPending = !pendingKit.isEmpty();
                     String targetKitName = hasPending ? pendingKit : currentKit;
                     if (targetKitName != null && !targetKitName.isEmpty() && !targetKitName.equals("Unassigned")) {
                        WarfareWorldData data = WarfareWorldData.get(sPlayer.serverLevel());
                        if (hasPending) {
                           ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                           if (!sPlayer.isCreative()) {
                              target.getPersistentData().putInt("WARFARE_VehicleMats", currentMats - cost);
                           }

                           sPlayer.displayClientMessage(Component.literal("Новый кит надет! Материалы техники: " + (currentMats - cost)).withStyle(ChatFormatting.GREEN), true);
                           sPlayer.level().playSound(null, sPlayer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                        } else {
                           WarfareWorldData.KitInfo kit = pTeam.equalsIgnoreCase("BLUE") ? data.blueKits.get(currentKit) : data.redKits.get(currentKit);
                           if (kit != null) {
                              if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                                 if (!sPlayer.isCreative()) {
                                    target.getPersistentData().putInt("WARFARE_VehicleMats", currentMats - cost);
                                 }

                                 sPlayer.displayClientMessage(
                                    Component.literal("Кит пополнен! Материалы техники: " + (currentMats - cost)).withStyle(ChatFormatting.GREEN), true
                                 );
                                 sPlayer.level().playSound(null, sPlayer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                              } else {
                                 sPlayer.displayClientMessage(Component.literal("БК уже полон! Материалы техники: " + currentMats).withStyle(ChatFormatting.YELLOW), true);
                              }
                           }
                        }

                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        return;
                     }

                     sPlayer.displayClientMessage(Component.literal("Кит не надет!").withStyle(ChatFormatting.RED), true);
                     event.setCanceled(true);
                     event.setCancellationResult(InteractionResult.SUCCESS);
                     return;
                  }
               }

               if ((Boolean)WarfareConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()) {
                  event.setCanceled(true);
                  event.setCancellationResult(InteractionResult.SUCCESS);
                  if (event.getLevel().isClientSide) {
                     player.displayClientMessage(Component.literal("Доступ к инвентарю отключён!").withStyle(ChatFormatting.RED), true);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onContainerOpen(Open event) {
      Player player = event.getEntity();
      if ((Boolean)WarfareConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get() && !player.isCreative() && player.getVehicle() != null) {
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
      if ((Boolean)WarfareConfig.PREVENT_BLOCK_BREAKING.get()) {
         Player player = event.getEntity();
         if (player.isCreative()) {
            return;
         }

         Level level = event.getLevel();
         boolean isStarted = false;
          if (level instanceof ServerLevel serverLevel) {
             WarfareWorldData data = WarfareWorldData.get(serverLevel);
             isStarted = data.isGameStarted || data.waitingActive;
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
      if (event.getPlacedBlock().is((Block)ModBlocks.GAME_START_TRIGGER.get()) && event.getLevel() instanceof ServerLevel level) {
         WarfareWorldData.get(level).triggerBlocks.add(event.getPos());
         WarfareWorldData.get(level).setDirty();
      }
   }

   @SubscribeEvent
   public static void onBlockBreak(BreakEvent event) {
      if (event.getState().is((Block)ModBlocks.GAME_START_TRIGGER.get()) && event.getLevel() instanceof ServerLevel level) {
         WarfareWorldData.get(level).triggerBlocks.remove(event.getPos());
         WarfareWorldData.get(level).setDirty();
      }

      if ((Boolean)WarfareConfig.PREVENT_BLOCK_BREAKING.get()) {
         Player player = event.getPlayer();
         if (!player.isCreative() && event.getLevel() instanceof ServerLevel serverLevel) {
             WarfareWorldData data = WarfareWorldData.get(serverLevel);
             boolean isStarted = data.isGameStarted || data.waitingActive;
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
      } else if (state.hasProperty(WallBlock.CONSTRUCTED) && !(Boolean)state.getValue(WallBlock.CONSTRUCTED)) {
         return true;
      } else if (state.hasProperty(BarbedWireBlock.CONSTRUCTED) && !(Boolean)state.getValue(BarbedWireBlock.CONSTRUCTED)) {
         return true;
      } else if (state.hasProperty(HubBlock.CONSTRUCTED) && !(Boolean)state.getValue(HubBlock.CONSTRUCTED)) {
         return true;
      } else if (!state.is((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get())
         && !state.is((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get())
         && !state.is((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get())
         && !state.is((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get())
         && !state.is((Block)ModBlocks.REB_CONSTRUCTION_BLOCK.get())) {
          boolean isDefense = state.is((Block)ModBlocks.WALL_BLOCK.get()) || state.is((Block)ModBlocks.BARBED_WIRE_BLOCK.get());
          boolean allowDefenses = (Boolean)WarfareConfig.ALLOW_BREAKING_DEFENSES.get();
          return state.is((Block)ModBlocks.BLUE_RALLY_BLOCK.get())
            || state.is((Block)ModBlocks.RED_RALLY_BLOCK.get())
            || state.is((Block)ModBlocks.AMMO_BAG_BLOCK.get())
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
          if (clickedState.is((Block)ModBlocks.MAIN_SUPPLY_BLOCK.get())) {
             if (!level.isClientSide) {
                ServerPlayer sPlayer = (ServerPlayer)player;
                WarfareWorldData data = WarfareWorldData.get(sPlayer.serverLevel());
                if (!data.isGameStarted && !sPlayer.isCreative()) {
                   sPlayer.sendSystemMessage(Component.literal("Игра ещё не началась!").withStyle(ChatFormatting.RED));
                   event.setCanceled(true);
                   return;
                }

                if (data.invasionSetupActive && !sPlayer.isCreative()) {
                   String pTeam = sPlayer.getTeam() != null ? sPlayer.getTeam().getName().toUpperCase() : "";
                   if (!pTeam.equalsIgnoreCase(data.invasionDefender)) {
                       sPlayer.sendSystemMessage(Component.literal("Основное снабжение недоступно во время подготовки!").withStyle(ChatFormatting.RED));
                      event.setCanceled(true);
                      return;
                   }
                }

                long lastMainUse = sPlayer.getPersistentData().getLong("WARFARE_LastMainResupply");
               long currentTime = sPlayer.level().getGameTime();
               if (!sPlayer.isCreative() && currentTime < lastMainUse + 1200L) {
                  long secondsLeft = (lastMainUse + 1200L - currentTime) / 20L;
                   sPlayer.displayClientMessage(Component.literal("Кулдаун основной базы: " + secondsLeft + "с").withStyle(ChatFormatting.RED), true);
                  event.setCanceled(true);
                  return;
               }

               if (sPlayer.getPersistentData().contains("WARFARE_PendingKit")) {
                  ResupplyHandler.tryApplyPendingKit(sPlayer, data);
                  sPlayer.getPersistentData().putLong("WARFARE_LastMainResupply", currentTime);
               } else {
                  String kitName = sPlayer.getPersistentData().getString("WARFARE_CurrentKit");
                  if (!kitName.isEmpty() && sPlayer.getTeam() != null) {
                     String t = sPlayer.getTeam().getName().toUpperCase();
                     WarfareWorldData.KitInfo kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                     if (kit != null) {
                        if (ResupplyHandler.resupplyPlayer(sPlayer, kit, false)) {
                           sPlayer.sendSystemMessage(Component.literal("Кит пополнен!").withStyle(ChatFormatting.GREEN));
                           sPlayer.level().playSound(null, sPlayer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                           sPlayer.getPersistentData().putLong("WARFARE_LastMainResupply", currentTime);
                        } else {
                           sPlayer.sendSystemMessage(Component.literal("Кит уже полон!").withStyle(ChatFormatting.YELLOW));
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
                  BlockState newState = (BlockState)((Block)ModBlocks.MORTAR_SHELL_STACK_BLOCK.get())
                     .defaultBlockState()
                     .setValue(MortarShellStackBlock.FACING, player.getDirection().getOpposite());
                  if (newState.canSurvive(level, placePos)) {
                     if (!level.isClientSide) {
                        level.setBlock(placePos, newState, 3);
                        level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
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
                  BlockState newState = (BlockState)((Block)ModBlocks.TOW_MISSILE_STACK_BLOCK.get())
                     .defaultBlockState()
                     .setValue(TOWMissileStackBlock.FACING, player.getDirection().getOpposite());
                  if (newState.canSurvive(level, placePos)) {
                     if (!level.isClientSide) {
                        level.setBlock(placePos, newState, 3);
                        level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
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
                  BlockState newState = (BlockState)((Block)ModBlocks.AGS_AMMO_STACK_BLOCK.get())
                     .defaultBlockState()
                     .setValue(AGSAmmoStackBlock.FACING, player.getDirection().getOpposite());
                  if (newState.canSurvive(level, placePos)) {
                     if (!level.isClientSide) {
                        level.setBlock(placePos, newState, 3);
                        if (level.getBlockEntity(placePos) instanceof AmmoStackBlockEntity ammoBe) {
                           int ammo = AGSAmmoItem.getAmmo(heldItem);
                           ammoBe.addAmmoBox(ammo);
                        }

                        level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
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
                  BlockState newState = (BlockState)((Block)ModBlocks.M2_AMMO_STACK_BLOCK.get())
                     .defaultBlockState()
                     .setValue(M2AmmoStackBlock.FACING, player.getDirection().getOpposite());
                  if (newState.canSurvive(level, placePos)) {
                     if (!level.isClientSide) {
                        level.setBlock(placePos, newState, 3);
                        if (level.getBlockEntity(placePos) instanceof AmmoStackBlockEntity ammoBe) {
                           int ammo = M2AmmoItem.getAmmo(heldItem);
                           ammoBe.addAmmoBox(ammo);
                        }

                        level.playSound(null, placePos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
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
      if ((Boolean)WarfareConfig.PREVENT_ENEMY_VEHICLE_ENTRY.get()) {
         if (!player.isCreative() && !player.isSpectator()) {
            Entity target = event.getTarget();
            if (target.getPersistentData().contains("WARFARE_VehicleTeam")) {
               String vTeam = target.getPersistentData().getString("WARFARE_VehicleTeam");
               if (!vTeam.isEmpty() && !vTeam.equalsIgnoreCase("NEUTRAL")) {
                  String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                  if (!vTeam.equalsIgnoreCase(pTeam)) {
                     event.setCanceled(true);
                     event.setCancellationResult(InteractionResult.FAIL);
                      player.displayClientMessage(Component.literal("Доступ запрещён: техника врага!").withStyle(ChatFormatting.RED), true);
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
      if (!vehicle.getPersistentData().contains("WARFARE_VehicleTeam")) return;

      String vType = vehicle.getPersistentData().getString("WARFARE_VehicleType");
      if (!isSpecialistVehicle(vType)) return;

      if (!vehicle.getPassengers().isEmpty()) return;

      String pKit = KitUtil.getEffectiveKit(player);
      if (!hasCorrectKit(vType, pKit)) {
         event.setCanceled(true);
         player.displayClientMessage(Component.literal("Вам нужен кит Механик/Пилот для этой техники").withStyle(ChatFormatting.RED), true);
         return;
      }

      int squadId = player.getPersistentData().getInt("WARFARE_SquadID");
      if (squadId == 0) {
         event.setCanceled(true);
         player.displayClientMessage(Component.literal("Вы не в отряде").withStyle(ChatFormatting.RED), true);
         return;
      }

      boolean isSL = player.getPersistentData().getBoolean("WARFARE_IsSquadLeader");
      boolean isFresh = vehicle.getPersistentData().getBoolean("WARFARE_FreshVehicle");
      if (!isFresh) return;

      UUID vehicleUUID = vehicle.getUUID();
      WarfareWorldData data = WarfareWorldData.get(player.serverLevel());

      if (isSL || data.hasApprovedDriver(vehicleUUID, player.getUUID())) {
         vehicle.getPersistentData().remove("WARFARE_FreshVehicle");
         return;
      }

      long cooldownUntil = player.getPersistentData().getLong("WARFARE_DriveRequestCooldown_" + vehicle.getId());
      if (cooldownUntil > player.level().getGameTime()) {
         long remaining = (cooldownUntil - player.level().getGameTime()) / 20L;
         event.setCanceled(true);
         player.displayClientMessage(Component.literal("Запрос отклонён. Повтор через " + remaining + "с").withStyle(ChatFormatting.RED), true);
         return;
      }

      event.setCanceled(true);
      Component requestMsg = Component.literal("Нужно разрешение командира отряда. ")
         .withStyle(ChatFormatting.YELLOW)
         .append(Component.literal("[ОТПРАВИТЬ ЗАПРОС]")
            .withStyle(style -> style.withColor(ChatFormatting.GREEN)
               .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vrequest " + vehicle.getId()))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Запросить разрешение у SL")))));
      player.sendSystemMessage(requestMsg);
   }

   private static boolean isSpecialistVehicle(String vType) {
      if (vType.equalsIgnoreCase("TANK")) return true;
      if (vType.equalsIgnoreCase("APC")) return true;
      if (vType.equalsIgnoreCase("Mobile ZU")) return true;
      if (vType.equalsIgnoreCase("HELICOPTER")) return true;
      if (vType.toUpperCase().contains("CAS")) return true;
      if (vType.toUpperCase().contains("SUPPLY HELICOPTER")) return true;
      return false;
   }

   private static boolean hasCorrectKit(String vType, String kit) {
      if (vType.equalsIgnoreCase("HELICOPTER") || vType.toUpperCase().contains("CAS") || vType.toUpperCase().contains("SUPPLY HELICOPTER")) {
         return kit.equals("Pilot") || kit.equals("Pilot Officer");
      }
      if (vType.equalsIgnoreCase("TANK") || vType.equalsIgnoreCase("APC") || vType.equalsIgnoreCase("Mobile ZU")) {
         return kit.equals("Mechanic") || kit.equals("Mechanic Officer");
      }
      return true;
   }
}

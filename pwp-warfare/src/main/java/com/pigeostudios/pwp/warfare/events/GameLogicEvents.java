package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.block.GameStartTriggerBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.RallyPointBlock;
import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.network.PacketCaptureNotification;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenVictoryScreen;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSyncDownedState;
import com.pigeostudios.pwp.warfare.network.PacketSyncGameData;
import com.pigeostudios.pwp.warfare.network.PacketSyncMapPlayers;
import com.pigeostudios.pwp.warfare.network.PacketSyncPoint;
import com.pigeostudios.pwp.warfare.network.PacketSyncSquads;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.PermissionHelper;
import com.pwp.coreclient.network.ConnectToServerPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
// РћСЃРЅРѕРІРЅРѕР№ РѕР±СЂР°Р±РѕС‚С‡РёРє РёРіСЂРѕРІРѕР№ Р»РѕРіРёРєРё
// РЈРїСЂР°РІР»СЏРµС‚ Р·Р°С…РІР°С‚РѕРј С‚РѕС‡РµРє, С‚РёРєРµС‚Р°РјРё, РІРѕР·СЂРѕР¶РґРµРЅРёРµРј, Р·Р°С‰РёС‚РЅС‹РјРё Р·РѕРЅР°РјРё Рё Р°СЂС‚РёР»Р»РµСЂРёРµР№
public class GameLogicEvents {
   public static final Map<UUID, String> pendingRespawnLocations = new HashMap<>();
   public static final Map<UUID, String> pendingTeams = new HashMap<>();
   public static final Map<UUID, Map<Integer, CompoundTag>> PERSISTENT_NBT_STORAGE = new HashMap<>();
    private static final Map<String, Boolean> lastBlueBlockedMap = new HashMap<>();
    private static final Map<String, Boolean> lastRedBlockedMap = new HashMap<>();
    private static int returnToLobbyTimer = -1;
    private static MinecraftServer returnToLobbyServer = null;

   // Р—Р°РїСѓСЃРє РѕР±СЂР°С‚РЅРѕРіРѕ РѕС‚СЃС‡С‘С‚Р° РїРµСЂРµРґ РЅР°С‡Р°Р»РѕРј РёРіСЂС‹
   public static void startGameCountdown(ServerLevel level) {
      WarfareWorldData data = WarfareWorldData.get(level);
      data.countdownTicks = 100;
      data.countdownActive = true;
      data.setDirty();
   }

   public static void cancelCountdown(ServerLevel level) {
      WarfareWorldData data = WarfareWorldData.get(level);
      data.countdownActive = false;
      data.countdownTicks = 0;
      data.setDirty();
   }

   public static void cancelCountdown() {
   }

   @SubscribeEvent
   public static void onItemToss(ItemTossEvent event) {
      if (event.getPlayer() != null && !event.getPlayer().level().isClientSide) {
         ServerPlayer player = (ServerPlayer)event.getPlayer();
         if (!player.isCreative()) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            if (data.isGameStarted) {
               boolean preventAll = false;

               try {
                  preventAll = (Boolean)WarfareConfig.PREVENT_ALL_ITEM_DROPS.get();
               } catch (Exception var5) {
               }

               if (preventAll) {
                  event.setCanceled(true);
                  player.getInventory().add(event.getEntity().getItem());
                  player.displayClientMessage(Component.literal("Item dropping is DISABLED during the game!").withStyle(ChatFormatting.RED), true);
               } else if (isHeavyItem(event.getEntity().getItem().getItem())) {
                  event.setCanceled(true);
                  player.getInventory().add(event.getEntity().getItem());
                  player.displayClientMessage(Component.literal("Cannot drop heavy ammo during combat!").withStyle(ChatFormatting.RED), true);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTickEffects(PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide) {
         if (event.player.tickCount % 10 == 0) {
            ServerPlayer player = (ServerPlayer)event.player;
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            if (data.isGameStarted && !player.isCreative()) {
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
                  player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, true));
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTickDriveCheck(PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide) {
         if ((Boolean)WarfareConfig.REQUIRE_SPECIALIST_TO_DRIVE.get()) {
            ServerPlayer player = (ServerPlayer)event.player;
            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
               player.getPersistentData().remove("WARFARE_DriveKickTimer");
               player.getPersistentData().remove("WARFARE_BoardingGrace");
               player.getPersistentData().remove("WARFARE_LastVehicleId");
            } else {
               int lastVehicleId = player.getPersistentData().getInt("WARFARE_LastVehicleId");
               if (lastVehicleId != vehicle.getId()) {
                  player.getPersistentData().putInt("WARFARE_LastVehicleId", vehicle.getId());
                  player.getPersistentData().putInt("WARFARE_BoardingGrace", 0);
                  player.getPersistentData().remove("WARFARE_DriveKickTimer");
               } else {
                   int boardingGrace = player.getPersistentData().getInt("WARFARE_BoardingGrace");
                   if (boardingGrace < 20) {
                      player.getPersistentData().putInt("WARFARE_BoardingGrace", boardingGrace + 1);
                   } else {
                        if (vehicle.getFirstPassenger() != player) {
                         player.getPersistentData().remove("WARFARE_DriveKickTimer");
                       } else if (!player.isCreative() && !player.isSpectator()) {
                           String vType = vehicle.getPersistentData().getString("WARFARE_VehicleType");
                           if (vType.isEmpty() || vType.equals("DEFAULT")) {
                              player.getPersistentData().remove("WARFARE_DriveKickTimer");
                              return;
                           }
                           String vTypeUpper = vType.toUpperCase();
                           String pKit = player.getPersistentData().getString("WARFARE_CurrentKit");
                           boolean isAuthorized = true;
                           String requiredSpecialist = "";
                           if (!vType.equalsIgnoreCase("HELICOPTER") && !vTypeUpper.contains("CAS") && !vTypeUpper.contains("SUPPLY HELICOPTER")) {
                              if ((vType.equalsIgnoreCase("TANK") || vType.equalsIgnoreCase("APC") || vType.equalsIgnoreCase("Mobile ZU"))
                                 && !pKit.equals("Mechanic")
                                 && !pKit.equals("Mechanic Officer")) {
                                 isAuthorized = false;
                                 requiredSpecialist = "MECHANIC";
                              }
                           } else if (!pKit.equals("Pilot") && !pKit.equals("Pilot Officer")) {
                              isAuthorized = false;
                              requiredSpecialist = "PILOT";
                           }

                        if (!isAuthorized) {
                           int timer = player.getPersistentData().getInt("WARFARE_DriveKickTimer");
                           if (++timer >= 100) {
                              player.stopRiding();
                              player.getPersistentData().remove("WARFARE_DriveKickTimer");
                              player.getPersistentData().remove("WARFARE_BoardingGrace");
                              player.getPersistentData().remove("WARFARE_LastVehicleId");
                               player.displayClientMessage(Component.translatable("pwpwarfare.message.ejected_not_qualified").withStyle(ChatFormatting.RED), true);
                           } else {
                              player.getPersistentData().putInt("WARFARE_DriveKickTimer", timer);
                               if (timer % 20 == 0) {
                                  player.displayClientMessage(
                                     Component.translatable("pwpwarfare.message.unauthorized_driver", requiredSpecialist, 5 - timer / 20)
                                        .withStyle(ChatFormatting.YELLOW),
                                     true
                                  );
                                 player.playNotifySound((SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
                              }
                           }
                        } else {
                           player.getPersistentData().remove("WARFARE_DriveKickTimer");
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerItemLogic(PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide) {
         if (event.player.level().getGameTime() % 20L == 0L) {
            Player player = event.player;
            ItemStack mainHandStack = player.getMainHandItem();
            ItemStack offHandStack = player.getOffhandItem();
            Item monitorItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "monitor"));
            Item walkieItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("walkietalkie", "netherite_walkietalkie"));
            if (monitorItem != null && walkieItem != null) {
               boolean hasMonitor = mainHandStack.getItem() == monitorItem;
               if (hasMonitor) {
                  if (offHandStack.getItem() != walkieItem && !offHandStack.isEmpty()) {
                     moveItemToInventory(player, offHandStack);
                     player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                  }

                  if (player.getOffhandItem().isEmpty()) {
                     int walkieSlot = findItemInInventory(player.getInventory(), walkieItem);
                     if (walkieSlot != -1) {
                        ItemStack walkieStack = player.getInventory().getItem(walkieSlot);
                        player.setItemInHand(InteractionHand.OFF_HAND, walkieStack);
                        player.getInventory().setItem(walkieSlot, ItemStack.EMPTY);
                     }
                  }
               } else if (!offHandStack.isEmpty() && offHandStack.getItem() == walkieItem) {
                  moveItemToInventory(player, offHandStack);
                  player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
               }
            }
         }
      }
   }

   private static boolean isHeavyItem(Item item) {
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

   private static int findItemInInventory(Inventory inventory, Item item) {
      for (int i = 0; i < inventory.items.size(); i++) {
         if (((ItemStack)inventory.items.get(i)).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private static void moveItemToInventory(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null) {
            int globalTick = server.getTickCount();

            for (ServerLevel level : server.getAllLevels()) {
               WarfareWorldData data = WarfareWorldData.get(level);
               handleMainProtectionZones(level, data);
               boolean blueIsBleeding = false;
               boolean redIsBleeding = false;
               if (data.blueCmdVoteActive) {
                  data.blueCmdVoteTimer--;
                  checkCmdVoteStatus(level, server, data, "BLUE");
               }

               if (data.redCmdVoteActive) {
                  data.redCmdVoteTimer--;
                  checkCmdVoteStatus(level, server, data, "RED");
               }

               if (data.voteActive && !data.isGameStarted) {
                  if (globalTick % 20 == 0 && data.voteTimer > 0) {
                     data.voteTimer--;
                     data.setDirty();
                     boolean bReady = isTeamReady(level, "Blue", data);
                     boolean rReady = isTeamReady(level, "Red", data);
                     if (bReady != data.blueReady || rReady != data.redReady || data.voteTimer % 5 == 0 || data.voteTimer < 5) {
                        data.blueReady = bReady;
                        data.redReady = rReady;
                        PacketHandler.sendToAllClients(level, data);
                     }
                  }

                  if (data.blueReady && data.redReady || data.voteTimer <= 0) {
                     data.voteActive = false;
                     data.setDirty();
                     startGameCountdown(level);
                     PacketHandler.sendToAllClients(level, data);
                  }
               }

               if (globalTick % 2 == 0) {
                  List<MapPlayerInfo> allPlayersInfo = buildPlayerInfo(level.players(), data);
                  PacketSyncMapPlayers allPlayersPacket = new PacketSyncMapPlayers(allPlayersInfo);

                  for (ServerPlayer p : level.players()) {
                     if (p.isSpectator() || p.isCreative()) {
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), allPlayersPacket);
                     } else if (p.getTeam() != null) {
                        String myTeamName = p.getTeam().getName();
                        List<MapPlayerInfo> teamOnly = allPlayersInfo.stream().filter(info -> info.team.equalsIgnoreCase(myTeamName)).toList();
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), new PacketSyncMapPlayers(teamOnly));
                     }
                  }

                  if (globalTick % 5 == 0) {
                     boolean needsSync = false;

                     for (WarfareWorldData.VehicleRecord record : data.markedVehicles) {
                        Entity vEntity = level.getEntity(record.uuid);
                        if (vEntity != null && vEntity.isAlive()) {
                           float currentYaw = vEntity.getYRot();
                           if (record.x != vEntity.getX() || record.yaw != currentYaw) {
                              record.x = vEntity.getX();
                              record.y = vEntity.getY();
                              record.z = vEntity.getZ();
                              record.yaw = currentYaw;
                              needsSync = true;
                           }
                        }
                     }

                     if (needsSync) {
                        data.setDirty();
                        PacketHandler.sendToAllClients(level, data);
                     }
                  }

                  if (globalTick % 40 == 0) {
                     boolean changed = false;
                     Iterator<WarfareWorldData.VehicleRecord> it = data.markedVehicles.iterator();

                     while (it.hasNext()) {
                        WarfareWorldData.VehicleRecord record = it.next();
                        Entity vEntity = level.getEntity(record.uuid);
                        if (vEntity != null && !vEntity.isAlive()) {
                           it.remove();
                           changed = true;
                        }
                     }

                      if (changed) {
                         data.setDirty();
                         PacketHandler.sendToAllClients(level, data);
                      }
                   }

                    if (globalTick % 40 == 0) {
                       List<Entity> wrecksToProcess = new ArrayList<>();
                       for (WarfareWorldData.VehicleRecord record : data.markedVehicles) {
                          Entity vehicle = level.getEntity(record.uuid);
                          if (vehicle != null && vehicle.isAlive()
                             && !(vehicle instanceof LivingEntity)
                             && vehicle.getPersistentData().contains("WARFARE_TicketPenalty")) {
                             float health = getVehicleHealth(vehicle);
                             if (health <= 0.0F) {
                                wrecksToProcess.add(vehicle);
                             }
                          }
                       }
                       for (Entity wreck : wrecksToProcess) {
                          processWreckLoss(wreck, data, level);
                       }
                    }
                }

               if (globalTick % 200 == 0) {
                  long time = level.getGameTime();
                  boolean removed = data.activeMarkers.removeIf(mx -> time >= mx.expiryTick);
                  if (removed) {
                     data.setDirty();
                     PacketHandler.sendToAllClients(level, data);
                  }
               }

               if (globalTick % 20 == 0) {
                  int rallyRadius = (Integer)WarfareConfig.RALLY_BLOCK_RADIUS.get();
                  int rallyEnemiesRequired = (Integer)WarfareConfig.RALLY_BLOCK_ENEMY_COUNT.get();

                  for (WarfareWorldData.Squad squad : data.squads) {
                     if (squad.rallyPos != null) {
                        int enemies = getEnemyCount(level, squad.rallyPos, squad.team, rallyRadius);
                        boolean currentlyBlocked = enemies >= rallyEnemiesRequired;
                        if (squad.isRallyBlocked != currentlyBlocked) {
                           squad.isRallyBlocked = currentlyBlocked;
                           data.setDirty();
                           PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), new PacketSyncSquads(data.squads));
                        }
                     }
                  }
               }

               if (globalTick % 400 == 0 && (Boolean)WarfareConfig.REQUIRE_OFFICER_FOR_SL.get()) {
                  for (WarfareWorldData.Squad squad : new ArrayList<>(data.squads)) {
                     ServerPlayer leader = server.getPlayerList().getPlayerByName(squad.leader);
                     if (leader != null) {
                        if (leader.isCreative()) {
                           squad.slNoOfficerSince = -1L;
                        } else {
                           String current = leader.getPersistentData().getString("WARFARE_CurrentKit");
                           String pending = leader.getPersistentData().getString("WARFARE_PendingKit");
                           String kitName = !pending.isEmpty() ? pending : current;
                           boolean hasCommandKit = false;
                           if (!kitName.isEmpty() && !kitName.equals("Unassigned")) {
                              String team = squad.team.toUpperCase();
                              WarfareWorldData.KitInfo kitInfo = team.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                              if (kitInfo != null && kitInfo.isLeaderOnly) {
                                 hasCommandKit = true;
                              }
                           }

                           if (hasCommandKit) {
                              squad.slNoOfficerSince = -1L;
                           } else {
                              if (squad.slNoOfficerSince == -1L) {
                                 squad.slNoOfficerSince = level.getGameTime();
                              }

                              long remaining = 2400L - (level.getGameTime() - squad.slNoOfficerSince);
                              if (remaining > 0L) {
                                 leader.displayClientMessage(
                                    Component.literal("В§6WARNING: В§eSelect a В§nLEADER ONLYВ§e kit or squad disbands in В§c" + remaining / 20L + "s!"), true
                                 );
                              } else {
                                 broadcastMessage(level, "Squad " + squad.name + " disbanded: Leader is not using a Leader kit!", ChatFormatting.RED);

                                 for (String memberName : new ArrayList<>(squad.members)) {
                                    ServerPlayer m = server.getPlayerList().getPlayerByName(memberName);
                                    if (m != null) {
                                       m.getPersistentData().putString("WARFARE_CurrentKit", "Unassigned");
                                       m.getPersistentData().remove("WARFARE_PendingKit");
                                       m.getPersistentData().remove("WARFARE_SquadID");
                                       m.getPersistentData().remove("WARFARE_IsSquadLeader");
                                       PacketSquadAction.removeRadio(m);
                                       m.getInventory().clearContent();
                                       ResupplyHandler.clearCurios(m);
                                       m.inventoryMenu.broadcastChanges();
                                       m.containerMenu.broadcastChanges();
                                       m.displayClientMessage(Component.literal("В§cYour squad was disbanded (No Officer kit)!"), true);
                                    }
                                 }

                                 data.squads.remove(squad);
                                 data.setDirty();
                                 PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), new PacketSyncSquads(data.squads));
                              }
                           }
                        }
                     }
                  }
               }

               long now = level.getGameTime();
               boolean squadsChanged = false;

               for (WarfareWorldData.Squad squad : data.squads) {
                  if (squad.rhombusMarkers.removeIf(rm -> now >= rm.expiryTick)) {
                     squadsChanged = true;
                  }

                  if (squad.rallyPos != null && squad.rallyExpiryTick != -1L && now >= squad.rallyExpiryTick) {
                     if (level.isLoaded(squad.rallyPos)) {
                        if (level.getBlockEntity(squad.rallyPos) instanceof RallyPointBlockEntity rbe) {
                           rbe.isDecay = true;
                        }

                        level.removeBlock(squad.rallyPos, false);
                     } else {
                        data.blueRallies.remove(squad.rallyPos);
                        data.redRallies.remove(squad.rallyPos);
                        squad.rallyPos = null;
                        squad.rallyExpiryTick = -1L;
                        squadsChanged = true;
                     }
                  }
               }

               if (squadsChanged) {
                  data.setDirty();
                  PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncSquads(data.squads));
               }

               if (globalTick % 200 == 0) {
                  long currentTick = level.getGameTime();
                  boolean anyMarkerRemoved = false;

                  for (WarfareWorldData.Squad squad : data.squads) {
                     if (squad.marker != null && currentTick >= squad.marker.expiryTick) {
                        squad.marker = null;
                        anyMarkerRemoved = true;
                     }
                  }

                  if (anyMarkerRemoved) {
                     data.setDirty();
                     PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), new PacketSyncSquads(data.squads));
                  }
               }

               if (data.countdownActive) {
                  if (data.countdownTicks > 0) {
                     if (data.countdownTicks % 20 == 0) {
                        int seconds = data.countdownTicks / 20;
                        sendTitleToLevel(level, String.valueOf(seconds), ChatFormatting.YELLOW);
                        level.playSound(null, new BlockPos(0, 100, 0), (SoundEvent)SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0F, 1.0F);
                     }

                     data.countdownTicks--;
                     data.setDirty();
                  } else {
                      data.countdownActive = false;
                      sendTitleToLevel(level, "GO!", ChatFormatting.GREEN);
                      data.isGameStarted = true;
                      data.setDirty();
                      sendSyncPacket(level, data);

                      MatchStatsTracker.get().startMatch(data.currentMapImage, "AAS");

                      for (ServerPlayer p : level.players()) {
                        String pending = p.getPersistentData().getString("WARFARE_PendingKit");
                        String current = p.getPersistentData().getString("WARFARE_CurrentKit");
                        String kitToApply = !pending.isEmpty() ? pending : current;
                        if (kitToApply != null && !kitToApply.isEmpty() && !kitToApply.equals("Unassigned")) {
                           ResupplyHandler.tryApplyPendingKit(p, data);
                        }
                     }

                     for (BlockPos p : data.triggerBlocks) {
                        if (level.isLoaded(p)) {
                           BlockState st = level.getBlockState(p);
                           if (st.is((Block)ModBlocks.GAME_START_TRIGGER.get())) {
                              level.setBlock(p, (BlockState)st.setValue(GameStartTriggerBlock.POWERED, true), 3);
                              level.scheduleTick(p, (Block)ModBlocks.GAME_START_TRIGGER.get(), 20);
                           }
                        }
                     }

                     sendSyncPacket(level, data);
                  }
               }

               if (data.isGameStarted && (Boolean)WarfareConfig.LOW_TICKETS_SIREN.get()) {
                  if (data.blueTickets <= 50 && data.blueTickets > 0 && !data.playedBlueSiren) {
                     playSirenForTeam(level, "Blue");
                     data.playedBlueSiren = true;
                     data.setDirty();
                  }

                  if (data.redTickets <= 50 && data.redTickets > 0 && !data.playedRedSiren) {
                     playSirenForTeam(level, "Red");
                     data.playedRedSiren = true;
                     data.setDirty();
                  }
               }

               boolean gameEnded = data.blueTickets <= 0 || data.redTickets <= 0;
               if (data.isGameStarted && !gameEnded && !data.capturePoints.isEmpty()) {
                  int totalPoints = data.capturePoints.size();
                  long blueOwned = data.capturePoints.stream().filter(p -> p.owner.equalsIgnoreCase("BLUE")).count();
                  long redOwned = data.capturePoints.stream().filter(p -> p.owner.equalsIgnoreCase("RED")).count();
                  blueIsBleeding = blueOwned == 0L && redOwned >= totalPoints - 1 && totalPoints > 0;
                  redIsBleeding = redOwned == 0L && blueOwned >= totalPoints - 1 && totalPoints > 0;
                  if (globalTick % 40 == 0) {
                     boolean changed = false;
                     if (blueIsBleeding) {
                        data.blueTickets--;
                        changed = true;
                     }

                     if (redIsBleeding) {
                        data.redTickets--;
                        changed = true;
                     }

                     if (changed) {
                        checkGameOver(level, data);
                        data.setDirty();
                     }
                  }
               }

               if (globalTick % 20 == 0) {
                  validateAndSync(level, false, blueIsBleeding, redIsBleeding);
               }

               if (data.countdownActive && data.countdownTicks == 1) {
                  long cdTicks = ((Integer)WarfareConfig.ART_STRIKE_COOLDOWN_MINUTES.get()).intValue() * 60L * 20L;
                  data.blueArtStrikeCD = level.getGameTime() + cdTicks;
                  data.redArtStrikeCD = level.getGameTime() + cdTicks;
               }

               if (data.blueArtRequest != null) {
                  data.blueArtRequest.timer--;
                  if (data.blueArtRequest.timer <= 0) {
                     data.blueArtRequest = null;
                     data.setDirty();
                     PacketHandler.sendToAllClients(level, data);
                  }
               }

                if (data.redArtRequest != null) {
                   data.redArtRequest.timer--;
                   if (data.redArtRequest.timer <= 0) {
                      data.redArtRequest = null;
                      data.setDirty();
                      PacketHandler.sendToAllClients(level, data);
                   }
                }
             }
          }

          if (returnToLobbyTimer > 0) {
             returnToLobbyTimer--;
             if (returnToLobbyTimer == 0 && returnToLobbyServer != null) {
                String lobbyHost = "127.0.0.1";
                int lobbyPort = 25565;
                for (ServerPlayer player : returnToLobbyServer.getPlayerList().getPlayers()) {
                   player.sendSystemMessage(
                      Component.literal("§e[PWP] Returning to lobby..."), false);
                   com.pwp.coreclient.network.PacketHandler.INSTANCE.send(
                      net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                      new ConnectToServerPacket(lobbyHost, lobbyPort));
                }
                returnToLobbyServer.halt(false);
                returnToLobbyServer = null;
             }
          }
       }
    }

   private static void spawnArtShell(ServerLevel level, BlockPos pos) {
      int rad = (Integer)WarfareConfig.ART_STRIKE_RADIUS.get();
      double x = pos.getX() + level.random.nextDouble() * rad * 2.0 - rad;
      double z = pos.getZ() + level.random.nextDouble() * rad * 2.0 - rad;
      double y = level.getMaxBuildHeight() - 2;
      EntityType<?> shellType = (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
      if (shellType != null) {
         Entity shell = shellType.create(level);
         if (shell != null) {
            shell.setPos(x, y, z);
            shell.setDeltaMovement(0.0, 0.0, 0.0);
            level.addFreshEntity(shell);
         }
      }
   }

   private static boolean isTeamReady(ServerLevel level, String teamName, WarfareWorldData data) {
      List<ServerPlayer> teamPlayers = level.players().stream().filter(p -> p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(teamName)).toList();
      if (teamPlayers.isEmpty()) {
         return true;
      }

      long yesVotes = teamPlayers.stream().filter(p -> data.votes.getOrDefault(p.getUUID(), false)).count();
      float percent = (float)yesVotes / teamPlayers.size() * 100.0F;
      return percent >= ((Integer)WarfareConfig.VOTE_REQUIRED_PERCENTAGE.get()).intValue();
   }

   private static List<MapPlayerInfo> buildPlayerInfo(List<ServerPlayer> players, WarfareWorldData data) {
      List<MapPlayerInfo> infoList = new ArrayList<>();

      for (ServerPlayer p : players) {
         String pName = p.getScoreboardName();
         String pTeam = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
         int squadId = p.getPersistentData().getInt("WARFARE_SquadID");
         if (squadId == 0) {
            squadId = -1;
         }

         boolean isLeader = p.getPersistentData().getBoolean("WARFARE_IsSquadLeader");

         for (WarfareWorldData.Squad s : data.squads) {
            if (s.members.contains(pName)) {
               squadId = s.id;
               if (s.leader.equals(pName)) {
                  isLeader = true;
               }
               break;
            }
         }

         boolean inVehicle = p.getVehicle() != null;
         int vId = -1;
         int seatIdx = -1;
         if (inVehicle) {
            Entity vehicle = p.getVehicle();
            vId = vehicle.getId();
            seatIdx = vehicle.getPassengers().indexOf(p);
         }

         boolean downed = p.getPersistentData().getBoolean("WARFARE_IsDowned");
         long shout = p.getPersistentData().getLong("WARFARE_LastMedicShoutTimeMS");
         infoList.add(
            new MapPlayerInfo(pName, p.getUUID(), p.getX(), p.getZ(), p.getYRot(), squadId, isLeader, downed, shout, inVehicle, vId, seatIdx, pTeam)
         );
      }

      return infoList;
   }

   @SubscribeEvent
   public static void onWorldTick(LevelTickEvent event) {
      if (!event.level.isClientSide && event.phase == Phase.END) {
         ServerLevel level = (ServerLevel)event.level;
         WarfareWorldData data = WarfareWorldData.get(level);
         Set<UUID> playersInPreciseZones = new HashSet<>();

         for (WarfareWorldData.CapturePoint point : data.capturePoints) {
            List<ServerPlayer> playersInBox = level.getEntitiesOfClass(ServerPlayer.class, point.area);
            int blueOnPointLiving = 0;
            int redOnPointLiving = 0;

            for (ServerPlayer p : playersInBox) {
               if (p.isAlive() && !p.isSpectator() && !p.getPersistentData().getBoolean("WARFARE_IsDowned") && point.isInside(p.position()) && p.getTeam() != null) {
                  if (p.getTeam().getName().equalsIgnoreCase("Blue")) {
                     blueOnPointLiving++;
                  } else if (p.getTeam().getName().equalsIgnoreCase("Red")) {
                     redOnPointLiving++;
                  }
               }
            }

            String dominantTeam = "NONE";
            int alliesOnPoint = 0;
            boolean isContested = false;
            boolean isTimeLocked = level.getGameTime() < point.lockedUntilTick;
            if (blueOnPointLiving > 0 && redOnPointLiving > 0) {
               if (blueOnPointLiving >= redOnPointLiving * 2) {
                  dominantTeam = "BLUE";
                  alliesOnPoint = blueOnPointLiving;
               } else if (redOnPointLiving >= blueOnPointLiving * 2) {
                  dominantTeam = "RED";
                  alliesOnPoint = redOnPointLiving;
               } else {
                  isContested = true;
               }
            } else if (blueOnPointLiving > 0) {
               dominantTeam = "BLUE";
               alliesOnPoint = blueOnPointLiving;
            } else if (redOnPointLiving > 0) {
               dominantTeam = "RED";
               alliesOnPoint = redOnPointLiving;
            }

            float multiplier = 1.0F;
            if (alliesOnPoint > 1) {
               multiplier += (alliesOnPoint - 1) * 0.5F;
            }

            if (multiplier > 4.0F) {
               multiplier = 4.0F;
            }

            int currentRate = 0;
            String teamToSync = "NONE";
            if (!dominantTeam.equals("NONE") && !isContested && !isTimeLocked) {
               boolean isOwner = point.owner.equals(dominantTeam);
               boolean isFullyCaptured = isOwner && point.progress >= 1.0F;
               if (!isFullyCaptured && canCapture(point, dominantTeam, data)) {
                  teamToSync = dominantTeam;
                  boolean isNeutralizing = !point.owner.equals("NEUTRAL") && !point.owner.equals(dominantTeam)
                     || point.owner.equals("NEUTRAL") && !point.capturingTeam.equals("NONE") && !point.capturingTeam.equals(dominantTeam);
                  currentRate = Math.round(multiplier);
                  if (isNeutralizing) {
                     currentRate = -currentRate;
                  }
               }
            }

            for (ServerPlayer p : playersInBox) {
               if (point.isInside(p.position())) {
                  playersInPreciseZones.add(p.getUUID());
                  boolean lockedUI = false;
                  String nextObjectiveForPlayer = "";
                  if (p.getTeam() != null) {
                     String teamKey = p.getTeam().getName().equalsIgnoreCase("Blue") ? "BLUE" : "RED";
                     if (isTimeLocked) {
                        lockedUI = true;
                        long totalSeconds = (point.lockedUntilTick - level.getGameTime()) / 20L;
                        nextObjectiveForPlayer = String.format("LOCKED: %dРј %dСЃ", totalSeconds / 60L, totalSeconds % 60L);
                     } else if (!canCapture(point, teamKey, data)) {
                        lockedUI = true;
                        nextObjectiveForPlayer = findRequiredPointName(point, teamKey, data);
                     }
                  }

                  PacketHandler.INSTANCE
                     .send(
                        PacketDistributor.PLAYER.with(() -> p),
                        new PacketSyncPoint(
                           true, point.name, point.owner, point.progress, lockedUI, nextObjectiveForPlayer, isContested, teamToSync, currentRate
                        )
                     );
               }
            }

            boolean isBeingActivelyCaptured = false;
            if (!dominantTeam.equals("NONE") && !isContested && !isTimeLocked && canCapture(point, dominantTeam, data)) {
               isBeingActivelyCaptured = true;
               float baseSpeed = 1.0F / (point.captureTimeMinutes * 60 * 20);
                String oldOwner = point.owner;
                handleTeamInfluence(point, dominantTeam, multiplier, baseSpeed, data, level);
                if (!point.owner.equals(oldOwner)) {
                    for (ServerPlayer p : playersInBox) {
                        if (p.isAlive() && !p.isSpectator() && p.getTeam() != null
                                && p.getTeam().getName().equalsIgnoreCase(dominantTeam)) {
                            MatchStatsTracker.get().recordCapture(p);
                        }
                    }
                }
               if (!point.owner.equals(oldOwner) && !point.owner.equals("NEUTRAL") && point.lockDurationMinutes > 0) {
                  point.lockedUntilTick = level.getGameTime() + point.lockDurationMinutes * 60L * 20L;
                  data.setDirty();
               }
            }

            if (!isBeingActivelyCaptured && !isContested && !isTimeLocked) {
               float baseSpeed = 1.0F / (point.captureTimeMinutes * 60 * 20);
               if (point.owner.equals("NEUTRAL") && point.progress > 0.0F) {
                  point.progress -= baseSpeed / 2.0F;
                  if (point.progress <= 0.0F) {
                     point.progress = 0.0F;
                     point.capturingTeam = "NONE";
                  }
               } else if (!point.owner.equals("NEUTRAL") && point.progress < 1.0F) {
                  point.progress += baseSpeed / 2.0F;
                  if (point.progress >= 1.0F) {
                     point.progress = 1.0F;
                     point.capturingTeam = "NONE";
                  }
               }
            }

            if (level.getGameTime() % 10L == 0L) {
               boolean isPointActive = point.progress > 0.0F && point.progress < 1.0F || !point.capturingTeam.equals("NONE");
               if (isPointActive) {
                  for (ServerPlayer player : level.players()) {
                     if (!playersInPreciseZones.contains(player.getUUID())) {
                        PacketHandler.INSTANCE
                           .send(
                              PacketDistributor.PLAYER.with(() -> player),
                              new PacketSyncPoint(false, point.name, point.owner, point.progress, false, "", isContested, point.capturingTeam, 0)
                           );
                     }
                  }
               }
            }
         }

         for (ServerPlayer player : level.players()) {
            if (!playersInPreciseZones.contains(player.getUUID())) {
               PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncPoint(false, "", "", 0.0F, false, "", false, "NONE", 0));
            }
         }
      }
   }

   private static void handleMainProtectionZones(ServerLevel level, WarfareWorldData data) {
      if (!data.mainZones.isEmpty()) {
         if (!data.isGameStarted) {
            for (ServerPlayer player : level.players()) {
               if (!player.isCreative() && !player.isSpectator()) {
                  String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                  if (!pTeam.equals("NEUTRAL")) {
                     for (WarfareWorldData.MainProtectionZone zone : data.mainZones) {
                        if (zone.team.equalsIgnoreCase(pTeam)) {
                           if (!zone.isInside(player.position())) {
                              String dim = level.dimension().location().toString();
                              BlockPos spawn = pTeam.equals("BLUE") ? data.blueSpawns.get(dim) : data.redSpawns.get(dim);
                              if (spawn != null) {
                                 player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                                 player.displayClientMessage(
                                    Component.literal("The match hasn't started yet! Wait in the Main Base.")
                                       .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}),
                                    true
                                 );
                                 player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 1.0F, 1.0F);
                              }
                           }
                           break;
                        }
                     }
                  }
               }
            }
         } else {
            label207:
            for (WarfareWorldData.MainProtectionZone zone : data.mainZones) {
               List<Entity> entitiesInZone = level.getEntitiesOfClass(Entity.class, zone.area);
               Iterator var23 = entitiesInZone.iterator();

               while (true) {
                  Entity entity;
                  while (true) {
                     if (!var23.hasNext()) {
                        continue label207;
                     }

                     entity = (Entity)var23.next();
                     if (zone.isInside(entity.position())) {
                        if (!entity.getPersistentData().contains("WARFARE_SpawnGraceTick")) {
                           break;
                        }

                        long graceTick = entity.getPersistentData().getLong("WARFARE_SpawnGraceTick");
                        if (level.getGameTime() - graceTick >= 100L) {
                           break;
                        }
                     }
                  }

                  if (entity instanceof ServerPlayer player) {
                     if (!player.isCreative() && !player.isSpectator()) {
                        String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                        if (!pTeam.equalsIgnoreCase(zone.team)) {
                           int ticks = player.getPersistentData().getInt("WARFARE_MainZoneTimer");
                           if (++ticks >= 200) {
                              player.getPersistentData().remove("WARFARE_MainZoneTimer");
                              player.kill();
                           } else {
                              player.getPersistentData().putInt("WARFARE_MainZoneTimer", ticks);
                              if (ticks % 20 == 0) {
                                 int secLeft = 10 - ticks / 20;
                                 player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 0));
                                 player.connection
                                    .send(
                                       new ClientboundSetSubtitleTextPacket(
                                          Component.literal("You will be killed in " + secLeft + "s!").withStyle(ChatFormatting.RED)
                                       )
                                    );
                                 player.connection
                                    .send(
                                       new ClientboundSetTitleTextPacket(
                                          Component.literal("ENEMY MAIN BASE").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD})
                                       )
                                    );
                              }
                           }
                        } else {
                           player.getPersistentData().remove("WARFARE_MainZoneTimer");
                        }
                     }
                  } else if (entity.getPersistentData().contains("WARFARE_VehicleTeam")) {
                     String vTeam = entity.getPersistentData().getString("WARFARE_VehicleTeam");
                     if (!vTeam.equalsIgnoreCase("NEUTRAL") && !vTeam.isEmpty()) {
                        if (vTeam.equalsIgnoreCase(zone.team)) {
                           entity.getPersistentData().remove("WARFARE_MainZoneTimer");
                        } else {
                           int ticks = entity.getPersistentData().getInt("WARFARE_MainZoneTimer");
                           if (++ticks >= 200) {
                              if (entity instanceof LivingEntity le) {
                                 le.kill();
                              } else {
                                 entity.discard();
                              }
                           } else {
                              entity.getPersistentData().putInt("WARFARE_MainZoneTimer", ticks);
                              if (ticks % 20 == 0) {
                                 int secLeft = 10 - ticks / 20;

                                 for (Entity passenger : entity.getPassengers()) {
                                    if (passenger instanceof ServerPlayer p) {
                                       p.displayClientMessage(
                                          Component.literal("WARNING! Enemy base! Vehicle destroyed in " + secLeft + "s!")
                                             .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}),
                                          true
                                       );
                                    }
                                 }
                              }
                           }
                        }
                     } else {
                        entity.discard();
                     }
                  } else if (!(entity instanceof PartEntity) && !(entity instanceof Display) && !(entity instanceof ItemEntity)) {
                     entity.discard();
                  }
               }
            }

            for (ServerPlayer player : level.players()) {
               if (player.getPersistentData().contains("WARFARE_MainZoneTimer")) {
                  boolean inEnemyZone = false;
                  String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";

                  for (WarfareWorldData.MainProtectionZone zone : data.mainZones) {
                     if (!zone.team.equalsIgnoreCase(pTeam) && zone.isInside(player.position())) {
                        inEnemyZone = true;
                        break;
                     }
                  }

                  if (inEnemyZone) {
                     player.getPersistentData().remove("WARFARE_MainZoneExitTick");
                  } else {
                     long exitTick = player.getPersistentData().getLong("WARFARE_MainZoneExitTick");
                     if (exitTick == 0) {
                        player.getPersistentData().putLong("WARFARE_MainZoneExitTick", level.getGameTime());
                     } else if (level.getGameTime() - exitTick >= 200) {
                        player.getPersistentData().remove("WARFARE_MainZoneTimer");
                        player.getPersistentData().remove("WARFARE_MainZoneExitTick");
                     }
                  }
               }
            }

            for (WarfareWorldData.VehicleRecord record : data.markedVehicles) {
               Entity vehicle = level.getEntity(record.uuid);
               if (vehicle != null && vehicle.getPersistentData().contains("WARFARE_MainZoneTimer")) {
                  boolean inEnemyZone = false;
                  String vTeam = vehicle.getPersistentData().getString("WARFARE_VehicleTeam");

                  for (WarfareWorldData.MainProtectionZone zone : data.mainZones) {
                     if (!zone.team.equalsIgnoreCase(vTeam) && zone.isInside(vehicle.position())) {
                        inEnemyZone = true;
                        break;
                     }
                  }

                  if (inEnemyZone) {
                     vehicle.getPersistentData().remove("WARFARE_MainZoneExitTick");
                  } else {
                     long exitTick = vehicle.getPersistentData().getLong("WARFARE_MainZoneExitTick");
                     if (exitTick == 0) {
                        vehicle.getPersistentData().putLong("WARFARE_MainZoneExitTick", level.getGameTime());
                     } else if (level.getGameTime() - exitTick >= 200) {
                        vehicle.getPersistentData().remove("WARFARE_MainZoneTimer");
                        vehicle.getPersistentData().remove("WARFARE_MainZoneExitTick");
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerChangeDimension(EntityTravelToDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ServerLevel level = player.serverLevel();
         WarfareWorldData data = WarfareWorldData.get(level);
         PacketSquadAction.leaveCurrentSquad(player, data);
         PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncSquads(data.squads));
         player.sendSystemMessage(Component.literal("You were removed from the squad because you changed worlds.").withStyle(ChatFormatting.YELLOW));
      }
   }

   private static String findRequiredPointName(WarfareWorldData.CapturePoint currentPoint, String team, WarfareWorldData data) {
      int currentPriority = team.equals("BLUE") ? currentPoint.bluePriority : currentPoint.redPriority;
      int requiredPriority = currentPriority - 1;
      if (requiredPriority < 1) {
         return "";
      }

      for (WarfareWorldData.CapturePoint p : data.capturePoints) {
         int pPriority = team.equals("BLUE") ? p.bluePriority : p.redPriority;
         if (pPriority == requiredPriority) {
            return p.name;
         }
      }

      return "Unknown";
   }

   @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
       if (!event.getEntity().level().isClientSide) {
          ServerPlayer player = (ServerPlayer)event.getEntity();
          PermissionHelper.autoOpIfAdmin(player);
          ServerLevel level = player.serverLevel();
          WarfareWorldData data = WarfareWorldData.get(level);
         sendSyncPacket(level, data);
         PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncSquads(data.squads));
         PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDownedState(player.getId(), false));
         if (!data.isGameStarted && player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("PWP Warfare is paused. /pwpwarfare gamestart true to start.").withStyle(ChatFormatting.YELLOW));
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerRespawnEvent event) {
      if (!event.getEntity().level().isClientSide) {
         ServerPlayer newPlayer = (ServerPlayer)event.getEntity();
         if (newPlayer.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
            newPlayer.setGameMode(GameType.SURVIVAL);
         }

         newPlayer.connection.send(new ClientboundPlayerAbilitiesPacket(newPlayer.getAbilities()));
         pendingRespawnLocations.remove(newPlayer.getUUID());
         pendingTeams.remove(newPlayer.getUUID());
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityDeath(LivingDeathEvent event) {
       if (!event.getEntity().level().isClientSide) {
          Entity entity = event.getEntity();
          if (entity instanceof ServerPlayer victim) {
             Entity sourceEntity = event.getSource().getEntity();
             String weapon = event.getSource().getMsgId();

             // Прямое убийство игроком
             if (sourceEntity instanceof ServerPlayer killer) {
                boolean isTeamkill = killer.getTeam() != null && victim.getTeam() != null
                        && killer.getTeam().isAlliedTo(victim.getTeam());

                if (!victim.getPersistentData().getBoolean("WARFARE_IsDowned") && isTeamkill) {
                   DownedHandler.handleTeamkill(killer);
                }

                if (isTeamkill) {
                   MatchStatsTracker.get().recordTeamKill(killer, victim);
                } else {
                   double dist = killer.distanceTo(victim);
                   MatchStatsTracker.get().recordKill(killer, victim, weapon, dist);
                }

             // Убийство техникой
             } else if (sourceEntity != null && sourceEntity.getPersistentData().contains("WARFARE_VehicleTeam")) {
                List<Entity> passengers = sourceEntity.getPassengers();
                if (!passengers.isEmpty() && passengers.get(0) instanceof ServerPlayer driver) {
                   MatchStatsTracker.get().recordVehicleKill(driver);
                }

             // Смерть через downed system (forceGiveUp/bleed out)
             } else {
                net.minecraft.world.entity.LivingEntity lastAttacker = victim.getLastHurtByMob();
                if (lastAttacker instanceof ServerPlayer killer && killer.isAlive()) {
                   double dist = killer.distanceTo(victim);
                   MatchStatsTracker.get().recordKill(killer, victim, "knockout", dist);
                }
             }

             processEntityLoss(victim);
             victim.getPersistentData().putBoolean("WARFARE_IsDowned", false);
             if (victim.getPersistentData().getBoolean("WARFARE_GivingUp")) {
                return;
             }
          } else if (entity.getPersistentData().contains("WARFARE_TicketPenalty")) {
             processEntityLoss(entity);
          }
       }
    }

   private static void saveKitNbtBeforeDeath(ServerPlayer player) {
      WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
      String kitName = player.getPersistentData().getString("WARFARE_CurrentKit");
      String team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
      if (!team.isEmpty() && !kitName.isEmpty() && !kitName.equals("Unassigned")) {
         WarfareWorldData.KitInfo kit = team.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
         if (kit != null) {
            Map<Integer, CompoundTag> savedTags = new HashMap<>();

            for (int i = 0; i < 41; i++) {
               if (i < kit.saveNbtFlags.length && kit.saveNbtFlags[i]) {
                  ItemStack item = player.getInventory().getItem(i);
                  if (!item.isEmpty() && item.hasTag()) {
                     savedTags.put(i, item.getTag().copy());
                  }
               }
            }

            if (!savedTags.isEmpty()) {
               PERSISTENT_NBT_STORAGE.put(player.getUUID(), savedTags);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      ServerPlayer oldPlayer = (ServerPlayer)event.getOriginal();
      ServerPlayer newPlayer = (ServerPlayer)event.getEntity();
      newPlayer.getPersistentData().putBoolean("WARFARE_IsDowned", false);
      newPlayer.getPersistentData().remove("WARFARE_GivingUp");
      newPlayer.getPersistentData().remove("WARFARE_DownedYaw");
      newPlayer.getPersistentData().remove("WARFARE_DownedPitch");
      newPlayer.getPersistentData().putLong("WARFARE_LastReviveTime", 0L);
      PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDownedState(newPlayer.getId(), false));
      CompoundTag oldData = oldPlayer.getPersistentData();
      CompoundTag newData = newPlayer.getPersistentData();
      if (oldData.contains("WARFARE_SquadID")) {
         newData.putInt("WARFARE_SquadID", oldData.getInt("WARFARE_SquadID"));
      }

      if (oldData.contains("WARFARE_IsSquadLeader")) {
         newData.putBoolean("WARFARE_IsSquadLeader", oldData.getBoolean("WARFARE_IsSquadLeader"));
      }

      if (oldData.contains("WARFARE_CurrentKit")) {
         newData.putString("WARFARE_CurrentKit", oldData.getString("WARFARE_CurrentKit"));
      }

      if (oldData.contains("WARFARE_PendingKit")) {
         newData.putString("WARFARE_PendingKit", oldData.getString("WARFARE_PendingKit"));
      }

      if (oldData.contains("WARFARE_LastFobResupply")) {
         newData.putLong("WARFARE_LastFobResupply", oldData.getLong("WARFARE_LastFobResupply"));
      }

      if (oldData.contains("WARFARE_LastMainResupply")) {
         newData.putLong("WARFARE_LastMainResupply", oldData.getLong("WARFARE_LastMainResupply"));
      }

      if (event.isWasDeath()) {
         String teamName = oldPlayer.getTeam() != null ? oldPlayer.getTeam().getName() : null;
         if (teamName != null) {
            Scoreboard scoreboard = newPlayer.getScoreboard();
            PlayerTeam pTeam = scoreboard.getPlayerTeam(teamName);
            if (pTeam != null) {
               scoreboard.addPlayerToTeam(newPlayer.getScoreboardName(), pTeam);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onEntityRemove(EntityLeaveLevelEvent event) {
      if (!event.getLevel().isClientSide) {
         Entity entity = event.getEntity();
         if (!(entity instanceof LivingEntity)) {
            RemovalReason reason = entity.getRemovalReason();
            if (reason != null && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
               processEntityLoss(entity);
            }
         }
      }
   }

   private static void processEntityLoss(Entity entity) {
      if (entity.level() != null && entity.level().getServer() != null) {
         ServerLevel level = (ServerLevel)entity.level();
         WarfareWorldData data = WarfareWorldData.get(level);
         boolean markerRemoved = data.markedVehicles.removeIf(v -> v.uuid.equals(entity.getUUID()));
         if (data.blueTickets > 0 && data.redTickets > 0) {
            boolean ticketsChanged = false;
            if (entity instanceof ServerPlayer player && player.getTeam() != null) {
               String teamName = player.getTeam().getName();
               int cost = data.deathTicketCost;
               if (teamName.equalsIgnoreCase("Blue")) {
                  data.blueTickets = Math.max(0, data.blueTickets - cost);
                  ticketsChanged = true;
               } else if (teamName.equalsIgnoreCase("Red")) {
                  data.redTickets = Math.max(0, data.redTickets - cost);
                  ticketsChanged = true;
               }
            }

            if (entity.getPersistentData().contains("WARFARE_TicketPenalty")) {
               int penalty = entity.getPersistentData().getInt("WARFARE_TicketPenalty");
               String vTeam = entity.getPersistentData().getString("WARFARE_VehicleTeam");
               String vType = entity.getPersistentData().getString("WARFARE_VehicleType");
               entity.getPersistentData().remove("WARFARE_TicketPenalty");
               if (penalty > 0) {
                  if (vTeam.equalsIgnoreCase("BLUE")) {
                     data.blueTickets = Math.max(0, data.blueTickets - penalty);
                     ticketsChanged = true;
                     broadcastMessage(level, "BLUE lost " + vType + " (-" + penalty + ")", ChatFormatting.BLUE);
                  } else if (vTeam.equalsIgnoreCase("RED")) {
                     data.redTickets = Math.max(0, data.redTickets - penalty);
                     ticketsChanged = true;
                     broadcastMessage(level, "RED lost " + vType + " (-" + penalty + ")", ChatFormatting.RED);
                  }
               }
            }

            if (ticketsChanged || markerRemoved) {
               if (ticketsChanged) {
                  checkGameOver(level, data);
               }

               data.setDirty();
               sendSyncPacket(level, data);
            }
         } else {
            if (markerRemoved) {
               data.setDirty();
               sendSyncPacket(level, data);
            }
         }
      }
   }

   private static float getVehicleHealth(Entity entity) {
      try {
         Object result = entity.getClass().getMethod("getHealth").invoke(entity);
         if (result instanceof Float health) {
            return health;
         }
      } catch (Exception ignored) {
      }
      return Float.MAX_VALUE;
   }

   private static void processWreckLoss(Entity entity, WarfareWorldData data, ServerLevel level) {
      if (entity.getPersistentData().contains("WARFARE_TicketPenalty")) {
         int penalty = entity.getPersistentData().getInt("WARFARE_TicketPenalty");
         String vTeam = entity.getPersistentData().getString("WARFARE_VehicleTeam");
         String vType = entity.getPersistentData().getString("WARFARE_VehicleType");
         entity.getPersistentData().remove("WARFARE_TicketPenalty");
         boolean markerRemoved = data.markedVehicles.removeIf(v -> v.uuid.equals(entity.getUUID()));
         if (penalty > 0 && data.blueTickets > 0 && data.redTickets > 0) {
            boolean ticketsChanged = false;
            if (vTeam.equalsIgnoreCase("BLUE")) {
               data.blueTickets = Math.max(0, data.blueTickets - penalty);
               ticketsChanged = true;
               broadcastMessage(level, "BLUE lost " + vType + " (-" + penalty + ")", ChatFormatting.BLUE);
            } else if (vTeam.equalsIgnoreCase("RED")) {
               data.redTickets = Math.max(0, data.redTickets - penalty);
               ticketsChanged = true;
               broadcastMessage(level, "RED lost " + vType + " (-" + penalty + ")", ChatFormatting.RED);
            }
            if (ticketsChanged || markerRemoved) {
               if (ticketsChanged) {
                  checkGameOver(level, data);
               }
               data.setDirty();
               sendSyncPacket(level, data);
            }
         } else if (markerRemoved) {
            data.setDirty();
            sendSyncPacket(level, data);
         }
      }
   }

   private static void handleTeamInfluence(
      WarfareWorldData.CapturePoint point, String attackingTeam, float multiplier, float baseSpeed, WarfareWorldData data, ServerLevel level
   ) {
      float speedBoosted = baseSpeed * multiplier;
      if (point.owner.equals("NEUTRAL")) {
         if (!point.capturingTeam.equals("NONE") && !point.capturingTeam.equals(attackingTeam)) {
            point.progress -= speedBoosted;
            if (point.progress <= 0.0F) {
               point.progress = 0.0F;
               point.capturingTeam = "NONE";
            }
         } else {
            point.capturingTeam = attackingTeam;
            point.progress += speedBoosted;
            if (point.progress >= 1.0F) {
               point.progress = 1.0F;
               point.owner = attackingTeam;
               PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketCaptureNotification(point.name, attackingTeam, false));
               if (point.captureDeduction > 0) {
                  if (attackingTeam.equals("BLUE")) {
                     data.redTickets = data.redTickets - point.captureDeduction;
                  } else {
                     data.blueTickets = data.blueTickets - point.captureDeduction;
                  }

                  checkGameOver(level, data);
               }

               data.setDirty();
               sendSyncPacket(level, data);
            }
         }
      } else if (point.owner.equals(attackingTeam)) {
         if (point.progress < 1.0F) {
            point.progress += speedBoosted;
            if (point.progress > 1.0F) {
               point.progress = 1.0F;
            }
         }
      } else {
         point.progress -= speedBoosted;
         if (point.progress <= 0.0F) {
            String oldOwnerName = point.owner;
            if (point.owner.equals("BLUE")) {
               data.blueTickets = data.blueTickets - point.ticketPenalty;
            } else {
               data.redTickets = data.redTickets - point.ticketPenalty;
            }

            checkGameOver(level, data);
            point.owner = "NEUTRAL";
            point.progress = 0.0F;
            point.capturingTeam = "NONE";
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketCaptureNotification(point.name, attackingTeam, true));
            data.setDirty();
            sendSyncPacket(level, data);
         }
      }
   }

   private static void validateAndSync(ServerLevel level, boolean forceSend, boolean blueBleed, boolean redBleed) {
      WarfareWorldData data = WarfareWorldData.get(level);
      boolean changed = false;
      String dimKey = level.dimension().location().toString();
      changed |= validateRallies(level, data.blueRallies);
      changed |= validateRallies(level, data.redRallies);
      int hubRadius = (Integer)WarfareConfig.HUB_BLOCK_RADIUS.get();
      int hubEnemiesRequired = (Integer)WarfareConfig.HUB_BLOCK_ENEMY_COUNT.get();

      for (WarfareWorldData.HubInfo hub : data.hubs) {
         if (hub.constructed && hub.dimension != null && hub.dimension.equals(dimKey) && level.isLoaded(hub.pos)) {
            boolean nowBlocked = getEnemyCount(level, hub.pos, hub.team, hubRadius) >= hubEnemiesRequired;
            if (hub.isBlocked != nowBlocked) {
               hub.isBlocked = nowBlocked;
               changed = true;
            }

            if (level.getBlockEntity(hub.pos) instanceof HubBlockEntity hubBe && hub.materials != hubBe.getMaterials()) {
               hub.materials = hubBe.getMaterials();
               changed = true;
            }
         }
      }

      int rallyRadius = (Integer)WarfareConfig.RALLY_BLOCK_RADIUS.get();
      int rallyEnemiesRequired = (Integer)WarfareConfig.RALLY_BLOCK_ENEMY_COUNT.get();
      boolean anySquadStatusChanged = false;
      boolean teamBlueBlocked = false;
      boolean teamRedBlocked = false;

      for (WarfareWorldData.Squad squad : data.squads) {
         if (squad.rallyPos != null && squad.rallyDimension.equals(dimKey)) {
            int enemies = getEnemyCount(level, squad.rallyPos, squad.team, rallyRadius);
            boolean currentlyBlocked = enemies >= rallyEnemiesRequired;
            if (squad.isRallyBlocked != currentlyBlocked) {
               squad.isRallyBlocked = currentlyBlocked;
               anySquadStatusChanged = true;
            }

            if (currentlyBlocked) {
               if (squad.team.equalsIgnoreCase("Blue")) {
                  teamBlueBlocked = true;
               } else {
                  teamRedBlocked = true;
               }
            }
         }
      }

      Boolean cachedBlue = lastBlueBlockedMap.getOrDefault(dimKey, false);
      Boolean cachedRed = lastRedBlockedMap.getOrDefault(dimKey, false);
      if (forceSend || changed || anySquadStatusChanged || teamBlueBlocked != cachedBlue || teamRedBlocked != cachedRed || blueBleed || redBleed) {
         sendSyncPacket(level, data, blueBleed, redBleed, teamBlueBlocked, teamRedBlocked);
         if (anySquadStatusChanged) {
            PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), new PacketSyncSquads(data.squads));
         }

         lastBlueBlockedMap.put(dimKey, teamBlueBlocked);
         lastRedBlockedMap.put(dimKey, teamRedBlocked);
      }

      if (changed || anySquadStatusChanged) {
         data.setDirty();
      }
   }

   private static void validateAndSync(ServerLevel level, boolean forceSend) {
      validateAndSync(level, forceSend, false, false);
   }

   private static boolean validateRallies(ServerLevel level, List<BlockPos> rallies) {
      return rallies.removeIf(pos -> level.isLoaded(pos) ? !(level.getBlockState(pos).getBlock() instanceof RallyPointBlock) : false);
   }

   private static boolean isEnemyNearby(ServerLevel level, BlockPos pos, String allyTeamName, int radius, int minCount) {
      return getEnemyCount(level, pos, allyTeamName, radius) >= minCount;
   }

   private static int getEnemyCount(ServerLevel level, BlockPos pos, String allyTeamName, int radius) {
      AABB checkArea = new AABB(pos).inflate(radius);
      List<ServerPlayer> enemies = level.getEntitiesOfClass(ServerPlayer.class, checkArea);
      int count = 0;

      for (ServerPlayer p : enemies) {
         if (!p.isSpectator() && (p.getTeam() == null || !p.getTeam().getName().equalsIgnoreCase(allyTeamName))) {
            count++;
         }
      }

      return count;
   }

   public static int getEnemyCount(ServerLevel level, BlockPos pos, String allyTeamName) {
      AABB checkArea = new AABB(pos).inflate(40.0);
      List<ServerPlayer> enemies = level.getEntitiesOfClass(ServerPlayer.class, checkArea);
      int count = 0;

      for (ServerPlayer p : enemies) {
         if (!p.isSpectator() && (p.getTeam() == null || !p.getTeam().getName().equalsIgnoreCase(allyTeamName))) {
            count++;
         }
      }

      return count;
   }

   public static void checkGameOver(ServerLevel level, WarfareWorldData data) {
      if (data.blueTickets <= 0) {
         data.blueTickets = 0;
         executeVictory(level, data, false);
      } else if (data.redTickets <= 0) {
         data.redTickets = 0;
         executeVictory(level, data, true);
      }
   }

    private static void executeVictory(ServerLevel level, WarfareWorldData data, boolean blueWon) {
        data.isGameStarted = false;
        data.setDirty();

        String winner = blueWon ? "BLUE" : "RED";
        MatchStatsTracker.get().finalizeMatch(winner, data.blueTickets, data.redTickets);

        String winnerName;
       String winnerFaction;
       if (blueWon) {
          winnerFaction = data.blueFaction;
          winnerName = winnerFaction != null && !winnerFaction.equals("none") && !winnerFaction.equals("bluefor")
             ? formatFactionName(winnerFaction)
             : (String)WarfareConfig.BLUE_TEAM_CUSTOM_NAME.get();
          if (winnerName.isEmpty()) {
             winnerName = "BLUE TEAM";
          }
       } else {
          winnerFaction = data.redFaction;
          winnerName = winnerFaction != null && !winnerFaction.equals("none") && !winnerFaction.equals("redfor")
             ? formatFactionName(winnerFaction)
             : (String)WarfareConfig.RED_TEAM_CUSTOM_NAME.get();
          if (winnerName.isEmpty()) {
             winnerName = "RED TEAM";
          }
       }

       String subText = (blueWon ? data.blueTickets : data.redTickets) + " tickets remaining";
       PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketOpenVictoryScreen(winnerName, winnerFaction, subText, blueWon));
       Scoreboard scoreboard = level.getScoreboard();
       PlayerTeam blueTeam = scoreboard.getPlayerTeam("Blue");
       PlayerTeam redTeam = scoreboard.getPlayerTeam("Red");
       if (blueTeam != null) {
          blueTeam.setAllowFriendlyFire(false);
       }

       if (redTeam != null) {
          redTeam.setAllowFriendlyFire(false);
       }

       String currentDim = level.dimension().location().toString();

       for (ServerPlayer player : level.players()) {
          if (player.getPersistentData().getBoolean("WARFARE_TeamKillSpectator")) {
             player.getPersistentData().remove("WARFARE_TeamKillSpectator");
             player.getPersistentData().remove("WARFARE_TeamKills");
             player.getPersistentData().remove("WARFARE_BaseRestrictedUntil");
             player.setGameMode(GameType.SURVIVAL);
          }

          if (!player.isCreative() && !player.isSpectator()) {
             if (player.getPersistentData().getBoolean("WARFARE_IsDowned")) {
                DownedHandler.revivePlayer(player);
             }

             player.setHealth(player.getMaxHealth());
             player.getPersistentData().putString("WARFARE_CurrentKit", "Unassigned");
             player.getPersistentData().remove("WARFARE_PendingKit");
             player.getInventory().clearContent();
             ResupplyHandler.clearCurios(player);
             player.inventoryMenu.broadcastChanges();
             player.containerMenu.broadcastChanges();
             String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
             BlockPos spawnPos = null;
             if (pTeam.equals("BLUE")) {
                spawnPos = data.blueSpawns.get(currentDim);
             } else if (pTeam.equals("RED")) {
                spawnPos = data.redSpawns.get(currentDim);
             }

             if (spawnPos != null) {
                player.setRespawnPosition(level.dimension(), spawnPos, 0.0F, true, false);
                if (player.isAlive()) {
                   player.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                }
             }
          }
       }

       returnToLobbyTimer = 300;
       returnToLobbyServer = level.getServer();
    }

   private static String formatFactionName(String faction) {
      return faction.replace("_", " ").toUpperCase();
   }

   private static void sendTitleToLevel(ServerLevel level, String text, ChatFormatting color) {
      Component title = Component.literal(text).withStyle(color).withStyle(ChatFormatting.BOLD);

      for (ServerPlayer player : level.players()) {
         player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 10));
         player.connection.send(new ClientboundSetTitleTextPacket(title));
      }
   }

   private static void sendTitleToAll(MinecraftServer server, String text, ChatFormatting color) {
      Component title = Component.literal(text).withStyle(color).withStyle(ChatFormatting.BOLD);

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 10));
         player.connection.send(new ClientboundSetTitleTextPacket(title));
      }
   }

   private static void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
      level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(text).withStyle(color), false);
   }

   private static void sendSyncPacket(ServerLevel level, WarfareWorldData data) {
      sendSyncPacket(level, data, false, false);
   }

   private static void sendSyncPacket(ServerLevel level, WarfareWorldData data, boolean blueBleed, boolean redBleed) {
      String dimKey = level.dimension().location().toString();
      boolean bBlocked = lastBlueBlockedMap.getOrDefault(dimKey, false);
      boolean rBlocked = lastRedBlockedMap.getOrDefault(dimKey, false);
      sendSyncPacket(level, data, blueBleed, redBleed, bBlocked, rBlocked);
   }

   private static void sendSyncPacket(ServerLevel level, WarfareWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
      PacketSyncGameData packet = createSyncPacket(data, blueBleed, redBleed, bBlocked, rBlocked);
      PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
   }

   private static PacketSyncGameData createSyncPacket(WarfareWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
      boolean hasBlue = !data.blueRallies.isEmpty();
      boolean hasRed = !data.redRallies.isEmpty();
      String bName = data.blueFaction.equals("none") ? (String)WarfareConfig.BLUE_TEAM_CUSTOM_NAME.get() : data.blueFaction.toUpperCase();
      String rName = data.redFaction.equals("none") ? (String)WarfareConfig.RED_TEAM_CUSTOM_NAME.get() : data.redFaction.toUpperCase();
      Map<String, String> pKits = new HashMap<>();
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String current = p.getPersistentData().getString("WARFARE_CurrentKit");
            String pending = p.getPersistentData().getString("WARFARE_PendingKit");
            String displayKit = !pending.isEmpty() ? pending : current;
            pKits.put(p.getScoreboardName(), displayKit.isEmpty() ? "Unassigned" : displayKit);
         }
      }

      return new PacketSyncGameData(
         data.blueTickets,
         data.redTickets,
         hasBlue,
         hasRed,
         blueBleed,
         redBleed,
         data.respawnTimer,
         bBlocked,
         rBlocked,
         (Boolean)WarfareConfig.HUB_SPAWN_COSTS_MATERIALS.get(),
         (Integer)WarfareConfig.HUB_SPAWN_MATERIAL_COST.get(),
         data.mapCenterX,
         data.mapCenterZ,
         data.mapSizeBlocks,
         data.currentMapImage,
         data.markedVehicles,
         data.hubs,
         data.blueFaction,
         data.redFaction,
         bName,
         rName,
         data.isGameStarted,
         data.capturePoints,
         data.blueSpawns,
         data.redSpawns,
         data.neutralSpawns,
         pKits,
         data.activeMarkers,
         data.voteActive,
         data.voteTimer,
         data.votes,
         data.blueCMDId,
         data.redCMDId,
         data.blueCmdVoteActive,
         data.blueCmdCandidateName,
         data.blueCmdCandidateId,
         data.blueCmdVoteTimer,
         data.blueCmdVotes,
         data.redCmdVoteActive,
         data.redCmdCandidateName,
         data.redCmdCandidateId,
         data.redCmdVoteTimer,
         data.redCmdVotes,
         data.activeStrikes,
         data.blueArtRequest != null ? data.blueArtRequest.pos : BlockPos.ZERO,
         data.redArtRequest != null ? data.redArtRequest.pos : BlockPos.ZERO,
         data.blueArtRequest != null ? data.blueArtRequest.timer : 0,
         data.redArtRequest != null ? data.redArtRequest.timer : 0,
         data.blueArtRequest != null ? data.blueArtRequest.requesterName : "",
         data.redArtRequest != null ? data.redArtRequest.requesterName : "",
         data.blueReady,
         data.redReady
      );
   }

   private static boolean canCapture(WarfareWorldData.CapturePoint target, String team, WarfareWorldData data) {
      int currentPriority = team.equals("BLUE") ? target.bluePriority : target.redPriority;
      if (currentPriority <= 1) {
         return true;
      }

      int requiredPriority = currentPriority - 1;

      for (WarfareWorldData.CapturePoint p : data.capturePoints) {
         int pPriority = team.equals("BLUE") ? p.bluePriority : p.redPriority;
         if (pPriority == requiredPriority && p.owner.equals(team)) {
            return true;
         }
      }

      return false;
   }

   public static void leaveCurrentSquad(ServerPlayer player, WarfareWorldData data) {
      String pName = player.getScoreboardName();

      for (WarfareWorldData.Squad s : data.squads) {
         if (s.members.contains(pName)) {
            s.members.remove(pName);
            if (s.leader.equals(pName)) {
               PacketSquadAction.removeRadio(player);
               if (!s.members.isEmpty()) {
                  s.leader = s.members.get(0);
                  ServerPlayer newLeader = player.server.getPlayerList().getPlayerByName(s.leader);
                  if (newLeader != null) {
                     PacketSquadAction.updatePlayerTags(newLeader, s.id, true);
                     if ((Boolean)WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                        PacketSquadAction.giveRadio(newLeader);
                     }
                  }
               }
            }
         }
      }

      data.squads.removeIf(sx -> sx.members.isEmpty());
      data.setDirty();
      PacketHandler.sendToAllClients(player.serverLevel(), data);
      player.getPersistentData().putString("WARFARE_CurrentKit", "Unassigned");
      player.getPersistentData().putString("WARFARE_PendingKit", "");
      player.getInventory().clearContent();
      ResupplyHandler.clearCurios(player);
      player.inventoryMenu.broadcastChanges();
      player.containerMenu.broadcastChanges();
      PacketSquadAction.removePlayerTags(player);
      player.displayClientMessage(Component.literal("В§eSquad left. Kit and reservations cleared."), true);
      data.setDirty();
      PacketHandler.sendToAllClients(player.serverLevel(), data);
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (!event.getEntity().level().isClientSide) {
         ServerPlayer player = (ServerPlayer)event.getEntity();
         ServerLevel level = player.serverLevel();
         WarfareWorldData data = WarfareWorldData.get(level);
         if (data.isGameStarted) {
            return;
         }

         PacketSquadAction.leaveCurrentSquad(player, data);
         player.getPersistentData().putString("WARFARE_CurrentKit", "Unassigned");
         player.getPersistentData().remove("WARFARE_PendingKit");
         player.getInventory().clearContent();
         ResupplyHandler.clearCurios(player);
         player.inventoryMenu.broadcastChanges();
         data.setDirty();
         PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), new PacketSyncSquads(data.squads));
      }
   }

   private static void playSirenForTeam(ServerLevel level, String teamName) {
      for (ServerPlayer player : level.players()) {
         if (player.getTeam() != null && player.getTeam().getName().equalsIgnoreCase(teamName)) {
            player.playNotifySound((SoundEvent)ModSounds.SIREN_ALARM.get(), SoundSource.MASTER, 1.0F, 1.0F);
         }
      }
   }

   public static void checkSirenManual(ServerLevel level, WarfareWorldData data) {
      if (data.isGameStarted && (Boolean)WarfareConfig.LOW_TICKETS_SIREN.get()) {
         if (data.blueTickets <= 50 && data.blueTickets >= 0 && !data.playedBlueSiren) {
            playSirenForTeam(level, "Blue");
            data.playedBlueSiren = true;
            data.setDirty();
         }

         if (data.redTickets <= 50 && data.redTickets >= 0 && !data.playedRedSiren) {
            playSirenForTeam(level, "Red");
            data.playedRedSiren = true;
            data.setDirty();
         }
      }
   }

   @SubscribeEvent
   public static void enforceMortarShellLimit(PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide) {
         if (event.player.tickCount % 10 == 0) {
            Player player = event.player;
            if (!player.isCreative() && !player.isSpectator()) {
               Item mortarItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
               if (mortarItem == null || mortarItem == Items.AIR) {
                  mortarItem = Items.ARROW;
               }

               int totalCount = 0;
               Inventory inv = player.getInventory();

               for (int i = 0; i < inv.getContainerSize(); i++) {
                  ItemStack stack = inv.getItem(i);
                  if (stack.getItem() == mortarItem) {
                     totalCount += stack.getCount();
                  }
               }

               if (totalCount > 8) {
                  int toRemove = totalCount - 8;
                  int removedSoFar = 0;

                  for (int i = 0; i < inv.getContainerSize() && removedSoFar < toRemove; i++) {
                     ItemStack stack = inv.getItem(i);
                     if (stack.getItem() == mortarItem) {
                        int shrinkAmount = Math.min(stack.getCount(), toRemove - removedSoFar);
                        stack.shrink(shrinkAmount);
                        removedSoFar += shrinkAmount;
                        ItemStack dropped = new ItemStack(mortarItem, shrinkAmount);
                        player.drop(dropped, false, true);
                     }
                  }

                  player.displayClientMessage(Component.literal("You can only carry up to 8 Mortar Shells!").withStyle(ChatFormatting.RED), true);
               }
            }
         }
      }
   }

   private static void checkCmdVoteStatus(ServerLevel level, MinecraftServer server, WarfareWorldData data, String team) {
      boolean isBlue = team.equals("BLUE");
      boolean active = isBlue ? data.blueCmdVoteActive : data.redCmdVoteActive;
      String candidateName = isBlue ? data.blueCmdCandidateName : data.redCmdCandidateName;
      int candidateId = isBlue ? data.blueCmdCandidateId : data.redCmdCandidateId;
      int timer = isBlue ? data.blueCmdVoteTimer : data.redCmdVoteTimer;
      Map<UUID, Boolean> votesMap = isBlue ? data.blueCmdVotes : data.redCmdVotes;
      if (active) {
         List<ServerPlayer> otherSLs = level.players()
            .stream()
            .filter(p -> p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(team))
            .filter(p -> p.getPersistentData().getBoolean("WARFARE_IsSquadLeader"))
            .filter(p -> !p.getScoreboardName().equals(candidateName))
            .toList();
         int totalVoters = otherSLs.size();
         int needed = totalVoters > 0 ? (int)Math.ceil(totalVoters / 2.0) : 1;
         long yes = votesMap.entrySet().stream().filter(e -> server.getPlayerList().getPlayer(e.getKey()) != null).filter(Entry::getValue).count();
         long no = votesMap.entrySet().stream().filter(e -> server.getPlayerList().getPlayer(e.getKey()) != null).filter(e -> !e.getValue()).count();
         boolean win = totalVoters > 0 && yes >= needed;
         boolean fail = totalVoters > 0 && no >= totalVoters || timer <= 0;
         if (win || fail) {
            if (win) {
               if (isBlue) {
                  data.blueCMDId = candidateId;
               } else {
                  data.redCMDId = candidateId;
               }

               broadcastTeamMessage(level, team, team + " COMMANDER ASSIGNED: " + candidateName, ChatFormatting.GREEN);
            } else {
               broadcastTeamMessage(level, team, team + " CMD Application rejected or timed out.", ChatFormatting.RED);
            }

            if (isBlue) {
               data.blueCmdVoteActive = false;
               data.blueCmdVotes.clear();
            } else {
               data.redCmdVoteActive = false;
               data.redCmdVotes.clear();
            }

            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
         }
      }
   }

   private static void broadcastTeamMessage(ServerLevel level, String team, String text, ChatFormatting color) {
      Component message = Component.literal(text).withStyle(color);

      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         if (player.getTeam() != null && player.getTeam().getName().equalsIgnoreCase(team)) {
            player.sendSystemMessage(message);
         }
      }
   }
}

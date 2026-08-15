package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.RallyPointBlock;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ Р·Р°РїСЂРѕСЃР° РЅР° РІРѕР·СЂРѕР¶РґРµРЅРёРµ: РіР»Р°РІРЅР°СЏ Р±Р°Р·Р°, С‚РѕС‡РєР° СЃР±РѕСЂР° РёР»Рё FOB
// РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ РІС‹Р±РѕСЂ С‚РѕС‡РєРё РїРѕСЏРІР»РµРЅРёСЏ Рё РїСЂРёРјРµРЅРµРЅРёРµ РєРёС‚Р°
public class PacketRespawnRequest {
   private final String type;

   public PacketRespawnRequest(String type) {
      this.type = type;
   }

   public static void encode(PacketRespawnRequest msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.type);
   }

   public static PacketRespawnRequest decode(FriendlyByteBuf buf) {
      return new PacketRespawnRequest(buf.readUtf());
   }

   // РћРїСЂРµРґРµР»СЏРµС‚ С‚РѕС‡РєСѓ РІРѕР·СЂРѕР¶РґРµРЅРёСЏ РїРѕ С‚РёРїСѓ (MAIN, RALLY, HUB) Рё С‚РµР»РµРїРѕСЂС‚РёСЂСѓРµС‚ РёРіСЂРѕРєР°
   public static void handle(PacketRespawnRequest msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                   if (player.getPersistentData().getBoolean("WARFARE_IsDowned")) {
                      DownedHandler.forceGiveUp(player);
                      return;
                   }
                   // Защита от гонки/эксплойта: деплой-респавн доступен только мёртвому.
                   // Живой не должен телепортироваться со спавном и получать кит повторно.
                   if (!player.isDeadOrDying()) {
                      player.sendSystemMessage(Component.literal("Респавн доступен только после смерти!").withStyle(ChatFormatting.YELLOW));
                      return;
                   }
                   ServerLevel level = player.serverLevel();
                  WarfareWorldData data = WarfareWorldData.get(level);
                  BlockPos targetPos = null;
                  ResourceKey<Level> targetDimension = level.dimension();
                  String currentDim = level.dimension().location().toString();
                  boolean useCustomSpawn = false;
                  String team = "NEUTRAL";
                  if (player.getTeam() != null) {
                     team = player.getTeam().getName().equalsIgnoreCase("Blue") ? "BLUE" : "RED";
                  }

                  boolean isBlue = team.equals("BLUE");
                  Map<String, BlockPos> mainSpawns = isBlue ? data.blueSpawns : (team.equals("RED") ? data.redSpawns : data.neutralSpawns);
                  if (msg.type.equals("MAIN")) {
                     if (mainSpawns.containsKey(currentDim)) {
                        targetPos = mainSpawns.get(currentDim);
                        useCustomSpawn = true;
                     }
                  } else if (msg.type.equals("RALLY")) {
                     String pName = player.getScoreboardName();
                     WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                     if (mySquad != null && mySquad.rallyPos != null) {
                        if (mySquad.isRallyBlocked && !player.isCreative()) {
                            player.sendSystemMessage(Component.literal("Спавн невозможен: рали отряда ПОД УГРОЗОЙ!").withStyle(ChatFormatting.RED));
                           return;
                        }

                        String rallyDim = mySquad.rallyDimension != null ? mySquad.rallyDimension : "minecraft:overworld";
                        if (!rallyDim.equals(currentDim)) {
                            player.sendSystemMessage(Component.literal("Рали в другом измерении!").withStyle(ChatFormatting.RED));
                           return;
                        }

                        if (!level.isLoaded(mySquad.rallyPos) || !(level.getBlockState(mySquad.rallyPos).getBlock() instanceof RallyPointBlock)) {
                            player.sendSystemMessage(Component.literal("Рали уничтожено!").withStyle(ChatFormatting.RED));
                           return;
                        }

                        targetPos = findRandomSafeSpawn(level, mySquad.rallyPos, 10);
                        useCustomSpawn = true;
                        if (targetPos == null) {
                           targetPos = mainSpawns.get(currentDim);
                            player.sendSystemMessage(Component.literal("Спавн у рали заблокирован! Перенаправление на основную базу.").withStyle(ChatFormatting.YELLOW));
                        }
                     }
                  } else if (msg.type.startsWith("HUB")) {
                     String[] parts = msg.type.split(":");
                     if (parts.length == 4) {
                        try {
                           BlockPos reqPos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));

                           for (WarfareWorldData.HubInfo h : data.hubs) {
                              if (h.pos.equals(reqPos) && h.team.equalsIgnoreCase(team) && h.constructed) {
                                 int hubRadius = (Integer)WarfareConfig.HUB_BLOCK_RADIUS.get();
                                 int hubEnemiesReq = (Integer)WarfareConfig.HUB_BLOCK_ENEMY_COUNT.get();
                                 int enemyCount = GameLogicEvents.getEnemyCount(level, reqPos, team);
                                 if ((h.isBlocked || enemyCount >= hubEnemiesReq) && !player.isCreative()) {
                                     player.sendSystemMessage(Component.literal("Спавн невозможен: ФОБ ПОД УГРОЗОЙ!").withStyle(ChatFormatting.RED));
                                    return;
                                 }

                                 boolean spawnCosts = (Boolean)WarfareConfig.HUB_SPAWN_COSTS_MATERIALS.get();
                                 int spawnCost = (Integer)WarfareConfig.HUB_SPAWN_MATERIAL_COST.get();
                                 if (spawnCosts) {
                                    level.getChunkSource().getChunk(reqPos.getX() >> 4, reqPos.getZ() >> 4, true);
                                    if (!(level.getBlockEntity(reqPos) instanceof HubBlockEntity hubBe)) {
                                        player.sendSystemMessage(Component.literal("Спавн невозможен: блок ФОБ отсутствует!").withStyle(ChatFormatting.RED));
                                       return;
                                    }

                                    if (hubBe.getMaterials() < spawnCost) {
                                        player.sendSystemMessage(
                                           Component.literal("Спавн невозможен: не хватает материалов! (" + hubBe.getMaterials() + "/" + spawnCost + ")")
                                              .withStyle(ChatFormatting.RED)
                                        );
                                       return;
                                    }

                                    hubBe.consumeMaterials(spawnCost);
                                    h.materials = hubBe.getMaterials();
                                    data.setDirty();
                                    PacketHandler.sendToAllClients(level, data);
                                 }

                                 targetPos = findRandomSafeSpawn(level, reqPos, 10);
                                 useCustomSpawn = true;
                                 if (targetPos == null) {
                                    targetPos = mainSpawns.get(currentDim);
                                     player.sendSystemMessage(Component.literal("Спавн у ФОБ заблокирован! Перенаправление на основную базу.").withStyle(ChatFormatting.YELLOW));
                                 }
                                 break;
                              }
                           }
                        } catch (Exception var23) {
                        }
                     }
                  }

                    if (useCustomSpawn && targetPos != null) {
                        player.setRespawnPosition(targetDimension, targetPos, 0.0F, true, false);
                        player.teleportTo(level, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), 0.0F);
                        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                           player.setGameMode(GameType.SURVIVAL);
                        }

                        if (data.isGameStarted) {
                            String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
                            boolean isSetupAttacker = data.invasionSetupActive && !pTeam.equalsIgnoreCase(data.invasionDefender);
                            if (!isSetupAttacker) {
                               if (player.getPersistentData().contains("WARFARE_PendingKit")) {
                                  ResupplyHandler.tryApplyPendingKit(player, data);
                               } else {
                                  String currentKitName = player.getPersistentData().getString("WARFARE_CurrentKit");
                                  if (!currentKitName.isEmpty() && !currentKitName.equals("Unassigned")) {
                                     String tName = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                                     WarfareWorldData.KitInfo kit = tName.equals("BLUE") ? data.blueKits.get(currentKitName) : data.redKits.get(currentKitName);
                                     if (kit != null) {
                                        ResupplyHandler.applyKitToPlayer(player, kit);
                                     }
                                  }
                               }
                               // Кит уже выдан этим пакетом: onPlayerRespawn (PlayerRespawnEvent)
                               // не должен пересобирать инвентарь второй раз («сразу всё одевает»)
                               player.getPersistentData().putBoolean("WARFARE_KitApplied", true);
                            }
                         }
                    } else {
                      player.sendSystemMessage(Component.literal("Точка спавна сейчас недоступна!").withStyle(ChatFormatting.RED));
                  }
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }

   private static WarfareWorldData.Squad getPlayerSquad(String playerName, WarfareWorldData data) {
      for (WarfareWorldData.Squad s : data.squads) {
         if (s.members.contains(playerName)) {
            return s;
         }
      }

      return null;
   }

   // РС‰РµС‚ СЃР»СѓС‡Р°Р№РЅСѓСЋ Р±РµР·РѕРїР°СЃРЅСѓСЋ С‚РѕС‡РєСѓ РґР»СЏ РІРѕР·СЂРѕР¶РґРµРЅРёСЏ РІ СЂР°РґРёСѓСЃРµ РѕС‚ С†РµРЅС‚СЂР°
   private static BlockPos findRandomSafeSpawn(ServerLevel level, BlockPos center, int radius) {
      Random rand = new Random();

      for (int i = 0; i < 100; i++) {
         int dx = rand.nextInt(radius * 2 + 1) - radius;
         int dz = rand.nextInt(radius * 2 + 1) - radius;
         BlockPos surface = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));

         for (int dy = 1; dy >= -2; dy--) {
            BlockPos candidate = surface.offset(0, dy, 0);
            if (isValidSpawnSpot(level, candidate)) {
               return candidate;
            }
         }
      }

      return null;
   }

   // РџСЂРѕРІРµСЂСЏРµС‚, Р±РµР·РѕРїР°СЃРЅРѕ Р»Рё РјРµСЃС‚Рѕ: СЃРІРѕР±РѕРґРЅРѕРµ РїСЂРѕСЃС‚СЂР°РЅСЃС‚РІРѕ, С‚РІС‘СЂРґР°СЏ Р·РµРјР»СЏ, РЅРµС‚ Р»Р°РІС‹/РєР°РєС‚СѓСЃРѕРІ
   private static boolean isValidSpawnSpot(ServerLevel level, BlockPos pos) {
      BlockState feet = level.getBlockState(pos);
      BlockState head = level.getBlockState(pos.above());
      BlockState ground = level.getBlockState(pos.below());
      boolean spaceClear = feet.getCollisionShape(level, pos).isEmpty() && head.getCollisionShape(level, pos.above()).isEmpty();
      boolean groundSolid = ground.isFaceSturdy(level, pos.below(), Direction.UP);
      boolean notDangerous = !feet.is(Blocks.LAVA)
         && !feet.is(Blocks.FIRE)
         && !ground.is(Blocks.LAVA)
         && !feet.is(Blocks.WATER)
         && !head.is(Blocks.WATER);
      return spaceClear && groundSolid && notDangerous;
   }
}

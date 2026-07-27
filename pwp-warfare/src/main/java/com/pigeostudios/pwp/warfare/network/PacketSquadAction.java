package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ СѓРїСЂР°РІР»РµРЅРёСЏ РѕС‚СЂСЏРґРѕРј: СЃРѕР·РґР°РЅРёРµ, РІСЃС‚СѓРїР»РµРЅРёРµ, РІС‹С…РѕРґ, РєРёРє, РїРѕРІС‹С€РµРЅРёРµ, Р±Р»РѕРєРёСЂРѕРІРєР°
// РЎРѕРґРµСЂР¶РёС‚ РІСЃСЋ Р»РѕРіРёРєСѓ СЂР°Р±РѕС‚С‹ СЃ РѕС‚РґРµР»РµРЅРёСЏРјРё РёРіСЂРѕРєРѕРІ
public class PacketSquadAction {
   private final int action;
   private final int squadId;
   private final String stringData;
   private static final String[] NATO_ALPHABET = new String[]{
      "Alpha",
      "Bravo",
      "Charlie",
      "Delta",
      "Echo",
      "Foxtrot",
      "Golf",
      "Hotel",
      "India",
      "Juliett",
      "Kilo",
      "Lima",
      "Mike",
      "November",
      "Oscar",
      "Papa",
      "Quebec",
      "Romeo",
      "Sierra",
      "Tango",
      "Uniform",
      "Victor",
      "Whiskey",
      "X-ray",
      "Yankee",
      "Zulu"
   };

   public PacketSquadAction(int action, int squadId, String stringData) {
      this.action = action;
      this.squadId = squadId;
      this.stringData = stringData;
   }

   public static void encode(PacketSquadAction msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.action);
      buf.writeInt(msg.squadId);
      buf.writeUtf(msg.stringData);
   }

   public static PacketSquadAction decode(FriendlyByteBuf buf) {
      return new PacketSquadAction(buf.readInt(), buf.readInt(), buf.readUtf());
   }

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ РґРµР№СЃС‚РІРёСЏ СЃ РѕС‚СЂСЏРґРѕРј РїРѕ actionId: 0-СЃРѕР·РґР°С‚СЊ, 1-РїСЂРёСЃРѕРµРґ., 2-РІС‹Р№С‚Рё, Рё С‚.Рґ.
   public static void handle(PacketSquadAction msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
               if (player != null) {
                  WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                  String pName = player.getScoreboardName();
                  String pTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
                  if (!pTeam.equals("NEUTRAL") || player.isCreative()) {
                     if (msg.action == 0) {
                        leaveCurrentSquad(player, data);
                        String rawName = msg.stringData.trim();
                        if (rawName.length() > 12) {
                           rawName = rawName.substring(0, 12);
                        }

                        String finalName = rawName.isEmpty() ? getAvailableSquadName(data, pTeam) : rawName;
                        int newId = 0;

                        for (int i = 1; i < 1000; i++) {
                           boolean idTaken = false;

                           for (WarfareWorldData.Squad s : data.squads) {
                               if (s.id == i) {
                                  idTaken = true;
                                  break;
                               }
                            }

                            if (!idTaken) {
                               newId = i;
                               break;
                            }
                         }

                         String currentDim = player.level().dimension().location().toString();
                        WarfareWorldData.Squad newSquad = new WarfareWorldData.Squad(newId, finalName, pTeam, pName, currentDim);
                        newSquad.members.add(pName);
                        data.squads.add(newSquad);
                        updatePlayerTags(player, newId, true);
                        if ((Boolean)WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                           giveRadio(player);
                        }

                        player.sendSystemMessage(Component.literal("Squad created: " + finalName).withStyle(ChatFormatting.GOLD));
                     } else if (msg.action == 1) {
                        for (WarfareWorldData.Squad s : data.squads) {
                           if (s.id == msg.squadId && s.team.equalsIgnoreCase(pTeam)) {
                              if (s.isLocked && !player.isCreative()) {
                                 player.sendSystemMessage(Component.literal("Squad is LOCKED!").withStyle(ChatFormatting.RED));
                                 return;
                              }

                              if (s.members.size() >= 9) {
                                 player.sendSystemMessage(Component.literal("Squad is full!").withStyle(ChatFormatting.RED));
                                 return;
                              }

                              leaveCurrentSquad(player, data);
                               s.members.add(pName);
                              updatePlayerTags(player, s.id, false);
                              player.sendSystemMessage(Component.literal("Joined squad: " + s.name).withStyle(ChatFormatting.GREEN));
                              break;
                           }
                        }
                     } else if (msg.action == 2) {
                        leaveCurrentSquad(player, data);
                        player.sendSystemMessage(Component.literal("You left the squad.").withStyle(ChatFormatting.YELLOW));
                     } else if (msg.action == 3) {
                        WarfareWorldData.Squad targetSquad = null;

                        for (WarfareWorldData.Squad s : data.squads) {
                           if (s.id == msg.squadId) {
                              targetSquad = s;
                              break;
                           }
                        }

                        if (targetSquad != null) {
                           String targetName = msg.stringData;
                           ServerPlayer targetEntity = player.server.getPlayerList().getPlayerByName(targetName);
                           boolean isLeaderKicking = targetSquad.leader.equals(pName) && !targetName.equals(pName);
                           boolean isKickingOfflineLeader = targetName.equals(targetSquad.leader)
                              && targetEntity == null
                              && targetSquad.members.contains(pName);
                           if ((isLeaderKicking || isKickingOfflineLeader) && targetSquad.members.remove(targetName)) {
                              if (targetName.equals(targetSquad.leader) && !targetSquad.members.isEmpty()) {
                                 targetSquad.leader = targetSquad.members.get(0);
                                 ServerPlayer newLeader = player.server.getPlayerList().getPlayerByName(targetSquad.leader);
                                 if (newLeader != null) {
                                    updatePlayerTags(newLeader, targetSquad.id, true);
                                    if ((Boolean)WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                                       giveRadio(newLeader);
                                    }

                                    newLeader.sendSystemMessage(
                                       Component.literal("The previous leader was offline and removed. You are the new Leader!")
                                          .withStyle(ChatFormatting.GOLD)
                                    );
                                 }
                              }

                               if (targetEntity != null) {
                                  removePlayerTags(targetEntity);
                                  removeRadio(targetEntity);
                                  targetEntity.displayClientMessage(Component.literal("You were kicked!").withStyle(ChatFormatting.RED), true);
                              }

                              player.server
                                 .getPlayerList()
                                 .broadcastSystemMessage(
                                    Component.literal("Offline leader " + targetName + " was removed from squad " + targetSquad.name)
                                       .withStyle(ChatFormatting.YELLOW),
                                    false
                                 );
                           }
                        }
                     } else if (msg.action == 4) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null && mySquad.leader.equals(pName)) {
                           String targetName = msg.stringData;
                           if (mySquad.members.contains(targetName)) {
                              removeRadio(player);
                              if (!pTeam.equalsIgnoreCase("Blue")) {
                                 ;
                              }

                              mySquad.leader = targetName;
                              updatePlayerTags(player, mySquad.id, false);
                              ServerPlayer target = player.server.getPlayerList().getPlayerByName(targetName);
                              if (target != null) {
                                 updatePlayerTags(target, mySquad.id, true);
                                 if ((Boolean)WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                                    giveRadio(target);
                                 }

                                 mySquad.leader = targetName;
                                 target.displayClientMessage(Component.literal("You have been promoted to Squad Leader!").withStyle(ChatFormatting.GOLD), true);
                              }

                              player.sendSystemMessage(Component.literal("Promoted " + targetName).withStyle(ChatFormatting.GOLD));
                           }
                        }
                     } else if (msg.action == 5) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null && mySquad.leader.equals(pName)) {
                           mySquad.isLocked = !mySquad.isLocked;
                           String status = mySquad.isLocked ? "LOCKED" : "UNLOCKED";
                           ChatFormatting color = mySquad.isLocked ? ChatFormatting.RED : ChatFormatting.GREEN;
                           player.sendSystemMessage(Component.literal("Squad is now " + status).withStyle(color));
                        }
                     } else if (msg.action == 6) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.bravoLeader.equals(pName))) {
                           String target = msg.stringData;
                           if (mySquad.members.contains(target) && !mySquad.leader.equals(target) && !mySquad.charlieLeader.equals(target)) {
                              mySquad.removeFromFireteams(target);
                              mySquad.bravoLeader = target;
                              if (!mySquad.bravoMembers.contains(target)) {
                                 mySquad.bravoMembers.add(target);
                              }

                              player.sendSystemMessage(Component.literal("Assigned " + target + " as Bravo FTL").withStyle(ChatFormatting.GOLD));
                           }
                        }
                     } else if (msg.action == 7) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.charlieLeader.equals(pName))) {
                           String target = msg.stringData;
                           if (mySquad.members.contains(target) && !mySquad.leader.equals(target) && !mySquad.bravoLeader.equals(target)) {
                              mySquad.removeFromFireteams(target);
                              mySquad.charlieLeader = target;
                              if (!mySquad.charlieMembers.contains(target)) {
                                 mySquad.charlieMembers.add(target);
                              }

                              player.sendSystemMessage(Component.literal("Assigned " + target + " as Charlie FTL").withStyle(ChatFormatting.GOLD));
                           }
                        }
                     } else if (msg.action == 8) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.bravoLeader.equals(pName))) {
                           String target = msg.stringData;
                           if (mySquad.members.contains(target) && !mySquad.leader.equals(target)) {
                              mySquad.removeFromFireteams(target);
                              mySquad.bravoMembers.add(target);
                           }
                        }
                     } else if (msg.action == 9) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.charlieLeader.equals(pName))) {
                           String target = msg.stringData;
                           if (mySquad.members.contains(target) && !mySquad.leader.equals(target)) {
                              mySquad.removeFromFireteams(target);
                              mySquad.charlieMembers.add(target);
                           }
                        }
                     } else if (msg.action == 10) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null) {
                           String target = msg.stringData;
                           boolean canRemove = mySquad.leader.equals(pName)
                              || mySquad.bravoLeader.equals(pName) && mySquad.bravoMembers.contains(target)
                              || mySquad.charlieLeader.equals(pName) && mySquad.charlieMembers.contains(target);
                           if (canRemove) {
                              mySquad.removeFromFireteams(target);
                           }
                        }
                     } else if (msg.action == 11) {
                        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                        if (mySquad != null && mySquad.id == msg.squadId && mySquad.leader.equals(pName)) {
                           String squadName = mySquad.name;
                           for (String m : new ArrayList<>(mySquad.members)) {
                              mySquad.members.remove(m);
                              mySquad.removeFromFireteams(m);
                              ServerPlayer mem = player.server.getPlayerList().getPlayerByName(m);
                              if (mem != null) {
                                 removePlayerTags(mem);
                                 removeRadio(mem);
                                 mem.displayClientMessage(Component.literal("Squad disbanded by leader.").withStyle(ChatFormatting.RED), true);
                              }
                           }
                           if (mySquad.id == data.blueCMDId) data.blueCMDId = -1;
                           if (mySquad.id == data.redCMDId) data.redCMDId = -1;
                           data.squads.remove(mySquad);
                           player.sendSystemMessage(Component.literal("Squad disbanded.").withStyle(ChatFormatting.GOLD));
                        }
                     }

                     data.setDirty();
                     PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(player.level()::dimension), new PacketSyncSquads(data.squads));
                  }
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }

   public static String getAvailableSquadName(WarfareWorldData data, String teamName) {
      for (String natoName : NATO_ALPHABET) {
         boolean taken = false;

         for (WarfareWorldData.Squad s : data.squads) {
            if (s.team.equalsIgnoreCase(teamName) && s.name.equalsIgnoreCase(natoName)) {
               taken = true;
               break;
            }
         }

         if (!taken) {
            return natoName;
         }
      }

      return "Squad " + (data.squads.size() + 1);
   }

   public static WarfareWorldData.Squad getPlayerSquad(String playerName, WarfareWorldData data) {
      for (WarfareWorldData.Squad s : data.squads) {
         if (s.members.contains(playerName)) {
            return s;
         }
      }

      return null;
   }

   // Р’С‹С…РѕРґ РёРіСЂРѕРєР° РёР· С‚РµРєСѓС‰РµРіРѕ РѕС‚СЂСЏРґР°: РѕС‡РёСЃС‚РєР° РґР°РЅРЅС‹С…, РЅР°Р·РЅР°С‡РµРЅРёРµ РЅРѕРІРѕРіРѕ Р»РёРґРµСЂР°
   public static void leaveCurrentSquad(ServerPlayer player, WarfareWorldData data) {
       String pName = player.getScoreboardName();

      for (WarfareWorldData.Squad s : data.squads) {
         if (s.members.contains(pName)) {
            s.members.remove(pName);
            s.removeFromFireteams(pName);
            String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            boolean voteCancelled = false;
            if (pTeam.equals("BLUE") && data.blueCmdVoteActive && data.blueCmdCandidateName.equals(pName)) {
               data.blueCmdVoteActive = false;
               data.blueCmdVotes.clear();
               voteCancelled = true;
            } else if (pTeam.equals("RED") && data.redCmdVoteActive && data.redCmdCandidateName.equals(pName)) {
               data.redCmdVoteActive = false;
               data.redCmdVotes.clear();
               voteCancelled = true;
            }

            if (voteCancelled) {
               Component cancelMsg = Component.literal("CMD Application cancelled: " + pName + " left.").withStyle(ChatFormatting.RED);
               player.server.getPlayerList().broadcastSystemMessage(cancelMsg, false);
            }

            if (s.leader.equals(pName)) {
               removeRadio(player);
               if (!s.members.isEmpty()) {
                  s.leader = s.members.get(0);
                  ServerPlayer newLeader = player.server.getPlayerList().getPlayerByName(s.leader);
                  if (newLeader != null) {
                     updatePlayerTags(newLeader, s.id, true);
                     if ((Boolean)WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                        giveRadio(newLeader);
                     }
                  }
               } else {
                  if (s.id == data.blueCMDId) {
                     data.blueCMDId = -1;
                  }

                  if (s.id == data.redCMDId) {
                     data.redCMDId = -1;
                  }
               }
            }
            break;
         }
      }

      data.squads.removeIf(sx -> sx.members.isEmpty());
      player.getPersistentData().putString("WARFARE_CurrentKit", "Unassigned");
      player.getPersistentData().putString("WARFARE_PendingKit", "");
      player.getInventory().clearContent();
      ResupplyHandler.clearCurios(player);
      player.inventoryMenu.broadcastChanges();
      player.containerMenu.broadcastChanges();
      removePlayerTags(player);
      if (!data.isGameStarted) {
         player.displayClientMessage(Component.literal("В§eLeft squad. Pre-game kit cleared."), true);
      } else {
         player.displayClientMessage(Component.literal("В§eLeft squad. Equipment reset."), true);
      }

      data.setDirty();
      PacketHandler.sendToAllClients(player.serverLevel(), data);
   }

   // Р’С‹РґР°С‘С‚ РёРіСЂРѕРєСѓ СЂР°РґРёРѕСЃС‚Р°РЅС†РёСЋ, РµСЃР»Рё РµС‘ РµС‰С‘ РЅРµС‚ РІ РёРЅРІРµРЅС‚Р°СЂРµ
   public static void giveRadio(ServerPlayer player) {
      if (player != null) {
         boolean hasRadio = false;

         for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ModItems.SQUAD_LEADER_RADIO.get()) {
               hasRadio = true;
               break;
            }
         }

         if (!hasRadio && player.getOffhandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get()) {
            hasRadio = true;
         }

         if (!hasRadio) {
            ItemStack radioStack = new ItemStack((ItemLike)ModItems.SQUAD_LEADER_RADIO.get());
            if (!player.getInventory().add(radioStack)) {
               player.drop(radioStack, false);
            }
         }
      }
   }

   public static void removeRadio(ServerPlayer player) {
      if (player != null) {
         if ((Boolean)WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
            player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.SQUAD_LEADER_RADIO.get(), -1, player.inventoryMenu.getCraftSlots());
            player.inventoryMenu.broadcastChanges();
         }
      }
   }

   public static void updatePlayerTags(ServerPlayer p, int squadId, boolean isLeader) {
      p.getPersistentData().putInt("WARFARE_SquadID", squadId);
      p.getPersistentData().putBoolean("WARFARE_IsSquadLeader", isLeader);
   }

   public static void removePlayerTags(ServerPlayer p) {
      p.getPersistentData().remove("WARFARE_SquadID");
      p.getPersistentData().remove("WARFARE_IsSquadLeader");
   }
}

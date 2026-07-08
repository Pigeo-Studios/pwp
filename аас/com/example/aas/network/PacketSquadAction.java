/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.network;

import com.example.aas.config.AASConfig;
import com.example.aas.item.ModItems;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.voicechat.VoicechatCompat;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketSquadAction {
    private final int action;
    private final int squadId;
    private final String stringData;
    private static final String[] NATO_ALPHABET = new String[]{"Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu"};

    public PacketSquadAction(int action, int squadId, String stringData) {
        this.action = action;
        this.squadId = squadId;
        this.stringData = stringData;
    }

    public static void encode(PacketSquadAction msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.action);
        buf.writeInt(msg.squadId);
        buf.m_130070_(msg.stringData);
    }

    public static PacketSquadAction decode(FriendlyByteBuf buf) {
        return new PacketSquadAction(buf.readInt(), buf.readInt(), buf.m_130277_());
    }

    public static void handle(PacketSquadAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            AASWorldData.Squad mySquad;
            String pTeam;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.m_284548_());
            String pName = player.m_6302_();
            String string = pTeam = player.m_5647_() != null ? player.m_5647_().m_5758_() : "NEUTRAL";
            if (pTeam.equals("NEUTRAL") && !player.m_7500_()) {
                return;
            }
            if (msg.action == 0) {
                PacketSquadAction.leaveCurrentSquad(player, data);
                String rawName = msg.stringData.trim();
                if (rawName.length() > 12) {
                    rawName = rawName.substring(0, 12);
                }
                String finalName = rawName.isEmpty() ? PacketSquadAction.getAvailableSquadName(data, pTeam) : rawName;
                int newId = 0;
                for (int i = 1; i < 1000; ++i) {
                    boolean idTaken = false;
                    for (AASWorldData.Squad s : data.squads) {
                        if (s.id != i) continue;
                        idTaken = true;
                        break;
                    }
                    if (idTaken) continue;
                    newId = i;
                    break;
                }
                String currentDim = player.m_9236_().m_46472_().m_135782_().toString();
                AASWorldData.Squad newSquad = new AASWorldData.Squad(newId, finalName, pTeam, pName, currentDim);
                newSquad.members.add(pName);
                data.squads.add(newSquad);
                VoicechatCompat.createAndJoinGroup(player, newId, finalName);
                PacketSquadAction.updatePlayerTags(player, newId, true);
                if (((Boolean)AASConfig.AUTO_GIVE_SL_RADIO.get()).booleanValue()) {
                    PacketSquadAction.giveRadio(player);
                }
                player.m_213846_((Component)Component.m_237113_((String)("Squad created: " + finalName)).m_130940_(ChatFormatting.GOLD));
            } else if (msg.action == 1) {
                for (AASWorldData.Squad s : data.squads) {
                    if (s.id != msg.squadId || !s.team.equalsIgnoreCase(pTeam)) continue;
                    if (s.isLocked && !player.m_7500_()) {
                        player.m_213846_((Component)Component.m_237113_((String)"Squad is LOCKED!").m_130940_(ChatFormatting.RED));
                        return;
                    }
                    if (s.members.size() >= 9) {
                        player.m_213846_((Component)Component.m_237113_((String)"Squad is full!").m_130940_(ChatFormatting.RED));
                        return;
                    }
                    PacketSquadAction.leaveCurrentSquad(player, data);
                    s.members.add(pName);
                    VoicechatCompat.joinGroup(player, s.id);
                    PacketSquadAction.updatePlayerTags(player, s.id, false);
                    player.m_213846_((Component)Component.m_237113_((String)("Joined squad: " + s.name)).m_130940_(ChatFormatting.GREEN));
                    break;
                }
            } else if (msg.action == 2) {
                PacketSquadAction.leaveCurrentSquad(player, data);
                player.m_213846_((Component)Component.m_237113_((String)"You left the squad.").m_130940_(ChatFormatting.YELLOW));
            } else if (msg.action == 3) {
                AASWorldData.Squad targetSquad = null;
                for (AASWorldData.Squad s : data.squads) {
                    if (s.id != msg.squadId) continue;
                    targetSquad = s;
                    break;
                }
                if (targetSquad != null) {
                    boolean isKickingOfflineLeader;
                    String targetName = msg.stringData;
                    ServerPlayer targetEntity = player.f_8924_.m_6846_().m_11255_(targetName);
                    boolean isLeaderKicking = targetSquad.leader.equals(pName) && !targetName.equals(pName);
                    boolean bl = isKickingOfflineLeader = targetName.equals(targetSquad.leader) && targetEntity == null && targetSquad.members.contains(pName);
                    if ((isLeaderKicking || isKickingOfflineLeader) && targetSquad.members.remove(targetName)) {
                        if (targetName.equals(targetSquad.leader) && !targetSquad.members.isEmpty()) {
                            targetSquad.leader = targetSquad.members.get(0);
                            ServerPlayer newLeader = player.f_8924_.m_6846_().m_11255_(targetSquad.leader);
                            if (newLeader != null) {
                                PacketSquadAction.updatePlayerTags(newLeader, targetSquad.id, true);
                                if (((Boolean)AASConfig.AUTO_GIVE_SL_RADIO.get()).booleanValue()) {
                                    PacketSquadAction.giveRadio(newLeader);
                                }
                                newLeader.m_213846_((Component)Component.m_237113_((String)"The previous leader was offline and removed. You are the new Leader!").m_130940_(ChatFormatting.GOLD));
                            }
                        }
                        if (targetEntity != null) {
                            PacketSquadAction.removePlayerTags(targetEntity);
                            PacketSquadAction.removeRadio(targetEntity);
                            VoicechatCompat.leaveGroup(targetEntity);
                            targetEntity.m_5661_((Component)Component.m_237113_((String)"You were kicked!").m_130940_(ChatFormatting.RED), true);
                        }
                        player.f_8924_.m_6846_().m_240416_((Component)Component.m_237113_((String)("Offline leader " + targetName + " was removed from squad " + targetSquad.name)).m_130940_(ChatFormatting.YELLOW), false);
                    }
                }
            } else if (msg.action == 4) {
                String targetName;
                AASWorldData.Squad mySquad2 = PacketSquadAction.getPlayerSquad(pName, data);
                if (mySquad2 != null && mySquad2.leader.equals(pName) && mySquad2.members.contains(targetName = msg.stringData)) {
                    PacketSquadAction.removeRadio(player);
                    int teamCMDId = pTeam.equalsIgnoreCase("Blue") ? data.blueCMDId : data.redCMDId;
                    mySquad2.leader = targetName;
                    PacketSquadAction.updatePlayerTags(player, mySquad2.id, false);
                    ServerPlayer target = player.f_8924_.m_6846_().m_11255_(targetName);
                    if (target != null) {
                        PacketSquadAction.updatePlayerTags(target, mySquad2.id, true);
                        if (((Boolean)AASConfig.AUTO_GIVE_SL_RADIO.get()).booleanValue()) {
                            PacketSquadAction.giveRadio(target);
                        }
                        mySquad2.leader = targetName;
                        target.m_5661_((Component)Component.m_237113_((String)"You have been promoted to Squad Leader!").m_130940_(ChatFormatting.GOLD), true);
                    }
                    player.m_213846_((Component)Component.m_237113_((String)("Promoted " + targetName)).m_130940_(ChatFormatting.GOLD));
                }
            } else if (msg.action == 5) {
                AASWorldData.Squad mySquad3 = PacketSquadAction.getPlayerSquad(pName, data);
                if (mySquad3 != null && mySquad3.leader.equals(pName)) {
                    mySquad3.isLocked = !mySquad3.isLocked;
                    String status = mySquad3.isLocked ? "LOCKED" : "UNLOCKED";
                    ChatFormatting color = mySquad3.isLocked ? ChatFormatting.RED : ChatFormatting.GREEN;
                    player.m_213846_((Component)Component.m_237113_((String)("Squad is now " + status)).m_130940_(color));
                }
            } else if (msg.action == 6) {
                String target;
                AASWorldData.Squad mySquad4 = PacketSquadAction.getPlayerSquad(pName, data);
                if (mySquad4 != null && (mySquad4.leader.equals(pName) || mySquad4.bravoLeader.equals(pName)) && mySquad4.members.contains(target = msg.stringData) && !mySquad4.leader.equals(target) && !mySquad4.charlieLeader.equals(target)) {
                    mySquad4.removeFromFireteams(target);
                    mySquad4.bravoLeader = target;
                    if (!mySquad4.bravoMembers.contains(target)) {
                        mySquad4.bravoMembers.add(target);
                    }
                    player.m_213846_((Component)Component.m_237113_((String)("Assigned " + target + " as Bravo FTL")).m_130940_(ChatFormatting.GOLD));
                }
            } else if (msg.action == 7) {
                String target;
                AASWorldData.Squad mySquad5 = PacketSquadAction.getPlayerSquad(pName, data);
                if (mySquad5 != null && (mySquad5.leader.equals(pName) || mySquad5.charlieLeader.equals(pName)) && mySquad5.members.contains(target = msg.stringData) && !mySquad5.leader.equals(target) && !mySquad5.bravoLeader.equals(target)) {
                    mySquad5.removeFromFireteams(target);
                    mySquad5.charlieLeader = target;
                    if (!mySquad5.charlieMembers.contains(target)) {
                        mySquad5.charlieMembers.add(target);
                    }
                    player.m_213846_((Component)Component.m_237113_((String)("Assigned " + target + " as Charlie FTL")).m_130940_(ChatFormatting.GOLD));
                }
            } else if (msg.action == 8) {
                String target;
                AASWorldData.Squad mySquad6 = PacketSquadAction.getPlayerSquad(pName, data);
                if (mySquad6 != null && (mySquad6.leader.equals(pName) || mySquad6.bravoLeader.equals(pName)) && mySquad6.members.contains(target = msg.stringData) && !mySquad6.leader.equals(target)) {
                    mySquad6.removeFromFireteams(target);
                    mySquad6.bravoMembers.add(target);
                }
            } else if (msg.action == 9) {
                String target;
                AASWorldData.Squad mySquad7 = PacketSquadAction.getPlayerSquad(pName, data);
                if (mySquad7 != null && (mySquad7.leader.equals(pName) || mySquad7.charlieLeader.equals(pName)) && mySquad7.members.contains(target = msg.stringData) && !mySquad7.leader.equals(target)) {
                    mySquad7.removeFromFireteams(target);
                    mySquad7.charlieMembers.add(target);
                }
            } else if (msg.action == 10 && (mySquad = PacketSquadAction.getPlayerSquad(pName, data)) != null) {
                boolean canRemove;
                String target = msg.stringData;
                boolean bl = canRemove = mySquad.leader.equals(pName) || mySquad.bravoLeader.equals(pName) && mySquad.bravoMembers.contains(target) || mySquad.charlieLeader.equals(pName) && mySquad.charlieMembers.contains(target);
                if (canRemove) {
                    mySquad.removeFromFireteams(target);
                }
            }
            data.m_77762_();
            PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)player.m_9236_()).m_46472_()), (Object)new PacketSyncSquads(data.squads));
        });
        ctx.get().setPacketHandled(true);
    }

    public static String getAvailableSquadName(AASWorldData data, String teamName) {
        for (String natoName : NATO_ALPHABET) {
            boolean taken = false;
            for (AASWorldData.Squad s : data.squads) {
                if (!s.team.equalsIgnoreCase(teamName) || !s.name.equalsIgnoreCase(natoName)) continue;
                taken = true;
                break;
            }
            if (taken) continue;
            return natoName;
        }
        return "Squad " + (data.squads.size() + 1);
    }

    public static AASWorldData.Squad getPlayerSquad(String playerName, AASWorldData data) {
        for (AASWorldData.Squad s : data.squads) {
            if (!s.members.contains(playerName)) continue;
            return s;
        }
        return null;
    }

    public static void leaveCurrentSquad(ServerPlayer player, AASWorldData data) {
        String pName = player.m_6302_();
        VoicechatCompat.leaveGroup(player);
        for (AASWorldData.Squad s2 : data.squads) {
            if (!s2.members.contains(pName)) continue;
            s2.members.remove(pName);
            s2.removeFromFireteams(pName);
            String pTeam = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
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
                MutableComponent cancelMsg = Component.m_237113_((String)("CMD Application cancelled: " + pName + " left.")).m_130940_(ChatFormatting.RED);
                player.f_8924_.m_6846_().m_240416_((Component)cancelMsg, false);
            }
            if (!s2.leader.equals(pName)) break;
            PacketSquadAction.removeRadio(player);
            if (!s2.members.isEmpty()) {
                s2.leader = s2.members.get(0);
                ServerPlayer newLeader = player.f_8924_.m_6846_().m_11255_(s2.leader);
                if (newLeader == null) break;
                PacketSquadAction.updatePlayerTags(newLeader, s2.id, true);
                if (!((Boolean)AASConfig.AUTO_GIVE_SL_RADIO.get()).booleanValue()) break;
                PacketSquadAction.giveRadio(newLeader);
                break;
            }
            if (s2.id == data.blueCMDId) {
                data.blueCMDId = -1;
            }
            if (s2.id != data.redCMDId) break;
            data.redCMDId = -1;
            break;
        }
        data.squads.removeIf(s -> s.members.isEmpty());
        player.getPersistentData().m_128359_("AAS_CurrentKit", "Unassigned");
        player.getPersistentData().m_128359_("AAS_PendingKit", "");
        player.m_150109_().m_6211_();
        ResupplyHandler.clearCurios(player);
        player.f_36095_.m_38946_();
        player.f_36096_.m_38946_();
        PacketSquadAction.removePlayerTags(player);
        if (!data.isGameStarted) {
            player.m_5661_((Component)Component.m_237113_((String)"\u00a7eLeft squad. Pre-game kit cleared."), true);
        } else {
            player.m_5661_((Component)Component.m_237113_((String)"\u00a7eLeft squad. Equipment reset."), true);
        }
        data.m_77762_();
        PacketHandler.sendToAllClients(player.m_284548_(), data);
    }

    public static void giveRadio(ServerPlayer player) {
        if (player == null) {
            return;
        }
        boolean hasRadio = false;
        for (ItemStack stack : player.m_150109_().f_35974_) {
            if (stack.m_41720_() != ModItems.SQUAD_LEADER_RADIO.get()) continue;
            hasRadio = true;
            break;
        }
        if (!hasRadio && player.m_21206_().m_41720_() == ModItems.SQUAD_LEADER_RADIO.get()) {
            hasRadio = true;
        }
        if (!hasRadio) {
            ItemStack radioStack = new ItemStack((ItemLike)ModItems.SQUAD_LEADER_RADIO.get());
            if (!player.m_150109_().m_36054_(radioStack)) {
                player.m_36176_(radioStack, false);
            }
        }
    }

    public static void removeRadio(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!((Boolean)AASConfig.AUTO_GIVE_SL_RADIO.get()).booleanValue()) {
            return;
        }
        player.m_150109_().m_36022_(p -> p.m_41720_() == ModItems.SQUAD_LEADER_RADIO.get(), -1, (Container)player.f_36095_.m_39730_());
        player.f_36095_.m_38946_();
    }

    public static void updatePlayerTags(ServerPlayer p, int squadId, boolean isLeader) {
        p.getPersistentData().m_128405_("AAS_SquadID", squadId);
        p.getPersistentData().m_128379_("AAS_IsSquadLeader", isLeader);
    }

    public static void removePlayerTags(ServerPlayer p) {
        p.getPersistentData().m_128473_("AAS_SquadID");
        p.getPersistentData().m_128473_("AAS_IsSquadLeader");
    }
}


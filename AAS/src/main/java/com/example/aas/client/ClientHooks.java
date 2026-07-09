/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.Display$BlockDisplay
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.example.aas.client;

import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.ModBlocks;
import com.example.aas.block.RallyPointBlockEntity;
import com.example.aas.client.ClientData;
import com.example.aas.client.RecoilHandler;
import com.example.aas.client.gui.CrateRadialScreen;
import com.example.aas.client.gui.DownedScreen;
import com.example.aas.client.gui.HubRadialScreen;
import com.example.aas.client.gui.KitTeamSelectScreen;
import com.example.aas.client.gui.PlayerKitSelectScreen;
import com.example.aas.client.gui.RadioRadialScreen;
import com.example.aas.client.gui.VictoryScreen;
import com.example.aas.client.sound.HubLoopingSound;
import com.example.aas.client.sound.RallyLoopingSound;
import com.example.aas.network.PacketOpenPlayerKitMenu;
import com.example.aas.network.PacketSpawnGhost;
import com.example.aas.network.PacketSyncGameData;
import com.example.aas.network.PacketSyncPoint;
import com.example.aas.world.AASWorldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ClientHooks {
    public static void handleRecoil(float pitch, float yaw) {
        RecoilHandler.addRecoil(pitch);
        if (yaw != 0.0f && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.turn((double)yaw, 0.0);
        }
    }

    public static Object playRallySound(RallyPointBlockEntity entity, Object currentSound) {
        if (currentSound == null) {
            RallyLoopingSound sound = new RallyLoopingSound(entity);
            Minecraft.getInstance().getSoundManager().play((SoundInstance)sound);
            return sound;
        }
        return currentSound;
    }

    public static void stopRallySound(Object sound) {
        if (sound instanceof RallyLoopingSound) {
            RallyLoopingSound s = (RallyLoopingSound)((Object)sound);
            s.stopSound();
        }
    }

    public static Object playHubSound(HubBlockEntity entity, Object currentSound) {
        if (currentSound != null) {
            HubLoopingSound sound = (HubLoopingSound)((Object)currentSound);
            if (sound.isStopped()) {
                return ClientHooks.playHubSoundInternal(entity);
            }
            return currentSound;
        }
        return ClientHooks.playHubSoundInternal(entity);
    }

    private static Object playHubSoundInternal(HubBlockEntity entity) {
        HubLoopingSound sound = new HubLoopingSound(entity);
        Minecraft.getInstance().getSoundManager().play((SoundInstance)sound);
        return sound;
    }

    public static void stopHubSound(Object sound) {
        if (sound instanceof HubLoopingSound) {
            HubLoopingSound s = (HubLoopingSound)((Object)sound);
            s.stopSound();
        }
    }

    public static void openPlayerKitMenu(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
        Minecraft.getInstance().setScreen((Screen)new PlayerKitSelectScreen(kits));
    }

    public static void openKitTeamSelect() {
        Minecraft.getInstance().setScreen((Screen)new KitTeamSelectScreen());
    }

    public static void openRadioMenu() {
        Minecraft.getInstance().setScreen((Screen)new RadioRadialScreen());
    }

    public static void openHubMenu(BlockPos pos) {
        Minecraft.getInstance().setScreen((Screen)new HubRadialScreen(pos));
    }

    public static void openCrateMenu(int entityId) {
        Minecraft.getInstance().setScreen((Screen)new CrateRadialScreen(entityId));
    }

    public static void tryOpenRadioMenu(Player player) {
        if (player.isCreative()) {
            ClientHooks.openRadioMenu();
            return;
        }
        if (player.getTeam() == null) {
            player.displayClientMessage((Component)Component.literal((String)"You must join a TEAM (Blue/Red) first!").withStyle(ChatFormatting.RED), true);
            return;
        }
        String playerName = player.getScoreboardName();
        boolean isInSquad = false;
        boolean isLeader = false;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(playerName)) continue;
            isInSquad = true;
            if (!s.leader.equals(playerName)) break;
            isLeader = true;
            break;
        }
        if (!isInSquad) {
            player.displayClientMessage((Component)Component.literal((String)"You must join a SQUAD first! Press 'K'.").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!isLeader) {
            player.displayClientMessage((Component)Component.literal((String)"You must be a Squad Leader to use this!").withStyle(ChatFormatting.RED), true);
            return;
        }
        ClientHooks.openRadioMenu();
    }

    public static void handleSpawnGhost(PacketSpawnGhost msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Display.BlockDisplay ghost = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, (Level)level);
            if (msg.blockId == 14) {
                CompoundTag tag = new CompoundTag();
                tag.put("block_state", (Tag)NbtUtils.writeBlockState((BlockState)((Block)ModBlocks.HUB_BLOCK.get()).defaultBlockState()));
                ghost.load(tag);
            }
            ghost.setPos((double)msg.pos.getX(), (double)msg.pos.getY(), (double)msg.pos.getZ());
            level.addFreshEntity((Entity)ghost);
        }
    }

    public static void handleDownedState(int entityId, boolean isDowned, boolean died) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (isDowned) {
            ClientData.DOWNED_PLAYERS.add(entityId);
            if (mc.player != null && mc.player.getId() == entityId) {
                mc.player.getPersistentData().putBoolean("AAS_IsDowned", true);
                if (ClientData.globalDeathTimestamp == 0L) {
                    ClientData.globalDeathTimestamp = System.currentTimeMillis();
                }
                mc.setScreen((Screen)new DownedScreen());
            }
        } else {
            ClientData.DOWNED_PLAYERS.remove(entityId);
            if (mc.player != null && mc.player.getId() == entityId) {
                mc.player.getPersistentData().putBoolean("AAS_IsDowned", false);
                if (mc.screen instanceof DownedScreen) {
                    mc.setScreen(null);
                }
                if (!died) {
                    ClientData.globalDeathTimestamp = 0L;
                }
            }
        }
    }

    public static void handleSyncGameData(PacketSyncGameData msg) {
        ClientData.BLUE_TICKETS = msg.blueTickets;
        ClientData.RED_TICKETS = msg.redTickets;
        ClientData.hasBlueRally = msg.hasBlueRally;
        ClientData.hasRedRally = msg.hasRedRally;
        ClientData.blueBleeding = msg.blueBleeding;
        ClientData.redBleeding = msg.redBleeding;
        ClientData.RESPAWN_TIME = msg.respawnTime;
        ClientData.blueRallyBlocked = msg.blueBlocked;
        ClientData.redRallyBlocked = msg.redBlocked;
        ClientData.clientHubs = new ArrayList<AASWorldData.HubInfo>(msg.hubs);
        ClientData.BLUE_FACTION = msg.blueFaction;
        ClientData.RED_FACTION = msg.redFaction;
        ClientData.isGameStarted = msg.isGameStarted;
        ClientData.allCapturePoints = new ArrayList<AASWorldData.CapturePoint>(msg.capturePoints);
        ClientData.customBlueName = msg.blueCustomName;
        ClientData.customRedName = msg.redCustomName;
        ClientData.activeMarkers = new ArrayList<AASWorldData.MapMarker>(msg.activeMarkers);
        ClientData.serverHubSpawnCosts = msg.hubSpawnCosts;
        ClientData.serverHubSpawnCostAmount = msg.hubSpawnCost;
        ClientData.mapCenterX = msg.mapCenterX;
        ClientData.mapCenterZ = msg.mapCenterZ;
        ClientData.mapSizeBlocks = msg.mapSizeBlocks;
        ClientData.currentMapImage = msg.currentMapImage;
        ClientData.blueReady = msg.blueReady;
        ClientData.redReady = msg.redReady;
        ClientData.clientVehicles = new ArrayList<AASWorldData.VehicleRecord>(msg.markedVehicles);
        ClientData.blueSpawns = new HashMap<String, BlockPos>(msg.blueSpawns);
        ClientData.redSpawns = new HashMap<String, BlockPos>(msg.redSpawns);
        ClientData.neutralSpawns = new HashMap<String, BlockPos>(msg.neutralSpawns);
        ClientData.playerKits = new HashMap<String, String>(msg.playerKits);
        ClientData.voteActive = msg.voteActive;
        ClientData.voteTimer = msg.voteTimer;
        ClientData.votes = new HashMap<UUID, Boolean>(msg.votes);
        ClientData.blueCMDId = msg.blueCMDId;
        ClientData.redCMDId = msg.redCMDId;
        ClientData.blueCmdVoteActive = msg.blueCmdVoteActive;
        ClientData.blueCmdCandidateName = msg.blueCmdCandidateName;
        ClientData.blueCmdVotes = new HashMap<UUID, Boolean>(msg.blueCmdVotes);
        ClientData.redCmdVoteActive = msg.redCmdVoteActive;
        ClientData.redCmdCandidateName = msg.redCmdCandidateName;
        ClientData.redCmdVotes = new HashMap<UUID, Boolean>(msg.redCmdVotes);
        ClientData.activeStrikes = new ArrayList<AASWorldData.ActiveStrike>(msg.activeStrikes);
        ClientData.blueArtPos = msg.blueArtPos;
        ClientData.redArtPos = msg.redArtPos;
        ClientData.blueArtTimer = msg.blueArtTimer;
        ClientData.redArtTimer = msg.redArtTimer;
        ClientData.blueArtReqName = msg.blueArtReqName;
        ClientData.redArtReqName = msg.redArtReqName;
    }

    public static void handleSyncPoint(PacketSyncPoint msg) {
        ClientData.isInsidePoint = msg.isInside;
        if (msg.name.isEmpty()) {
            ClientData.pointName = "";
            return;
        }
        if (msg.isInside) {
            ClientData.pointName = msg.name;
            ClientData.pointOwner = msg.owner;
            ClientData.pointProgress = msg.progress;
            ClientData.isLocked = msg.isLocked;
            ClientData.nextObjectiveName = msg.nextObjective;
            ClientData.isContested = msg.isContested;
            ClientData.pointCapturingTeam = msg.capturingTeam;
            ClientData.pointCaptureRate = msg.captureRate;
        }
        if (ClientData.allCapturePoints != null) {
            for (AASWorldData.CapturePoint cp : ClientData.allCapturePoints) {
                if (!cp.name.equals(msg.name)) continue;
                cp.owner = msg.owner;
                cp.progress = msg.progress;
                cp.capturingTeam = msg.capturingTeam;
                break;
            }
        }
    }

    public static void openVictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner) {
        Minecraft.getInstance().setScreen((Screen)new VictoryScreen(winnerName, winnerFaction, subText, isBlueWinner));
    }
}


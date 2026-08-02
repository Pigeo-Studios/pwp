package com.pigeostudios.pwp.warfare.client;

import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.client.gui.CrateRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.DownedScreen;
import com.pigeostudios.pwp.warfare.client.gui.HubRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.FactionSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.KitTeamSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.PlayerKitSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.SkinListScreen;
import com.pigeostudios.pwp.warfare.client.gui.RadioRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.FactionVehicleSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.VictoryScreen;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.client.sound.HubLoopingSound;
import com.pigeostudios.pwp.warfare.client.sound.RallyLoopingSound;
import com.pigeostudios.pwp.warfare.client.sound.StationLoopingSound;
import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketSpawnGhost;
import com.pigeostudios.pwp.warfare.network.PacketSyncGameData;
import com.pigeostudios.pwp.warfare.network.PacketSyncPoint;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

// РҐСѓРєРё, РІС‹Р·С‹РІР°РµРјС‹Рµ РёР· СЃРµСЂРІРµСЂРЅРѕРіРѕ РєРѕРґР° РґР»СЏ РІС‹РїРѕР»РЅРµРЅРёСЏ РєР»РёРµРЅС‚СЃРєРёС… РѕРїРµСЂР°С†РёР№
// РћС‚РєСЂС‹С‚РёРµ GUI, СѓРїСЂР°РІР»РµРЅРёРµ Р·РІСѓРєР°РјРё, РѕР±СЂР°Р±РѕС‚РєР° СЃРµС‚РµРІС‹С… РїР°РєРµС‚РѕРІ
public class ClientHooks {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static void handleRecoil(float pitch, float yaw) {
      RecoilHandler.addRecoil(pitch);
      if (yaw != 0.0F && Minecraft.getInstance().player != null) {
         Minecraft.getInstance().player.turn(yaw, 0.0);
      }
   }

   public static Object playRallySound(RallyPointBlockEntity entity, Object currentSound) {
      if (currentSound == null) {
         RallyLoopingSound sound = new RallyLoopingSound(entity);
         Minecraft.getInstance().getSoundManager().play(sound);
         return sound;
      } else {
         return currentSound;
      }
   }

   public static void stopRallySound(Object sound) {
      if (sound instanceof RallyLoopingSound s) {
         s.stopSound();
      }
   }

   public static Object playHubSound(HubBlockEntity entity, Object currentSound) {
      if (currentSound != null) {
         HubLoopingSound sound = (HubLoopingSound)currentSound;
         return sound.isStopped() ? playHubSoundInternal(entity) : currentSound;
      } else {
         return playHubSoundInternal(entity);
      }
   }

   private static Object playHubSoundInternal(HubBlockEntity entity) {
      HubLoopingSound sound = new HubLoopingSound(entity);
      Minecraft.getInstance().getSoundManager().play(sound);
      return sound;
   }

   public static void stopHubSound(Object sound) {
      if (sound instanceof HubLoopingSound s) {
         s.stopSound();
      }
   }

   public static Object playStationSound(com.pigeostudios.pwp.warfare.block.VehicleStationBlockEntity entity, Object currentSound) {
      if (currentSound != null) {
         StationLoopingSound sound = (StationLoopingSound)currentSound;
         return sound.isStopped() ? playStationSoundInternal(entity) : currentSound;
      } else {
         return playStationSoundInternal(entity);
      }
   }

   private static Object playStationSoundInternal(com.pigeostudios.pwp.warfare.block.VehicleStationBlockEntity entity) {
      StationLoopingSound sound = new StationLoopingSound(entity);
      Minecraft.getInstance().getSoundManager().play(sound);
      return sound;
   }

   public static void stopStationSound(Object sound) {
      if (sound instanceof StationLoopingSound s) {
         s.stopSound();
      }
   }

    public static void openPlayerKitMenu(PacketOpenPlayerKitMenu msg) {
        List<PacketOpenPlayerKitMenu.KitDTO> kits = msg.kits;
        ClientData.availableKits = new ArrayList<>(kits);

        // Restore slot selections from server
        if (!msg.playerSelections.isEmpty()) {
            String selKit = "";
            for (var k : kits) {
                if (k.isSelected) { selKit = k.name; break; }
            }
            if (!selKit.isEmpty()) {
                DeployData.slotSelections.put(selKit, new HashMap<>(msg.playerSelections));
            }
        }

        var p = Minecraft.getInstance().player;
        if (p != null && p.isAlive() && !p.isDeadOrDying()) {
            var s = Minecraft.getInstance().screen;
            if (s instanceof com.pigeostudios.pwp.warfare.client.gui.DeployScreen ds) {
                ds.populateData();
            }
            return;
        }
        ClientData.deployRequested = true;
        Minecraft.getInstance().setScreen(new com.pigeostudios.pwp.warfare.client.gui.DeployScreen());
    }

    public static void openKitTeamSelect() {
        Minecraft.getInstance().setScreen(new KitTeamSelectScreen());
    }

    public static void openFactionKitSelect() {
        Minecraft.getInstance().setScreen(new FactionSelectScreen());
    }

    public static void openFactionVehicleSelect() {
        Minecraft.getInstance().setScreen(new FactionVehicleSelectScreen());
    }

    public static void openSkinSetup() {
       Minecraft.getInstance().setScreen(new SkinListScreen());
    }

   public static void openRadioMenu() {
      Minecraft.getInstance().setScreen(new RadioRadialScreen());
   }

   public static void openHubMenu(BlockPos pos) {
      Minecraft.getInstance().setScreen(new HubRadialScreen(pos));
   }

   public static void openCrateMenu(int entityId) {
      Minecraft.getInstance().setScreen(new CrateRadialScreen(entityId));
   }

   public static void tryOpenRadioMenu(Player player) {
      if (player.isCreative()) {
         openRadioMenu();
      } else if (player.getTeam() == null) {
         player.displayClientMessage(Component.literal("You must join a TEAM (Blue/Red) first!").withStyle(ChatFormatting.RED), true);
      } else {
         String playerName = player.getScoreboardName();
         boolean isInSquad = false;
         boolean isLeader = false;

         for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(playerName)) {
               isInSquad = true;
               if (s.leader.equals(playerName)) {
                  isLeader = true;
               }
               break;
            }
         }

         if (!isInSquad) {
            player.displayClientMessage(Component.literal("You must join a SQUAD first! Press 'K'.").withStyle(ChatFormatting.RED), true);
         } else if (!isLeader) {
            player.displayClientMessage(Component.literal("You must be a Squad Leader to use this!").withStyle(ChatFormatting.RED), true);
         } else {
            openRadioMenu();
         }
      }
   }

   public static void handleSpawnGhost(PacketSpawnGhost msg) {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         BlockDisplay ghost = new BlockDisplay(EntityType.BLOCK_DISPLAY, level);
         if (msg.blockId == 14) {
            CompoundTag tag = new CompoundTag();
            tag.put("block_state", NbtUtils.writeBlockState(((Block)ModBlocks.HUB_BLOCK.get()).defaultBlockState()));
            ghost.load(tag);
         }

         ghost.setPos(msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
         level.addFreshEntity(ghost);
      }
   }

   public static void handleDownedState(int entityId, boolean isDowned) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         if (isDowned) {
            ClientData.DOWNED_PLAYERS.add(entityId);
            if (mc.player != null && mc.player.getId() == entityId) {
               ClientData.downedTimestamp = System.currentTimeMillis();
               mc.player.getPersistentData().putBoolean("WARFARE_IsDowned", true);
               mc.setScreen(new DownedScreen());
            }
         } else {
            ClientData.DOWNED_PLAYERS.remove(entityId);
            if (mc.player != null && mc.player.getId() == entityId) {
               if (ClientData.downedTimestamp > 0L) {
                  ClientData.downedBleedoutDuration = (int)((System.currentTimeMillis() - ClientData.downedTimestamp) / 1000L);
               }
               ClientData.downedTimestamp = 0L;
               mc.player.getPersistentData().putBoolean("WARFARE_IsDowned", false);
               if (mc.screen instanceof DownedScreen) {
                  mc.setScreen(null);
               }
            }
         }
      }
   }

      public static void handleSyncGameData(PacketSyncGameData msg) {
         ClientData.BLUE_TICKETS = msg.blueTickets;
         ClientData.RED_TICKETS = msg.redTickets;
         ClientData.BLUE_PLAYER_COUNT = msg.bluePlayerCount;
         ClientData.RED_PLAYER_COUNT = msg.redPlayerCount;
        ClientData.hasBlueRally = msg.hasBlueRally;
        ClientData.hasRedRally = msg.hasRedRally;
        ClientData.blueBleeding = msg.blueBleeding;
        ClientData.redBleeding = msg.redBleeding;
        ClientData.RESPAWN_TIME = msg.respawnTime;
        ClientData.blueRallyBlocked = msg.blueBlocked;
        ClientData.redRallyBlocked = msg.redBlocked;
        ClientData.clientHubs = new ArrayList<>(msg.hubs);
        ClientData.clientvehicleStations = new ArrayList<>(msg.vehicleStations);
        ClientData.BLUE_FACTION = msg.blueFaction;
        ClientData.RED_FACTION = msg.redFaction;
        if (msg.isGameStarted && ClientData.matchStartTime == 0L) ClientData.matchStartTime = System.currentTimeMillis();
        if (!msg.isGameStarted) ClientData.matchStartTime = 0L;
        ClientData.isGameStarted = msg.isGameStarted;
        ClientData.invasionSetupActive = msg.invasionSetupActive;
        ClientData.invasionSetupTimer = msg.invasionSetupTimer;
        ClientData.invasionDefender = msg.invasionDefender;
        ClientData.allCapturePoints = new ArrayList<>(msg.capturePoints);
        ClientData.customBlueName = msg.blueCustomName;
        ClientData.customRedName = msg.redCustomName;
        ClientData.activeMarkers = new ArrayList<>(msg.activeMarkers);
        ClientData.serverHubSpawnCosts = msg.hubSpawnCosts;
        ClientData.serverHubSpawnCostAmount = msg.hubSpawnCost;
        ClientData.mapCenterX = msg.mapCenterX;
        ClientData.mapCenterZ = msg.mapCenterZ;
        ClientData.mapSizeBlocks = msg.mapSizeBlocks;
        ClientData.currentMapImage = msg.currentMapImage;
        ClientData.gameMode = msg.gameMode;
        ClientData.blueReady = msg.blueReady;
        ClientData.redReady = msg.redReady;
        ClientData.clientVehicles = new ArrayList<>(msg.markedVehicles);
        ClientData.blueSpawns = new HashMap<>(msg.blueSpawns);
        ClientData.redSpawns = new HashMap<>(msg.redSpawns);
        ClientData.neutralSpawns = new HashMap<>(msg.neutralSpawns);
        ClientData.playerKits = new HashMap<>(msg.playerKits);
        ClientData.voteActive = msg.voteActive;
        ClientData.voteTimer = msg.voteTimer;
        ClientData.votes = new HashMap<>(msg.votes);
        ClientData.blueCMDId = msg.blueCMDId;
        ClientData.redCMDId = msg.redCMDId;
        ClientData.blueCmdVoteActive = msg.blueCmdVoteActive;
        ClientData.blueCmdCandidateName = msg.blueCmdCandidateName;
        ClientData.blueCmdVotes = new HashMap<>(msg.blueCmdVotes);
        ClientData.redCmdVoteActive = msg.redCmdVoteActive;
        ClientData.redCmdCandidateName = msg.redCmdCandidateName;
        ClientData.redCmdVotes = new HashMap<>(msg.redCmdVotes);
        ClientData.activeStrikes = new ArrayList<>(msg.activeStrikes);
        ClientData.blueArtPos = msg.blueArtPos;
        ClientData.redArtPos = msg.redArtPos;
        ClientData.blueArtTimer = msg.blueArtTimer;
        ClientData.redArtTimer = msg.redArtTimer;
        ClientData.blueArtReqName = msg.blueArtReqName;
        ClientData.redArtReqName = msg.redArtReqName;
        updatePlayerSkin();

        // Discord RPC лаунчера: статус «в бою против кого» / «в лобби»
        var mcPl = Minecraft.getInstance().player;
        String pTeam = mcPl != null && mcPl.getTeam() != null ? mcPl.getTeam().getName() : "";
        String pNick = mcPl != null ? mcPl.getScoreboardName() : "";
        LauncherStatusReporter.onSyncGameData(msg.isGameStarted, msg.blueFaction, msg.redFaction, pTeam, pNick);

        // Auto-open team selection screen if player has no team
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
           String teamName = mc.player.getTeam() != null ? mc.player.getTeam().getName() : "";
           if (teamName.equalsIgnoreCase("Blue") || teamName.equalsIgnoreCase("Red")) {
              ClientData.teamSelectSent = false;
           } else if (!ClientData.teamSelectSent || System.currentTimeMillis() - ClientData.teamSelectSentTime > 5000L) {
              ClientData.teamSelectSent = false;
              mc.setScreen(new com.pigeostudios.pwp.warfare.client.gui.TeamSelectionScreen());
           }
        }
     }

     public static void updatePlayerSkin() {
        ClientSkinManager.applyAllSkins();
     }

   public static void handleSyncPoint(PacketSyncPoint msg) {
      ClientData.isInsidePoint = msg.isInside;
      if (msg.name.isEmpty()) {
         ClientData.pointName = "";
      } else {
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
            for (WarfareWorldData.CapturePoint cp : ClientData.allCapturePoints) {
               if (cp.name.equals(msg.name)) {
                  cp.owner = msg.owner;
                  cp.progress = msg.progress;
                  cp.capturingTeam = msg.capturingTeam;
                  break;
               }
            }
         }
      }
   }

    public static void openVictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner,
                                          int matchKills, int matchDeaths,
                                          int matchVehicleKills, int matchVehiclesDestroyed,
                                          int matchAirVehiclesDestroyed, int matchCaptures,
                                          int matchRevives, int matchHeadshots, int matchScore,
                                          int matchDurationSec) {
       Minecraft.getInstance().setScreen(new VictoryScreen(winnerName, winnerFaction, subText, isBlueWinner,
          matchKills, matchDeaths,
          matchVehicleKills, matchVehiclesDestroyed,
          matchAirVehiclesDestroyed, matchCaptures,
          matchRevives, matchHeadshots, matchScore, matchDurationSec));
    }

    public static void openSkinInventory() {
       Minecraft.getInstance().setScreen(new com.pwp.cosmetics.gui.SkinInventoryScreen());
    }
}

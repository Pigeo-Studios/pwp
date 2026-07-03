package com.pigeostudios.pwp.warfare.client;

import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.client.gui.CrateRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.DownedScreen;
import com.pigeostudios.pwp.warfare.client.gui.HubRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.KitTeamSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.PlayerKitSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.SkinListScreen;
import com.pigeostudios.pwp.warfare.client.gui.RadioRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.VictoryScreen;
import com.pigeostudios.pwp.warfare.client.sound.HubLoopingSound;
import com.pigeostudios.pwp.warfare.client.sound.RallyLoopingSound;
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
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.lang.reflect.Field;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.Team;
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

   public static void openPlayerKitMenu(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
      Minecraft.getInstance().setScreen(new PlayerKitSelectScreen(kits));
   }

    public static void openKitTeamSelect() {
       Minecraft.getInstance().setScreen(new KitTeamSelectScreen());
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
               mc.player.getPersistentData().putBoolean("WARFARE_IsDowned", true);
               mc.setScreen(new DownedScreen());
            }
         } else {
            ClientData.DOWNED_PLAYERS.remove(entityId);
            if (mc.player != null && mc.player.getId() == entityId) {
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
       ClientData.hasBlueRally = msg.hasBlueRally;
       ClientData.hasRedRally = msg.hasRedRally;
       ClientData.blueBleeding = msg.blueBleeding;
       ClientData.redBleeding = msg.redBleeding;
       ClientData.RESPAWN_TIME = msg.respawnTime;
       ClientData.blueRallyBlocked = msg.blueBlocked;
       ClientData.redRallyBlocked = msg.redBlocked;
       ClientData.clientHubs = new ArrayList<>(msg.hubs);
       ClientData.BLUE_FACTION = msg.blueFaction;
       ClientData.RED_FACTION = msg.redFaction;
       ClientData.isGameStarted = msg.isGameStarted;
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
    }

    public static void updatePlayerSkin() {
       Minecraft mc = Minecraft.getInstance();
       if (mc.player == null) return;
       AbstractClientPlayer player = (AbstractClientPlayer) mc.player;

       Team team = player.getTeam();
       if (team == null) return;

       String faction;
       if (team.getName().equalsIgnoreCase("Blue")) {
          faction = ClientData.BLUE_FACTION;
       } else if (team.getName().equalsIgnoreCase("Red")) {
          faction = ClientData.RED_FACTION;
       } else {
          return;
       }

       if (faction == null || faction.equals("none")) return;

       String kit = ClientData.playerKits.getOrDefault(player.getScoreboardName(), "Unassigned");
       String kitFileName = "base";
       if (!kit.equals("Unassigned") && !kit.isEmpty()) {
          kitFileName = kit.toLowerCase().replace(" ", "_").replace("-", "_");
       }

       ResourceLocation tex = new ResourceLocation("pwpwarfare", "textures/skins/" + faction.toLowerCase() + "/" + kitFileName + ".png");

       try {
          PlayerInfo info = getPlayerInfoField(player);
          if (info == null) {
             LOGGER.warn("updatePlayerSkin: playerInfo field not found on AbstractClientPlayer");
             return;
          }
          Map<MinecraftProfileTexture.Type, ResourceLocation> texMap = getTexturesField(info);
          if (texMap == null) {
             LOGGER.warn("updatePlayerSkin: textures field not found on PlayerInfo");
             return;
          }
          texMap.put(MinecraftProfileTexture.Type.SKIN, tex);
          LOGGER.info("updatePlayerSkin: set skin to {}", tex);
       } catch (Exception e) {
          LOGGER.warn("updatePlayerSkin: unexpected error", e);
       }
    }

    private static PlayerInfo getPlayerInfoField(AbstractClientPlayer player) {
       try {
          Field f = AbstractClientPlayer.class.getDeclaredField("playerInfo");
          f.setAccessible(true);
          PlayerInfo info = (PlayerInfo) f.get(player);
          LOGGER.info("getPlayerInfoField: found via 'playerInfo'");
          return info;
       } catch (Exception e1) {
          LOGGER.warn("getPlayerInfoField: 'playerInfo' not found, trying SRG...");
          try {
             Field f = AbstractClientPlayer.class.getDeclaredField("f_108546_");
             f.setAccessible(true);
             PlayerInfo info = (PlayerInfo) f.get(player);
             LOGGER.info("getPlayerInfoField: found via 'f_108546_'");
             return info;
          } catch (Exception e2) {
             LOGGER.error("getPlayerInfoField: both 'playerInfo' and 'f_108546_' failed", e2);
             return null;
          }
       }
    }

    private static Map<MinecraftProfileTexture.Type, ResourceLocation> getTexturesField(PlayerInfo info) {
       try {
          Field f = PlayerInfo.class.getDeclaredField("textures");
          f.setAccessible(true);
          Map<MinecraftProfileTexture.Type, ResourceLocation> map = (Map<MinecraftProfileTexture.Type, ResourceLocation>) f.get(info);
          LOGGER.info("getTexturesField: found via 'textures'");
          return map;
       } catch (Exception e1) {
          LOGGER.warn("getTexturesField: 'textures' not found, trying SRG...");
          try {
             Field f = PlayerInfo.class.getDeclaredField("f_105299_");
             f.setAccessible(true);
             Map<MinecraftProfileTexture.Type, ResourceLocation> map = (Map<MinecraftProfileTexture.Type, ResourceLocation>) f.get(info);
             LOGGER.info("getTexturesField: found via 'f_105299_'");
             return map;
          } catch (Exception e2) {
             LOGGER.error("getTexturesField: both 'textures' and 'f_105299_' failed", e2);
             return null;
          }
       }
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

    public static void openVictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner) {
       Minecraft.getInstance().setScreen(new VictoryScreen(winnerName, winnerFaction, subText, isBlueWinner));
    }

    public static void openSkinInventory() {
       Minecraft.getInstance().setScreen(new com.pwp.cosmetics.gui.SkinInventoryScreen());
    }
}

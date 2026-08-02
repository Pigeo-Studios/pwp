package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.scores.Team;

// Р¦РµРЅС‚СЂР°Р»СЊРЅС‹Р№ СЂРµРіРёСЃС‚СЂР°С‚РѕСЂ СЃРµС‚РµРІС‹С… РїР°РєРµС‚РѕРІ РјРѕРґР°
// РЎРѕРґРµСЂР¶РёС‚ РєР°РЅР°Р» СЃРІСЏР·Рё Рё РјРµС‚РѕРґС‹ РѕС‚РїСЂР°РІРєРё РґР°РЅРЅС‹С… РІСЃРµРј РєР»РёРµРЅС‚Р°Рј
public class PacketHandler {
   private static final String PROTOCOL_VERSION = "1";
   public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation("pwpwarfare:main"), () -> "1", "1"::equals, "1"::equals);

   // Р РµРіРёСЃС‚СЂРёСЂСѓРµС‚ РІСЃРµ РїР°РєРµС‚С‹ РјРѕРґР° СЃ РёС… РєРѕРґРµРєР°РјРё Рё РѕР±СЂР°Р±РѕС‚С‡РёРєР°РјРё
   public static void register() {
      int id = 0;
      INSTANCE.registerMessage(id++, PacketSyncGameData.class, PacketSyncGameData::encode, PacketSyncGameData::decode, PacketSyncGameData::handle);
      INSTANCE.registerMessage(id++, PacketSyncPoint.class, PacketSyncPoint::encode, PacketSyncPoint::decode, PacketSyncPoint::handle);
      INSTANCE.registerMessage(id++, PacketDebugFill.class, PacketDebugFill::encode, PacketDebugFill::decode, PacketDebugFill::handle);
      INSTANCE.registerMessage(id++, PacketApplyMarker.class, PacketApplyMarker::encode, PacketApplyMarker::decode, PacketApplyMarker::handle);
      INSTANCE.registerMessage(id++, PacketRespawnRequest.class, PacketRespawnRequest::encode, PacketRespawnRequest::decode, PacketRespawnRequest::handle);
      INSTANCE.registerMessage(id++, PacketDebugSpawnRally.class, PacketDebugSpawnRally::encode, PacketDebugSpawnRally::decode, PacketDebugSpawnRally::handle);
      INSTANCE.registerMessage(id++, PacketSpawnGhost.class, PacketSpawnGhost::encode, PacketSpawnGhost::decode, PacketSpawnGhost::handle);
      INSTANCE.registerMessage(id++, PacketBuildRequest.class, PacketBuildRequest::encode, PacketBuildRequest::decode, PacketBuildRequest::handle);
      INSTANCE.registerMessage(id++, PacketToggleAim.class, PacketToggleAim::encode, PacketToggleAim::decode, PacketToggleAim::handle);
      INSTANCE.registerMessage(id++, PacketVehicleShoot.class, PacketVehicleShoot::encode, PacketVehicleShoot::decode, PacketVehicleShoot::handle);
      INSTANCE.registerMessage(id++, PacketRequestAmmo.class, PacketRequestAmmo::encode, PacketRequestAmmo::decode, PacketRequestAmmo::handle);
      INSTANCE.registerMessage(id++, PacketDropCrate.class, PacketDropCrate::encode, PacketDropCrate::decode, PacketDropCrate::handle);
      INSTANCE.registerMessage(id++, PacketUpdateSpawner.class, PacketUpdateSpawner::encode, PacketUpdateSpawner::decode, PacketUpdateSpawner::handle);
      INSTANCE.registerMessage(id++, PacketRecoil.class, PacketRecoil::encode, PacketRecoil::decode, PacketRecoil::handle);
      INSTANCE.registerMessage(id++, PacketSyncSquads.class, PacketSyncSquads::encode, PacketSyncSquads::new, PacketSyncSquads::handle);
      INSTANCE.registerMessage(id++, PacketSquadAction.class, PacketSquadAction::encode, PacketSquadAction::decode, PacketSquadAction::handle);
      INSTANCE.registerMessage(id++, PacketTeamSelect.class, PacketTeamSelect::encode, PacketTeamSelect::decode, PacketTeamSelect::handle);
      INSTANCE.registerMessage(id++, PacketSquadChat.class, PacketSquadChat::encode, PacketSquadChat::decode, PacketSquadChat::handle);
      INSTANCE.registerMessage(id++, PacketSyncMapPlayers.class, PacketSyncMapPlayers::encode, PacketSyncMapPlayers::decode, PacketSyncMapPlayers::handle);
      INSTANCE.registerMessage(id++, PacketOpenKitEditor.class, PacketOpenKitEditor::encode, PacketOpenKitEditor::decode, PacketOpenKitEditor::handle);
      INSTANCE.registerMessage(id++, PacketSaveKit.class, PacketSaveKit::encode, PacketSaveKit::decode, PacketSaveKit::handle);
      INSTANCE.registerMessage(id++, PacketRequestKitMenu.class, PacketRequestKitMenu::encode, PacketRequestKitMenu::decode, PacketRequestKitMenu::handle);
      INSTANCE.registerMessage(
         id++, PacketOpenPlayerKitMenu.class, PacketOpenPlayerKitMenu::encode, PacketOpenPlayerKitMenu::decode, PacketOpenPlayerKitMenu::handle
      );
      INSTANCE.registerMessage(id++, PacketSelectKit.class, PacketSelectKit::encode, PacketSelectKit::decode, PacketSelectKit::handle);
      INSTANCE.registerMessage(
         id++, PacketRequestCrateAmmo.class, PacketRequestCrateAmmo::encode, PacketRequestCrateAmmo::decode, PacketRequestCrateAmmo::handle
      );
      INSTANCE.registerMessage(id++, PacketRequestKitData.class, PacketRequestKitData::encode, PacketRequestKitData::decode, PacketRequestKitData::handle);
      INSTANCE.registerMessage(id++, PacketSendKitData.class, PacketSendKitData::encode, PacketSendKitData::decode, PacketSendKitData::handle);
      INSTANCE.registerMessage(id++, PacketPasteKit.class, PacketPasteKit::encode, PacketPasteKit::decode, PacketPasteKit::handle);
      INSTANCE.registerMessage(id++, PacketPasteTeam.class, PacketPasteTeam::encode, PacketPasteTeam::decode, PacketPasteTeam::handle);
      INSTANCE.registerMessage(id++, PacketDownedAction.class, PacketDownedAction::encode, PacketDownedAction::decode, PacketDownedAction::handle);
      INSTANCE.registerMessage(id++, PacketSyncDownedState.class, PacketSyncDownedState::encode, PacketSyncDownedState::decode, PacketSyncDownedState::handle);
      INSTANCE.registerMessage(id++, PacketSquadMarker.class, PacketSquadMarker::encode, PacketSquadMarker::decode, PacketSquadMarker::handle);
      INSTANCE.registerMessage(id++, PacketPlaceMapMarker.class, PacketPlaceMapMarker::encode, PacketPlaceMapMarker::decode, PacketPlaceMapMarker::handle);
      INSTANCE.registerMessage(id++, PacketRadioAction.class, PacketRadioAction::encode, PacketRadioAction::decode, PacketRadioAction::handle);
      INSTANCE.registerMessage(id++, PacketVoteAction.class, PacketVoteAction::encode, PacketVoteAction::decode, PacketVoteAction::handle);
      INSTANCE.registerMessage(
         id++, PacketRequestVehicleAmmo.class, PacketRequestVehicleAmmo::encode, PacketRequestVehicleAmmo::decode, PacketRequestVehicleAmmo::handle
      );
      INSTANCE.registerMessage(
         id++, PacketCaptureNotification.class, PacketCaptureNotification::encode, PacketCaptureNotification::decode, PacketCaptureNotification::handle
      );
      INSTANCE.registerMessage(id++, PacketPlacePing.class, PacketPlacePing::encode, PacketPlacePing::decode, PacketPlacePing::handle);
      INSTANCE.registerMessage(id++, PacketRequestCMD.class, PacketRequestCMD::encode, PacketRequestCMD::decode, PacketRequestCMD::handle);
      INSTANCE.registerMessage(id++, PacketCMDVote.class, PacketCMDVote::encode, PacketCMDVote::decode, PacketCMDVote::handle);
      INSTANCE.registerMessage(
         id++, PacketConfirmArtStrike.class, PacketConfirmArtStrike::encode, PacketConfirmArtStrike::decode, PacketConfirmArtStrike::handle
      );
      INSTANCE.registerMessage(id++, PacketSyncMyKit.class, PacketSyncMyKit::encode, PacketSyncMyKit::decode, PacketSyncMyKit::handle);
      INSTANCE.registerMessage(id++, PacketOpenFactionKitEditor.class, PacketOpenFactionKitEditor::encode, PacketOpenFactionKitEditor::decode, PacketOpenFactionKitEditor::handle);
      INSTANCE.registerMessage(id++, PacketSaveFactionKit.class, PacketSaveFactionKit::encode, PacketSaveFactionKit::decode, PacketSaveFactionKit::handle);
      INSTANCE.registerMessage(id++, PacketSaveFactionVehicle.class, PacketSaveFactionVehicle::encode, PacketSaveFactionVehicle::decode, PacketSaveFactionVehicle::handle);
      INSTANCE.registerMessage(id++, PacketVoiceActivity.class, PacketVoiceActivity::encode, PacketVoiceActivity::decode, PacketVoiceActivity::handle);
      INSTANCE.registerMessage(
         id++, PacketRadioVoiceActivity.class, PacketRadioVoiceActivity::encode, PacketRadioVoiceActivity::decode, PacketRadioVoiceActivity::handle
      );
      INSTANCE.registerMessage(
         id++, PacketVoiceChannelState.class, PacketVoiceChannelState::encode, PacketVoiceChannelState::decode, PacketVoiceChannelState::handle
      );
      INSTANCE.registerMessage(
         id++, PacketOpenVictoryScreen.class, PacketOpenVictoryScreen::encode, PacketOpenVictoryScreen::decode, PacketOpenVictoryScreen::handle
      );
      INSTANCE.registerMessage(
         id++, PacketOpenSkinInventory.class, PacketOpenSkinInventory::encode, PacketOpenSkinInventory::decode, PacketOpenSkinInventory::handle
      );
      INSTANCE.registerMessage(
         id++, PacketSyncPlayerSkin.class, PacketSyncPlayerSkin::encode, PacketSyncPlayerSkin::decode, PacketSyncPlayerSkin::handle
      );
      INSTANCE.registerMessage(id++, PacketRequestData.class, PacketRequestData::encode, PacketRequestData::decode, PacketRequestData::handle);
      INSTANCE.registerMessage(id++, PacketSendData.class, PacketSendData::encode, PacketSendData::decode, PacketSendData::handle);
      INSTANCE.registerMessage(id++, PacketApiAction.class, PacketApiAction::encode, PacketApiAction::decode, PacketApiAction::handle);
      INSTANCE.registerMessage(
         id++, PacketVehicleDriveRequest.class, PacketVehicleDriveRequest::encode, PacketVehicleDriveRequest::decode, PacketVehicleDriveRequest::handle
      );
      INSTANCE.registerMessage(
         id++, PacketVehicleDriveAnswer.class, PacketVehicleDriveAnswer::encode, PacketVehicleDriveAnswer::decode, PacketVehicleDriveAnswer::handle
      );
      INSTANCE.registerMessage(id++, PacketPlaceMarker.class, PacketPlaceMarker::encode, PacketPlaceMarker::decode, PacketPlaceMarker::handle);
      INSTANCE.registerMessage(id++, PacketSyncMarker.class, PacketSyncMarker::encode, PacketSyncMarker::decode, PacketSyncMarker::handle);
        INSTANCE.registerMessage(id++, PacketRemoveMarker.class, PacketRemoveMarker::encode, PacketRemoveMarker::decode, PacketRemoveMarker::handle);
        INSTANCE.registerMessage(id++, PacketSyncSpawners.class, PacketSyncSpawners::encode, PacketSyncSpawners::decode, PacketSyncSpawners::handle);
        INSTANCE.registerMessage(id++, PacketPlacePath.class, PacketPlacePath::encode, PacketPlacePath::decode, PacketPlacePath::handle);
        INSTANCE.registerMessage(id++, PacketSyncPath.class, PacketSyncPath::encode, PacketSyncPath::decode, PacketSyncPath::handle);
        INSTANCE.registerMessage(id++, PacketRemovePath.class, PacketRemovePath::encode, PacketRemovePath::decode, PacketRemovePath::handle);
        INSTANCE.registerMessage(id++, PacketForceCMD.class, PacketForceCMD::encode, PacketForceCMD::decode, PacketForceCMD::handle);
     }

   private static String getFactionName(String currentFaction, boolean isBlue) {
      if (currentFaction != null && !currentFaction.equals("none") && !currentFaction.equals("bluefor") && !currentFaction.equals("redfor")) {
         return currentFaction.toUpperCase();
      } else {
         return isBlue ? (String)WarfareConfig.BLUE_TEAM_CUSTOM_NAME.get() : (String)WarfareConfig.RED_TEAM_CUSTOM_NAME.get();
      }
   }

   private static Map<String, String> getPlayerKitsMap() {
      Map<String, String> pKits = new HashMap<>();
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String current = p.getPersistentData().getString("WARFARE_CurrentKit");
            String pending = p.getPersistentData().getString("WARFARE_PendingKit");
            String displayKit = !pending.isEmpty() ? pending : current;
            pKits.put(p.getScoreboardName(), displayKit != null && !displayKit.isEmpty() && !displayKit.equals("Unassigned") ? displayKit : "Unassigned");
         }
      }

      return pKits;
   }

   // РћС‚РїСЂР°РІР»СЏРµС‚ РїРѕР»РЅС‹Р№ РїР°РєРµС‚ SyncGameData РІСЃРµРј РєР»РёРµРЅС‚Р°Рј (РїРµСЂРµРіСЂСѓР·РєР° СЃ РїСЂСЏРјС‹РјРё РїР°СЂР°РјРµС‚СЂР°РјРё)
   public static void sendToAllClients(
      int blue,
      int red,
      boolean hasBlue,
      boolean hasRed,
      boolean blueBleed,
      boolean redBleed,
      int respawnTime,
      boolean blueBlocked,
      boolean redBlocked,
      List<WarfareWorldData.HubInfo> hubs,
      String bFac,
      String rFac
   ) {
      String bName = getFactionName(bFac, true);
      String rName = getFactionName(rFac, false);
      int blueCount = 0, redCount = 0;
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String t = p.getTeam() != null ? p.getTeam().getName() : "";
            if (t.equalsIgnoreCase("Blue")) blueCount++;
            else if (t.equalsIgnoreCase("Red")) redCount++;
         }
      }
      INSTANCE.send(
         PacketDistributor.ALL.noArg(),
         new PacketSyncGameData(
            blue,
            red,
            blueCount,
            redCount,
            hasBlue,
            hasRed,
            blueBleed,
            redBleed,
            respawnTime,
            blueBlocked,
            redBlocked,
            (Boolean)WarfareConfig.HUB_SPAWN_COSTS_MATERIALS.get(),
            (Integer)WarfareConfig.HUB_SPAWN_MATERIAL_COST.get(),
            0,
            0,
            2048,
            "map1",
             new ArrayList<>(),
             hubs,
             new ArrayList<>(),
             bFac,
            rFac,
             bName,
             rName,
             "aas",
             false,
             false,
             0,
             "RED",
            new ArrayList<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            getPlayerKitsMap(),
            new ArrayList<>(),
            false,
            0,
            new HashMap<>(),
            -1,
            -1,
            false,
            "",
            -1,
            0,
            new HashMap<>(),
            false,
            "",
            -1,
            0,
            new HashMap<>(),
            new ArrayList<>(),
            BlockPos.ZERO,
            BlockPos.ZERO,
            0,
            0,
            "",
            "",
            false,
            false
         )
      );
   }

   // РћС‚РїСЂР°РІР»СЏРµС‚ РїР°РєРµС‚ SyncGameData РІСЃРµРј РєР»РёРµРЅС‚Р°Рј (РїРµСЂРµРіСЂСѓР·РєР° СЃ WarfareWorldData)
   public static void sendToAllClients(WarfareWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
      INSTANCE.send(PacketDistributor.ALL.noArg(), createSyncPacket(data, blueBleed, redBleed, bBlocked, rBlocked));
   }

   // РћС‚РїСЂР°РІР»СЏРµС‚ РїР°РєРµС‚ SyncGameData РІСЃРµРј РёРіСЂРѕРєР°Рј РІ СѓРєР°Р·Р°РЅРЅРѕРј РёР·РјРµСЂРµРЅРёРё
   public static void sendToAllClients(ServerLevel level, WarfareWorldData data) {
      INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), createSyncPacket(data, false, false, false, false));
   }

   public static void broadcastPlayerSkin(ServerPlayer targetPlayer) {
      if (targetPlayer == null) return;
      UUID uuid = targetPlayer.getUUID();
      String faction = "none";
      Team team = targetPlayer.getTeam();
      if (team != null) {
         String tName = team.getName();
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
               WarfareWorldData data = WarfareWorldData.get(level);
               if (tName.equalsIgnoreCase("Blue")) {
                  faction = data.blueFaction;
                  break;
               } else if (tName.equalsIgnoreCase("Red")) {
                  faction = data.redFaction;
                  break;
               }
            }
         }
      }
      String kit = targetPlayer.getPersistentData().getString("WARFARE_CurrentKit");
      if (kit.isEmpty()) kit = "Unassigned";
      PacketSyncPlayerSkin packet = new PacketSyncPlayerSkin(uuid, faction, kit);
      INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
   }

   private static PacketSyncGameData createSyncPacket(WarfareWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
      boolean hasBlue = !data.blueRallies.isEmpty();
      boolean hasRed = !data.redRallies.isEmpty();
      String bName = getFactionName(data.blueFaction, true);
      String rName = getFactionName(data.redFaction, false);
      int blueCount = 0, redCount = 0;
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String t = p.getTeam() != null ? p.getTeam().getName() : "";
            if (t.equalsIgnoreCase("Blue")) blueCount++;
            else if (t.equalsIgnoreCase("Red")) redCount++;
         }
      }
      return new PacketSyncGameData(
         data.blueTickets,
         data.redTickets,
         blueCount,
         redCount,
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
         data.mainSupplies,
          data.blueFaction,
          data.redFaction,
          bName,
          rName,
           data.gameMode != null ? data.gameMode : "aas",
           data.isGameStarted,
           data.invasionSetupActive,
           data.invasionSetupTimer,
           data.invasionDefender != null ? data.invasionDefender : "RED",
          data.capturePoints,
         data.blueSpawns,
         data.redSpawns,
         data.neutralSpawns,
         getPlayerKitsMap(),
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
}

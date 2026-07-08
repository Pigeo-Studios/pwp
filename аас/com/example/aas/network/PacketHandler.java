/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkRegistry
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.network.simple.SimpleChannel
 *  net.minecraftforge.server.ServerLifecycleHooks
 */
package com.example.aas.network;

import com.example.aas.config.AASConfig;
import com.example.aas.network.PacketApplyMarker;
import com.example.aas.network.PacketBuildRequest;
import com.example.aas.network.PacketCMDVote;
import com.example.aas.network.PacketCaptureNotification;
import com.example.aas.network.PacketConfirmArtStrike;
import com.example.aas.network.PacketDebugFill;
import com.example.aas.network.PacketDebugSpawnRally;
import com.example.aas.network.PacketDownedAction;
import com.example.aas.network.PacketDropCrate;
import com.example.aas.network.PacketOpenKitEditor;
import com.example.aas.network.PacketOpenPlayerKitMenu;
import com.example.aas.network.PacketOpenVictoryScreen;
import com.example.aas.network.PacketPasteKit;
import com.example.aas.network.PacketPasteTeam;
import com.example.aas.network.PacketPlaceMapMarker;
import com.example.aas.network.PacketPlacePing;
import com.example.aas.network.PacketRadioAction;
import com.example.aas.network.PacketRadioVoiceActivity;
import com.example.aas.network.PacketRecoil;
import com.example.aas.network.PacketRequestAmmo;
import com.example.aas.network.PacketRequestCMD;
import com.example.aas.network.PacketRequestCrateAmmo;
import com.example.aas.network.PacketRequestKitData;
import com.example.aas.network.PacketRequestKitMenu;
import com.example.aas.network.PacketRequestVehicleAmmo;
import com.example.aas.network.PacketRespawnRequest;
import com.example.aas.network.PacketSaveKit;
import com.example.aas.network.PacketSelectKit;
import com.example.aas.network.PacketSendKitData;
import com.example.aas.network.PacketSpawnGhost;
import com.example.aas.network.PacketSquadAction;
import com.example.aas.network.PacketSquadChat;
import com.example.aas.network.PacketSquadMarker;
import com.example.aas.network.PacketSyncDownedState;
import com.example.aas.network.PacketSyncGameData;
import com.example.aas.network.PacketSyncMapPlayers;
import com.example.aas.network.PacketSyncMyKit;
import com.example.aas.network.PacketSyncPoint;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.network.PacketTeamSelect;
import com.example.aas.network.PacketToggleAim;
import com.example.aas.network.PacketUpdateSpawner;
import com.example.aas.network.PacketVehicleShoot;
import com.example.aas.network.PacketVoiceActivity;
import com.example.aas.network.PacketVoteAction;
import com.example.aas.world.AASWorldData;
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

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel((ResourceLocation)new ResourceLocation("aas:main"), () -> "1", "1"::equals, "1"::equals);

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
        INSTANCE.registerMessage(id++, PacketOpenPlayerKitMenu.class, PacketOpenPlayerKitMenu::encode, PacketOpenPlayerKitMenu::decode, PacketOpenPlayerKitMenu::handle);
        INSTANCE.registerMessage(id++, PacketSelectKit.class, PacketSelectKit::encode, PacketSelectKit::decode, PacketSelectKit::handle);
        INSTANCE.registerMessage(id++, PacketRequestCrateAmmo.class, PacketRequestCrateAmmo::encode, PacketRequestCrateAmmo::decode, PacketRequestCrateAmmo::handle);
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
        INSTANCE.registerMessage(id++, PacketRequestVehicleAmmo.class, PacketRequestVehicleAmmo::encode, PacketRequestVehicleAmmo::decode, PacketRequestVehicleAmmo::handle);
        INSTANCE.registerMessage(id++, PacketCaptureNotification.class, PacketCaptureNotification::encode, PacketCaptureNotification::decode, PacketCaptureNotification::handle);
        INSTANCE.registerMessage(id++, PacketPlacePing.class, PacketPlacePing::encode, PacketPlacePing::decode, PacketPlacePing::handle);
        INSTANCE.registerMessage(id++, PacketRequestCMD.class, PacketRequestCMD::encode, PacketRequestCMD::decode, PacketRequestCMD::handle);
        INSTANCE.registerMessage(id++, PacketCMDVote.class, PacketCMDVote::encode, PacketCMDVote::decode, PacketCMDVote::handle);
        INSTANCE.registerMessage(id++, PacketConfirmArtStrike.class, PacketConfirmArtStrike::encode, PacketConfirmArtStrike::decode, PacketConfirmArtStrike::handle);
        INSTANCE.registerMessage(id++, PacketSyncMyKit.class, PacketSyncMyKit::encode, PacketSyncMyKit::decode, PacketSyncMyKit::handle);
        INSTANCE.registerMessage(id++, PacketVoiceActivity.class, PacketVoiceActivity::encode, PacketVoiceActivity::decode, PacketVoiceActivity::handle);
        INSTANCE.registerMessage(id++, PacketRadioVoiceActivity.class, PacketRadioVoiceActivity::encode, PacketRadioVoiceActivity::decode, PacketRadioVoiceActivity::handle);
        INSTANCE.registerMessage(id++, PacketOpenVictoryScreen.class, PacketOpenVictoryScreen::encode, PacketOpenVictoryScreen::decode, PacketOpenVictoryScreen::handle);
    }

    private static String getFactionName(String currentFaction, boolean isBlue) {
        if (currentFaction == null || currentFaction.equals("none") || currentFaction.equals("bluefor") || currentFaction.equals("redfor")) {
            return isBlue ? (String)AASConfig.BLUE_TEAM_CUSTOM_NAME.get() : (String)AASConfig.RED_TEAM_CUSTOM_NAME.get();
        }
        return currentFaction.toUpperCase();
    }

    private static Map<String, String> getPlayerKitsMap() {
        HashMap<String, String> pKits = new HashMap<String, String>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer p : server.m_6846_().m_11314_()) {
                String current = p.getPersistentData().m_128461_("AAS_CurrentKit");
                String pending = p.getPersistentData().m_128461_("AAS_PendingKit");
                String displayKit = !pending.isEmpty() ? pending : current;
                pKits.put(p.m_6302_(), displayKit == null || displayKit.isEmpty() || displayKit.equals("Unassigned") ? "Unassigned" : displayKit);
            }
        }
        return pKits;
    }

    public static void sendToAllClients(int blue, int red, boolean hasBlue, boolean hasRed, boolean blueBleed, boolean redBleed, int respawnTime, boolean blueBlocked, boolean redBlocked, List<AASWorldData.HubInfo> hubs, String bFac, String rFac) {
        String bName = PacketHandler.getFactionName(bFac, true);
        String rName = PacketHandler.getFactionName(rFac, false);
        INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncGameData(blue, red, hasBlue, hasRed, blueBleed, redBleed, respawnTime, blueBlocked, redBlocked, (Boolean)AASConfig.HUB_SPAWN_COSTS_MATERIALS.get(), (Integer)AASConfig.HUB_SPAWN_MATERIAL_COST.get(), 0, 0, 2048, "map1", new ArrayList<AASWorldData.VehicleRecord>(), hubs, bFac, rFac, bName, rName, false, new ArrayList<AASWorldData.CapturePoint>(), new HashMap<String, BlockPos>(), new HashMap<String, BlockPos>(), new HashMap<String, BlockPos>(), PacketHandler.getPlayerKitsMap(), new ArrayList<AASWorldData.MapMarker>(), false, 0, new HashMap<UUID, Boolean>(), -1, -1, false, "", -1, 0, new HashMap<UUID, Boolean>(), false, "", -1, 0, new HashMap<UUID, Boolean>(), new ArrayList<AASWorldData.ActiveStrike>(), BlockPos.f_121853_, BlockPos.f_121853_, 0, 0, "", "", false, false));
    }

    public static void sendToAllClients(AASWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)PacketHandler.createSyncPacket(data, blueBleed, redBleed, bBlocked, rBlocked));
    }

    public static void sendToAllClients(ServerLevel level, AASWorldData data) {
        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).m_46472_()), (Object)PacketHandler.createSyncPacket(data, false, false, false, false));
    }

    private static PacketSyncGameData createSyncPacket(AASWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
        boolean hasBlue = !data.blueRallies.isEmpty();
        boolean hasRed = !data.redRallies.isEmpty();
        String bName = PacketHandler.getFactionName(data.blueFaction, true);
        String rName = PacketHandler.getFactionName(data.redFaction, false);
        return new PacketSyncGameData(data.blueTickets, data.redTickets, hasBlue, hasRed, blueBleed, redBleed, data.respawnTimer, bBlocked, rBlocked, (Boolean)AASConfig.HUB_SPAWN_COSTS_MATERIALS.get(), (Integer)AASConfig.HUB_SPAWN_MATERIAL_COST.get(), data.mapCenterX, data.mapCenterZ, data.mapSizeBlocks, data.currentMapImage, data.markedVehicles, data.hubs, data.blueFaction, data.redFaction, bName, rName, data.isGameStarted, data.capturePoints, data.blueSpawns, data.redSpawns, data.neutralSpawns, PacketHandler.getPlayerKitsMap(), data.activeMarkers, data.voteActive, data.voteTimer, data.votes, data.blueCMDId, data.redCMDId, data.blueCmdVoteActive, data.blueCmdCandidateName, data.blueCmdCandidateId, data.blueCmdVoteTimer, data.blueCmdVotes, data.redCmdVoteActive, data.redCmdCandidateName, data.redCmdCandidateId, data.redCmdVoteTimer, data.redCmdVotes, data.activeStrikes, data.blueArtRequest != null ? data.blueArtRequest.pos : BlockPos.f_121853_, data.redArtRequest != null ? data.redArtRequest.pos : BlockPos.f_121853_, data.blueArtRequest != null ? data.blueArtRequest.timer : 0, data.redArtRequest != null ? data.redArtRequest.timer : 0, data.blueArtRequest != null ? data.blueArtRequest.requesterName : "", data.redArtRequest != null ? data.redArtRequest.requesterName : "", data.blueReady, data.redReady);
    }
}


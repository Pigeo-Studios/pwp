/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.client.ClientHooks;
import com.example.aas.world.AASWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncGameData {
    public final int blueTickets;
    public final int redTickets;
    public final boolean hasBlueRally;
    public final boolean hasRedRally;
    public final boolean blueBleeding;
    public final boolean redBleeding;
    public final int respawnTime;
    public final boolean blueBlocked;
    public final boolean redBlocked;
    public final boolean hubSpawnCosts;
    public final int hubSpawnCost;
    public final boolean blueReady;
    public final boolean redReady;
    public final int mapCenterX;
    public final int mapCenterZ;
    public final int mapSizeBlocks;
    public final String currentMapImage;
    public final List<AASWorldData.VehicleRecord> markedVehicles;
    public final List<AASWorldData.HubInfo> hubs;
    public final String blueFaction;
    public final String redFaction;
    public final String blueCustomName;
    public final String redCustomName;
    public final boolean isGameStarted;
    public final List<AASWorldData.CapturePoint> capturePoints;
    public final Map<String, BlockPos> blueSpawns;
    public final Map<String, BlockPos> redSpawns;
    public final Map<String, BlockPos> neutralSpawns;
    public final Map<String, String> playerKits;
    public final List<AASWorldData.MapMarker> activeMarkers;
    public final boolean voteActive;
    public final int voteTimer;
    public final Map<UUID, Boolean> votes;
    public final int blueCMDId;
    public final int redCMDId;
    public final boolean blueCmdVoteActive;
    public final String blueCmdCandidateName;
    public final int blueCmdCandidateId;
    public final int blueCmdVoteTimer;
    public final Map<UUID, Boolean> blueCmdVotes;
    public final boolean redCmdVoteActive;
    public final String redCmdCandidateName;
    public final int redCmdCandidateId;
    public final int redCmdVoteTimer;
    public final Map<UUID, Boolean> redCmdVotes;
    public final List<AASWorldData.ActiveStrike> activeStrikes;
    public final BlockPos blueArtPos;
    public final BlockPos redArtPos;
    public final int blueArtTimer;
    public final int redArtTimer;
    public final String blueArtReqName;
    public final String redArtReqName;

    public PacketSyncGameData(int blueTickets, int redTickets, boolean hasBlueRally, boolean hasRedRally, boolean blueBleeding, boolean redBleeding, int respawnTime, boolean blueBlocked, boolean redBlocked, boolean hubSpawnCosts, int hubSpawnCost, int mapCenterX, int mapCenterZ, int mapSizeBlocks, String currentMapImage, List<AASWorldData.VehicleRecord> markedVehicles, List<AASWorldData.HubInfo> hubs, String blueFaction, String redFaction, String blueCustomName, String redCustomName, boolean isGameStarted, List<AASWorldData.CapturePoint> capturePoints, Map<String, BlockPos> blueSpawns, Map<String, BlockPos> redSpawns, Map<String, BlockPos> neutralSpawns, Map<String, String> playerKits, List<AASWorldData.MapMarker> activeMarkers, boolean voteActive, int voteTimer, Map<UUID, Boolean> votes, int blueCMDId, int redCMDId, boolean bCmdActive, String bCmdName, int bCmdId, int bCmdTimer, Map<UUID, Boolean> bCmdVotes, boolean rCmdActive, String rCmdName, int rCmdId, int rCmdTimer, Map<UUID, Boolean> rCmdVotes, List<AASWorldData.ActiveStrike> activeStrikes, BlockPos blueArtPos, BlockPos redArtPos, int blueArtTimer, int redArtTimer, String blueArtReqName, String redArtReqName, boolean blueReady, boolean redReady) {
        this.blueTickets = blueTickets;
        this.redTickets = redTickets;
        this.hasBlueRally = hasBlueRally;
        this.hasRedRally = hasRedRally;
        this.blueBleeding = blueBleeding;
        this.redBleeding = redBleeding;
        this.respawnTime = respawnTime;
        this.blueBlocked = blueBlocked;
        this.redBlocked = redBlocked;
        this.hubSpawnCosts = hubSpawnCosts;
        this.hubSpawnCost = hubSpawnCost;
        this.mapCenterX = mapCenterX;
        this.mapCenterZ = mapCenterZ;
        this.mapSizeBlocks = mapSizeBlocks;
        this.currentMapImage = currentMapImage;
        this.markedVehicles = markedVehicles;
        this.hubs = hubs;
        this.blueFaction = blueFaction;
        this.redFaction = redFaction;
        this.blueCustomName = blueCustomName;
        this.redCustomName = redCustomName;
        this.isGameStarted = isGameStarted;
        this.capturePoints = capturePoints;
        this.blueSpawns = blueSpawns;
        this.redSpawns = redSpawns;
        this.neutralSpawns = neutralSpawns;
        this.playerKits = playerKits;
        this.activeMarkers = activeMarkers;
        this.voteActive = voteActive;
        this.voteTimer = voteTimer;
        this.votes = votes;
        this.blueCMDId = blueCMDId;
        this.redCMDId = redCMDId;
        this.blueCmdVoteActive = bCmdActive;
        this.blueCmdCandidateName = bCmdName;
        this.blueCmdCandidateId = bCmdId;
        this.blueCmdVoteTimer = bCmdTimer;
        this.blueCmdVotes = bCmdVotes;
        this.redCmdVoteActive = rCmdActive;
        this.redCmdCandidateName = rCmdName;
        this.redCmdCandidateId = rCmdId;
        this.redCmdVoteTimer = rCmdTimer;
        this.redCmdVotes = rCmdVotes;
        this.activeStrikes = activeStrikes;
        this.blueArtPos = blueArtPos;
        this.redArtPos = redArtPos;
        this.blueArtTimer = blueArtTimer;
        this.redArtTimer = redArtTimer;
        this.blueArtReqName = blueArtReqName;
        this.redArtReqName = redArtReqName;
        this.blueReady = blueReady;
        this.redReady = redReady;
    }

    public static void encode(PacketSyncGameData msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.blueTickets);
        buf.writeInt(msg.redTickets);
        buf.writeBoolean(msg.hasBlueRally);
        buf.writeBoolean(msg.hasRedRally);
        buf.writeBoolean(msg.blueBleeding);
        buf.writeBoolean(msg.redBleeding);
        buf.writeInt(msg.respawnTime);
        buf.writeBoolean(msg.blueBlocked);
        buf.writeBoolean(msg.redBlocked);
        buf.writeBoolean(msg.hubSpawnCosts);
        buf.writeInt(msg.hubSpawnCost);
        buf.writeInt(msg.mapCenterX);
        buf.writeInt(msg.mapCenterZ);
        buf.writeInt(msg.mapSizeBlocks);
        buf.m_130070_(msg.currentMapImage);
        buf.m_236828_(msg.markedVehicles, (b, v) -> {
            b.m_130077_(v.uuid);
            b.m_130070_(v.team);
            b.m_130070_(v.type);
            b.writeDouble(v.x);
            b.writeDouble(v.y);
            b.writeDouble(v.z);
            b.writeFloat(v.yaw);
        });
        buf.m_236828_(msg.hubs, (b, h) -> {
            b.m_130064_(h.pos);
            b.m_130070_(h.team);
            b.writeBoolean(h.constructed);
            b.m_130070_(h.dimension);
            b.writeBoolean(h.isBlocked);
            b.writeInt(h.materials);
        });
        buf.m_130070_(msg.blueFaction);
        buf.m_130070_(msg.redFaction);
        buf.m_130070_(msg.blueCustomName);
        buf.m_130070_(msg.redCustomName);
        buf.writeBoolean(msg.isGameStarted);
        CompoundTag pointsTag = new CompoundTag();
        ListTag list = new ListTag();
        for (AASWorldData.CapturePoint cp : msg.capturePoints) {
            list.add((Object)cp.save());
        }
        pointsTag.m_128365_("Points", (Tag)list);
        buf.m_130079_(pointsTag);
        buf.m_236831_(msg.blueSpawns, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130064_);
        buf.m_236831_(msg.redSpawns, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130064_);
        buf.m_236831_(msg.neutralSpawns, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130064_);
        buf.m_236831_(msg.playerKits, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130070_);
        buf.m_236828_(msg.activeMarkers, (b, m) -> {
            b.m_130064_(m.pos);
            b.m_130070_(m.type);
            b.m_130070_(m.team);
            b.writeLong(m.expiryTick);
        });
        buf.writeBoolean(msg.voteActive);
        buf.writeInt(msg.voteTimer);
        buf.m_236831_(msg.votes, FriendlyByteBuf::m_130077_, FriendlyByteBuf::writeBoolean);
        buf.writeInt(msg.blueCMDId);
        buf.writeInt(msg.redCMDId);
        buf.writeBoolean(msg.blueCmdVoteActive);
        buf.m_130070_(msg.blueCmdCandidateName);
        buf.writeInt(msg.blueCmdCandidateId);
        buf.writeInt(msg.blueCmdVoteTimer);
        buf.m_236831_(msg.blueCmdVotes, FriendlyByteBuf::m_130077_, FriendlyByteBuf::writeBoolean);
        buf.writeBoolean(msg.redCmdVoteActive);
        buf.m_130070_(msg.redCmdCandidateName);
        buf.writeInt(msg.redCmdCandidateId);
        buf.writeInt(msg.redCmdVoteTimer);
        buf.m_236831_(msg.redCmdVotes, FriendlyByteBuf::m_130077_, FriendlyByteBuf::writeBoolean);
        buf.m_236828_(msg.activeStrikes, (b, s) -> {
            b.m_130064_(s.pos);
            b.m_130070_(s.team);
            b.writeInt(s.stage);
            b.writeInt(s.ticksLeft);
        });
        buf.m_130064_(msg.blueArtPos);
        buf.m_130064_(msg.redArtPos);
        buf.writeInt(msg.blueArtTimer);
        buf.writeInt(msg.redArtTimer);
        buf.m_130070_(msg.blueArtReqName);
        buf.m_130070_(msg.redArtReqName);
        buf.writeBoolean(msg.blueReady);
        buf.writeBoolean(msg.redReady);
    }

    public static PacketSyncGameData decode(FriendlyByteBuf buf) {
        int bT = buf.readInt();
        int rT = buf.readInt();
        boolean hBR = buf.readBoolean();
        boolean hRR = buf.readBoolean();
        boolean bBl = buf.readBoolean();
        boolean rBl = buf.readBoolean();
        int rTime = buf.readInt();
        boolean bB = buf.readBoolean();
        boolean rB = buf.readBoolean();
        boolean hsc = buf.readBoolean();
        int hsca = buf.readInt();
        int mCX = buf.readInt();
        int mCZ = buf.readInt();
        int mSB = buf.readInt();
        String cMI = buf.m_130277_();
        List vL = buf.m_236845_(b -> new AASWorldData.VehicleRecord(b.m_130259_(), b.m_130277_(), b.m_130277_(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), null));
        List hL = buf.m_236845_(b -> {
            AASWorldData.HubInfo h = new AASWorldData.HubInfo(b.m_130135_(), b.m_130277_(), b.readBoolean(), b.m_130277_());
            h.isBlocked = b.readBoolean();
            h.materials = b.readInt();
            return h;
        });
        String bF = buf.m_130277_();
        String rF = buf.m_130277_();
        String bCN = buf.m_130277_();
        String rCN = buf.m_130277_();
        boolean started = buf.readBoolean();
        ArrayList<AASWorldData.CapturePoint> pL = new ArrayList<AASWorldData.CapturePoint>();
        CompoundTag pTag = buf.m_130260_();
        if (pTag != null && pTag.m_128441_("Points")) {
            ListTag list = pTag.m_128437_("Points", 10);
            for (int i = 0; i < list.size(); ++i) {
                pL.add(AASWorldData.CapturePoint.load(list.m_128728_(i)));
            }
        }
        Map bSp = buf.m_236847_(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130135_);
        Map rSp = buf.m_236847_(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130135_);
        Map nSp = buf.m_236847_(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130135_);
        Map pK = buf.m_236847_(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130277_);
        List aM = buf.m_236845_(b -> new AASWorldData.MapMarker(b.m_130135_(), b.m_130277_(), b.m_130277_(), b.readLong()));
        boolean vActive = buf.readBoolean();
        int vTimer = buf.readInt();
        Map vMap = buf.m_236847_(FriendlyByteBuf::m_130259_, FriendlyByteBuf::readBoolean);
        int bCMDId = buf.readInt();
        int rCMDId = buf.readInt();
        boolean bCA = buf.readBoolean();
        String bCNam = buf.m_130277_();
        int bCId = buf.readInt();
        int bCTim = buf.readInt();
        Map bCVM = buf.m_236847_(FriendlyByteBuf::m_130259_, FriendlyByteBuf::readBoolean);
        boolean rCA = buf.readBoolean();
        String rCNam = buf.m_130277_();
        int rCId = buf.readInt();
        int rCTim = buf.readInt();
        Map rCVM = buf.m_236847_(FriendlyByteBuf::m_130259_, FriendlyByteBuf::readBoolean);
        List aStrikes = buf.m_236845_(b -> {
            AASWorldData.ActiveStrike s = new AASWorldData.ActiveStrike(b.m_130135_(), b.m_130277_());
            s.stage = b.readInt();
            s.ticksLeft = b.readInt();
            return s;
        });
        BlockPos bAP = buf.m_130135_();
        BlockPos rAP = buf.m_130135_();
        int bAT = buf.readInt();
        int rAT = buf.readInt();
        String bARN = buf.m_130277_();
        String rARN = buf.m_130277_();
        boolean bReady = buf.readBoolean();
        boolean rReady = buf.readBoolean();
        return new PacketSyncGameData(bT, rT, hBR, hRR, bBl, rBl, rTime, bB, rB, hsc, hsca, mCX, mCZ, mSB, cMI, vL, hL, bF, rF, bCN, rCN, started, pL, bSp, rSp, nSp, pK, aM, vActive, vTimer, vMap, bCMDId, rCMDId, bCA, bCNam, bCId, bCTim, bCVM, rCA, rCNam, rCId, rCTim, rCVM, aStrikes, bAP, rAP, bAT, rAT, bARN, rARN, bReady, rReady);
    }

    public static void handle(PacketSyncGameData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.handleSyncGameData(msg)));
        ctx.get().setPacketHandled(true);
    }
}


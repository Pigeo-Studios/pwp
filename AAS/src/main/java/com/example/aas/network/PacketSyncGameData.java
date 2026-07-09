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
    final public int blueTickets;
    final public int redTickets;
    final public boolean hasBlueRally;
    final public boolean hasRedRally;
    final public boolean blueBleeding;
    final public boolean redBleeding;
    final public int respawnTime;
    final public boolean blueBlocked;
    final public boolean redBlocked;
    final public boolean hubSpawnCosts;
    final public int hubSpawnCost;
    final public boolean blueReady;
    final public boolean redReady;
    final public int mapCenterX;
    final public int mapCenterZ;
    final public int mapSizeBlocks;
    final public String currentMapImage;
    final public List<AASWorldData.VehicleRecord> markedVehicles;
    final public List<AASWorldData.HubInfo> hubs;
    final public String blueFaction;
    final public String redFaction;
    final public String blueCustomName;
    final public String redCustomName;
    final public boolean isGameStarted;
    final public List<AASWorldData.CapturePoint> capturePoints;
    final public Map<String, BlockPos> blueSpawns;
    final public Map<String, BlockPos> redSpawns;
    final public Map<String, BlockPos> neutralSpawns;
    final public Map<String, String> playerKits;
    final public List<AASWorldData.MapMarker> activeMarkers;
    final public boolean voteActive;
    final public int voteTimer;
    final public Map<UUID, Boolean> votes;
    final public int blueCMDId;
    final public int redCMDId;
    final public boolean blueCmdVoteActive;
    final public String blueCmdCandidateName;
    final public int blueCmdCandidateId;
    final public int blueCmdVoteTimer;
    final public Map<UUID, Boolean> blueCmdVotes;
    final public boolean redCmdVoteActive;
    final public String redCmdCandidateName;
    final public int redCmdCandidateId;
    final public int redCmdVoteTimer;
    final public Map<UUID, Boolean> redCmdVotes;
    final public List<AASWorldData.ActiveStrike> activeStrikes;
    final public BlockPos blueArtPos;
    final public BlockPos redArtPos;
    final public int blueArtTimer;
    final public int redArtTimer;
    final public String blueArtReqName;
    final public String redArtReqName;

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
        buf.writeUtf(msg.currentMapImage);
        buf.writeCollection(msg.markedVehicles, (b, v) -> {
            b.writeUUID(v.uuid);
            b.writeUtf(v.team);
            b.writeUtf(v.type);
            b.writeDouble(v.x);
            b.writeDouble(v.y);
            b.writeDouble(v.z);
            b.writeFloat(v.yaw);
        });
        buf.writeCollection(msg.hubs, (b, h) -> {
            b.writeBlockPos(h.pos);
            b.writeUtf(h.team);
            b.writeBoolean(h.constructed);
            b.writeUtf(h.dimension);
            b.writeBoolean(h.isBlocked);
            b.writeInt(h.materials);
        });
        buf.writeUtf(msg.blueFaction);
        buf.writeUtf(msg.redFaction);
        buf.writeUtf(msg.blueCustomName);
        buf.writeUtf(msg.redCustomName);
        buf.writeBoolean(msg.isGameStarted);
        CompoundTag pointsTag = new CompoundTag();
        ListTag list = new ListTag();
        for (AASWorldData.CapturePoint cp : msg.capturePoints) {
            list.add((Object)cp.save());
        }
        pointsTag.put("Points", (Tag)list);
        buf.writeNbt(pointsTag);
        buf.writeMap(msg.blueSpawns, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130064_);
        buf.writeMap(msg.redSpawns, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130064_);
        buf.writeMap(msg.neutralSpawns, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130064_);
        buf.writeMap(msg.playerKits, FriendlyByteBuf::m_130070_, FriendlyByteBuf::m_130070_);
        buf.writeCollection(msg.activeMarkers, (b, m) -> {
            b.writeBlockPos(m.pos);
            b.writeUtf(m.type);
            b.writeUtf(m.team);
            b.writeLong(m.expiryTick);
        });
        buf.writeBoolean(msg.voteActive);
        buf.writeInt(msg.voteTimer);
        buf.writeMap(msg.votes, FriendlyByteBuf::m_130077_, FriendlyByteBuf::writeBoolean);
        buf.writeInt(msg.blueCMDId);
        buf.writeInt(msg.redCMDId);
        buf.writeBoolean(msg.blueCmdVoteActive);
        buf.writeUtf(msg.blueCmdCandidateName);
        buf.writeInt(msg.blueCmdCandidateId);
        buf.writeInt(msg.blueCmdVoteTimer);
        buf.writeMap(msg.blueCmdVotes, FriendlyByteBuf::m_130077_, FriendlyByteBuf::writeBoolean);
        buf.writeBoolean(msg.redCmdVoteActive);
        buf.writeUtf(msg.redCmdCandidateName);
        buf.writeInt(msg.redCmdCandidateId);
        buf.writeInt(msg.redCmdVoteTimer);
        buf.writeMap(msg.redCmdVotes, FriendlyByteBuf::m_130077_, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(msg.activeStrikes, (b, s) -> {
            b.writeBlockPos(s.pos);
            b.writeUtf(s.team);
            b.writeInt(s.stage);
            b.writeInt(s.ticksLeft);
        });
        buf.writeBlockPos(msg.blueArtPos);
        buf.writeBlockPos(msg.redArtPos);
        buf.writeInt(msg.blueArtTimer);
        buf.writeInt(msg.redArtTimer);
        buf.writeUtf(msg.blueArtReqName);
        buf.writeUtf(msg.redArtReqName);
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
        String cMI = buf.readUtf();
        List vL = buf.readList(b -> new AASWorldData.VehicleRecord(b.readUUID(), b.readUtf(), b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), null));
        List hL = buf.readList(b -> {
            AASWorldData.HubInfo h = new AASWorldData.HubInfo(b.readBlockPos(), b.readUtf(), b.readBoolean(), b.readUtf());
            h.isBlocked = b.readBoolean();
            h.materials = b.readInt();
            return h;
        });
        String bF = buf.readUtf();
        String rF = buf.readUtf();
        String bCN = buf.readUtf();
        String rCN = buf.readUtf();
        boolean started = buf.readBoolean();
        ArrayList<AASWorldData.CapturePoint> pL = new ArrayList<AASWorldData.CapturePoint>();
        CompoundTag pTag = buf.readNbt();
        if (pTag != null && pTag.contains("Points")) {
            ListTag list = pTag.getList("Points", 10);
            for (int i = 0; i < list.size(); ++i) {
                pL.add(AASWorldData.CapturePoint.load(list.getCompound(i)));
            }
        }
        Map bSp = buf.readMap(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130135_);
        Map rSp = buf.readMap(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130135_);
        Map nSp = buf.readMap(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130135_);
        Map pK = buf.readMap(FriendlyByteBuf::m_130277_, FriendlyByteBuf::m_130277_);
        List aM = buf.readList(b -> new AASWorldData.MapMarker(b.readBlockPos(), b.readUtf(), b.readUtf(), b.readLong()));
        boolean vActive = buf.readBoolean();
        int vTimer = buf.readInt();
        Map vMap = buf.readMap(FriendlyByteBuf::m_130259_, FriendlyByteBuf::readBoolean);
        int bCMDId = buf.readInt();
        int rCMDId = buf.readInt();
        boolean bCA = buf.readBoolean();
        String bCNam = buf.readUtf();
        int bCId = buf.readInt();
        int bCTim = buf.readInt();
        Map bCVM = buf.readMap(FriendlyByteBuf::m_130259_, FriendlyByteBuf::readBoolean);
        boolean rCA = buf.readBoolean();
        String rCNam = buf.readUtf();
        int rCId = buf.readInt();
        int rCTim = buf.readInt();
        Map rCVM = buf.readMap(FriendlyByteBuf::m_130259_, FriendlyByteBuf::readBoolean);
        List aStrikes = buf.readList(b -> {
            AASWorldData.ActiveStrike s = new AASWorldData.ActiveStrike(b.readBlockPos(), b.readUtf());
            s.stage = b.readInt();
            s.ticksLeft = b.readInt();
            return s;
        });
        BlockPos bAP = buf.readBlockPos();
        BlockPos rAP = buf.readBlockPos();
        int bAT = buf.readInt();
        int rAT = buf.readInt();
        String bARN = buf.readUtf();
        String rARN = buf.readUtf();
        boolean bReady = buf.readBoolean();
        boolean rReady = buf.readBoolean();
        return new PacketSyncGameData(bT, rT, hBR, hRR, bBl, rBl, rTime, bB, rB, hsc, hsca, mCX, mCZ, mSB, cMI, vL, hL, bF, rF, bCN, rCN, started, pL, bSp, rSp, nSp, pK, aM, vActive, vTimer, vMap, bCMDId, rCMDId, bCA, bCNam, bCId, bCTim, bCVM, rCA, rCNam, rCId, rCTim, rCVM, aStrikes, bAP, rAP, bAT, rAT, bARN, rARN, bReady, rReady);
    }

    public static void handle(PacketSyncGameData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.handleSyncGameData(msg)));
        ctx.get().setPacketHandled(true);
    }
}


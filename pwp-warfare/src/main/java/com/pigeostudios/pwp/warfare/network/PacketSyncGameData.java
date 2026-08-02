package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет полной синхронизации игровых данных: билеты, точки захвата,
// хабы, маркеры, голосования, артиллерия и другая информация о состоянии игры
public class PacketSyncGameData {
   public final int blueTickets;
   public final int redTickets;
   public final int bluePlayerCount;
   public final int redPlayerCount;
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
   public final List<WarfareWorldData.VehicleRecord> markedVehicles;
   public final List<WarfareWorldData.HubInfo> hubs;
   public final List<WarfareWorldData.MainSupplyInfo> mainSupplies;
   public final String blueFaction;
   public final String redFaction;
    public final String blueCustomName;
    public final String redCustomName;
    public final String gameMode;
    public final boolean isGameStarted;
    public final boolean invasionSetupActive;
    public final int invasionSetupTimer;
    public final String invasionDefender;
   public final List<WarfareWorldData.CapturePoint> capturePoints;
   public final Map<String, BlockPos> blueSpawns;
   public final Map<String, BlockPos> redSpawns;
   public final Map<String, BlockPos> neutralSpawns;
   public final Map<String, String> playerKits;
   public final List<WarfareWorldData.MapMarker> activeMarkers;
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
   public final List<WarfareWorldData.ActiveStrike> activeStrikes;
   public final BlockPos blueArtPos;
   public final BlockPos redArtPos;
   public final int blueArtTimer;
   public final int redArtTimer;
   public final String blueArtReqName;
   public final String redArtReqName;

   public PacketSyncGameData(
      int blueTickets,
      int redTickets,
      int bluePlayerCount,
      int redPlayerCount,
      boolean hasBlueRally,
      boolean hasRedRally,
      boolean blueBleeding,
      boolean redBleeding,
      int respawnTime,
      boolean blueBlocked,
      boolean redBlocked,
      boolean hubSpawnCosts,
      int hubSpawnCost,
      int mapCenterX,
      int mapCenterZ,
      int mapSizeBlocks,
      String currentMapImage,
      List<WarfareWorldData.VehicleRecord> markedVehicles,
      List<WarfareWorldData.HubInfo> hubs,
      List<WarfareWorldData.MainSupplyInfo> mainSupplies,
      String blueFaction,
      String redFaction,
       String blueCustomName,
       String redCustomName,
        String gameMode,
        boolean isGameStarted,
        boolean invasionSetupActive,
        int invasionSetupTimer,
        String invasionDefender,
       List<WarfareWorldData.CapturePoint> capturePoints,
      Map<String, BlockPos> blueSpawns,
      Map<String, BlockPos> redSpawns,
      Map<String, BlockPos> neutralSpawns,
      Map<String, String> playerKits,
      List<WarfareWorldData.MapMarker> activeMarkers,
      boolean voteActive,
      int voteTimer,
      Map<UUID, Boolean> votes,
      int blueCMDId,
      int redCMDId,
      boolean bCmdActive,
      String bCmdName,
      int bCmdId,
      int bCmdTimer,
      Map<UUID, Boolean> bCmdVotes,
      boolean rCmdActive,
      String rCmdName,
      int rCmdId,
      int rCmdTimer,
      Map<UUID, Boolean> rCmdVotes,
      List<WarfareWorldData.ActiveStrike> activeStrikes,
      BlockPos blueArtPos,
      BlockPos redArtPos,
      int blueArtTimer,
      int redArtTimer,
      String blueArtReqName,
      String redArtReqName,
      boolean blueReady,
      boolean redReady
   ) {
      this.blueTickets = blueTickets;
      this.redTickets = redTickets;
      this.bluePlayerCount = bluePlayerCount;
      this.redPlayerCount = redPlayerCount;
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
      this.mainSupplies = mainSupplies;
      this.blueFaction = blueFaction;
      this.redFaction = redFaction;
       this.blueCustomName = blueCustomName;
       this.redCustomName = redCustomName;
       this.gameMode = gameMode;
        this.isGameStarted = isGameStarted;
        this.invasionSetupActive = invasionSetupActive;
        this.invasionSetupTimer = invasionSetupTimer;
        this.invasionDefender = invasionDefender;
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
      buf.writeInt(msg.bluePlayerCount);
      buf.writeInt(msg.redPlayerCount);
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
         b.writeInt(v.ticketPenalty);
      });
      buf.writeCollection(msg.hubs, (b, h) -> {
         b.writeBlockPos(h.pos);
         b.writeUtf(h.team);
         b.writeBoolean(h.constructed);
         b.writeUtf(h.dimension);
         b.writeBoolean(h.isBlocked);
         b.writeInt(h.materials);
      });
      buf.writeCollection(msg.mainSupplies, (b, s) -> {
         b.writeBlockPos(s.pos);
         b.writeUtf(s.team);
         b.writeUtf(s.dimension);
      });
      buf.writeUtf(msg.blueFaction);
      buf.writeUtf(msg.redFaction);
      buf.writeUtf(msg.blueCustomName);
      buf.writeUtf(msg.redCustomName);
      buf.writeUtf(msg.gameMode);
      buf.writeBoolean(msg.isGameStarted);
      buf.writeBoolean(msg.invasionSetupActive);
      buf.writeInt(msg.invasionSetupTimer);
      buf.writeUtf(msg.invasionDefender);
      CompoundTag pointsTag = new CompoundTag();
      ListTag list = new ListTag();

      for (WarfareWorldData.CapturePoint cp : msg.capturePoints) {
         list.add(cp.save());
      }

      pointsTag.put("Points", list);
      buf.writeNbt(pointsTag);
      buf.writeMap(msg.blueSpawns, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeBlockPos);
      buf.writeMap(msg.redSpawns, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeBlockPos);
      buf.writeMap(msg.neutralSpawns, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeBlockPos);
      buf.writeMap(msg.playerKits, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
      buf.writeCollection(msg.activeMarkers, (b, m) -> {
         b.writeBlockPos(m.pos);
         b.writeUtf(m.type);
         b.writeUtf(m.team);
         b.writeLong(m.expiryTick);
      });
      buf.writeBoolean(msg.voteActive);
      buf.writeInt(msg.voteTimer);
      buf.writeMap(msg.votes, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeBoolean);
      buf.writeInt(msg.blueCMDId);
      buf.writeInt(msg.redCMDId);
      buf.writeBoolean(msg.blueCmdVoteActive);
      buf.writeUtf(msg.blueCmdCandidateName);
      buf.writeInt(msg.blueCmdCandidateId);
      buf.writeInt(msg.blueCmdVoteTimer);
      buf.writeMap(msg.blueCmdVotes, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeBoolean);
      buf.writeBoolean(msg.redCmdVoteActive);
      buf.writeUtf(msg.redCmdCandidateName);
      buf.writeInt(msg.redCmdCandidateId);
      buf.writeInt(msg.redCmdVoteTimer);
      buf.writeMap(msg.redCmdVotes, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeBoolean);
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
      int bPC = buf.readInt();
      int rPC = buf.readInt();
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
      List<WarfareWorldData.VehicleRecord> vL = buf.readList(
         b -> new WarfareWorldData.VehicleRecord(b.readUUID(), b.readUtf(), b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), null, b.readInt())
      );
      List<WarfareWorldData.HubInfo> hL = buf.readList(b -> {
         WarfareWorldData.HubInfo h = new WarfareWorldData.HubInfo(b.readBlockPos(), b.readUtf(), b.readBoolean(), b.readUtf());
         h.isBlocked = b.readBoolean();
         h.materials = b.readInt();
         return h;
      });
      List<WarfareWorldData.MainSupplyInfo> sL = buf.readList(b -> new WarfareWorldData.MainSupplyInfo(b.readBlockPos(), b.readUtf(), b.readUtf()));
      String bF = buf.readUtf();
      String rF = buf.readUtf();
      String bCN = buf.readUtf();
      String rCN = buf.readUtf();
       String gMode = buf.readUtf();
       boolean started = buf.readBoolean();
       boolean invSetupActive = buf.readBoolean();
       int invSetupTimer = buf.readInt();
       String invDefender = buf.readUtf();
       List<WarfareWorldData.CapturePoint> pL = new ArrayList<>();
      CompoundTag pTag = buf.readNbt();
      if (pTag != null && pTag.contains("Points")) {
         ListTag list = pTag.getList("Points", 10);

         for (int i = 0; i < list.size(); i++) {
            pL.add(WarfareWorldData.CapturePoint.load(list.getCompound(i)));
         }
      }

      Map<String, BlockPos> bSp = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readBlockPos);
      Map<String, BlockPos> rSp = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readBlockPos);
      Map<String, BlockPos> nSp = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readBlockPos);
      Map<String, String> pK = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
      List<WarfareWorldData.MapMarker> aM = buf.readList(b -> new WarfareWorldData.MapMarker(b.readBlockPos(), b.readUtf(), b.readUtf(), b.readLong()));
      boolean vActive = buf.readBoolean();
      int vTimer = buf.readInt();
      Map<UUID, Boolean> vMap = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readBoolean);
      int bCMDId = buf.readInt();
      int rCMDId = buf.readInt();
      boolean bCA = buf.readBoolean();
      String bCNam = buf.readUtf();
      int bCId = buf.readInt();
      int bCTim = buf.readInt();
      Map<UUID, Boolean> bCVM = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readBoolean);
      boolean rCA = buf.readBoolean();
      String rCNam = buf.readUtf();
      int rCId = buf.readInt();
      int rCTim = buf.readInt();
      Map<UUID, Boolean> rCVM = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readBoolean);
      List<WarfareWorldData.ActiveStrike> aStrikes = buf.readList(b -> {
         WarfareWorldData.ActiveStrike s = new WarfareWorldData.ActiveStrike(b.readBlockPos(), b.readUtf());
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
      return new PacketSyncGameData(
         bT,
         rT,
         bPC,
         rPC,
         hBR,
         hRR,
         bBl,
         rBl,
         rTime,
         bB,
         rB,
         hsc,
         hsca,
         mCX,
         mCZ,
         mSB,
          cMI,
          vL,
          hL,
          sL,
          bF,
         rF,
          bCN,
          rCN,
           gMode,
           started,
           invSetupActive,
           invSetupTimer,
           invDefender,
          pL,
          bSp,
         rSp,
         nSp,
         pK,
         aM,
         vActive,
         vTimer,
         vMap,
         bCMDId,
         rCMDId,
         bCA,
         bCNam,
         bCId,
         bCTim,
         bCVM,
         rCA,
         rCNam,
         rCId,
         rCTim,
         rCVM,
         aStrikes,
         bAP,
         rAP,
         bAT,
         rAT,
         bARN,
         rARN,
         bReady,
         rReady
      );
   }

   // Передаёт полный снимок игровых данных клиентскому обработчику
   public static void handle(PacketSyncGameData msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleSyncGameData(msg)));
      ctx.get().setPacketHandled(true);
   }
}

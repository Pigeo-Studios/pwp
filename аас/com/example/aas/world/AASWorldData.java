/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.NonNullList
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.LongTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.example.aas.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AASWorldData
extends SavedData {
    public List<CapturePoint> capturePoints = new ArrayList<CapturePoint>();
    public List<BlockPos> blueRallies = new ArrayList<BlockPos>();
    public List<BlockPos> redRallies = new ArrayList<BlockPos>();
    public List<HubInfo> hubs = new ArrayList<HubInfo>();
    public List<Squad> squads = new ArrayList<Squad>();
    public int blueCMDId = -1;
    public int redCMDId = -1;
    public boolean blueCmdVoteActive = false;
    public String blueCmdCandidateName = "";
    public int blueCmdCandidateId = -1;
    public int blueCmdVoteTimer = 0;
    public Map<UUID, Boolean> blueCmdVotes = new HashMap<UUID, Boolean>();
    public boolean redCmdVoteActive = false;
    public String redCmdCandidateName = "";
    public int redCmdCandidateId = -1;
    public int redCmdVoteTimer = 0;
    public Map<UUID, Boolean> redCmdVotes = new HashMap<UUID, Boolean>();
    public long blueArtStrikeCD = 0L;
    public long redArtStrikeCD = 0L;
    public String shapeType = "CUBE";
    public int lockDurationMinutes = 0;
    public long lockedUntilTick = 0L;
    public Map<String, BlockPos> blueSpawns = new HashMap<String, BlockPos>();
    public Map<String, BlockPos> redSpawns = new HashMap<String, BlockPos>();
    public Map<String, BlockPos> neutralSpawns = new HashMap<String, BlockPos>();
    public List<VehicleRecord> markedVehicles = new ArrayList<VehicleRecord>();
    public int blueTickets = 800;
    public int redTickets = 800;
    public int respawnTimer = 10;
    public int deathTicketCost = 2;
    public String blueFaction = "none";
    public String redFaction = "none";
    public boolean isGameStarted = false;
    public int countdownTicks = 0;
    public boolean countdownActive = false;
    public boolean playedBlueSiren = false;
    public boolean playedRedSiren = false;
    public Map<String, KitInfo> blueKits = new HashMap<String, KitInfo>();
    public Map<String, KitInfo> redKits = new HashMap<String, KitInfo>();
    public int mapCenterX = 0;
    public int mapCenterZ = 0;
    public int mapSizeBlocks = 2048;
    public String currentMapImage = "map1";
    public static final String[] KIT_NAMES = new String[]{"Officer", "Pilot Officer", "Mechanic Officer", "Scout", "LAT", "HAT", "Sapper", "Sniper", "Marksman", "LMG", "HMG", "Rifleman", "Medic", "Grenadier", "Assault", "Pilot", "Mechanic", "Drone Operator", "Anti_air"};
    public List<BlockPos> triggerBlocks = new ArrayList<BlockPos>();
    public ArtStrikeRequest blueArtRequest = null;
    public ArtStrikeRequest redArtRequest = null;
    public List<ActiveStrike> activeStrikes = new ArrayList<ActiveStrike>();
    public List<MapMarker> activeMarkers = new ArrayList<MapMarker>();
    public List<MainProtectionZone> mainZones = new ArrayList<MainProtectionZone>();
    public boolean voteActive = false;
    public int voteTimer = 0;
    public boolean blueReady = false;
    public boolean redReady = false;
    public Map<UUID, Boolean> votes = new HashMap<UUID, Boolean>();

    public AASWorldData() {
        for (String name : KIT_NAMES) {
            this.blueKits.put(name, new KitInfo(name));
            this.redKits.put(name, new KitInfo(name));
        }
    }

    public CompoundTag m_7176_(CompoundTag tag) {
        tag.m_128405_("BlueTickets", this.blueTickets);
        tag.m_128405_("RedTickets", this.redTickets);
        tag.m_128405_("RespawnTimer", this.respawnTimer);
        tag.m_128405_("DeathTicketCost", this.deathTicketCost);
        tag.m_128359_("BlueFaction", this.blueFaction);
        tag.m_128359_("RedFaction", this.redFaction);
        tag.m_128379_("IsGameStarted", this.isGameStarted);
        tag.m_128405_("CountdownTicks", this.countdownTicks);
        tag.m_128379_("CountdownActive", this.countdownActive);
        tag.m_128379_("PlayedBlueSiren", this.playedBlueSiren);
        tag.m_128379_("PlayedRedSiren", this.playedRedSiren);
        tag.m_128405_("MapCenterX", this.mapCenterX);
        tag.m_128405_("MapCenterZ", this.mapCenterZ);
        tag.m_128405_("MapSizeBlocks", this.mapSizeBlocks);
        tag.m_128359_("CurrentMapImage", this.currentMapImage);
        tag.m_128356_("BlueArtCD", this.blueArtStrikeCD);
        tag.m_128356_("RedArtCD", this.redArtStrikeCD);
        tag.m_128405_("BlueCMDId", this.blueCMDId);
        tag.m_128405_("RedCMDId", this.redCMDId);
        tag.m_128379_("BlueCmdActive", this.blueCmdVoteActive);
        tag.m_128359_("BlueCmdCandName", this.blueCmdCandidateName);
        tag.m_128405_("BlueCmdCandId", this.blueCmdCandidateId);
        tag.m_128405_("BlueCmdTimer", this.blueCmdVoteTimer);
        CompoundTag blueVotesTag = new CompoundTag();
        this.blueCmdVotes.forEach((uuid, val) -> blueVotesTag.m_128379_(uuid.toString(), val.booleanValue()));
        tag.m_128365_("BlueCmdVotesMap", (Tag)blueVotesTag);
        tag.m_128379_("RedCmdActive", this.redCmdVoteActive);
        tag.m_128359_("RedCmdCandName", this.redCmdCandidateName);
        tag.m_128405_("RedCmdCandId", this.redCmdCandidateId);
        tag.m_128405_("RedCmdTimer", this.redCmdVoteTimer);
        CompoundTag redVotesTag = new CompoundTag();
        this.redCmdVotes.forEach((uuid, val) -> redVotesTag.m_128379_(uuid.toString(), val.booleanValue()));
        tag.m_128365_("RedCmdVotesMap", (Tag)redVotesTag);
        ListTag vehicleList = new ListTag();
        for (VehicleRecord vehicleRecord : this.markedVehicles) {
            vehicleList.add((Object)vehicleRecord.save());
        }
        ListTag triggerList = new ListTag();
        for (BlockPos blockPos : this.triggerBlocks) {
            triggerList.add((Object)LongTag.m_128882_((long)blockPos.m_121878_()));
        }
        tag.m_128365_("TriggerBlocks", (Tag)triggerList);
        tag.m_128365_("MarkedVehicles", (Tag)vehicleList);
        ListTag listTag = new ListTag();
        for (MapMarker mapMarker : this.activeMarkers) {
            listTag.add((Object)mapMarker.save());
        }
        tag.m_128365_("TacticalMarkers", (Tag)listTag);
        ListTag listTag2 = new ListTag();
        for (BlockPos blockPos : this.blueRallies) {
            listTag2.add((Object)LongTag.m_128882_((long)blockPos.m_121878_()));
        }
        tag.m_128365_("BlueRallies", (Tag)listTag2);
        ListTag listTag3 = new ListTag();
        for (MainProtectionZone mainProtectionZone : this.mainZones) {
            listTag3.add((Object)mainProtectionZone.save());
        }
        tag.m_128365_("MainZones", (Tag)listTag3);
        ListTag listTag4 = new ListTag();
        for (BlockPos blockPos : this.redRallies) {
            listTag4.add((Object)LongTag.m_128882_((long)blockPos.m_121878_()));
        }
        tag.m_128365_("RedRallies", (Tag)listTag4);
        ListTag listTag5 = new ListTag();
        for (HubInfo h : this.hubs) {
            listTag5.add((Object)h.save());
        }
        tag.m_128365_("HubsData", (Tag)listTag5);
        CompoundTag compoundTag = new CompoundTag();
        this.blueSpawns.forEach((dim, pos) -> blueSpawnsTag.m_128356_(dim, pos.m_121878_()));
        tag.m_128365_("BlueSpawnsMap", (Tag)compoundTag);
        CompoundTag redSpawnsTag = new CompoundTag();
        this.redSpawns.forEach((dim, pos) -> redSpawnsTag.m_128356_(dim, pos.m_121878_()));
        tag.m_128365_("RedSpawnsMap", (Tag)redSpawnsTag);
        CompoundTag neutralSpawnsTag = new CompoundTag();
        this.neutralSpawns.forEach((dim, pos) -> neutralSpawnsTag.m_128356_(dim, pos.m_121878_()));
        tag.m_128365_("NeutralSpawnsMap", (Tag)neutralSpawnsTag);
        ListTag pointsList = new ListTag();
        for (CapturePoint capturePoint : this.capturePoints) {
            pointsList.add((Object)capturePoint.save());
        }
        tag.m_128365_("CapturePoints", (Tag)pointsList);
        ListTag squadList = new ListTag();
        for (Squad squad : this.squads) {
            squadList.add((Object)squad.save());
        }
        tag.m_128365_("Squads", (Tag)squadList);
        CompoundTag compoundTag2 = new CompoundTag();
        for (KitInfo kitInfo : this.blueKits.values()) {
            compoundTag2.m_128365_(kitInfo.name, (Tag)kitInfo.save());
        }
        tag.m_128365_("BlueKits", (Tag)compoundTag2);
        CompoundTag compoundTag3 = new CompoundTag();
        for (KitInfo k : this.redKits.values()) {
            compoundTag3.m_128365_(k.name, (Tag)k.save());
        }
        tag.m_128365_("RedKits", (Tag)compoundTag3);
        if (this.blueArtRequest != null) {
            CompoundTag compoundTag4 = new CompoundTag();
            compoundTag4.m_128359_("Name", this.blueArtRequest.requesterName);
            compoundTag4.m_128356_("Pos", this.blueArtRequest.pos.m_121878_());
            compoundTag4.m_128405_("Timer", this.blueArtRequest.timer);
            tag.m_128365_("BlueArtReq", (Tag)compoundTag4);
        }
        if (this.redArtRequest != null) {
            CompoundTag compoundTag5 = new CompoundTag();
            compoundTag5.m_128359_("Name", this.redArtRequest.requesterName);
            compoundTag5.m_128356_("Pos", this.redArtRequest.pos.m_121878_());
            compoundTag5.m_128405_("Timer", this.redArtRequest.timer);
            tag.m_128365_("RedArtReq", (Tag)compoundTag5);
        }
        ListTag listTag6 = new ListTag();
        for (ActiveStrike s : this.activeStrikes) {
            CompoundTag sTag = new CompoundTag();
            sTag.m_128356_("Pos", s.pos.m_121878_());
            sTag.m_128359_("Team", s.team);
            sTag.m_128405_("Stage", s.stage);
            sTag.m_128405_("Ticks", s.ticksLeft);
            listTag6.add((Object)sTag);
        }
        tag.m_128365_("ActiveStrikesList", (Tag)listTag6);
        return tag;
    }

    public static AASWorldData load(CompoundTag tag) {
        CompoundTag req;
        CompoundTag map;
        ListTag list;
        CompoundTag cv;
        AASWorldData data = new AASWorldData();
        data.blueTickets = tag.m_128451_("BlueTickets");
        data.redTickets = tag.m_128451_("RedTickets");
        data.respawnTimer = tag.m_128451_("RespawnTimer");
        data.deathTicketCost = tag.m_128451_("DeathTicketCost");
        data.blueFaction = tag.m_128461_("BlueFaction");
        data.redFaction = tag.m_128461_("RedFaction");
        data.isGameStarted = tag.m_128471_("IsGameStarted");
        data.countdownTicks = tag.m_128451_("CountdownTicks");
        data.countdownActive = tag.m_128471_("CountdownActive");
        data.playedBlueSiren = tag.m_128471_("PlayedBlueSiren");
        data.playedRedSiren = tag.m_128471_("PlayedRedSiren");
        data.mapCenterX = tag.m_128451_("MapCenterX");
        data.mapCenterZ = tag.m_128451_("MapCenterZ");
        data.mapSizeBlocks = tag.m_128441_("MapSizeBlocks") ? tag.m_128451_("MapSizeBlocks") : 2048;
        data.currentMapImage = tag.m_128441_("CurrentMapImage") ? tag.m_128461_("CurrentMapImage") : "map1";
        data.blueArtStrikeCD = tag.m_128454_("BlueArtCD");
        data.redArtStrikeCD = tag.m_128454_("RedArtCD");
        data.blueCMDId = tag.m_128451_("BlueCMDId");
        data.redCMDId = tag.m_128451_("RedCMDId");
        data.blueCmdVoteActive = tag.m_128471_("BlueCmdActive");
        data.blueCmdCandidateName = tag.m_128461_("BlueCmdCandName");
        data.blueCmdCandidateId = tag.m_128451_("BlueCmdCandId");
        data.blueCmdVoteTimer = tag.m_128451_("BlueCmdTimer");
        if (tag.m_128441_("BlueCmdVotesMap")) {
            cv = tag.m_128469_("BlueCmdVotesMap");
            for (String key : cv.m_128431_()) {
                data.blueCmdVotes.put(UUID.fromString(key), cv.m_128471_(key));
            }
        }
        data.redCmdVoteActive = tag.m_128471_("RedCmdActive");
        data.redCmdCandidateName = tag.m_128461_("RedCmdCandName");
        data.redCmdCandidateId = tag.m_128451_("RedCmdCandId");
        data.redCmdVoteTimer = tag.m_128451_("RedCmdTimer");
        if (tag.m_128441_("RedCmdVotesMap")) {
            cv = tag.m_128469_("RedCmdVotesMap");
            for (String key : cv.m_128431_()) {
                data.redCmdVotes.put(UUID.fromString(key), cv.m_128471_(key));
            }
        }
        if (tag.m_128441_("TacticalMarkers")) {
            list = tag.m_128437_("TacticalMarkers", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.activeMarkers.add(MapMarker.load(list.m_128728_(i)));
            }
        }
        if (tag.m_128441_("TriggerBlocks")) {
            list = tag.m_128437_("TriggerBlocks", 4);
            for (Tag t : list) {
                data.triggerBlocks.add(BlockPos.m_122022_((long)((LongTag)t).m_7046_()));
            }
        }
        if (tag.m_128441_("MainZones")) {
            list = tag.m_128437_("MainZones", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.mainZones.add(MainProtectionZone.load(list.m_128728_(i)));
            }
        }
        if (tag.m_128441_("MarkedVehicles")) {
            list = tag.m_128437_("MarkedVehicles", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.markedVehicles.add(VehicleRecord.load(list.m_128728_(i)));
            }
        }
        if (tag.m_128441_("BlueRallies")) {
            list = tag.m_128437_("BlueRallies", 4);
            for (Tag t : list) {
                data.blueRallies.add(BlockPos.m_122022_((long)((LongTag)t).m_7046_()));
            }
        }
        if (tag.m_128441_("RedRallies")) {
            list = tag.m_128437_("RedRallies", 4);
            for (Tag t : list) {
                data.redRallies.add(BlockPos.m_122022_((long)((LongTag)t).m_7046_()));
            }
        }
        if (tag.m_128441_("HubsData")) {
            list = tag.m_128437_("HubsData", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.hubs.add(HubInfo.load(list.m_128728_(i)));
            }
        }
        if (tag.m_128441_("BlueSpawnsMap")) {
            map = tag.m_128469_("BlueSpawnsMap");
            for (String key : map.m_128431_()) {
                data.blueSpawns.put(key, BlockPos.m_122022_((long)map.m_128454_(key)));
            }
        }
        if (tag.m_128441_("RedSpawnsMap")) {
            map = tag.m_128469_("RedSpawnsMap");
            for (String key : map.m_128431_()) {
                data.redSpawns.put(key, BlockPos.m_122022_((long)map.m_128454_(key)));
            }
        }
        if (tag.m_128441_("NeutralSpawnsMap")) {
            map = tag.m_128469_("NeutralSpawnsMap");
            for (String key : map.m_128431_()) {
                data.neutralSpawns.put(key, BlockPos.m_122022_((long)map.m_128454_(key)));
            }
        }
        if (tag.m_128441_("CapturePoints")) {
            list = tag.m_128437_("CapturePoints", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.capturePoints.add(CapturePoint.load(list.m_128728_(i)));
            }
        }
        if (tag.m_128441_("Squads")) {
            list = tag.m_128437_("Squads", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.squads.add(Squad.load(list.m_128728_(i)));
            }
        }
        if (tag.m_128441_("BlueKits")) {
            CompoundTag bk = tag.m_128469_("BlueKits");
            for (String key : bk.m_128431_()) {
                data.blueKits.put(key, KitInfo.load(bk.m_128469_(key)));
            }
        }
        if (tag.m_128441_("RedKits")) {
            CompoundTag rk = tag.m_128469_("RedKits");
            for (String key : rk.m_128431_()) {
                data.redKits.put(key, KitInfo.load(rk.m_128469_(key)));
            }
        }
        if (tag.m_128441_("BlueArtReq")) {
            req = tag.m_128469_("BlueArtReq");
            data.blueArtRequest = new ArtStrikeRequest(req.m_128461_("Name"), BlockPos.m_122022_((long)req.m_128454_("Pos")));
            data.blueArtRequest.timer = req.m_128451_("Timer");
        }
        if (tag.m_128441_("RedArtReq")) {
            req = tag.m_128469_("RedArtReq");
            data.redArtRequest = new ArtStrikeRequest(req.m_128461_("Name"), BlockPos.m_122022_((long)req.m_128454_("Pos")));
            data.redArtRequest.timer = req.m_128451_("Timer");
        }
        if (tag.m_128441_("ActiveStrikesList")) {
            list = tag.m_128437_("ActiveStrikesList", 10);
            for (int i = 0; i < list.size(); ++i) {
                CompoundTag sTag = list.m_128728_(i);
                ActiveStrike s = new ActiveStrike(BlockPos.m_122022_((long)sTag.m_128454_("Pos")), sTag.m_128461_("Team"));
                s.stage = sTag.m_128451_("Stage");
                s.ticksLeft = sTag.m_128451_("Ticks");
                data.activeStrikes.add(s);
            }
        }
        return data;
    }

    public static AASWorldData get(ServerLevel level) {
        String dimId = level.m_46472_().m_135782_().toString().replace(":", "_");
        String dataName = "aas_data_" + dimId;
        return (AASWorldData)level.m_8895_().m_164861_(AASWorldData::load, AASWorldData::new, dataName);
    }

    public static class ArtStrikeRequest {
        public String requesterName;
        public BlockPos pos;
        public int timer = 200;

        public ArtStrikeRequest(String name, BlockPos p) {
            this.requesterName = name;
            this.pos = p;
        }
    }

    public static class KitInfo {
        public String name;
        public NonNullList<ItemStack> inventory = NonNullList.m_122780_((int)49, (Object)ItemStack.f_41583_);
        public boolean[] resupplyFlags = new boolean[49];
        public boolean[] saveNbtFlags = new boolean[49];
        public boolean isLeaderOnly = false;
        public int maxPerTeam = -1;
        public int maxPerSquad = -1;
        public int minSquadPlayers = 0;

        public KitInfo(String name) {
            this.name = name;
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.m_128359_("Name", this.name);
            t.m_128379_("LeaderOnly", this.isLeaderOnly);
            t.m_128405_("MaxTeam", this.maxPerTeam);
            t.m_128405_("MaxSquad", this.maxPerSquad);
            t.m_128405_("MinSquadPlayers", this.minSquadPlayers);
            ListTag items = new ListTag();
            for (int i = 0; i < 49; ++i) {
                if (((ItemStack)this.inventory.get(i)).m_41619_()) continue;
                CompoundTag itemTag = new CompoundTag();
                itemTag.m_128344_("Slot", (byte)i);
                itemTag.m_128379_("Resupply", this.resupplyFlags[i]);
                itemTag.m_128379_("SaveNbt", this.saveNbtFlags[i]);
                ((ItemStack)this.inventory.get(i)).m_41739_(itemTag);
                items.add((Object)itemTag);
            }
            t.m_128365_("Items", (Tag)items);
            return t;
        }

        public static KitInfo load(CompoundTag t) {
            KitInfo k = new KitInfo(t.m_128461_("Name"));
            k.isLeaderOnly = t.m_128471_("LeaderOnly");
            k.maxPerTeam = t.m_128451_("MaxTeam");
            k.maxPerSquad = t.m_128451_("MaxSquad");
            if (t.m_128441_("MinSquadPlayers")) {
                k.minSquadPlayers = t.m_128451_("MinSquadPlayers");
            }
            ListTag items = t.m_128437_("Items", 10);
            for (int i = 0; i < items.size(); ++i) {
                CompoundTag itemTag = items.m_128728_(i);
                int slot = itemTag.m_128445_("Slot") & 0xFF;
                if (slot < 0 || slot >= 49) continue;
                k.inventory.set(slot, (Object)ItemStack.m_41712_((CompoundTag)itemTag));
                k.resupplyFlags[slot] = itemTag.m_128471_("Resupply");
                k.saveNbtFlags[slot] = itemTag.m_128471_("SaveNbt");
            }
            return k;
        }
    }

    public static class VehicleRecord {
        public UUID uuid;
        public String team;
        public String type;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public BlockPos spawnerPos;

        public VehicleRecord(UUID uuid, String team, String type, double x, double y, double z, float yaw, BlockPos spawnerPos) {
            this.uuid = uuid;
            this.team = team;
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.spawnerPos = spawnerPos;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.m_128362_("UUID", this.uuid);
            tag.m_128359_("Team", this.team);
            tag.m_128359_("Type", this.type);
            tag.m_128347_("X", this.x);
            tag.m_128347_("Y", this.y);
            tag.m_128347_("Z", this.z);
            tag.m_128350_("Yaw", this.yaw);
            if (this.spawnerPos != null) {
                tag.m_128356_("SpawnerPos", this.spawnerPos.m_121878_());
            }
            return tag;
        }

        public static VehicleRecord load(CompoundTag tag) {
            BlockPos sPos = tag.m_128441_("SpawnerPos") ? BlockPos.m_122022_((long)tag.m_128454_("SpawnerPos")) : null;
            return new VehicleRecord(tag.m_128342_("UUID"), tag.m_128461_("Team"), tag.m_128461_("Type"), tag.m_128459_("X"), tag.m_128459_("Y"), tag.m_128459_("Z"), tag.m_128457_("Yaw"), sPos);
        }
    }

    public static class MapMarker {
        public BlockPos pos;
        public String type;
        public String team;
        public long expiryTick;

        public MapMarker(BlockPos pos, String type, String team, long expiryTick) {
            this.pos = pos;
            this.type = type;
            this.team = team;
            this.expiryTick = expiryTick;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.m_128356_("Pos", this.pos.m_121878_());
            tag.m_128359_("Type", this.type);
            tag.m_128359_("Team", this.team);
            tag.m_128356_("Expiry", this.expiryTick);
            return tag;
        }

        public static MapMarker load(CompoundTag tag) {
            return new MapMarker(BlockPos.m_122022_((long)tag.m_128454_("Pos")), tag.m_128461_("Type"), tag.m_128461_("Team"), tag.m_128454_("Expiry"));
        }
    }

    public static class MainProtectionZone {
        public String team;
        public String shape;
        public AABB area;

        public MainProtectionZone(String team, String shape, AABB area) {
            this.team = team;
            this.shape = shape;
            this.area = area;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.m_128359_("Team", this.team);
            tag.m_128359_("Shape", this.shape);
            tag.m_128347_("minX", this.area.f_82288_);
            tag.m_128347_("minY", this.area.f_82289_);
            tag.m_128347_("minZ", this.area.f_82290_);
            tag.m_128347_("maxX", this.area.f_82291_);
            tag.m_128347_("maxY", this.area.f_82292_);
            tag.m_128347_("maxZ", this.area.f_82293_);
            return tag;
        }

        public static MainProtectionZone load(CompoundTag tag) {
            AABB aabb = new AABB(tag.m_128459_("minX"), tag.m_128459_("minY"), tag.m_128459_("minZ"), tag.m_128459_("maxX"), tag.m_128459_("maxY"), tag.m_128459_("maxZ"));
            return new MainProtectionZone(tag.m_128461_("Team"), tag.m_128461_("Shape"), aabb);
        }

        public boolean isInside(Vec3 pos) {
            if ("CYLINDER".equalsIgnoreCase(this.shape)) {
                double centerX = (this.area.f_82288_ + this.area.f_82291_) / 2.0;
                double dx = pos.f_82479_ - centerX;
                double centerZ = (this.area.f_82290_ + this.area.f_82293_) / 2.0;
                double dz = pos.f_82481_ - centerZ;
                double radius = (this.area.f_82291_ - this.area.f_82288_) / 2.0;
                return dx * dx + dz * dz <= radius * radius && pos.f_82480_ >= this.area.f_82289_ && pos.f_82480_ <= this.area.f_82292_;
            }
            return this.area.m_82390_(pos);
        }
    }

    public static class HubInfo {
        public BlockPos pos;
        public String team;
        public boolean constructed;
        public String dimension;
        public boolean isBlocked;
        public int materials;

        public HubInfo(BlockPos pos, String team, boolean constructed, String dimension) {
            this.pos = pos;
            this.team = team;
            this.constructed = constructed;
            this.dimension = dimension;
            this.isBlocked = false;
            this.materials = 0;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.m_128356_("Pos", this.pos.m_121878_());
            tag.m_128359_("Team", this.team);
            tag.m_128379_("Constructed", this.constructed);
            tag.m_128359_("Dimension", this.dimension != null ? this.dimension : "minecraft:overworld");
            tag.m_128405_("Materials", this.materials);
            return tag;
        }

        public static HubInfo load(CompoundTag tag) {
            BlockPos p = BlockPos.m_122022_((long)tag.m_128454_("Pos"));
            String t = tag.m_128461_("Team");
            boolean c = tag.m_128471_("Constructed");
            String d = tag.m_128441_("Dimension") ? tag.m_128461_("Dimension") : "minecraft:overworld";
            HubInfo h = new HubInfo(p, t, c, d);
            if (tag.m_128441_("Materials")) {
                h.materials = tag.m_128451_("Materials");
            }
            return h;
        }
    }

    public static class CapturePoint {
        public String name;
        public AABB area;
        public int bluePriority;
        public int redPriority;
        public int captureTimeMinutes;
        public int ticketPenalty;
        public int captureDeduction;
        public String owner = "NEUTRAL";
        public float progress = 0.0f;
        public String capturingTeam = "NONE";
        public String shapeType = "CUBE";
        public int lockDurationMinutes = 0;
        public long lockedUntilTick = 0L;

        public CapturePoint(String name, AABB area, int bp, int rp, int time, int penalty, int deduct, String shape, int lockMin) {
            this.name = name;
            this.area = area;
            this.bluePriority = bp;
            this.redPriority = rp;
            this.captureTimeMinutes = time;
            this.ticketPenalty = penalty;
            this.captureDeduction = deduct;
            this.shapeType = shape;
            this.lockDurationMinutes = lockMin;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.m_128359_("Name", this.name);
            tag.m_128347_("minX", this.area.f_82288_);
            tag.m_128347_("minY", this.area.f_82289_);
            tag.m_128347_("minZ", this.area.f_82290_);
            tag.m_128347_("maxX", this.area.f_82291_);
            tag.m_128347_("maxY", this.area.f_82292_);
            tag.m_128347_("maxZ", this.area.f_82293_);
            tag.m_128405_("BluePriority", this.bluePriority);
            tag.m_128405_("RedPriority", this.redPriority);
            tag.m_128405_("Time", this.captureTimeMinutes);
            tag.m_128405_("Penalty", this.ticketPenalty);
            tag.m_128405_("Deduct", this.captureDeduction);
            tag.m_128359_("Owner", this.owner);
            tag.m_128350_("Progress", this.progress);
            tag.m_128359_("CapturingTeam", this.capturingTeam);
            tag.m_128359_("Shape", this.shapeType);
            tag.m_128405_("LockMin", this.lockDurationMinutes);
            tag.m_128356_("LockedUntil", this.lockedUntilTick);
            return tag;
        }

        public static CapturePoint load(CompoundTag tag) {
            AABB area = new AABB(tag.m_128459_("minX"), tag.m_128459_("minY"), tag.m_128459_("minZ"), tag.m_128459_("maxX"), tag.m_128459_("maxY"), tag.m_128459_("maxZ"));
            CapturePoint point = new CapturePoint(tag.m_128461_("Name"), area, tag.m_128451_("BluePriority"), tag.m_128451_("RedPriority"), tag.m_128451_("Time"), tag.m_128451_("Penalty"), tag.m_128451_("Deduct"), tag.m_128441_("Shape") ? tag.m_128461_("Shape") : "CUBE", tag.m_128441_("LockMin") ? tag.m_128451_("LockMin") : 0);
            if (tag.m_128441_("Owner")) {
                point.owner = tag.m_128461_("Owner");
            }
            if (tag.m_128441_("Progress")) {
                point.progress = tag.m_128457_("Progress");
            }
            if (tag.m_128441_("CapturingTeam")) {
                point.capturingTeam = tag.m_128461_("CapturingTeam");
            }
            if (tag.m_128441_("LockedUntil")) {
                point.lockedUntilTick = tag.m_128454_("LockedUntil");
            }
            return point;
        }

        public boolean isInside(Vec3 pos) {
            if ("CYLINDER".equalsIgnoreCase(this.shapeType)) {
                double centerX = (this.area.f_82288_ + this.area.f_82291_) / 2.0;
                double dx = pos.f_82479_ - centerX;
                double centerZ = (this.area.f_82290_ + this.area.f_82293_) / 2.0;
                double dz = pos.f_82481_ - centerZ;
                double radius = (this.area.f_82291_ - this.area.f_82288_) / 2.0;
                boolean inCircle = dx * dx + dz * dz <= radius * radius;
                boolean inHeight = pos.f_82480_ >= this.area.f_82289_ && pos.f_82480_ <= this.area.f_82292_;
                return inCircle && inHeight;
            }
            return this.area.m_82390_(pos);
        }
    }

    public static class Squad {
        public int id;
        public String name;
        public String team;
        public String leader;
        public boolean isLocked = false;
        public BlockPos rallyPos = null;
        public String dimension;
        public String rallyDimension;
        public List<String> members = new ArrayList<String>();
        public String bravoLeader = "";
        public String charlieLeader = "";
        public List<String> bravoMembers = new ArrayList<String>();
        public List<String> charlieMembers = new ArrayList<String>();
        public SquadMarker bravoMarker = null;
        public SquadMarker charlieMarker = null;
        public BlockPos bravoPingPos = null;
        public long bravoPingExpiry = -1L;
        public BlockPos charliePingPos = null;
        public long charliePingExpiry = -1L;
        public List<SquadMarker> rhombusMarkers = new ArrayList<SquadMarker>();
        public SquadMarker marker = null;
        public long rallyExpiryTick = -1L;
        public long slNoOfficerSince = -1L;
        public long nextRallyAvailableTick = -1L;
        public BlockPos pingPos = null;
        public long pingExpiry = -1L;
        public boolean isRallyBlocked = false;

        public Squad(int id, String name, String team, String leader, String dimension) {
            this.id = id;
            this.name = name;
            this.team = team;
            this.leader = leader;
            this.dimension = dimension;
            this.rallyDimension = dimension;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.m_128405_("ID", this.id);
            tag.m_128359_("Name", this.name);
            tag.m_128359_("Team", this.team);
            tag.m_128359_("Leader", this.leader);
            tag.m_128379_("IsLocked", this.isLocked);
            tag.m_128359_("Dimension", this.dimension != null ? this.dimension : "minecraft:overworld");
            tag.m_128356_("RallyExpiry", this.rallyExpiryTick);
            tag.m_128356_("SLNoOfficerSince", this.slNoOfficerSince);
            tag.m_128356_("NextRallyAvailable", this.nextRallyAvailableTick);
            tag.m_128379_("RallyBlocked", this.isRallyBlocked);
            if (this.pingPos != null) {
                tag.m_128356_("PingPos", this.pingPos.m_121878_());
                tag.m_128356_("PingExpiry", this.pingExpiry);
            }
            if (this.rallyPos != null) {
                tag.m_128356_("RallyPos", this.rallyPos.m_121878_());
                tag.m_128359_("RallyDim", this.rallyDimension != null ? this.rallyDimension : "minecraft:overworld");
            }
            tag.m_128359_("BravoLeader", this.bravoLeader);
            tag.m_128359_("CharlieLeader", this.charlieLeader);
            ListTag bList = new ListTag();
            for (String string : this.bravoMembers) {
                bList.add((Object)StringTag.m_129297_((String)string));
            }
            tag.m_128365_("BravoMembers", (Tag)bList);
            ListTag cList = new ListTag();
            for (String string : this.charlieMembers) {
                cList.add((Object)StringTag.m_129297_((String)string));
            }
            tag.m_128365_("CharlieMembers", (Tag)cList);
            if (this.bravoPingPos != null) {
                tag.m_128356_("BravoPingPos", this.bravoPingPos.m_121878_());
                tag.m_128356_("BravoPingExp", this.bravoPingExpiry);
            }
            if (this.charliePingPos != null) {
                tag.m_128356_("CharliePingPos", this.charliePingPos.m_121878_());
                tag.m_128356_("CharliePingExp", this.charliePingExpiry);
            }
            if (this.bravoMarker != null) {
                tag.m_128365_("BravoMarker", (Tag)this.bravoMarker.save());
            }
            if (this.charlieMarker != null) {
                tag.m_128365_("CharlieMarker", (Tag)this.charlieMarker.save());
            }
            ListTag listTag = new ListTag();
            for (SquadMarker rm : this.rhombusMarkers) {
                listTag.add((Object)rm.save());
            }
            tag.m_128365_("RhombusList", (Tag)listTag);
            ListTag listTag2 = new ListTag();
            for (String m : this.members) {
                listTag2.add((Object)StringTag.m_129297_((String)m));
            }
            tag.m_128365_("Members", (Tag)listTag2);
            if (this.marker != null) {
                tag.m_128365_("SquadMarker", (Tag)this.marker.save());
            }
            return tag;
        }

        public static Squad load(CompoundTag tag) {
            String l = tag.m_128441_("Leader") ? tag.m_128461_("Leader") : "";
            String dim = tag.m_128441_("Dimension") ? tag.m_128461_("Dimension") : "minecraft:overworld";
            Squad s = new Squad(tag.m_128451_("ID"), tag.m_128461_("Name"), tag.m_128461_("Team"), l, dim);
            s.rallyExpiryTick = tag.m_128454_("RallyExpiry");
            s.slNoOfficerSince = tag.m_128454_("SLNoOfficerSince");
            s.isRallyBlocked = tag.m_128471_("RallyBlocked");
            if (tag.m_128441_("NextRallyAvailable")) {
                s.nextRallyAvailableTick = tag.m_128454_("NextRallyAvailable");
            }
            if (tag.m_128441_("PingPos")) {
                s.pingPos = BlockPos.m_122022_((long)tag.m_128454_("PingPos"));
                s.pingExpiry = tag.m_128454_("PingExpiry");
            }
            if (tag.m_128441_("IsLocked")) {
                s.isLocked = tag.m_128471_("IsLocked");
            }
            if (tag.m_128441_("RallyPos")) {
                s.rallyPos = BlockPos.m_122022_((long)tag.m_128454_("RallyPos"));
                s.rallyDimension = tag.m_128441_("RallyDim") ? tag.m_128461_("RallyDim") : dim;
            }
            s.bravoLeader = tag.m_128461_("BravoLeader");
            s.charlieLeader = tag.m_128461_("CharlieLeader");
            if (tag.m_128441_("BravoMembers")) {
                ListTag bList = tag.m_128437_("BravoMembers", 8);
                for (Tag t : bList) {
                    s.bravoMembers.add(t.m_7916_());
                }
            }
            if (tag.m_128441_("CharlieMembers")) {
                ListTag cList = tag.m_128437_("CharlieMembers", 8);
                for (Tag t : cList) {
                    s.charlieMembers.add(t.m_7916_());
                }
            }
            if (tag.m_128441_("BravoPingPos")) {
                s.bravoPingPos = BlockPos.m_122022_((long)tag.m_128454_("BravoPingPos"));
                s.bravoPingExpiry = tag.m_128454_("BravoPingExp");
            }
            if (tag.m_128441_("CharliePingPos")) {
                s.charliePingPos = BlockPos.m_122022_((long)tag.m_128454_("CharliePingPos"));
                s.charliePingExpiry = tag.m_128454_("CharliePingExp");
            }
            if (tag.m_128441_("BravoMarker")) {
                s.bravoMarker = SquadMarker.load(tag.m_128469_("BravoMarker"));
            }
            if (tag.m_128441_("CharlieMarker")) {
                s.charlieMarker = SquadMarker.load(tag.m_128469_("CharlieMarker"));
            }
            if (tag.m_128441_("RhombusList")) {
                ListTag list = tag.m_128437_("RhombusList", 10);
                s.rhombusMarkers.clear();
                for (int i = 0; i < list.size(); ++i) {
                    s.rhombusMarkers.add(SquadMarker.load(list.m_128728_(i)));
                }
            }
            if (tag.m_128441_("Members")) {
                ListTag memList = tag.m_128437_("Members", 8);
                for (Tag t : memList) {
                    s.members.add(t.m_7916_());
                }
            }
            if (s.leader.isEmpty() && !s.members.isEmpty()) {
                s.leader = s.members.get(0);
            }
            if (tag.m_128441_("SquadMarker")) {
                s.marker = SquadMarker.load(tag.m_128469_("SquadMarker"));
            }
            return s;
        }

        public void removeFromFireteams(String pName) {
            if (this.bravoLeader.equals(pName)) {
                this.bravoLeader = "";
            }
            if (this.charlieLeader.equals(pName)) {
                this.charlieLeader = "";
            }
            this.bravoMembers.remove(pName);
            this.charlieMembers.remove(pName);
        }
    }

    public static class ActiveStrike {
        public BlockPos pos;
        public String team;
        public int stage = 0;
        public int ticksLeft;

        public ActiveStrike(BlockPos p, String t) {
            this.pos = p;
            this.team = t;
            this.ticksLeft = 400;
        }
    }

    public static class SquadMarker {
        public int x;
        public int y;
        public int z;
        public int type;
        public long expiryTick;
        public boolean isPhysical;

        public SquadMarker(int x, int y, int z, int type, long expiryTick, boolean isPhysical) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.expiryTick = expiryTick;
            this.isPhysical = isPhysical;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.m_128405_("X", this.x);
            tag.m_128405_("Y", this.y);
            tag.m_128405_("Z", this.z);
            tag.m_128405_("Type", this.type);
            tag.m_128356_("Expiry", this.expiryTick);
            tag.m_128379_("IsPhysical", this.isPhysical);
            return tag;
        }

        public static SquadMarker load(CompoundTag tag) {
            return new SquadMarker(tag.m_128451_("X"), tag.m_128451_("Y"), tag.m_128451_("Z"), tag.m_128451_("Type"), tag.m_128454_("Expiry"), tag.m_128471_("IsPhysical"));
        }
    }
}


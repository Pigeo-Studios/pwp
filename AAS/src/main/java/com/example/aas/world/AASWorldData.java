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
    static final public String[] KIT_NAMES = new String[]{"Officer", "Pilot Officer", "Mechanic Officer", "Scout", "LAT", "HAT", "Sapper", "Sniper", "Marksman", "LMG", "HMG", "Rifleman", "Medic", "Grenadier", "Assault", "Pilot", "Mechanic", "Drone Operator", "Anti_air"};
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

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("BlueTickets", this.blueTickets);
        tag.putInt("RedTickets", this.redTickets);
        tag.putInt("RespawnTimer", this.respawnTimer);
        tag.putInt("DeathTicketCost", this.deathTicketCost);
        tag.putString("BlueFaction", this.blueFaction);
        tag.putString("RedFaction", this.redFaction);
        tag.putBoolean("IsGameStarted", this.isGameStarted);
        tag.putInt("CountdownTicks", this.countdownTicks);
        tag.putBoolean("CountdownActive", this.countdownActive);
        tag.putBoolean("PlayedBlueSiren", this.playedBlueSiren);
        tag.putBoolean("PlayedRedSiren", this.playedRedSiren);
        tag.putInt("MapCenterX", this.mapCenterX);
        tag.putInt("MapCenterZ", this.mapCenterZ);
        tag.putInt("MapSizeBlocks", this.mapSizeBlocks);
        tag.putString("CurrentMapImage", this.currentMapImage);
        tag.putLong("BlueArtCD", this.blueArtStrikeCD);
        tag.putLong("RedArtCD", this.redArtStrikeCD);
        tag.putInt("BlueCMDId", this.blueCMDId);
        tag.putInt("RedCMDId", this.redCMDId);
        tag.putBoolean("BlueCmdActive", this.blueCmdVoteActive);
        tag.putString("BlueCmdCandName", this.blueCmdCandidateName);
        tag.putInt("BlueCmdCandId", this.blueCmdCandidateId);
        tag.putInt("BlueCmdTimer", this.blueCmdVoteTimer);
        CompoundTag blueVotesTag = new CompoundTag();
        this.blueCmdVotes.forEach((uuid, val) -> blueVotesTag.putBoolean(uuid.toString(), val.booleanValue()));
        tag.put("BlueCmdVotesMap", (Tag)blueVotesTag);
        tag.putBoolean("RedCmdActive", this.redCmdVoteActive);
        tag.putString("RedCmdCandName", this.redCmdCandidateName);
        tag.putInt("RedCmdCandId", this.redCmdCandidateId);
        tag.putInt("RedCmdTimer", this.redCmdVoteTimer);
        CompoundTag redVotesTag = new CompoundTag();
        this.redCmdVotes.forEach((uuid, val) -> redVotesTag.putBoolean(uuid.toString(), val.booleanValue()));
        tag.put("RedCmdVotesMap", (Tag)redVotesTag);
        ListTag vehicleList = new ListTag();
        for (VehicleRecord vehicleRecord : this.markedVehicles) {
            vehicleList.add((Object)vehicleRecord.save());
        }
        ListTag triggerList = new ListTag();
        for (BlockPos blockPos : this.triggerBlocks) {
            triggerList.add((Object)LongTag.valueOf((long)blockPos.asLong()));
        }
        tag.put("TriggerBlocks", (Tag)triggerList);
        tag.put("MarkedVehicles", (Tag)vehicleList);
        ListTag listTag = new ListTag();
        for (MapMarker mapMarker : this.activeMarkers) {
            listTag.add((Object)mapMarker.save());
        }
        tag.put("TacticalMarkers", (Tag)listTag);
        ListTag listTag2 = new ListTag();
        for (BlockPos blockPos : this.blueRallies) {
            listTag2.add((Object)LongTag.valueOf((long)blockPos.asLong()));
        }
        tag.put("BlueRallies", (Tag)listTag2);
        ListTag listTag3 = new ListTag();
        for (MainProtectionZone mainProtectionZone : this.mainZones) {
            listTag3.add((Object)mainProtectionZone.save());
        }
        tag.put("MainZones", (Tag)listTag3);
        ListTag listTag4 = new ListTag();
        for (BlockPos blockPos : this.redRallies) {
            listTag4.add((Object)LongTag.valueOf((long)blockPos.asLong()));
        }
        tag.put("RedRallies", (Tag)listTag4);
        ListTag listTag5 = new ListTag();
        for (HubInfo h : this.hubs) {
            listTag5.add((Object)h.save());
        }
        tag.put("HubsData", (Tag)listTag5);
        CompoundTag compoundTag = new CompoundTag();
        this.blueSpawns.forEach((dim, pos) -> blueSpawnsTag.putLong(dim, pos.asLong()));
        tag.put("BlueSpawnsMap", (Tag)compoundTag);
        CompoundTag redSpawnsTag = new CompoundTag();
        this.redSpawns.forEach((dim, pos) -> redSpawnsTag.putLong(dim, pos.asLong()));
        tag.put("RedSpawnsMap", (Tag)redSpawnsTag);
        CompoundTag neutralSpawnsTag = new CompoundTag();
        this.neutralSpawns.forEach((dim, pos) -> neutralSpawnsTag.putLong(dim, pos.asLong()));
        tag.put("NeutralSpawnsMap", (Tag)neutralSpawnsTag);
        ListTag pointsList = new ListTag();
        for (CapturePoint capturePoint : this.capturePoints) {
            pointsList.add((Object)capturePoint.save());
        }
        tag.put("CapturePoints", (Tag)pointsList);
        ListTag squadList = new ListTag();
        for (Squad squad : this.squads) {
            squadList.add((Object)squad.save());
        }
        tag.put("Squads", (Tag)squadList);
        CompoundTag compoundTag2 = new CompoundTag();
        for (KitInfo kitInfo : this.blueKits.values()) {
            compoundTag2.put(kitInfo.name, (Tag)kitInfo.save());
        }
        tag.put("BlueKits", (Tag)compoundTag2);
        CompoundTag compoundTag3 = new CompoundTag();
        for (KitInfo k : this.redKits.values()) {
            compoundTag3.put(k.name, (Tag)k.save());
        }
        tag.put("RedKits", (Tag)compoundTag3);
        if (this.blueArtRequest != null) {
            CompoundTag compoundTag4 = new CompoundTag();
            compoundTag4.putString("Name", this.blueArtRequest.requesterName);
            compoundTag4.putLong("Pos", this.blueArtRequest.pos.asLong());
            compoundTag4.putInt("Timer", this.blueArtRequest.timer);
            tag.put("BlueArtReq", (Tag)compoundTag4);
        }
        if (this.redArtRequest != null) {
            CompoundTag compoundTag5 = new CompoundTag();
            compoundTag5.putString("Name", this.redArtRequest.requesterName);
            compoundTag5.putLong("Pos", this.redArtRequest.pos.asLong());
            compoundTag5.putInt("Timer", this.redArtRequest.timer);
            tag.put("RedArtReq", (Tag)compoundTag5);
        }
        ListTag listTag6 = new ListTag();
        for (ActiveStrike s : this.activeStrikes) {
            CompoundTag sTag = new CompoundTag();
            sTag.putLong("Pos", s.pos.asLong());
            sTag.putString("Team", s.team);
            sTag.putInt("Stage", s.stage);
            sTag.putInt("Ticks", s.ticksLeft);
            listTag6.add((Object)sTag);
        }
        tag.put("ActiveStrikesList", (Tag)listTag6);
        return tag;
    }

    public static AASWorldData load(CompoundTag tag) {
        CompoundTag req;
        CompoundTag map;
        ListTag list;
        CompoundTag cv;
        AASWorldData data = new AASWorldData();
        data.blueTickets = tag.getInt("BlueTickets");
        data.redTickets = tag.getInt("RedTickets");
        data.respawnTimer = tag.getInt("RespawnTimer");
        data.deathTicketCost = tag.getInt("DeathTicketCost");
        data.blueFaction = tag.getString("BlueFaction");
        data.redFaction = tag.getString("RedFaction");
        data.isGameStarted = tag.getBoolean("IsGameStarted");
        data.countdownTicks = tag.getInt("CountdownTicks");
        data.countdownActive = tag.getBoolean("CountdownActive");
        data.playedBlueSiren = tag.getBoolean("PlayedBlueSiren");
        data.playedRedSiren = tag.getBoolean("PlayedRedSiren");
        data.mapCenterX = tag.getInt("MapCenterX");
        data.mapCenterZ = tag.getInt("MapCenterZ");
        data.mapSizeBlocks = tag.contains("MapSizeBlocks") ? tag.getInt("MapSizeBlocks") : 2048;
        data.currentMapImage = tag.contains("CurrentMapImage") ? tag.getString("CurrentMapImage") : "map1";
        data.blueArtStrikeCD = tag.getLong("BlueArtCD");
        data.redArtStrikeCD = tag.getLong("RedArtCD");
        data.blueCMDId = tag.getInt("BlueCMDId");
        data.redCMDId = tag.getInt("RedCMDId");
        data.blueCmdVoteActive = tag.getBoolean("BlueCmdActive");
        data.blueCmdCandidateName = tag.getString("BlueCmdCandName");
        data.blueCmdCandidateId = tag.getInt("BlueCmdCandId");
        data.blueCmdVoteTimer = tag.getInt("BlueCmdTimer");
        if (tag.contains("BlueCmdVotesMap")) {
            cv = tag.getCompound("BlueCmdVotesMap");
            for (String key : cv.getAllKeys()) {
                data.blueCmdVotes.put(UUID.fromString(key), cv.getBoolean(key));
            }
        }
        data.redCmdVoteActive = tag.getBoolean("RedCmdActive");
        data.redCmdCandidateName = tag.getString("RedCmdCandName");
        data.redCmdCandidateId = tag.getInt("RedCmdCandId");
        data.redCmdVoteTimer = tag.getInt("RedCmdTimer");
        if (tag.contains("RedCmdVotesMap")) {
            cv = tag.getCompound("RedCmdVotesMap");
            for (String key : cv.getAllKeys()) {
                data.redCmdVotes.put(UUID.fromString(key), cv.getBoolean(key));
            }
        }
        if (tag.contains("TacticalMarkers")) {
            list = tag.getList("TacticalMarkers", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.activeMarkers.add(MapMarker.load(list.getCompound(i)));
            }
        }
        if (tag.contains("TriggerBlocks")) {
            list = tag.getList("TriggerBlocks", 4);
            for (Tag t : list) {
                data.triggerBlocks.add(BlockPos.of((long)((LongTag)t).getAsLong()));
            }
        }
        if (tag.contains("MainZones")) {
            list = tag.getList("MainZones", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.mainZones.add(MainProtectionZone.load(list.getCompound(i)));
            }
        }
        if (tag.contains("MarkedVehicles")) {
            list = tag.getList("MarkedVehicles", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.markedVehicles.add(VehicleRecord.load(list.getCompound(i)));
            }
        }
        if (tag.contains("BlueRallies")) {
            list = tag.getList("BlueRallies", 4);
            for (Tag t : list) {
                data.blueRallies.add(BlockPos.of((long)((LongTag)t).getAsLong()));
            }
        }
        if (tag.contains("RedRallies")) {
            list = tag.getList("RedRallies", 4);
            for (Tag t : list) {
                data.redRallies.add(BlockPos.of((long)((LongTag)t).getAsLong()));
            }
        }
        if (tag.contains("HubsData")) {
            list = tag.getList("HubsData", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.hubs.add(HubInfo.load(list.getCompound(i)));
            }
        }
        if (tag.contains("BlueSpawnsMap")) {
            map = tag.getCompound("BlueSpawnsMap");
            for (String key : map.getAllKeys()) {
                data.blueSpawns.put(key, BlockPos.of((long)map.getLong(key)));
            }
        }
        if (tag.contains("RedSpawnsMap")) {
            map = tag.getCompound("RedSpawnsMap");
            for (String key : map.getAllKeys()) {
                data.redSpawns.put(key, BlockPos.of((long)map.getLong(key)));
            }
        }
        if (tag.contains("NeutralSpawnsMap")) {
            map = tag.getCompound("NeutralSpawnsMap");
            for (String key : map.getAllKeys()) {
                data.neutralSpawns.put(key, BlockPos.of((long)map.getLong(key)));
            }
        }
        if (tag.contains("CapturePoints")) {
            list = tag.getList("CapturePoints", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.capturePoints.add(CapturePoint.load(list.getCompound(i)));
            }
        }
        if (tag.contains("Squads")) {
            list = tag.getList("Squads", 10);
            for (int i = 0; i < list.size(); ++i) {
                data.squads.add(Squad.load(list.getCompound(i)));
            }
        }
        if (tag.contains("BlueKits")) {
            CompoundTag bk = tag.getCompound("BlueKits");
            for (String key : bk.getAllKeys()) {
                data.blueKits.put(key, KitInfo.load(bk.getCompound(key)));
            }
        }
        if (tag.contains("RedKits")) {
            CompoundTag rk = tag.getCompound("RedKits");
            for (String key : rk.getAllKeys()) {
                data.redKits.put(key, KitInfo.load(rk.getCompound(key)));
            }
        }
        if (tag.contains("BlueArtReq")) {
            req = tag.getCompound("BlueArtReq");
            data.blueArtRequest = new ArtStrikeRequest(req.getString("Name"), BlockPos.of((long)req.getLong("Pos")));
            data.blueArtRequest.timer = req.getInt("Timer");
        }
        if (tag.contains("RedArtReq")) {
            req = tag.getCompound("RedArtReq");
            data.redArtRequest = new ArtStrikeRequest(req.getString("Name"), BlockPos.of((long)req.getLong("Pos")));
            data.redArtRequest.timer = req.getInt("Timer");
        }
        if (tag.contains("ActiveStrikesList")) {
            list = tag.getList("ActiveStrikesList", 10);
            for (int i = 0; i < list.size(); ++i) {
                CompoundTag sTag = list.getCompound(i);
                ActiveStrike s = new ActiveStrike(BlockPos.of((long)sTag.getLong("Pos")), sTag.getString("Team"));
                s.stage = sTag.getInt("Stage");
                s.ticksLeft = sTag.getInt("Ticks");
                data.activeStrikes.add(s);
            }
        }
        return data;
    }

    public static AASWorldData get(ServerLevel level) {
        String dimId = level.dimension().location().toString().replace(":", "_");
        String dataName = "aas_data_" + dimId;
        return (AASWorldData)level.getDataStorage().computeIfAbsent(AASWorldData::load, AASWorldData::new, dataName);
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
        public NonNullList<ItemStack> inventory = NonNullList.withSize(49, (Object)ItemStack.EMPTY);
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
            t.putString("Name", this.name);
            t.putBoolean("LeaderOnly", this.isLeaderOnly);
            t.putInt("MaxTeam", this.maxPerTeam);
            t.putInt("MaxSquad", this.maxPerSquad);
            t.putInt("MinSquadPlayers", this.minSquadPlayers);
            ListTag items = new ListTag();
            for (int i = 0; i < 49; ++i) {
                if (((ItemStack)this.inventory.get(i)).isEmpty()) continue;
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte)i);
                itemTag.putBoolean("Resupply", this.resupplyFlags[i]);
                itemTag.putBoolean("SaveNbt", this.saveNbtFlags[i]);
                ((ItemStack)this.inventory.get(i)).save(itemTag);
                items.add((Object)itemTag);
            }
            t.put("Items", (Tag)items);
            return t;
        }

        public static KitInfo load(CompoundTag t) {
            KitInfo k = new KitInfo(t.getString("Name"));
            k.isLeaderOnly = t.getBoolean("LeaderOnly");
            k.maxPerTeam = t.getInt("MaxTeam");
            k.maxPerSquad = t.getInt("MaxSquad");
            if (t.contains("MinSquadPlayers")) {
                k.minSquadPlayers = t.getInt("MinSquadPlayers");
            }
            ListTag items = t.getList("Items", 10);
            for (int i = 0; i < items.size(); ++i) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getByte("Slot") & 0xFF;
                if (slot < 0 || slot >= 49) continue;
                k.inventory.set(slot, (Object)ItemStack.of((CompoundTag)itemTag));
                k.resupplyFlags[slot] = itemTag.getBoolean("Resupply");
                k.saveNbtFlags[slot] = itemTag.getBoolean("SaveNbt");
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
            tag.putUUID("UUID", this.uuid);
            tag.putString("Team", this.team);
            tag.putString("Type", this.type);
            tag.putDouble("X", this.x);
            tag.putDouble("Y", this.y);
            tag.putDouble("Z", this.z);
            tag.putFloat("Yaw", this.yaw);
            if (this.spawnerPos != null) {
                tag.putLong("SpawnerPos", this.spawnerPos.asLong());
            }
            return tag;
        }

        public static VehicleRecord load(CompoundTag tag) {
            BlockPos sPos = tag.contains("SpawnerPos") ? BlockPos.of((long)tag.getLong("SpawnerPos")) : null;
            return new VehicleRecord(tag.getUUID("UUID"), tag.getString("Team"), tag.getString("Type"), tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"), tag.getFloat("Yaw"), sPos);
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
            tag.putLong("Pos", this.pos.asLong());
            tag.putString("Type", this.type);
            tag.putString("Team", this.team);
            tag.putLong("Expiry", this.expiryTick);
            return tag;
        }

        public static MapMarker load(CompoundTag tag) {
            return new MapMarker(BlockPos.of((long)tag.getLong("Pos")), tag.getString("Type"), tag.getString("Team"), tag.getLong("Expiry"));
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
            tag.putString("Team", this.team);
            tag.putString("Shape", this.shape);
            tag.putDouble("minX", this.area.minX);
            tag.putDouble("minY", this.area.minY);
            tag.putDouble("minZ", this.area.minZ);
            tag.putDouble("maxX", this.area.maxX);
            tag.putDouble("maxY", this.area.maxY);
            tag.putDouble("maxZ", this.area.maxZ);
            return tag;
        }

        public static MainProtectionZone load(CompoundTag tag) {
            AABB aabb = new AABB(tag.getDouble("minX"), tag.getDouble("minY"), tag.getDouble("minZ"), tag.getDouble("maxX"), tag.getDouble("maxY"), tag.getDouble("maxZ"));
            return new MainProtectionZone(tag.getString("Team"), tag.getString("Shape"), aabb);
        }

        public boolean isInside(Vec3 pos) {
            if ("CYLINDER".equalsIgnoreCase(this.shape)) {
                double centerX = (this.area.minX + this.area.maxX) / 2.0;
                double dx = pos.x - centerX;
                double centerZ = (this.area.minZ + this.area.maxZ) / 2.0;
                double dz = pos.z - centerZ;
                double radius = (this.area.maxX - this.area.minX) / 2.0;
                return dx * dx + dz * dz <= radius * radius && pos.y >= this.area.minY && pos.y <= this.area.maxY;
            }
            return this.area.contains(pos);
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
            tag.putLong("Pos", this.pos.asLong());
            tag.putString("Team", this.team);
            tag.putBoolean("Constructed", this.constructed);
            tag.putString("Dimension", this.dimension != null ? this.dimension : "minecraft:overworld");
            tag.putInt("Materials", this.materials);
            return tag;
        }

        public static HubInfo load(CompoundTag tag) {
            BlockPos p = BlockPos.of((long)tag.getLong("Pos"));
            String t = tag.getString("Team");
            boolean c = tag.getBoolean("Constructed");
            String d = tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld";
            HubInfo h = new HubInfo(p, t, c, d);
            if (tag.contains("Materials")) {
                h.materials = tag.getInt("Materials");
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
            tag.putString("Name", this.name);
            tag.putDouble("minX", this.area.minX);
            tag.putDouble("minY", this.area.minY);
            tag.putDouble("minZ", this.area.minZ);
            tag.putDouble("maxX", this.area.maxX);
            tag.putDouble("maxY", this.area.maxY);
            tag.putDouble("maxZ", this.area.maxZ);
            tag.putInt("BluePriority", this.bluePriority);
            tag.putInt("RedPriority", this.redPriority);
            tag.putInt("Time", this.captureTimeMinutes);
            tag.putInt("Penalty", this.ticketPenalty);
            tag.putInt("Deduct", this.captureDeduction);
            tag.putString("Owner", this.owner);
            tag.putFloat("Progress", this.progress);
            tag.putString("CapturingTeam", this.capturingTeam);
            tag.putString("Shape", this.shapeType);
            tag.putInt("LockMin", this.lockDurationMinutes);
            tag.putLong("LockedUntil", this.lockedUntilTick);
            return tag;
        }

        public static CapturePoint load(CompoundTag tag) {
            AABB area = new AABB(tag.getDouble("minX"), tag.getDouble("minY"), tag.getDouble("minZ"), tag.getDouble("maxX"), tag.getDouble("maxY"), tag.getDouble("maxZ"));
            CapturePoint point = new CapturePoint(tag.getString("Name"), area, tag.getInt("BluePriority"), tag.getInt("RedPriority"), tag.getInt("Time"), tag.getInt("Penalty"), tag.getInt("Deduct"), tag.contains("Shape") ? tag.getString("Shape") : "CUBE", tag.contains("LockMin") ? tag.getInt("LockMin") : 0);
            if (tag.contains("Owner")) {
                point.owner = tag.getString("Owner");
            }
            if (tag.contains("Progress")) {
                point.progress = tag.getFloat("Progress");
            }
            if (tag.contains("CapturingTeam")) {
                point.capturingTeam = tag.getString("CapturingTeam");
            }
            if (tag.contains("LockedUntil")) {
                point.lockedUntilTick = tag.getLong("LockedUntil");
            }
            return point;
        }

        public boolean isInside(Vec3 pos) {
            if ("CYLINDER".equalsIgnoreCase(this.shapeType)) {
                double centerX = (this.area.minX + this.area.maxX) / 2.0;
                double dx = pos.x - centerX;
                double centerZ = (this.area.minZ + this.area.maxZ) / 2.0;
                double dz = pos.z - centerZ;
                double radius = (this.area.maxX - this.area.minX) / 2.0;
                boolean inCircle = dx * dx + dz * dz <= radius * radius;
                boolean inHeight = pos.y >= this.area.minY && pos.y <= this.area.maxY;
                return inCircle && inHeight;
            }
            return this.area.contains(pos);
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
            tag.putInt("ID", this.id);
            tag.putString("Name", this.name);
            tag.putString("Team", this.team);
            tag.putString("Leader", this.leader);
            tag.putBoolean("IsLocked", this.isLocked);
            tag.putString("Dimension", this.dimension != null ? this.dimension : "minecraft:overworld");
            tag.putLong("RallyExpiry", this.rallyExpiryTick);
            tag.putLong("SLNoOfficerSince", this.slNoOfficerSince);
            tag.putLong("NextRallyAvailable", this.nextRallyAvailableTick);
            tag.putBoolean("RallyBlocked", this.isRallyBlocked);
            if (this.pingPos != null) {
                tag.putLong("PingPos", this.pingPos.asLong());
                tag.putLong("PingExpiry", this.pingExpiry);
            }
            if (this.rallyPos != null) {
                tag.putLong("RallyPos", this.rallyPos.asLong());
                tag.putString("RallyDim", this.rallyDimension != null ? this.rallyDimension : "minecraft:overworld");
            }
            tag.putString("BravoLeader", this.bravoLeader);
            tag.putString("CharlieLeader", this.charlieLeader);
            ListTag bList = new ListTag();
            for (String string : this.bravoMembers) {
                bList.add((Object)StringTag.valueOf((String)string));
            }
            tag.put("BravoMembers", (Tag)bList);
            ListTag cList = new ListTag();
            for (String string : this.charlieMembers) {
                cList.add((Object)StringTag.valueOf((String)string));
            }
            tag.put("CharlieMembers", (Tag)cList);
            if (this.bravoPingPos != null) {
                tag.putLong("BravoPingPos", this.bravoPingPos.asLong());
                tag.putLong("BravoPingExp", this.bravoPingExpiry);
            }
            if (this.charliePingPos != null) {
                tag.putLong("CharliePingPos", this.charliePingPos.asLong());
                tag.putLong("CharliePingExp", this.charliePingExpiry);
            }
            if (this.bravoMarker != null) {
                tag.put("BravoMarker", (Tag)this.bravoMarker.save());
            }
            if (this.charlieMarker != null) {
                tag.put("CharlieMarker", (Tag)this.charlieMarker.save());
            }
            ListTag listTag = new ListTag();
            for (SquadMarker rm : this.rhombusMarkers) {
                listTag.add((Object)rm.save());
            }
            tag.put("RhombusList", (Tag)listTag);
            ListTag listTag2 = new ListTag();
            for (String m : this.members) {
                listTag2.add((Object)StringTag.valueOf((String)m));
            }
            tag.put("Members", (Tag)listTag2);
            if (this.marker != null) {
                tag.put("SquadMarker", (Tag)this.marker.save());
            }
            return tag;
        }

        public static Squad load(CompoundTag tag) {
            String l = tag.contains("Leader") ? tag.getString("Leader") : "";
            String dim = tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld";
            Squad s = new Squad(tag.getInt("ID"), tag.getString("Name"), tag.getString("Team"), l, dim);
            s.rallyExpiryTick = tag.getLong("RallyExpiry");
            s.slNoOfficerSince = tag.getLong("SLNoOfficerSince");
            s.isRallyBlocked = tag.getBoolean("RallyBlocked");
            if (tag.contains("NextRallyAvailable")) {
                s.nextRallyAvailableTick = tag.getLong("NextRallyAvailable");
            }
            if (tag.contains("PingPos")) {
                s.pingPos = BlockPos.of((long)tag.getLong("PingPos"));
                s.pingExpiry = tag.getLong("PingExpiry");
            }
            if (tag.contains("IsLocked")) {
                s.isLocked = tag.getBoolean("IsLocked");
            }
            if (tag.contains("RallyPos")) {
                s.rallyPos = BlockPos.of((long)tag.getLong("RallyPos"));
                s.rallyDimension = tag.contains("RallyDim") ? tag.getString("RallyDim") : dim;
            }
            s.bravoLeader = tag.getString("BravoLeader");
            s.charlieLeader = tag.getString("CharlieLeader");
            if (tag.contains("BravoMembers")) {
                ListTag bList = tag.getList("BravoMembers", 8);
                for (Tag t : bList) {
                    s.bravoMembers.add(t.getAsString());
                }
            }
            if (tag.contains("CharlieMembers")) {
                ListTag cList = tag.getList("CharlieMembers", 8);
                for (Tag t : cList) {
                    s.charlieMembers.add(t.getAsString());
                }
            }
            if (tag.contains("BravoPingPos")) {
                s.bravoPingPos = BlockPos.of((long)tag.getLong("BravoPingPos"));
                s.bravoPingExpiry = tag.getLong("BravoPingExp");
            }
            if (tag.contains("CharliePingPos")) {
                s.charliePingPos = BlockPos.of((long)tag.getLong("CharliePingPos"));
                s.charliePingExpiry = tag.getLong("CharliePingExp");
            }
            if (tag.contains("BravoMarker")) {
                s.bravoMarker = SquadMarker.load(tag.getCompound("BravoMarker"));
            }
            if (tag.contains("CharlieMarker")) {
                s.charlieMarker = SquadMarker.load(tag.getCompound("CharlieMarker"));
            }
            if (tag.contains("RhombusList")) {
                ListTag list = tag.getList("RhombusList", 10);
                s.rhombusMarkers.clear();
                for (int i = 0; i < list.size(); ++i) {
                    s.rhombusMarkers.add(SquadMarker.load(list.getCompound(i)));
                }
            }
            if (tag.contains("Members")) {
                ListTag memList = tag.getList("Members", 8);
                for (Tag t : memList) {
                    s.members.add(t.getAsString());
                }
            }
            if (s.leader.isEmpty() && !s.members.isEmpty()) {
                s.leader = s.members.get(0);
            }
            if (tag.contains("SquadMarker")) {
                s.marker = SquadMarker.load(tag.getCompound("SquadMarker"));
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
            tag.putInt("X", this.x);
            tag.putInt("Y", this.y);
            tag.putInt("Z", this.z);
            tag.putInt("Type", this.type);
            tag.putLong("Expiry", this.expiryTick);
            tag.putBoolean("IsPhysical", this.isPhysical);
            return tag;
        }

        public static SquadMarker load(CompoundTag tag) {
            return new SquadMarker(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"), tag.getInt("Type"), tag.getLong("Expiry"), tag.getBoolean("IsPhysical"));
        }
    }
}


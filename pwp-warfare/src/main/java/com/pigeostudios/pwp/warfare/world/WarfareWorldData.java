package com.pigeostudios.pwp.warfare.world;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import com.pigeostudios.pwp.warfare.data.FactionVehicleData;
import com.pigeostudios.pwp.warfare.item.VehicleMarkerItem;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

// РЎРѕС…СЂР°РЅСЏРµРјС‹Рµ РґР°РЅРЅС‹Рµ РјРёСЂР° РґР»СЏ РёРіСЂРѕРІРѕРіРѕ СЂРµР¶РёРјР° В«Advance And SecureВ»
// РҐСЂР°РЅРёС‚ СЃРѕСЃС‚РѕСЏРЅРёРµ Р·Р°С…РІР°С‚Р° С‚РѕС‡РµРє, РѕС‚СЂСЏРґРѕРІ, FOB, С‚РµС…РЅРёРєРё Рё РЅР°СЃС‚СЂРѕРµРє РёРіСЂС‹
public class WarfareWorldData extends SavedData {
   public List<WarfareWorldData.CapturePoint> capturePoints = new ArrayList<>();
   public List<BlockPos> blueRallies = new ArrayList<>();
   public List<BlockPos> redRallies = new ArrayList<>();
   public List<WarfareWorldData.HubInfo> hubs = new ArrayList<>();
   public List<WarfareWorldData.MainSupplyInfo> mainSupplies = new ArrayList<>();
   public List<WarfareWorldData.Squad> squads = new ArrayList<>();
   public int blueCMDId = -1;
   public int redCMDId = -1;
   public boolean blueCmdVoteActive = false;
   public String blueCmdCandidateName = "";
   public int blueCmdCandidateId = -1;
   public int blueCmdVoteTimer = 0;
   public Map<UUID, Boolean> blueCmdVotes = new HashMap<>();
   public boolean redCmdVoteActive = false;
   public String redCmdCandidateName = "";
   public int redCmdCandidateId = -1;
   public int redCmdVoteTimer = 0;
   public Map<UUID, Boolean> redCmdVotes = new HashMap<>();
   public long blueArtStrikeCD = 0L;
   public long redArtStrikeCD = 0L;
   public String shapeType = "CUBE";
   public int lockDurationMinutes = 0;
   public long lockedUntilTick = 0L;
   public Map<String, BlockPos> blueSpawns = new HashMap<>();
   public Map<String, BlockPos> redSpawns = new HashMap<>();
   public Map<String, BlockPos> neutralSpawns = new HashMap<>();
    public List<WarfareWorldData.VehicleRecord> markedVehicles = new ArrayList<>();
    public final List<WarfareWorldData.SpawnerInfo> spawnerInfos = new java.util.concurrent.CopyOnWriteArrayList<>();
    public Map<UUID, Set<UUID>> approvedDrivers = new HashMap<>();
   public int blueTickets = 800;
   public int redTickets = 800;
   public int respawnTimer = 10;
   public int deathTicketCost = 2;
   public String blueFaction = "none";
   public String redFaction = "none";
    public boolean isGameStarted = false;
    public int countdownTicks = 0;
    public String gameMode = "aas";
    public String invasionDefender = "RED";
    public int invasionCaptureBonus = 100;
   public boolean countdownActive = false;
   public boolean playedBlueSiren = false;
   public boolean playedRedSiren = false;
    public Map<String, WarfareWorldData.KitInfo> blueKits = new HashMap<>();
    public Map<String, WarfareWorldData.KitInfo> redKits = new HashMap<>();
    public Map<String, FactionVehicleData> blueFactionVehicles = new HashMap<>();
    public Map<String, FactionVehicleData> redFactionVehicles = new HashMap<>();
    public List<ConfigVehicleSpawner> configVehicleSpawners = new ArrayList<>();
    public int mapCenterX = 0;
   public int mapCenterZ = 0;
   public int mapSizeBlocks = 2048;
    public String currentMapImage = "map1";
    public boolean configApplied = false;
    public boolean hideDeathMessages = false;
    public boolean hideNametags = false;
    public boolean disableHunger = false;
    public boolean disableNaturalRegen = false;
    public boolean disableBlockDrops = true;
    public boolean disableEntityDrops = true;
    public boolean disableFireSpread = true;
    public boolean disableWeatherCycle = true;
    public boolean isPaused = false;
    public boolean waitingActive = false;
    public int waitingTimer = 0;
    public boolean invasionSetupActive = false;
    public int invasionSetupTimer = 300;
    public List<String> getAvailableMapImages() {
       List<String> images = new ArrayList<>();
       try {
          java.nio.file.Path mapsDir = java.nio.file.Paths.get("maps");
          if (!java.nio.file.Files.isDirectory(mapsDir)) {
             mapsDir = java.nio.file.Paths.get("..", "maps");
          }
          if (java.nio.file.Files.isDirectory(mapsDir)) {
             java.nio.file.Files.list(mapsDir).filter(java.nio.file.Files::isDirectory).forEach(dir -> {
                java.nio.file.Path cfg = dir.resolve("map_config.json");
                if (java.nio.file.Files.exists(cfg)) {
                   try {
                      String content = new String(java.nio.file.Files.readAllBytes(cfg));
                      int idx = content.indexOf("\"image\"");
                      if (idx != -1) {
                         int start = content.indexOf('"', idx + 7) + 1;
                         int end = content.indexOf('"', start);
                         if (start > 0 && end > start) {
                            images.add(content.substring(start, end));
                         }
                      }
                   } catch (Exception ignored) {}
                }
             });
          }
       } catch (Exception ignored) {}
       return images;
    }

    public static final String[] KIT_NAMES = new String[]{
      "Officer",
      "Pilot Officer",
      "Mechanic Officer",
      "Scout",
      "LAT",
      "HAT",
      "Sapper",
      "Sniper",
      "Marksman",
      "LMG",
      "HMG",
      "Rifleman",
      "Medic",
      "Grenadier",
      "Assault",
      "Pilot",
      "Mechanic",
      "Drone Operator",
      "Anti_air"
   };
   public void loadKitsFromApi(String team, JsonObject apiResponse) {
      if (apiResponse == null || !apiResponse.has("data")) return;
      JsonArray kits = apiResponse.getAsJsonArray("data");
      Map<String, WarfareWorldData.KitInfo> target = team.equalsIgnoreCase("BLUE") ? this.blueKits : this.redKits;
      target.clear(); // API — источник истины: старые киты из NBT мира не должны оставаться
      for (int i = 0; i < kits.size(); i++) {
         JsonObject kitJson = kits.get(i).getAsJsonObject();
         String kitName = kitJson.get("kitName").getAsString();
         target.put(kitName, WarfareWorldData.KitInfo.loadFromJson(kitJson));
      }
   }

   public void loadKitsFromApi(String team, String jsonString) {
      try {
         Gson gson = new Gson();
         loadKitsFromApi(team, gson.fromJson(jsonString, JsonObject.class));
      } catch (Exception ignored) {}
   }

    public void loadFactionVehiclesFromApi(String team, JsonObject apiResponse) {
        if (apiResponse == null || !apiResponse.has("data")) return;
        JsonArray arr = apiResponse.getAsJsonArray("data");
        Map<String, FactionVehicleData> target = team.equalsIgnoreCase("BLUE") ? this.blueFactionVehicles : this.redFactionVehicles;
        target.clear();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            FactionVehicleData veh = new FactionVehicleData();
            veh.faction = obj.has("faction") ? obj.get("faction").getAsString() : "";
            veh.vehicleName = obj.has("vehicleName") ? obj.get("vehicleName").getAsString() : "";
            veh.displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : "";
            veh.vehicleId = obj.has("vehicleId") ? obj.get("vehicleId").getAsString() : "";
            veh.yaw = obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0;
            veh.respawnTime = obj.has("respawnTime") ? obj.get("respawnTime").getAsInt() : 60;
            veh.initialTime = obj.has("initialTime") ? obj.get("initialTime").getAsInt() : 60;
            if (obj.has("inventory")) {
                try {
                    JsonArray invArr = obj.getAsJsonArray("inventory");
                    for (int j = 0; j < invArr.size(); j++) {
                        JsonObject itemJson = invArr.get(j).getAsJsonObject();
                        int slot = itemJson.get("slot").getAsInt();
                        if (slot >= 0 && slot < 33 && itemJson.has("item")) {
                            JsonObject itemData = itemJson.getAsJsonObject("item");
                            String id = itemData.has("id") ? itemData.get("id").getAsString() : "";
                            int count = itemData.has("Count") ? itemData.get("Count").getAsInt() : 1;
                            if (!id.isEmpty() && !id.equals("minecraft:air")) {
                                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id));
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, count);
                                    if (itemData.has("tag") && itemData.get("tag").isJsonObject()) {
                                        var tag = WarfareWorldData.KitInfo.jsonToCompound(itemData.getAsJsonObject("tag"));
                                        if (!tag.isEmpty()) stack.setTag(tag);
                                    }
                                    veh.inventory.set(slot, stack);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            target.put(veh.vehicleName, veh);
        }
    }

    public void parseConfigVehicleSpawners(JsonArray spawners, String team, ServerLevel level) {
        if (spawners == null) return;
        for (var el : spawners) {
            JsonObject obj = el.getAsJsonObject();
            ConfigVehicleSpawner spawner = new ConfigVehicleSpawner();
            spawner.team = team;
            spawner.vehicleName = obj.get("vehicleName").getAsString();
            spawner.x = obj.get("x").getAsInt();
            spawner.y = obj.get("y").getAsInt();
            spawner.z = obj.get("z").getAsInt();
            if (obj.has("yaw")) spawner.yaw = obj.get("yaw").getAsFloat();
            spawner.fixed = obj.has("fixed") && obj.get("fixed").getAsBoolean();
            configVehicleSpawners.add(spawner);
        }
    }

    public void fillVehicleSpawnersFromFactionDefaults(ServerLevel level) {
        Map<String, FactionVehicleData> blueVehicles = this.blueFactionVehicles;
        Map<String, FactionVehicleData> redVehicles = this.redFactionVehicles;

        for (ConfigVehicleSpawner cfg : configVehicleSpawners) {
            BlockPos pos = new BlockPos(cfg.x, cfg.y, cfg.z);
            Map<String, FactionVehicleData> factionData = cfg.team.equalsIgnoreCase("BLUE") ? blueVehicles : redVehicles;
            FactionVehicleData veh = factionData.get(cfg.vehicleName);
            if (veh == null) continue;

            if (level.isLoaded(pos)) {
                var be = level.getBlockEntity(pos);
                if (be instanceof VehicleSpawnerBlockEntity spawner) {
                    if (!cfg.fixed) {
                        spawner.vehicleName = cfg.vehicleName;
                        spawner.loadDefaultsFromFactionVehicle(veh);
                        if (cfg.yaw != 0) spawner.vehicleYaw = cfg.yaw;
                    }
                } else {
                    level.setBlock(pos, com.pigeostudios.pwp.warfare.block.ModBlocks.VEHICLE_SPAWNER_BLOCK.get().defaultBlockState(), 3);
                    be = level.getBlockEntity(pos);
                    if (be instanceof VehicleSpawnerBlockEntity spawner) {
                        spawner.vehicleName = cfg.vehicleName;
                        spawner.loadDefaultsFromFactionVehicle(veh);
                        if (cfg.yaw != 0) spawner.vehicleYaw = cfg.yaw;
                    }
                }
            }
        }
    }

        public List<BlockPos> triggerBlocks = new ArrayList<>();
   public WarfareWorldData.ArtStrikeRequest blueArtRequest = null;
   public WarfareWorldData.ArtStrikeRequest redArtRequest = null;
   public List<WarfareWorldData.ActiveStrike> activeStrikes = new ArrayList<>();
    public List<WarfareWorldData.MapMarker> activeMarkers = new ArrayList<>();
    public List<WarfareWorldData.MainProtectionZone> mainZones = new ArrayList<>();
    public final List<MarkerSnapshot> savedMarkers = new ArrayList<>();
    public final List<PathSnapshot> savedPaths = new ArrayList<>();

    public record MarkerSnapshot(UUID id, String team, String category, String iconType, BlockPos pos, UUID ownerUUID, long createdAt) {
        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id); t.putString("Team", team); t.putString("Category", category);
            t.putString("Icon", iconType); t.putLong("Pos", pos.asLong());
            t.putUUID("Owner", ownerUUID); t.putLong("Created", createdAt);
            return t;
        }
        public static MarkerSnapshot load(CompoundTag t) {
            return new MarkerSnapshot(t.getUUID("Id"), t.getString("Team"), t.getString("Category"),
                t.getString("Icon"), BlockPos.of(t.getLong("Pos")), t.getUUID("Owner"), t.getLong("Created"));
        }
    }

    public record PathSnapshot(UUID id, String owner, String team, String type, int squadNum, long createdAt, List<PathPoint> points) {
        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id); t.putString("Owner", owner); t.putString("Team", team);
            t.putString("Type", type); t.putInt("SquadNum", squadNum); t.putLong("Created", createdAt);
            ListTag ptList = new ListTag();
            for (var p : points) { CompoundTag pt = new CompoundTag(); pt.putDouble("X", p.x); pt.putDouble("Z", p.z); ptList.add(pt); }
            t.put("Points", ptList);
            return t;
        }
        public static PathSnapshot load(CompoundTag t) {
            UUID id = t.getUUID("Id"); String owner = t.getString("Owner"); String team = t.getString("Team");
            String type = t.getString("Type"); int squadNum = t.getInt("SquadNum");
            long createdAt = t.getLong("Created");
            List<PathPoint> pts = new ArrayList<>();
            ListTag ptList = t.getList("Points", 10);
            for (int i = 0; i < ptList.size(); i++) {
                CompoundTag pt = ptList.getCompound(i);
                pts.add(new PathPoint(pt.getDouble("X"), pt.getDouble("Z")));
            }
            return new PathSnapshot(id, owner, team, type, squadNum, createdAt, pts);
        }
    }
   public boolean voteActive = false;
   public int voteTimer = 0;
   public boolean blueReady = false;
   public boolean redReady = false;
   public Map<UUID, Boolean> votes = new HashMap<>();

    public boolean hasApprovedDriver(UUID vehicleUUID, UUID playerUUID) {
       Set<UUID> drivers = approvedDrivers.get(vehicleUUID);
       return drivers != null && drivers.contains(playerUUID);
    }

    public void addApprovedDriver(UUID vehicleUUID, UUID playerUUID) {
       approvedDrivers.computeIfAbsent(vehicleUUID, k -> new HashSet<>()).add(playerUUID);
       setDirty();
    }

    public void removeApprovedDrivers(UUID vehicleUUID) {
       approvedDrivers.remove(vehicleUUID);
       setDirty();
    }

    public WarfareWorldData() {
      for (String name : KIT_NAMES) {
         this.blueKits.put(name, new WarfareWorldData.KitInfo(name));
         this.redKits.put(name, new WarfareWorldData.KitInfo(name));
      }
   }

    public CompoundTag save(CompoundTag tag) {
       com.pigeostudios.pwp.warfare.server.MarkerManager.saveTo(this);
       com.pigeostudios.pwp.warfare.server.PathManager.saveTo(this);
       tag.putInt("BlueTickets", this.blueTickets);
      tag.putInt("RedTickets", this.redTickets);
      tag.putInt("RespawnTimer", this.respawnTimer);
      tag.putInt("DeathTicketCost", this.deathTicketCost);
      tag.putString("BlueFaction", this.blueFaction);
      tag.putString("RedFaction", this.redFaction);
      tag.putBoolean("IsGameStarted", this.isGameStarted);
      tag.putInt("CountdownTicks", this.countdownTicks);
      tag.putBoolean("CountdownActive", this.countdownActive);
      tag.putString("GameMode", this.gameMode != null ? this.gameMode : "aas");
      tag.putString("InvasionDefender", this.invasionDefender != null ? this.invasionDefender : "RED");
      tag.putInt("InvasionCapBonus", this.invasionCaptureBonus);
      tag.putBoolean("PlayedBlueSiren", this.playedBlueSiren);
      tag.putBoolean("PlayedRedSiren", this.playedRedSiren);
      tag.putInt("MapCenterX", this.mapCenterX);
      tag.putInt("MapCenterZ", this.mapCenterZ);
      tag.putInt("MapSizeBlocks", this.mapSizeBlocks);
      tag.putString("CurrentMapImage", this.currentMapImage);
      tag.putBoolean("HideDeathMessages", this.hideDeathMessages);
      tag.putBoolean("HideNametags", this.hideNametags);
      tag.putBoolean("DisableHunger", this.disableHunger);
      tag.putBoolean("DisableNaturalRegen", this.disableNaturalRegen);
      tag.putBoolean("DisableBlockDrops", this.disableBlockDrops);
      tag.putBoolean("DisableEntityDrops", this.disableEntityDrops);
      tag.putBoolean("DisableFireSpread", this.disableFireSpread);
      tag.putBoolean("DisableWeatherCycle", this.disableWeatherCycle);
      tag.putBoolean("IsPaused", this.isPaused);
      tag.putBoolean("WaitingActive", this.waitingActive);
      tag.putInt("WaitingTimer", this.waitingTimer);
      tag.putBoolean("InvasionSetupActive", this.invasionSetupActive);
      tag.putInt("InvasionSetupTimer", this.invasionSetupTimer);
      tag.putLong("BlueArtCD", this.blueArtStrikeCD);
      tag.putLong("RedArtCD", this.redArtStrikeCD);
      tag.putInt("BlueCMDId", this.blueCMDId);
      tag.putInt("RedCMDId", this.redCMDId);
      tag.putBoolean("BlueCmdActive", this.blueCmdVoteActive);
      tag.putString("BlueCmdCandName", this.blueCmdCandidateName);
      tag.putInt("BlueCmdCandId", this.blueCmdCandidateId);
      tag.putInt("BlueCmdTimer", this.blueCmdVoteTimer);
      CompoundTag blueVotesTag = new CompoundTag();
      this.blueCmdVotes.forEach((uuid, val) -> blueVotesTag.putBoolean(uuid.toString(), val));
      tag.put("BlueCmdVotesMap", blueVotesTag);
      tag.putBoolean("RedCmdActive", this.redCmdVoteActive);
      tag.putString("RedCmdCandName", this.redCmdCandidateName);
      tag.putInt("RedCmdCandId", this.redCmdCandidateId);
      tag.putInt("RedCmdTimer", this.redCmdVoteTimer);
      CompoundTag redVotesTag = new CompoundTag();
      this.redCmdVotes.forEach((uuid, val) -> redVotesTag.putBoolean(uuid.toString(), val));
      tag.put("RedCmdVotesMap", redVotesTag);
      ListTag vehicleList = new ListTag();

      for (WarfareWorldData.VehicleRecord v : this.markedVehicles) {
         vehicleList.add(v.save());
      }

      ListTag triggerList = new ListTag();

      for (BlockPos p : this.triggerBlocks) {
         triggerList.add(LongTag.valueOf(p.asLong()));
      }

      tag.put("TriggerBlocks", triggerList);
      tag.put("MarkedVehicles", vehicleList);
      ListTag markerList = new ListTag();

      for (WarfareWorldData.MapMarker m : this.activeMarkers) {
         markerList.add(m.save());
      }

      tag.put("TacticalMarkers", markerList);
      ListTag pwpMarkerList = new ListTag();
      for (MarkerSnapshot m : this.savedMarkers) pwpMarkerList.add(m.save());
      tag.put("PwpMarkers", pwpMarkerList);
      ListTag pathList = new ListTag();
      for (PathSnapshot p : this.savedPaths) pathList.add(p.save());
      tag.put("PwpPaths", pathList);
      ListTag blueList = new ListTag();

      for (BlockPos pos : this.blueRallies) {
         blueList.add(LongTag.valueOf(pos.asLong()));
      }

      tag.put("BlueRallies", blueList);
      ListTag mainZoneList = new ListTag();

      for (WarfareWorldData.MainProtectionZone zone : this.mainZones) {
         mainZoneList.add(zone.save());
      }

      tag.put("MainZones", mainZoneList);
      ListTag redList = new ListTag();

      for (BlockPos pos : this.redRallies) {
         redList.add(LongTag.valueOf(pos.asLong()));
      }

      tag.put("RedRallies", redList);
      ListTag hubList = new ListTag();

      for (WarfareWorldData.HubInfo h : this.hubs) {
         hubList.add(h.save());
      }

      tag.put("HubsData", hubList);
      ListTag supplyList = new ListTag();

      for (WarfareWorldData.MainSupplyInfo s : this.mainSupplies) {
         supplyList.add(s.save());
      }

      tag.put("MainSupplies", supplyList);
      CompoundTag blueSpawnsTag = new CompoundTag();
      this.blueSpawns.forEach((dim, pos) -> blueSpawnsTag.putLong(dim, pos.asLong()));
      tag.put("BlueSpawnsMap", blueSpawnsTag);
      CompoundTag redSpawnsTag = new CompoundTag();
      this.redSpawns.forEach((dim, pos) -> redSpawnsTag.putLong(dim, pos.asLong()));
      tag.put("RedSpawnsMap", redSpawnsTag);
      CompoundTag neutralSpawnsTag = new CompoundTag();
      this.neutralSpawns.forEach((dim, pos) -> neutralSpawnsTag.putLong(dim, pos.asLong()));
      tag.put("NeutralSpawnsMap", neutralSpawnsTag);
      ListTag pointsList = new ListTag();

      for (WarfareWorldData.CapturePoint point : this.capturePoints) {
         pointsList.add(point.save());
      }

      tag.put("CapturePoints", pointsList);
      ListTag squadList = new ListTag();

      for (WarfareWorldData.Squad s : this.squads) {
         squadList.add(s.save());
      }

      tag.put("Squads", squadList);
      CompoundTag bKitsTag = new CompoundTag();

      for (WarfareWorldData.KitInfo k : this.blueKits.values()) {
         bKitsTag.put(k.name, k.save());
      }

      tag.put("BlueKits", bKitsTag);
      CompoundTag rKitsTag = new CompoundTag();

      for (WarfareWorldData.KitInfo k : this.redKits.values()) {
         rKitsTag.put(k.name, k.save());
      }

      tag.put("RedKits", rKitsTag);
      if (this.blueArtRequest != null) {
         CompoundTag req = new CompoundTag();
         req.putString("Name", this.blueArtRequest.requesterName);
         req.putLong("Pos", this.blueArtRequest.pos.asLong());
         req.putInt("Timer", this.blueArtRequest.timer);
         tag.put("BlueArtReq", req);
      }

      if (this.redArtRequest != null) {
         CompoundTag req = new CompoundTag();
         req.putString("Name", this.redArtRequest.requesterName);
         req.putLong("Pos", this.redArtRequest.pos.asLong());
         req.putInt("Timer", this.redArtRequest.timer);
         tag.put("RedArtReq", req);
      }

      ListTag artList = new ListTag();

      for (WarfareWorldData.ActiveStrike s : this.activeStrikes) {
         CompoundTag sTag = new CompoundTag();
         sTag.putLong("Pos", s.pos.asLong());
         sTag.putString("Team", s.team);
         sTag.putInt("Stage", s.stage);
         sTag.putInt("Ticks", s.ticksLeft);
         artList.add(sTag);
      }

      tag.put("ActiveStrikesList", artList);
      return tag;
   }

   public static WarfareWorldData load(CompoundTag tag) {
      WarfareWorldData data = new WarfareWorldData();
      data.blueTickets = tag.getInt("BlueTickets");
      data.redTickets = tag.getInt("RedTickets");
      data.respawnTimer = tag.getInt("RespawnTimer");
      data.deathTicketCost = tag.getInt("DeathTicketCost");
      data.blueFaction = tag.getString("BlueFaction");
      data.redFaction = tag.getString("RedFaction");
      data.isGameStarted = tag.getBoolean("IsGameStarted");
      data.countdownTicks = tag.getInt("CountdownTicks");
      data.countdownActive = tag.getBoolean("CountdownActive");
      data.gameMode = tag.contains("GameMode") ? tag.getString("GameMode") : "aas";
      data.invasionDefender = tag.contains("InvasionDefender") ? tag.getString("InvasionDefender") : "RED";
      data.invasionCaptureBonus = tag.contains("InvasionCapBonus") ? tag.getInt("InvasionCapBonus") : 100;
      data.playedBlueSiren = tag.getBoolean("PlayedBlueSiren");
      data.playedRedSiren = tag.getBoolean("PlayedRedSiren");
      data.mapCenterX = tag.getInt("MapCenterX");
      data.mapCenterZ = tag.getInt("MapCenterZ");
      data.mapSizeBlocks = tag.contains("MapSizeBlocks") ? tag.getInt("MapSizeBlocks") : 2048;
      data.currentMapImage = tag.contains("CurrentMapImage") ? tag.getString("CurrentMapImage") : "map1";
      data.hideDeathMessages = tag.contains("HideDeathMessages") && tag.getBoolean("HideDeathMessages");
      data.hideNametags = tag.contains("HideNametags") && tag.getBoolean("HideNametags");
      data.disableHunger = tag.contains("DisableHunger") && tag.getBoolean("DisableHunger");
      data.disableNaturalRegen = tag.contains("DisableNaturalRegen") && tag.getBoolean("DisableNaturalRegen");
      data.disableBlockDrops = !tag.contains("DisableBlockDrops") || tag.getBoolean("DisableBlockDrops");
      data.disableEntityDrops = !tag.contains("DisableEntityDrops") || tag.getBoolean("DisableEntityDrops");
      data.disableFireSpread = !tag.contains("DisableFireSpread") || tag.getBoolean("DisableFireSpread");
      data.disableWeatherCycle = !tag.contains("DisableWeatherCycle") || tag.getBoolean("DisableWeatherCycle");
      data.isPaused = tag.contains("IsPaused") && tag.getBoolean("IsPaused");
      data.waitingActive = tag.contains("WaitingActive") && tag.getBoolean("WaitingActive");
      data.waitingTimer = tag.contains("WaitingTimer") ? tag.getInt("WaitingTimer") : 0;
      data.invasionSetupActive = tag.contains("InvasionSetupActive") && tag.getBoolean("InvasionSetupActive");
      data.invasionSetupTimer = tag.contains("InvasionSetupTimer") ? tag.getInt("InvasionSetupTimer") : 300;
      data.blueArtStrikeCD = tag.getLong("BlueArtCD");
      data.redArtStrikeCD = tag.getLong("RedArtCD");
      data.blueCMDId = tag.getInt("BlueCMDId");
      data.redCMDId = tag.getInt("RedCMDId");
      data.blueCmdVoteActive = tag.getBoolean("BlueCmdActive");
      data.blueCmdCandidateName = tag.getString("BlueCmdCandName");
      data.blueCmdCandidateId = tag.getInt("BlueCmdCandId");
      data.blueCmdVoteTimer = tag.getInt("BlueCmdTimer");
      if (tag.contains("BlueCmdVotesMap")) {
         CompoundTag cv = tag.getCompound("BlueCmdVotesMap");

         for (String key : cv.getAllKeys()) {
            data.blueCmdVotes.put(UUID.fromString(key), cv.getBoolean(key));
         }
      }

      data.redCmdVoteActive = tag.getBoolean("RedCmdActive");
      data.redCmdCandidateName = tag.getString("RedCmdCandName");
      data.redCmdCandidateId = tag.getInt("RedCmdCandId");
      data.redCmdVoteTimer = tag.getInt("RedCmdTimer");
      if (tag.contains("RedCmdVotesMap")) {
         CompoundTag cv = tag.getCompound("RedCmdVotesMap");

         for (String key : cv.getAllKeys()) {
            data.redCmdVotes.put(UUID.fromString(key), cv.getBoolean(key));
         }
      }

      if (tag.contains("TacticalMarkers")) {
         ListTag list = tag.getList("TacticalMarkers", 10);

         for (int i = 0; i < list.size(); i++) {
            data.activeMarkers.add(WarfareWorldData.MapMarker.load(list.getCompound(i)));
         }
      }

      if (tag.contains("PwpMarkers")) {
         ListTag list = tag.getList("PwpMarkers", 10);
         for (int i = 0; i < list.size(); i++) data.savedMarkers.add(MarkerSnapshot.load(list.getCompound(i)));
      }
      if (tag.contains("PwpPaths")) {
         ListTag list = tag.getList("PwpPaths", 10);
         for (int i = 0; i < list.size(); i++) data.savedPaths.add(PathSnapshot.load(list.getCompound(i)));
      }

      if (tag.contains("TriggerBlocks")) {
         for (Tag t : tag.getList("TriggerBlocks", 4)) {
            data.triggerBlocks.add(BlockPos.of(((LongTag)t).getAsLong()));
         }
      }

      if (tag.contains("MainZones")) {
         ListTag list = tag.getList("MainZones", 10);

         for (int i = 0; i < list.size(); i++) {
            data.mainZones.add(WarfareWorldData.MainProtectionZone.load(list.getCompound(i)));
         }
      }

      if (tag.contains("MarkedVehicles")) {
         ListTag list = tag.getList("MarkedVehicles", 10);

         for (int i = 0; i < list.size(); i++) {
            data.markedVehicles.add(WarfareWorldData.VehicleRecord.load(list.getCompound(i)));
         }
      }

      if (tag.contains("BlueRallies")) {
         for (Tag t : tag.getList("BlueRallies", 4)) {
            data.blueRallies.add(BlockPos.of(((LongTag)t).getAsLong()));
         }
      }

      if (tag.contains("RedRallies")) {
         for (Tag t : tag.getList("RedRallies", 4)) {
            data.redRallies.add(BlockPos.of(((LongTag)t).getAsLong()));
         }
      }

      if (tag.contains("HubsData")) {
         ListTag list = tag.getList("HubsData", 10);

         for (int i = 0; i < list.size(); i++) {
            data.hubs.add(WarfareWorldData.HubInfo.load(list.getCompound(i)));
         }
      }

      if (tag.contains("MainSupplies")) {
         ListTag list = tag.getList("MainSupplies", 10);

         for (int i = 0; i < list.size(); i++) {
            data.mainSupplies.add(WarfareWorldData.MainSupplyInfo.load(list.getCompound(i)));
         }
      }

      if (tag.contains("BlueSpawnsMap")) {
         CompoundTag map = tag.getCompound("BlueSpawnsMap");

         for (String key : map.getAllKeys()) {
            data.blueSpawns.put(key, BlockPos.of(map.getLong(key)));
         }
      }

      if (tag.contains("RedSpawnsMap")) {
         CompoundTag map = tag.getCompound("RedSpawnsMap");

         for (String key : map.getAllKeys()) {
            data.redSpawns.put(key, BlockPos.of(map.getLong(key)));
         }
      }

      if (tag.contains("NeutralSpawnsMap")) {
         CompoundTag map = tag.getCompound("NeutralSpawnsMap");

         for (String key : map.getAllKeys()) {
            data.neutralSpawns.put(key, BlockPos.of(map.getLong(key)));
         }
      }

      if (tag.contains("CapturePoints")) {
         ListTag list = tag.getList("CapturePoints", 10);

         for (int i = 0; i < list.size(); i++) {
            data.capturePoints.add(WarfareWorldData.CapturePoint.load(list.getCompound(i)));
         }
      }

      if (tag.contains("Squads")) {
         ListTag list = tag.getList("Squads", 10);

         for (int i = 0; i < list.size(); i++) {
            data.squads.add(WarfareWorldData.Squad.load(list.getCompound(i)));
         }
      }

      if (tag.contains("BlueKits")) {
         CompoundTag bk = tag.getCompound("BlueKits");

         for (String key : bk.getAllKeys()) {
            data.blueKits.put(key, WarfareWorldData.KitInfo.load(bk.getCompound(key)));
         }
      }

      if (tag.contains("RedKits")) {
         CompoundTag rk = tag.getCompound("RedKits");

         for (String key : rk.getAllKeys()) {
            data.redKits.put(key, WarfareWorldData.KitInfo.load(rk.getCompound(key)));
         }
      }

      if (tag.contains("BlueArtReq")) {
         CompoundTag req = tag.getCompound("BlueArtReq");
         data.blueArtRequest = new WarfareWorldData.ArtStrikeRequest(req.getString("Name"), BlockPos.of(req.getLong("Pos")));
         data.blueArtRequest.timer = req.getInt("Timer");
      }

      if (tag.contains("RedArtReq")) {
         CompoundTag req = tag.getCompound("RedArtReq");
         data.redArtRequest = new WarfareWorldData.ArtStrikeRequest(req.getString("Name"), BlockPos.of(req.getLong("Pos")));
         data.redArtRequest.timer = req.getInt("Timer");
      }

      if (tag.contains("ActiveStrikesList")) {
         ListTag list = tag.getList("ActiveStrikesList", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag sTag = list.getCompound(i);
            WarfareWorldData.ActiveStrike s = new WarfareWorldData.ActiveStrike(BlockPos.of(sTag.getLong("Pos")), sTag.getString("Team"));
            s.stage = sTag.getInt("Stage");
            s.ticksLeft = sTag.getInt("Ticks");
            data.activeStrikes.add(s);
         }
      }

      com.pigeostudios.pwp.warfare.server.MarkerManager.loadFrom(data);
      com.pigeostudios.pwp.warfare.server.PathManager.loadFrom(data);
      return data;
   }

   public static WarfareWorldData get(ServerLevel level) {
      String dimId = level.dimension().location().toString().replace(":", "_");
      String dataName = "WARFARE_data_" + dimId;
      return (WarfareWorldData)level.getDataStorage().computeIfAbsent(WarfareWorldData::load, WarfareWorldData::new, dataName);
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

   public static class ArtStrikeRequest {
      public String requesterName;
      public BlockPos pos;
      public int timer = 200;

      public ArtStrikeRequest(String name, BlockPos p) {
         this.requesterName = name;
         this.pos = p;
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
      public float progress = 0.0F;
      public String capturingTeam = "NONE";
      public String shapeType = "CUBE";
       public int lockDurationMinutes = 0;
       public long lockedUntilTick = 0L;
       public boolean invLocked = false;
       // Тикеты, которые команда получит за нейтрализацию/захват этой точки
       public int ticketGainNeutralize = 0;
       public int ticketGainCapture = 0;

       public CapturePoint(String name, AABB area, int bp, int rp, int time, int penalty, int deduct, String shape, int lockMin, int gainNeut, int gainCap) {
          this.name = name;
          this.area = area;
          this.bluePriority = bp;
          this.redPriority = rp;
          this.captureTimeMinutes = time;
          this.ticketPenalty = penalty;
          this.captureDeduction = deduct;
          this.shapeType = shape;
          this.lockDurationMinutes = lockMin;
          this.ticketGainNeutralize = gainNeut;
          this.ticketGainCapture = gainCap;
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
          tag.putInt("GainNeut", this.ticketGainNeutralize);
          tag.putInt("GainCap", this.ticketGainCapture);
          tag.putString("Owner", this.owner);
         tag.putFloat("Progress", this.progress);
         tag.putString("CapturingTeam", this.capturingTeam);
          tag.putString("Shape", this.shapeType);
          tag.putInt("LockMin", this.lockDurationMinutes);
          tag.putLong("LockedUntil", this.lockedUntilTick);
          tag.putBoolean("InvLocked", this.invLocked);
          return tag;
      }

      public static WarfareWorldData.CapturePoint load(CompoundTag tag) {
         AABB area = new AABB(
            tag.getDouble("minX"), tag.getDouble("minY"), tag.getDouble("minZ"), tag.getDouble("maxX"), tag.getDouble("maxY"), tag.getDouble("maxZ")
         );
         WarfareWorldData.CapturePoint point = new WarfareWorldData.CapturePoint(
            tag.getString("Name"),
            area,
            tag.getInt("BluePriority"),
            tag.getInt("RedPriority"),
            tag.getInt("Time"),
            tag.getInt("Penalty"),
            tag.getInt("Deduct"),
            tag.contains("Shape") ? tag.getString("Shape") : "CUBE",
            tag.contains("LockMin") ? tag.getInt("LockMin") : 0,
            tag.contains("GainNeut") ? tag.getInt("GainNeut") : 0,
            tag.contains("GainCap") ? tag.getInt("GainCap") : 0
         );
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

          if (tag.contains("InvLocked")) {
             point.invLocked = tag.getBoolean("InvLocked");
          }

          return point;
      }

      public boolean isInside(Vec3 pos) {
         if ("CYLINDER".equalsIgnoreCase(this.shapeType)) {
            double centerX = (this.area.minX + this.area.maxX) / 2.0;
            double centerZ = (this.area.minZ + this.area.maxZ) / 2.0;
            double radius = (this.area.maxX - this.area.minX) / 2.0;
            double dx = pos.x - centerX;
            double dz = pos.z - centerZ;
            boolean inCircle = dx * dx + dz * dz <= radius * radius;
            boolean inHeight = pos.y >= this.area.minY && pos.y <= this.area.maxY;
            return inCircle && inHeight;
         } else {
            return this.area.contains(pos);
         }
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

      public static WarfareWorldData.HubInfo load(CompoundTag tag) {
         BlockPos p = BlockPos.of(tag.getLong("Pos"));
         String t = tag.getString("Team");
         boolean c = tag.getBoolean("Constructed");
         String d = tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld";
         WarfareWorldData.HubInfo h = new WarfareWorldData.HubInfo(p, t, c, d);
         if (tag.contains("Materials")) {
            h.materials = tag.getInt("Materials");
         }

         return h;
      }
   }

   public static class MainSupplyInfo {
      public BlockPos pos;
      public String team;
      public String dimension;

      public MainSupplyInfo(BlockPos pos, String team, String dimension) {
         this.pos = pos;
         this.team = team;
         this.dimension = dimension;
      }

      public CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putLong("Pos", this.pos.asLong());
         tag.putString("Team", this.team);
         tag.putString("Dimension", this.dimension != null ? this.dimension : "minecraft:overworld");
         return tag;
      }

      public static WarfareWorldData.MainSupplyInfo load(CompoundTag tag) {
         BlockPos p = BlockPos.of(tag.getLong("Pos"));
         String t = tag.getString("Team");
         String d = tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld";
         return new WarfareWorldData.MainSupplyInfo(p, t, d);
      }
   }

    public static class KitInfo {
       public String name;
       public String category = "INFANTRY";
       public String description = "";
       public NonNullList<ItemStack> inventory = NonNullList.withSize(49, ItemStack.EMPTY);
       public boolean[] resupplyFlags = new boolean[49];
       public boolean[] saveNbtFlags = new boolean[49];
       public boolean isLeaderOnly = false;
       public int maxPerTeam = -1;
       public int maxPerSquad = -1;
       public int minSquadPlayers = 0;
       public Map<Integer, List<String>> slotSkins = new HashMap<>();

      public KitInfo(String name) {
         this.name = name;
      }

      public CompoundTag save() {
         CompoundTag t = new CompoundTag();
          t.putString("Name", this.name);
          t.putString("Category", this.category);
          t.putString("Description", this.description);
          t.putBoolean("LeaderOnly", this.isLeaderOnly);
         t.putInt("MaxTeam", this.maxPerTeam);
         t.putInt("MaxSquad", this.maxPerSquad);
         t.putInt("MinSquadPlayers", this.minSquadPlayers);
         ListTag items = new ListTag();

         for (int i = 0; i < 49; i++) {
            if (!((ItemStack)this.inventory.get(i)).isEmpty()) {
               CompoundTag itemTag = new CompoundTag();
               itemTag.putByte("Slot", (byte)i);
               itemTag.putBoolean("Resupply", this.resupplyFlags[i]);
               itemTag.putBoolean("SaveNbt", this.saveNbtFlags[i]);
               ((ItemStack)this.inventory.get(i)).save(itemTag);
               items.add(itemTag);
            }
         }

         t.put("Items", items);

         if (this.slotSkins != null && !this.slotSkins.isEmpty()) {
            CompoundTag skins = new CompoundTag();
            for (Map.Entry<Integer, List<String>> e : this.slotSkins.entrySet()) {
               ListTag list = new ListTag();
               for (String s : e.getValue()) list.add(StringTag.valueOf(s));
               skins.put(String.valueOf(e.getKey()), list);
            }
            t.put("SlotSkins", skins);
         }

         return t;
      }

      public static WarfareWorldData.KitInfo load(CompoundTag t) {
         WarfareWorldData.KitInfo k = new WarfareWorldData.KitInfo(t.getString("Name"));
         k.isLeaderOnly = t.getBoolean("LeaderOnly");
         k.maxPerTeam = t.getInt("MaxTeam");
         k.maxPerSquad = t.getInt("MaxSquad");
          if (t.contains("MinSquadPlayers")) {
             k.minSquadPlayers = t.getInt("MinSquadPlayers");
          }
          if (t.contains("Category")) k.category = t.getString("Category");
          if (t.contains("Description")) k.description = t.getString("Description");

          ListTag items = t.getList("Items", 10);

         for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < 49) {
               k.inventory.set(slot, ItemStack.of(itemTag));
               k.resupplyFlags[slot] = itemTag.getBoolean("Resupply");
               k.saveNbtFlags[slot] = itemTag.getBoolean("SaveNbt");
            }
         }

         if (t.contains("SlotSkins")) {
            CompoundTag skins = t.getCompound("SlotSkins");
            for (String key : skins.getAllKeys()) {
               ListTag list = skins.getList(key, 8);
               List<String> ids = new ArrayList<>();
               for (int i = 0; i < list.size(); i++) ids.add(list.getString(i));
               k.slotSkins.put(Integer.parseInt(key), ids);
            }
         }

         return k;
      }

      public static WarfareWorldData.KitInfo loadFromJson(JsonObject json) {
         WarfareWorldData.KitInfo k = new WarfareWorldData.KitInfo(json.get("kitName").getAsString());
         k.isLeaderOnly = json.has("leaderOnly") && json.get("leaderOnly").getAsBoolean();
         k.maxPerTeam = json.has("maxPerTeam") ? json.get("maxPerTeam").getAsInt() : -1;
         k.maxPerSquad = json.has("maxPerSquad") ? json.get("maxPerSquad").getAsInt() : -1;
         k.minSquadPlayers = json.has("minSquadPlayers") ? json.get("minSquadPlayers").getAsInt() : 0;

         if (json.has("items")) {
            try {
               JsonArray items = new Gson().fromJson(json.get("items").getAsString(), JsonArray.class);
               for (int i = 0; i < items.size(); i++) {
                  JsonObject itemJson = items.get(i).getAsJsonObject();
                  int slot = itemJson.get("slot").getAsInt();
                  if (slot < 0 || slot >= 49) continue;
                  k.resupplyFlags[slot] = itemJson.has("resupply") && itemJson.get("resupply").getAsBoolean();
                  k.saveNbtFlags[slot] = itemJson.has("saveNbt") && itemJson.get("saveNbt").getAsBoolean();
                  if (itemJson.has("item") && itemJson.get("item").isJsonObject()) {
                     JsonObject itemData = itemJson.getAsJsonObject("item");
                     String itemId = itemData.has("id") ? itemData.get("id").getAsString() : "";
                     if (!itemId.isEmpty() && !itemId.equals("minecraft:air")) {
                        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                           int count = itemData.has("Count") ? itemData.get("Count").getAsInt() : 1;
                           ItemStack stack = new ItemStack(item, count);
                           if (itemData.has("tag") && itemData.get("tag").isJsonObject()) {
                              CompoundTag tag = jsonToCompound(itemData.getAsJsonObject("tag"));
                              if (!tag.isEmpty()) stack.setTag(tag);
                           }
                           k.inventory.set(slot, stack);
                        }
                     }
                  }
               }
            } catch (Exception e) {
               LogUtils.getLogger().warn("Failed to parse items for kit " + k.name + ": " + e.getMessage());
            }
         }

         if (json.has("slotSkins")) {
            try {
               JsonObject skins = new Gson().fromJson(json.get("slotSkins").getAsString(), JsonObject.class);
               for (String key : skins.keySet()) {
                  JsonArray list = skins.getAsJsonArray(key);
                  List<String> ids = new ArrayList<>();
                  for (int i = 0; i < list.size(); i++) ids.add(list.get(i).getAsString());
                  k.slotSkins.put(Integer.parseInt(key), ids);
               }
            } catch (Exception e) {
               LogUtils.getLogger().warn("Failed to parse slot skins for kit " + k.name + ": " + e.getMessage());
            }
         }

         return k;
      }

      public static CompoundTag jsonToCompound(JsonObject json) {
         CompoundTag tag = new CompoundTag();
         for (String key : json.keySet()) {
            try {
               JsonElement val = json.get(key);
               if (val.isJsonObject()) {
                  tag.put(key, jsonToCompound(val.getAsJsonObject()));
               } else if (val.isJsonArray()) {
                  JsonArray arr = val.getAsJsonArray();
                  if (arr.size() > 0) {
                     JsonElement first = arr.get(0);
                     if (first.isJsonObject()) {
                        ListTag list = new ListTag();
                        for (int i = 0; i < arr.size(); i++) {
                           list.add(jsonToCompound(arr.get(i).getAsJsonObject()));
                        }
                        tag.put(key, list);
                     } else if (first.isJsonPrimitive() && first.getAsJsonPrimitive().isString()) {
                        ListTag list = new ListTag();
                        for (int i = 0; i < arr.size(); i++) {
                           list.add(StringTag.valueOf(arr.get(i).getAsString()));
                        }
                        tag.put(key, list);
                     } else if (first.isJsonPrimitive() && first.getAsJsonPrimitive().isNumber()) {
                        ListTag list = new ListTag();
                        for (int i = 0; i < arr.size(); i++) {
                           list.add(IntTag.valueOf(arr.get(i).getAsInt()));
                        }
                        tag.put(key, list);
                     }
                  }
               } else if (val.isJsonPrimitive()) {
                  var prim = val.getAsJsonPrimitive();
                  if (prim.isString()) {
                     tag.putString(key, prim.getAsString());
                  } else if (prim.isNumber()) {
                     double d = prim.getAsDouble();
                     if (d == Math.floor(d) && !Double.isInfinite(d)) {
                        if (d >= Byte.MIN_VALUE && d <= Byte.MAX_VALUE) tag.putByte(key, (byte)d);
                        else if (d >= Short.MIN_VALUE && d <= Short.MAX_VALUE) tag.putShort(key, (short)d);
                        else if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) tag.putInt(key, (int)d);
                        else tag.putLong(key, (long)d);
                     } else {
                        if (d == (float)d) tag.putFloat(key, (float)d);
                        else tag.putDouble(key, d);
                     }
                  } else if (prim.isBoolean()) {
                     tag.putBoolean(key, prim.getAsBoolean());
                  }
               }
            } catch (Exception e) {
               LogUtils.getLogger().warn("Failed to convert JSON field '{}' to NBT: {}", key, e.getMessage());
            }
         }
         return tag;
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

      public static WarfareWorldData.MainProtectionZone load(CompoundTag tag) {
         AABB aabb = new AABB(
            tag.getDouble("minX"), tag.getDouble("minY"), tag.getDouble("minZ"), tag.getDouble("maxX"), tag.getDouble("maxY"), tag.getDouble("maxZ")
         );
         return new WarfareWorldData.MainProtectionZone(tag.getString("Team"), tag.getString("Shape"), aabb);
      }

      public boolean isInside(Vec3 pos) {
         if (!"CYLINDER".equalsIgnoreCase(this.shape)) {
            return this.area.contains(pos);
         }

         double centerX = (this.area.minX + this.area.maxX) / 2.0;
         double centerZ = (this.area.minZ + this.area.maxZ) / 2.0;
         double radius = (this.area.maxX - this.area.minX) / 2.0;
         double dx = pos.x - centerX;
         double dz = pos.z - centerZ;
         return dx * dx + dz * dz <= radius * radius && pos.y >= this.area.minY && pos.y <= this.area.maxY;
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

      public static WarfareWorldData.MapMarker load(CompoundTag tag) {
         return new WarfareWorldData.MapMarker(BlockPos.of(tag.getLong("Pos")), tag.getString("Type"), tag.getString("Team"), tag.getLong("Expiry"));
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
      public List<String> members = new ArrayList<>();
      public String bravoLeader = "";
      public String charlieLeader = "";
      public List<String> bravoMembers = new ArrayList<>();
      public List<String> charlieMembers = new ArrayList<>();
      public WarfareWorldData.SquadMarker bravoMarker = null;
      public WarfareWorldData.SquadMarker charlieMarker = null;
      public BlockPos bravoPingPos = null;
      public long bravoPingExpiry = -1L;
      public BlockPos charliePingPos = null;
      public long charliePingExpiry = -1L;
      public List<WarfareWorldData.SquadMarker> rhombusMarkers = new ArrayList<>();
      public WarfareWorldData.SquadMarker marker = null;
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

         for (String m : this.bravoMembers) {
            bList.add(StringTag.valueOf(m));
         }

         tag.put("BravoMembers", bList);
         ListTag cList = new ListTag();

         for (String m : this.charlieMembers) {
            cList.add(StringTag.valueOf(m));
         }

         tag.put("CharlieMembers", cList);
         if (this.bravoPingPos != null) {
            tag.putLong("BravoPingPos", this.bravoPingPos.asLong());
            tag.putLong("BravoPingExp", this.bravoPingExpiry);
         }

         if (this.charliePingPos != null) {
            tag.putLong("CharliePingPos", this.charliePingPos.asLong());
            tag.putLong("CharliePingExp", this.charliePingExpiry);
         }

         if (this.bravoMarker != null) {
            tag.put("BravoMarker", this.bravoMarker.save());
         }

         if (this.charlieMarker != null) {
            tag.put("CharlieMarker", this.charlieMarker.save());
         }

         ListTag rhombusList = new ListTag();

         for (WarfareWorldData.SquadMarker rm : this.rhombusMarkers) {
            rhombusList.add(rm.save());
         }

         tag.put("RhombusList", rhombusList);
         ListTag memList = new ListTag();

         for (String m : this.members) {
            memList.add(StringTag.valueOf(m));
         }

         tag.put("Members", memList);
         if (this.marker != null) {
            tag.put("SquadMarker", this.marker.save());
         }

         return tag;
      }

      public static WarfareWorldData.Squad load(CompoundTag tag) {
         String l = tag.contains("Leader") ? tag.getString("Leader") : "";
         String dim = tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld";
         WarfareWorldData.Squad s = new WarfareWorldData.Squad(tag.getInt("ID"), tag.getString("Name"), tag.getString("Team"), l, dim);
         s.rallyExpiryTick = tag.getLong("RallyExpiry");
         s.slNoOfficerSince = tag.getLong("SLNoOfficerSince");
         s.isRallyBlocked = tag.getBoolean("RallyBlocked");
         if (tag.contains("NextRallyAvailable")) {
            s.nextRallyAvailableTick = tag.getLong("NextRallyAvailable");
         }

         if (tag.contains("PingPos")) {
            s.pingPos = BlockPos.of(tag.getLong("PingPos"));
            s.pingExpiry = tag.getLong("PingExpiry");
         }

         if (tag.contains("IsLocked")) {
            s.isLocked = tag.getBoolean("IsLocked");
         }

         if (tag.contains("RallyPos")) {
            s.rallyPos = BlockPos.of(tag.getLong("RallyPos"));
            s.rallyDimension = tag.contains("RallyDim") ? tag.getString("RallyDim") : dim;
         }

         s.bravoLeader = tag.getString("BravoLeader");
         s.charlieLeader = tag.getString("CharlieLeader");
         if (tag.contains("BravoMembers")) {
            for (Tag t : tag.getList("BravoMembers", 8)) {
               s.bravoMembers.add(t.getAsString());
            }
         }

         if (tag.contains("CharlieMembers")) {
            for (Tag t : tag.getList("CharlieMembers", 8)) {
               s.charlieMembers.add(t.getAsString());
            }
         }

         if (tag.contains("BravoPingPos")) {
            s.bravoPingPos = BlockPos.of(tag.getLong("BravoPingPos"));
            s.bravoPingExpiry = tag.getLong("BravoPingExp");
         }

         if (tag.contains("CharliePingPos")) {
            s.charliePingPos = BlockPos.of(tag.getLong("CharliePingPos"));
            s.charliePingExpiry = tag.getLong("CharliePingExp");
         }

         if (tag.contains("BravoMarker")) {
            s.bravoMarker = WarfareWorldData.SquadMarker.load(tag.getCompound("BravoMarker"));
         }

         if (tag.contains("CharlieMarker")) {
            s.charlieMarker = WarfareWorldData.SquadMarker.load(tag.getCompound("CharlieMarker"));
         }

         if (tag.contains("RhombusList")) {
            ListTag list = tag.getList("RhombusList", 10);
            s.rhombusMarkers.clear();

            for (int i = 0; i < list.size(); i++) {
               s.rhombusMarkers.add(WarfareWorldData.SquadMarker.load(list.getCompound(i)));
            }
         }

         if (tag.contains("Members")) {
            for (Tag t : tag.getList("Members", 8)) {
               s.members.add(t.getAsString());
            }
         }

         if (s.leader.isEmpty() && !s.members.isEmpty()) {
            s.leader = s.members.get(0);
         }

         if (tag.contains("SquadMarker")) {
            s.marker = WarfareWorldData.SquadMarker.load(tag.getCompound("SquadMarker"));
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

      public static WarfareWorldData.SquadMarker load(CompoundTag tag) {
         return new WarfareWorldData.SquadMarker(
            tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"), tag.getInt("Type"), tag.getLong("Expiry"), tag.getBoolean("IsPhysical")
         );
      }
   }

    public static class ConfigVehicleSpawner {
        public String team;
        public String vehicleName;
        public int x, y, z;
        public float yaw;
        public boolean fixed;
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
      public int ticketPenalty;

      public VehicleRecord(UUID uuid, String team, String type, double x, double y, double z, float yaw, BlockPos spawnerPos, int ticketPenalty) {
         this.uuid = uuid;
         this.team = team;
         this.type = type;
         this.x = x;
         this.y = y;
         this.z = z;
         this.yaw = yaw;
         this.spawnerPos = spawnerPos;
         this.ticketPenalty = ticketPenalty;
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
         tag.putInt("TicketPenalty", this.ticketPenalty);
         if (this.spawnerPos != null) {
            tag.putLong("SpawnerPos", this.spawnerPos.asLong());
         }

         return tag;
      }

      public static WarfareWorldData.VehicleRecord load(CompoundTag tag) {
         BlockPos sPos = tag.contains("SpawnerPos") ? BlockPos.of(tag.getLong("SpawnerPos")) : null;
         return new WarfareWorldData.VehicleRecord(
            tag.getUUID("UUID"),
            tag.getString("Team"),
            tag.getString("Type"),
            tag.getDouble("X"),
            tag.getDouble("Y"),
            tag.getDouble("Z"),
            tag.getFloat("Yaw"),
            sPos,
            tag.getInt("TicketPenalty")
         );
      }
   }

    public static class SpawnerInfo {
       public BlockPos pos;
       public String team;
       public String type;
       public int ticketPenalty;
       public int respawnTime;
       public long targetSpawnTick;
       public boolean isAlive;
       public boolean hasSpawnedOnce;

       public SpawnerInfo(BlockPos pos, String team, String type, int ticketPenalty, int respawnTime, long targetSpawnTick, boolean isAlive, boolean hasSpawnedOnce) {
          this.pos = pos;
          this.team = team;
          this.type = type;
          this.ticketPenalty = ticketPenalty;
          this.respawnTime = respawnTime;
          this.targetSpawnTick = targetSpawnTick;
          this.isAlive = isAlive;
          this.hasSpawnedOnce = hasSpawnedOnce;
       }
    }
}

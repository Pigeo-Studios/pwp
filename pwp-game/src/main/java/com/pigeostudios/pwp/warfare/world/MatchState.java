package com.pigeostudios.pwp.warfare.world;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Состояние матча: тикеты, режим, фазы (ожидание/голосование/отсчёт), точки захвата, спавны мейнов, настройки карты и правил, арт-удары. Часть {@link WarfareWorldData}. */
public final class MatchState {
    public List<WarfareWorldData.CapturePoint> capturePoints = new ArrayList<>();
    public long blueArtStrikeCD = 0L;
    public long redArtStrikeCD = 0L;
    public String shapeType = "CUBE";
    public int lockDurationMinutes = 0;
    public long lockedUntilTick = 0L;
    public Map<String, BlockPos> blueSpawns = new HashMap<>();
    public Map<String, BlockPos> redSpawns = new HashMap<>();
    public Map<String, BlockPos> neutralSpawns = new HashMap<>();
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
    public WarfareWorldData.ArtStrikeRequest blueArtRequest = null;
    public WarfareWorldData.ArtStrikeRequest redArtRequest = null;
    public List<WarfareWorldData.ActiveStrike> activeStrikes = new ArrayList<>();
    public boolean voteActive = false;
    public int voteTimer = 0;
    public boolean blueReady = false;
    public boolean redReady = false;
    public Map<UUID, Boolean> votes = new HashMap<>();

    /** Пишет свои ключи в корень тега WarfareWorldData (формат прежний). */
    public void save(CompoundTag tag) {
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
    }

    /** Читает свои ключи из корня тега WarfareWorldData. */
    public void load(CompoundTag tag) {
        this.blueTickets = tag.getInt("BlueTickets");
        this.redTickets = tag.getInt("RedTickets");
        this.respawnTimer = tag.getInt("RespawnTimer");
        this.deathTicketCost = tag.getInt("DeathTicketCost");
        this.blueFaction = tag.getString("BlueFaction");
        this.redFaction = tag.getString("RedFaction");
        this.isGameStarted = tag.getBoolean("IsGameStarted");
        this.countdownTicks = tag.getInt("CountdownTicks");
        this.countdownActive = tag.getBoolean("CountdownActive");
        this.gameMode = tag.contains("GameMode") ? tag.getString("GameMode") : "aas";
        this.invasionDefender = tag.contains("InvasionDefender") ? tag.getString("InvasionDefender") : "RED";
        this.invasionCaptureBonus = tag.contains("InvasionCapBonus") ? tag.getInt("InvasionCapBonus") : 100;
        this.playedBlueSiren = tag.getBoolean("PlayedBlueSiren");
        this.playedRedSiren = tag.getBoolean("PlayedRedSiren");
        this.mapCenterX = tag.getInt("MapCenterX");
        this.mapCenterZ = tag.getInt("MapCenterZ");
        this.mapSizeBlocks = tag.contains("MapSizeBlocks") ? tag.getInt("MapSizeBlocks") : 2048;
        this.currentMapImage = tag.contains("CurrentMapImage") ? tag.getString("CurrentMapImage") : "map1";
        this.hideDeathMessages = tag.contains("HideDeathMessages") && tag.getBoolean("HideDeathMessages");
        this.hideNametags = tag.contains("HideNametags") && tag.getBoolean("HideNametags");
        this.disableHunger = tag.contains("DisableHunger") && tag.getBoolean("DisableHunger");
        this.disableNaturalRegen = tag.contains("DisableNaturalRegen") && tag.getBoolean("DisableNaturalRegen");
        this.disableBlockDrops = !tag.contains("DisableBlockDrops") || tag.getBoolean("DisableBlockDrops");
        this.disableEntityDrops = !tag.contains("DisableEntityDrops") || tag.getBoolean("DisableEntityDrops");
        this.disableFireSpread = !tag.contains("DisableFireSpread") || tag.getBoolean("DisableFireSpread");
        this.disableWeatherCycle = !tag.contains("DisableWeatherCycle") || tag.getBoolean("DisableWeatherCycle");
        this.isPaused = tag.contains("IsPaused") && tag.getBoolean("IsPaused");
        this.waitingActive = tag.contains("WaitingActive") && tag.getBoolean("WaitingActive");
        this.waitingTimer = tag.contains("WaitingTimer") ? tag.getInt("WaitingTimer") : 0;
        this.invasionSetupActive = tag.contains("InvasionSetupActive") && tag.getBoolean("InvasionSetupActive");
        this.invasionSetupTimer = tag.contains("InvasionSetupTimer") ? tag.getInt("InvasionSetupTimer") : 300;
        this.blueArtStrikeCD = tag.getLong("BlueArtCD");
        this.redArtStrikeCD = tag.getLong("RedArtCD");
        if (tag.contains("BlueSpawnsMap")) {
            CompoundTag map = tag.getCompound("BlueSpawnsMap");

            for (String key : map.getAllKeys()) {
                this.blueSpawns.put(key, BlockPos.of(map.getLong(key)));
            }
        }

        if (tag.contains("RedSpawnsMap")) {
            CompoundTag map = tag.getCompound("RedSpawnsMap");

            for (String key : map.getAllKeys()) {
                this.redSpawns.put(key, BlockPos.of(map.getLong(key)));
            }
        }

        if (tag.contains("NeutralSpawnsMap")) {
            CompoundTag map = tag.getCompound("NeutralSpawnsMap");

            for (String key : map.getAllKeys()) {
                this.neutralSpawns.put(key, BlockPos.of(map.getLong(key)));
            }
        }

        if (tag.contains("CapturePoints")) {
            ListTag list = tag.getList("CapturePoints", 10);

            for (int i = 0; i < list.size(); i++) {
                this.capturePoints.add(WarfareWorldData.CapturePoint.load(list.getCompound(i)));
            }
        }

        if (tag.contains("BlueArtReq")) {
            CompoundTag req = tag.getCompound("BlueArtReq");
            this.blueArtRequest =
                    new WarfareWorldData.ArtStrikeRequest(req.getString("Name"), BlockPos.of(req.getLong("Pos")));
            this.blueArtRequest.timer = req.getInt("Timer");
        }

        if (tag.contains("RedArtReq")) {
            CompoundTag req = tag.getCompound("RedArtReq");
            this.redArtRequest =
                    new WarfareWorldData.ArtStrikeRequest(req.getString("Name"), BlockPos.of(req.getLong("Pos")));
            this.redArtRequest.timer = req.getInt("Timer");
        }

        if (tag.contains("ActiveStrikesList")) {
            ListTag list = tag.getList("ActiveStrikesList", 10);

            for (int i = 0; i < list.size(); i++) {
                CompoundTag sTag = list.getCompound(i);
                WarfareWorldData.ActiveStrike s =
                        new WarfareWorldData.ActiveStrike(BlockPos.of(sTag.getLong("Pos")), sTag.getString("Team"));
                s.stage = sTag.getInt("Stage");
                s.ticksLeft = sTag.getInt("Ticks");
                this.activeStrikes.add(s);
            }
        }
    }
}

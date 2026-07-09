/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 */
package com.example.aas.client;

import com.example.aas.network.MapPlayerInfo;
import com.example.aas.world.AASWorldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class ClientData {
    static public int BLUE_TICKETS = 800;
    static public int RED_TICKETS = 800;
    static public List<AASWorldData.Squad> clientSquads = new ArrayList<AASWorldData.Squad>();
    static public List<AASWorldData.HubInfo> clientHubs = new ArrayList<AASWorldData.HubInfo>();
    static public String BLUE_FACTION = "none";
    static public String RED_FACTION = "none";
    static final public Set<Integer> DOWNED_PLAYERS = new HashSet<Integer>();
    static public List<AASWorldData.MapMarker> activeMarkers = new ArrayList<AASWorldData.MapMarker>();
    static public boolean serverHubSpawnCosts = false;
    static public int serverHubSpawnCostAmount = 0;
    static public String customBlueName = "BLUEFOR";
    static public String customRedName = "REDFOR";
    static public String myCurrentKit = "Unassigned";
    static final public Map<String, Long> SQUAD_SPEAKERS = new ConcurrentHashMap<String, Long>();
    static final public Map<String, Long> RADIO_SPEAKERS = new ConcurrentHashMap<String, Long>();
    static public long globalDeathTimestamp = 0L;
    static public long deathFadeStartTime = 0L;
    static public boolean deathFadePlayed = false;
    static public int blueCMDId = -1;
    static public int redCMDId = -1;
    static public boolean blueCmdVoteActive = false;
    static public String blueCmdCandidateName = "";
    static public Map<UUID, Boolean> blueCmdVotes = new HashMap<UUID, Boolean>();
    static public boolean redCmdVoteActive = false;
    static public String redCmdCandidateName = "";
    static public Map<UUID, Boolean> redCmdVotes = new HashMap<UUID, Boolean>();
    static public boolean voteActive = false;
    static public int voteTimer = 0;
    static public Map<UUID, Boolean> votes = new HashMap<UUID, Boolean>();
    static public float voteTransition = 0.0f;
    static public boolean blueReady = false;
    static public boolean redReady = false;
    static public double mapScale = 4.0;
    static public int mapCenterX = 0;
    static public int mapCenterZ = 0;
    static public int mapSizeBlocks = 2048;
    static public String currentMapImage = "map1";
    static public boolean isMapOpen = false;
    static public float mapTransition = 0.0f;
    static public boolean isGameStarted = false;
    static public boolean isInsidePoint = false;
    static public String pointName = "";
    static public String pointOwner = "NEUTRAL";
    static public float pointProgress = 0.0f;
    static public boolean isLocked = false;
    static public String nextObjectiveName = "";
    static public boolean isContested = false;
    static public String pointCapturingTeam = "NONE";
    static public int pointCaptureRate = 0;
    static public List<AASWorldData.CapturePoint> allCapturePoints = new ArrayList<AASWorldData.CapturePoint>();
    static public Map<String, MapPlayerInfo> mapPlayers = new HashMap<String, MapPlayerInfo>();
    static public long lastFobResupplyTime = 0L;
    static public List<AASWorldData.ActiveStrike> activeStrikes = new ArrayList<AASWorldData.ActiveStrike>();
    static public BlockPos blueArtPos = BlockPos.ZERO;
    static public BlockPos redArtPos = BlockPos.ZERO;
    static public int blueArtTimer = 0;
    static public int redArtTimer = 0;
    static public String blueArtReqName = "";
    static public String redArtReqName = "";
    static public Map<String, BlockPos> blueSpawns = new HashMap<String, BlockPos>();
    static public Map<String, BlockPos> redSpawns = new HashMap<String, BlockPos>();
    static public Map<String, BlockPos> neutralSpawns = new HashMap<String, BlockPos>();
    static public List<AASWorldData.VehicleRecord> clientVehicles = new ArrayList<AASWorldData.VehicleRecord>();
    static public Map<String, String> playerKits = new HashMap<String, String>();
    static public boolean hasBlueRally = false;
    static public boolean hasRedRally = false;
    static public boolean blueRallyBlocked = false;
    static public boolean redRallyBlocked = false;
    static public boolean blueBleeding = false;
    static public boolean redBleeding = false;
    static public int RESPAWN_TIME = 10;
    static public List<Component> menuChatHistory = new ArrayList<Component>();
    static public List<CaptureNotification> captureNotifications = new CopyOnWriteArrayList<CaptureNotification>();

    public static void zoomMap(double delta) {
        double zoomFactor = 1.2;
        double maxScaleLimit = Math.max(2.0, (double)mapSizeBlocks / 350.0);
        mapScale = delta > 0.0 ? Math.max(0.5, mapScale / zoomFactor) : Math.min(maxScaleLimit, mapScale * zoomFactor);
    }

    public static void addChatMessage(Component msg) {
        menuChatHistory.add(0, msg);
        if (menuChatHistory.size() > 50) {
            menuChatHistory.remove(menuChatHistory.size() - 1);
        }
    }

    public static class CaptureNotification {
        public String pointName;
        public String team;
        public boolean isNeutralized;
        public long startTime;
        public long duration = 5000L;

        public CaptureNotification(String name, String team, boolean neutralized) {
            this.pointName = name;
            this.team = team;
            this.isNeutralized = neutralized;
            this.startTime = System.currentTimeMillis();
        }
    }
}


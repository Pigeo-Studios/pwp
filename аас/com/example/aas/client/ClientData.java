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
    public static int BLUE_TICKETS = 800;
    public static int RED_TICKETS = 800;
    public static List<AASWorldData.Squad> clientSquads = new ArrayList<AASWorldData.Squad>();
    public static List<AASWorldData.HubInfo> clientHubs = new ArrayList<AASWorldData.HubInfo>();
    public static String BLUE_FACTION = "none";
    public static String RED_FACTION = "none";
    public static final Set<Integer> DOWNED_PLAYERS = new HashSet<Integer>();
    public static List<AASWorldData.MapMarker> activeMarkers = new ArrayList<AASWorldData.MapMarker>();
    public static boolean serverHubSpawnCosts = false;
    public static int serverHubSpawnCostAmount = 0;
    public static String customBlueName = "BLUEFOR";
    public static String customRedName = "REDFOR";
    public static String myCurrentKit = "Unassigned";
    public static final Map<String, Long> SQUAD_SPEAKERS = new ConcurrentHashMap<String, Long>();
    public static final Map<String, Long> RADIO_SPEAKERS = new ConcurrentHashMap<String, Long>();
    public static long globalDeathTimestamp = 0L;
    public static long deathFadeStartTime = 0L;
    public static boolean deathFadePlayed = false;
    public static int blueCMDId = -1;
    public static int redCMDId = -1;
    public static boolean blueCmdVoteActive = false;
    public static String blueCmdCandidateName = "";
    public static Map<UUID, Boolean> blueCmdVotes = new HashMap<UUID, Boolean>();
    public static boolean redCmdVoteActive = false;
    public static String redCmdCandidateName = "";
    public static Map<UUID, Boolean> redCmdVotes = new HashMap<UUID, Boolean>();
    public static boolean voteActive = false;
    public static int voteTimer = 0;
    public static Map<UUID, Boolean> votes = new HashMap<UUID, Boolean>();
    public static float voteTransition = 0.0f;
    public static boolean blueReady = false;
    public static boolean redReady = false;
    public static double mapScale = 4.0;
    public static int mapCenterX = 0;
    public static int mapCenterZ = 0;
    public static int mapSizeBlocks = 2048;
    public static String currentMapImage = "map1";
    public static boolean isMapOpen = false;
    public static float mapTransition = 0.0f;
    public static boolean isGameStarted = false;
    public static boolean isInsidePoint = false;
    public static String pointName = "";
    public static String pointOwner = "NEUTRAL";
    public static float pointProgress = 0.0f;
    public static boolean isLocked = false;
    public static String nextObjectiveName = "";
    public static boolean isContested = false;
    public static String pointCapturingTeam = "NONE";
    public static int pointCaptureRate = 0;
    public static List<AASWorldData.CapturePoint> allCapturePoints = new ArrayList<AASWorldData.CapturePoint>();
    public static Map<String, MapPlayerInfo> mapPlayers = new HashMap<String, MapPlayerInfo>();
    public static long lastFobResupplyTime = 0L;
    public static List<AASWorldData.ActiveStrike> activeStrikes = new ArrayList<AASWorldData.ActiveStrike>();
    public static BlockPos blueArtPos = BlockPos.f_121853_;
    public static BlockPos redArtPos = BlockPos.f_121853_;
    public static int blueArtTimer = 0;
    public static int redArtTimer = 0;
    public static String blueArtReqName = "";
    public static String redArtReqName = "";
    public static Map<String, BlockPos> blueSpawns = new HashMap<String, BlockPos>();
    public static Map<String, BlockPos> redSpawns = new HashMap<String, BlockPos>();
    public static Map<String, BlockPos> neutralSpawns = new HashMap<String, BlockPos>();
    public static List<AASWorldData.VehicleRecord> clientVehicles = new ArrayList<AASWorldData.VehicleRecord>();
    public static Map<String, String> playerKits = new HashMap<String, String>();
    public static boolean hasBlueRally = false;
    public static boolean hasRedRally = false;
    public static boolean blueRallyBlocked = false;
    public static boolean redRallyBlocked = false;
    public static boolean blueBleeding = false;
    public static boolean redBleeding = false;
    public static int RESPAWN_TIME = 10;
    public static List<Component> menuChatHistory = new ArrayList<Component>();
    public static List<CaptureNotification> captureNotifications = new CopyOnWriteArrayList<CaptureNotification>();

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


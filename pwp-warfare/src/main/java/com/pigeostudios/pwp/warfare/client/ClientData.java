package com.pigeostudios.pwp.warfare.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketVoiceChannelState.Channel;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
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

// Центральное хранилище клиентских данных, синхронизируемых с сервером
// Содержит информацию об игроках, отрядах, точках захвата, хабах и маркерах
public class ClientData {
   public static JsonObject leaderboardData = null;
   public static JsonArray skinsData = null;
   public static JsonArray factionsData = null;
    public static JsonArray factionVehiclesData = null;
    public static JsonObject factionVehicleDetail = null;
    public static JsonArray factionKitsData = null;
   public static int BLUE_TICKETS = 800;
   public static int RED_TICKETS = 800;
   public static int BLUE_PLAYER_COUNT = 0;
   public static int RED_PLAYER_COUNT = 0;
   public static List<WarfareWorldData.Squad> clientSquads = new ArrayList<>();
   public static List<WarfareWorldData.HubInfo> clientHubs = new ArrayList<>();
   public static String BLUE_FACTION = "none";
   public static String RED_FACTION = "none";
   public static final Set<Integer> DOWNED_PLAYERS = new HashSet<>();
   public static List<WarfareWorldData.MapMarker> activeMarkers = new ArrayList<>();
   public static boolean serverHubSpawnCosts = false;
   public static int serverHubSpawnCostAmount = 0;
   public static String customBlueName = "BLUEFOR";
   public static String customRedName = "REDFOR";
   public static String myCurrentKit = "Unassigned";
    public static final Map<String, Long> SQUAD_SPEAKERS = new ConcurrentHashMap<>();
    public static final Map<String, Long> RADIO_SPEAKERS = new ConcurrentHashMap<>();
    public static Channel currentVoiceChannel = Channel.LOCAL;
   public static int blueCMDId = -1;
   public static int redCMDId = -1;
   public static boolean blueCmdVoteActive = false;
   public static String blueCmdCandidateName = "";
   public static Map<UUID, Boolean> blueCmdVotes = new HashMap<>();
   public static boolean redCmdVoteActive = false;
   public static String redCmdCandidateName = "";
   public static Map<UUID, Boolean> redCmdVotes = new HashMap<>();
   public static boolean voteActive = false;
   public static int voteTimer = 0;
   public static Map<UUID, Boolean> votes = new HashMap<>();
   public static float voteTransition = 0.0F;
   public static boolean blueReady = false;
   public static boolean redReady = false;
   public static double mapScale = 4.0;
   public static int mapCenterX = 0;
   public static int mapCenterZ = 0;
   public static int mapSizeBlocks = 2048;
    public static String currentMapImage = "map1";
    public static String gameMode = "aas";
   public static boolean isMapOpen = false;
   public static float mapTransition = 0.0F;
   public static boolean isGameStarted = false;
   public static boolean isInsidePoint = false;
   public static String pointName = "";
   public static String pointOwner = "NEUTRAL";
   public static float pointProgress = 0.0F;
   public static boolean isLocked = false;
   public static String nextObjectiveName = "";
   public static boolean isContested = false;
   public static String pointCapturingTeam = "NONE";
   public static int pointCaptureRate = 0;
   public static List<WarfareWorldData.CapturePoint> allCapturePoints = new ArrayList<>();
   public static Map<String, MapPlayerInfo> mapPlayers = new HashMap<>();
   public static long lastFobResupplyTime = 0L;
   public static List<WarfareWorldData.ActiveStrike> activeStrikes = new ArrayList<>();
   public static BlockPos blueArtPos = BlockPos.ZERO;
   public static BlockPos redArtPos = BlockPos.ZERO;
   public static int blueArtTimer = 0;
   public static int redArtTimer = 0;
   public static String blueArtReqName = "";
   public static String redArtReqName = "";
   public static Map<String, BlockPos> blueSpawns = new HashMap<>();
   public static Map<String, BlockPos> redSpawns = new HashMap<>();
   public static Map<String, BlockPos> neutralSpawns = new HashMap<>();
    public static List<WarfareWorldData.VehicleRecord> clientVehicles = new ArrayList<>();
    public static List<WarfareWorldData.SpawnerInfo> clientSpawners = new ArrayList<>();
    public static Map<String, String> playerKits = new HashMap<>();
   public static boolean hasBlueRally = false;
   public static boolean hasRedRally = false;
   public static boolean blueRallyBlocked = false;
   public static boolean redRallyBlocked = false;
   public static boolean blueBleeding = false;
   public static boolean redBleeding = false;
    public static long teamScreenLastClosed = 0L;
    public static long matchStartTime = 0L;
    public static List<PacketOpenPlayerKitMenu.KitDTO> availableKits = new ArrayList<>();
   public static boolean teamSelectSent = false;
   public static long teamSelectSentTime = 0L;
   public static int RESPAWN_TIME = 10;
   public static long globalDeathTimestamp = 0L;
   public static long deathFadeStartTime = 0L;
   public static boolean deathFadePlayed = false;
    public static long downedTimestamp = 0L;
    public static boolean deployRequested = false;
   public static int downedBleedoutDuration = 0;
   public static List<Component> menuChatHistory = new ArrayList<>();
   public static List<ClientData.CaptureNotification> captureNotifications = new CopyOnWriteArrayList<>();

   public static void zoomMap(double delta) {
      double zoomFactor = 1.2;
      double maxScaleLimit = Math.max(2.0, mapSizeBlocks / 350.0);
      if (delta > 0.0) {
         mapScale = Math.max(0.5, mapScale / zoomFactor);
      } else {
         mapScale = Math.min(maxScaleLimit, mapScale * zoomFactor);
      }
   }

   public static void addChatMessage(Component msg) {
      menuChatHistory.add(0, msg);
      if (menuChatHistory.size() > 50) {
         menuChatHistory.remove(menuChatHistory.size() - 1);
      }
   }

   // Уведомление о захвате/нейтрализации точки
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

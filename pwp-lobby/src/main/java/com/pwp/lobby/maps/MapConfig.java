package com.pwp.lobby.maps;

import java.util.List;

public class MapConfig {

    public String name;
    public String displayName;
    public int maxPlayers = 100;
    public int minPlayers = 10;
    public String image = "map1";
    public String description = "";

    public TeamConfig BLUE = new TeamConfig();
    public TeamConfig RED = new TeamConfig();
    public GameSettings settings = new GameSettings();
    public List<SpawnConfig> spawns;
    public List<CapturePointConfig> capturePoints;
    public MapBoundsConfig mapBounds = new MapBoundsConfig();

    public static class TeamConfig {
        public String faction = "bluefor";
        public int tickets = 800;
    }

    public static class GameSettings {
        public int respawnTimer = 10;
        public int deathTicketCost = 2;
        public int lockDurationMinutes = 0;
    }

    public static class SpawnConfig {
        public String team;
        public int x;
        public int z;
    }

    public static class CapturePointConfig {
        public String name;
        public int x;
        public int z;
        public int radius = 30;
        public String shape = "CIRCLE";
        public int priority = 19;
        public int ticketPenalty = 60;
    }

    public static class MapBoundsConfig {
        public int centerX;
        public int centerZ;
        public int sizeBlocks = 2048;
    }
}

package com.pwp.lobby.maps;

import java.util.List;
import java.util.Map;

public class MapConfig {

    public String name;
    public String displayName;
    public int maxPlayers = 100;
    public int minPlayers = 2;
    public String image = "map1";
    public String description = "";
    public String version = "1.0";
    public String mode = "aas";
    public String modeDisplayName = "Advance and Secure";
    public int durationMinutes = 90;

    public String worldPath;

    public Teams teams = new Teams();
    public GameSettings settings = new GameSettings();

    public List<String> availableFactions;
    public Map<String, ModeConfig> modes;
    public List<CapturePointPattern> capturePointPatterns;

    public static class ModeConfig {
        public int attackerTickets = 300;
        public int defenderTickets = 1400;
        public int captureBonus = 100;
    }

    public static class CapturePointPattern {
        public String name;
        public List<CapturePointConfig> points;
    }

    public static class Teams {
        public TeamConfig BLUE = new TeamConfig();
        public TeamConfig RED = new TeamConfig();
        {
            RED.faction = "redfor";
        }
    }
    public List<MainZoneConfig> mainZones;
    public List<CapturePointConfig> capturePoints;
    public MapBoundsConfig mapBounds = new MapBoundsConfig();
    public Map<String, List<String>> vehicles;

    public static class TeamConfig {
        public String faction = "bluefor";
        public int tickets = 800;
        public SpawnConfig spawn = new SpawnConfig();
    }

    public static class SpawnConfig {
        public String dimension = "minecraft:overworld";
        public int x;
        public int y = 64;
        public int z;
    }

    public static class GameSettings {
        public int respawnTimer = 10;
        public int deathTicketCost = 2;
        public int lockDurationMinutes = 0;
    }

    public static class MainZoneConfig {
        public String team;
        public String shape = "cylinder";
        public Pos1 pos1 = new Pos1();
        public Pos2 pos2 = new Pos2();

        public static class Pos1 {
            public int x;
            public int y;
            public int z;
        }

        public static class Pos2 {
            public int x;
            public int y;
            public int z;
        }
    }

    public static class CapturePointConfig {
        public String name;
        public String shape = "cylinder";
        public Pos1 pos1 = new Pos1();
        public Pos2 pos2 = new Pos2();
        public int bluePriority = 10;
        public int redPriority = 10;
        public int captureTimeMinutes = 2;
        public int ticketPenalty = 60;
        public int captureDeduction;
        public int lockDurationMinutes;

        public static class Pos1 {
            public int x;
            public int y;
            public int z;
        }

        public static class Pos2 {
            public int x;
            public int y;
            public int z;
        }
    }

    public static class MapBoundsConfig {
        public int centerX;
        public int centerZ;
        public int sizeBlocks = 2048;
    }
}

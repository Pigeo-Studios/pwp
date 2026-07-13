package com.pigeostudios.pwp.warfare.config;

public class MatchConfig {
    public Server server = new Server();
    public Rules rules = new Rules();
    public Superbwarfare superbwarfare = new Superbwarfare();

    public static class Server {
        public int viewDistance = 8;
        public int simulationDistance = 6;
        public int entityBroadcastRange = 50;
    }

    public static class Rules {
        public boolean hideNametags = true;
        public boolean keepInventory = true;
        public boolean disableHunger = true;
        public boolean disableNaturalRegen = true;
        public boolean disableWeatherCycle = true;
        public boolean disableDaylightCycle = false;
        public boolean disableFireSpread = true;
        public boolean disableBlockDrops = true;
        public boolean disableEntityDrops = true;
        public boolean showDeathMessages = false;
        public boolean commandFeedback = false;
        public boolean spectatorsGenerateChunks = false;
    }

    public static class Superbwarfare {
        public int vehicleInfoDisplayDistance = 0;
        public boolean vehicleChunkLoading = true;
        public boolean projectileChunkLoading = true;
    }
}

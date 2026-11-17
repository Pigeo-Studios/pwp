package com.pwp.api.config;

/**
 * Единственный POJO файла {@code match_config.json} (корень матч-сервера, из {@code match_template/}).
 *
 * <p>Читают двое: лобби ({@code ServerManager} — секции {@code server} и {@code superbwarfare} для
 * server.properties и superbwarfare-server.toml) и warfare ({@code MatchConfigLoader} — секция
 * {@code rules}: gamerules и правила матча). Разбор — Gson по именам полей: поля НЕ переименовывать,
 * keep-правило в {@code proguard.pro}.
 */
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
        /** true — ванильные и собственные (добивание) сообщения о смерти показываются. */
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

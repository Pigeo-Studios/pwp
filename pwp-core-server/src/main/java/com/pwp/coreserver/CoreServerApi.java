package com.pwp.coreserver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;


public class CoreServerApi {

    private static final Logger log = LoggerFactory.getLogger(CoreServerApi.class);
    private static final Gson GSON = new Gson();

    private static String baseUrl = "http://localhost:8080";
    private static String apiKey = "";
    private static boolean trustAllCerts = false;

    /** Heartbeats run off the server thread so a slow Core Service can't lag the MC server. */
    private static final java.util.concurrent.ExecutorService HEARTBEAT_EXECUTOR =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pwp-heartbeat");
            t.setDaemon(true);
            return t;
        });

    static {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            }, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
        } catch (Exception e) {
            log.warn("Failed to initialize relaxed SSL context (not needed for HTTP-only setups)", e);
        }
    }

    public static void configure(String url, String key) {
        baseUrl = url;
        apiKey = key;
    }

    public static void setTrustAllCerts(boolean val) {
        trustAllCerts = val;
    }

    // ====== PLAYER ======

    public static JsonObject loadPlayer(String uuid) {
        return post("/api/v1/player/load", map("uuid", uuid));
    }

    public static JsonObject createPlayer(String uuid, String nickname) {
        return post("/api/v1/player/create", map("uuid", uuid, "nickname", nickname));
    }

    public static JsonObject saveStats(String uuid, JsonObject stats) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid);
        body.add("stats", stats);
        return post("/api/v1/player/save", body);
    }

    // ====== BANS ======

    /** Проверка банов онлайна: сервер передаёт игроков, получает кого кикать. */
    public static JsonObject checkBans(JsonArray players) {
        JsonObject body = new JsonObject();
        body.add("players", players);
        return post("/api/v1/launcher/ban-check", body);
    }

    // ====== CURRENCY / XP ======

    public static JsonObject addCurrency(String uuid, long amount, String reason) {
        return post("/api/v1/currency/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    public static JsonObject spendCurrency(String uuid, long amount, String itemId) {
        return post("/api/v1/currency/spend", map("uuid", uuid, "amount", amount, "itemId", itemId));
    }

    public static JsonObject addXp(String uuid, long amount, String reason) {
        return post("/api/v1/xp/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    // ====== MATCH ======

    public static JsonObject saveMatch(JsonObject matchData) {
        return post("/api/v1/match/save", matchData);
    }

    // ====== COSMETICS ======

    public static JsonObject grantItem(String uuid, String skinId, String source) {
        return post("/api/v1/cosmetics/grant", map("uuid", uuid, "skinId", skinId, "source", source));
    }

    public static JsonObject equipItem(String uuid, String itemUuid, String slotType, String role) {
        return post("/api/v1/cosmetics/equip", map("uuid", uuid, "itemUuid", itemUuid,
                "slotType", slotType, "role", role));
    }

    public static JsonObject unequipItem(String uuid, String slotType, String role) {
        return post("/api/v1/cosmetics/unequip", map("uuid", uuid, "slotType", slotType, "role", role));
    }

    // ====== SKINS ======

    public static JsonObject getSkins() {
        return get("/api/v1/skins");
    }

    public static JsonObject getSkinsBySlot(String slotType) {
        return get("/api/v1/skins/slot/" + slotType);
    }

    public static JsonObject getSkinsByWeapon(String weaponTag) {
        return get("/api/v1/skins/weapon/" + weaponTag);
    }

    public static JsonObject saveSkin(String skinId, String name, String description, String slotType,
                                       String weaponTag, String rarity, String modelPath) {
        return post("/api/v1/skins/save", map(
                "skinId", skinId, "name", name, "description", description,
                "slotType", slotType, "weaponTag", weaponTag, "rarity", rarity,
                "modelPath", modelPath, "enabled", true));
    }

    public static JsonObject deleteSkin(String skinId) {
        return post("/api/v1/skins/delete", map("skinId", skinId));
    }

    public static JsonObject getCosmetics(String uuid) {
        return get("/api/v1/cosmetics/" + uuid);
    }

    // ====== CASES ======

    public static JsonObject getCases() {
        return get("/api/v1/cases");
    }

    public static JsonObject openCase(String uuid, String caseId) {
        return post("/api/v1/cases/open", map("uuid", uuid, "caseId", caseId));
    }

    // ====== REWARDS ======

    public static JsonObject calculateRewards(String uuid, String team, String winner,
                                               int kills, int assists, int vehicleKills,
                                               int captures, int revives, int headshots,
                                               int durationMinutes) {
        return post("/api/v1/rewards/calculate", map(
                "uuid", uuid, "team", team, "winner", winner,
                "kills", kills, "assists", assists, "vehicleKills", vehicleKills,
                "captures", captures, "revives", revives, "headshots", headshots,
                "durationMinutes", durationMinutes));
    }

    // ====== LEADERBOARD / RANK ======

    public static JsonObject getLeaderboard(String orderBy, int page, int limit) {
        return get("/api/v1/leaderboard?orderBy=" + orderBy + "&page=" + page + "&limit=" + limit);
    }

    public static JsonObject getPlayerRank(String uuid, String orderBy) {
        return get("/api/v1/player/" + uuid + "/rank?orderBy=" + orderBy);
    }

    public static JsonObject getPlayerProfile(String uuid) {
        return get("/api/v1/player/" + uuid);
    }

    // ====== RANKS ======

    public static JsonObject checkRank(String uuid) {
        return post("/api/v1/ranks/check", map("uuid", uuid));
    }

    public static JsonObject getPlayerRanks(String uuid) {
        return get("/api/v1/ranks/player/" + uuid);
    }

    // ====== MATCH POLICY ======

    /** Кэш политики: запросы к core-service синхронные, чтобы не лагать серверный тик. */
    private static volatile long policyCheckedAt = 0;
    private static volatile boolean policyCached = true;
    private static volatile String policyReason = null;
    private static final long POLICY_TTL_MS = 15_000;

    public static boolean canStartNewMatch() {
        refreshPolicy();
        return policyCached;
    }

    public static String getMatchPolicyReason() {
        refreshPolicy();
        return policyReason;
    }

    private static synchronized void refreshPolicy() {
        long now = System.currentTimeMillis();
        if (now - policyCheckedAt < POLICY_TTL_MS) return;
        policyCheckedAt = now;
        try {
            JsonObject resp = get("/api/v1/network/match-policy");
            if (resp != null && resp.has("data")) {
                JsonObject data = resp.getAsJsonObject("data");
                if (data.has("canStartNewMatch")) {
                    policyCached = data.get("canStartNewMatch").getAsBoolean();
                }
                if (data.has("reason") && !data.get("reason").isJsonNull()) {
                    policyReason = data.get("reason").getAsString();
                } else {
                    policyReason = null;
                }
            }
        } catch (Exception e) {
            // core-service недоступен — оставляем последнее известное значение
            log.warn("Failed to refresh match policy: {}", e.getMessage());
        }
    }

    public static JsonObject setMatchPolicy(boolean canStart, String reason) {
        policyCheckedAt = 0; // инвалидируем кэш
        return post("/api/v1/network/match-policy", map("canStartNewMatch", canStart, "reason", reason));
    }

    // ====== NETWORK ======

    /** Статус всех серверов из core-service (heartbeat'ы) — для живого онлайна матчей. */
    public static JsonObject fetchServersStatus() {
        return get("/api/v1/network/status");
    }

    public static JsonObject sendHeartbeat(String server, int online) {
        HEARTBEAT_EXECUTOR.execute(() ->
            post("/api/v1/network/heartbeat", map("server", server, "online", online)));
        return null;
    }

    public static JsonObject sendHeartbeat(String server, int online,
                                           String mapName, String mode,
                                           String blueFaction, String redFaction,
                                           int blueScore, int redScore,
                                           String phase, int maxPlayers,
                                           long matchStartedAt, int matchPlayers) {
        JsonObject body = map("server", server, "online", online);
        if (mapName != null) {
            body.addProperty("mapName", mapName);
            body.addProperty("mode", mode != null ? mode : "");
            body.addProperty("blueFaction", blueFaction != null ? blueFaction : "");
            body.addProperty("redFaction", redFaction != null ? redFaction : "");
            body.addProperty("blueScore", blueScore);
            body.addProperty("redScore", redScore);
            body.addProperty("phase", phase != null ? phase : "");
            body.addProperty("maxPlayers", maxPlayers);
            body.addProperty("matchStartedAt", matchStartedAt);
            body.addProperty("matchPlayers", matchPlayers);
        }
        HEARTBEAT_EXECUTOR.execute(() -> post("/api/v1/network/heartbeat", body));
        return null;
    }

    /** Heartbeat матч-сервера с портом — лобби сверяет по нему матч. */
    public static JsonObject sendHeartbeat(String server, int online, int port,
                                           String mapName, String mode,
                                           String blueFaction, String redFaction,
                                           int blueScore, int redScore,
                                           String phase, int maxPlayers,
                                           long matchStartedAt, int matchPlayers) {
        JsonObject body = map("server", server, "online", online);
        body.addProperty("port", port);
        if (mapName != null) {
            body.addProperty("mapName", mapName);
            body.addProperty("mode", mode != null ? mode : "");
            body.addProperty("blueFaction", blueFaction != null ? blueFaction : "");
            body.addProperty("redFaction", redFaction != null ? redFaction : "");
            body.addProperty("blueScore", blueScore);
            body.addProperty("redScore", redScore);
            body.addProperty("phase", phase != null ? phase : "");
            body.addProperty("maxPlayers", maxPlayers);
            body.addProperty("matchStartedAt", matchStartedAt);
            body.addProperty("matchPlayers", matchPlayers);
        }
        HEARTBEAT_EXECUTOR.execute(() -> post("/api/v1/network/heartbeat", body));
        return null;
    }

    // ====== KITS ======

    public static JsonObject getFactions() {
        return get("/api/v1/kits/factions");
    }

    public static JsonObject getFactionKits(String faction) {
        return get("/api/v1/kits/faction/" + faction.toLowerCase());
    }

    public static JsonObject getFactionKit(String faction, String kitName) {
        return get("/api/v1/kits/faction/" + faction.toLowerCase() + "/" + enc(kitName));
    }

    public static JsonObject saveFactionKit(String faction, String kitName, JsonObject kitData) {
        return put("/api/v1/kits/faction/" + faction.toLowerCase() + "/" + enc(kitName), kitData);
    }

    public static JsonObject bulkSaveFactionKits(String faction, JsonArray kits) {
        return post("/api/v1/kits/faction/" + faction.toLowerCase() + "/bulk", kits);
    }

    // ====== FACTION VEHICLES ======

    public static JsonObject getFactionVehicles(String faction) {
        return get("/api/v1/factions/" + faction.toLowerCase() + "/vehicles");
    }

    public static JsonObject getFactionVehicle(String faction, String vehicleName) {
        return get("/api/v1/factions/" + faction.toLowerCase() + "/vehicles/" + enc(vehicleName));
    }

    public static JsonObject saveFactionVehicle(String faction, String vehicleName, JsonObject data) {
        return put("/api/v1/factions/" + faction.toLowerCase() + "/vehicles/" + enc(vehicleName), data);
    }

    public static JsonObject deleteFactionVehicle(String faction, String vehicleName) {
        HttpURLConnection conn = null;
        try {
            String path = "/api/v1/factions/" + faction.toLowerCase() + "/vehicles/" + enc(vehicleName);
            URI uri = new URI(baseUrl + path);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("DELETE");
            addAuthHeaders(conn, path);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream is = conn.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = is.read(buf)) != -1) buffer.write(buf, 0, n);
                }
                conn.disconnect();
                return GSON.fromJson(buffer.toString(StandardCharsets.UTF_8.name()), JsonObject.class);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.warn("Core API delete failed: {}", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    // ====== VOICE MUTE ======

    public static JsonObject voiceMute(String target, String mutedByUuid, String mutedByNickname,
                                        String reason, int durationMinutes) {
        return post("/api/v1/voicemute", map(
                "target", target,
                "mutedByUuid", mutedByUuid,
                "mutedByNickname", mutedByNickname,
                "reason", reason != null ? reason : "",
                "durationMinutes", durationMinutes));
    }

    public static JsonObject voiceUnmute(String target) {
        return post("/api/v1/voiceunmute", map("target", target));
    }

    public static JsonObject getVoiceMute(String uuid) {
        return get("/api/v1/voicemute/" + uuid);
    }

    public static JsonObject getVoiceMutes() {
        return get("/api/v1/voicemutes");
    }

    // ====== MODERATION (/pwp) ======

    public static JsonObject ban(String target, String reason, String duration, String adminUuid) {
        return post("/api/v1/launcher/ban", map(
                "target", target,
                "reason", reason != null ? reason : "",
                "duration", duration != null ? duration : "perm",
                "adminUuid", adminUuid != null ? adminUuid : ""));
    }

    public static JsonObject unban(String target, String adminUuid) {
        return post("/api/v1/launcher/unban", map(
                "target", target,
                "adminUuid", adminUuid != null ? adminUuid : ""));
    }

    public static JsonObject warn(String target, String reason, String adminUuid) {
        return post("/api/v1/launcher/warn", map(
                "target", target,
                "reason", reason != null ? reason : "",
                "adminUuid", adminUuid != null ? adminUuid : ""));
    }

    public static JsonObject kick(String target, String reason, String adminUuid) {
        return post("/api/v1/launcher/kick", map(
                "target", target,
                "reason", reason != null ? reason : "",
                "adminUuid", adminUuid != null ? adminUuid : ""));
    }

    public static JsonObject chatMute(String target, String mutedByUuid, String mutedByNickname,
                                       String reason, int durationMinutes) {
        return post("/api/v1/chatmute", map(
                "target", target,
                "mutedByUuid", mutedByUuid,
                "mutedByNickname", mutedByNickname,
                "reason", reason != null ? reason : "",
                "durationMinutes", durationMinutes));
    }

    public static JsonObject chatUnmute(String target, String unmutedByUuid) {
        return post("/api/v1/chatunmute", map(
                "target", target,
                "unmutedByUuid", unmutedByUuid != null ? unmutedByUuid : ""));
    }

    public static JsonObject getChatMute(String uuid) {
        return get("/api/v1/chatmute/" + uuid);
    }

    public static JsonObject getChatMutes() {
        return get("/api/v1/chatmutes");
    }

    public static JsonObject getBannedPlayers() {
        return get("/api/v1/punishments/banned");
    }

    public static JsonObject getPunishments(String uuid) {
        return get("/api/v1/punishments/" + uuid);
    }

    private static void addAuthHeaders(HttpURLConnection conn, String path) {
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
    }

    // ====== HTTP HELPERS ======

    private static JsonObject get(String path) {
        try {
            URI uri = new URI(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            addAuthHeaders(conn, path);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream is = conn.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = is.read(buf)) != -1) buffer.write(buf, 0, n);
                }
                conn.disconnect();
                return GSON.fromJson(buffer.toString(StandardCharsets.UTF_8.name()), JsonObject.class);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.warn("Core API get {} failed: {}", path, e.getMessage());
        }
        return null;
    }

    private static JsonObject put(String path, Object body) {
        try {
            URI uri = new URI(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("PUT");
            addAuthHeaders(conn, path);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) buffer.write(buf, 0, n);
            }
            conn.disconnect();
            String responseBody = buffer.toString(StandardCharsets.UTF_8.name());
            if (code == 200 || code == 201) {
                return GSON.fromJson(responseBody, JsonObject.class);
            }
            log.warn("Core API {} returned {}: {}", path, code, responseBody);
        } catch (Exception e) {
            log.warn("Core API call {} failed: {}", path, e.getMessage());
        }
        return null;
    }

    private static JsonObject post(String path, Object body) {
        try {
            URI uri = new URI(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            addAuthHeaders(conn, path);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (body != null) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
                }
            }

            int code = conn.getResponseCode();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) buffer.write(buf, 0, n);
            }
            conn.disconnect();
            String responseBody = buffer.toString(StandardCharsets.UTF_8.name());
            if (code == 200 || code == 201) {
                return GSON.fromJson(responseBody, JsonObject.class);
            }
            log.warn("Core API {} returned {}: {}", path, code, responseBody);
        } catch (Exception e) {
            log.warn("Core API call {} failed: {}", path, e.getMessage());
        }
        return null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static JsonObject map(Object... keysValues) {
        JsonObject obj = new JsonObject();
        for (int i = 0; i < keysValues.length; i += 2) {
            String key = (String) keysValues[i];
            Object val = keysValues[i + 1];
            if (val instanceof String) obj.addProperty(key, (String) val);
            else if (val instanceof Number) obj.addProperty(key, (Number) val);
            else if (val instanceof Boolean) obj.addProperty(key, (Boolean) val);
        }
        return obj;
    }
}

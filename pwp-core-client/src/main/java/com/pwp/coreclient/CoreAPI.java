package com.pwp.coreclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class CoreAPI {

    private static final Logger log = LoggerFactory.getLogger(CoreAPI.class);
    private static final Gson GSON = new Gson();

    private static boolean enabled = true;
    private static String baseUrl = "http://localhost:8080";
    private static String apiKey = "pwp_server_key_change_me";

    public static void configure(String url, String key) {
        baseUrl = url;
        apiKey = key;
    }

    public static void setEnabled(boolean val) { enabled = val; }

    public static boolean isEnabled() { return enabled; }

    public static JsonObject loadPlayer(String uuid) {
        return post("/api/v1/player/load", map("uuid", uuid));
    }

    public static JsonObject createPlayer(String uuid, String nickname) {
        return post("/api/v1/player/create", map("uuid", uuid, "nickname", nickname));
    }

    public static JsonObject saveStats(String uuid, JsonObject stats) {
        return post("/api/v1/player/save", map("uuid", uuid, "stats", stats));
    }

    public static JsonObject addCurrency(String uuid, long amount, String reason) {
        return post("/api/v1/currency/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    public static JsonObject spendCurrency(String uuid, long amount, String itemId) {
        return post("/api/v1/currency/spend", map("uuid", uuid, "amount", amount, "itemId", itemId));
    }

    public static JsonObject addXp(String uuid, long amount, String reason) {
        return post("/api/v1/xp/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    public static JsonObject saveMatch(JsonObject matchData) {
        return post("/api/v1/match/save", matchData);
    }

    public static JsonObject grantItem(String uuid, String skinId, String source) {
        return post("/api/v1/cosmetics/grant", map("uuid", uuid, "skinId", skinId, "source", source));
    }

    public static JsonObject equipItem(String uuid, String itemUuid, String slotType, String role) {
        return post("/api/v1/cosmetics/equip", map("uuid", uuid, "itemUuid", itemUuid,
                "slotType", slotType, "role", role));
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

    // ====== RANKS ======
    public static JsonObject checkRank(String uuid) {
        return post("/api/v1/ranks/check", map("uuid", uuid));
    }

    public static JsonObject getPlayerRanks(String uuid) {
        return get("/api/v1/ranks/player/" + uuid);
    }

    // ====== KITS ======
    public static JsonObject getFactionKits(String faction) {
        return get("/api/v1/kits/faction/" + faction.toLowerCase());
    }

    public static JsonObject getFactionKit(String faction, String kitName) {
        return get("/api/v1/kits/faction/" + faction.toLowerCase() + "/" + kitName);
    }

    public static JsonObject saveFactionKit(String faction, String kitName, JsonObject kitData) {
        return put("/api/v1/kits/faction/" + faction.toLowerCase() + "/" + kitName, kitData);
    }

    public static JsonObject bulkSaveFactionKits(String faction, JsonArray kits) {
        return post("/api/v1/kits/faction/" + faction.toLowerCase() + "/bulk", kits);
    }

    private static JsonObject get(String path) {
        if (!enabled) return null;
        try {
            URI uri = new URI(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
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
        if (!enabled) return null;
        try {
            URI uri = new URI(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
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
        if (!enabled) return null;
        try {
            URI uri = new URI(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
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

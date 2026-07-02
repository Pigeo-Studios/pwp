package com.pwp.coreclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // ====== ИГРОКИ ======

    public static JsonObject loadPlayer(String uuid) {
        return post("/api/v1/player/load", map("uuid", uuid));
    }

    public static JsonObject createPlayer(String uuid, String nickname) {
        return post("/api/v1/player/create", map("uuid", uuid, "nickname", nickname));
    }

    // ====== СТАТИСТИКА ======

    public static JsonObject saveStats(String uuid, JsonObject stats) {
        return post("/api/v1/player/save", map("uuid", uuid, "stats", stats));
    }

    // ====== ВАЛЮТА ======

    public static JsonObject addCurrency(String uuid, long amount, String reason) {
        return post("/api/v1/currency/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    public static JsonObject spendCurrency(String uuid, long amount, String itemId) {
        return post("/api/v1/currency/spend", map("uuid", uuid, "amount", amount, "itemId", itemId));
    }

    // ====== XP ======

    public static JsonObject addXp(String uuid, long amount, String reason) {
        return post("/api/v1/xp/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    // ====== МАТЧИ ======

    public static JsonObject saveMatch(JsonObject matchData) {
        return post("/api/v1/match/save", matchData);
    }

    // ====== КОСМЕТИКА ======

    public static JsonObject grantItem(String uuid, String skinId, String source) {
        return post("/api/v1/cosmetics/grant", map("uuid", uuid, "skinId", skinId, "source", source));
    }

    public static JsonObject equipItem(String uuid, String itemUuid, String slotType, String role) {
        return post("/api/v1/cosmetics/equip", map("uuid", uuid, "itemUuid", itemUuid,
                "slotType", slotType, "role", role));
    }

    // ====== HTTP ======

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
            if (code == 200 || code == 201) {
                byte[] bytes;
                try (var is = conn.getInputStream()) {
                    bytes = is.readAllBytes();
                }
                conn.disconnect();
                return GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.warn("Core API call to {} failed: {}", path, e.getMessage());
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

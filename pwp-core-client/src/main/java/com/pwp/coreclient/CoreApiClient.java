package com.pwp.coreclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class CoreApiClient {

    private static final Gson GSON = new Gson();

    private static HttpURLConnection openConnection(String path, String method) throws Exception {
        URI uri = new URI(CoreClientMod.CORE_API_URL + path);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + CoreClientMod.CORE_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoInput(true);
        return conn;
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try (var reader = new InputStreamReader(
                conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8)) {
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) buffer.write(new String(buf, 0, n).getBytes(StandardCharsets.UTF_8));
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static String post(String path, Object body) throws Exception {
        HttpURLConnection conn = openConnection(path, "POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
        }
        String response = readResponse(conn);
        int code = conn.getResponseCode();
        conn.disconnect();
        if (code >= 400) throw new RuntimeException("Core API error " + code + ": " + response);
        return response;
    }

    private static String get(String path) throws Exception {
        HttpURLConnection conn = openConnection(path, "GET");
        String response = readResponse(conn);
        int code = conn.getResponseCode();
        conn.disconnect();
        if (code >= 400) throw new RuntimeException("Core API error " + code + ": " + response);
        return response;
    }

    private static JsonObject parseResult(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.get("success").getAsBoolean()) {
            throw new RuntimeException("Core API: " + obj.get("error").getAsString());
        }
        return obj.get("data").getAsJsonObject();
    }

    public static JsonObject loadPlayer(String uuid) throws Exception {
        return parseResult(post("/api/v1/player/load", new Object() { String xuuid = uuid; }));
    }

    public static JsonObject createPlayer(String uuid, String nickname) throws Exception {
        return parseResult(post("/api/v1/player/create", new Object() {
            String xuuid = uuid; String xnickname = nickname;
        }));
    }

    public static JsonObject addXp(String uuid, long amount, String reason) throws Exception {
        return parseResult(post("/api/v1/xp/add", new Object() {
            String xuuid = uuid; long xamount = amount; String xreason = reason;
        }));
    }

    public static JsonObject addCurrency(String uuid, long amount, String reason) throws Exception {
        return parseResult(post("/api/v1/currency/add", new Object() {
            String xuuid = uuid; long xamount = amount; String xreason = reason;
        }));
    }

    public static JsonObject spendCurrency(String uuid, long amount, String itemId) throws Exception {
        return parseResult(post("/api/v1/currency/spend", new Object() {
            String xuuid = uuid; long xamount = amount; String xitemId = itemId;
        }));
    }

    public static JsonObject getPlayerProfile(String uuid) throws Exception {
        return parseResult(get("/api/v1/player/" + uuid));
    }

    // ====== SKINS ======
    public static JsonObject getSkins() throws Exception {
        return parseResult(get("/api/v1/skins"));
    }

    public static JsonObject getSkinsBySlot(String slotType) throws Exception {
        return parseResult(get("/api/v1/skins/slot/" + slotType));
    }

    public static JsonObject getSkinsByWeapon(String weaponTag) throws Exception {
        return parseResult(get("/api/v1/skins/weapon/" + weaponTag));
    }

    public static JsonObject getCosmetics(String uuid) throws Exception {
        return parseResult(get("/api/v1/cosmetics/" + uuid));
    }

    // ====== CASES ======
    public static JsonObject getCases() throws Exception {
        return parseResult(get("/api/v1/cases"));
    }

    public static JsonObject openCase(String uuid, String caseId) throws Exception {
        return parseResult(post("/api/v1/cases/open", new Object() {
            String xuuid = uuid; String xcaseId = caseId;
        }));
    }

    // ====== REWARDS ======
    public static JsonObject calculateRewards(String uuid, String team, String winner,
                                               int kills, int assists, int vehicleKills,
                                               int captures, int revives, int headshots,
                                               int durationMinutes) throws Exception {
        return parseResult(post("/api/v1/rewards/calculate", new Object() {
            String xuuid = uuid; String xteam = team; String xwinner = winner;
            int xkills = kills; int xassists = assists; int xvehicleKills = vehicleKills;
            int xcaptures = captures; int xrevives = revives; int xheadshots = headshots;
            int xdurationMinutes = durationMinutes;
        }));
    }

    // ====== RANKS ======
    public static JsonObject checkRank(String uuid) throws Exception {
        return parseResult(post("/api/v1/ranks/check", new Object() { String xuuid = uuid; }));
    }

    public static JsonObject getPlayerRanks(String uuid) throws Exception {
        return parseResult(get("/api/v1/ranks/player/" + uuid));
    }
}

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
        try (var reader = new InputStreamReader(
                conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8)) {
            return new String(reader.readAllBytes(), StandardCharsets.UTF_8);
        }
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
}

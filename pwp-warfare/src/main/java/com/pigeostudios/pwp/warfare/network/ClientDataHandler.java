package com.pigeostudios.pwp.warfare.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pwp.coreclient.PlayerData;
import java.util.UUID;

public class ClientDataHandler {
   private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(ClientDataHandler.class);
    private static final Gson GSON = new Gson();

    public static void handleData(String dataType, String jsonData) {
        switch (dataType) {
            case "profile" -> handleProfile(jsonData);
            case "leaderboard" -> handleLeaderboard(jsonData);
            case "skins" -> handleSkins(jsonData);
            case "factions" -> handleFactions(jsonData);
            case "factionVehicles" -> handleFactionVehicles(jsonData);
            case "factionVehicle" -> handleFactionVehicle(jsonData);
            case "factionKits" -> handleFactionKits(jsonData);
        }
    }

    private static void handleProfile(String json) {
        try {
            JsonObject data = GSON.fromJson(json, JsonObject.class);
            if (data.has("data")) {
                JsonObject profile = data.getAsJsonObject("data");
                String uuid = profile.has("player")
                    ? profile.getAsJsonObject("player").get("uuid").getAsString()
                    : "";
                if (!uuid.isEmpty()) {
                    PlayerData.put(UUID.fromString(uuid), profile);
                }
            }
        } catch (Exception ignored) { PWP_LOG.warn("ClientDataHandler.handleProfile: исключение", ignored); }
    }

    private static void handleLeaderboard(String json) {
        try {
            JsonObject data = GSON.fromJson(json, JsonObject.class);
            ClientData.leaderboardData = data.has("data") ? data.getAsJsonObject("data") : null;
        } catch (Exception ignored) { PWP_LOG.warn("ClientDataHandler.handleLeaderboard: исключение", ignored); }
    }

    private static void handleSkins(String json) {
        try {
            JsonObject data = GSON.fromJson(json, JsonObject.class);
            ClientData.skinsData = data.has("data") ? data.getAsJsonArray("data") : null;
        } catch (Exception ignored) { PWP_LOG.warn("ClientDataHandler.handleSkins: исключение", ignored); }
    }

    private static void handleFactions(String json) {
        try {
            JsonObject data = GSON.fromJson(json, JsonObject.class);
            ClientData.factionsData = data.has("data") ? data.getAsJsonArray("data") : null;
        } catch (Exception ignored) { PWP_LOG.warn("ClientDataHandler.handleFactions: исключение", ignored); }
    }

    private static void handleFactionVehicles(String json) {
        try {
            JsonObject data = GSON.fromJson(json, JsonObject.class);
            ClientData.factionVehiclesData = data.has("data") ? data.getAsJsonArray("data") : null;
        } catch (Exception ignored) { PWP_LOG.warn("ClientDataHandler.handleFactionVehicles: исключение", ignored); }
    }

    private static void handleFactionVehicle(String json) {
        try {
            JsonObject data = GSON.fromJson(json, JsonObject.class);
            ClientData.factionVehicleDetail = data.has("data") ? data.getAsJsonObject("data") : null;
        } catch (Exception ignored) { PWP_LOG.warn("ClientDataHandler.handleFactionVehicle: исключение", ignored); }
    }

    private static void handleFactionKits(String json) {
        try {
            JsonObject data = GSON.fromJson(json, JsonObject.class);
            ClientData.factionKitsData = data.has("data") ? data.getAsJsonArray("data") : null;
        } catch (Exception ignored) { PWP_LOG.warn("ClientDataHandler.handleFactionKits: исключение", ignored); }
    }
}

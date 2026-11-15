package com.pwp.coreclient.network.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.network.ClientResponseCache;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Клиентская сторона единой системы данных: ответ {@link DataResponse} раскладывается во
 * встроенные кэши core-client ({@link ClientResponseCache}, {@link PlayerData}) и передаётся
 * подписчикам других модов (warfare кладёт в свой ClientData). Только клиентский поток.
 */
public final class ClientDataResponses {
    private static final Logger LOG = LoggerFactory.getLogger(ClientDataResponses.class);
    private static final Gson GSON = new Gson();
    private static final List<BiConsumer<String, String>> LISTENERS = new CopyOnWriteArrayList<>();

    private ClientDataResponses() {}

    /** Подписка на все ответы (type, json). */
    public static void listen(BiConsumer<String, String> listener) {
        LISTENERS.add(listener);
    }

    static void dispatch(String type, String json) {
        builtIn(type, json);
        for (BiConsumer<String, String> l : LISTENERS) {
            try {
                l.accept(type, json);
            } catch (RuntimeException e) {
                LOG.warn("Подписчик ответа {} упал", type, e);
            }
        }
    }

    static void builtIn(String type, String data) {
        try {
            switch (type) {
                case "profile": {
                    JsonObject json = GSON.fromJson(data, JsonObject.class);
                    if (json.has("data")) {
                        JsonObject profile = json.getAsJsonObject("data");
                        String uuid = profile.has("player")
                                ? profile.getAsJsonObject("player").get("uuid").getAsString()
                                : "";
                        if (!uuid.isEmpty()) {
                            PlayerData.put(UUID.fromString(uuid), profile);
                        }
                    }
                    break;
                }
                case "skins": {
                    JsonObject json = GSON.fromJson(data, JsonObject.class);
                    if (json.has("data")) {
                        ClientResponseCache.skinsData = json.getAsJsonArray("data");
                    }
                    break;
                }
                case "leaderboard": {
                    JsonObject json = GSON.fromJson(data, JsonObject.class);
                    ClientResponseCache.leaderboardData = json.has("data") ? json.getAsJsonObject("data") : null;
                    break;
                }
                case "factions": {
                    JsonObject json = GSON.fromJson(data, JsonObject.class);
                    if (json.has("data")) {
                        ClientResponseCache.factionsData = json.getAsJsonArray("data");
                    }
                    break;
                }
                case "factionVehicles": {
                    JsonObject json = GSON.fromJson(data, JsonObject.class);
                    if (json.has("data")) {
                        ClientResponseCache.factionVehiclesData = json.getAsJsonArray("data");
                    }
                    break;
                }
                case "factionVehicle": {
                    JsonObject json = GSON.fromJson(data, JsonObject.class);
                    if (json.has("data")) {
                        ClientResponseCache.factionVehicleDetail = json.getAsJsonObject("data");
                    }
                    break;
                }
                case DataRequests.ACTION_RESULT: {
                    JsonObject json = GSON.fromJson(data, JsonObject.class);
                    if (ClientResponseCache.actionResultListener != null) {
                        ClientResponseCache.actionResultListener.accept(json);
                    }
                    break;
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("Не удалось разобрать ответ сервера типа {}", type, e);
        }
    }
}

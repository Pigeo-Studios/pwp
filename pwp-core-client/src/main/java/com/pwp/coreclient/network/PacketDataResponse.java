package com.pwp.coreclient.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.coreclient.PlayerData;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketDataResponse {
    private static final Logger LOG = LoggerFactory.getLogger(PacketDataResponse.class);
    private static final Gson GSON = new Gson();
    /** Тип ответа на админ-действие {@link PacketAction}: {@code {action, success, error}}. */
    public static final String ACTION_RESULT = "actionResult";
    private final String type;
    private final String jsonData;

    public PacketDataResponse(String type, String jsonData) {
        this.type = type;
        this.jsonData = jsonData;
    }

    public static void encode(PacketDataResponse msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.type);
        buf.writeUtf(msg.jsonData);
    }

    public static PacketDataResponse decode(FriendlyByteBuf buf) {
        return new PacketDataResponse(buf.readUtf(), buf.readUtf(32767));
    }

    public static void handle(PacketDataResponse msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                handleOnClient(msg.type, msg.jsonData);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleOnClient(String type, String data) {
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
                case ACTION_RESULT: {
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

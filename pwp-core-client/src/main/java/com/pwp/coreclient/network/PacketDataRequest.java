package com.pwp.coreclient.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.pwp.api.core.CoreApi;
import com.pwp.api.util.ServerThread;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Клиент → сервер: запрос данных core-service (профиль, скины, лидерборд, фракции, техника). */
public class PacketDataRequest {
    private static final Logger LOG = LoggerFactory.getLogger(PacketDataRequest.class);
    private static final Gson GSON = new Gson();
    private final String type;
    private final String paramsJson;

    public PacketDataRequest(String type, String paramsJson) {
        this.type = type;
        this.paramsJson = paramsJson;
    }

    public static void encode(PacketDataRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.type);
        buf.writeUtf(msg.paramsJson);
    }

    public static PacketDataRequest decode(FriendlyByteBuf buf) {
        return new PacketDataRequest(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketDataRequest msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // HTTP — в фоне, ответ игроку — с главного потока
            ServerThread.thenOnServer(fetchData(msg.type, msg.paramsJson, player), player.server, result -> {
                if (player.hasDisconnected()) return;
                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new PacketDataResponse(msg.type, result != null ? result.toString() : "{}"));
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static CompletableFuture<JsonObject> fetchData(String type, String params, ServerPlayer player) {
        JsonObject p = parse(params);
        switch (type) {
            case "profile":
                return CoreApi.loadPlayer(player.getStringUUID());
            case "skins":
                return CoreApi.getSkins();
            case "leaderboard": {
                String orderBy = p.has("orderBy") ? p.get("orderBy").getAsString() : "kills";
                int page = p.has("page") ? p.get("page").getAsInt() : 1;
                return CoreApi.getLeaderboard(orderBy, page, 20);
            }
            case "factions":
                return CoreApi.getFactions();
            case "factionVehicles": {
                String faction = p.has("faction") ? p.get("faction").getAsString() : "";
                return faction.isEmpty() ? empty() : CoreApi.getFactionVehicles(faction);
            }
            case "factionVehicle": {
                String faction = p.has("faction") ? p.get("faction").getAsString() : "";
                String vehicle = p.has("vehicle") ? p.get("vehicle").getAsString() : "";
                return faction.isEmpty() || vehicle.isEmpty() ? empty() : CoreApi.getFactionVehicle(faction, vehicle);
            }
            default:
                return empty();
        }
    }

    private static CompletableFuture<JsonObject> empty() {
        return CompletableFuture.completedFuture(new JsonObject());
    }

    private static JsonObject parse(String params) {
        if (params == null || params.isEmpty()) return new JsonObject();
        try {
            JsonObject o = GSON.fromJson(params, JsonObject.class);
            return o != null ? o : new JsonObject();
        } catch (JsonParseException e) {
            LOG.warn("Некорректные параметры запроса данных: {}", params, e);
            return new JsonObject();
        }
    }
}

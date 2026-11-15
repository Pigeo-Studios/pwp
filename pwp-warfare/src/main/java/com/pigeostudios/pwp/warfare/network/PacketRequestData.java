package com.pigeostudios.pwp.warfare.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.core.CoreApi;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;

public class PacketRequestData {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PacketRequestData.class);
    private static final Gson GSON = new Gson();
    private final String dataType;
    private final String params;

    public PacketRequestData(String dataType, String params) {
        this.dataType = dataType;
        this.params = params;
    }

    public static void encode(PacketRequestData msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.dataType);
        buf.writeUtf(msg.params);
    }

    public static PacketRequestData decode(FriendlyByteBuf buf) {
        return new PacketRequestData(buf.readUtf(), buf.readUtf());
    }

    private static final RequestGuard GUARD = new RequestGuard(300);

    public static void handle(PacketRequestData msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            JsonObject params = parseJson(msg.params);
            // HTTP — в фоне; разбор с фолбэком на данные мира и отправка — на главном потоке
            // Антиспам: повтор того же типа, пока первый в полёте/кулдаун, — игнорируется
            ServerThread.guardedForPlayer(
                    GUARD, msg.dataType, player, () -> fetch(msg.dataType, params, player), (online, response) -> {
                        String json = toResult(msg.dataType, params, response, online);
                        PacketHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> online), new PacketSendData(msg.dataType, json));
                    });
        });
        ctx.get().setPacketHandled(true);
    }

    private static CompletableFuture<JsonObject> fetch(String type, JsonObject params, ServerPlayer player) {
        String faction = str(params, "faction");
        switch (type) {
            case "profile":
                return CoreApi.loadPlayer(player.getStringUUID());
            case "leaderboard": {
                String orderBy = params.has("orderBy") ? params.get("orderBy").getAsString() : "kills";
                int page = params.has("page") ? params.get("page").getAsInt() : 1;
                return CoreApi.getLeaderboard(orderBy, page, 20);
            }
            case "skins":
                return CoreApi.getSkins();
            case "factions":
                return CoreApi.getFactions();
            case "factionVehicles":
                return faction.isEmpty() ? none() : CoreApi.getFactionVehicles(faction);
            case "factionVehicle": {
                String vehicle = str(params, "vehicle");
                return faction.isEmpty() || vehicle.isEmpty() ? none() : CoreApi.getFactionVehicle(faction, vehicle);
            }
            case "factionKits":
                return faction.isEmpty() ? none() : CoreApi.getFactionKits(faction);
            default:
                return none();
        }
    }

    /** JSON-ответ клиенту; при отсутствии ответа API — те же значения по умолчанию, что и раньше. */
    private static String toResult(String type, JsonObject params, JsonObject response, ServerPlayer player) {
        switch (type) {
            case "profile":
            case "leaderboard":
                return response != null ? response.toString() : "{}";
            case "skins":
            case "factions":
                return response != null ? response.toString() : "{\"data\":[]}";
            case "factionVehicles":
                if (str(params, "faction").isEmpty()) return "{}";
                return response != null ? response.toString() : "{\"data\":[]}";
            case "factionVehicle":
                if (str(params, "faction").isEmpty() || str(params, "vehicle").isEmpty()) return "{}";
                return response != null ? response.toString() : "{\"data\":{}}";
            case "factionKits": {
                String faction = str(params, "faction");
                if (faction.isEmpty()) return "{}";
                return "{\"data\":" + kitMetadata(faction, response, player) + "}";
            }
            default:
                return "{}";
        }
    }

    private static JsonArray kitMetadata(String faction, JsonObject response, ServerPlayer player) {
        JsonArray arr = new JsonArray();
        JsonArray apiKits = CoreApi.dataArray(response);
        if (apiKits != null) {
            // API отдаёт полные KitDefinition (items) — клиенту нужны только метаданные,
            // иначе ответ превышает лимит пакета (32767 байт) и загрузка виснет
            for (JsonElement el : apiKits) {
                JsonObject kit = el.getAsJsonObject();
                JsonObject entry = new JsonObject();
                entry.addProperty("kitName", str(kit, "kitName"));
                entry.addProperty("category", str(kit, "category"));
                entry.addProperty("description", str(kit, "description"));
                arr.add(entry);
            }
            return arr;
        }
        // Фолбэк: киты текущего матча из памяти (API недоступен)
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        Map<String, WarfareWorldData.KitInfo> target =
                faction.equalsIgnoreCase(data.blueFaction) ? data.blueKits : data.redKits;
        if (target != null) {
            for (WarfareWorldData.KitInfo k : target.values()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("kitName", k.name);
                entry.addProperty("category", k.category);
                entry.addProperty("description", k.description);
                arr.add(entry);
            }
        }
        return arr;
    }

    private static CompletableFuture<JsonObject> none() {
        return CompletableFuture.completedFuture(null);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static JsonObject parseJson(String s) {
        if (s == null || s.isEmpty()) return new JsonObject();
        try {
            JsonObject o = GSON.fromJson(s, JsonObject.class);
            return o != null ? o : new JsonObject();
        } catch (JsonParseException e) {
            LOG.warn("Некорректные параметры запроса данных: {}", s, e);
            return new JsonObject();
        }
    }
}

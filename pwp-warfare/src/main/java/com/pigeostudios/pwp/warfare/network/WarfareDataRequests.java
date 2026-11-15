package com.pigeostudios.pwp.warfare.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.core.CoreApi;
import com.pwp.coreclient.network.data.DataRequests;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;

/**
 * Типы запросов данных warfare в единой системе {@link DataRequests} (core-client).
 * {@code factionKits} — метаданные китов фракции; если API недоступен — киты текущего матча из памяти.
 */
public final class WarfareDataRequests {
    private WarfareDataRequests() {}

    public static void register() {
        DataRequests.register("factionKits", new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer player, JsonObject params) {
                String faction = str(params, "faction");
                return faction.isEmpty() ? CompletableFuture.completedFuture(null) : CoreApi.getFactionKits(faction);
            }

            @Override
            public String toJson(ServerPlayer player, JsonObject params, JsonObject response) {
                String faction = str(params, "faction");
                if (faction.isEmpty()) return "{}";
                return "{\"data\":" + kitMetadata(faction, response, player) + "}";
            }
        });
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
                faction.equalsIgnoreCase(data.match.blueFaction) ? data.blueKits : data.redKits;
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

    private static String str(JsonObject o, String key) {
        return DataRequests.str(o, key, "");
    }
}

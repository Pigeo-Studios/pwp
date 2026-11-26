package com.pwp.coreclient.network.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;

/**
 * Новости в игре (этап 17b-2): тип данных {@link #TYPE} — сервер берёт ту же ленту, что сайт и лаунчер
 * ({@code GET /api/v1/public/news}), и отдаёт клиенту {@code {items: [...]}}; core недоступен — пустой список с
 * {@code error}. Клиент к core не ходит (ключа у него нет и не должно быть).
 */
public final class NewsData {
    public static final String TYPE = "news";
    static final int LIMIT = 20;

    private NewsData() {}

    public static void registerData() {
        DataRequests.register(TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return CoreApi.news(LIMIT);
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                return build(r).toString();
            }
        });
    }

    static JsonObject build(JsonObject r) {
        JsonObject o = new JsonObject();
        JsonArray items = r != null
                        && CoreApi.isSuccess(r)
                        && r.has("data")
                        && r.get("data").isJsonArray()
                ? r.getAsJsonArray("data")
                : new JsonArray();
        o.add("items", items);
        if (r == null || !CoreApi.isSuccess(r)) o.addProperty("error", "pwp_core.news.error");
        return o;
    }
}

package com.pwp.coreclient.network.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Игровой магазин за Coins (этап 8 ROADMAP). Тип данных {@code shop}: товары {@code /api/v1/shop} + баланс +
 * «уже куплено» из инвентаря косметики. Покупка — C2S {@link Buy}: игрок шлёт только skinId, сервер сам
 * подставляет его uuid и зовёт атомарную покупку core-service ({@code POST /api/v1/shop/buy}, списание и выдача
 * одной транзакцией). Реальные деньги в игре не принимаются — только сайт/лаунчер ({@code /donate/process}).
 */
public final class ShopData {
    public static final String TYPE = "shop";
    public static final String ACTION_BUY = "shop.buy";
    private static final RequestGuard GUARD = new RequestGuard(1500);

    private ShopData() {}

    /** Регистрация типа данных (commonSetup core). */
    public static void registerData() {
        DataRequests.register(TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                String uuid = p.getStringUUID();
                CompletableFuture<JsonObject> shop = CoreApi.getShop();
                CompletableFuture<JsonObject> balance = CoreApi.getBalance(uuid);
                CompletableFuture<JsonObject> owned = CoreApi.getCosmetics(uuid);
                return CompletableFuture.allOf(shop, balance, owned).handle((v, e) -> {
                    JsonObject combined = new JsonObject();
                    combined.add("shop", shop.getNow(null));
                    combined.add("balance", balance.getNow(null));
                    combined.add("owned", owned.getNow(null));
                    return combined;
                });
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                return build(r).toString();
            }
        });
    }

    /** {@code {ok, coins, items: [{skinId, name, description, slotType, rarity, priceCoins, owned}]}}. */
    static JsonObject build(JsonObject r) {
        JsonObject out = new JsonObject();
        JsonArray items = new JsonArray();
        JsonObject shop = r == null ? null : obj(r, "shop");
        out.addProperty("ok", shop != null && CoreApi.isSuccess(shop));
        Set<String> owned = new HashSet<>();
        JsonObject inv = r == null ? null : obj(r, "owned");
        if (inv != null && inv.has("data") && inv.get("data").isJsonArray()) {
            for (JsonElement e : inv.getAsJsonArray("data")) {
                if (e.isJsonObject() && e.getAsJsonObject().has("skinId")) {
                    owned.add(e.getAsJsonObject().get("skinId").getAsString());
                }
            }
        }
        long coins = -1;
        JsonObject bal = r == null ? null : obj(r, "balance");
        if (bal != null && bal.has("data") && bal.get("data").isJsonObject()) {
            JsonObject d = bal.getAsJsonObject("data");
            if (d.has("coins")) coins = d.get("coins").getAsLong();
            else if (d.has("balance")) coins = d.get("balance").getAsLong();
        }
        if (shop != null && shop.has("data") && shop.get("data").isJsonArray()) {
            for (JsonElement e : shop.getAsJsonArray("data")) {
                if (!e.isJsonObject()) continue;
                JsonObject s = e.getAsJsonObject();
                int price = s.has("priceCoins") ? s.get("priceCoins").getAsInt() : 0;
                if (price <= 0) continue; // только за реальные деньги — в игре не показываем
                JsonObject it = new JsonObject();
                String id = str(s, "skinId");
                it.addProperty("skinId", id);
                it.addProperty("name", str(s, "name"));
                it.addProperty("description", str(s, "description"));
                it.addProperty("slotType", str(s, "slotType"));
                it.addProperty("rarity", str(s, "rarity"));
                it.addProperty("priceCoins", price);
                it.addProperty("owned", owned.contains(id));
                items.add(it);
            }
        }
        out.addProperty("coins", coins);
        out.add("items", items);
        return out;
    }

    /** «Купить за Coins»: только skinId от клиента; uuid, цена и право — на стороне сервера и core. */
    public record Buy(String skinId) implements Net.C2S {
        public static void encode(Buy m, FriendlyByteBuf b) {
            b.writeUtf(m.skinId, 128);
        }

        public static Buy decode(FriendlyByteBuf b) {
            return new Buy(b.readUtf(128));
        }

        @Override
        public String validate() {
            return Validate.label("skinId", skinId, 128);
        }

        @Override
        public void handle(ServerPlayer sender) {
            boolean started = ServerThread.guardedForPlayer(
                    GUARD,
                    ACTION_BUY,
                    sender,
                    () -> CoreApi.shopBuy(sender.getStringUUID(), skinId),
                    (online, resp) -> {
                        boolean ok = CoreApi.isSuccess(resp);
                        String error = resp != null
                                        && resp.has("error")
                                        && !resp.get("error").isJsonNull()
                                ? "pwp_core.shop.error."
                                        + resp.get("error").getAsString().replace(' ', '_')
                                : AdminAction.ERR_CORE_DOWN;
                        AdminAction.reply(online, ACTION_BUY, ok, ok ? "" : error);
                        // купленный скин сразу доступен для экипировки; витрина — с новым балансом
                        if (ok) com.pwp.cosmetics.CosmeticService.loadForPlayer(online);
                        DataRequests.refresh(online, TYPE);
                    });
            if (!started) AdminAction.reply(sender, ACTION_BUY, false, AdminAction.ERR_BUSY);
        }
    }

    private static JsonObject obj(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonObject() ? o.getAsJsonObject(k) : null;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }
}

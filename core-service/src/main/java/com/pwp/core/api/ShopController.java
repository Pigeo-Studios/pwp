package com.pwp.core.api;

import com.pwp.core.db.ShopRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

public class ShopController {

    public ShopController(Javalin app) {
        app.get("/api/v1/shop", ctx -> {
            var items = ShopRepository.getAllEnabled();
            ctx.json(ApiResponse.ok(items));
        });

        // Покупка за Coins из игрового магазина (этап 8 ROADMAP). Только игровой сервер (AccessPolicy): игрок
        // нажимает «Купить» в игре, сервер проверяет его и шлёт uuid. Реальные деньги — /donate/process.
        app.post("/api/v1/shop/buy", ctx -> {
            BuyRequest req = ctx.bodyAsClass(BuyRequest.class);
            if (req == null || req.uuid == null || req.skinId == null
                    || !req.uuid.matches("[0-9a-fA-F-]{32,36}") || !req.skinId.matches("[A-Za-z0-9_.:/-]{1,128}")) {
                ctx.json(ApiResponse.error("bad request"));
                return;
            }
            ShopRepository.BuyResult r = ShopRepository.buyWithCoins(req.uuid, req.skinId);
            if (r.error != null) {
                ctx.json(ApiResponse.error(r.error));
                return;
            }
            com.pwp.core.db.LogRepository.log(req.uuid, "SHOP_BUY", 0,
                    "{\"item_id\":\"" + req.skinId.replace("\"", "'") + "\"}");
            ctx.json(ApiResponse.ok(r));
        });
    }

    private static class BuyRequest {
        public String uuid;
        public String skinId;
    }
}

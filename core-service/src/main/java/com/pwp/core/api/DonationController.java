package com.pwp.core.api;

import com.pwp.core.db.*;
import com.pwp.core.model.*;
import io.javalin.Javalin;

public class DonationController {

    public DonationController(Javalin app) {
        app.post("/api/v1/donate/process", ctx -> {
            DonateRequest req = ctx.bodyAsClass(DonateRequest.class);

            ShopItem shopItem = ShopRepository.findBySkinId(req.itemId);
            if (shopItem == null) {
                ctx.json(ApiResponse.error("item not found"));
                return;
            }

            long transactionId = DonationRepository.create(
                    req.uuid, req.itemId, req.amount, req.currency, req.paymentId
            );

            boolean completed = DonationRepository.complete(transactionId);
            if (completed) {
                CosmeticItem item = CosmeticsRepository.grantItem(
                        req.uuid, req.itemId, shopItem.slotType, shopItem.rarity, "DONATE"
                );
                LogRepository.log(req.uuid, "DONATE_COMPLETE", (long) (req.amount * 100),
                        "{\"item_id\":\"" + req.itemId + "\",\"payment_id\":\"" + req.paymentId + "\"}");
                ctx.json(ApiResponse.ok(new DonateResponse(transactionId, item.itemUuid)));
            } else {
                ctx.json(ApiResponse.error("transaction failed"));
            }
        });
    }

    private static class DonateRequest {
        public String uuid; public String itemId; public double amount;
        public String currency; public String paymentId; public String signature;
    }
    private static class DonateResponse {
        public long transactionId; public String itemUuid;
        DonateResponse(long id, String uuid) { transactionId = id; itemUuid = uuid; }
    }
}

package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.*;
import com.pwp.core.model.*;
import io.javalin.Javalin;

import java.util.Arrays;

public class DonationController {

    public DonationController(Javalin app) {
        app.post("/api/v1/donate/process", ctx -> {
            DonateRequest req = ctx.bodyAsClass(DonateRequest.class);

            // Validate signature (simple HMAC-like check using API keys)
            if (req.signature == null || req.signature.isEmpty()) {
                ctx.json(ApiResponse.error("missing signature"));
                return;
            }
            boolean sigValid = false;
            for (String key : CoreApplication.config.api.keys) {
                String expected = hash(req.uuid + req.itemId + req.amount + req.currency + key);
                if (expected.equals(req.signature)) {
                    sigValid = true;
                    break;
                }
            }
            if (!sigValid) {
                ctx.json(ApiResponse.error("invalid signature"));
                return;
            }

            ShopItem shopItem = ShopRepository.findBySkinId(req.itemId);
            if (shopItem == null) {
                ctx.json(ApiResponse.error("item not found"));
                return;
            }

            // Validate amount matches shop price
            if (Math.abs(req.amount - shopItem.priceReal) > 0.01) {
                ctx.json(ApiResponse.error("amount does not match item price"));
                return;
            }

            if (req.amount <= 0) {
                ctx.json(ApiResponse.error("amount must be positive"));
                return;
            }

            // Check for duplicate paymentId (idempotency)
            if (req.paymentId != null && !req.paymentId.isEmpty()) {
                // paymentId uniqueness enforced by DB unique constraint
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
                        "{\"item_id\":\"" + req.itemId.replace("\"", "'") +
                        "\",\"payment_id\":\"" + (req.paymentId != null ? req.paymentId.replace("\"", "'") : "") + "\"}");
                ctx.json(ApiResponse.ok(new DonateResponse(transactionId, item.itemUuid)));
            } else {
                ctx.json(ApiResponse.error("transaction already completed"));
            }
        });
    }

    private static String hash(String input) {
        // Simple hash for donation signature validation
        // In production, replace with HMAC-SHA256
        int h = input.hashCode();
        return Integer.toHexString(h);
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

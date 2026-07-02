package com.pwp.core.api;

import com.pwp.core.db.CurrencyRepository;
import com.pwp.core.db.LogRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

public class CurrencyController {

    public CurrencyController(Javalin app) {
        app.post("/api/v1/currency/add", ctx -> {
            AddRequest req = ctx.bodyAsClass(AddRequest.class);
            if (req.amount <= 0) {
                ctx.json(ApiResponse.error("amount must be positive"));
                return;
            }
            boolean ok = CurrencyRepository.add(req.uuid, req.amount);
            if (ok) {
                LogRepository.log(req.uuid, "COINS_ADD", req.amount,
                        "{\"reason\":\"" + (req.reason != null ? req.reason.replace("\"", "'") : "") + "\"}");
                long balance = CurrencyRepository.getBalance(req.uuid);
                ctx.json(ApiResponse.ok(new BalanceResponse(balance)));
            } else {
                ctx.json(ApiResponse.error("failed to add currency"));
            }
        });

        app.post("/api/v1/currency/spend", ctx -> {
            SpendRequest req = ctx.bodyAsClass(SpendRequest.class);
            if (req.amount <= 0) {
                ctx.json(ApiResponse.error("amount must be positive"));
                return;
            }
            boolean ok = CurrencyRepository.spend(req.uuid, req.amount);
            if (ok) {
                LogRepository.log(req.uuid, "COINS_SPEND", -req.amount,
                        "{\"item_id\":\"" + (req.itemId != null ? req.itemId.replace("\"", "'") : "") + "\"}");
                long balance = CurrencyRepository.getBalance(req.uuid);
                ctx.json(ApiResponse.ok(new BalanceResponse(balance)));
            } else {
                ctx.json(ApiResponse.error("insufficient funds"));
            }
        });

        app.get("/api/v1/currency/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            long balance = CurrencyRepository.getBalance(uuid);
            long totalEarned = 0, totalSpent = 0;
            ctx.json(ApiResponse.ok(new FullBalanceResponse(balance, totalEarned, totalSpent)));
        });
    }

    private static class AddRequest { public String uuid; public long amount; public String reason; }
    private static class SpendRequest { public String uuid; public long amount; public String itemId; }
    private static class BalanceResponse { public long balance; BalanceResponse(long b) { balance = b; } }
    private static class FullBalanceResponse {
        public long balance; public long totalEarned; public long totalSpent;
        FullBalanceResponse(long b, long e, long s) { balance = b; totalEarned = e; totalSpent = s; }
    }
}

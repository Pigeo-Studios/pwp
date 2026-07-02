package com.pwp.core.api;

import com.pwp.core.db.LogRepository;
import com.pwp.core.db.XpRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

public class XpController {

    public XpController(Javalin app) {
        app.post("/api/v1/xp/add", ctx -> {
            AddXpRequest req = ctx.bodyAsClass(AddXpRequest.class);
            if (req.amount <= 0) {
                ctx.json(ApiResponse.error("amount must be positive"));
                return;
            }
            XpRepository.XpData result = XpRepository.addXp(req.uuid, req.amount);
            LogRepository.log(req.uuid, "XP_ADD", req.amount,
                    "{\"reason\":\"" + (req.reason != null ? req.reason.replace("\"", "'") : "") + "\"}");
            ctx.json(ApiResponse.ok(result));
        });

        app.get("/api/v1/xp/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            XpRepository.XpData data = XpRepository.get(uuid);
            if (data == null) {
                ctx.json(ApiResponse.error("player not found"));
            } else {
                ctx.json(ApiResponse.ok(data));
            }
        });
    }

    private static class AddXpRequest { public String uuid; public long amount; public String reason; }
}

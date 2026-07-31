package com.pwp.core.api;

import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

import java.util.LinkedHashMap;
import java.util.Map;

public class MatchPolicyController {

    private static volatile boolean canStartNewMatch = true;
    private static volatile String reason = null;

    public MatchPolicyController(Javalin app) {

        app.get("/api/v1/network/match-policy", ctx -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("canStartNewMatch", canStartNewMatch);
            data.put("reason", reason);
            ctx.json(ApiResponse.ok(data));
        });

        app.post("/api/v1/network/match-policy", ctx -> {
            MatchPolicyReq req = ctx.bodyAsClass(MatchPolicyReq.class);
            canStartNewMatch = req.canStartNewMatch;
            reason = req.reason;
            ctx.json(ApiResponse.ok(Map.of("status", "ok")));
        });
    }

    public static boolean canStartNewMatch() {
        return canStartNewMatch;
    }

    private static class MatchPolicyReq {
        public boolean canStartNewMatch;
        public String reason;
    }
}

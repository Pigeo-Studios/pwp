package com.pwp.core.api;

import com.pwp.core.db.MatchRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.MatchResult;
import io.javalin.Javalin;

public class MatchController {

    public MatchController(Javalin app) {
        app.post("/api/v1/match/save", ctx -> {
            MatchResult match = ctx.bodyAsClass(MatchResult.class);
            long matchId = MatchRepository.saveMatch(match);
            ctx.json(ApiResponse.ok(new MatchSavedResponse(matchId)));
        });
    }

    private static class MatchSavedResponse {
        public long matchId;
        MatchSavedResponse(long id) { matchId = id; }
    }
}

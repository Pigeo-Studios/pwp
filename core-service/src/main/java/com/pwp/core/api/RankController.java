package com.pwp.core.api;

import com.pwp.core.db.LogRepository;
import com.pwp.core.db.RankRepository;
import com.pwp.core.db.XpRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.RankDefinition;
import io.javalin.Javalin;

import java.util.List;

public class RankController {

    public RankController(Javalin app) {
        app.get("/api/v1/ranks", ctx -> {
            ctx.json(ApiResponse.ok(RankRepository.getAll()));
        });

        app.get("/api/v1/ranks/player/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            List<RankDefinition> ranks = RankRepository.getPlayerRanks(uuid);
            ctx.json(ApiResponse.ok(ranks));
        });

        app.post("/api/v1/ranks/check", ctx -> {
            CheckRequest req = ctx.bodyAsClass(CheckRequest.class);

            XpRepository.XpData xpData = XpRepository.get(req.uuid);
            if (xpData == null) {
                ctx.json(ApiResponse.error("player not found"));
                return;
            }

            RankDefinition nextRank = RankRepository.getRankForLevel(xpData.level);
            if (nextRank == null) {
                ctx.json(ApiResponse.ok(new CheckResult(false, null, false)));
                return;
            }

            boolean alreadyHas = RankRepository.hasRank(req.uuid, nextRank.rankId);
            if (!alreadyHas) {
                RankRepository.grantRank(req.uuid, nextRank.rankId);
                LogRepository.log(req.uuid, "RANK_UP", 0,
                        "{\"rank_id\":" + nextRank.rankId + ",\"rank_name\":\"" + nextRank.rankName + "\"}");
                ctx.json(ApiResponse.ok(new CheckResult(true, nextRank.rankName, true)));
            } else {
                ctx.json(ApiResponse.ok(new CheckResult(false, null, false)));
            }
        });
    }

    private static class CheckRequest {
        public String uuid;
    }

    private static class CheckResult {
        public boolean rankUp;
        public String rankName;
        public boolean isNew;
        CheckResult(boolean rankUp, String rankName, boolean isNew) {
            this.rankUp = rankUp;
            this.rankName = rankName;
            this.isNew = isNew;
        }
    }
}

package com.pwp.core.api;

import com.pwp.core.db.RewardConfigRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.RewardConfig;
import io.javalin.Javalin;

import java.util.Map;

public class RewardController {

    public RewardController(Javalin app) {
        app.get("/api/v1/rewards/config", ctx -> {
            ctx.json(ApiResponse.ok(RewardConfigRepository.getAll()));
        });

        app.post("/api/v1/rewards/calculate", ctx -> {
            CalculateRequest req = ctx.bodyAsClass(CalculateRequest.class);
            Map<String, RewardConfig> config = RewardConfigRepository.getAll();

            long xp = 0;
            long coins = 0;
            int score = 0;

            RewardConfig killCfg = config.get("KILL");
            if (killCfg != null) {
                xp += req.kills * killCfg.xpReward;
                coins += req.kills * killCfg.coinsReward;
                score += req.kills * killCfg.scoreReward;
            }

            RewardConfig assistCfg = config.get("ASSIST");
            if (assistCfg != null) {
                xp += req.assists * assistCfg.xpReward;
                coins += req.assists * assistCfg.coinsReward;
                score += req.assists * assistCfg.scoreReward;
            }

            RewardConfig vkCfg = config.get("VEHICLE_KILL");
            if (vkCfg != null) {
                xp += req.vehicleKills * vkCfg.xpReward;
                coins += req.vehicleKills * vkCfg.coinsReward;
                score += req.vehicleKills * vkCfg.scoreReward;
            }

            RewardConfig capCfg = config.get("CAPTURE");
            if (capCfg != null) {
                xp += req.captures * capCfg.xpReward;
                coins += req.captures * capCfg.coinsReward;
                score += req.captures * capCfg.scoreReward;
            }

            RewardConfig revCfg = config.get("REVIVE");
            if (revCfg != null) {
                xp += req.revives * revCfg.xpReward;
                coins += req.revives * revCfg.coinsReward;
                score += req.revives * revCfg.scoreReward;
            }

            RewardConfig timeCfg = config.get("TIME_MINUTE");
            if (timeCfg != null && req.durationMinutes > 0) {
                xp += req.durationMinutes * timeCfg.xpReward;
                coins += req.durationMinutes * timeCfg.coinsReward;
                score += req.durationMinutes * timeCfg.scoreReward;
            }

            boolean winner = req.team != null && req.team.equals(req.winner);
            RewardConfig winCfg = config.get(winner ? "WIN" : "LOSS");
            if (winCfg != null) {
                xp += winCfg.xpReward;
                coins += winCfg.coinsReward;
                score += winCfg.scoreReward;
            }

            ctx.json(ApiResponse.ok(new CalculateResult(xp, coins, score)));
        });

        app.post("/api/v1/rewards/config/update", ctx -> {
            UpdateRequest req = ctx.bodyAsClass(UpdateRequest.class);
            RewardConfigRepository.update(req.action, req.xpReward, req.coinsReward, req.scoreReward);
            ctx.json(ApiResponse.ok("updated"));
        });
    }

    private static class CalculateRequest {
        public String uuid;
        public String team;
        public String winner;
        public int kills, assists, vehicleKills, captures, revives, headshots;
        public int durationMinutes;
    }

    private static class CalculateResult {
        public long xp;
        public long coins;
        public int score;
        CalculateResult(long xp, long coins, int score) {
            this.xp = xp; this.coins = coins; this.score = score;
        }
    }

    private static class UpdateRequest {
        public String action;
        public long xpReward;
        public long coinsReward;
        public int scoreReward;
    }
}

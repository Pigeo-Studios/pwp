package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.db.CurrencyRepository;
import com.pwp.core.db.XpRepository;
import com.pwp.core.db.CosmeticsRepository;
import com.pwp.core.model.*;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.util.List;

public class PlayerController {

    public PlayerController(Javalin app, CoreApplication.Config config) {
        app.post("/api/v1/player/load", ctx -> {
            String uuid = ctx.bodyAsClass(LoadRequest.class).uuid;
            if (uuid == null) {
                ctx.json(ApiResponse.error("uuid is required"));
                return;
            }
            Player player = PlayerRepository.findByUuid(uuid);
            if (player == null) {
                ctx.json(ApiResponse.error("player not found"));
                return;
            }
            PlayerStats stats = PlayerRepository.getStats(uuid);
            XpRepository.XpData xp = XpRepository.get(uuid);
            long coins = CurrencyRepository.getBalance(uuid);
            List<CosmeticItem> cosmetics = CosmeticsRepository.getInventory(uuid);

            PlayerProfile profile = new PlayerProfile();
            profile.player = player;
            profile.stats = stats;
            profile.coins = coins;
            profile.xp = xp != null ? xp.xp : 0;
            profile.level = xp != null ? xp.level : 1;
            profile.prestige = xp != null ? xp.prestige : 0;
            profile.cosmetics = cosmetics;
            ctx.json(ApiResponse.ok(profile));
        });

        app.post("/api/v1/player/create", ctx -> {
            CreateRequest req = ctx.bodyAsClass(CreateRequest.class);
            if (req.uuid == null || req.nickname == null) {
                ctx.json(ApiResponse.error("uuid and nickname are required"));
                return;
            }
            PlayerRepository.createOrUpdate(req.uuid, req.nickname);
            ctx.json(ApiResponse.ok("player created"));
        });

        app.post("/api/v1/player/save", ctx -> {
            SaveRequest req = ctx.bodyAsClass(SaveRequest.class);
            if (req.uuid == null || req.stats == null) {
                ctx.json(ApiResponse.error("uuid and stats are required"));
                return;
            }
            PlayerRepository.updateStats(req.uuid, req.stats);
            ctx.json(ApiResponse.ok("stats saved"));
        });

        app.get("/api/v1/player/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            Player player = PlayerRepository.findByUuid(uuid);
            if (player == null) throw new NotFoundResponse();

            PlayerStats stats = PlayerRepository.getStats(uuid);
            XpRepository.XpData xp = XpRepository.get(uuid);
            long coins = CurrencyRepository.getBalance(uuid);
            List<CosmeticItem> cosmetics = CosmeticsRepository.getInventory(uuid);

            PlayerProfile profile = new PlayerProfile();
            profile.player = player;
            profile.stats = stats;
            profile.coins = coins;
            profile.xp = xp != null ? xp.xp : 0;
            profile.level = xp != null ? xp.level : 1;
            profile.prestige = xp != null ? xp.prestige : 0;
            profile.cosmetics = cosmetics;
            ctx.json(ApiResponse.ok(profile));
        });

        app.get("/api/v1/leaderboard", ctx -> {
            String orderBy = ctx.queryParam("orderBy") != null ? ctx.queryParam("orderBy") : "kills";
            int page = ctx.queryParam("page") != null ? Integer.parseInt(ctx.queryParam("page")) : 1;
            int limit = ctx.queryParam("limit") != null ? Integer.parseInt(ctx.queryParam("limit")) : 50;
            int offset = (page - 1) * limit;

            List<PlayerProfile> list = PlayerRepository.getLeaderboard(orderBy, limit, offset);
            long total = PlayerRepository.getPlayerCount();
            ctx.json(ApiResponse.ok(new LeaderboardResponse(list, total, page, limit)));
        });

        app.get("/api/v1/player/{uuid}/rank", ctx -> {
            String uuid = ctx.pathParam("uuid");
            String orderBy = ctx.queryParam("orderBy") != null ? ctx.queryParam("orderBy") : "kills";
            Player player = PlayerRepository.findByUuid(uuid);
            if (player == null) throw new NotFoundResponse();

            PlayerStats stats = PlayerRepository.getStats(uuid);
            int rank = PlayerRepository.getPlayerRank(uuid, orderBy);
            long total = PlayerRepository.getPlayerCount();

            ctx.json(ApiResponse.ok(new RankResponse(uuid, player.nickname, rank, total, stats)));
        });

        app.post("/api/v1/player/ban", ctx -> {
            BanRequest req = ctx.bodyAsClass(BanRequest.class);
            ctx.json(ApiResponse.ok("not implemented"));
        });
    }

    private static class LoadRequest { public String uuid; }
    private static class CreateRequest { public String uuid; public String nickname; }
    private static class SaveRequest { public String uuid; public PlayerStats stats; }
    private static class BanRequest { public String uuid; public String reason; }
    private static class LeaderboardResponse {
        public List<PlayerProfile> players; public long total; public int page; public int limit;
        LeaderboardResponse(List<PlayerProfile> p, long t, int pg, int lim) {
            players = p; total = t; page = pg; limit = lim;
        }
    }
    private static class RankResponse {
        public String uuid; public String nickname; public int rank; public long total; public PlayerStats stats;
        RankResponse(String u, String n, int r, long t, PlayerStats s) {
            uuid = u; nickname = n; rank = r; total = t; stats = s;
        }
    }
}

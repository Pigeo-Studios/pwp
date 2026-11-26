package com.pwp.core.api;

import com.pwp.core.db.CommunityRepository;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.Player;
import com.pwp.core.model.PlayerProfile;
import com.pwp.core.model.PlayerStats;
import io.javalin.Javalin;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Публичное API для сайта (этап 10d) — без авторизации ({@code /api/v1/public/} в {@code AuthMiddleware}),
 * только чтение и только то, что можно показывать всем: ник, статистика, ранг. Нет uuid-паролей-почты-IP,
 * нет IP/портов серверов. Лимит запросов — общий IP rate-limit core.
 *
 * <ul>
 *   <li>{@code GET /api/v1/public/online} — общий онлайн и серверы (карта, режим, фракции, тикеты)
 *   <li>{@code GET /api/v1/public/top?by=kd|kills|wins|playtime&limit=} — таблица лидеров
 *   <li>{@code GET /api/v1/public/player/{nickname}} — профиль игрока
 *   <li>{@code GET /api/v1/public/bans} — активные баны (ник, причина, до)
 * </ul>
 */
public class PublicController {
    private static final Set<String> ORDER = Set.of("kd", "kills", "wins", "playtime");

    public PublicController(Javalin app) {
        app.get("/api/v1/public/online", ctx -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("online", NetworkController.publicTotalOnline());
            m.put("servers", NetworkController.publicServers());
            ctx.json(ApiResponse.ok(m));
        });

        app.get("/api/v1/public/top", ctx -> {
            String by = ctx.queryParam("by");
            if (by == null || !ORDER.contains(by)) by = "kd";
            int limit = 25;
            try {
                limit = Math.max(1, Math.min(100, Integer.parseInt(ctx.queryParam("limit"))));
            } catch (NumberFormatException e) {
                // нет параметра — 25
            }
            List<Map<String, Object>> out = new ArrayList<>();
            int place = 1;
            for (PlayerProfile pp : PlayerRepository.getLeaderboard(by, limit, 0)) {
                Map<String, Object> e = stats(pp.player.nickname, pp.stats);
                e.put("place", place++);
                e.put("level", pp.level);
                e.put("prestige", pp.prestige);
                out.add(e);
            }
            ctx.json(ApiResponse.ok(out));
        });

        app.get("/api/v1/public/player/{nickname}", ctx -> {
            Player p = CommunityController.byNickname(ctx.pathParam("nickname"));
            if (p == null) {
                ctx.status(404).json(ApiResponse.error("player not found"));
                return;
            }
            PlayerStats s = PlayerRepository.getStats(p.uuid);
            Map<String, Object> m = stats(p.nickname, s == null ? new PlayerStats() : s);
            m.put("firstJoin", p.firstJoin);
            m.put("lastJoin", p.lastJoin);
            m.put("rankKd", PlayerRepository.getPlayerRank(p.uuid, "kd"));
            m.put("banned", p.isBanned && !PlayerRepository.isBanExpired(p));
            ctx.json(ApiResponse.ok(m));
        });

        app.get("/api/v1/public/bans", ctx -> ctx.json(ApiResponse.ok(CommunityRepository.activeBans(500))));

        // ссылки для главной сайта (лаунчер, Discord) — из config.json → site
        app.get("/api/v1/public/site", ctx -> {
            var s = com.pwp.core.CoreApplication.config.site;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("launcherUrl", s == null ? "" : s.launcherUrl);
            m.put("discordUrl", s == null ? "" : s.discordUrl);
            ctx.json(ApiResponse.ok(m));
        });
    }

    private static Map<String, Object> stats(String nickname, PlayerStats s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nickname", nickname);
        m.put("kills", s.kills);
        m.put("deaths", s.deaths);
        m.put("kd", s.getKd());
        m.put("wins", s.wins);
        m.put("losses", s.losses);
        m.put("matches", s.matchesPlayed);
        m.put("revives", s.revives);
        m.put("captures", s.captures);
        m.put("vehiclesDestroyed", s.vehiclesDestroyed);
        m.put("playtimeHours", s.playtimeSeconds / 3600);
        return m;
    }
}

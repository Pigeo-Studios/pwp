package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.PlayerProfile;
import io.javalin.Javalin;

import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkController {

    private static final ConcurrentHashMap<String, ServerHeartbeat> heartbeats = new ConcurrentHashMap<>();
    private static int recordOnline = 0;
    private static String recordOnlineDate = "";

    public static int recordOnline() { return recordOnline; }

    public NetworkController(Javalin app) {

        app.post("/api/v1/network/heartbeat", ctx -> {
            HeartbeatReq req = ctx.bodyAsClass(HeartbeatReq.class);
            if (req.server == null) {
                ctx.json(ApiResponse.error("server name required"));
                return;
            }
            heartbeats.put(req.server, new ServerHeartbeat(req.server, req.online, req.ip, req.port, System.currentTimeMillis()));
            int total = currentTotalOnline();
            if (total > recordOnline) {
                recordOnline = total;
                recordOnlineDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            }
            ctx.json(ApiResponse.ok(Map.of("total_online", total)));
        });

        app.get("/api/v1/network/status", ctx -> {
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            String uptime = formatUptime(uptimeMs);
            int totalOnline = currentTotalOnline();

            Map<String, Object> services = new LinkedHashMap<>();
            services.put("core", true);
            services.put("launcher", false);
            services.put("telegram", false);
            services.put("discord", false);
            List<Map<String, Object>> serverList = new ArrayList<>();
            for (ServerHeartbeat h : heartbeats.values()) {
                if (System.currentTimeMillis() - h.lastSeen < 120_000) {
                    services.put(h.server, true);
                    Map<String, Object> srv = new HashMap<>();
                    srv.put("name", h.server);
                    srv.put("online", h.online);
                    srv.put("ip", h.ip != null ? h.ip : "");
                    srv.put("port", h.port > 0 ? h.port : 0);
                    serverList.add(srv);
                }
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("servers", serverList);
            data.put("online", totalOnline);
            data.put("uptime", uptime);
            data.put("version", CoreApplication.projectVersion);
            data.put("services", services);
            ctx.json(ApiResponse.ok(data));
        });

        app.get("/api/v1/network/statistics", ctx -> {
            int accounts = PlayerRepository.countAccounts();
            int todayPlayers = 0;
            long playtimeHours = 0;
            long totalKills = 0;
            long totalVehiclesDestroyed = 0;
            long totalCaptures = 0;
            try {
                todayPlayers = PlayerRepository.getTodayPlayerCount();
                playtimeHours = PlayerRepository.getTotalPlaytimeHours();
                totalKills = PlayerRepository.getTotalKills();
                totalVehiclesDestroyed = PlayerRepository.getTotalVehiclesDestroyed();
                totalCaptures = PlayerRepository.getTotalCaptures();
            } catch (SQLException e) {
                ctx.json(ApiResponse.error("db error"));
                return;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("accounts", accounts);
            data.put("todayPlayers", todayPlayers);
            data.put("recordOnline", recordOnline);
            data.put("playtimeHours", playtimeHours);
            data.put("totalKills", totalKills);
            data.put("totalVehiclesDestroyed", totalVehiclesDestroyed);
            data.put("totalCaptures", totalCaptures);
            ctx.json(ApiResponse.ok(data));
        });

        app.get("/api/v1/network/top", ctx -> {
            try {
                List<PlayerProfile> list = PlayerRepository.getLeaderboard("kd", 3, 0);
                List<Map<String, Object>> top = new ArrayList<>();
                String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};
                for (int i = 0; i < list.size(); i++) {
                    PlayerProfile pp = list.get(i);
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("rank", i + 1);
                    entry.put("medal", i < medals.length ? medals[i] : "");
                    entry.put("nickname", pp.player.nickname);
                    entry.put("kdr", pp.stats.getKd());
                    entry.put("kills", pp.stats.kills);
                    entry.put("deaths", pp.stats.deaths);
                    entry.put("playtimeHours", pp.stats.playtimeSeconds / 3600);
                    entry.put("damage", (int) pp.stats.damageDealt);
                    top.add(entry);
                }
                ctx.json(ApiResponse.ok(top));
            } catch (SQLException e) {
                ctx.json(ApiResponse.error("db error"));
            }
        });

        app.get("/api/v1/network/release", ctx -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("version", CoreApplication.projectVersion);
            data.put("commit", CoreApplication.projectCommit);
            data.put("date", CoreApplication.projectDate);
            data.put("changelog", CoreApplication.projectChangelog);
            ctx.json(ApiResponse.ok(data));
        });

        app.get("/api/v1/admin/audit", ctx -> {
            int limit = parseInt(ctx.queryParam("limit"), 50);
            int after = parseInt(ctx.queryParam("after"), 0);
            String action = ctx.queryParam("action");
            try {
                var logs = PlayerRepository.getLogs(Math.min(limit, 100), after);
                ctx.json(ApiResponse.ok(logs));
            } catch (SQLException e) {
                ctx.json(ApiResponse.error("db error"));
            }
        });
    }

    private static int currentTotalOnline() {
        return heartbeats.values().stream()
            .filter(h -> System.currentTimeMillis() - h.lastSeen < 120_000)
            .mapToInt(h -> h.online)
            .sum();
    }

    private static String formatUptime(long ms) {
        long days = ms / 86_400_000;
        long hours = (ms % 86_400_000) / 3_600_000;
        long minutes = (ms % 3_600_000) / 60_000;
        if (days > 0) return days + "д " + hours + "ч";
        if (hours > 0) return hours + "ч " + minutes + "м";
        return minutes + "м";
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public static class HeartbeatReq {
        public String server;
        public int online;
        public String ip;
        public int port;
    }

    private static class ServerHeartbeat {
        final String server;
        final int online;
        final String ip;
        final int port;
        final long lastSeen;
        ServerHeartbeat(String server, int online, String ip, int port, long lastSeen) {
            this.server = server; this.online = online; this.ip = ip; this.port = port; this.lastSeen = lastSeen;
        }
    }
}

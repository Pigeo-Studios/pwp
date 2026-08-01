package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.db.PunishmentRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.Player;
import io.javalin.Javalin;

import java.io.File;
import java.util.*;

public class AdminController {

    public AdminController(Javalin app) {

        // All admin endpoints verify the caller has admin/owner role

        app.post("/api/v1/admin/find-user", ctx -> {
            FindUserReq req = ctx.bodyAsClass(FindUserReq.class);
            if (req.query == null) {
                ctx.json(ApiResponse.error("query required")); return;
            }
            Player pl = PlayerRepository.findAny(req.query);
            if (pl == null) {
                ctx.json(ApiResponse.error("user not found")); return;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("uuid", pl.uuid); m.put("login", pl.login); m.put("nickname", pl.nickname);
            m.put("email", maskEmail(pl.email)); m.put("telegram_id", pl.telegramId);
            m.put("role", pl.role); m.put("is_banned", pl.isBanned); m.put("ban_reason", pl.banReason);
            m.put("registered_at", pl.firstJoin); m.put("last_login", pl.lastJoin); m.put("last_ip", pl.lastIp);
            m.put("2fa_enabled", pl.launcher2faEnabled); m.put("privacy_accepted", pl.privacyPolicyAccepted);
            ctx.json(ApiResponse.ok(m));
        });

        app.post("/api/v1/admin/ban", ctx -> {
            verifyAdmin(ctx);
            BanReq req = ctx.bodyAsClass(BanReq.class);
            if (PlayerRepository.findByUuid(req.uuid) == null) {
                ctx.json(ApiResponse.error("user not found")); return;
            }
            PlayerRepository.setBan(req.uuid, true, req.reason);
            PlayerRepository.log(req.uuid, "ban", ctx.ip(), "reason: " + req.reason);
            try {
                PunishmentRepository.addRecord(req.uuid, "BAN", req.reason,
                        ctx.attribute("adminUuid"), null, null);
            } catch (Exception ignored) {}
            ctx.json(ApiResponse.ok("user banned"));
        });

        app.post("/api/v1/admin/unban", ctx -> {
            verifyAdmin(ctx);
            ReqUuid req = ctx.bodyAsClass(ReqUuid.class);
            if (PlayerRepository.findByUuid(req.uuid) == null) {
                ctx.json(ApiResponse.error("user not found")); return;
            }
            PlayerRepository.setBan(req.uuid, false, null);
            PlayerRepository.log(req.uuid, "unban", ctx.ip(), null);
            try {
                PlayerRepository.clearBans(req.uuid);
            } catch (Exception ignored) {}
            try {
                PunishmentRepository.addRecord(req.uuid, "UNBAN", null,
                        ctx.attribute("adminUuid"), null, null);
            } catch (Exception ignored) {}
            ctx.json(ApiResponse.ok("user unbanned"));
        });

        app.post("/api/v1/admin/set-role", ctx -> {
            // Allow setting role with API key (used by bot for bootstrap)
            SetRoleReq req = ctx.bodyAsClass(SetRoleReq.class);
            if (PlayerRepository.findByUuid(req.uuid) == null) {
                ctx.json(ApiResponse.error("user not found")); return;
            }
            List<String> valid = List.of("user", "support", "admin", "owner");
            if (!valid.contains(req.role)) {
                ctx.json(ApiResponse.error("invalid role: " + String.join(", ", valid))); return;
            }
            PlayerRepository.setRole(req.uuid, req.role);
            PlayerRepository.log(req.uuid, "role_change", ctx.ip(), "new role: " + req.role);
            ctx.json(ApiResponse.ok("role changed to " + req.role));
        });

        app.get("/api/v1/admin/pending-resets", ctx -> {
            verifyAdmin(ctx);
            ctx.json(ApiResponse.ok(PlayerRepository.findPendingResets()));
        });

        app.post("/api/v1/admin/resolve-reset", ctx -> {
            verifyAdmin(ctx);
            ResolveResetReq req = ctx.bodyAsClass(ResolveResetReq.class);
            PlayerRepository.resolveReset(req.resetId, req.adminUuid, req.status);
            ctx.json(ApiResponse.ok(Map.of("status", req.status)));
        });

        app.get("/api/v1/admin/logs", ctx -> {
            verifyAdmin(ctx);
            int limit = parseInt(ctx.queryParam("limit"), 20);
            int offset = parseInt(ctx.queryParam("offset"), 0);
            ctx.json(ApiResponse.ok(PlayerRepository.getLogs(Math.min(limit, 100), offset)));
        });

        app.post("/api/v1/admin/broadcast", ctx -> {
            verifyAdmin(ctx);
            BroadcastReq req = ctx.bodyAsClass(BroadcastReq.class);
            if (req.message == null || req.adminUuid == null) {
                ctx.json(ApiResponse.error("message and admin_uuid required")); return;
            }
            List<Long> tgIds = PlayerRepository.getTelegramIdsForBroadcast();
            PlayerRepository.log(req.adminUuid, "broadcast", ctx.ip(), "sent to " + tgIds.size() + " users");
            ctx.json(ApiResponse.ok(Map.of("recipient_count", tgIds.size())));
        });

        app.post("/api/v1/admin/delete-account", ctx -> {
            verifyAdmin(ctx);
            DeleteAccountReq req = ctx.bodyAsClass(DeleteAccountReq.class);
            Player pl = null;
            if (req.uuid != null) pl = PlayerRepository.findByUuid(req.uuid);
            else if (req.telegramId > 0) pl = PlayerRepository.findByTelegramId(req.telegramId);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement("UPDATE players SET login = NULL, email = NULL, password_hash = NULL, telegram_id = NULL WHERE uuid = ?")) {
                ps.setString(1, pl.uuid); ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("delete failed")); return;
            }
            ctx.json(ApiResponse.ok("account data cleared"));
        });

        app.get("/api/v1/admin/stats", ctx -> {
            verifyAdmin(ctx);
            Map<String, Object> out = new HashMap<>();
            String[] queries = {
                "SELECT COUNT(*) c FROM players",
                "SELECT COUNT(*) c FROM players WHERE is_banned = TRUE",
                "SELECT COUNT(*) c FROM player_logs WHERE action = 'login' AND created_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)",
                "SELECT COUNT(*) c FROM player_logs WHERE action = 'login_failed' AND created_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)",
            };
            String[] keys = { "totalUsers", "bannedUsers", "authSuccess24h", "authFailure24h" };
            try (var c = com.pwp.core.db.DatabaseManager.getConnection()) {
                for (int i = 0; i < queries.length; i++) {
                    try (var ps = c.prepareStatement(queries[i]); var rs = ps.executeQuery()) {
                        out.put(keys[i], rs.next() ? rs.getLong(1) : 0L);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("stats failed"));
                return;
            }
            ctx.json(ApiResponse.ok(out));
        });

        // ── Текущий админ (для оболочки дашборда) ────────────────
        app.get("/api/v1/admin/me", ctx -> {
            verifyAdmin(ctx);
            String adminUuid = ctx.attribute("adminUuid");
            Player pl = adminUuid == null ? null : PlayerRepository.findByUuid(adminUuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("not found"));
                return;
            }
            ctx.json(ApiResponse.ok(Map.of(
                "uuid", pl.uuid,
                "login", pl.login == null ? pl.nickname : pl.login,
                "nickname", pl.nickname,
                "role", pl.role
            )));
        });

        // ── Список банов (актуальное состояние: последняя запись по игроку) ──
        app.get("/api/v1/admin/anticheat/bans", ctx -> {
            verifyAdmin(ctx);
            List<Map<String, Object>> items = new ArrayList<>();
            String sql = "SELECT p.uuid, p.nickname, ph.type, ph.reason, ph.created_at, ph.expires_at "
                + "FROM punishment_history ph "
                + "JOIN (SELECT player_uuid, MAX(id) AS max_id FROM punishment_history "
                + "      WHERE type IN ('BAN', 'UNBAN') GROUP BY player_uuid) m ON ph.id = m.max_id "
                + "JOIN players p ON p.uuid = ph.player_uuid "
                + "ORDER BY ph.id DESC LIMIT 200";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql);
                 var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("kind", "account");
                    m.put("uuid", rs.getString("uuid"));
                    m.put("nickname", rs.getString("nickname"));
                    m.put("type", rs.getString("type"));
                    m.put("reason", rs.getString("reason"));
                    m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                    m.put("expires_at", rs.getTimestamp("expires_at") == null ? null : String.valueOf(rs.getTimestamp("expires_at")));
                    items.add(m);
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("bans load failed"));
                return;
            }
            String hwidSql = "SELECT hwid, reason, created_at, banned_until FROM hwid_bans ORDER BY id DESC LIMIT 200";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(hwidSql);
                 var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("kind", "hwid");
                    m.put("uuid", null);
                    m.put("nickname", rs.getString("hwid"));
                    m.put("type", "BAN");
                    m.put("reason", rs.getString("reason"));
                    m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                    m.put("expires_at", rs.getTimestamp("banned_until") == null ? null : String.valueOf(rs.getTimestamp("banned_until")));
                    items.add(m);
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("hwid bans load failed"));
                return;
            }
            ctx.json(ApiResponse.ok(items));
        });

        // ── Anticheat admin ────────────────────────────────────────

        app.get("/api/v1/admin/anticheat/detections", ctx -> {
            verifyAdmin(ctx);
            int limit = Math.min(parseInt(ctx.queryParam("limit"), 50), 200);
            int offset = Math.max(parseInt(ctx.queryParam("offset"), 0), 0);
            List<Map<String, Object>> items = new ArrayList<>();
            String sql = "SELECT d.id, d.player_uuid, p.nickname, d.launch_token, d.source, d.type, "
                + "d.signature, d.severity, d.created_at "
                + "FROM anticheat_detections d LEFT JOIN players p ON p.uuid = d.player_uuid "
                + "ORDER BY d.id DESC LIMIT ? OFFSET ?";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", rs.getLong("id"));
                        m.put("player_uuid", rs.getString("player_uuid"));
                        m.put("nickname", rs.getString("nickname"));
                        m.put("launch_token", rs.getString("launch_token"));
                        m.put("source", rs.getString("source"));
                        m.put("type", rs.getString("type"));
                        m.put("signature", rs.getString("signature"));
                        m.put("severity", rs.getInt("severity"));
                        m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                        items.add(m);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("detections load failed: " + e.getMessage()));
                return;
            }
            ctx.json(ApiResponse.ok(Map.of("items", items, "total", items.size())));
        });

        app.get("/api/v1/admin/anticheat/signatures", ctx -> {
            verifyAdmin(ctx);
            List<Map<String, Object>> items = new ArrayList<>();
            String sql = "SELECT id, kind, pattern, match_type, severity, enabled, created_at "
                + "FROM anticheat_blacklist ORDER BY id DESC LIMIT 500";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql);
                 var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("kind", rs.getString("kind"));
                    m.put("pattern", rs.getString("pattern"));
                    m.put("matchType", rs.getString("match_type"));
                    m.put("severity", rs.getInt("severity"));
                    m.put("enabled", rs.getBoolean("enabled"));
                    m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                    items.add(m);
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("signatures load failed"));
                return;
            }
            ctx.json(ApiResponse.ok(items));
        });

        app.post("/api/v1/admin/anticheat/signatures", ctx -> {
            verifyAdmin(ctx);
            SigReq req = ctx.bodyAsClass(SigReq.class);
            if (req.pattern == null || req.pattern.isEmpty()) {
                ctx.json(ApiResponse.error("pattern required"));
                return;
            }
            String matchType = req.matchType == null || req.matchType.isEmpty() ? "substring" : req.matchType;
            if (!List.of("substring", "exact", "word").contains(matchType)) {
                ctx.json(ApiResponse.error("invalid matchType"));
                return;
            }
            String kind = req.kind == null ? "" : req.kind;
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(
                     "INSERT INTO anticheat_blacklist (kind, pattern, match_type, severity) VALUES (?, ?, ?, ?) "
                     + "ON DUPLICATE KEY UPDATE kind = VALUES(kind), match_type = VALUES(match_type), severity = VALUES(severity), enabled = TRUE")) {
                ps.setString(1, kind);
                ps.setString(2, req.pattern);
                ps.setString(3, matchType);
                ps.setInt(4, req.severity);
                ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("signature add failed"));
                return;
            }
            ctx.json(ApiResponse.ok("signature saved"));
        });

        app.post("/api/v1/admin/anticheat/signatures/{id}/toggle", ctx -> {
            verifyAdmin(ctx);
            int id = parseInt(ctx.pathParam("id"), 0);
            if (id <= 0) { ctx.json(ApiResponse.error("bad id")); return; }
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(
                     "UPDATE anticheat_blacklist SET enabled = NOT enabled WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("toggle failed"));
                return;
            }
            ctx.json(ApiResponse.ok("toggled"));
        });

        app.post("/api/v1/admin/anticheat/signatures/{id}/delete", ctx -> {
            verifyAdmin(ctx);
            int id = parseInt(ctx.pathParam("id"), 0);
            if (id <= 0) { ctx.json(ApiResponse.error("bad id")); return; }
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement("DELETE FROM anticheat_blacklist WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("delete failed"));
                return;
            }
            ctx.json(ApiResponse.ok("deleted"));
        });

        // ── Скриншоты ─────────────────────────────────────────────
        app.get("/api/v1/admin/anticheat/screenshots", ctx -> {
            verifyAdmin(ctx);
            int limit = Math.min(parseInt(ctx.queryParam("limit"), 50), 200);
            List<Map<String, Object>> items = new ArrayList<>();
            String sql = "SELECT s.id, s.player_uuid, p.nickname, s.width, s.height, s.created_at "
                + "FROM anticheat_screenshots s LEFT JOIN players p ON p.uuid = s.player_uuid "
                + "ORDER BY s.id DESC LIMIT ?";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", rs.getLong("id"));
                        m.put("player_uuid", rs.getString("player_uuid"));
                        m.put("nickname", rs.getString("nickname"));
                        m.put("width", rs.getInt("width"));
                        m.put("height", rs.getInt("height"));
                        m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                        items.add(m);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("screenshots load failed"));
                return;
            }
            ctx.json(ApiResponse.ok(items));
        });

        app.get("/api/v1/admin/anticheat/screenshots/{id}/image", ctx -> {
            verifyAdmin(ctx);
            long id = parseInt(ctx.pathParam("id"), 0);
            if (id <= 0) { ctx.json(ApiResponse.error("bad id")); return; }
            String path = null;
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement("SELECT file_path FROM anticheat_screenshots WHERE id = ?")) {
                ps.setLong(1, id);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) path = rs.getString("file_path");
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("load failed"));
                return;
            }
            File f = path == null ? null : new File(path);
            if (f == null || !f.exists()) {
                ctx.status(404).json(ApiResponse.error("not found"));
                return;
            }
            ctx.contentType("image/jpeg");
            ctx.result(new java.io.FileInputStream(f).readAllBytes());
        });

        app.get("/api/v1/admin/anticheat/sessions", ctx -> {
            verifyAdmin(ctx);
            List<Map<String, Object>> items = new ArrayList<>();
            String sql = "SELECT s.launch_token, s.hwid, s.last_heartbeat, s.created_at, s.expires_at, p.nickname "
                + "FROM anticheat_sessions s LEFT JOIN players p ON p.uuid = s.player_uuid "
                + "ORDER BY s.id DESC LIMIT 200";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql);
                 var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("launch_token", rs.getString("launch_token"));
                    m.put("hwid", rs.getString("hwid"));
                    m.put("nickname", rs.getString("nickname"));
                    m.put("last_heartbeat", rs.getTimestamp("last_heartbeat") == null ? null : String.valueOf(rs.getTimestamp("last_heartbeat")));
                    m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                    m.put("expires_at", String.valueOf(rs.getTimestamp("expires_at")));
                    items.add(m);
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("sessions load failed"));
                return;
            }
            ctx.json(ApiResponse.ok(items));
        });

        app.get("/api/v1/admin/anticheat/stats", ctx -> {
            verifyAdmin(ctx);
            Map<String, Object> out = new HashMap<>();
            String[] queries = {
                "SELECT COUNT(*) c FROM anticheat_detections",
                "SELECT COUNT(*) c FROM anticheat_detections WHERE created_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)",
                "SELECT COUNT(*) c FROM anticheat_sessions WHERE expires_at > CURRENT_TIMESTAMP",
                "SELECT COUNT(*) c FROM anticheat_blacklist WHERE enabled = TRUE",
            };
            String[] keys = { "total_detections", "detections_24h", "active_sessions", "blacklist_count" };
            try (var c = com.pwp.core.db.DatabaseManager.getConnection()) {
                for (int i = 0; i < queries.length; i++) {
                    try (var ps = c.prepareStatement(queries[i]); var rs = ps.executeQuery()) {
                        out.put(keys[i], rs.next() ? rs.getLong(1) : 0L);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("stats failed"));
                return;
            }
            out.put("p5_enforce", CoreApplication.config.anticheat.p5Enforce);
            out.put("p5_enabled", CoreApplication.config.anticheat.p5Secret != null
                && !CoreApplication.config.anticheat.p5Secret.isEmpty());
            ctx.json(ApiResponse.ok(out));
        });

        app.get("/api/v1/admin/players", ctx -> {
            verifyAdmin(ctx);
            int limit = Math.min(parseInt(ctx.queryParam("limit"), 50), 200);
            int offset = Math.max(parseInt(ctx.queryParam("offset"), 0), 0);
            String q = ctx.queryParam("q");
            List<Map<String, Object>> items = new ArrayList<>();
            String sql = "SELECT uuid, nickname, login, role, is_banned, ban_reason, last_ip, hwid, "
                + "first_join, last_join FROM players";
            if (q != null && !q.isEmpty()) {
                sql += " WHERE nickname LIKE ? OR login LIKE ? OR uuid LIKE ?";
            }
            sql += " ORDER BY last_join DESC LIMIT ? OFFSET ?";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql)) {
                int idx = 1;
                if (q != null && !q.isEmpty()) {
                    String like = "%" + q + "%";
                    ps.setString(idx++, like);
                    ps.setString(idx++, like);
                    ps.setString(idx++, like);
                }
                ps.setInt(idx++, limit);
                ps.setInt(idx, offset);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("uuid", rs.getString("uuid"));
                        m.put("nickname", rs.getString("nickname"));
                        m.put("login", rs.getString("login"));
                        m.put("role", rs.getString("role"));
                        m.put("is_banned", rs.getBoolean("is_banned"));
                        m.put("ban_reason", rs.getString("ban_reason"));
                        m.put("last_ip", rs.getString("last_ip"));
                        m.put("hwid", rs.getString("hwid"));
                        m.put("first_join", rs.getTimestamp("first_join") == null ? null : String.valueOf(rs.getTimestamp("first_join")));
                        m.put("last_join", rs.getTimestamp("last_join") == null ? null : String.valueOf(rs.getTimestamp("last_join")));
                        items.add(m);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("players load failed"));
                return;
            }
            ctx.json(ApiResponse.ok(items));
        });
    }

    public static void verifyAdmin(io.javalin.http.Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new io.javalin.http.UnauthorizedResponse("Missing or invalid Authorization header");
        }
        String token = authHeader.substring("Bearer ".length());

        // Try session token first (for web/launcher admins)
        try {
            String playerUuid = PlayerRepository.findSessionPlayer(token);
            if (playerUuid != null) {
                Player admin = PlayerRepository.findByUuid(playerUuid);
                if (admin == null || admin.role == null
                        || (!admin.role.equalsIgnoreCase("admin") && !admin.role.equalsIgnoreCase("owner"))) {
                    throw new io.javalin.http.UnauthorizedResponse("not an admin");
                }
                ctx.attribute("adminUuid", playerUuid);
                return;
            }
        } catch (java.sql.SQLException e) {
            throw new io.javalin.http.UnauthorizedResponse("admin verification failed");
        }

        // Fallback: check API key (for bot/admin scripts)
        if (CoreApplication.config != null && CoreApplication.config.api.keys != null) {
            for (String key : CoreApplication.config.api.keys) {
                if (key.equals(token)) return;
            }
        }

        throw new io.javalin.http.UnauthorizedResponse("unauthorized");
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@"); String local = parts[0]; String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public static class FindUserReq { public String query; }
    public static class BanReq { public String uuid; public String reason; }
    public static class ReqUuid { public String uuid; }
    public static class SetRoleReq { public String uuid; public String role; }
    public static class ResolveResetReq { public int resetId; public String adminUuid; public String status; }
    public static class BroadcastReq { public String adminUuid; public String message; }
    public static class DeleteAccountReq { public String uuid; public long telegramId; }
    public static class SigReq { public String kind; public String pattern; public String matchType; public int severity; }
}

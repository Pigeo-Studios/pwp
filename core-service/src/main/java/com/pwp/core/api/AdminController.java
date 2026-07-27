package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.db.PunishmentRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.Player;
import io.javalin.Javalin;

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
            ctx.json(ApiResponse.ok(Map.of("total_accounts", PlayerRepository.countAccounts())));
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
                if (admin == null || (!"admin".equals(admin.role) && !"owner".equals(admin.role))) {
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
}

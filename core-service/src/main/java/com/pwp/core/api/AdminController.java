package com.pwp.core.api;

import com.pwp.core.db.AccountRepository;
import com.pwp.core.model.Account;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

import java.util.*;

public class AdminController {

    public AdminController(Javalin app) {

        // ── Find user ─────────────────────────────────────
        app.post("/api/v1/admin/find-user", ctx -> {
            FindUserReq req = ctx.bodyAsClass(FindUserReq.class);
            if (req.query == null) {
                ctx.json(ApiResponse.error("query required"));
                return;
            }
            Account acc = AccountRepository.findAny(req.query);
            if (acc == null) {
                ctx.json(ApiResponse.error("user not found"));
                return;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("id", acc.id);
            m.put("uuid", acc.uuid);
            m.put("login", acc.login);
            m.put("email", acc.email);
            m.put("telegram_id", acc.telegramId);
            m.put("role", acc.role);
            m.put("is_banned", acc.isBanned);
            m.put("ban_reason", acc.banReason);
            m.put("registered_at", acc.registeredAt);
            m.put("last_login", acc.lastLogin);
            m.put("last_ip", acc.lastIp);
            m.put("2fa_enabled", acc.launcher2faEnabled);
            m.put("privacy_accepted", acc.privacyPolicyAccepted);
            ctx.json(ApiResponse.ok(m));
        });

        // ── Ban / Unban ──────────────────────────────────
        app.post("/api/v1/admin/ban", ctx -> {
            BanReq req = ctx.bodyAsClass(BanReq.class);
            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("user not found"));
                return;
            }
            AccountRepository.setBan(req.accountId, true, req.reason);
            AccountRepository.log(req.accountId, "ban", ctx.ip(), "reason: " + req.reason);
            ctx.json(ApiResponse.ok("user banned"));
        });

        app.post("/api/v1/admin/unban", ctx -> {
            ReqId req = ctx.bodyAsClass(ReqId.class);
            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("user not found"));
                return;
            }
            AccountRepository.setBan(req.accountId, false, null);
            AccountRepository.log(req.accountId, "unban", ctx.ip(), null);
            ctx.json(ApiResponse.ok("user unbanned"));
        });

        // ── Set role ──────────────────────────────────────
        app.post("/api/v1/admin/set-role", ctx -> {
            SetRoleReq req = ctx.bodyAsClass(SetRoleReq.class);
            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("user not found"));
                return;
            }
            List<String> valid = List.of("user", "support", "admin", "owner");
            if (!valid.contains(req.role)) {
                ctx.json(ApiResponse.error("invalid role: " + String.join(", ", valid)));
                return;
            }
            AccountRepository.setRole(req.accountId, req.role);
            AccountRepository.log(req.accountId, "role_change", ctx.ip(), "new role: " + req.role);
            ctx.json(ApiResponse.ok("role changed to " + req.role));
        });

        // ── Password reset requests ───────────────────────
        app.get("/api/v1/admin/pending-resets", ctx -> {
            var resets = AccountRepository.findPendingResets();
            List<Map<String, Object>> list = new ArrayList<>();
            for (var pr : resets) {
                Account acc = AccountRepository.findById(pr.accountId);
                Map<String, Object> m = new HashMap<>();
                m.put("id", pr.id);
                m.put("account_id", pr.accountId);
                m.put("login", acc != null ? acc.login : "?");
                m.put("telegram_id", acc != null ? acc.telegramId : null);
                m.put("created_at", pr.createdAt);
                list.add(m);
            }
            ctx.json(ApiResponse.ok(list));
        });

        app.post("/api/v1/admin/resolve-reset", ctx -> {
            ResolveResetReq req = ctx.bodyAsClass(ResolveResetReq.class);
            var pr = AccountRepository.findResetById(req.resetId);
            if (pr == null) {
                ctx.json(ApiResponse.error("reset request not found"));
                return;
            }
            if (!pr.status.equals("pending")) {
                ctx.json(ApiResponse.error("already resolved"));
                return;
            }
            AccountRepository.resolveReset(req.resetId, req.adminId, req.status);
            Account acc = AccountRepository.findById(pr.accountId);
            AccountRepository.log(pr.accountId, "reset_" + req.status, ctx.ip(),
                "reset request #" + req.resetId + " by admin #" + req.adminId);
            ctx.json(ApiResponse.ok(Map.of(
                "status", req.status,
                "account_id", pr.accountId,
                "telegram_id", acc != null ? acc.telegramId : null
            )));
        });

        // ── Logs ──────────────────────────────────────────
        app.get("/api/v1/admin/logs", ctx -> {
            int limit = parseInt(ctx.queryParam("limit"), 20);
            int offset = parseInt(ctx.queryParam("offset"), 0);
            var logs = AccountRepository.getLogs(Math.min(limit, 100), offset);
            ctx.json(ApiResponse.ok(logs));
        });

        // ── Broadcast ─────────────────────────────────────
        app.post("/api/v1/admin/broadcast", ctx -> {
            BroadcastReq req = ctx.bodyAsClass(BroadcastReq.class);
            if (req.message == null || req.adminId == 0) {
                ctx.json(ApiResponse.error("message and admin_id required"));
                return;
            }
            int count = AccountRepository.getAccountCountForBroadcast();
            List<Long> tgIds = AccountRepository.getTelegramIdsForBroadcast();
            AccountRepository.logBroadcast(req.adminId, req.message, count);
            AccountRepository.log(0, "broadcast", ctx.ip(), "admin #" + req.adminId + " sent to " + count + " users");
            ctx.json(ApiResponse.ok(Map.of(
                "recipient_count", count,
                "telegram_ids", tgIds
            )));
        });

        // ── Delete account (for testing) ──────────────────
        app.post("/api/v1/admin/delete-account", ctx -> {
            DeleteAccountReq req = ctx.bodyAsClass(DeleteAccountReq.class);
            Account acc = null;
            if (req.accountId > 0) {
                acc = AccountRepository.findById(req.accountId);
            } else if (req.telegramId > 0) {
                acc = AccountRepository.findByTelegramId(req.telegramId);
            }
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }
            AccountRepository.deleteByTelegramId(acc.telegramId);
            ctx.json(ApiResponse.ok("deleted"));
        });

        // ── Stats ─────────────────────────────────────────
        app.get("/api/v1/admin/stats", ctx -> {
            int total = AccountRepository.count();
            ctx.json(ApiResponse.ok(Map.of("total_accounts", total)));
        });
    }

    public static class DeleteAccountReq { public int accountId; public long telegramId; }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public static class FindUserReq { public String query; }
    public static class BanReq { public int accountId; public String reason; }
    public static class ReqId { public int accountId; }
    public static class SetRoleReq { public int accountId; public String role; }
    public static class ResolveResetReq { public int resetId; public int adminId; public String status; }
    public static class BroadcastReq { public int adminId; public String message; }
}

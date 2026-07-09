package com.pwp.core.api;

import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class HWIDBanController {

    public HWIDBanController(Javalin app) {

        // ── Ban HWID ──────────────────────────────────────
        app.post("/api/v1/admin/hwid-ban", ctx -> {
            AdminController.verifyAdmin(ctx);
            BanHwidReq req = ctx.bodyAsClass(BanHwidReq.class);
            if (req.hwid == null || req.hwid.isEmpty()) {
                ctx.json(ApiResponse.error("hwid required"));
                return;
            }
            String adminUuid = ctx.attribute("adminUuid");
            if (adminUuid == null) adminUuid = "system";

            String sql = "INSERT IGNORE INTO hwid_bans (hwid, reason, banned_by) VALUES (?, ?, ?)";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, req.hwid);
                ps.setString(2, req.reason);
                ps.setString(3, adminUuid);
                ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("hwid ban failed"));
                return;
            }

            // Also ban all accounts with this HWID
            String banAccounts = "UPDATE players SET is_banned = TRUE, ban_reason = ? WHERE uuid IN (SELECT account_uuid FROM hwid_history WHERE hwid = ?)";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(banAccounts)) {
                ps.setString(1, "HWID BAN: " + (req.reason != null ? req.reason : ""));
                ps.setString(2, req.hwid);
                ps.executeUpdate();
            } catch (Exception ignored) {}

            ctx.json(ApiResponse.ok("hwid banned"));
        });

        // ── Unban HWID ────────────────────────────────────
        app.post("/api/v1/admin/hwid-unban", ctx -> {
            AdminController.verifyAdmin(ctx);
            UnbanHwidReq req = ctx.bodyAsClass(UnbanHwidReq.class);
            if (req.hwid == null || req.hwid.isEmpty()) {
                ctx.json(ApiResponse.error("hwid required"));
                return;
            }
            String sql = "DELETE FROM hwid_bans WHERE hwid = ?";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, req.hwid);
                ps.executeUpdate();
            }
            ctx.json(ApiResponse.ok("hwid unbanned"));
        });

        // ── List HWID bans ────────────────────────────────
        app.get("/api/v1/admin/hwid-bans", ctx -> {
            AdminController.verifyAdmin(ctx);
            List<Map<String, Object>> bans = new ArrayList<>();
            String sql = "SELECT hb.*, p.login AS banned_by_login FROM hwid_bans hb " +
                         "LEFT JOIN players p ON hb.banned_by = p.uuid ORDER BY hb.id DESC LIMIT 100";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("id", rs.getInt("id"));
                    b.put("hwid", rs.getString("hwid"));
                    b.put("reason", rs.getString("reason"));
                    b.put("banned_by", rs.getString("banned_by_login"));
                    b.put("created_at", rs.getString("created_at"));
                    bans.add(b);
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("list failed"));
                return;
            }
            ctx.json(ApiResponse.ok(bans));
        });

        // ── Check HWID by player UUID ─────────────────────
        app.get("/api/v1/admin/player-hwid", ctx -> {
            AdminController.verifyAdmin(ctx);
            String uuid = ctx.queryParam("uuid");
            if (uuid == null) {
                ctx.json(ApiResponse.error("uuid required"));
                return;
            }
            List<Map<String, Object>> history = new ArrayList<>();
            String sql = "SELECT * FROM hwid_history WHERE account_uuid = ? ORDER BY id DESC LIMIT 20";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> h = new HashMap<>();
                        h.put("hwid", rs.getString("hwid"));
                        h.put("pc_name", rs.getString("pc_name"));
                        h.put("ip", rs.getString("ip"));
                        h.put("flags", rs.getInt("flags"));
                        h.put("created_at", rs.getString("created_at"));
                        history.add(h);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("query failed"));
                return;
            }
            ctx.json(ApiResponse.ok(history));
        });
    }

    private static class BanHwidReq { public String hwid; public String reason; }
    private static class UnbanHwidReq { public String hwid; }
}

package com.pwp.core.api;

import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerId;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PunishmentController {

    public PunishmentController(Javalin app) {
        // ── История наказаний игрока (ник или UUID) ─────────
        app.get("/api/v1/punishments/{uuid}", ctx -> {
            String target = ctx.pathParam("uuid");
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Connection c = DatabaseManager.getConnection()) {
                String uuid = PlayerId.resolve(c, target);
                if (uuid == null) {
                    ctx.json(ApiResponse.ok(rows));
                    return;
                }
                try (PreparedStatement ps = c.prepareStatement(
                     "SELECT type, reason, admin_uuid, duration_minutes, expires_at, created_at "
                     + "FROM punishment_history WHERE player_uuid = ? ORDER BY created_at DESC, id DESC LIMIT 100")) {
                    ps.setString(1, uuid);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> r = new LinkedHashMap<>();
                            r.put("type", rs.getString("type"));
                            r.put("reason", rs.getString("reason"));
                            r.put("admin_uuid", rs.getString("admin_uuid"));
                            Integer dm = rs.getInt("duration_minutes");
                            r.put("duration_minutes", rs.wasNull() ? null : dm);
                            r.put("expires_at", rs.getTimestamp("expires_at"));
                            r.put("created_at", rs.getTimestamp("created_at"));
                            rows.add(r);
                        }
                    }
                }
            }
            ctx.json(ApiResponse.ok(rows));
        });

        // ── Список актуально забаненных (для TAB-подсказок /pwp unban) ──
        app.get("/api/v1/punishments/banned", ctx -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT uuid, nickname, ban_reason, banned_until FROM players "
                     + "WHERE is_banned = TRUE OR (banned_until IS NOT NULL AND banned_until > NOW()) "
                     + "ORDER BY nickname")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("uuid", rs.getString("uuid"));
                        r.put("nickname", rs.getString("nickname"));
                        r.put("reason", rs.getString("ban_reason"));
                        r.put("until", rs.getTimestamp("banned_until"));
                        rows.add(r);
                    }
                }
            }
            ctx.json(ApiResponse.ok(rows));
        });
    }
}

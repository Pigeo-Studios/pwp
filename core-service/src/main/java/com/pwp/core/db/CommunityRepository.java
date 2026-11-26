package com.pwp.core.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Публичный сервер (этап 10 ROADMAP): жалобы игроков ({@code player_reports}), история наказаний для
 * админ-панели ({@code punishment_history} — таблица старая, писали только баны), привязка Discord
 * ({@code discord_links}) и публичный банлист для сайта. Таблицы создаёт {@code DatabaseManager}.
 */
public class CommunityRepository {

    // ---------------------------------------------------------------- жалобы

    public static class Report {
        public long id;
        public String server;
        public String reporterUuid;
        public String reporterName;
        public String targetUuid;
        public String targetName;
        public String reason;
        public String details;
        public String status;
        public String handledBy;
        public long createdAt;
        public long closedAt;
    }

    public static long addReport(Report r) throws SQLException {
        String sql = "INSERT INTO player_reports (server, reporter_uuid, reporter_name, target_uuid, target_name, "
                + "reason, details, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'open', ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.server);
            ps.setString(2, r.reporterUuid);
            ps.setString(3, r.reporterName);
            ps.setString(4, r.targetUuid);
            ps.setString(5, r.targetName);
            ps.setString(6, r.reason);
            ps.setString(7, r.details);
            ps.setLong(8, r.createdAt);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                return k.next() ? k.getLong(1) : 0;
            }
        }
    }

    /** Жалобы со статусом {@code status} (null — все) и id больше {@code afterId}, новые сверху. */
    public static List<Report> reports(String status, long afterId, int limit) throws SQLException {
        String sql = "SELECT * FROM player_reports WHERE id > ?" + (status == null ? "" : " AND status = ?")
                + " ORDER BY id DESC LIMIT ?";
        List<Report> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, afterId);
            if (status != null) ps.setString(i++, status);
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Report r = new Report();
                    r.id = rs.getLong("id");
                    r.server = rs.getString("server");
                    r.reporterUuid = rs.getString("reporter_uuid");
                    r.reporterName = rs.getString("reporter_name");
                    r.targetUuid = rs.getString("target_uuid");
                    r.targetName = rs.getString("target_name");
                    r.reason = rs.getString("reason");
                    r.details = rs.getString("details");
                    r.status = rs.getString("status");
                    r.handledBy = rs.getString("handled_by");
                    r.createdAt = rs.getLong("created_at");
                    r.closedAt = rs.getLong("closed_at");
                    out.add(r);
                }
            }
        }
        return out;
    }

    public static boolean closeReport(long id, String by, long now) throws SQLException {
        String sql = "UPDATE player_reports SET status = 'closed', handled_by = ?, closed_at = ? "
                + "WHERE id = ? AND status = 'open'";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, by);
            ps.setLong(2, now);
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------- наказания

    /** Последние {@code limit} записей игрока (новые сверху): type, reason, admin, minutes, created, expires. */
    public static List<Map<String, Object>> punishments(String uuid, int limit) throws SQLException {
        String sql = "SELECT ph.type, ph.reason, ph.duration_minutes, ph.created_at, ph.expires_at, p.nickname AS admin "
                + "FROM punishment_history ph LEFT JOIN players p ON p.uuid = ph.admin_uuid "
                + "WHERE ph.player_uuid = ? ORDER BY ph.id DESC LIMIT ?";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", rs.getString("type"));
                    m.put("reason", rs.getString("reason"));
                    int min = rs.getInt("duration_minutes");
                    m.put("minutes", rs.wasNull() ? null : min);
                    m.put("admin", rs.getString("admin"));
                    Timestamp created = rs.getTimestamp("created_at");
                    Timestamp expires = rs.getTimestamp("expires_at");
                    m.put("createdAt", created == null ? 0 : created.getTime());
                    m.put("expiresAt", expires == null ? 0 : expires.getTime());
                    out.add(m);
                }
            }
        }
        return out;
    }

    /** Активные баны (последняя запись BAN/UNBAN игрока — BAN и не истёк) — для сайта, без uuid/IP. */
    public static List<Map<String, Object>> activeBans(int limit) throws SQLException {
        String sql = "SELECT p.nickname, ph.reason, ph.created_at, ph.expires_at FROM punishment_history ph "
                + "JOIN (SELECT player_uuid, MAX(id) AS max_id FROM punishment_history "
                + "      WHERE type IN ('BAN', 'UNBAN') GROUP BY player_uuid) m ON ph.id = m.max_id "
                + "JOIN players p ON p.uuid = ph.player_uuid "
                + "WHERE ph.type = 'BAN' AND (ph.expires_at IS NULL OR ph.expires_at > NOW()) "
                + "ORDER BY ph.id DESC LIMIT ?";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nickname", rs.getString("nickname"));
                    m.put("reason", rs.getString("reason"));
                    Timestamp created = rs.getTimestamp("created_at");
                    Timestamp expires = rs.getTimestamp("expires_at");
                    m.put("createdAt", created == null ? 0 : created.getTime());
                    m.put("expiresAt", expires == null ? 0 : expires.getTime());
                    out.add(m);
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- Discord

    public static void linkDiscord(String uuid, String discordId, long now) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            // один Discord — один аккаунт: старую привязку этого Discord снимаем
            try (PreparedStatement del = c.prepareStatement("DELETE FROM discord_links WHERE discord_id = ? AND uuid <> ?")) {
                del.setString(1, discordId);
                del.setString(2, uuid);
                del.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO discord_links (uuid, discord_id, linked_at) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id), linked_at = VALUES(linked_at)")) {
                ps.setString(1, uuid);
                ps.setString(2, discordId);
                ps.setLong(3, now);
                ps.executeUpdate();
            }
        }
    }

    /** {@code column} — "uuid" или "discord_id". Возвращает {uuid, nickname, discordId, linkedAt} или null. */
    public static Map<String, Object> findLink(String column, String value) throws SQLException {
        if (!column.equals("uuid") && !column.equals("discord_id")) throw new IllegalArgumentException(column);
        String sql = "SELECT d.uuid, d.discord_id, d.linked_at, p.nickname FROM discord_links d "
                + "LEFT JOIN players p ON p.uuid = d.uuid WHERE d." + column + " = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("uuid", rs.getString("uuid"));
                m.put("nickname", rs.getString("nickname"));
                m.put("discordId", rs.getString("discord_id"));
                m.put("linkedAt", rs.getLong("linked_at"));
                return m;
            }
        }
    }
}

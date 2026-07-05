package com.pwp.core.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoiceMuteRepository {

    public static VoiceMuteData findByUuid(String uuid) throws SQLException {
        String sql = "SELECT * FROM voice_mutes WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMute(rs);
            }
        }
        return null;
    }

    public static void setMute(String uuid, String mutedByUuid, String mutedByNickname,
                                String reason, long expiresAt) throws SQLException {
        String sql = "INSERT INTO voice_mutes (uuid, muted_by_uuid, muted_by_nickname, reason, muted_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE muted_by_uuid = VALUES(muted_by_uuid), "
                + "muted_by_nickname = VALUES(muted_by_nickname), reason = VALUES(reason), "
                + "muted_at = VALUES(muted_at), expires_at = VALUES(expires_at)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, mutedByUuid);
            ps.setString(3, mutedByNickname);
            ps.setString(4, reason != null ? reason : "");
            ps.setLong(5, System.currentTimeMillis());
            ps.setLong(6, expiresAt);
            ps.executeUpdate();
        }
    }

    public static void removeMute(String uuid) throws SQLException {
        String sql = "DELETE FROM voice_mutes WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }

    public static List<VoiceMuteData> getAllActive() throws SQLException {
        List<VoiceMuteData> result = new ArrayList<>();
        String sql = "SELECT * FROM voice_mutes WHERE expires_at = 0 OR expires_at > ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapMute(rs));
            }
        }
        return result;
    }

    public static void cleanupExpired() throws SQLException {
        String sql = "DELETE FROM voice_mutes WHERE expires_at > 0 AND expires_at <= ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private static VoiceMuteData mapMute(ResultSet rs) throws SQLException {
        VoiceMuteData data = new VoiceMuteData();
        data.uuid = rs.getString("uuid");
        data.mutedByUuid = rs.getString("muted_by_uuid");
        data.mutedByNickname = rs.getString("muted_by_nickname");
        data.reason = rs.getString("reason");
        data.mutedAt = rs.getLong("muted_at");
        data.expiresAt = rs.getLong("expires_at");
        return data;
    }

    public static class VoiceMuteData {
        public String uuid;
        public String mutedByUuid;
        public String mutedByNickname;
        public String reason;
        public long mutedAt;
        public long expiresAt;

        public boolean isActive() {
            return expiresAt == 0 || expiresAt > System.currentTimeMillis();
        }

        public boolean isPermanent() {
            return expiresAt == 0;
        }

        public long remainingMs() {
            if (expiresAt == 0) return -1;
            return Math.max(0, expiresAt - System.currentTimeMillis());
        }
    }
}

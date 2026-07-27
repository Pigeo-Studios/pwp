package com.pwp.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class PunishmentRepository {

    public static void addRecord(String playerUuid, String type, String reason,
                                  String adminUuid, Integer durationMinutes,
                                  Timestamp expiresAt) throws java.sql.SQLException {
        String sql = "INSERT INTO punishment_history (player_uuid, type, reason, admin_uuid, duration_minutes, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, type);
            if (reason != null) ps.setString(3, reason);
            else ps.setNull(3, java.sql.Types.VARCHAR);
            if (adminUuid != null) ps.setString(4, adminUuid);
            else ps.setNull(4, java.sql.Types.VARCHAR);
            if (durationMinutes != null) ps.setInt(5, durationMinutes);
            else ps.setNull(5, java.sql.Types.INTEGER);
            if (expiresAt != null) ps.setTimestamp(6, expiresAt);
            else ps.setNull(6, java.sql.Types.TIMESTAMP);
            ps.executeUpdate();
        }
    }
}

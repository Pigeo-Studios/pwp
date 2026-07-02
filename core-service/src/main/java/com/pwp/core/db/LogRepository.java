package com.pwp.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class LogRepository {

    public static void log(String uuid, String operationType, long amount, String details) {
        String sql = "INSERT INTO operation_logs (uuid, operation_type, amount, details) VALUES (?, ?, ?, ?::json)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, operationType);
            ps.setLong(3, amount);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }
}

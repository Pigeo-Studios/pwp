package com.pwp.core.db;

import com.pwp.core.model.DonationTransaction;

import java.sql.*;

public class DonationRepository {

    public static long create(String uuid, String itemId, double amount, String currency,
                               String paymentId) throws SQLException {
        String sql = "INSERT INTO donation_transactions (uuid, item_id, amount, currency, payment_id, status) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDING')";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, uuid);
            ps.setString(2, itemId);
            ps.setDouble(3, amount);
            ps.setString(4, currency);
            ps.setString(5, paymentId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getLong(1);
        }
    }

    public static boolean complete(long id) throws SQLException {
        String sql = "UPDATE donation_transactions SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING'";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}

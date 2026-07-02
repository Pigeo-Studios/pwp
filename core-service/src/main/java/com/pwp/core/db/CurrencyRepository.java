package com.pwp.core.db;

import java.sql.*;

public class CurrencyRepository {

    public static long getBalance(String uuid) throws SQLException {
        String sql = "SELECT coins FROM player_currency WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("coins");
            }
        }
        return 0;
    }

    public static boolean add(String uuid, long amount) throws SQLException {
        String sql = "UPDATE player_currency SET coins = coins + ?, total_earned = total_earned + ? WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setLong(2, amount);
            ps.setString(3, uuid);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean spend(String uuid, long amount) throws SQLException {
        String sql = "UPDATE player_currency SET coins = coins - ?, total_spent = total_spent + ? WHERE uuid = ? AND coins >= ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setLong(2, amount);
            ps.setString(3, uuid);
            ps.setLong(4, amount);
            return ps.executeUpdate() > 0;
        }
    }
}

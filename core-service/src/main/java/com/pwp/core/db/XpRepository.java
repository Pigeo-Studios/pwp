package com.pwp.core.db;

import java.sql.*;

public class XpRepository {

    public static XpData get(String uuid) throws Exception {
        String sql = "SELECT * FROM player_xp WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    XpData d = new XpData();
                    d.xp = rs.getLong("xp");
                    d.level = rs.getInt("level");
                    d.prestige = rs.getInt("prestige");
                    return d;
                }
            }
        }
        return null;
    }

    public static XpData addXp(String uuid, long amount) throws Exception {
        Connection c = DatabaseManager.getConnection();
        c.setAutoCommit(false);
        try {
            // Ensure player exists (creates players + player_xp rows if missing)
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO players (uuid, nickname) VALUES (?, 'unknown')")) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO player_xp (uuid) VALUES (?)")) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }

            PreparedStatement ps = c.prepareStatement(
                    "UPDATE player_xp SET xp = xp + ? WHERE uuid = ?");
            ps.setLong(1, amount);
            ps.setString(2, uuid);
            ps.executeUpdate();
            ps.close();

            ps = c.prepareStatement("SELECT xp, level, prestige FROM player_xp WHERE uuid = ?");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                c.rollback();
                throw new SQLException("Player not found: " + uuid);
            }
            long newXp = rs.getLong("xp");
            int level = rs.getInt("level");
            int prestige = rs.getInt("prestige");
            rs.close();
            ps.close();

            long xpForNext = level * 1000L;
            boolean leveledUp = false;
            while (newXp >= xpForNext) {
                newXp -= xpForNext;
                level++;
                xpForNext = level * 1000L;
                leveledUp = true;
            }

            if (leveledUp) {
                ps = c.prepareStatement("UPDATE player_xp SET xp = ?, level = ? WHERE uuid = ?");
                ps.setLong(1, newXp);
                ps.setInt(2, level);
                ps.setString(3, uuid);
                ps.executeUpdate();
                ps.close();
            }

            c.commit();

            XpData d = new XpData();
            d.xp = newXp;
            d.level = level;
            d.prestige = prestige;
            d.xpForNext = xpForNext;
            d.leveledUp = leveledUp;
            return d;
        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
            c.close();
        }
    }

    public static class XpData {
        public long xp;
        public int level;
        public int prestige;
        public long xpForNext;
        public boolean leveledUp;
    }
}

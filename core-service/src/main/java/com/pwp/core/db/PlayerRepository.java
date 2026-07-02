package com.pwp.core.db;

import com.pwp.core.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerRepository {

    public static Player findByUuid(String uuid) throws SQLException {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlayer(rs);
            }
        }
        return null;
    }

    public static Player createOrUpdate(String uuid, String nickname) throws SQLException {
        String sql = "INSERT INTO players (uuid, nickname) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), last_join = CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, nickname);
            ps.executeUpdate();
        }
        ensureRowExists(uuid, "player_stats");
        ensureRowExists(uuid, "player_currency");
        ensureRowExists(uuid, "player_xp");
        return findByUuid(uuid);
    }

    private static void ensureRowExists(String uuid, String table) throws SQLException {
        String sql = "INSERT IGNORE INTO " + table + " (uuid) VALUES (?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }

    public static PlayerStats getStats(String uuid) throws SQLException {
        String sql = "SELECT * FROM player_stats WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapStats(rs);
            }
        }
        return null;
    }

    public static void updateStats(String uuid, PlayerStats delta) throws SQLException {
        String sql = "UPDATE player_stats SET kills = kills + ?, deaths = deaths + ?, " +
                "wins = wins + ?, losses = losses + ?, playtime_seconds = playtime_seconds + ?, " +
                "shots_fired = shots_fired + ?, shots_hit = shots_hit + ?, revives = revives + ? " +
                "WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, delta.kills);
            ps.setInt(2, delta.deaths);
            ps.setInt(3, delta.wins);
            ps.setInt(4, delta.losses);
            ps.setLong(5, delta.playtimeSeconds);
            ps.setInt(6, delta.shotsFired);
            ps.setInt(7, delta.shotsHit);
            ps.setInt(8, delta.revives);
            ps.setString(9, uuid);
            ps.executeUpdate();
        }
    }

    public static List<PlayerProfile> getLeaderboard(String orderBy, int limit, int offset) throws SQLException {
        String column = switch (orderBy) {
            case "kills" -> "ps.kills";
            case "wins" -> "ps.wins";
            case "winrate" -> "(ps.wins / GREATEST(ps.wins + ps.losses, 1))";
            case "playtime" -> "ps.playtime_seconds";
            default -> "ps.kills";
        };
        String sql = "SELECT p.uuid, p.nickname, ps.kills, ps.deaths, ps.wins, ps.losses, " +
                "ps.playtime_seconds, pc.coins, px.level, px.prestige " +
                "FROM players p " +
                "JOIN player_stats ps ON p.uuid = ps.uuid " +
                "JOIN player_currency pc ON p.uuid = pc.uuid " +
                "JOIN player_xp px ON p.uuid = px.uuid " +
                "ORDER BY " + column + " DESC LIMIT ? OFFSET ?";
        List<PlayerProfile> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerProfile pp = new PlayerProfile();
                    pp.player = new Player();
                    pp.player.uuid = rs.getString("uuid");
                    pp.player.nickname = rs.getString("nickname");
                    pp.stats = new PlayerStats();
                    pp.stats.kills = rs.getInt("kills");
                    pp.stats.deaths = rs.getInt("deaths");
                    pp.stats.wins = rs.getInt("wins");
                    pp.stats.losses = rs.getInt("losses");
                    pp.stats.playtimeSeconds = rs.getLong("playtime_seconds");
                    pp.coins = rs.getLong("coins");
                    pp.level = rs.getInt("level");
                    pp.prestige = rs.getInt("prestige");
                    list.add(pp);
                }
            }
        }
        return list;
    }

    public static long getPlayerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM players";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    private static Player mapPlayer(ResultSet rs) throws SQLException {
        Player p = new Player();
        p.uuid = rs.getString("uuid");
        p.nickname = rs.getString("nickname");
        p.firstJoin = rs.getString("first_join");
        p.lastJoin = rs.getString("last_join");
        p.donateTier = rs.getString("donate_tier");
        p.role = rs.getString("role");
        p.isBanned = rs.getBoolean("is_banned");
        p.banReason = rs.getString("ban_reason");
        return p;
    }

    private static PlayerStats mapStats(ResultSet rs) throws SQLException {
        PlayerStats s = new PlayerStats();
        s.uuid = rs.getString("uuid");
        s.kills = rs.getInt("kills");
        s.deaths = rs.getInt("deaths");
        s.wins = rs.getInt("wins");
        s.losses = rs.getInt("losses");
        s.playtimeSeconds = rs.getLong("playtime_seconds");
        s.shotsFired = rs.getInt("shots_fired");
        s.shotsHit = rs.getInt("shots_hit");
        s.revives = rs.getInt("revives");
        return s;
    }
}

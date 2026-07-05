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
        String sql = "UPDATE player_stats SET "
                + "kills = kills + ?, deaths = deaths + ?, "
                + "assists = assists + ?, "
                + "wins = wins + ?, losses = losses + ?, "
                + "playtime_seconds = playtime_seconds + ?, "
                + "shots_fired = shots_fired + ?, shots_hit = shots_hit + ?, "
                + "revives = revives + ?, "
                + "vehicle_kills = vehicle_kills + ?, "
                + "captures = captures + ?, "
                + "damage_dealt = damage_dealt + ?, "
                + "healing_done = healing_done + ?, "
                + "supplies_delivered = supplies_delivered + ?, "
                + "longest_kill = GREATEST(longest_kill, ?), "
                + "best_kill_streak = GREATEST(best_kill_streak, ?), "
                + "matches_played = matches_played + ?, "
                + "hub_destructions = hub_destructions + ?, "
                + "base_defends = base_defends + ?, "
                + "vehicles_destroyed = vehicles_destroyed + ?, "
                + "air_vehicles_destroyed = air_vehicles_destroyed + ?, "
                + "team_kills = team_kills + ?, "
                + "match_mvp_count = match_mvp_count + ?, "
                + "current_win_streak = CASE WHEN ? > 0 THEN current_win_streak + 1 ELSE 0 END, "
                + "best_win_streak = GREATEST(best_win_streak, CASE WHEN ? > 0 THEN current_win_streak + 1 ELSE 0 END), "
                + "survival_time = survival_time + ?, "
                + "distance_traveled = distance_traveled + ?, "
                + "headshots = headshots + ? "
                + "WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, delta.kills);
            ps.setInt(2, delta.deaths);
            ps.setInt(3, delta.assists);
            ps.setInt(4, delta.wins);
            ps.setInt(5, delta.losses);
            ps.setLong(6, delta.playtimeSeconds);
            ps.setInt(7, delta.shotsFired);
            ps.setInt(8, delta.shotsHit);
            ps.setInt(9, delta.revives);
            ps.setInt(10, delta.vehicleKills);
            ps.setInt(11, delta.captures);
            ps.setDouble(12, delta.damageDealt);
            ps.setDouble(13, delta.healingDone);
            ps.setInt(14, delta.suppliesDelivered);
            ps.setDouble(15, delta.longestKill);
            ps.setInt(16, delta.bestKillStreak);
            ps.setInt(17, delta.matchesPlayed);
            ps.setInt(18, delta.hubDestructions);
            ps.setInt(19, delta.baseDefends);
            ps.setInt(20, delta.vehiclesDestroyed);
            ps.setInt(21, delta.airVehiclesDestroyed);
            ps.setInt(22, delta.teamKills);
            ps.setInt(23, delta.matchMVPCount);
            ps.setInt(24, delta.wins);  // current_win_streak trigger
            ps.setInt(25, delta.wins);  // best_win_streak trigger
            ps.setLong(26, delta.survivalTime);
            ps.setDouble(27, delta.distanceTraveled);
            ps.setInt(28, delta.headshots);
            ps.setString(29, uuid);
            ps.executeUpdate();
        }
    }

    public static List<PlayerProfile> getLeaderboard(String orderBy, int limit, int offset) throws SQLException {
        String column = switch (orderBy) {
            case "kills" -> "ps.kills";
            case "deaths" -> "ps.deaths";
            case "assists" -> "ps.assists";
            case "wins" -> "ps.wins";
            case "winrate" -> "(ps.wins / GREATEST(ps.wins + ps.losses, 1))";
            case "playtime" -> "ps.playtime_seconds";
            case "kd" -> "(ps.kills / GREATEST(ps.deaths, 1))";
            case "vehicle_kills" -> "ps.vehicle_kills";
            case "captures" -> "ps.captures";
            case "damage" -> "ps.damage_dealt";
            case "healing" -> "ps.healing_done";
            case "vehicles_destroyed" -> "ps.vehicles_destroyed";
            case "air_destroyed" -> "ps.air_vehicles_destroyed";
            case "headshots" -> "ps.headshots";
            case "hub_destructions" -> "ps.hub_destructions";
            case "score" -> "(ps.kills * 100 + ps.assists * 25 + ps.vehicle_kills * 150 + ps.captures * 200 + ps.revives * 75 + ps.damage_dealt + ps.healing_done)";
            case "level" -> "px.level";
            case "prestige" -> "px.prestige";
            default -> "ps.kills";
        };
        String sql = "SELECT p.uuid, p.nickname, ps.kills, ps.deaths, ps.assists, ps.wins, ps.losses, " +
                "ps.playtime_seconds, ps.vehicle_kills, ps.captures, ps.damage_dealt, ps.healing_done, " +
                "ps.hub_destructions, ps.base_defends, ps.vehicles_destroyed, ps.air_vehicles_destroyed, " +
                "ps.team_kills, ps.headshots, ps.supplies_delivered, ps.longest_kill, ps.best_kill_streak, " +
                "ps.matches_played, ps.match_mvp_count, ps.current_win_streak, ps.best_win_streak, " +
                "ps.survival_time, ps.distance_traveled, ps.shots_fired, ps.shots_hit, ps.revives, " +
                "pc.coins, px.level, px.prestige, px.xp " +
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
                    pp.stats = mapStats(rs);
                    pp.coins = rs.getLong("coins");
                    pp.level = rs.getInt("level");
                    pp.prestige = rs.getInt("prestige");
                    pp.xp = rs.getLong("xp");
                    list.add(pp);
                }
            }
        }
        return list;
    }

    public static int getPlayerRank(String uuid, String orderBy) throws SQLException {
        String column = switch (orderBy) {
            case "kills" -> "ps.kills";
            case "deaths" -> "ps.deaths";
            case "assists" -> "ps.assists";
            case "wins" -> "ps.wins";
            case "winrate" -> "(ps.wins / GREATEST(ps.wins + ps.losses, 1))";
            case "playtime" -> "ps.playtime_seconds";
            case "kd" -> "(ps.kills / GREATEST(ps.deaths, 1))";
            case "vehicle_kills" -> "ps.vehicle_kills";
            case "captures" -> "ps.captures";
            case "damage" -> "ps.damage_dealt";
            case "healing" -> "ps.healing_done";
            case "vehicles_destroyed" -> "ps.vehicles_destroyed";
            case "air_destroyed" -> "ps.air_vehicles_destroyed";
            case "headshots" -> "ps.headshots";
            case "hub_destructions" -> "ps.hub_destructions";
            case "score" -> "(ps.kills * 100 + ps.assists * 25 + ps.vehicle_kills * 150 + ps.captures * 200 + ps.revives * 75 + ps.damage_dealt + ps.healing_done)";
            case "level" -> "px.level";
            default -> "ps.kills";
        };
        String sql = "SELECT 1 + COUNT(*) AS rank FROM player_stats ps "
                + "JOIN player_xp px ON ps.uuid = px.uuid "
                + "WHERE " + column + " > (SELECT " + column + " FROM player_stats WHERE uuid = ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("rank");
            }
        }
        return -1;
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
        s.assists = rs.getInt("assists");
        s.wins = rs.getInt("wins");
        s.losses = rs.getInt("losses");
        s.playtimeSeconds = rs.getLong("playtime_seconds");
        s.shotsFired = rs.getInt("shots_fired");
        s.shotsHit = rs.getInt("shots_hit");
        s.revives = rs.getInt("revives");
        s.vehicleKills = rs.getInt("vehicle_kills");
        s.captures = rs.getInt("captures");
        s.damageDealt = rs.getDouble("damage_dealt");
        s.healingDone = rs.getDouble("healing_done");
        s.suppliesDelivered = rs.getInt("supplies_delivered");
        s.longestKill = rs.getDouble("longest_kill");
        s.bestKillStreak = rs.getInt("best_kill_streak");
        s.matchesPlayed = rs.getInt("matches_played");
        s.hubDestructions = rs.getInt("hub_destructions");
        s.baseDefends = rs.getInt("base_defends");
        s.vehiclesDestroyed = rs.getInt("vehicles_destroyed");
        s.airVehiclesDestroyed = rs.getInt("air_vehicles_destroyed");
        s.teamKills = rs.getInt("team_kills");
        s.matchMVPCount = rs.getInt("match_mvp_count");
        s.currentWinStreak = rs.getInt("current_win_streak");
        s.bestWinStreak = rs.getInt("best_win_streak");
        s.survivalTime = rs.getLong("survival_time");
        s.distanceTraveled = rs.getDouble("distance_traveled");
        s.headshots = rs.getInt("headshots");
        return s;
    }
}

package com.pwp.core.db;

import com.pwp.core.model.RankDefinition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RankRepository {

    public static List<RankDefinition> getAll() throws SQLException {
        String sql = "SELECT * FROM rank_definitions ORDER BY rank_id";
        List<RankDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public static RankDefinition findById(int rankId) throws SQLException {
        String sql = "SELECT * FROM rank_definitions WHERE rank_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rankId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public static RankDefinition getRankForLevel(int level) throws SQLException {
        String sql = "SELECT * FROM rank_definitions WHERE level_required <= ? ORDER BY level_required DESC LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, level);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public static boolean hasRank(String uuid, int rankId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM player_ranks WHERE uuid = ? AND rank_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setInt(2, rankId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public static void grantRank(String uuid, int rankId) throws SQLException {
        String sql = "INSERT IGNORE INTO player_ranks (uuid, rank_id) VALUES (?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setInt(2, rankId);
            ps.executeUpdate();
        }
    }

    public static List<RankDefinition> getPlayerRanks(String uuid) throws SQLException {
        String sql = "SELECT r.* FROM rank_definitions r "
                + "JOIN player_ranks pr ON r.rank_id = pr.rank_id "
                + "WHERE pr.uuid = ? ORDER BY r.rank_id";
        List<RankDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private static RankDefinition map(ResultSet rs) throws SQLException {
        RankDefinition r = new RankDefinition();
        r.rankId = rs.getInt("rank_id");
        r.rankName = rs.getString("rank_name");
        r.xpRequired = rs.getLong("xp_required");
        r.levelRequired = rs.getInt("level_required");
        r.kitsUnlocked = rs.getString("kits_unlocked");
        r.description = rs.getString("description");
        return r;
    }
}

package com.pwp.core.db;

import com.pwp.core.model.RewardConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class RewardConfigRepository {

    public static Map<String, RewardConfig> getAll() throws SQLException {
        String sql = "SELECT * FROM reward_config";
        Map<String, RewardConfig> map = new HashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                RewardConfig r = new RewardConfig();
                r.action = rs.getString("action");
                r.xpReward = rs.getLong("xp_reward");
                r.coinsReward = rs.getLong("coins_reward");
                r.scoreReward = rs.getInt("score_reward");
                map.put(r.action, r);
            }
        }
        return map;
    }

    public static void update(String action, long xp, long coins, int score) throws SQLException {
        String sql = "INSERT INTO reward_config (action, xp_reward, coins_reward, score_reward) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE xp_reward=VALUES(xp_reward), coins_reward=VALUES(coins_reward), score_reward=VALUES(score_reward)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setLong(2, xp);
            ps.setLong(3, coins);
            ps.setInt(4, score);
            ps.executeUpdate();
        }
    }
}

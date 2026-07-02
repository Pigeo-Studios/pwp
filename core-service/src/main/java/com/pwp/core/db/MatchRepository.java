package com.pwp.core.db;

import com.pwp.core.model.MatchPlayer;
import com.pwp.core.model.MatchResult;

import java.sql.*;

public class MatchRepository {

    public static long saveMatch(MatchResult match) throws SQLException {
        Connection c = DatabaseManager.getConnection();
        c.setAutoCommit(false);
        try {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO match_history (map_name, mode, team_blue_score, team_red_score, " +
                            "winner, duration_seconds, started_at, ended_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, match.mapName);
            ps.setString(2, match.mode);
            ps.setInt(3, match.teamBlueScore);
            ps.setInt(4, match.teamRedScore);
            ps.setString(5, match.winner);
            ps.setInt(6, match.durationSeconds);
            ps.setString(7, match.startedAt);
            ps.setString(8, match.endedAt);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            long matchId = keys.getLong(1);
            keys.close();
            ps.close();

            if (match.players != null) {
                ps = c.prepareStatement(
                        "INSERT INTO match_players (match_id, uuid, team, kills, deaths, score, role) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)");
                for (MatchPlayer mp : match.players) {
                    ps.setLong(1, matchId);
                    ps.setString(2, mp.uuid);
                    ps.setString(3, mp.team);
                    ps.setInt(4, mp.kills);
                    ps.setInt(5, mp.deaths);
                    ps.setInt(6, mp.score);
                    ps.setString(7, mp.role);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            c.commit();
            return matchId;
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
            c.close();
        }
    }
}

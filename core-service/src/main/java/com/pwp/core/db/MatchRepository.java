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
                        "INSERT INTO match_players (match_id, uuid, team, kills, deaths, assists, score, " +
                                "vehicle_kills, captures, revives, shots_fired, shots_hit, " +
                                "damage_dealt, healing_done, supplies_delivered, longest_kill, " +
                                "role, squad_id, was_squad_leader, " +
                                "vehicles_destroyed, air_vehicles_destroyed, headshots) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                for (MatchPlayer mp : match.players) {
                    ps.setLong(1, matchId);
                    ps.setString(2, mp.uuid);
                    ps.setString(3, mp.team);
                    ps.setInt(4, mp.kills);
                    ps.setInt(5, mp.deaths);
                    ps.setInt(6, mp.assists);
                    ps.setInt(7, mp.score);
                    ps.setInt(8, mp.vehicleKills);
                    ps.setInt(9, mp.captures);
                    ps.setInt(10, mp.revives);
                    ps.setInt(11, mp.shotsFired);
                    ps.setInt(12, mp.shotsHit);
                    ps.setDouble(13, mp.damageDealt);
                    ps.setDouble(14, mp.healingDone);
                    ps.setInt(15, mp.suppliesDelivered);
                    ps.setDouble(16, mp.longestKill);
                    ps.setString(17, mp.role != null ? mp.role : "");
                    ps.setInt(18, mp.squadId);
                    ps.setBoolean(19, mp.wasSquadLeader);
                    ps.setInt(20, mp.vehiclesDestroyed);
                    ps.setInt(21, mp.airVehiclesDestroyed);
                    ps.setInt(22, mp.headshots);
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

    public static MatchResult getLastMatch() throws SQLException {
        Connection c = DatabaseManager.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT map_name, mode, team_blue_score, team_red_score, " +
                    "winner, duration_seconds, started_at, ended_at " +
                    "FROM match_history ORDER BY ended_at DESC LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            MatchResult m = new MatchResult();
            m.mapName = rs.getString("map_name");
            m.mode = rs.getString("mode");
            m.teamBlueScore = rs.getInt("team_blue_score");
            m.teamRedScore = rs.getInt("team_red_score");
            m.winner = rs.getString("winner");
            m.durationSeconds = rs.getInt("duration_seconds");
            m.startedAt = rs.getString("started_at");
            m.endedAt = rs.getString("ended_at");
            rs.close();
            ps.close();
            return m;
        } finally {
            c.close();
        }
    }
}

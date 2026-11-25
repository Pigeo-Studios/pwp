package com.pwp.core.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Дивизии (составы) фракций — таблица {@code faction_divisions} (этап 6 ROADMAP; создаётся при старте core и
 * миграцией {@code sql/migration_add_faction_divisions.sql}). {@code vehicles} — JSON-массив записей техники,
 * формат проверяет контроллер.
 */
public class DivisionRepository {

    public static class Row {
        public String faction;
        public String divisionId;
        public String displayName;
        public String modes;
        public String mapSizes;
        public String vehicles;
        public long updatedAt;
    }

    public static List<Row> byFaction(String faction) throws SQLException {
        String sql = "SELECT * FROM faction_divisions WHERE faction = ? ORDER BY division_id";
        List<Row> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public static void save(Row r) throws SQLException {
        String sql = "INSERT INTO faction_divisions (faction, division_id, display_name, modes, map_sizes, vehicles, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), modes=VALUES(modes), "
                + "map_sizes=VALUES(map_sizes), vehicles=VALUES(vehicles), updated_at=VALUES(updated_at)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.faction);
            ps.setString(2, r.divisionId);
            ps.setString(3, r.displayName);
            ps.setString(4, r.modes);
            ps.setString(5, r.mapSizes);
            ps.setString(6, r.vehicles);
            ps.setLong(7, r.updatedAt);
            ps.executeUpdate();
        }
    }

    public static boolean delete(String faction, String divisionId) throws SQLException {
        String sql = "DELETE FROM faction_divisions WHERE faction = ? AND division_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            ps.setString(2, divisionId);
            return ps.executeUpdate() > 0;
        }
    }

    private static Row map(ResultSet rs) throws SQLException {
        Row r = new Row();
        r.faction = rs.getString("faction");
        r.divisionId = rs.getString("division_id");
        r.displayName = rs.getString("display_name");
        r.modes = rs.getString("modes");
        r.mapSizes = rs.getString("map_sizes");
        r.vehicles = rs.getString("vehicles");
        r.updatedAt = rs.getLong("updated_at");
        return r;
    }
}

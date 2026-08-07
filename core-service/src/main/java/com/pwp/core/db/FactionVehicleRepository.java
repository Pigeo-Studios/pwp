package com.pwp.core.db;

import com.pwp.core.model.FactionVehicleDefinition;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FactionVehicleRepository {

    public static List<FactionVehicleDefinition> getByFaction(String faction) throws SQLException {
        String sql = "SELECT * FROM faction_vehicles WHERE faction = ? ORDER BY vehicle_name";
        List<FactionVehicleDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapVehicle(rs));
            }
        }
        return list;
    }

    public static FactionVehicleDefinition get(String faction, String vehicleName) throws SQLException {
        String sql = "SELECT * FROM faction_vehicles WHERE faction = ? AND vehicle_name = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            ps.setString(2, vehicleName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapVehicle(rs);
            }
        }
        return null;
    }

    public static void save(FactionVehicleDefinition v) throws SQLException {
        String sql = "INSERT INTO faction_vehicles (faction, vehicle_name, display_name, vehicle_id, yaw, respawn_time, initial_time, category, inventory) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), vehicle_id=VALUES(vehicle_id), "
                + "yaw=VALUES(yaw), respawn_time=VALUES(respawn_time), initial_time=VALUES(initial_time), "
                + "category=VALUES(category), inventory=VALUES(inventory)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.faction);
            ps.setString(2, v.vehicleName);
            ps.setString(3, v.displayName);
            ps.setString(4, v.vehicleId);
            ps.setFloat(5, v.yaw);
            ps.setInt(6, v.respawnTime);
            ps.setInt(7, v.initialTime);
            ps.setString(8, v.category);
            ps.setString(9, v.inventory);
            ps.executeUpdate();
        }
    }

    public static void delete(String faction, String vehicleName) throws SQLException {
        String sql = "DELETE FROM faction_vehicles WHERE faction = ? AND vehicle_name = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            ps.setString(2, vehicleName);
            ps.executeUpdate();
        }
    }

    private static FactionVehicleDefinition mapVehicle(ResultSet rs) throws SQLException {
        FactionVehicleDefinition v = new FactionVehicleDefinition();
        v.faction = rs.getString("faction");
        v.vehicleName = rs.getString("vehicle_name");
        v.displayName = rs.getString("display_name");
        v.vehicleId = rs.getString("vehicle_id");
        v.yaw = rs.getFloat("yaw");
        v.respawnTime = rs.getInt("respawn_time");
        v.initialTime = rs.getInt("initial_time");
        v.category = rs.getString("category");
        v.inventory = rs.getString("inventory");
        return v;
    }
}

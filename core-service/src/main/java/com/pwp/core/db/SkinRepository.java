package com.pwp.core.db;

import com.pwp.core.model.SkinDefinition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SkinRepository {

    public static List<SkinDefinition> getAllEnabled() throws SQLException {
        String sql = "SELECT * FROM skin_definitions WHERE enabled = TRUE ORDER BY rarity, skin_id";
        List<SkinDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public static List<SkinDefinition> getBySlotType(String slotType) throws SQLException {
        String sql = "SELECT * FROM skin_definitions WHERE enabled = TRUE AND slot_type = ? ORDER BY rarity";
        List<SkinDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, slotType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public static List<SkinDefinition> getByWeaponTag(String weaponTag) throws SQLException {
        String sql = "SELECT * FROM skin_definitions WHERE enabled = TRUE AND (weapon_tag = ? OR weapon_tag = 'any') ORDER BY rarity";
        List<SkinDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, weaponTag);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public static SkinDefinition findById(String skinId) throws SQLException {
        String sql = "SELECT * FROM skin_definitions WHERE skin_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, skinId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public static void delete(String skinId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM skin_definitions WHERE skin_id = ?")) {
            ps.setString(1, skinId);
            ps.executeUpdate();
        }
    }

    public static void save(SkinDefinition s) throws SQLException {
        String sql = "INSERT INTO skin_definitions (skin_id, name, description, slot_type, weapon_tag, rarity, model_path, image_url, enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), "
                + "slot_type=VALUES(slot_type), weapon_tag=VALUES(weapon_tag), rarity=VALUES(rarity), "
                + "model_path=VALUES(model_path), image_url=VALUES(image_url), enabled=VALUES(enabled)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.skinId);
            ps.setString(2, s.name);
            ps.setString(3, s.description);
            ps.setString(4, s.slotType);
            ps.setString(5, s.weaponTag);
            ps.setString(6, s.rarity);
            ps.setString(7, s.modelPath);
            ps.setString(8, s.imageUrl);
            ps.setBoolean(9, s.enabled);
            ps.executeUpdate();
        }
    }

    private static SkinDefinition map(ResultSet rs) throws SQLException {
        SkinDefinition s = new SkinDefinition();
        s.skinId = rs.getString("skin_id");
        s.name = rs.getString("name");
        s.description = rs.getString("description");
        s.slotType = rs.getString("slot_type");
        s.weaponTag = rs.getString("weapon_tag");
        s.rarity = rs.getString("rarity");
        s.modelPath = rs.getString("model_path");
        s.imageUrl = rs.getString("image_url");
        s.enabled = rs.getBoolean("enabled");
        s.createdAt = rs.getString("created_at");
        return s;
    }
}

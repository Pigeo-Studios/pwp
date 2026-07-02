package com.pwp.core.db;

import com.pwp.core.model.CosmeticItem;
import com.pwp.core.model.EquipmentSlot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CosmeticsRepository {

    public static List<CosmeticItem> getInventory(String uuid) throws SQLException {
        String sql = "SELECT c.*, e.item_uuid IS NOT NULL AS equipped " +
                "FROM player_cosmetics c " +
                "LEFT JOIN player_equipment e ON c.player_uuid = e.uuid AND c.item_uuid = e.item_uuid " +
                "WHERE c.player_uuid = ? ORDER BY c.obtained_at DESC";
        List<CosmeticItem> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CosmeticItem item = new CosmeticItem();
                    item.itemUuid = rs.getString("item_uuid");
                    item.playerUuid = rs.getString("player_uuid");
                    item.skinId = rs.getString("skin_id");
                    item.slotType = rs.getString("slot_type");
                    item.rarity = rs.getString("rarity");
                    item.obtainedAt = rs.getString("obtained_at");
                    item.source = rs.getString("source");
                    item.tradeable = rs.getBoolean("tradeable");
                    item.deletable = rs.getBoolean("deletable");
                    item.equipped = rs.getBoolean("equipped");
                    list.add(item);
                }
            }
        }
        return list;
    }

    public static CosmeticItem grantItem(String uuid, String skinId, String slotType,
                                          String rarity, String source) throws SQLException {
        String itemUuid = UUID.randomUUID().toString();
        String sql = "INSERT INTO player_cosmetics (item_uuid, player_uuid, skin_id, slot_type, rarity, source) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, itemUuid);
            ps.setString(2, uuid);
            ps.setString(3, skinId);
            ps.setString(4, slotType);
            ps.setString(5, rarity);
            ps.setString(6, source);
            ps.executeUpdate();
        }
        CosmeticItem item = new CosmeticItem();
        item.itemUuid = itemUuid;
        item.playerUuid = uuid;
        item.skinId = skinId;
        item.slotType = slotType;
        item.rarity = rarity;
        item.source = source;
        return item;
    }

    public static boolean equipItem(String uuid, String itemUuid, String slotType, String role) throws SQLException {
        String sql = "REPLACE INTO player_equipment (uuid, slot_type, role, item_uuid) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, slotType);
            ps.setString(3, role);
            ps.setString(4, itemUuid);
            return ps.executeUpdate() > 0;
        }
    }

    public static List<EquipmentSlot> getEquipment(String uuid) throws SQLException {
        String sql = "SELECT e.*, c.skin_id, c.slot_type AS item_slot, c.rarity " +
                "FROM player_equipment e " +
                "JOIN player_cosmetics c ON e.item_uuid = c.item_uuid " +
                "WHERE e.uuid = ?";
        List<EquipmentSlot> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EquipmentSlot slot = new EquipmentSlot();
                    slot.uuid = rs.getString("uuid");
                    slot.slotType = rs.getString("slot_type");
                    slot.role = rs.getString("role");
                    slot.itemUuid = rs.getString("item_uuid");
                    slot.item = new CosmeticItem();
                    slot.item.skinId = rs.getString("skin_id");
                    slot.item.slotType = rs.getString("item_slot");
                    slot.item.rarity = rs.getString("rarity");
                    list.add(slot);
                }
            }
        }
        return list;
    }
}

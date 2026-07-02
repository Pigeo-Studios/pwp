package com.pwp.core.db;

import com.pwp.core.model.ShopItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShopRepository {

    public static List<ShopItem> getAllEnabled() throws SQLException {
        String sql = "SELECT * FROM shop_items WHERE enabled = TRUE ORDER BY rarity, price_coins";
        List<ShopItem> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapShopItem(rs));
            }
        }
        return list;
    }

    public static ShopItem findBySkinId(String skinId) throws SQLException {
        String sql = "SELECT * FROM shop_items WHERE skin_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, skinId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapShopItem(rs);
            }
        }
        return null;
    }

    private static ShopItem mapShopItem(ResultSet rs) throws SQLException {
        ShopItem item = new ShopItem();
        item.skinId = rs.getString("skin_id");
        item.name = rs.getString("name");
        item.description = rs.getString("description");
        item.slotType = rs.getString("slot_type");
        item.rarity = rs.getString("rarity");
        item.priceCoins = rs.getInt("price_coins");
        item.priceReal = rs.getDouble("price_real");
        item.modelPath = rs.getString("model_path");
        item.enabled = rs.getBoolean("enabled");
        return item;
    }
}

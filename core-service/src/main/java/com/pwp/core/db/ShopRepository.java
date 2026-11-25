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

    /** Итог покупки за Coins: {@code error} null — успех. */
    public static final class BuyResult {
        public String error;
        public long balance;
        public String itemUuid;

        static BuyResult fail(String e) {
            BuyResult r = new BuyResult();
            r.error = e;
            return r;
        }
    }

    /**
     * Покупка скина за Coins (игровой магазин, этап 8 ROADMAP) — одной транзакцией: товар включён и продаётся
     * за Coins, у игрока его ещё нет, Coins хватает (списание {@code WHERE coins >= price}), выдача предмета.
     * Любая ошибка — откат, деньги не теряются.
     */
    public static BuyResult buyWithCoins(String uuid, String skinId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                ShopItem item = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM shop_items WHERE skin_id = ? AND enabled = TRUE")) {
                    ps.setString(1, skinId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) item = mapShopItem(rs);
                    }
                }
                if (item == null) return rollback(c, "item not found");
                if (item.priceCoins <= 0) return rollback(c, "not for coins");
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM player_cosmetics WHERE player_uuid = ? AND skin_id = ? LIMIT 1")) {
                    ps.setString(1, uuid);
                    ps.setString(2, skinId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rollback(c, "already owned");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE player_currency SET coins = coins - ?, total_spent = total_spent + ? WHERE uuid = ? AND coins >= ?")) {
                    ps.setLong(1, item.priceCoins);
                    ps.setLong(2, item.priceCoins);
                    ps.setString(3, uuid);
                    ps.setLong(4, item.priceCoins);
                    if (ps.executeUpdate() == 0) return rollback(c, "insufficient funds");
                }
                String itemUuid = java.util.UUID.randomUUID().toString();
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO player_cosmetics (item_uuid, player_uuid, skin_id, slot_type, rarity, source) VALUES (?, ?, ?, ?, ?, 'SHOP')")) {
                    ps.setString(1, itemUuid);
                    ps.setString(2, uuid);
                    ps.setString(3, skinId);
                    ps.setString(4, item.slotType);
                    ps.setString(5, item.rarity);
                    ps.executeUpdate();
                }
                long balance = 0;
                try (PreparedStatement ps = c.prepareStatement("SELECT coins FROM player_currency WHERE uuid = ?")) {
                    ps.setString(1, uuid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) balance = rs.getLong(1);
                    }
                }
                c.commit();
                BuyResult r = new BuyResult();
                r.balance = balance;
                r.itemUuid = itemUuid;
                return r;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        }
    }

    private static BuyResult rollback(Connection c, String error) throws SQLException {
        c.rollback();
        return BuyResult.fail(error);
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

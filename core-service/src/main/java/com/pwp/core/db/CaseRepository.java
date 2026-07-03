package com.pwp.core.db;

import com.pwp.core.model.CaseDefinition;
import com.pwp.core.model.CaseLootEntry;
import com.pwp.core.model.SkinDefinition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CaseRepository {

    public static List<CaseDefinition> getAllEnabled() throws SQLException {
        String sql = "SELECT * FROM case_definitions WHERE enabled = TRUE ORDER BY name";
        List<CaseDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                CaseDefinition cd = map(rs);
                cd.loot = getLoot(cd.caseId);
                list.add(cd);
            }
        }
        return list;
    }

    public static CaseDefinition findById(String caseId) throws SQLException {
        String sql = "SELECT * FROM case_definitions WHERE case_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CaseDefinition cd = map(rs);
                    cd.loot = getLoot(cd.caseId);
                    return cd;
                }
            }
        }
        return null;
    }

    public static List<CaseLootEntry> getLoot(String caseId) throws SQLException {
        String sql = "SELECT cl.*, sd.* FROM case_loot cl "
                + "JOIN skin_definitions sd ON cl.skin_id = sd.skin_id "
                + "WHERE cl.case_id = ?";
        List<CaseLootEntry> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CaseLootEntry e = new CaseLootEntry();
                    e.caseId = rs.getString("case_id");
                    e.skinId = rs.getString("skin_id");
                    e.weight = rs.getInt("weight");
                    e.isGuaranteed = rs.getBoolean("is_guaranteed");
                    SkinDefinition sd = new SkinDefinition();
                    sd.skinId = e.skinId;
                    sd.name = rs.getString("name");
                    sd.description = rs.getString("description");
                    sd.slotType = rs.getString("slot_type");
                    sd.weaponTag = rs.getString("weapon_tag");
                    sd.rarity = rs.getString("rarity");
                    sd.modelPath = rs.getString("model_path");
                    sd.enabled = rs.getBoolean("enabled");
                    e.skin = sd;
                    list.add(e);
                }
            }
        }
        return list;
    }

    public static void saveCase(CaseDefinition cd) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            String sql = "INSERT INTO case_definitions (case_id, name, description, price_coins, icon_path, enabled) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), "
                    + "price_coins=VALUES(price_coins), icon_path=VALUES(icon_path), enabled=VALUES(enabled)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cd.caseId);
                ps.setString(2, cd.name);
                ps.setString(3, cd.description);
                ps.setInt(4, cd.priceCoins);
                ps.setString(5, cd.iconPath);
                ps.setBoolean(6, cd.enabled);
                ps.executeUpdate();
            }

            if (cd.loot != null) {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM case_loot WHERE case_id = ?")) {
                    del.setString(1, cd.caseId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO case_loot (case_id, skin_id, weight, is_guaranteed) VALUES (?, ?, ?, ?)")) {
                    for (CaseLootEntry e : cd.loot) {
                        ins.setString(1, cd.caseId);
                        ins.setString(2, e.skinId);
                        ins.setInt(3, e.weight);
                        ins.setBoolean(4, e.isGuaranteed);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public static String openCase(String caseId) throws SQLException {
        List<CaseLootEntry> loot = getLoot(caseId);
        if (loot.isEmpty()) return null;

        int totalWeight = 0;
        for (CaseLootEntry e : loot) totalWeight += e.weight;

        int roll = (int) (Math.random() * totalWeight);
        int cumulative = 0;
        for (CaseLootEntry e : loot) {
            cumulative += e.weight;
            if (roll < cumulative) return e.skinId;
        }
        return loot.get(loot.size() - 1).skinId;
    }

    private static CaseDefinition map(ResultSet rs) throws SQLException {
        CaseDefinition cd = new CaseDefinition();
        cd.caseId = rs.getString("case_id");
        cd.name = rs.getString("name");
        cd.description = rs.getString("description");
        cd.priceCoins = rs.getInt("price_coins");
        cd.iconPath = rs.getString("icon_path");
        cd.enabled = rs.getBoolean("enabled");
        cd.createdAt = rs.getString("created_at");
        return cd;
    }
}

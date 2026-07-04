package com.pwp.core.db;

import com.pwp.core.model.KitDefinition;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KitRepository {

    public static List<KitDefinition> getByFaction(String faction) throws SQLException {
        String sql = "SELECT * FROM kit_definitions WHERE faction = ? ORDER BY kit_name";
        List<KitDefinition> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapKit(rs));
            }
        }
        return list;
    }

    public static KitDefinition get(String faction, String kitName) throws SQLException {
        String sql = "SELECT * FROM kit_definitions WHERE faction = ? AND kit_name = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            ps.setString(2, kitName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapKit(rs);
            }
        }
        return null;
    }

    public static void save(KitDefinition kit) throws SQLException {
        String sql = "INSERT INTO kit_definitions (faction, kit_name, leader_only, max_per_team, max_per_squad, min_squad_players, items, slot_skins) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE leader_only=VALUES(leader_only), max_per_team=VALUES(max_per_team), "
                + "max_per_squad=VALUES(max_per_squad), min_squad_players=VALUES(min_squad_players), "
                + "items=VALUES(items), slot_skins=VALUES(slot_skins)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, kit.faction);
            ps.setString(2, kit.kitName);
            ps.setBoolean(3, kit.leaderOnly);
            ps.setInt(4, kit.maxPerTeam);
            ps.setInt(5, kit.maxPerSquad);
            ps.setInt(6, kit.minSquadPlayers);
            ps.setString(7, kit.items);
            ps.setString(8, kit.slotSkins);
            ps.executeUpdate();
        }
    }

    public static void delete(String faction, String kitName) throws SQLException {
        String sql = "DELETE FROM kit_definitions WHERE faction = ? AND kit_name = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, faction);
            ps.setString(2, kitName);
            ps.executeUpdate();
        }
    }

    public static List<String> getFactions() throws SQLException {
        String sql = "SELECT DISTINCT faction FROM kit_definitions ORDER BY faction";
        List<String> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("faction"));
        }
        return list;
    }

    private static KitDefinition mapKit(ResultSet rs) throws SQLException {
        KitDefinition k = new KitDefinition();
        k.faction = rs.getString("faction");
        k.kitName = rs.getString("kit_name");
        k.leaderOnly = rs.getBoolean("leader_only");
        k.maxPerTeam = rs.getInt("max_per_team");
        k.maxPerSquad = rs.getInt("max_per_squad");
        k.minSquadPlayers = rs.getInt("min_squad_players");
        k.items = rs.getString("items");
        k.slotSkins = rs.getString("slot_skins");
        return k;
    }
}

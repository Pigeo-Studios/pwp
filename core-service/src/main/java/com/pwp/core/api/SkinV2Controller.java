package com.pwp.core.api;

import com.pwp.core.db.CosmeticsRepository;
import com.pwp.core.db.SkinRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.CosmeticItem;
import com.pwp.core.model.SkinDefinition;
import io.javalin.Javalin;

import java.sql.*;
import java.util.*;

public class SkinV2Controller {

    public SkinV2Controller(Javalin app) {
        // Get weapon tags list (reads from skin_definitions)
        app.get("/api/v2/skins/weapon-tags", ctx -> {
            Set<String> tags = new HashSet<>();
            for (SkinDefinition def : SkinRepository.getAllEnabled()) {
                if (def.weaponTag != null && !def.weaponTag.isEmpty() && !def.weaponTag.equals("any")) {
                    tags.add(def.weaponTag);
                }
            }
            ctx.json(ApiResponse.ok(tags));
        });

        // Get owned skins for player
        app.get("/api/v2/skins/owned/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            try (Connection c = com.pwp.core.db.DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT skin_id FROM player_owned_skins WHERE uuid = ?")) {
                ps.setString(1, uuid);
                ResultSet rs = ps.executeQuery();
                List<String> list = new ArrayList<>();
                while (rs.next()) list.add(rs.getString("skin_id"));
                ctx.json(ApiResponse.ok(list));
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });

        // Grant skin to player
        app.post("/api/v2/skins/grant", ctx -> {
            GrantReq req = ctx.bodyAsClass(GrantReq.class);
            try (Connection c = com.pwp.core.db.DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO player_owned_skins (uuid, skin_id) VALUES (?, ?)")) {
                ps.setString(1, req.uuid);
                ps.setString(2, req.skinId);
                ps.executeUpdate();
                ctx.json(ApiResponse.ok("granted"));
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });

        // Get equipped skins
        app.get("/api/v2/skins/equipped/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            try (Connection c = com.pwp.core.db.DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT e.weapon_tag, e.skin_id, s.name, s.model_path " +
                    "FROM player_equipped_skins e " +
                    "LEFT JOIN skin_definitions s ON e.skin_id = s.skin_id " +
                    "WHERE e.uuid = ?")) {
                ps.setString(1, uuid);
                ResultSet rs = ps.executeQuery();
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("weaponTag", rs.getString("weapon_tag"));
                    m.put("skinId", rs.getString("skin_id"));
                    m.put("name", rs.getString("name"));
                    m.put("modelPath", rs.getString("model_path"));
                    list.add(m);
                }
                ctx.json(ApiResponse.ok(list));
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });

        // Bind skin to weapon
        app.post("/api/v2/skins/bind", ctx -> {
            BindReq req = ctx.bodyAsClass(BindReq.class);
            try (Connection c = com.pwp.core.db.DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO player_equipped_skins (uuid, weapon_tag, skin_id) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE skin_id = VALUES(skin_id)")) {
                ps.setString(1, req.uuid);
                ps.setString(2, req.weaponTag);
                ps.setString(3, req.skinId);
                ps.executeUpdate();
                ctx.json(ApiResponse.ok("bound"));
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });

        // Unbind skin from weapon
        app.post("/api/v2/skins/unbind", ctx -> {
            BindReq req = ctx.bodyAsClass(BindReq.class);
            try (Connection c = com.pwp.core.db.DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM player_equipped_skins WHERE uuid = ? AND weapon_tag = ?")) {
                ps.setString(1, req.uuid);
                ps.setString(2, req.weaponTag);
                ps.executeUpdate();
                ctx.json(ApiResponse.ok("unbound"));
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });
    }

    private static class GrantReq { public String uuid; public String skinId; }
    private static class BindReq { public String uuid; public String weaponTag; public String skinId; }
}
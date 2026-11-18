package com.pwp.core.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Версии раскладки карты (таблица {@code map_layout_versions}, миграция
 * {@code sql/migration_add_map_layout_versions.sql}). Версии только добавляются: откат = новая копия.
 */
public class MapLayoutRepository {

    public static class Version {
        public long id;
        public String mapName;
        public String authorUuid;
        public String authorName;
        public long createdAt;
        public String comment;
        /** null в списке версий (без тела). */
        public String json;
    }

    /** Последние {@code limit} версий карты, новые первыми, без JSON. */
    public static List<Version> list(String mapName, int limit) throws SQLException {
        String sql = "SELECT id, map_name, author_uuid, author_name, created_at, comment FROM map_layout_versions "
                + "WHERE map_name = ? ORDER BY id DESC LIMIT ?";
        List<Version> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, mapName);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs, false));
            }
        }
        return out;
    }

    public static Version latest(String mapName) throws SQLException {
        String sql = "SELECT * FROM map_layout_versions WHERE map_name = ? ORDER BY id DESC LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, mapName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, true) : null;
            }
        }
    }

    public static Version byId(long id) throws SQLException {
        String sql = "SELECT * FROM map_layout_versions WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, true) : null;
            }
        }
    }

    /** Новая версия; возвращает её с id. */
    public static Version insert(String mapName, String json, String authorUuid, String authorName, String comment)
            throws SQLException {
        String sql = "INSERT INTO map_layout_versions (map_name, json, author_uuid, author_name, created_at, comment) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        long now = System.currentTimeMillis();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, mapName);
            ps.setString(2, json);
            ps.setString(3, authorUuid);
            ps.setString(4, authorName);
            ps.setLong(5, now);
            ps.setString(6, comment == null ? "" : comment);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                Version v = new Version();
                v.id = keys.next() ? keys.getLong(1) : 0L;
                v.mapName = mapName;
                v.json = json;
                v.authorUuid = authorUuid;
                v.authorName = authorName;
                v.createdAt = now;
                v.comment = comment == null ? "" : comment;
                return v;
            }
        }
    }

    private static Version map(ResultSet rs, boolean withJson) throws SQLException {
        Version v = new Version();
        v.id = rs.getLong("id");
        v.mapName = rs.getString("map_name");
        v.authorUuid = rs.getString("author_uuid");
        v.authorName = rs.getString("author_name");
        v.createdAt = rs.getLong("created_at");
        v.comment = rs.getString("comment");
        if (withJson) v.json = rs.getString("json");
        return v;
    }
}

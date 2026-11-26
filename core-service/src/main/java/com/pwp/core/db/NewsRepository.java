package com.pwp.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Таблица {@code news} (этап 17b): лента новостей для сайта, лаунчера и игры. */
public final class NewsRepository {
    public record News(String title, String body, String tag, String imageUrl, boolean pinned, boolean published) {}

    private NewsRepository() {}

    /** Новые первыми, закреплённые — выше всех; {@code all} — и скрытые (для модерации). */
    public static List<Map<String, Object>> list(int limit, boolean all) throws SQLException {
        String sql = "SELECT id, title, body, tag, image_url, pinned, published, author, created_at, updated_at FROM news "
                + (all ? "" : "WHERE published = 1 ")
                + "ORDER BY pinned DESC, created_at DESC LIMIT ?";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("title", rs.getString("title"));
                    m.put("body", rs.getString("body"));
                    m.put("tag", rs.getString("tag"));
                    m.put("imageUrl", rs.getString("image_url"));
                    m.put("pinned", rs.getBoolean("pinned"));
                    if (all) {
                        m.put("published", rs.getBoolean("published"));
                        m.put("author", rs.getString("author"));
                    }
                    m.put("createdAt", rs.getLong("created_at"));
                    m.put("updatedAt", rs.getLong("updated_at"));
                    out.add(m);
                }
            }
        }
        return out;
    }

    public static long create(News n, String author) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = "INSERT INTO news (title, body, tag, image_url, pinned, published, author, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.title());
            ps.setString(2, n.body());
            ps.setString(3, n.tag());
            ps.setString(4, n.imageUrl());
            ps.setBoolean(5, n.pinned());
            ps.setBoolean(6, n.published());
            ps.setString(7, author.length() > 32 ? author.substring(0, 32) : author);
            ps.setLong(8, now);
            ps.setLong(9, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }

    public static boolean update(long id, News n) throws SQLException {
        String sql = "UPDATE news SET title = ?, body = ?, tag = ?, image_url = ?, pinned = ?, published = ?, "
                + "updated_at = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.title());
            ps.setString(2, n.body());
            ps.setString(3, n.tag());
            ps.setString(4, n.imageUrl());
            ps.setBoolean(5, n.pinned());
            ps.setBoolean(6, n.published());
            ps.setLong(7, System.currentTimeMillis());
            ps.setLong(8, id);
            return ps.executeUpdate() > 0;
        }
    }
}

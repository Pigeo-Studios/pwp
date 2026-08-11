package com.pwp.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/** Резолв цели наказания по нику или UUID (общий для мутов и кика). */
public class PlayerId {

    /** Возвращает uuid по target (если это UUID — как есть, иначе по nickname). */
    public static String resolve(Connection c, String target) throws java.sql.SQLException {
        if (target == null) return null;
        String t = target.trim();
        if (t.isEmpty()) return null;
        try {
            return UUID.fromString(t).toString();
        } catch (IllegalArgumentException ignored) {
            // не UUID — ищем по нику
        }
        String sql = "SELECT uuid FROM players WHERE nickname = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("uuid");
            }
        }
        return null;
    }
}

package com.pwp.core.db;

import com.pwp.core.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;

public class PlayerRepository {

    public static Player findByUuid(String uuid) throws SQLException {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlayer(rs);
            }
        }
        return null;
    }

    public static Player findByAccountId(long accountId) throws SQLException {
        String sql = "SELECT * FROM players WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlayer(rs);
            }
        }
        return null;
    }

    public static Player createOrUpdate(String uuid, String nickname) throws SQLException {
        String sql = "INSERT INTO players (uuid, nickname) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), last_join = CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, nickname);
            ps.executeUpdate();
        }
        ensureRowExists(uuid, "player_stats");
        ensureRowExists(uuid, "player_currency");
        ensureRowExists(uuid, "player_xp");
        Player p = findByUuid(uuid);
        if (p != null) {
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT id FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) p.accountId = rs.getLong("id");
                }
            }
        }
        return p;
    }

    private static void ensureRowExists(String uuid, String table) throws SQLException {
        String sql = "INSERT IGNORE INTO " + table + " (uuid) VALUES (?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }

    public static PlayerStats getStats(String uuid) throws SQLException {
        String sql = "SELECT * FROM player_stats WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapStats(rs);
            }
        }
        return null;
    }

    public static void ensurePlayerExists(String uuid) throws SQLException {
        String sql = "INSERT IGNORE INTO players (uuid, nickname) VALUES (?, 'unknown')";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
        ensureRowExists(uuid, "player_stats");
        ensureRowExists(uuid, "player_currency");
        ensureRowExists(uuid, "player_xp");
    }

    public static void updateStats(String uuid, PlayerStats delta) throws SQLException {
        ensurePlayerExists(uuid);
        String sql = "UPDATE player_stats SET "
                + "kills = kills + ?, deaths = deaths + ?, "
                + "wins = wins + ?, losses = losses + ?, "
                + "playtime_seconds = playtime_seconds + ?, "
                + "shots_fired = shots_fired + ?, shots_hit = shots_hit + ?, "
                + "revives = revives + ?, "
                + "vehicle_kills = vehicle_kills + ?, "
                + "captures = captures + ?, "
                + "damage_dealt = damage_dealt + ?, "
                + "healing_done = healing_done + ?, "
                + "supplies_delivered = supplies_delivered + ?, "
                + "longest_kill = GREATEST(longest_kill, ?), "
                + "best_kill_streak = GREATEST(best_kill_streak, ?), "
                + "matches_played = matches_played + ?, "
                + "vehicles_destroyed = vehicles_destroyed + ?, "
                + "air_vehicles_destroyed = air_vehicles_destroyed + ?, "
                + "team_kills = team_kills + ?, "
                + "current_win_streak = CASE WHEN ? > 0 THEN current_win_streak + 1 ELSE 0 END, "
                + "best_win_streak = GREATEST(best_win_streak, CASE WHEN ? > 0 THEN current_win_streak + 1 ELSE 0 END), "
                + "survival_time = survival_time + ?, "
                + "headshots = headshots + ? "
                + "WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, delta.kills);
            ps.setInt(2, delta.deaths);
            ps.setInt(3, delta.wins);
            ps.setInt(4, delta.losses);
            ps.setLong(5, delta.playtimeSeconds);
            ps.setInt(6, delta.shotsFired);
            ps.setInt(7, delta.shotsHit);
            ps.setInt(8, delta.revives);
            ps.setInt(9, delta.vehicleKills);
            ps.setInt(10, delta.captures);
            ps.setDouble(11, delta.damageDealt);
            ps.setDouble(12, delta.healingDone);
            ps.setInt(13, delta.suppliesDelivered);
            ps.setDouble(14, delta.longestKill);
            ps.setInt(15, delta.bestKillStreak);
            ps.setInt(16, delta.matchesPlayed);
            ps.setInt(17, delta.vehiclesDestroyed);
            ps.setInt(18, delta.airVehiclesDestroyed);
            ps.setInt(19, delta.teamKills);
            ps.setInt(20, delta.wins);  // current_win_streak trigger
            ps.setInt(21, delta.wins);  // best_win_streak trigger
            ps.setLong(22, delta.survivalTime);
            ps.setInt(23, delta.headshots);
            ps.setString(24, uuid);
            ps.executeUpdate();
        }
    }

    private static final Map<String, String> ORDER_BY_COLUMNS = Map.ofEntries(
        Map.entry("kills", "ps.kills"),
        Map.entry("deaths", "ps.deaths"),
        Map.entry("wins", "ps.wins"),
        Map.entry("winrate", "(ps.wins / GREATEST(ps.wins + ps.losses, 1))"),
        Map.entry("playtime", "ps.playtime_seconds"),
        Map.entry("kd", "(ps.kills / GREATEST(ps.deaths, 1))"),
        Map.entry("vehicle_kills", "ps.vehicle_kills"),
        Map.entry("captures", "ps.captures"),
        Map.entry("damage", "ps.damage_dealt"),
        Map.entry("healing", "ps.healing_done"),
        Map.entry("vehicles_destroyed", "ps.vehicles_destroyed"),
        Map.entry("air_destroyed", "ps.air_vehicles_destroyed"),
        Map.entry("headshots", "ps.headshots"),
        Map.entry("score", "(ps.kills * 100 + ps.vehicle_kills * 150 + ps.captures * 200 + ps.revives * 75 + ps.healing_done)"),
        Map.entry("level", "px.level"),
        Map.entry("prestige", "px.prestige")
    );

    public static List<PlayerProfile> getLeaderboard(String orderBy, int limit, int offset) throws SQLException {
        String column = ORDER_BY_COLUMNS.getOrDefault(orderBy, "ps.kills");
        String sql = "SELECT p.uuid, p.nickname, ps.kills, ps.deaths, ps.wins, ps.losses, " +
                "ps.playtime_seconds, ps.vehicle_kills, ps.captures, ps.damage_dealt, ps.healing_done, " +
                "ps.vehicles_destroyed, ps.air_vehicles_destroyed, " +
                "ps.team_kills, ps.headshots, ps.supplies_delivered, ps.longest_kill, ps.best_kill_streak, " +
                "ps.matches_played, ps.current_win_streak, ps.best_win_streak, " +
                "ps.survival_time, ps.shots_fired, ps.shots_hit, ps.revives, " +
                "pc.coins, px.level, px.prestige, px.xp " +
                "FROM players p " +
                "JOIN player_stats ps ON p.uuid = ps.uuid " +
                "JOIN player_currency pc ON p.uuid = pc.uuid " +
                "JOIN player_xp px ON p.uuid = px.uuid " +
                "ORDER BY " + column + " DESC LIMIT ? OFFSET ?";
        List<PlayerProfile> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerProfile pp = new PlayerProfile();
                    pp.player = new Player();
                    pp.player.uuid = rs.getString("uuid");
                    pp.player.nickname = rs.getString("nickname");
                    pp.stats = mapStats(rs);
                    pp.coins = rs.getLong("coins");
                    pp.level = rs.getInt("level");
                    pp.prestige = rs.getInt("prestige");
                    pp.xp = rs.getLong("xp");
                    list.add(pp);
                }
            }
        }
        return list;
    }

    // в”Ђв”Ђ Account methods в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static Player findByTelegramId(long telegramId) throws SQLException {
        String sql = "SELECT * FROM players WHERE telegram_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, telegramId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlayer(rs);
            }
        }
        return null;
    }

    public static Player findByLogin(String login) throws SQLException {
        String sql = "SELECT * FROM players WHERE login = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlayer(rs);
            }
        }
        return null;
    }

    public static Player findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM players WHERE email = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlayer(rs);
            }
        }
        return null;
    }

    public static Player findByNickname(String nickname) throws SQLException {
        String sql = "SELECT * FROM players WHERE nickname = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlayer(rs);
            }
        }
        return null;
    }

    public static Player register(String uuid, String nickname, String login, String email, String passwordHash, long telegramId) throws SQLException {
        // Check if this nickname already exists вЂ” link account to existing player
        Player existing = findByNickname(nickname);
        if (existing != null) {
            String sql = "UPDATE players SET login = ?, email = ?, password_hash = ?, telegram_id = ? WHERE uuid = ?";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, login);
                ps.setString(2, email);
                ps.setString(3, passwordHash);
                ps.setLong(4, telegramId);
                ps.setString(5, existing.uuid);
                ps.executeUpdate();
            }
            return findByUuid(existing.uuid);
        }
        String sql = "INSERT INTO players (uuid, nickname, login, email, password_hash, telegram_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, nickname);
            ps.setString(3, login);
            ps.setString(4, email);
            ps.setString(5, passwordHash);
            ps.setLong(6, telegramId);
            ps.executeUpdate();
        }
        ensureRowExists(uuid, "player_stats");
        ensureRowExists(uuid, "player_currency");
        ensureRowExists(uuid, "player_xp");
        return findByUuid(uuid);
    }

    public static void updatePassword(String uuid, String newHash) throws SQLException {
        String sql = "UPDATE players SET password_hash = ? WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, uuid);
            ps.executeUpdate();
        }
    }

    public static void updateLastLogin(String uuid, String ip) throws SQLException {
        String sql = "UPDATE players SET last_join = CURRENT_TIMESTAMP, last_ip = ? WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, uuid);
            ps.executeUpdate();
        }
    }

    public static void acceptPrivacy(String uuid) throws SQLException {
        String sql = "UPDATE players SET privacy_policy_accepted = TRUE WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }

    public static void toggle2fa(String uuid, boolean enabled) throws SQLException {
        String sql = "UPDATE players SET launcher_2fa_enabled = ? WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setString(2, uuid);
            ps.executeUpdate();
        }
    }

    public static void setBan(String uuid, boolean banned, String reason) throws SQLException {
        // Вечный бан/разбан: banned_until всегда сбрасываем, чтобы не оставался «просроченный» статус
        setBan(uuid, banned, reason, null);
    }

    /** Бан/разбан с указанием срока действия (banned_until = null для вечного). */
    public static void setBan(String uuid, boolean banned, String reason, java.sql.Timestamp bannedUntil) throws SQLException {
        String sql = "UPDATE players SET is_banned = ?, ban_reason = ?, banned_until = ? WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, banned);
            ps.setString(2, reason);
            ps.setTimestamp(3, bannedUntil);
            ps.setString(4, uuid);
            ps.executeUpdate();
        }
    }

    /** Истёк ли временный бан (banned_until в прошлом). */
    public static boolean isBanExpired(Player pl) {
        if (pl == null || pl.bannedUntil == null || pl.bannedUntil.isEmpty()) return false;
        try {
            java.sql.Timestamp until = java.sql.Timestamp.valueOf(pl.bannedUntil.replace('T', ' '));
            return until.before(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    public static void setRole(String uuid, String role) throws SQLException {
        String sql = "UPDATE players SET role = ? WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, uuid);
            ps.executeUpdate();
        }
    }

    // в”Ђв”Ђ Trusted IPs в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static boolean isIpTrusted(String playerUuid, String ip) throws SQLException {
        String sql = "SELECT 1 FROM trusted_ips WHERE player_uuid = ? AND ip = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, ip);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void trustIp(String playerUuid, String ip) throws SQLException {
        String sql = "INSERT IGNORE INTO trusted_ips (player_uuid, ip) VALUES (?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, ip);
            ps.executeUpdate();
        }
    }

    // в”Ђв”Ђ Sessions (v1) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static void createSession(String playerUuid, String token, String ip) throws SQLException {
        String sql = "INSERT INTO sessions (player_uuid, token, ip, expires_at) VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, token);
            ps.setString(3, ip);
            ps.executeUpdate();
        }
    }

    public static String findSessionPlayer(String token) throws SQLException {
        String sql = "SELECT s.player_uuid, p.is_banned FROM sessions s JOIN players p ON s.player_uuid = p.uuid WHERE (s.access_token = ? OR s.token = ?) AND s.expires_at > CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && !rs.getBoolean("is_banned")) {
                    return rs.getString("player_uuid");
                }
            }
        }
        return null;
    }

    public static void deleteSession(String token) throws SQLException {
        String sql = "DELETE FROM sessions WHERE token = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    // в”Ђв”Ђ Sessions (v2: access/refresh tokens) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    private static final SecureRandom SESSION_RNG = new SecureRandom();

    public static String generateSessionKey() {
        byte[] key = new byte[32];
        SESSION_RNG.nextBytes(key);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key);
    }

    public static String generateTokenPart() {
        byte[] bytes = new byte[48];
        SESSION_RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static void createSessionV2(String playerUuid, String accessToken, String refreshToken, String sessionKey, String ip) throws SQLException {
        createSessionV2(playerUuid, accessToken, refreshToken, sessionKey, ip, null);
    }

    public static void createSessionV2(String playerUuid, String accessToken, String refreshToken, String sessionKey, String ip, String hmacSecret) throws SQLException {
        String sql = "INSERT INTO sessions (player_uuid, token, access_token, refresh_token, session_key, hmac_secret, ip, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, accessToken); // token column for backward compat
            ps.setString(3, accessToken);
            ps.setString(4, refreshToken);
            ps.setString(5, sessionKey);
            ps.setString(6, hmacSecret);
            ps.setString(7, ip);
            ps.executeUpdate();
        }
    }

    public static String findHmacSecretByAccessToken(String accessToken) throws SQLException {
        String sql = "SELECT hmac_secret FROM sessions WHERE (access_token = ? OR token = ?) AND expires_at > CURRENT_TIMESTAMP AND hmac_secret IS NOT NULL";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accessToken);
            ps.setString(2, accessToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("hmac_secret");
            }
        }
        // Токен ротирован при refresh, чей ответ клиент мог потерять (обрыв сети) —
        // в grace-окне отдаём прежний секрет, чтобы клиент со старыми токенами не умер.
        return lookupGracePrevHmac(accessToken);
    }

    public static String findHmacSecretByRefreshToken(String refreshToken) throws SQLException {
        String sql = "SELECT hmac_secret FROM sessions WHERE refresh_token = ? AND expires_at > CURRENT_TIMESTAMP AND hmac_secret IS NOT NULL";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, refreshToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("hmac_secret");
            }
        }
        return lookupGracePrevHmac(refreshToken);
    }

    public static String findUuidByAccessToken(String accessToken) throws SQLException {
        String sql = "SELECT s.player_uuid, p.is_banned FROM sessions s JOIN players p ON s.player_uuid = p.uuid "
                    + "WHERE (s.access_token = ? OR s.token = ?) AND s.expires_at > CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accessToken);
            ps.setString(2, accessToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && !rs.getBoolean("is_banned")) {
                    return rs.getString("player_uuid");
                }
            }
        }
        return null;
    }

    public static String findSessionKeyByAccessToken(String accessToken) throws SQLException {
        String sql = "SELECT session_key FROM sessions WHERE (access_token = ? OR token = ?) AND expires_at > CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accessToken);
            ps.setString(2, accessToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("session_key");
            }
        }
        return null;
    }

    public static boolean refreshSession(String refreshToken, String newAccessToken, String newRefreshToken, String newSessionKey, String newHmacSecret) throws SQLException {
        String sql = "UPDATE sessions SET access_token = ?, token = ?, refresh_token = ?, session_key = ?, hmac_secret = ?, "
                    + "expires_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY) "
                    + "WHERE refresh_token = ? AND expires_at > CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newAccessToken);
            ps.setString(2, newAccessToken);
            ps.setString(3, newRefreshToken);
            ps.setString(4, newSessionKey);
            ps.setString(5, newHmacSecret);
            ps.setString(6, refreshToken);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Grace-окно ротации токенов (против гонки «ответ refresh потерялся при обрыве сети») ──
    // Сервер ротирует refresh_token/access_token/hmac_secret на каждый refresh. Если ответ
    // до лаунчера не дошёл (обрыв сети ровно в этот момент), лаунчер остаётся со старыми
    // токенами и старым секретом: middleware не находил сессию → 401 → «Сессия истекла».
    // В течение GRACE_TTL_MS после ротации старые токены продолжают «работать»: middleware
    // принимает их HMAC (prevHmacSecret), а refresh-хэндлер отдаёт ТЕКУЩИЕ токены (ресинк).
    // In-memory кэш (без миграции БД); потеря при рестарте core-service приемлема (окно 2 мин).

    public static final class TokenGrace {
        public final String prevHmacSecret;
        public final String currentAccessToken;
        public final String currentRefreshToken;
        public final String currentSessionKey;
        public final String currentHmacSecret;
        public final long rotatedAtMillis;

        TokenGrace(String prevHmacSecret, String currentAccessToken, String currentRefreshToken,
                   String currentSessionKey, String currentHmacSecret, long rotatedAtMillis) {
            this.prevHmacSecret = prevHmacSecret;
            this.currentAccessToken = currentAccessToken;
            this.currentRefreshToken = currentRefreshToken;
            this.currentSessionKey = currentSessionKey;
            this.currentHmacSecret = currentHmacSecret;
            this.rotatedAtMillis = rotatedAtMillis;
        }
    }

    private static final Map<String, TokenGrace> TOKEN_GRACE = new ConcurrentHashMap<>();
    private static final long GRACE_TTL_MS = 2 * 60 * 1000L;

    /** Запомнить старые токены после ротации (старые ключи → текущие значения). */
    public static void storeTokenGrace(String oldAccessToken, String oldRefreshToken,
                                       String newAccessToken, String newRefreshToken,
                                       String newSessionKey, String newHmacSecret,
                                       String prevHmacSecret) {
        if (oldAccessToken == null || oldRefreshToken == null) return;
        long now = System.currentTimeMillis();
        TokenGrace g = new TokenGrace(prevHmacSecret, newAccessToken, newRefreshToken,
                newSessionKey, newHmacSecret, now);
        TOKEN_GRACE.put(oldAccessToken, g);
        TOKEN_GRACE.put(oldRefreshToken, g);
        if (TOKEN_GRACE.size() > 2000) cleanupTokenGrace(now);
    }

    private static void cleanupTokenGrace(long now) {
        TOKEN_GRACE.entrySet().removeIf(e -> now - e.getValue().rotatedAtMillis > GRACE_TTL_MS);
    }

    /** Секрет, которым клиент со старыми токенами подпишет запрос (или null вне окна). */
    private static String lookupGracePrevHmac(String token) {
        if (token == null) return null;
        TokenGrace g = TOKEN_GRACE.get(token);
        if (g == null) return null;
        long now = System.currentTimeMillis();
        if (now - g.rotatedAtMillis > GRACE_TTL_MS) {
            TOKEN_GRACE.remove(token);
            return null;
        }
        return g.prevHmacSecret;
    }

    /** Grace по refresh-токену для ресинка (потребляет запись — ресинк случился один раз). */
    public static TokenGrace consumeRefreshTokenGrace(String refreshToken) {
        if (refreshToken == null) return null;
        TokenGrace g = TOKEN_GRACE.get(refreshToken);
        if (g == null) return null;
        if (System.currentTimeMillis() - g.rotatedAtMillis > GRACE_TTL_MS) {
            TOKEN_GRACE.remove(refreshToken);
            return null;
        }
        TOKEN_GRACE.remove(refreshToken);
        return g;
    }

    /** Текущие (access, refresh, hmac) строки сессии по refresh-токену — ДО ротации. */
    public static String[] findSessionTokensByRefresh(String refreshToken) throws SQLException {
        String sql = "SELECT access_token, refresh_token, hmac_secret FROM sessions "
                    + "WHERE refresh_token = ? AND expires_at > CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, refreshToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new String[]{rs.getString(1), rs.getString(2), rs.getString(3)};
            }
        }
    }

    public static boolean updateHeartbeat(String accessToken) throws SQLException {
        String sql = "UPDATE sessions SET last_heartbeat = CURRENT_TIMESTAMP, "
                    + "expires_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY) WHERE access_token = ? AND expires_at > CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accessToken);
            return ps.executeUpdate() > 0;
        }
    }

    public static void deleteSessionsByUuid(String playerUuid) throws SQLException {
        String sql = "DELETE FROM sessions WHERE player_uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.executeUpdate();
        }
    }

    // в”Ђв”Ђ 2FA Codes в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private static final SecureRandom RNG = new SecureRandom();

    public static String create2faCode(String playerUuid, String ip) throws SQLException {
        int code = RNG.nextInt(1000000);
        String codeStr = String.format("%06d", code);
        String sql = "INSERT INTO twofa_codes (player_uuid, code, ip, expires_at) VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, codeStr);
            ps.setString(3, ip);
            ps.executeUpdate();
        }
        return codeStr;
    }

    public static boolean validate2faCode(String playerUuid, String code) throws SQLException {
        String sql = "SELECT id FROM twofa_codes WHERE player_uuid = ? AND code = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP ORDER BY id DESC LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    try (PreparedStatement up = c.prepareStatement("UPDATE twofa_codes SET used = TRUE WHERE id = ?")) {
                        up.setInt(1, id);
                        up.executeUpdate();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // в”Ђв”Ђ Password Resets в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static int createResetRequest(String uuid) throws SQLException {
        String sql = "INSERT INTO password_resets (player_uuid) VALUES (?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public static List<PasswordResetEntry> findPendingResets() throws SQLException {
        List<PasswordResetEntry> list = new ArrayList<>();
        String sql = "SELECT pr.*, p.nickname, p.login FROM password_resets pr JOIN players p ON pr.player_uuid = p.uuid WHERE pr.status = 'pending' ORDER BY pr.id DESC";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                PasswordResetEntry e = new PasswordResetEntry();
                e.id = rs.getInt("id");
                e.playerUuid = rs.getString("player_uuid");
                e.login = rs.getString("login");
                e.status = rs.getString("status");
                e.createdAt = rs.getString("created_at");
                list.add(e);
            }
        }
        return list;
    }

    public static void resolveReset(int id, String adminUuid, String status) throws SQLException {
        String sql = "UPDATE password_resets SET admin_uuid = ?, status = ?, resolved_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adminUuid);
            ps.setString(2, status);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /** Последняя ОДОБРЕННАЯ заявка на сброс игрока (для set-password-after-reset). */
    public static PasswordResetEntry findApprovedReset(String uuid) throws SQLException {
        String sql = "SELECT pr.*, p.nickname, p.login FROM password_resets pr JOIN players p ON pr.player_uuid = p.uuid "
                + "WHERE pr.player_uuid = ? AND pr.status = 'approved' ORDER BY pr.id DESC LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PasswordResetEntry e = new PasswordResetEntry();
                    e.id = rs.getInt("id");
                    e.playerUuid = rs.getString("player_uuid");
                    e.login = rs.getString("login");
                    e.status = rs.getString("status");
                    e.createdAt = rs.getString("created_at");
                    return e;
                }
            }
        }
        return null;
    }

    /** Заявка использована — новый пароль задан, повторный сброс не нужен. */
    public static void markResetUsed(int id) throws SQLException {
        String sql = "UPDATE password_resets SET status = 'used' WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // в”Ђв”Ђ Logs в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    // в”Ђв”Ђ Auth Tokens (v2) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    private static final SecureRandom TOKEN_RNG = new SecureRandom();

    public static String createAuthToken(long accountId) throws SQLException {
        byte[] bytes = new byte[32];
        TOKEN_RNG.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // Revoke any existing tokens for this account
        revokeAllForAccount(accountId);
        // Insert new token
        String sql = "INSERT INTO auth_tokens (account_id, token, expires_at) VALUES (?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setString(2, token);
            ps.executeUpdate();
        }
        return token;
    }

    public static Player findByAuthToken(String token) throws SQLException {
        String sql = "SELECT p.*, a.id as aid, a.expires_at, a.revoked FROM auth_tokens a "
                + "JOIN players p ON a.account_id = p.id "
                + "WHERE a.token = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean revoked = rs.getBoolean("revoked");
                    boolean expired = rs.getTimestamp("expires_at").before(new java.util.Date());
                    if (revoked || expired) return null;
                    // Update last_used
                    try (PreparedStatement up = c.prepareStatement(
                            "UPDATE auth_tokens SET last_used_at = CURRENT_TIMESTAMP WHERE token = ?")) {
                        up.setString(1, token);
                        up.executeUpdate();
                    }
                    return mapPlayer(rs);
                }
            }
        }
        return null;
    }

    public static void revokeAllForAccount(long accountId) throws SQLException {
        String sql = "UPDATE auth_tokens SET revoked = TRUE WHERE account_id = ? AND revoked = FALSE";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.executeUpdate();
        }
    }

    public static void revokeToken(String token) throws SQLException {
        String sql = "UPDATE auth_tokens SET revoked = TRUE WHERE token = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    public static void log(String playerUuid, String action, String ip, String details) throws SQLException {
        String sql = "INSERT INTO player_logs (player_uuid, action, ip, details) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, action);
            ps.setString(3, ip);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }

    public static List<LogEntry> getLogs(int limit, int offset) throws SQLException {
        List<LogEntry> list = new ArrayList<>();
        String sql = "SELECT pl.*, p.login FROM player_logs pl LEFT JOIN players p ON pl.player_uuid = p.uuid ORDER BY pl.id DESC LIMIT ? OFFSET ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.min(limit, 100));
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogEntry e = new LogEntry();
                    e.id = rs.getInt("id");
                    e.playerUuid = rs.getString("player_uuid");
                    e.login = rs.getString("login");
                    e.action = rs.getString("action");
                    e.ip = rs.getString("ip");
                    e.details = rs.getString("details");
                    e.createdAt = rs.getString("created_at");
                    list.add(e);
                }
            }
        }
        return list;
    }

    // в”Ђв”Ђ Broadcast в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static List<Long> getTelegramIdsForBroadcast() throws SQLException {
        List<Long> ids = new ArrayList<>();
        String sql = "SELECT telegram_id FROM players WHERE is_banned = FALSE AND telegram_id IS NOT NULL";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getLong(1));
        }
        return ids;
    }

    public static int countAccounts() throws SQLException {
        String sql = "SELECT COUNT(*) FROM players WHERE login IS NOT NULL";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // в”Ђв”Ђ Find any player by query в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    public static Player findAny(String query) throws SQLException {
        Player p = findByLogin(query);
        if (p != null) return p;
        p = findByUuid(query);
        if (p != null) return p;
        p = findByNickname(query);
        if (p != null) return p;
        p = findByEmail(query);
        if (p != null) return p;
        try { return findByTelegramId(Long.parseLong(query)); } catch (Exception ignored) {}
        return null;
    }

    public static class PasswordResetEntry {
        public int id;
        public String playerUuid;
        public String login;
        public String status;
        public String createdAt;
    }

    public static class LogEntry {
        public int id;
        public String playerUuid;
        public String login;
        public String action;
        public String ip;
        public String details;
        public String createdAt;
    }

    public static int getPlayerRank(String uuid, String orderBy) throws SQLException {
        String column = ORDER_BY_COLUMNS.getOrDefault(orderBy, "ps.kills");
        String sql = "SELECT 1 + COUNT(*) AS rank FROM player_stats ps "
                + "JOIN player_xp px ON ps.uuid = px.uuid "
                + "WHERE " + column + " > (SELECT " + column + " FROM player_stats WHERE uuid = ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("rank");
            }
        }
        return -1;
    }

    public static long getPlayerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM players";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public static int getTodayPlayerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM players WHERE DATE(last_join) = CURDATE()";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static long getTotalPlaytimeHours() throws SQLException {
        String sql = "SELECT COALESCE(SUM(playtime_seconds), 0) / 3600 FROM player_stats";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public static long getTotalKills() throws SQLException {
        String sql = "SELECT COALESCE(SUM(kills), 0) FROM player_stats";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public static long getTotalVehiclesDestroyed() throws SQLException {
        String sql = "SELECT COALESCE(SUM(vehicles_destroyed), 0) FROM player_stats";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public static long getTotalCaptures() throws SQLException {
        String sql = "SELECT COALESCE(SUM(captures), 0) FROM player_stats";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    private static Player mapPlayer(ResultSet rs) throws SQLException {
        Player p = new Player();
        p.accountId = rs.getLong("id");
        p.uuid = rs.getString("uuid");
        p.nickname = rs.getString("nickname");
        p.login = rs.getString("login");
        p.email = rs.getString("email");
        p.passwordHash = rs.getString("password_hash");
        p.telegramId = (Long) rs.getObject("telegram_id");
        p.hwid = rs.getString("hwid");
        p.privacyPolicyAccepted = rs.getBoolean("privacy_policy_accepted");
        p.launcher2faEnabled = rs.getBoolean("launcher_2fa_enabled");
        p.firstJoin = rs.getString("first_join");
        p.lastJoin = rs.getString("last_join");
        p.lastIp = rs.getString("last_ip");
        p.donateTier = rs.getString("donate_tier");
        p.role = rs.getString("role");
        p.isBanned = rs.getBoolean("is_banned");
        p.banReason = rs.getString("ban_reason");
        p.bannedUntil = rs.getString("banned_until");
        return p;
    }

    private static PlayerStats mapStats(ResultSet rs) throws SQLException {
        PlayerStats s = new PlayerStats();
        s.uuid = rs.getString("uuid");
        s.kills = rs.getInt("kills");
        s.deaths = rs.getInt("deaths");
        s.wins = rs.getInt("wins");
        s.losses = rs.getInt("losses");
        s.playtimeSeconds = rs.getLong("playtime_seconds");
        s.shotsFired = rs.getInt("shots_fired");
        s.shotsHit = rs.getInt("shots_hit");
        s.revives = rs.getInt("revives");
        s.vehicleKills = rs.getInt("vehicle_kills");
        s.captures = rs.getInt("captures");
        s.damageDealt = rs.getDouble("damage_dealt");
        s.healingDone = rs.getDouble("healing_done");
        s.suppliesDelivered = rs.getInt("supplies_delivered");
        s.longestKill = rs.getDouble("longest_kill");
        s.bestKillStreak = rs.getInt("best_kill_streak");
        s.matchesPlayed = rs.getInt("matches_played");
        s.vehiclesDestroyed = rs.getInt("vehicles_destroyed");
        s.airVehiclesDestroyed = rs.getInt("air_vehicles_destroyed");
        s.teamKills = rs.getInt("team_kills");
        s.currentWinStreak = rs.getInt("current_win_streak");
        s.bestWinStreak = rs.getInt("best_win_streak");
        s.survivalTime = rs.getLong("survival_time");
        s.headshots = rs.getInt("headshots");
        return s;
    }
    /** Снимает HWID- и IP-баны игрока (полный разбан устройства). */
    public static void clearBans(String playerUuid) throws SQLException {
        String hwid = null, lastIp = null;
        String sql = "SELECT hwid, last_ip FROM players WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    hwid = rs.getString("hwid");
                    lastIp = rs.getString("last_ip");
                }
            }
        }
        if (hwid != null && !hwid.isEmpty()) {
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM hwid_bans WHERE hwid = ?")) {
                ps.setString(1, hwid);
                ps.executeUpdate();
            }
        }
        if (lastIp != null && !lastIp.isEmpty()) {
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM ip_blocks WHERE ip = ?")) {
                ps.setString(1, lastIp);
                ps.executeUpdate();
            }
        }
    }
}
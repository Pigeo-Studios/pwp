package com.pwp.core.db;

import com.pwp.core.model.Account;
import com.pwp.core.model.LoginSession;
import com.pwp.core.model.TwoFaCode;
import com.pwp.core.model.PasswordReset;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountRepository {

    // ── Accounts ──────────────────────────────────────────

    public static Account findByTelegramId(long telegramId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE telegram_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, telegramId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        }
        return null;
    }

    public static Account findByLogin(String login) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE login = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        }
        return null;
    }

    public static Account findByUuid(String uuid) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE uuid = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        }
        return null;
    }

    public static Account findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE email = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        }
        return null;
    }

    public static Account findById(int id) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        }
        return null;
    }

    public static Account create(String login, String email, String passwordHash, long telegramId) throws SQLException {
        String uuid = UUID.randomUUID().toString();
        String sql = "INSERT INTO accounts (uuid, login, email, password_hash, telegram_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, uuid);
            ps.setString(2, login);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            ps.setLong(5, telegramId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Account acc = new Account();
                    acc.id = keys.getInt(1);
                    acc.uuid = uuid;
                    acc.login = login;
                    acc.email = email;
                    acc.telegramId = telegramId;
                    acc.role = "user";
                    acc.privacyPolicyAccepted = false;
                    acc.launcher2faEnabled = false;
                    acc.isBanned = false;
                    return acc;
                }
            }
        }
        return null;
    }

    public static void updatePassword(int accountId, String newHash) throws SQLException {
        String sql = "UPDATE accounts SET password_hash = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    public static void updateLastLogin(int accountId, String ip) throws SQLException {
        String sql = "UPDATE accounts SET last_login = CURRENT_TIMESTAMP, last_ip = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    public static void acceptPrivacy(int accountId) throws SQLException {
        String sql = "UPDATE accounts SET privacy_policy_accepted = TRUE, privacy_policy_accepted_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
        }
    }

    public static void toggle2fa(int accountId, boolean enabled) throws SQLException {
        String sql = "UPDATE accounts SET launcher_2fa_enabled = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    public static void setRole(int accountId, String role) throws SQLException {
        String sql = "UPDATE accounts SET role = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    public static void setBan(int accountId, boolean banned, String reason) throws SQLException {
        String sql = "UPDATE accounts SET is_banned = ?, ban_reason = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, banned);
            ps.setString(2, reason);
            ps.setInt(3, accountId);
            ps.executeUpdate();
        }
    }

    public static List<Account> findAll(int limit, int offset) throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY id DESC LIMIT ? OFFSET ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAccount(rs));
            }
        }
        return list;
    }

    public static int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM accounts";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static void deleteByTelegramId(long telegramId) throws SQLException {
        String sql = "DELETE FROM accounts WHERE telegram_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, telegramId);
            ps.executeUpdate();
        }
    }

    // ── Trusted IPs ───────────────────────────────────────

    public static boolean isIpTrusted(int accountId, String ip) throws SQLException {
        String sql = "SELECT 1 FROM trusted_ips WHERE account_id = ? AND ip = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, ip);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void trustIp(int accountId, String ip) throws SQLException {
        String sql = "INSERT IGNORE INTO trusted_ips (account_id, ip) VALUES (?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, ip);
            ps.executeUpdate();
        }
    }

    // ── Sessions ──────────────────────────────────────────

    public static LoginSession createSession(int accountId, String token, String ip, int hoursValid) throws SQLException {
        String sql = "INSERT INTO sessions (account_id, token, ip, expires_at) VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL ? HOUR))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, accountId);
            ps.setString(2, token);
            ps.setString(3, ip);
            ps.setInt(4, hoursValid);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    LoginSession s = new LoginSession();
                    s.id = keys.getInt(1);
                    s.token = token;
                    s.ip = ip;
                    return s;
                }
            }
        }
        return null;
    }

    public static LoginSession findSession(String token) throws SQLException {
        String sql = "SELECT s.*, a.is_banned FROM sessions s JOIN accounts a ON s.account_id = a.id WHERE s.token = ? AND s.expires_at > CURRENT_TIMESTAMP";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean banned = rs.getBoolean("is_banned");
                    if (banned) return null;
                    LoginSession s = new LoginSession();
                    s.id = rs.getInt("id");
                    s.accountId = rs.getInt("account_id");
                    s.token = rs.getString("token");
                    s.ip = rs.getString("ip");
                    s.createdAt = rs.getString("created_at");
                    s.expiresAt = rs.getString("expires_at");
                    return s;
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

    // ── 2FA Codes ─────────────────────────────────────────

    public static TwoFaCode create2faCode(int accountId, String code, String ip) throws SQLException {
        String sql = "INSERT INTO twofa_codes (account_id, code, ip, expires_at) VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, accountId);
            ps.setString(2, code);
            ps.setString(3, ip);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    TwoFaCode t = new TwoFaCode();
                    t.id = keys.getInt(1);
                    t.code = code;
                    return t;
                }
            }
        }
        return null;
    }

    public static TwoFaCode findValidCode(int accountId, String code) throws SQLException {
        String sql = "SELECT * FROM twofa_codes WHERE account_id = ? AND code = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP ORDER BY id DESC LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TwoFaCode t = new TwoFaCode();
                    t.id = rs.getInt("id");
                    t.accountId = rs.getInt("account_id");
                    t.code = rs.getString("code");
                    t.ip = rs.getString("ip");
                    return t;
                }
            }
        }
        return null;
    }

    public static void markCodeUsed(int codeId) throws SQLException {
        String sql = "UPDATE twofa_codes SET used = TRUE WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, codeId);
            ps.executeUpdate();
        }
    }

    // ── Password Resets ───────────────────────────────────

    public static PasswordReset createResetRequest(int accountId) throws SQLException {
        String sql = "INSERT INTO password_resets (account_id) VALUES (?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    PasswordReset pr = new PasswordReset();
                    pr.id = keys.getInt(1);
                    pr.accountId = accountId;
                    pr.status = "pending";
                    return pr;
                }
            }
        }
        return null;
    }

    public static PasswordReset findResetById(int id) throws SQLException {
        String sql = "SELECT * FROM password_resets WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapReset(rs);
            }
        }
        return null;
    }

    public static List<PasswordReset> findPendingResets() throws SQLException {
        List<PasswordReset> list = new ArrayList<>();
        String sql = "SELECT * FROM password_resets WHERE status = 'pending' ORDER BY id DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapReset(rs));
            }
        }
        return list;
    }

    public static void resolveReset(int id, int adminId, String status) throws SQLException {
        String sql = "UPDATE password_resets SET admin_id = ?, status = ?, resolved_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, status);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    // ── Logs ──────────────────────────────────────────────

    public static void log(int accountId, String action, String ip, String details) throws SQLException {
        String sql = "INSERT INTO account_logs (account_id, action, ip, details) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (accountId > 0) ps.setInt(1, accountId);
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, ip);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }

    public static List<AccountLogEntry> getLogs(int limit, int offset) throws SQLException {
        List<AccountLogEntry> list = new ArrayList<>();
        String sql = "SELECT al.*, a.login FROM account_logs al LEFT JOIN accounts a ON al.account_id = a.id ORDER BY al.id DESC LIMIT ? OFFSET ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AccountLogEntry e = new AccountLogEntry();
                    e.id = rs.getInt("id");
                    e.accountId = rs.getInt("account_id");
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

    // ── Broadcast ─────────────────────────────────────────

    public static int getAccountCountForBroadcast() throws SQLException {
        String sql = "SELECT COUNT(*) FROM accounts WHERE is_banned = FALSE AND telegram_id IS NOT NULL";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static List<Long> getTelegramIdsForBroadcast() throws SQLException {
        List<Long> ids = new ArrayList<>();
        String sql = "SELECT telegram_id FROM accounts WHERE is_banned = FALSE AND telegram_id IS NOT NULL";
        try (Connection c = DatabaseManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getLong(1));
        }
        return ids;
    }

    public static void logBroadcast(int adminId, String message, int count) throws SQLException {
        String sql = "INSERT INTO broadcast_log (admin_id, message, recipient_count) VALUES (?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, message);
            ps.setInt(3, count);
            ps.executeUpdate();
        }
    }

    // ── Query helper ──────────────────────────────────────

    public static Account findAny(String query) throws SQLException {
        // Try by login, uuid, telegram_id, email
        Account a = findByLogin(query);
        if (a != null) return a;
        a = findByUuid(query);
        if (a != null) return a;
        a = findByEmail(query);
        if (a != null) return a;
        try {
            long tid = Long.parseLong(query);
            return findByTelegramId(tid);
        } catch (NumberFormatException ignored) {}
        return null;
    }

    // ── Mappers ───────────────────────────────────────────

    private static Account mapAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.id = rs.getInt("id");
        a.uuid = rs.getString("uuid");
        a.login = rs.getString("login");
        a.email = rs.getString("email");
        a.passwordHash = rs.getString("password_hash");
        a.telegramId = (Long) rs.getObject("telegram_id");
        a.role = rs.getString("role");
        a.hwid = rs.getString("hwid");
        a.privacyPolicyAccepted = rs.getBoolean("privacy_policy_accepted");
        a.privacyPolicyAcceptedAt = rs.getString("privacy_policy_accepted_at");
        a.launcher2faEnabled = rs.getBoolean("launcher_2fa_enabled");
        a.isBanned = rs.getBoolean("is_banned");
        a.banReason = rs.getString("ban_reason");
        a.lastIp = rs.getString("last_ip");
        a.lastLogin = rs.getString("last_login");
        a.registeredAt = rs.getString("registered_at");
        return a;
    }

    private static PasswordReset mapReset(ResultSet rs) throws SQLException {
        PasswordReset pr = new PasswordReset();
        pr.id = rs.getInt("id");
        pr.accountId = rs.getInt("account_id");
        pr.adminId = (Integer) rs.getObject("admin_id");
        pr.status = rs.getString("status");
        pr.createdAt = rs.getString("created_at");
        pr.resolvedAt = rs.getString("resolved_at");
        return pr;
    }

    public static class AccountLogEntry {
        public int id;
        public int accountId;
        public String login;
        public String action;
        public String ip;
        public String details;
        public String createdAt;
    }
}

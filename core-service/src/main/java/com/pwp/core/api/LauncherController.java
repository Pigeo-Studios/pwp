package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.db.PunishmentRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.Player;
import io.javalin.Javalin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class LauncherController {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LauncherController.class);

    public LauncherController(Javalin app) {

        // ── Check launcher version ──────────────────────────
        app.get("/api/v1/launcher/version", ctx -> {
            String currentVersion = ctx.queryParam("current");
            String plat = ctx.queryParam("platform");
            if (plat == null) plat = "windows";

            var map = new HashMap<String, Object>();
            map.put("latest_version", CoreApplication.projectVersion);
            map.put("update_available", false);
            map.put("download_url", "");
            map.put("sha256", "");
            map.put("mandatory", false);

            String sql = "SELECT version, url, sha256, mandatory FROM launcher_versions ORDER BY id DESC LIMIT 1";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String latest = rs.getString("version");
                    boolean mandatory = rs.getBoolean("mandatory");
                    map.put("latest_version", latest);
                    if (currentVersion == null || !currentVersion.equals(latest)) {
                        map.put("update_available", true);
                        map.put("download_url", rs.getString("url"));
                        map.put("sha256", rs.getString("sha256"));
                        map.put("mandatory", mandatory);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("version check failed"));
                return;
            }
            ctx.json(ApiResponse.ok(map));
        });

        // ── Get file manifest ────────────────────────────────
        app.get("/api/v1/launcher/manifest", ctx -> {
            String category = ctx.queryParam("category"); // null = all
            String version = ctx.queryParam("version");

            List<Map<String, Object>> files = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                "SELECT file_path, file_size, sha256, category, mod_name, mod_description, mod_optional " +
                "FROM file_manifests WHERE 1=1");
            if (category != null && !category.isEmpty()) {
                sql.append(" AND category = ?");
            }
            if (version != null && !version.isEmpty()) {
                sql.append(" AND version = ?");
            }

            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql.toString())) {
                int idx = 1;
                if (category != null && !category.isEmpty()) {
                    ps.setString(idx++, category);
                }
                if (version != null && !version.isEmpty()) {
                    ps.setString(idx, version);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> f = new HashMap<>();
                        f.put("path", rs.getString("file_path"));
                        f.put("size", rs.getLong("file_size"));
                        f.put("sha256", rs.getString("sha256"));
                        f.put("category", rs.getString("category"));
                        f.put("mod_name", rs.getString("mod_name"));
                        f.put("mod_description", rs.getString("mod_description"));
                        f.put("mod_optional", rs.getBoolean("mod_optional"));
                        files.add(f);
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("manifest load failed"));
                return;
            }
            ctx.json(ApiResponse.ok(Map.of("files", files, "total", files.size())));
        });

        // ── Submit HWID (sent once on login) ───────────────
        app.post("/api/v1/launcher/submit-hwid", ctx -> {
            SubmitHwidReq req = ctx.bodyAsClass(SubmitHwidReq.class);
            if ((req.sessionToken == null && req.accessToken == null) || req.hwid == null) {
                ctx.json(ApiResponse.error("session_token or access_token and hwid required"));
                return;
            }
            String playerUuid = req.accessToken != null
                ? PlayerRepository.findUuidByAccessToken(req.accessToken)
                : PlayerRepository.findSessionPlayer(req.sessionToken);
            if (playerUuid == null) {
                ctx.json(ApiResponse.error("invalid session"));
                return;
            }
            updateHwidHistory(playerUuid, req.hwid, req.hwidComponents, req.pcName, ctx.ip(), req.flags);
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE players SET hwid = ? WHERE uuid = ?")) {
                ps.setString(1, req.hwid);
                ps.setString(2, playerUuid);
                ps.executeUpdate();
            } catch (Exception ignored) {}
            ctx.json(ApiResponse.ok("hwid saved"));
        });

        // ── Get auth token (launcher → Minecraft, backward compat) ──
        app.post("/api/v1/launcher/server-token", ctx -> {
            ServerTokenReq req = ctx.bodyAsClass(ServerTokenReq.class);
            if (req.accessToken == null && req.sessionToken == null) {
                ctx.json(ApiResponse.error("access_token required"));
                return;
            }
            String playerUuid = req.accessToken != null
                ? PlayerRepository.findUuidByAccessToken(req.accessToken)
                : PlayerRepository.findSessionPlayer(req.sessionToken);
            if (playerUuid == null) {
                ctx.json(ApiResponse.error("invalid or expired session"));
                return;
            }
            Player pl = PlayerRepository.findByUuid(playerUuid);
            if (pl == null || pl.isBanned) {
                ctx.json(ApiResponse.error("account is banned"));
                return;
            }
            // Create auth token (7 days)
            String authToken = PlayerRepository.createAuthToken(pl.accountId);
            ctx.json(ApiResponse.ok(Map.of(
                "server_token", authToken,
                "accountId", pl.accountId,
                "uuid", playerUuid,
                "username", pl.nickname,
                "role", pl.role,
                "expires_in", 604800
            )));
        });

        // ── Verify auth token (server-to-server) ──────
        app.post("/api/v1/launcher/verify", ctx -> {
            VerifyReq req = ctx.bodyAsClass(VerifyReq.class);
            if (req.token == null) {
                ctx.json(ApiResponse.error("token required"));
                return;
            }
            Player pl = PlayerRepository.findByAuthToken(req.token);
            if (pl == null) {
                ctx.json(ApiResponse.error("INVALID_TOKEN"));
                return;
            }
            if (pl.isBanned) {
                ctx.json(ApiResponse.error("ACCOUNT_BANNED"));
                return;
            }
            ctx.json(ApiResponse.ok(Map.of(
                "valid", true,
                "accountId", pl.accountId,
                "uuid", pl.uuid,
                "nickname", pl.nickname,
                "role", pl.role
            )));
        });

        // ── Upload launcher logs ────────────────────────────
        app.post("/api/v1/launcher/logs", ctx -> {
            LogUploadReq req = ctx.bodyAsClass(LogUploadReq.class);
            if (req.logs == null || req.logs.isEmpty()) {
                ctx.json(ApiResponse.error("logs required"));
                return;
            }
            String uuid = null;
            if (req.sessionToken != null) {
                uuid = PlayerRepository.findSessionPlayer(req.sessionToken);
            }
            String finalUuid = uuid;
            String sql = "INSERT INTO launcher_logs (account_uuid, level, message) VALUES (?, ?, ?)";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (var entry : req.logs) {
                    ps.setString(1, finalUuid);
                    String level = entry.level != null ? entry.level : "INFO";
                    if (level.length() > 8) level = level.substring(0, 8);
                    ps.setString(2, level);
                    String msg = entry.message != null ? entry.message : "";
                    if (msg.length() > 2000) msg = msg.substring(0, 2000);
                    ps.setString(3, msg);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("log upload failed"));
                return;
            }
            ctx.json(ApiResponse.ok("logs received"));
        });

        // ── File download serving ──────────────────────────
        app.get("/launcher/files/{path}", ctx -> {
            String relPath = ctx.pathParam("path");
            if (relPath.isEmpty() || relPath.contains("..")) {
                ctx.status(400).result("Invalid path"); return;
            }
            java.io.File file = new java.io.File(CoreApplication.config.launcher.filesDir, relPath);
            if (!file.exists() || !file.isFile()) {
                ctx.status(404).result("File not found"); return;
            }
            ctx.contentType(guessMime(relPath));
            ctx.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            try (var in = new java.io.FileInputStream(file)) {
                ctx.result(in.readAllBytes());
            }
            log.info("Served: {} ({} bytes)", relPath, file.length());
        });

        // ── Check session by UUID (только UUID, без nickname fallback) ─
        app.post("/api/v1/launcher/check-session", ctx -> {
            CheckSessionReq req = ctx.bodyAsClass(CheckSessionReq.class);
            if (req.uuid == null) {
                ctx.json(ApiResponse.error("uuid required"));
                return;
            }
            boolean valid = false;
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM sessions WHERE player_uuid = ? AND expires_at > CURRENT_TIMESTAMP LIMIT 1")) {
                ps.setString(1, req.uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    valid = rs.next();
                }
            } catch (Exception e) {
                log.warn("check-session error: {}", e.getMessage());
            }
            ctx.json(Map.of("valid", valid));
        });

        // ── Check ban by UUID ─────────────────────────────
        app.post("/api/v1/launcher/check-ban", ctx -> {
            CheckBanReq req = ctx.bodyAsClass(CheckBanReq.class);
            if (req.uuid == null) {
                ctx.json(ApiResponse.error("uuid required"));
                return;
            }
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT is_banned, ban_reason, banned_until FROM players WHERE uuid = ?")) {
                ps.setString(1, req.uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        java.sql.Timestamp until = rs.getTimestamp("banned_until");
                        boolean timedOut = until != null && until.before(new java.util.Date());
                        if (timedOut) {
                            try (PreparedStatement up = c.prepareStatement(
                                "UPDATE players SET is_banned = FALSE, ban_reason = NULL, banned_until = NULL WHERE uuid = ?")) {
                                up.setString(1, req.uuid);
                                up.executeUpdate();
                            }
                            ctx.json(Map.of("banned", false));
                        } else if (rs.getBoolean("is_banned")) {
                            ctx.json(Map.of("banned", true, "reason", rs.getString("ban_reason")));
                        } else {
                            ctx.json(Map.of("banned", false));
                        }
                    } else {
                        ctx.json(Map.of("banned", false));
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });

        // ── HWID ban check ──────────────────────────────────
        app.post("/api/v1/launcher/check-hwid-ban", ctx -> {
            HwidCheckReq req = ctx.bodyAsClass(HwidCheckReq.class);
            if (req.hwid == null) {
                ctx.json(ApiResponse.error("hwid required"));
                return;
            }
            boolean banned = isHwidBanned(req.hwid);
            ctx.json(ApiResponse.ok(Map.of("banned", banned)));
        });

        // ── Ban player ──────────────────────────────────────
        app.post("/api/v1/launcher/ban", ctx -> {
            BanReq req = ctx.bodyAsClass(BanReq.class);
            if (req.target == null || req.reason == null) {
                ctx.json(ApiResponse.error("target and reason required"));
                return;
            }

            try (Connection c = DatabaseManager.getConnection()) {
                // Parse duration
                String durationStr = req.duration;
                String expiresClause = "NULL";
                String displayDuration = "\u043D\u0430\u0432\u0441\u0435\u0433\u0434\u0430";
                Integer durationMinutes = null;
                if (durationStr != null && !durationStr.isEmpty() && !"perm".equals(durationStr) && !"0".equals(durationStr)) {
                    try {
                        durationMinutes = parseDuration(durationStr);
                        expiresClause = "DATE_ADD(NOW(), INTERVAL " + durationMinutes + " MINUTE)";
                        displayDuration = formatDuration(durationMinutes);
                    } catch (Exception ignored) {}
                }

                // Lookup target by UUID or nickname
                String uuid = null;
                String nickname = null;
                String hwid = null;
                String lastIp = null;
                Long telegramId = null;

                String lookupSql = "SELECT uuid, nickname, hwid, last_ip, telegram_id FROM players WHERE uuid = ? OR nickname = ?";
                try (PreparedStatement ps = c.prepareStatement(lookupSql)) {
                    ps.setString(1, req.target);
                    ps.setString(2, req.target);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            uuid = rs.getString("uuid");
                            nickname = rs.getString("nickname");
                            hwid = rs.getString("hwid");
                            lastIp = rs.getString("last_ip");
                            telegramId = rs.getLong("telegram_id");
                        }
                    }
                }

                if (uuid == null) {
                    ctx.json(ApiResponse.error("player not found"));
                    return;
                }

                // Ban account
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE players SET is_banned = TRUE, ban_reason = ?, banned_until = " + expiresClause + " WHERE uuid = ?")) {
                    ps.setString(1, req.reason);
                    ps.setString(2, uuid);
                    ps.executeUpdate();
                }

                // Ban HWID
                if (hwid != null && !hwid.isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT IGNORE INTO hwid_bans (hwid, reason, banned_until, created_at) VALUES (?, ?, " + expiresClause + ", NOW())")) {
                        ps.setString(1, hwid);
                        ps.setString(2, req.reason);
                        ps.executeUpdate();
                    }
                }

                // Ban IP (if exists)
                if (lastIp != null && !lastIp.isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ip_blocks (ip, blocked_until, reason) VALUES (?, " + expiresClause + ", ?)")) {
                        ps.setString(1, lastIp);
                        ps.setString(2, req.reason);
                        ps.executeUpdate();
                    }
                }

                // Notify via Telegram
                if (telegramId != null && telegramId > 0) {
                    sendBanTelegram(telegramId, nickname, req.reason, displayDuration);
                }

                log.info("Player {} ({}) banned ({}): {}", nickname, uuid, displayDuration, req.reason);

                try {
                    java.sql.Timestamp expiresTs = null;
                    if (durationMinutes != null) {
                        expiresTs = new java.sql.Timestamp(System.currentTimeMillis() + durationMinutes * 60000L);
                    }
                    PunishmentRepository.addRecord(uuid, "BAN", req.reason, req.adminUuid, durationMinutes, expiresTs);
                } catch (Exception ignored) {}

                ctx.json(ApiResponse.ok(Map.of("uuid", uuid, "nickname", nickname, "duration", displayDuration)));
            } catch (Exception e) {
                log.error("Ban error", e);
                ctx.status(500).json(ApiResponse.error(e.getMessage()));
            }
        });

        // ── Unban player ────────────────────────────────────
        app.post("/api/v1/launcher/unban", ctx -> {
            UnbanReq req = ctx.bodyAsClass(UnbanReq.class);
            if (req.target == null) {
                ctx.json(ApiResponse.error("target required"));
                return;
            }

            try (Connection c = DatabaseManager.getConnection()) {
                String lookupSql = "SELECT uuid, nickname, hwid, last_ip, telegram_id FROM players WHERE uuid = ? OR nickname = ?";
                String uuid = null;
                String nickname = null;
                try (PreparedStatement ps = c.prepareStatement(lookupSql)) {
                    ps.setString(1, req.target);
                    ps.setString(2, req.target);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            uuid = rs.getString("uuid");
                            nickname = rs.getString("nickname");
                        }
                    }
                }
                if (uuid == null) {
                    ctx.json(ApiResponse.error("player not found"));
                    return;
                }

                // Get telegramId for notification
                Long tgId = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT telegram_id FROM players WHERE uuid = ?")) {
                    ps.setString(1, uuid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) tgId = rs.getLong("telegram_id");
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE players SET is_banned = FALSE, ban_reason = NULL, banned_until = NULL WHERE uuid = ?")) {
                    ps.setString(1, uuid);
                    ps.executeUpdate();
                }

                // Полный разбан: снимаем HWID- и IP-баны устройства
                try {
                    PlayerRepository.clearBans(uuid);
                } catch (Exception ignored) {}

                try {
                    PunishmentRepository.addRecord(uuid, "UNBAN", null, req.adminUuid, null, null);
                } catch (Exception ignored) {}

                if (tgId != null && tgId > 0) {
                    sendUnbanTelegram(tgId, nickname);
                }

                log.info("Player {} ({}) unbanned", nickname, uuid);
                ctx.json(ApiResponse.ok(Map.of("uuid", uuid, "nickname", nickname)));
            } catch (Exception e) {
                log.error("Unban error", e);
                ctx.status(500).json(ApiResponse.error(e.getMessage()));
            }
        });

        // ── Warn player ──────────────────────────────────────
        app.post("/api/v1/launcher/warn", ctx -> {
            WarnReq req = ctx.bodyAsClass(WarnReq.class);
            if (req.target == null || req.reason == null) {
                ctx.json(ApiResponse.error("target and reason required"));
                return;
            }

            try (Connection c = DatabaseManager.getConnection()) {
                String uuid = null;
                String nickname = null;
                String lookupSql = "SELECT uuid, nickname FROM players WHERE uuid = ? OR nickname = ?";
                try (PreparedStatement ps = c.prepareStatement(lookupSql)) {
                    ps.setString(1, req.target);
                    ps.setString(2, req.target);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            uuid = rs.getString("uuid");
                            nickname = rs.getString("nickname");
                        }
                    }
                }

                if (uuid == null) {
                    ctx.json(ApiResponse.error("player not found"));
                    return;
                }

                PunishmentRepository.addRecord(uuid, "WARN", req.reason, req.adminUuid, null, null);
                log.info("Player {} ({}) warned by {}: {}", nickname, uuid, req.adminUuid, req.reason);
                ctx.json(ApiResponse.ok(Map.of("uuid", uuid, "nickname", nickname)));
            } catch (Exception e) {
                log.error("Warn error", e);
                ctx.status(500).json(ApiResponse.error(e.getMessage()));
            }
        });
    }

    private static final String TG_COMMANDS_DIR = "C:/Users/maska/OneDrive/Desktop/PWP/bots/commands";

    private static void sendBanTelegram(Long telegramId, String nickname, String reason, String duration) {
        try {
            File dir = new File(TG_COMMANDS_DIR);
            dir.mkdirs();
            String fileName = "tg_ban_" + System.currentTimeMillis() + ".json";
            String text = "\u26D4 <b>\u0412\u042B \u0417\u0410\u0411\u041B\u041E\u041A\u0418\u0420\u041E\u0412\u0410\u041D\u042B</b>\n\n"
                + "\u041F\u0440\u0438\u0447\u0438\u043D\u0430: " + reason + "\n"
                + "\u0421\u0440\u043E\u043A: " + duration;
            String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            String json = "{\"action\":\"send\",\"chat_id\":\"" + telegramId + "\",\"text\":\"" + escaped + "\",\"parse_mode\":\"HTML\"}";
            Files.writeString(new File(dir, fileName).toPath(), json, StandardCharsets.UTF_8);
            log.info("Ban notification sent to tg {}", telegramId);
        } catch (Exception e) {
            log.warn("Failed to send ban Telegram: {}", e.getMessage());
        }
    }

    private static void sendUnbanTelegram(Long telegramId, String nickname) {
        try {
            File dir = new File(TG_COMMANDS_DIR);
            dir.mkdirs();
            String fileName = "tg_unban_" + System.currentTimeMillis() + ".json";
            String text = "\u2705 <b>\u0412\u042B \u0420\u0410\u0417\u0411\u041B\u041E\u041A\u0418\u0420\u041E\u0412\u0410\u041D\u042B</b>\n\n"
                + "\u041F\u0440\u0438\u044F\u0442\u043D\u043E\u0439 \u0438\u0433\u0440\u044B!";
            String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            String json = "{\"action\":\"send\",\"chat_id\":\"" + telegramId + "\",\"text\":\"" + escaped + "\",\"parse_mode\":\"HTML\"}";
            Files.writeString(new File(dir, fileName).toPath(), json, StandardCharsets.UTF_8);
            log.info("Unban notification sent to tg {}", telegramId);
        } catch (Exception e) {
            log.warn("Failed to send unban Telegram: {}", e.getMessage());
        }
    }

    private static int parseDuration(String s) {
        s = s.trim().toLowerCase();
        if (s.endsWith("m")) return Integer.parseInt(s.substring(0, s.length() - 1));
        if (s.endsWith("h")) return Integer.parseInt(s.substring(0, s.length() - 1)) * 60;
        if (s.endsWith("d")) return Integer.parseInt(s.substring(0, s.length() - 1)) * 1440;
        return Integer.parseInt(s) * 60; // default: hours
    }

    private static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " \u043C\u0438\u043D";
        if (minutes < 1440) return (minutes / 60) + " \u0447";
        return (minutes / 1440) + " \u0434\u043D\u0435\u0439";
    }

    private static boolean isHwidBanned(String hwid) {
        String sql = "SELECT banned_until FROM hwid_bans WHERE hwid = ? LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hwid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                java.sql.Timestamp until = rs.getTimestamp("banned_until");
                if (until != null && until.before(new java.util.Date())) {
                    try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM hwid_bans WHERE hwid = ?")) {
                        del.setString(1, hwid);
                        del.executeUpdate();
                    }
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void updateHwidHistory(String uuid, String hwid, String components, String pcName, String ip, int flags) {
        String sql = "INSERT INTO hwid_history (account_uuid, hwid, hwid_components, pc_name, ip, flags) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, hwid);
            if (components != null && components.length() > 2000) components = components.substring(0, 2000);
            ps.setString(3, components);
            if (pcName != null && pcName.length() > 128) pcName = pcName.substring(0, 128);
            ps.setString(4, pcName);
            ps.setString(5, ip);
            ps.setInt(6, flags);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private static String guessMime(String path) {
        if (path.endsWith(".jar")) return "application/java-archive";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".zip")) return "application/zip";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".txt")) return "text/plain";
        if (path.endsWith(".cfg") || path.endsWith(".conf") || path.endsWith(".toml"))
            return "text/plain";
        return "application/octet-stream";
    }

    private static class ServerTokenReq {
        public String sessionToken;
        public String accessToken;
        public String hwid;
    }

    private static class SubmitHwidReq {
        public String sessionToken;
        public String accessToken;
        public String hwid;
        public String hwidComponents;
        public String pcName;
        public int flags;
    }

    private static class HwidCheckReq {
        public String hwid;
    }

    private static class LogUploadReq {
        public String sessionToken;
        public List<LogEntry> logs;
    }

    private static class LogEntry {
        public String level;
        public String message;
    }

    public static class BanReq {
        public String target;
        public String reason;
        public String duration; // "30m", "2h", "7d", "perm"
        public String adminUuid;
    }

    public static class UnbanReq {
        public String target;
        public String adminUuid;
    }

    public static class WarnReq {
        public String target;
        public String reason;
        public String adminUuid;
    }

    public static class CheckBanReq {
        public String uuid;
    }

    public static class CheckSessionReq {
        public String uuid;
        public String nickname;
    }

    public static class VerifyReq {
        public String token;
    }
}

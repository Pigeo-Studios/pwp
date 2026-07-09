package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

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

        // ── Get server token (launcher → server auth) ──────
        app.post("/api/v1/launcher/server-token", ctx -> {
            ServerTokenReq req = ctx.bodyAsClass(ServerTokenReq.class);
            if ((req.sessionToken == null && req.accessToken == null) && req.hwid == null) {
                ctx.json(ApiResponse.error("session_token or access_token and hwid required"));
                return;
            }

            // Validate session
            String playerUuid = req.accessToken != null
                ? PlayerRepository.findUuidByAccessToken(req.accessToken)
                : PlayerRepository.findSessionPlayer(req.sessionToken);
            if (playerUuid == null) {
                ctx.json(ApiResponse.error("invalid or expired session"));
                return;
            }

            // Check ban
            var pl = PlayerRepository.findByUuid(playerUuid);
            if (pl == null || pl.isBanned) {
                ctx.json(ApiResponse.error("account is banned"));
                return;
            }

            // Check HWID ban (check using launcher's last known HWID)
            // Generate one-time server token (valid 2 minutes)
            String serverToken = generateToken();
            String sql = "INSERT INTO server_tokens (token, account_uuid, ip, expires_at) " +
                         "VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 MINUTE))";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, serverToken);
                ps.setString(2, playerUuid);
                ps.setString(3, ctx.ip());
                ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("token generation failed"));
                return;
            }

            PlayerRepository.log(playerUuid, "server_token", ctx.ip(), "token issued");

            ctx.json(ApiResponse.ok(Map.of(
                "server_token", serverToken,
                "username", pl.nickname,
                "uuid", playerUuid,
                "role", pl.role,
                "expires_in", 120
            )));
        });

        // ── Verify server token (server-side check) ────────
        app.post("/api/v1/launcher/verify-server-token", ctx -> {
            String token = ctx.queryParam("token");
            if (token == null) {
                ctx.json(ApiResponse.error("token required"));
                return;
            }

            String sql = "SELECT st.account_uuid, st.hwid, st.used, p.nickname, p.role, p.is_banned " +
                         "FROM server_tokens st JOIN players p ON st.account_uuid = p.uuid " +
                         "WHERE st.token = ? AND st.expires_at > CURRENT_TIMESTAMP";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getBoolean("used")) {
                            ctx.json(ApiResponse.error("token already used"));
                            return;
                        }
                        if (rs.getBoolean("is_banned")) {
                            ctx.json(ApiResponse.error("account is banned"));
                            return;
                        }
                        // Mark as used
                        try (PreparedStatement up = c.prepareStatement(
                                "UPDATE server_tokens SET used = TRUE WHERE token = ?")) {
                            up.setString(1, token);
                            up.executeUpdate();
                        }
                        ctx.json(ApiResponse.ok(Map.of(
                            "valid", true,
                            "uuid", rs.getString("account_uuid"),
                            "username", rs.getString("nickname"),
                            "role", rs.getString("role"),
                            "hwid", rs.getString("hwid")
                        )));
                    } else {
                        ctx.json(ApiResponse.error("invalid or expired token"));
                    }
                }
            } catch (Exception e) {
                ctx.json(ApiResponse.error("verification failed"));
            }
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
        app.get("/launcher/files/*", ctx -> {
            String relPath = ctx.path().substring("/launcher/files/".length());
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
    }

    private static boolean isHwidBanned(String hwid) {
        String sql = "SELECT 1 FROM hwid_bans WHERE hwid = ? LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hwid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
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

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static class SubmitHwidReq {
        public String sessionToken;
        public String accessToken;
        public String hwid;
        public String hwidComponents;
        public String pcName;
        public int flags;
    }

    private static class ServerTokenReq {
        public String sessionToken;
        public String accessToken;
        public String hwid;
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
}

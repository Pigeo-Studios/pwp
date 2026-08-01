package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.db.PunishmentRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.Player;
import io.javalin.Javalin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Античит-эндпоинты по контракту лаунчера (handshake.rs) + серверный P5-хэндшейк.
 *   GET  /api/v1/launcher/anticheat/blacklist → [{kind, pattern, matchType, severity}]
 *   POST /api/v1/launcher/anticheat/init      → {allowed, launchToken, reason}
 *   POST /api/v1/launcher/anticheat/detect    → {launchToken, source, type, signature, severity}
 *   POST /api/v1/launcher/p5/verify           (X-AC-P5-Secret)  вход-хэндшейк игрового сервера
 *   POST /api/v1/launcher/p5/revoked          (X-AC-P5-Secret)  отзыв доступа
 * Любой сбой = fail-open: пускаем, но пишем в лог.
 */
public class AnticheatController {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnticheatController.class);

    public AnticheatController(Javalin app) {

        // ── Раздача артефактов античита (agent.jar, anticheat.dll) ──
        app.get("/api/v1/launcher/anticheat/artifact/{name}", ctx -> {
            String name = ctx.pathParam("name");
            if (!name.matches("[A-Za-z0-9._-]+")) {
                ctx.status(400).json(ApiResponse.error("invalid artifact name"));
                return;
            }
            java.nio.file.Path dir = java.nio.file.Paths.get(CoreApplication.config.anticheat.artifactsDir);
            java.nio.file.Path file = dir.resolve(name).normalize();
            if (!file.startsWith(dir) || !java.nio.file.Files.exists(file)) {
                ctx.status(404).json(ApiResponse.error("artifact not found"));
                return;
            }
            try {
                ctx.contentType(name.endsWith(".jar")
                        ? "application/java-archive" : "application/octet-stream");
                ctx.result(java.nio.file.Files.newInputStream(file));
            } catch (Exception e) {
                ctx.status(500).json(ApiResponse.error("artifact read failed"));
            }
        });

        // ── Чёрный список сигнатур (хардкод-фолбэк остаётся в лаунчере) ──
        app.get("/api/v1/launcher/anticheat/blacklist", ctx -> {
            List<Map<String, Object>> items = new ArrayList<>();
            String sql = "SELECT kind, pattern, match_type, severity FROM anticheat_blacklist WHERE enabled = TRUE ORDER BY severity DESC";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("kind", rs.getString("kind") == null ? "" : rs.getString("kind"));
                    m.put("pattern", rs.getString("pattern") == null ? "" : rs.getString("pattern"));
                    m.put("matchType", rs.getString("match_type") == null ? "substring" : rs.getString("match_type"));
                    m.put("severity", rs.getInt("severity"));
                    items.add(m);
                }
            } catch (Exception e) {
                log.warn("blacklist load failed: {}", e.getMessage());
                ctx.json(ApiResponse.error("blacklist load failed"));
                return;
            }
            ctx.json(items);
        });

        // ── Init (handshake перед запуском игры) ──
        app.post("/api/v1/launcher/anticheat/init", ctx -> {
            InitReq req = ctx.bodyAsClass(InitReq.class);
            String playerUuid = PlayerRepository.findUuidByAccessToken(ctx.queryParam("access_token"));
            if (playerUuid == null) {
                ctx.json(ApiResponse.error("invalid session"));
                return;
            }
            // HWID ban — блокируем
            if (req.hwid != null && !req.hwid.isEmpty() && isHwidBanned(req.hwid)) {
                log.warn("AC init blocked (hwid ban) uuid={}", playerUuid);
                ctx.json(ApiResponse.ok(Map.of(
                    "allowed", false,
                    "launchToken", "",
                    "reason", "Ваше устройство заблокировано системой защиты.")));
                return;
            }
            // Логируем детекты, пришедшие с init
            List<Map<String, Object>> detections = req.detections != null ? req.detections : Collections.emptyList();
            for (Map<String, Object> d : detections) {
                insertDetection(playerUuid, null, "launcher", str(d.get("kind")), str(d.get("signature")), 0);
            }
            // Выдаём launch-token (30 мин)
            String token = randomToken(48);
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO anticheat_sessions (player_uuid, launch_token, hwid, expires_at) VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 MINUTE)) "
                     + "ON DUPLICATE KEY UPDATE hwid = VALUES(hwid), expires_at = VALUES(expires_at)")) {
                ps.setString(1, playerUuid);
                ps.setString(2, token);
                ps.setString(3, req.hwid);
                ps.executeUpdate();
            } catch (Exception e) {
                log.warn("launch token create failed: {}", e.getMessage());
                ctx.json(ApiResponse.error("session create failed"));
                return;
            }
            log.info("AC init ok uuid={}", playerUuid);
            ctx.json(ApiResponse.ok(Map.of("allowed", true, "launchToken", token)));
        });

        // ── Отчёт о детекте/событии агента (agent-alive = heartbeat) ──
        app.post("/api/v1/launcher/anticheat/detect", ctx -> {
            DetectReq req = ctx.bodyAsClass(DetectReq.class);
            String playerUuid = null;
            if (req.launchToken != null && !req.launchToken.isEmpty()) {
                playerUuid = findUuidByLaunchToken(req.launchToken);
                if ("agent-alive".equals(req.type)) {
                    touchHeartbeat(req.launchToken);
                }
            }
            insertDetection(playerUuid, req.launchToken, req.source, req.type, req.signature, req.severity);
            // Авто-бан при критическом severity (порог из конфига)
            int banSeverity = CoreApplication.config.anticheat.banSeverity;
            if (req.severity >= banSeverity && playerUuid != null) {
                autoBan(playerUuid, req.type, req.signature);
            }
            ctx.json(ApiResponse.ok("detect received"));
        });

        // ── Инвентарь модов (whitelist: сверка с манифестом) ──
        app.post("/api/v1/launcher/anticheat/files", ctx -> {
            FilesReq req = ctx.bodyAsClass(FilesReq.class);
            String playerUuid = null;
            if (req.launchToken != null && !req.launchToken.isEmpty()) {
                playerUuid = findUuidByLaunchToken(req.launchToken);
                touchHeartbeat(req.launchToken);
            }
            List<Map<String, Object>> files = req.files != null ? req.files : Collections.emptyList();
            if (files.isEmpty()) {
                ctx.json(ApiResponse.ok(Map.of("unknown", List.of())));
                return;
            }
            // Whitelist: все моды из манифеста (mods/ + optional_mods/), имя файла → sha256
            Map<String, String> whitelist = new HashMap<>();
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT file_path, sha256 FROM file_manifests "
                     + "WHERE category = 'mod' OR file_path LIKE 'mods/%' OR file_path LIKE 'optional_mods/%'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String path = rs.getString("file_path");
                        String leaf = path.substring(path.lastIndexOf('/') + 1);
                        whitelist.put(leaf, rs.getString("sha256") == null ? "" : rs.getString("sha256").toLowerCase());
                    }
                }
            } catch (Exception e) {
                log.warn("whitelist load failed: {}", e.getMessage());
                ctx.json(ApiResponse.error("whitelist load failed"));
                return;
            }
            // Fail-open: манифест не импортирован — ничего не проверяем
            if (whitelist.isEmpty()) {
                ctx.json(ApiResponse.ok(Map.of("unknown", List.of())));
                return;
            }
            List<String> unknown = new ArrayList<>();
            for (Map<String, Object> f : files) {
                String path = str(f.get("p"));
                String sha = str(f.get("s")).toLowerCase();
                String leaf = path.substring(path.lastIndexOf('/') + 1);
                String expected = whitelist.get(leaf);
                if (expected == null) {
                    unknown.add(path);
                    insertDetection(playerUuid, req.launchToken, "agent", "unknown-mod",
                        path + " (нет в манифесте)", 9);
                } else if (!expected.isEmpty() && !expected.equals(sha)) {
                    unknown.add(path);
                    insertDetection(playerUuid, req.launchToken, "agent", "tampered-mod",
                        path + " (SHA-256 не совпадает)", 9);
                }
            }
            ctx.json(ApiResponse.ok(Map.of("unknown", unknown)));
        });

        // ── Скриншот от агента (base64 BMP → JPEG + БД) ──
        app.post("/api/v1/launcher/anticheat/screenshot", ctx -> {
            ShotReq req = ctx.bodyAsClass(ShotReq.class);
            String playerUuid = null;
            if (req.launchToken != null && !req.launchToken.isEmpty()) {
                playerUuid = findUuidByLaunchToken(req.launchToken);
                touchHeartbeat(req.launchToken);
            }
            if (req.data == null || req.data.isEmpty()) {
                ctx.json(ApiResponse.error("data required"));
                return;
            }
            try {
                byte[] bmp = java.util.Base64.getMimeDecoder().decode(req.data);
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bmp));
                if (img == null) {
                    insertDetection(playerUuid, req.launchToken, "agent", "screenshot-error", "не удалось декодировать кадр", 7);
                    ctx.json(ApiResponse.error("decode failed"));
                    return;
                }
                File dir = new File(CoreApplication.config.anticheat.screenshotDir);
                if (!dir.exists()) dir.mkdirs();
                String name = (playerUuid == null ? "unknown" : playerUuid) + "_" + System.currentTimeMillis() + ".jpg";
                File out = new File(dir, name);
                if (!javax.imageio.ImageIO.write(img, "jpg", out)) {
                    ctx.json(ApiResponse.error("jpeg write failed"));
                    return;
                }
                try (Connection c = DatabaseManager.getConnection();
                     PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO anticheat_screenshots (player_uuid, file_path, width, height) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, playerUuid);
                    ps.setString(2, out.getAbsolutePath());
                    ps.setInt(3, img.getWidth());
                    ps.setInt(4, img.getHeight());
                    ps.executeUpdate();
                }
                log.info("AC screenshot saved: {} ({}x{})", name, img.getWidth(), img.getHeight());
                ctx.json(ApiResponse.ok(Map.of("saved", name)));
            } catch (Exception e) {
                log.warn("screenshot save failed: {}", e.getMessage());
                insertDetection(playerUuid, req.launchToken, "agent", "screenshot-error", "ошибка сохранения", 7);
                ctx.json(ApiResponse.error("screenshot save failed"));
            }
        });

        // ── P5: вход-хэндшейк (игровой сервер → бэкенд) ──
        app.post("/api/v1/launcher/p5/verify", ctx -> {
            if (!p5Authorized(ctx.header("X-AC-P5-Secret"))) {
                ctx.status(403).json(ApiResponse.error("forbidden"));
                return;
            }
            P5VerifyReq req = ctx.bodyAsClass(P5VerifyReq.class);
            String playerName = req.playerName == null ? "" : req.playerName;
            String challenge = req.challenge == null ? "" : req.challenge;
            String proof = req.proof == null ? "" : req.proof.toLowerCase();

            Player pl = findByNicknameSafe(playerName);
            if (pl == null) {
                log.warn("P5 verify: unknown player '{}'", playerName);
                ctx.json(verifyResponse(false, "Игрок не найден", "unknown player"));
                return;
            }
            String accessToken = findAccessTokenByUuid(pl.uuid);
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("P5 verify: no active session for '{}'", playerName);
                ctx.json(verifyResponse(false, "Сессия не подтверждена", "no active session"));
                return;
            }
            boolean ok = challenge.isEmpty() || constantTimeEquals(hmacHex(challenge, accessToken), proof);
            if (ok) {
                log.info("P5 verify OK: {}", playerName);
                ctx.json(Map.of("allow", true, "reason", "ok"));
            } else {
                log.warn("P5 verify MISMATCH: {} (challenge={}, proof={})", playerName, challenge, proof);
                ctx.json(verifyResponse(false, "Подтверждение входа не пройдено", "proof mismatch"));
            }
        });

        // ── P5: отзыв доступа (игровой сервер → бэкенд, раз в 25с) ──
        app.post("/api/v1/launcher/p5/revoked", ctx -> {
            if (!p5Authorized(ctx.header("X-AC-P5-Secret"))) {
                ctx.status(403).json(ApiResponse.error("forbidden"));
                return;
            }
            P5RevokedReq req = ctx.bodyAsClass(P5RevokedReq.class);
            boolean enforce = CoreApplication.config.anticheat.p5Enforce;
            int heartbeatTimeoutSec = CoreApplication.config.anticheat.heartbeatTimeoutSec;
            List<Map<String, Object>> kick = new ArrayList<>();
            List<String> players = req.players != null ? req.players : Collections.emptyList();

            for (String name : players) {
                String reason = revokeReason(name, heartbeatTimeoutSec);
                if (reason == null) continue;
                if (enforce) {
                    kick.add(Map.of("player", name, "reason", reason));
                    log.info("P5 revoke KICK {}: {}", name, reason);
                } else {
                    log.warn("P5 revoke (report-only) {}: {}", name, reason);
                }
            }
            ctx.json(Map.of("kick", kick, "reportOnly", !enforce));
        });
    }

    // ── P5 helpers ──

    private static Map<String, Object> verifyResponse(boolean allow, String reason, String logDetail) {
        boolean enforce = CoreApplication.config.anticheat.p5Enforce;
        if (!enforce) {
            return Map.of("allow", true, "reason", reason, "reportOnly", true);
        }
        return Map.of("allow", allow, "reason", reason);
    }

    private static boolean p5Authorized(String header) {
        String secret = CoreApplication.config.anticheat.p5Secret;
        if (secret == null || secret.isEmpty()) {
            return false; // P5 выключен — сервер-мод получит 403 и должен быть в fail-open
        }
        return constantTimeEquals(secret, header == null ? "" : header);
    }

    /** Причина отзыва для игрока: детект или молчание heartbeat агента. */
    private static String revokeReason(String playerName, int heartbeatTimeoutSec) {
        Player pl = findByNicknameSafe(playerName);
        if (pl == null) return null;
        // 1. Свежие hard-детекты (severity >= 8), кроме heartbeat
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT d.type, d.signature, d.severity FROM anticheat_detections d "
                 + "WHERE d.player_uuid = ? AND d.type <> 'agent-alive' AND d.severity >= 8 "
                 + "AND d.created_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE) "
                 + "ORDER BY d.id DESC LIMIT 1")) {
            ps.setString(1, pl.uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Детект: " + rs.getString("type") + " (" + rs.getString("signature") + ")";
                }
            }
        } catch (Exception e) {
            log.warn("revoke detect query failed: {}", e.getMessage());
        }
        // 2. Активная launch-сессия есть, но heartbeat замолчал (агента убили в игре)
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT s.launch_token FROM anticheat_sessions s "
                 + "WHERE s.player_uuid = ? AND s.expires_at > CURRENT_TIMESTAMP "
                 + "AND (s.last_heartbeat IS NULL OR s.last_heartbeat < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? SECOND)) "
                 + "LIMIT 1")) {
            ps.setString(1, pl.uuid);
            ps.setInt(2, heartbeatTimeoutSec);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Агент античита перестал отвечать";
                }
            }
        } catch (Exception e) {
            log.warn("revoke heartbeat query failed: {}", e.getMessage());
        }
        return null;
    }

    private static void touchHeartbeat(String launchToken) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE anticheat_sessions SET last_heartbeat = CURRENT_TIMESTAMP WHERE launch_token = ?")) {
            ps.setString(1, launchToken);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private static String findUuidByLaunchToken(String launchToken) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT player_uuid FROM anticheat_sessions WHERE launch_token = ? LIMIT 1")) {
            ps.setString(1, launchToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("player_uuid");
            }
        } catch (Exception e) {
            log.warn("launch token lookup failed: {}", e.getMessage());
        }
        return null;
    }

    private static String findAccessTokenByUuid(String playerUuid) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT access_token FROM sessions WHERE player_uuid = ? AND access_token IS NOT NULL "
                 + "AND expires_at > CURRENT_TIMESTAMP ORDER BY expires_at DESC LIMIT 1")) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("access_token");
            }
        } catch (Exception e) {
            log.warn("access token lookup failed: {}", e.getMessage());
        }
        return null;
    }

    private static void insertDetection(String playerUuid, String launchToken, String source, String type, String signature, int severity) {
        if (type == null || type.isEmpty()) return;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO anticheat_detections (player_uuid, launch_token, source, type, signature, severity) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, playerUuid);
            ps.setString(2, launchToken);
            ps.setString(3, source == null ? "launcher" : source);
            ps.setString(4, type);
            if (signature != null && signature.length() > 1000) signature = signature.substring(0, 1000);
            ps.setString(5, signature);
            ps.setInt(6, severity);
            ps.executeUpdate();
            if (severity > 0) {
                log.warn("AC detection: uuid={} source={} type={} sig={} severity={}", playerUuid, source, type, signature, severity);
                notifyTelegramAdmins(type, signature, playerUuid, severity);
            }
        } catch (Exception e) {
            log.warn("detection insert failed: {}", e.getMessage());
        }
    }

    /** Алерт админам в Telegram через файл-команду для бота (PWP/bots). */
    private static void notifyTelegramAdmins(String type, String signature, String playerUuid, int severity) {
        String tgIds = CoreApplication.config.api.adminTelegramIds;
        if (tgIds == null || tgIds.trim().isEmpty()) return;
        String nickname = playerUuid == null ? "—" : playerUuid;
        try {
            Player pl = playerUuid == null ? null : PlayerRepository.findByUuid(playerUuid);
            if (pl != null) nickname = pl.nickname;
        } catch (Exception ignored) {}
        String text = "\u26A0\uFE0F <b>ДЕТЕКТ АНТИЧИТА</b>\n\n"
            + "\uD83D\uDC64 Игрок: <b>" + escapeHtml(nickname) + "</b>\n"
            + "\uD83D\uDD17 Тип: <b>" + escapeHtml(type) + "</b>\n"
            + "\uD83D\uDCCB Сигнатура: " + escapeHtml(signature) + "\n"
            + "\uD83D\uDCA5 Severity: <b>" + severity + "</b>";
        for (String id : tgIds.split(",")) {
            String chatId = id.trim();
            if (chatId.isEmpty()) continue;
            try {
                File dir = new File("C:/Users/maska/OneDrive/Desktop/PWP/bots/commands");
                dir.mkdirs();
                String json = "{\"action\":\"send\",\"chat_id\":\"" + chatId + "\",\"text\":\""
                    + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                    + "\",\"parse_mode\":\"HTML\"}";
                java.nio.file.Files.writeString(new File(dir, "tg_ac_" + System.currentTimeMillis() + "_" + chatId + ".json").toPath(),
                    json, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("tg alert failed: {}", e.getMessage());
            }
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void autoBan(String playerUuid, String type, String signature) {
        try {
            Player pl = PlayerRepository.findByUuid(playerUuid);
            if (pl == null) return;
            PlayerRepository.setBan(playerUuid, true, "Античит: " + type);
            PunishmentRepository.addRecord(playerUuid, "BAN", "Античит: " + type + " (" + signature + ")", null, null, null);
            log.warn("AC AUTO-BAN: {} ({}) type={}", pl.nickname, playerUuid, type);
        } catch (Exception e) {
            log.warn("auto-ban failed: {}", e.getMessage());
        }
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
                    try (PreparedStatement del = c.prepareStatement("DELETE FROM hwid_bans WHERE hwid = ?")) {
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

    private static Player findByNicknameSafe(String nickname) {
        try {
            return PlayerRepository.findByNickname(nickname);
        } catch (Exception e) {
            return null;
        }
    }

    private static String randomToken(int hexLen) {
        byte[] b = new byte[hexLen / 2];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String hmacHex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            return bytesToHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    // ── Request bodies ──

    public static class InitReq {
        public String hwid;
        public String hwidComponents;
        public String pcName;
        public int flags;
        public List<Map<String, Object>> detections;
    }

    public static class DetectReq {
        public String launchToken;
        public String source;
        public String type;
        public String signature;
        public int severity;
    }

    public static class FilesReq {
        public String launchToken;
        public List<Map<String, Object>> files;
    }

    public static class ShotReq {
        public String launchToken;
        public String id;
        public String data;
    }

    public static class P5VerifyReq {
        public String playerName;
        public String challenge;
        public String proof;
    }

    public static class P5RevokedReq {
        public List<String> players;
    }
}

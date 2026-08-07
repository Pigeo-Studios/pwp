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

        // ── Правила для Java-агента (PJM-контракт): правила + версия для инкрементального
        //    ре-фетча. Агент шлёт совпадения на /detect с severity правила; кик решает сервер.
        app.get("/api/v1/launcher/anticheat/rules", ctx -> {
            if (!validLaunchToken(ctx.header("X-Launch-Token"))) {
                ctx.status(401).json(ApiResponse.error("invalid token"));
                return;
            }
            List<Map<String, Object>> rules = new ArrayList<>();
            String sql = "SELECT pattern, match_type, hash, severity FROM anticheat_blacklist WHERE enabled = TRUE ORDER BY severity DESC";
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("pattern", rs.getString("pattern") == null ? "" : rs.getString("pattern"));
                    m.put("matchType", rs.getString("match_type") == null ? "substring" : rs.getString("match_type"));
                    m.put("hash", rs.getString("hash") == null ? "" : rs.getString("hash"));
                    m.put("severity", rs.getInt("severity"));
                    rules.add(m);
                }
            } catch (Exception e) {
                log.warn("rules load failed: {}", e.getMessage());
                ctx.json(ApiResponse.error("rules load failed"));
                return;
            }
            long version = 0;
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT COALESCE(MAX(UNIX_TIMESTAMP(updated_at)), 0) FROM anticheat_blacklist")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) version = rs.getLong(1);
                }
            } catch (Exception e) {
                log.warn("rules version failed: {}", e.getMessage());
            }
            ctx.json(Map.of("version", version, "rules", rules));
        });

        // ── Init (handshake перед запуском игры) ──
        app.post("/api/v1/launcher/anticheat/init", ctx -> {
            InitReq req = parseBody(ctx, InitReq.class);
            if (req == null) {
                ctx.status(400).json(ApiResponse.error("invalid body"));
                return;
            }
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
                insertDetection(playerUuid, null, "launcher", str(d.get("kind")), str(d.get("signature")), "", 0);
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
            DetectReq req = parseBody(ctx, DetectReq.class);
            if (req == null) {
                ctx.status(400).json(ApiResponse.error("invalid body"));
                return;
            }
            String playerUuid = null;
            if (req.launchToken != null && !req.launchToken.isEmpty()) {
                playerUuid = findUuidByLaunchToken(req.launchToken);
                if ("agent-alive".equals(req.type)) {
                    touchHeartbeat(req.launchToken);
                }
            }
            String detail = req.details != null && req.details.get("name") != null
                ? str(req.details.get("name")) : "";
            insertDetection(playerUuid, req.launchToken, req.source, req.type, req.signature, detail, req.severity);
            // Авто-бан при критическом severity (порог из конфига)
            int banSeverity = CoreApplication.config.anticheat.banSeverity;
            boolean kick = req.severity >= banSeverity;
            if (kick && playerUuid != null) {
                autoBan(playerUuid, req.type, req.signature);
            }
            ctx.json(Map.of("action", kick ? "kick" : "none"));
        });

        // ── Инвентарь модов (whitelist: сверка с манифестом) ──
        app.post("/api/v1/launcher/anticheat/files", ctx -> {
            FilesReq req = parseBody(ctx, FilesReq.class);
            if (req == null) {
                ctx.status(400).json(ApiResponse.error("invalid body"));
                return;
            }
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
                        path + " (нет в манифесте)", "", 9);
                } else if (!expected.isEmpty() && !expected.equals(sha)) {
                    unknown.add(path);
                    insertDetection(playerUuid, req.launchToken, "agent", "tampered-mod",
                        path + " (SHA-256 не совпадает)", "", 9);
                }
            }
            ctx.json(Map.of("unknown", unknown, "action", unknown.isEmpty() ? "none" : "kick"));
        });

        // ── Heartbeat агента (PJM-контракт): пингует сессию, отвечает киком при отзыве
        //    и отдаёт версию правил для инкрементального ре-фетча. Сеть = fail-open.
        app.post("/api/v1/launcher/anticheat/heartbeat", ctx -> {
            HeartbeatReq req = parseBody(ctx, HeartbeatReq.class);
            if (req == null || req.launchToken == null || req.launchToken.isEmpty()) {
                ctx.status(400).json(ApiResponse.error("invalid body"));
                return;
            }
            String playerUuid = findUuidByLaunchToken(req.launchToken);
            String action = "none";
            String reason = "";
            if (playerUuid == null) {
                // Сессия не найдена/истекла — агент работает с чужим токеном
                action = "kick";
                reason = "session-invalid";
            } else {
                touchHeartbeat(req.launchToken);
                // Отозванная сессия или бан игрока → кик
                if (isSessionRevoked(req.launchToken)) {
                    action = "kick";
                    reason = "session-revoked";
                } else if (isPlayerBanned(playerUuid)) {
                    action = "kick";
                    reason = "account-banned";
                }
            }
            long version = 0;
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT COALESCE(MAX(UNIX_TIMESTAMP(updated_at)), 0) FROM anticheat_blacklist")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) version = rs.getLong(1);
                }
            } catch (Exception e) {
                log.warn("rules version failed: {}", e.getMessage());
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("action", action);
            resp.put("blacklistVersion", version);
            if (!reason.isEmpty()) resp.put("reason", reason);
            ctx.json(resp);
        });

        // ── Скриншот от агента (base64 BMP → JPEG + БД) ──
        app.post("/api/v1/launcher/anticheat/screenshot", ctx -> {
            ShotReq req = parseBody(ctx, ShotReq.class);
            if (req == null) {
                ctx.status(400).json(ApiResponse.error("invalid body"));
                return;
            }
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
                    insertDetection(playerUuid, req.launchToken, "agent", "screenshot-error", "не удалось декодировать кадр", "", 7);
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
                insertDetection(playerUuid, req.launchToken, "agent", "screenshot-error", "ошибка сохранения", "", 7);
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
                 "UPDATE anticheat_sessions SET last_heartbeat = CURRENT_TIMESTAMP, "
                 + "expires_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 MINUTE) WHERE launch_token = ?")) {
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

    /** true, если launch-токен существует и не истёк (для рулзов/правил агента). */
    private static boolean validLaunchToken(String launchToken) {
        if (launchToken == null || launchToken.isEmpty()) return false;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT 1 FROM anticheat_sessions WHERE launch_token = ? AND expires_at > CURRENT_TIMESTAMP LIMIT 1")) {
            ps.setString(1, launchToken);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            log.warn("launch token check failed: {}", e.getMessage());
            return false;
        }
    }

    /** true, если сессия помечена отозванной (админ/другой поток). */
    private static boolean isSessionRevoked(String launchToken) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT revoked FROM anticheat_sessions WHERE launch_token = ? LIMIT 1")) {
            ps.setString(1, launchToken);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPlayerBanned(String playerUuid) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT is_banned FROM players WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBoolean(1)) {
                    return true; // бан до banned_until уже обрабатывается на входе
                }
                // Временный бан: is_banned может быть FALSE при истёкшем сроке — проверяем отдельно
                try (PreparedStatement ps2 = c.prepareStatement(
                        "SELECT banned_until FROM players WHERE uuid = ?")) {
                    ps2.setString(1, playerUuid);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            java.sql.Timestamp until = rs2.getTimestamp("banned_until");
                            return until != null && until.after(new java.util.Date());
                        }
                    }
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
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

    private static void insertDetection(String playerUuid, String launchToken, String source, String type, String signature, String detail, int severity) {
        if (type == null || type.isEmpty()) return;
        // Дедуп: тот же (player, type, signature) в окне 10 минут — один буст детектов,
        // одна запись (иначе каждый запуск игры флудил БД и TG одним и тем же сигналом).
        if (playerUuid != null && signature != null && !signature.isEmpty()) {
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM anticheat_detections WHERE player_uuid = ? AND type = ? AND signature = ? "
                     + "AND created_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 MINUTE)")) {
                ps.setString(1, playerUuid);
                ps.setString(2, type);
                ps.setString(3, signature);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return; // дубликат — не флудим БД и алерты
                    }
                }
            } catch (Exception e) {
                log.warn("detection dedupe failed: {}", e.getMessage());
            }
        }
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO anticheat_detections (player_uuid, launch_token, source, type, signature, details, severity) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, playerUuid);
            ps.setString(2, launchToken);
            ps.setString(3, source == null ? "launcher" : source);
            ps.setString(4, type);
            if (signature != null && signature.length() > 1000) signature = signature.substring(0, 1000);
            ps.setString(5, signature);
            if (detail != null && detail.length() > 2000) detail = detail.substring(0, 2000);
            ps.setString(6, detail);
            ps.setInt(7, severity);
            ps.executeUpdate();
            if (severity > 0) {
                log.warn("AC detection: uuid={} source={} type={} sig={} details={} severity={}", playerUuid, source, type, signature, detail, severity);
                notifyTelegramAdmins(type, signature, playerUuid, severity, detail);
            }
        } catch (Exception e) {
            log.warn("detection insert failed: {}", e.getMessage());
        }
    }

    /** Алерт админам в Telegram через файл-команду для бота (PWP/bots). */
    private static void notifyTelegramAdmins(String type, String signature, String playerUuid, int severity, String detail) {
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
            + (detail == null || detail.isEmpty() ? "" : "\uD83D\uDCC4 Детали: " + escapeHtml(detail) + "\n")
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

    /** Безопасный парсинг тела: кривой JSON → null (400), а не 500. */
    private static <T> T parseBody(io.javalin.http.Context ctx, Class<T> type) {
        try {
            return ctx.bodyAsClass(type);
        } catch (Exception e) {
            log.warn("bad JSON body for {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
            return null;
        }
    }

    // ── Request bodies ──

    public static class InitReq {
        public String hwid;
        public String hwidComponents;
        public String pcName;
        public String flags;
        public List<Map<String, Object>> detections;
    }

    public static class DetectReq {
        public String launchToken;
        public String source;
        public String type;
        public String signature;
        public int severity;
        public Map<String, Object> details;
    }

    public static class FilesReq {
        public String launchToken;
        public List<Map<String, Object>> files;
    }

    public static class HeartbeatReq {
        public String launchToken;
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

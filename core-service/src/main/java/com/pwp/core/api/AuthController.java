package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.model.Player;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final ConcurrentHashMap<String, RateBucket> loginBuckets = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RateBucket> resetBuckets = new ConcurrentHashMap<>();

    public AuthController(Javalin app, CoreApplication.Config config) {

        // ── Check availability (rate-limited) ─────────────
        app.get("/api/v1/auth/check-login", ctx -> {
            checkRateLimit(ctx.ip(), "check", 30, config);
            String login = ctx.queryParam("login");
            boolean taken = false;
            Player byLogin = PlayerRepository.findByLogin(login);
            if (byLogin != null) {
                taken = true; // login already linked
            } else {
                Player byNick = PlayerRepository.findByNickname(login);
                // nickname exists but has no launcher account — allow linking
                if (byNick != null && byNick.passwordHash != null) {
                    taken = true;
                }
            }
            ctx.json(ApiResponse.ok(Map.of("available", !taken)));
        });

        app.get("/api/v1/auth/check-email", ctx -> {
            checkRateLimit(ctx.ip(), "check", 30, config);
            String email = ctx.queryParam("email");
            boolean taken = email != null && PlayerRepository.findByEmail(email) != null;
            ctx.json(ApiResponse.ok(Map.of("available", !taken)));
        });

        // ── Register ──────────────────────────────────────
        app.post("/api/v1/auth/register", ctx -> {
            RegisterReq req = ctx.bodyAsClass(RegisterReq.class);
            if (req.login == null || req.email == null || req.password == null) {
                ctx.json(ApiResponse.error("login, email and password are required")); return;
            }
            if (req.login.length() < 3 || req.login.length() > 32) {
                ctx.json(ApiResponse.error("login must be 3-32 characters")); return;
            }
            if (req.password.length() < 8) {
                ctx.json(ApiResponse.error("password must be at least 8 characters")); return;
            }
            if (!EMAIL_PATTERN.matcher(req.email).matches()) {
                ctx.json(ApiResponse.error("invalid email format")); return;
            }
            if (req.telegramId == 0) {
                ctx.json(ApiResponse.error("telegram_id is required")); return;
            }
            if (PlayerRepository.findByLogin(req.login) != null) {
                ctx.json(ApiResponse.error("login already taken")); return;
            }
            if (PlayerRepository.findByEmail(req.email) != null) {
                ctx.json(ApiResponse.error("email already registered")); return;
            }
            if (PlayerRepository.findByTelegramId(req.telegramId) != null) {
                ctx.json(ApiResponse.error("telegram account already registered")); return;
            }

            String hash = BCrypt.hashpw(req.password, BCrypt.gensalt(12));
            String uuid = UUID.randomUUID().toString();
            Player pl = PlayerRepository.register(uuid, req.login, req.login, req.email, hash, req.telegramId);
            if (pl == null) {
                ctx.json(ApiResponse.error("registration failed")); return;
            }
            PlayerRepository.log(uuid, "register", ctx.ip(), "registered via telegram");
            ctx.json(ApiResponse.ok(Map.of("uuid", uuid, "login", req.login)));
        });

        // ── Accept Privacy Policy ─────────────────────────
        app.post("/api/v1/auth/accept-privacy", ctx -> {
            ReqUuid req = ctx.bodyAsClass(ReqUuid.class);
            Player pl = PlayerRepository.findByUuid(req.uuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            PlayerRepository.acceptPrivacy(req.uuid);
            PlayerRepository.log(req.uuid, "accept_privacy", ctx.ip(), null);
            ctx.json(ApiResponse.ok("ok"));
        });

        // ── Login (rate-limited per IP) ──────────────────
        app.post("/api/v1/auth/login", ctx -> {
            checkRateLimit(ctx.ip(), "login", 10, config);
            LoginReq req = ctx.bodyAsClass(LoginReq.class);
            if (req.login == null || req.password == null) {
                ctx.json(ApiResponse.error("login and password required")); return;
            }
            Player pl = PlayerRepository.findByLogin(req.login);
            if (pl == null) {
                ctx.json(ApiResponse.error("invalid login or password")); return;
            }
            if (pl.isBanned) {
                ctx.json(ApiResponse.error("account is banned")); return;
            }
            if (pl.passwordHash == null || !BCrypt.checkpw(req.password, pl.passwordHash)) {
                ctx.json(ApiResponse.error("invalid login or password")); return;
            }
            if (!pl.privacyPolicyAccepted) {
                ctx.json(ApiResponse.error("privacy policy not accepted")); return;
            }

            String clientIp = ctx.ip();
            boolean needs2fa = pl.launcher2faEnabled && !PlayerRepository.isIpTrusted(pl.uuid, clientIp);

            if (needs2fa) {
                String code = PlayerRepository.create2faCode(pl.uuid, clientIp);
                PlayerRepository.log(pl.uuid, "2fa_sent", clientIp, "2fa code: " + code);
                send2faToTelegram(pl.telegramId, code);
                ctx.json(ApiResponse.ok(Map.of("2fa_required", true, "uuid", pl.uuid, "telegram_id", pl.telegramId)));
            } else {
                String accessToken = PlayerRepository.generateTokenPart();
                String refreshToken = PlayerRepository.generateTokenPart();
                String sessionKey = PlayerRepository.generateSessionKey();
                PlayerRepository.createSessionV2(pl.uuid, accessToken, refreshToken, sessionKey, clientIp);
                String authToken = PlayerRepository.createAuthToken(pl.accountId);
                PlayerRepository.trustIp(pl.uuid, clientIp);
                PlayerRepository.updateLastLogin(pl.uuid, clientIp);
                PlayerRepository.log(pl.uuid, "login", clientIp, "login from ip");
                ctx.json(ApiResponse.ok(Map.of(
                    "authToken", authToken,
                    "access_token", accessToken,
                    "refresh_token", refreshToken,
                    "session_key", sessionKey,
                    "expiresIn", 604800,
                    "accountId", pl.accountId,
                    "uuid", pl.uuid,
                    "login", pl.login,
                    "nickname", pl.nickname,
                    "role", pl.role
                )));
            }
        });

        // ── Verify 2FA (rate-limited) ────────────────────
        app.post("/api/v1/auth/verify-2fa", ctx -> {
            checkRateLimit(ctx.ip(), "2fa", 10, config);
            Verify2faReq req = ctx.bodyAsClass(Verify2faReq.class);
            if (req.uuid == null || req.code == null) {
                ctx.json(ApiResponse.error("uuid and code required")); return;
            }
            if (!PlayerRepository.validate2faCode(req.uuid, req.code)) {
                ctx.json(ApiResponse.error("invalid or expired code")); return;
            }
            Player pl = PlayerRepository.findByUuid(req.uuid);
            if (pl == null || pl.isBanned) {
                ctx.json(ApiResponse.error("player not found or banned")); return;
            }
            String clientIp = ctx.ip();
            String accessToken = PlayerRepository.generateTokenPart();
            String refreshToken = PlayerRepository.generateTokenPart();
            String sessionKey = PlayerRepository.generateSessionKey();
            PlayerRepository.createSessionV2(pl.uuid, accessToken, refreshToken, sessionKey, clientIp);
            String authToken = PlayerRepository.createAuthToken(pl.accountId);
            PlayerRepository.trustIp(pl.uuid, clientIp);
            PlayerRepository.updateLastLogin(pl.uuid, clientIp);
            PlayerRepository.log(pl.uuid, "login_2fa", clientIp, "login via 2fa");
            ctx.json(ApiResponse.ok(Map.of(
                "authToken", authToken,
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "session_key", sessionKey,
                "expiresIn", 604800,
                "accountId", pl.accountId,
                "uuid", pl.uuid,
                "login", pl.login,
                "nickname", pl.nickname,
                "role", pl.role
            )));
        });

        // ── Confirm login from TG ─────────────────────────
        app.post("/api/v1/auth/confirm-login", ctx -> {
            ConfirmLoginReq req = ctx.bodyAsClass(ConfirmLoginReq.class);
            Player pl = PlayerRepository.findByTelegramId(req.telegramId);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            String ip = req.ip != null ? req.ip : ctx.ip();
            PlayerRepository.trustIp(pl.uuid, ip);
            PlayerRepository.log(pl.uuid, "ip_confirmed", ip, "user confirmed IP via telegram");
            ctx.json(ApiResponse.ok("ip trusted"));
        });

        // ── Get profile (requires session token OR API key) ──
        app.get("/api/v1/auth/profile", ctx -> {
            String uuid = ctx.queryParam("uuid");
            String sessionToken = ctx.queryParam("token");
            // Allow if valid session token for this UUID or if called with API key (admin/bot)
            if (sessionToken != null) {
                String found = PlayerRepository.findSessionPlayer(sessionToken);
                if (found == null || !found.equals(uuid)) {
                    ctx.json(ApiResponse.error("unauthorized")); return;
                }
            }
            // If no session token, rely on API key (already validated by AuthMiddleware)
            Player pl = PlayerRepository.findByUuid(uuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("uuid", pl.uuid);
            m.put("login", pl.login);
            m.put("email", maskEmail(pl.email));
            m.put("nickname", pl.nickname);
            m.put("role", pl.role);
            m.put("registered_at", pl.firstJoin);
            m.put("last_login", pl.lastJoin);
            m.put("2fa_enabled", pl.launcher2faEnabled);
            m.put("privacy_accepted", pl.privacyPolicyAccepted);
            ctx.json(ApiResponse.ok(m));
        });

        app.get("/api/v1/auth/profile-by-tg", ctx -> {
            long tgId;
            try { tgId = Long.parseLong(ctx.queryParam("telegram_id")); }
            catch (Exception e) { ctx.json(ApiResponse.error("invalid telegram_id")); return; }
            Player pl = PlayerRepository.findByTelegramId(tgId);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("uuid", pl.uuid);
            m.put("login", pl.login);
            m.put("email", maskEmail(pl.email));
            m.put("nickname", pl.nickname);
            m.put("role", pl.role);
            m.put("registered_at", pl.firstJoin);
            m.put("last_login", pl.lastJoin);
            m.put("2fa_enabled", pl.launcher2faEnabled);
            m.put("privacy_accepted", pl.privacyPolicyAccepted);
            ctx.json(ApiResponse.ok(m));
        });

        // ── Change password ───────────────────────────────
        app.post("/api/v1/auth/change-password", ctx -> {
            ChangePasswordReq req = ctx.bodyAsClass(ChangePasswordReq.class);
            if (req.uuid == null || req.oldPassword == null || req.newPassword == null) {
                ctx.json(ApiResponse.error("uuid, old_password and new_password required")); return;
            }
            if (req.newPassword.length() < 8) {
                ctx.json(ApiResponse.error("password must be at least 8 characters")); return;
            }
            Player pl = PlayerRepository.findByUuid(req.uuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            if (pl.passwordHash == null || !BCrypt.checkpw(req.oldPassword, pl.passwordHash)) {
                ctx.json(ApiResponse.error("old password is incorrect")); return;
            }
            String newHash = BCrypt.hashpw(req.newPassword, BCrypt.gensalt(12));
            PlayerRepository.updatePassword(req.uuid, newHash);
            PlayerRepository.log(req.uuid, "change_password", ctx.ip(), "password changed");
            ctx.json(ApiResponse.ok("password changed"));
        });

        // ── Forgot password (rate-limited per UUID) ──────
        app.post("/api/v1/auth/forgot-password", ctx -> {
            ReqUuid req = ctx.bodyAsClass(ReqUuid.class);
            checkRateLimit("forgot_" + req.uuid, "forgot", 3, config);
            Player pl = PlayerRepository.findByUuid(req.uuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            int rid = PlayerRepository.createResetRequest(req.uuid);
            PlayerRepository.log(req.uuid, "forgot_password", ctx.ip(), "reset request #" + rid);
            ctx.json(ApiResponse.ok(Map.of("reset_id", rid, "status", "pending")));
        });

        // ── Set new password after reset (check approved request) ──
        app.post("/api/v1/auth/set-password-after-reset", ctx -> {
            try {
                SetPasswordReq req = ctx.bodyAsClass(SetPasswordReq.class);
                if (req.uuid == null || req.newPassword == null) {
                    ctx.json(ApiResponse.error("uuid and new_password required")); return;
                }
                if (req.newPassword.length() < 8) {
                    ctx.json(ApiResponse.error("password must be at least 8 characters")); return;
                }
                // Verify there's been a reset request
                var resets = PlayerRepository.findPendingResets();
                boolean hasRequest = false;
                if (resets != null) {
                    for (var r : resets) {
                        if (r.playerUuid != null && r.playerUuid.equals(req.uuid)) {
                            hasRequest = true; break;
                        }
                    }
                }
                if (!hasRequest) {
                    ctx.json(ApiResponse.error("no reset request found")); return;
                }
                Player pl = PlayerRepository.findByUuid(req.uuid);
                if (pl == null) {
                    ctx.json(ApiResponse.error("player not found")); return;
                }
                String newHash = BCrypt.hashpw(req.newPassword, BCrypt.gensalt(12));
                PlayerRepository.updatePassword(req.uuid, newHash);
                PlayerRepository.log(req.uuid, "reset_password_set", ctx.ip(), "new password set after reset approval");
                ctx.json(ApiResponse.ok("password updated"));
            } catch (Exception e) {
                ctx.json(ApiResponse.error("internal error: " + e.getMessage()));
            }
        });

        // ── Toggle 2FA ────────────────────────────────────
        app.post("/api/v1/auth/toggle-2fa", ctx -> {
            Toggle2faReq req = ctx.bodyAsClass(Toggle2faReq.class);
            Player pl = PlayerRepository.findByUuid(req.uuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            PlayerRepository.toggle2fa(req.uuid, req.enabled);
            PlayerRepository.log(req.uuid, "toggle_2fa", ctx.ip(), "2fa: " + req.enabled);
            ctx.json(ApiResponse.ok("2fa " + (req.enabled ? "enabled" : "disabled")));
        });

        // ── Link telegram to existing account ────────────
        app.post("/api/v1/auth/link-telegram", ctx -> {
            LinkTelegramReq req = ctx.bodyAsClass(LinkTelegramReq.class);
            if (req.login == null || req.password == null || req.telegramId == 0) {
                ctx.json(ApiResponse.error("login, password and telegram_id required")); return;
            }
            Player pl = PlayerRepository.findByLogin(req.login);
            if (pl == null) {
                ctx.json(ApiResponse.error("invalid login or password")); return;
            }
            if (pl.passwordHash == null || !BCrypt.checkpw(req.password, pl.passwordHash)) {
                ctx.json(ApiResponse.error("invalid login or password")); return;
            }
            // Check if telegram already linked to another account
            Player existing = PlayerRepository.findByTelegramId(req.telegramId);
            if (existing != null && !existing.uuid.equals(pl.uuid)) {
                ctx.json(ApiResponse.error("telegram already linked to another account")); return;
            }
            String sql = "UPDATE players SET telegram_id = ? WHERE uuid = ?";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql)) {
                ps.setLong(1, req.telegramId);
                ps.setString(2, pl.uuid);
                ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("link failed")); return;
            }
            PlayerRepository.log(pl.uuid, "link_telegram", ctx.ip(), "telegram linked via login");
            ctx.json(ApiResponse.ok(Map.of("uuid", pl.uuid, "login", pl.login, "role", pl.role)));
        });

        // ── Unlink telegram from account ──────────────────
        app.post("/api/v1/auth/unlink-telegram", ctx -> {
            UnlinkTelegramReq req = ctx.bodyAsClass(UnlinkTelegramReq.class);
            if (req.uuid == null || req.telegramId == 0) {
                ctx.json(ApiResponse.error("uuid and telegram_id required")); return;
            }
            Player pl = PlayerRepository.findByUuid(req.uuid);
            if (pl == null || !Long.valueOf(req.telegramId).equals(pl.telegramId)) {
                ctx.json(ApiResponse.error("account not found or telegram mismatch")); return;
            }
            String sql = "UPDATE players SET telegram_id = NULL WHERE uuid = ?";
            try (var c = com.pwp.core.db.DatabaseManager.getConnection();
                 var ps = c.prepareStatement(sql)) {
                ps.setString(1, req.uuid);
                ps.executeUpdate();
            } catch (Exception e) {
                ctx.json(ApiResponse.error("unlink failed")); return;
            }
            PlayerRepository.log(req.uuid, "unlink_telegram", ctx.ip(), "telegram unlinked");
            ctx.json(ApiResponse.ok("unlinked"));
        });

        // ── Validate session ──────────────────────────────
        app.get("/api/v1/auth/validate-session", ctx -> {
            String token = ctx.queryParam("token");
            if (token == null) {
                ctx.json(ApiResponse.error("token required")); return;
            }
            String playerUuid = PlayerRepository.findSessionPlayer(token);
            if (playerUuid == null) {
                ctx.json(ApiResponse.error("invalid or expired session")); return;
            }
            Player pl = PlayerRepository.findByUuid(playerUuid);
            ctx.json(ApiResponse.ok(Map.of("valid", true, "uuid", pl.uuid, "login", pl.login, "role", pl.role, "is_banned", pl.isBanned)));
        });

        // ── Refresh session ───────────────────────────────
        app.post("/api/v1/auth/refresh", ctx -> {
            RefreshReq req = ctx.bodyAsClass(RefreshReq.class);
            if (req.refreshToken == null) {
                ctx.json(ApiResponse.error("refresh_token required")); return;
            }
            try {
                String newAccess = PlayerRepository.generateTokenPart();
                String newRefresh = PlayerRepository.generateTokenPart();
                String newKey = PlayerRepository.generateSessionKey();
                boolean ok = PlayerRepository.refreshSession(req.refreshToken, newAccess, newRefresh, newKey);
                if (!ok) {
                    ctx.json(ApiResponse.error("invalid or expired refresh token")); return;
                }
                ctx.json(ApiResponse.ok(Map.of(
                    "session_key", newKey,
                    "access_token", newAccess,
                    "refresh_token", newRefresh
                )));
            } catch (Exception e) {
                ctx.json(ApiResponse.error("refresh failed: " + e.getMessage()));
            }
        });

        // ── Heartbeat ──────────────────────────────────────
        app.post("/api/v1/auth/heartbeat", ctx -> {
            HeartbeatReq req = ctx.bodyAsClass(HeartbeatReq.class);
            if (req.accessToken == null) {
                ctx.json(ApiResponse.error("access_token required")); return;
            }
            try {
                String uuid = PlayerRepository.findUuidByAccessToken(req.accessToken);
                if (uuid == null) {
                    ctx.json(ApiResponse.error("invalid or expired session")); return;
                }
                // Check HWID if provided
                if (req.hwid != null && !req.hwid.isEmpty()) {
                    Player pl = PlayerRepository.findByUuid(uuid);
                    if (pl != null && pl.hwid != null && !pl.hwid.equals(req.hwid)) {
                        ctx.json(ApiResponse.ok(Map.of(
                            "hwid_changed", true,
                            "message", "HWID changed, need re-verification"
                        )));
                        return;
                    }
                }
                PlayerRepository.updateHeartbeat(req.accessToken);
                PlayerRepository.log(uuid, "heartbeat", ctx.ip(), null);
                ctx.json(ApiResponse.ok(Map.of("extended", true, "expires_in", 30)));
            } catch (Exception e) {
                ctx.json(ApiResponse.error("heartbeat failed"));
            }
        });

        // ── Revoke sessions (logout everywhere) ───────────
        app.post("/api/v1/auth/revoke-sessions", ctx -> {
            RevokeReq req = ctx.bodyAsClass(RevokeReq.class);
            if (req.accessToken == null) {
                ctx.json(ApiResponse.error("access_token required")); return;
            }
            try {
                String uuid = PlayerRepository.findUuidByAccessToken(req.accessToken);
                if (uuid != null) {
                    PlayerRepository.deleteSessionsByUuid(uuid);
                    PlayerRepository.log(uuid, "revoke_sessions", ctx.ip(), "all sessions revoked");
                }
                ctx.json(ApiResponse.ok("sessions revoked"));
            } catch (Exception e) {
                ctx.json(ApiResponse.error("revoke failed"));
            }
        });

        // ── Verify auth token (server-to-server) ─────────
        app.post("/api/v1/auth/verify", ctx -> {
            VerifyTokenReq req = ctx.bodyAsClass(VerifyTokenReq.class);
            if (req.token == null) {
                ctx.json(ApiResponse.error("token required")); return;
            }
            Player pl = PlayerRepository.findByAuthToken(req.token);
            if (pl == null) {
                ctx.json(ApiResponse.error("INVALID_TOKEN")); return;
            }
            if (pl.isBanned) {
                ctx.json(ApiResponse.error("ACCOUNT_BANNED")); return;
            }
            ctx.json(ApiResponse.ok(Map.of(
                "valid", true,
                "accountId", pl.accountId,
                "uuid", pl.uuid,
                "nickname", pl.nickname,
                "role", pl.role
            )));
        });

        // ── Logout (revoke token) ─────────────────────────
        app.post("/api/v1/auth/logout", ctx -> {
            LogoutReq req = ctx.bodyAsClass(LogoutReq.class);
            if (req.token == null) {
                ctx.json(ApiResponse.error("token required")); return;
            }
            PlayerRepository.revokeToken(req.token);
            ctx.json(ApiResponse.ok("logged out"));
        });

        // ── Send 2FA (internal, no code in response) ─────
        app.post("/api/v1/auth/send-2fa", ctx -> {
            Send2faReq req = ctx.bodyAsClass(Send2faReq.class);
            Player pl = PlayerRepository.findByUuid(req.uuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("player not found")); return;
            }
            if (pl.telegramId == null) {
                ctx.json(ApiResponse.error("no telegram linked")); return;
            }
            String code = PlayerRepository.create2faCode(pl.uuid, ctx.ip());
            PlayerRepository.log(pl.uuid, "2fa_external", ctx.ip(), "2fa sent externally");
            send2faToTelegram(pl.telegramId, code);
            ctx.json(ApiResponse.ok(Map.of("sent", true)));
        });


    }

    private static void checkRateLimit(String key, String prefix, int max, CoreApplication.Config cfg) {
        long now = System.currentTimeMillis();
        String fullKey = prefix + ":" + key;
        RateBucket bucket = loginBuckets.computeIfAbsent(fullKey, k -> new RateBucket(now));
        synchronized (bucket) {
            if (now - bucket.windowStart > 60_000) { bucket.windowStart = now; bucket.count = 0; }
            bucket.count++;
            if (bucket.count > max) {
                throw new io.javalin.http.TooManyRequestsResponse("rate limit exceeded");
            }
        }
    }

    private static String generateSessionToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return UUID.randomUUID().toString().replace("-", "") + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0]; String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    private static final String TG_COMMANDS_DIR = "C:/Users/maska/OneDrive/Desktop/PWP/bots/commands";

    private static void send2faToTelegram(Long telegramId, String code) {
        if (telegramId == null) return;
        try {
            java.io.File dir = new java.io.File(TG_COMMANDS_DIR);
            dir.mkdirs();
            String fileName = "tg_2fa_" + System.currentTimeMillis() + ".json";
            String json = String.format(
                "{\"action\":\"send\",\"chat_id\":\"%d\",\"text\":\"🔐 <b>Код подтверждения</b>\\n\\nВаш код: <code>%s</code>\\n\\nДействителен 5 минут.\"}",
                telegramId, code);
            java.nio.file.Files.writeString(new java.io.File(dir, fileName).toPath(), json,
                    java.nio.charset.StandardCharsets.UTF_8);
            log.info("2FA sent to tg {}", telegramId);
        } catch (Exception e) {
            log.warn("Failed to send 2FA: {}", e.getMessage());
        }
    }

    private static class RateBucket { long windowStart; int count; RateBucket(long n) { windowStart = n; } }

    public static class RegisterReq { public String login, email, password; public long telegramId; }
    public static class LoginReq { public String login, password; }
    public static class ReqUuid { public String uuid; }
    public static class Verify2faReq { public String uuid; public String code; }
    public static class ConfirmLoginReq { public long telegramId; public String ip; }
    public static class ChangePasswordReq { public String uuid; public String oldPassword, newPassword; }
    public static class SetPasswordReq { public String uuid; public String newPassword; }
    public static class Toggle2faReq { public String uuid; public boolean enabled; }
    public static class Send2faReq { public String uuid; }
    public static class LinkTelegramReq { public String login, password; public long telegramId; }
    public static class VerifyTokenReq { public String token; }
    public static class LogoutReq { public String token; }
    public static class UnlinkTelegramReq { public String uuid; public long telegramId; }
    public static class RefreshReq { public String refreshToken; }
    public static class HeartbeatReq { public String accessToken; public String hwid; }
    public static class RevokeReq { public String accessToken; }
}

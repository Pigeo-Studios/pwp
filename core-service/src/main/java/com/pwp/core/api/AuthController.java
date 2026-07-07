package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.AccountRepository;
import com.pwp.core.model.Account;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.LoginSession;
import com.pwp.core.model.PasswordReset;
import com.pwp.core.model.TwoFaCode;
import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthController(Javalin app, CoreApplication.Config config) {
        String adminIds = config.api.adminTelegramIds;

        // ── Check availability ────────────────────────────
        app.get("/api/v1/auth/check-login", ctx -> {
            String login = ctx.queryParam("login");
            boolean taken = login != null && AccountRepository.findByLogin(login) != null;
            ctx.json(ApiResponse.ok(Map.of("available", !taken)));
        });

        app.get("/api/v1/auth/check-email", ctx -> {
            String email = ctx.queryParam("email");
            boolean taken = email != null && AccountRepository.findByEmail(email) != null;
            ctx.json(ApiResponse.ok(Map.of("available", !taken)));
        });

        // ── Register ──────────────────────────────────────
        app.post("/api/v1/auth/register", ctx -> {
            RegisterReq req = ctx.bodyAsClass(RegisterReq.class);
            if (req.login == null || req.email == null || req.password == null) {
                ctx.json(ApiResponse.error("login, email and password are required"));
                return;
            }
            if (req.login.length() < 3 || req.login.length() > 32) {
                ctx.json(ApiResponse.error("login must be 3-32 characters"));
                return;
            }
            if (req.password.length() < 8) {
                ctx.json(ApiResponse.error("password must be at least 8 characters"));
                return;
            }
            if (req.telegramId == 0) {
                ctx.json(ApiResponse.error("telegram_id is required"));
                return;
            }

            // Check existing
            if (AccountRepository.findByLogin(req.login) != null) {
                ctx.json(ApiResponse.error("login already taken"));
                return;
            }
            if (AccountRepository.findByEmail(req.email) != null) {
                ctx.json(ApiResponse.error("email already registered"));
                return;
            }
            if (AccountRepository.findByTelegramId(req.telegramId) != null) {
                ctx.json(ApiResponse.error("telegram account already registered"));
                return;
            }

            String hash = BCrypt.hashpw(req.password, BCrypt.gensalt(12));
            Account acc = AccountRepository.create(req.login, req.email, hash, req.telegramId);
            if (acc == null) {
                ctx.json(ApiResponse.error("registration failed"));
                return;
            }

            AccountRepository.log(acc.id, "register", ctx.ip(), "registered via telegram");
            ctx.json(ApiResponse.ok(Map.of(
                "id", acc.id,
                "uuid", acc.uuid,
                "login", acc.login
            )));
        });

        // ── Accept Privacy Policy ─────────────────────────
        app.post("/api/v1/auth/accept-privacy", ctx -> {
            ReqId req = ctx.bodyAsClass(ReqId.class);
            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }
            AccountRepository.acceptPrivacy(req.accountId);
            AccountRepository.log(req.accountId, "accept_privacy", ctx.ip(), null);
            ctx.json(ApiResponse.ok("ok"));
        });

        // ── Login ─────────────────────────────────────────
        app.post("/api/v1/auth/login", ctx -> {
            LoginReq req = ctx.bodyAsClass(LoginReq.class);
            if (req.login == null || req.password == null) {
                ctx.json(ApiResponse.error("login and password required"));
                return;
            }

            Account acc = AccountRepository.findByLogin(req.login);
            if (acc == null) {
                ctx.json(ApiResponse.error("invalid login or password"));
                return;
            }
            if (acc.isBanned) {
                ctx.json(ApiResponse.error("account is banned: " + (acc.banReason != null ? acc.banReason : "no reason")));
                return;
            }
            if (!BCrypt.checkpw(req.password, acc.passwordHash)) {
                ctx.json(ApiResponse.error("invalid login or password"));
                return;
            }
            if (!acc.privacyPolicyAccepted) {
                ctx.json(ApiResponse.error("privacy policy not accepted"));
                return;
            }

            String clientIp = ctx.ip();
            boolean needs2fa = acc.launcher2faEnabled && !AccountRepository.isIpTrusted(acc.id, clientIp);

            if (needs2fa) {
                // Send 2FA code to TG
                String code = String.format("%06d", RANDOM.nextInt(1000000));
                AccountRepository.create2faCode(acc.id, code, clientIp);
                AccountRepository.log(acc.id, "2fa_sent", clientIp, "2fa code sent to telegram");
                ctx.json(ApiResponse.ok(Map.of(
                    "2fa_required", true,
                    "account_id", acc.id,
                    "code_length", 6
                )));
            } else {
                String token = generateSessionToken();
                AccountRepository.createSession(acc.id, token, clientIp, 24);
                AccountRepository.trustIp(acc.id, clientIp);
                AccountRepository.updateLastLogin(acc.id, clientIp);
                AccountRepository.log(acc.id, "login", clientIp, "login from ip");
                ctx.json(ApiResponse.ok(Map.of(
                    "token", token,
                    "uuid", acc.uuid,
                    "login", acc.login,
                    "role", acc.role
                )));
            }
        });

        // ── Verify 2FA ────────────────────────────────────
        app.post("/api/v1/auth/verify-2fa", ctx -> {
            Verify2faReq req = ctx.bodyAsClass(Verify2faReq.class);
            if (req.accountId == 0 || req.code == null) {
                ctx.json(ApiResponse.error("account_id and code required"));
                return;
            }

            TwoFaCode code = AccountRepository.findValidCode(req.accountId, req.code);
            if (code == null) {
                ctx.json(ApiResponse.error("invalid or expired code"));
                return;
            }

            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null || acc.isBanned) {
                ctx.json(ApiResponse.error("account not found or banned"));
                return;
            }

            AccountRepository.markCodeUsed(code.id);
            String clientIp = ctx.ip();
            String token = generateSessionToken();
            AccountRepository.createSession(acc.id, token, clientIp, 24);
            AccountRepository.trustIp(acc.id, clientIp);
            AccountRepository.updateLastLogin(acc.id, clientIp);
            AccountRepository.log(acc.id, "login_2fa", clientIp, "login via 2fa");

            ctx.json(ApiResponse.ok(Map.of(
                "token", token,
                "uuid", acc.uuid,
                "login", acc.login,
                "role", acc.role
            )));
        });

        // ── Confirm login from TG (trust IP) ──────────────
        app.post("/api/v1/auth/confirm-login", ctx -> {
            ConfirmLoginReq req = ctx.bodyAsClass(ConfirmLoginReq.class);
            Account acc = AccountRepository.findByTelegramId(req.telegramId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }

            String ip = req.ip != null ? req.ip : ctx.ip();
            AccountRepository.trustIp(acc.id, ip);
            AccountRepository.log(acc.id, "ip_confirmed", ip, "user confirmed IP via telegram");
            ctx.json(ApiResponse.ok("ip trusted"));
        });

        // ── Get profile ───────────────────────────────────
        app.get("/api/v1/auth/profile", ctx -> {
            int accountId = Integer.parseInt(ctx.queryParam("account_id"));
            Account acc = AccountRepository.findById(accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }
            Map<String, Object> pm = new HashMap<>();
            pm.put("uuid", acc.uuid);
            pm.put("login", acc.login);
            pm.put("email", maskEmail(acc.email));
            pm.put("role", acc.role);
            pm.put("registered_at", acc.registeredAt);
            pm.put("last_login", acc.lastLogin);
            pm.put("last_ip", acc.lastIp);
            pm.put("2fa_enabled", acc.launcher2faEnabled);
            pm.put("privacy_accepted", acc.privacyPolicyAccepted);
            ctx.json(ApiResponse.ok(pm));
        });

        app.get("/api/v1/auth/profile-by-tg", ctx -> {
            long tgId = Long.parseLong(ctx.queryParam("telegram_id"));
            Account acc = AccountRepository.findByTelegramId(tgId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }
            Map<String, Object> pm = new HashMap<>();
            pm.put("id", acc.id);
            pm.put("uuid", acc.uuid);
            pm.put("login", acc.login);
            pm.put("email", maskEmail(acc.email));
            pm.put("role", acc.role);
            pm.put("registered_at", acc.registeredAt);
            pm.put("last_login", acc.lastLogin);
            pm.put("last_ip", acc.lastIp);
            pm.put("2fa_enabled", acc.launcher2faEnabled);
            pm.put("privacy_accepted", acc.privacyPolicyAccepted);
            ctx.json(ApiResponse.ok(pm));
        });

        // ── Change password ───────────────────────────────
        app.post("/api/v1/auth/change-password", ctx -> {
            ChangePasswordReq req = ctx.bodyAsClass(ChangePasswordReq.class);
            if (req.accountId == 0 || req.oldPassword == null || req.newPassword == null) {
                ctx.json(ApiResponse.error("account_id, old_password and new_password required"));
                return;
            }
            if (req.newPassword.length() < 8) {
                ctx.json(ApiResponse.error("password must be at least 8 characters"));
                return;
            }

            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }
            if (!BCrypt.checkpw(req.oldPassword, acc.passwordHash)) {
                ctx.json(ApiResponse.error("old password is incorrect"));
                return;
            }

            String newHash = BCrypt.hashpw(req.newPassword, BCrypt.gensalt(12));
            AccountRepository.updatePassword(req.accountId, newHash);
            AccountRepository.log(req.accountId, "change_password", ctx.ip(), "password changed");
            ctx.json(ApiResponse.ok("password changed"));
        });

        // ── Forgot password (request reset) ───────────────
        app.post("/api/v1/auth/forgot-password", ctx -> {
            ReqId req = ctx.bodyAsClass(ReqId.class);
            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }

            // Check existing pending
            for (PasswordReset pr : AccountRepository.findPendingResets()) {
                if (pr.accountId == req.accountId) {
                    ctx.json(ApiResponse.error("already have a pending request"));
                    return;
                }
            }

            PasswordReset pr = AccountRepository.createResetRequest(req.accountId);
            AccountRepository.log(req.accountId, "forgot_password", ctx.ip(), "reset request #" + pr.id);
            ctx.json(ApiResponse.ok(Map.of("reset_id", pr.id, "status", "pending")));
        });

        // ── Set new password after reset approved ─────────
        app.post("/api/v1/auth/set-password-after-reset", ctx -> {
            SetPasswordReq req = ctx.bodyAsClass(SetPasswordReq.class);
            if (req.accountId == 0 || req.newPassword == null) {
                ctx.json(ApiResponse.error("account_id and new_password required"));
                return;
            }
            if (req.newPassword.length() < 8) {
                ctx.json(ApiResponse.error("password must be at least 8 characters"));
                return;
            }

            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }

            String newHash = BCrypt.hashpw(req.newPassword, BCrypt.gensalt(12));
            AccountRepository.updatePassword(req.accountId, newHash);
            AccountRepository.log(req.accountId, "reset_password_set", ctx.ip(), "new password set after reset approval");
            ctx.json(ApiResponse.ok("password updated"));
        });

        // ── Toggle 2FA ────────────────────────────────────
        app.post("/api/v1/auth/toggle-2fa", ctx -> {
            Toggle2faReq req = ctx.bodyAsClass(Toggle2faReq.class);
            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }
            AccountRepository.toggle2fa(req.accountId, req.enabled);
            AccountRepository.log(req.accountId, "toggle_2fa", ctx.ip(), "2fa: " + req.enabled);
            ctx.json(ApiResponse.ok("2fa " + (req.enabled ? "enabled" : "disabled")));
        });

        // ── Validate session (for game servers) ───────────
        app.get("/api/v1/auth/validate-session", ctx -> {
            String token = ctx.queryParam("token");
            if (token == null) {
                ctx.json(ApiResponse.error("token required"));
                return;
            }
            LoginSession session = AccountRepository.findSession(token);
            if (session == null) {
                ctx.json(ApiResponse.error("invalid or expired session"));
                return;
            }
            Account acc = AccountRepository.findById(session.accountId);
            ctx.json(ApiResponse.ok(Map.of(
                "valid", true,
                "account_id", acc.id,
                "uuid", acc.uuid,
                "login", acc.login,
                "role", acc.role,
                "is_banned", acc.isBanned
            )));
        });

        // ── Send 2FA (internal, from launcher -> core) ────
        app.post("/api/v1/auth/send-2fa", ctx -> {
            Send2faReq req = ctx.bodyAsClass(Send2faReq.class);
            Account acc = AccountRepository.findById(req.accountId);
            if (acc == null) {
                ctx.json(ApiResponse.error("account not found"));
                return;
            }
            if (acc.telegramId == null) {
                ctx.json(ApiResponse.error("no telegram linked"));
                return;
            }
            String code = String.format("%06d", RANDOM.nextInt(1000000));
            AccountRepository.create2faCode(acc.id, code, ctx.ip());
            AccountRepository.log(acc.id, "2fa_external", ctx.ip(), "2fa sent externally");
            ctx.json(ApiResponse.ok(Map.of(
                "telegram_id", acc.telegramId,
                "code", code
            )));
        });
    }

    private static String generateSessionToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return UUID.randomUUID().toString().replace("-", "") + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    // ── Request DTOs ─────────────────────────────────────
    public static class RegisterReq {
        public String login, email, password;
        public long telegramId;
    }
    public static class LoginReq {
        public String login, password;
    }
    public static class ReqId {
        public int accountId;
    }
    public static class Verify2faReq {
        public int accountId;
        public String code;
    }
    public static class ConfirmLoginReq {
        public long telegramId;
        public String ip;
    }
    public static class ChangePasswordReq {
        public int accountId;
        public String oldPassword, newPassword;
    }
    public static class SetPasswordReq {
        public int accountId;
        public String newPassword;
    }
    public static class Toggle2faReq {
        public int accountId;
        public boolean enabled;
    }
    public static class Send2faReq {
        public int accountId;
    }
}

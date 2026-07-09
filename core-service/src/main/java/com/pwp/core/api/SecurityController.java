package com.pwp.core.api;

import com.pwp.core.CoreApplication;
import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.Player;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);
    private static final String TG_COMMANDS_DIR = "C:/Users/maska/OneDrive/Desktop/PWP/bots/commands";

    public SecurityController(Javalin app, CoreApplication.Config config) {

        app.post("/api/v1/auth/verify-ip", ctx -> {
            var body = ctx.bodyAsClass(VerifyIpReq.class);
            String uuid = body.uuid;
            String clientIp = body.ip != null ? body.ip : ctx.ip();

            Player pl = PlayerRepository.findByUuid(uuid);
            if (pl == null) {
                ctx.json(ApiResponse.error("Player not found"));
                return;
            }

            // IP matches last known → allow
            if (pl.lastIp != null && pl.lastIp.equals(clientIp)) {
                ctx.json(new VerifyIpResp(true, null, null));
                return;
            }

            // Check ip_blocks
            if (isIpBlocked(clientIp)) {
                ctx.json(new VerifyIpResp(false, "IP blocked for 1 hour", null));
                return;
            }

            // Create pending confirmation
            long confirmId = createConfirmation(uuid, clientIp);

            // Send Telegram inline keyboard
            sendIpConfirmTelegram(pl.telegramId, pl.nickname, clientIp, confirmId);

            ctx.json(new VerifyIpResp(false, "IP confirmation required", confirmId));
        });

        app.get("/api/v1/auth/check-ip-confirm", ctx -> {
            long id = Long.parseLong(ctx.queryParam("id"));
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT status FROM ip_confirmations WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        if ("allowed".equals(status)) {
                            // Update player's lastIp
                            String confirmedUuid = uuidByConfirmId(id);
                            if (confirmedUuid != null) {
                                try (PreparedStatement ps2 = c.prepareStatement(
                                    "SELECT new_ip FROM ip_confirmations WHERE id = ?")) {
                                    ps2.setLong(1, id);
                                    try (ResultSet rs2 = ps2.executeQuery()) {
                                        if (rs2.next()) {
                                            String newIp = rs2.getString("new_ip");
                                            PlayerRepository.updateLastLogin(confirmedUuid, newIp);
                                        }
                                    }
                                }
                            }
                            ctx.json(new VerifyIpResp(true, null, null));
                        } else if ("denied".equals(status)) {
                            ctx.json(new VerifyIpResp(false, "IP denied", null));
                        } else {
                            ctx.json(new VerifyIpResp(false, "pending", id));
                        }
                    } else {
                        ctx.json(ApiResponse.error("Confirmation not found"));
                    }
                }
            } catch (Exception e) {
                log.error("check-ip-confirm error", e);
                ctx.status(500).json(ApiResponse.error(e.getMessage()));
            }
        });

        app.post("/api/v1/auth/confirm-ip", ctx -> {
            var body = ctx.bodyAsClass(ConfirmIpReq.class);
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "UPDATE ip_confirmations SET status = ?, responded_at = NOW() WHERE id = ? AND status = 'pending'")) {
                ps.setString(1, body.action);
                ps.setLong(2, body.confirmId);
                int updated = ps.executeUpdate();

                if (updated > 0 && "denied".equals(body.action)) {
                    try (PreparedStatement ps2 = c.prepareStatement(
                        "SELECT new_ip FROM ip_confirmations WHERE id = ?")) {
                        ps2.setLong(1, body.confirmId);
                        try (ResultSet rs = ps2.executeQuery()) {
                            if (rs.next()) {
                                String ip = rs.getString("new_ip");
                                blockIp(ip);
                            }
                        }
                    }
                }

                ctx.json(ApiResponse.ok("OK"));
            } catch (Exception e) {
                log.error("confirm-ip error", e);
                ctx.status(500).json(ApiResponse.error(e.getMessage()));
            }
        });
    }

    private static boolean isIpBlocked(String ip) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM ip_blocks WHERE ip = ? AND blocked_until > NOW()")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            log.error("isIpBlocked error", e);
            return false;
        }
    }

    private static long createConfirmation(String uuid, String ip) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO ip_confirmations (player_uuid, new_ip) VALUES (?, ?)",
                 PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, uuid);
            ps.setString(2, ip);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("createConfirmation error", e);
        }
        return -1;
    }

    private static void blockIp(String ip) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO ip_blocks (ip, blocked_until) VALUES (?, DATE_ADD(NOW(), INTERVAL 1 HOUR))")) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("blockIp error", e);
        }
    }

    private static String uuidByConfirmId(long confirmId) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT player_uuid FROM ip_confirmations WHERE id = ?")) {
            ps.setLong(1, confirmId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("player_uuid");
            }
        } catch (Exception e) {
            log.error("uuidByConfirmId error", e);
        }
        return null;
    }

    private static void sendIpConfirmTelegram(Long telegramId, String nickname, String ip, long confirmId) {
        if (telegramId == null) return;
        try {
            File dir = new File(TG_COMMANDS_DIR);
            dir.mkdirs();
            String fileName = "tg_ipconfirm_" + System.currentTimeMillis() + ".json";

            String text = String.format(
                "\u26A0\uFE0F <b>Вход с нового IP</b>\n\n" +
                "Ваш аккаунт <b>%s</b> заходит с нового IP:\n" +
                "\uD83D\uDCCD <code>%s</code>\n\n" +
                "Это вы?", nickname, ip);

            String json = String.format(
                "{\"action\":\"send_keyboard\",\"chat_id\":\"%d\",\"text\":\"%s\"," +
                "\"keyboard\":[[{\"text\":\"\u2705 Разрешить\",\"callback_data\":\"ip_confirm_%d_allow\"}," +
                "{\"text\":\"\u274C Запретить на час\",\"callback_data\":\"ip_confirm_%d_deny\"}]]}",
                telegramId, text.replace("\"", "\\\""), confirmId, confirmId);

            Files.writeString(new File(dir, fileName).toPath(), json, StandardCharsets.UTF_8);
            log.info("IP confirm sent to tg {} (confirmId={})", telegramId, confirmId);
        } catch (Exception e) {
            log.warn("Failed to send IP confirm: {}", e.getMessage());
        }
    }

    public static class VerifyIpReq { public String uuid; public String ip; }
    public static class ConfirmIpReq { public long confirmId; public String action; }
    public static class VerifyIpResp {
        public boolean success; public String error; public Long confirmId;
        public VerifyIpResp(boolean success, String error, Long confirmId) {
            this.success = success; this.error = error; this.confirmId = confirmId;
        }
    }
}

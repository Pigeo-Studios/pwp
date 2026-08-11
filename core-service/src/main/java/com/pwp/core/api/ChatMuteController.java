package com.pwp.core.api;

import com.pwp.core.db.ChatMuteRepository;
import com.pwp.core.db.ChatMuteRepository.ChatMuteData;
import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerId;
import com.pwp.core.db.PunishmentRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

import java.sql.Connection;
import java.util.List;

public class ChatMuteController {

    public ChatMuteController(Javalin app) {
        // ── Текстовый мут (чат) ─────────────────────────────
        app.post("/api/v1/chatmute", ctx -> {
            MuteRequest req = ctx.bodyAsClass(MuteRequest.class);
            if (req.mutedByUuid == null || req.mutedByNickname == null || req.target == null) {
                ctx.json(ApiResponse.error("target, mutedByUuid and mutedByNickname are required"));
                return;
            }
            try (Connection c = DatabaseManager.getConnection()) {
                String uuid = PlayerId.resolve(c, req.target);
                if (uuid == null) {
                    ctx.json(ApiResponse.error("player not found"));
                    return;
                }
                long expiresAt = req.durationMinutes > 0
                        ? System.currentTimeMillis() + req.durationMinutes * 60000L
                        : 0L;
                ChatMuteRepository.setMute(uuid, req.mutedByUuid, req.mutedByNickname,
                        req.reason, expiresAt);
                try {
                    Integer durationMin = req.durationMinutes > 0 ? req.durationMinutes : null;
                    java.sql.Timestamp expiresTs = expiresAt > 0
                            ? new java.sql.Timestamp(expiresAt) : null;
                    PunishmentRepository.addRecord(uuid, "CHAT_MUTE", req.reason,
                            req.mutedByUuid, durationMin, expiresTs);
                } catch (Exception ignored) {}
                ctx.json(ApiResponse.ok("muted"));
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });

        app.post("/api/v1/chatunmute", ctx -> {
            UnmuteRequest req = ctx.bodyAsClass(UnmuteRequest.class);
            if (req.target == null) {
                ctx.json(ApiResponse.error("target is required"));
                return;
            }
            try (Connection c = DatabaseManager.getConnection()) {
                String uuid = PlayerId.resolve(c, req.target);
                if (uuid == null) {
                    ctx.json(ApiResponse.error("player not found"));
                    return;
                }
                ChatMuteRepository.removeMute(uuid);
                try {
                    PunishmentRepository.addRecord(uuid, "CHAT_UNMUTE", null,
                            req.unmutedByUuid, null, null);
                } catch (Exception ignored) {}
                ctx.json(ApiResponse.ok("unmuted"));
            } catch (Exception e) {
                ctx.json(ApiResponse.error(e.getMessage()));
            }
        });

        app.get("/api/v1/chatmute/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            ChatMuteData data = ChatMuteRepository.findByUuid(uuid);
            if (data == null || !data.isActive()) {
                ctx.json(ApiResponse.ok(new MuteStatusResponse(false, null)));
                return;
            }
            ctx.json(ApiResponse.ok(new MuteStatusResponse(true, data)));
        });

        app.get("/api/v1/chatmutes", ctx -> {
            ChatMuteRepository.cleanupExpired();
            List<ChatMuteData> list = ChatMuteRepository.getAllActive();
            ctx.json(ApiResponse.ok(list));
        });
    }

    public static class MuteRequest {
        public String target;
        public String mutedByUuid;
        public String mutedByNickname;
        public String reason;
        public int durationMinutes;
    }

    public static class UnmuteRequest {
        public String target;
        public String unmutedByUuid;
    }

    public static class MuteStatusResponse {
        public boolean muted;
        public ChatMuteData mute;

        MuteStatusResponse(boolean muted, ChatMuteData mute) {
            this.muted = muted;
            this.mute = mute;
        }
    }
}

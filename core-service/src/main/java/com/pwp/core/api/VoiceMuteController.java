package com.pwp.core.api;

import com.pwp.core.db.VoiceMuteRepository;
import com.pwp.core.db.VoiceMuteRepository.VoiceMuteData;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

import java.util.List;

public class VoiceMuteController {

    public VoiceMuteController(Javalin app) {
        app.post("/api/v1/voicemute", ctx -> {
            MuteRequest req = ctx.bodyAsClass(MuteRequest.class);
            if (req.uuid == null || req.mutedByUuid == null || req.mutedByNickname == null) {
                ctx.json(ApiResponse.error("uuid, mutedByUuid and mutedByNickname are required"));
                return;
            }
            long expiresAt = req.durationMinutes > 0
                    ? System.currentTimeMillis() + req.durationMinutes * 60000L
                    : 0L;
            VoiceMuteRepository.setMute(req.uuid, req.mutedByUuid, req.mutedByNickname,
                    req.reason, expiresAt);
            ctx.json(ApiResponse.ok("muted"));
        });

        app.post("/api/v1/voiceunmute", ctx -> {
            UnmuteRequest req = ctx.bodyAsClass(UnmuteRequest.class);
            if (req.uuid == null) {
                ctx.json(ApiResponse.error("uuid is required"));
                return;
            }
            VoiceMuteRepository.removeMute(req.uuid);
            ctx.json(ApiResponse.ok("unmuted"));
        });

        app.get("/api/v1/voicemute/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            VoiceMuteData data = VoiceMuteRepository.findByUuid(uuid);
            if (data == null || !data.isActive()) {
                ctx.json(ApiResponse.ok(new MuteStatusResponse(false, null)));
                return;
            }
            ctx.json(ApiResponse.ok(new MuteStatusResponse(true, data)));
        });

        app.get("/api/v1/voicemutes", ctx -> {
            VoiceMuteRepository.cleanupExpired();
            List<VoiceMuteData> list = VoiceMuteRepository.getAllActive();
            ctx.json(ApiResponse.ok(list));
        });
    }

    private static class MuteRequest {
        public String uuid;
        public String mutedByUuid;
        public String mutedByNickname;
        public String reason;
        public int durationMinutes;
    }

    private static class UnmuteRequest {
        public String uuid;
    }

    private static class MuteStatusResponse {
        public boolean muted;
        public VoiceMuteData mute;

        MuteStatusResponse(boolean muted, VoiceMuteData mute) {
            this.muted = muted;
            this.mute = mute;
        }
    }
}

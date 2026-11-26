package com.pwp.core.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.core.auth.LinkCodes;
import com.pwp.core.backup.BackupService;
import com.pwp.core.db.CommunityRepository;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.db.PunishmentRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.Player;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Публичный сервер, серверная сторона (этап 10 ROADMAP). Писать могут только игровые серверы и боты по
 * API-ключу ({@code AccessPolicy.SERVER_ONLY}); {@code /api/v1/admin/*} проверяет общий {@code verifyAdmin}.
 *
 * <ul>
 *   <li>{@code POST /api/v1/reports} {server, reporterUuid, reporterName, targetUuid, targetName, reason, details}
 *   <li>{@code GET  /api/v1/admin/reports?status=open|closed|all&after=<id>&limit=} — для F6 и ботов
 *   <li>{@code POST /api/v1/admin/reports/{id}/close} {by}
 *   <li>{@code POST /api/v1/punishments} {uuid, type, reason, adminUuid, minutes} — кик/варн/мут/тимкилл
 *   <li>{@code GET  /api/v1/admin/player/{uuid}/punishments}
 *   <li>{@code POST /api/v1/discord/code} {uuid} → {code, ttlSeconds}; {@code POST /api/v1/discord/link} {code, discordId}
 *   <li>{@code GET  /api/v1/discord/by-discord/{id}}, {@code GET /api/v1/discord/by-uuid/{uuid}}
 *   <li>{@code GET  /api/v1/admin/backup}, {@code POST /api/v1/admin/backup/run}
 * </ul>
 */
public class CommunityController {
    static final Pattern UUID = Pattern.compile("[0-9a-fA-F-]{32,36}");
    static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    static final Pattern DISCORD_ID = Pattern.compile("\\d{5,25}");
    static final Set<String> TYPES = Set.of("KICK", "WARN", "MUTE", "TEAMKILL", "AFK");
    private static final int MAX_BODY = 8 * 1024;

    static final LinkCodes CODES = new LinkCodes(new SecureRandom());

    public CommunityController(Javalin app) {
        app.post("/api/v1/reports", ctx -> {
            CommunityRepository.Report r = parseReport(body(ctx.body()));
            r.createdAt = System.currentTimeMillis();
            r.id = CommunityRepository.addReport(r);
            ctx.json(ApiResponse.ok(Map.of("id", r.id)));
        });

        app.get("/api/v1/admin/reports", ctx -> {
            String st = ctx.queryParam("status");
            String status = st == null || st.isBlank() ? "open" : st.toLowerCase(Locale.ROOT);
            if (!Set.of("open", "closed", "all").contains(status)) throw new BadRequestResponse("bad status");
            long after = parseLong(ctx.queryParam("after"), 0);
            int limit = (int) Math.max(1, Math.min(200, parseLong(ctx.queryParam("limit"), 50)));
            List<Map<String, Object>> out = new ArrayList<>();
            for (CommunityRepository.Report r : CommunityRepository.reports("all".equals(status) ? null : status, after, limit)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.id);
                m.put("server", r.server);
                m.put("reporter", r.reporterName);
                m.put("target", r.targetName);
                m.put("targetUuid", r.targetUuid);
                m.put("reason", r.reason);
                m.put("details", r.details);
                m.put("status", r.status);
                m.put("handledBy", r.handledBy);
                m.put("createdAt", r.createdAt);
                m.put("closedAt", r.closedAt);
                out.add(m);
            }
            ctx.json(ApiResponse.ok(out));
        });

        app.post("/api/v1/admin/reports/{id}/close", ctx -> {
            long id = parseLong(ctx.pathParam("id"), -1);
            if (id <= 0) throw new BadRequestResponse("bad id");
            JsonObject o = body(ctx.body().isBlank() ? "{}" : ctx.body());
            String by = clip(str(o, "by"), 32);
            boolean ok = CommunityRepository.closeReport(id, by.isEmpty() ? "admin" : by, System.currentTimeMillis());
            ctx.json(ok ? ApiResponse.ok("closed") : ApiResponse.error("report not found or already closed"));
        });

        app.post("/api/v1/punishments", ctx -> {
            JsonObject o = body(ctx.body());
            String uuid = uuid(str(o, "uuid"));
            String type = str(o, "type").toUpperCase(Locale.ROOT);
            if (!TYPES.contains(type)) throw new BadRequestResponse("bad type: " + type);
            String admin = str(o, "adminUuid");
            Integer minutes = o.has("minutes") && o.get("minutes").isJsonPrimitive() ? o.get("minutes").getAsInt() : null;
            PlayerRepository.ensurePlayerExists(uuid);
            PunishmentRepository.addRecord(
                    uuid, type, clip(str(o, "reason"), 500), admin.isEmpty() ? null : uuid(admin), minutes, null);
            ctx.json(ApiResponse.ok("recorded"));
        });

        app.get("/api/v1/admin/player/{uuid}/punishments", ctx -> {
            String uuid = uuid(ctx.pathParam("uuid"));
            ctx.json(ApiResponse.ok(CommunityRepository.punishments(uuid, 50)));
        });

        app.post("/api/v1/discord/code", ctx -> {
            String uuid = uuid(str(body(ctx.body()), "uuid"));
            String code = CODES.issue(uuid, System.currentTimeMillis());
            ctx.json(ApiResponse.ok(Map.of("code", code, "ttlSeconds", LinkCodes.TTL_MS / 1000)));
        });

        app.post("/api/v1/discord/link", ctx -> {
            JsonObject o = body(ctx.body());
            String discordId = str(o, "discordId");
            if (!DISCORD_ID.matcher(discordId).matches()) throw new BadRequestResponse("bad discordId");
            String uuid = CODES.redeem(str(o, "code"), System.currentTimeMillis());
            if (uuid == null) {
                ctx.json(ApiResponse.error("code invalid or expired"));
                return;
            }
            CommunityRepository.linkDiscord(uuid, discordId, System.currentTimeMillis());
            ctx.json(ApiResponse.ok(CommunityRepository.findLink("uuid", uuid)));
        });

        app.get("/api/v1/discord/by-discord/{id}", ctx -> {
            String id = ctx.pathParam("id");
            if (!DISCORD_ID.matcher(id).matches()) throw new BadRequestResponse("bad id");
            Map<String, Object> m = CommunityRepository.findLink("discord_id", id);
            ctx.json(m == null ? ApiResponse.error("not linked") : ApiResponse.ok(m));
        });

        app.get("/api/v1/discord/by-uuid/{uuid}", ctx -> {
            Map<String, Object> m = CommunityRepository.findLink("uuid", uuid(ctx.pathParam("uuid")));
            ctx.json(m == null ? ApiResponse.error("not linked") : ApiResponse.ok(m));
        });

        app.get("/api/v1/admin/backup", ctx -> ctx.json(ApiResponse.ok(BackupService.status())));

        app.post("/api/v1/admin/backup/run", ctx -> {
            // в фоне: дамп большой БД — минуты, HTTP-запрос не держим
            Thread t = new Thread(BackupService::runNow, "PWP-Backup-Manual");
            t.setDaemon(true);
            t.start();
            ctx.json(ApiResponse.ok("backup started"));
        });
    }

    static CommunityRepository.Report parseReport(JsonObject o) {
        CommunityRepository.Report r = new CommunityRepository.Report();
        r.server = clip(str(o, "server"), 64);
        r.reporterUuid = uuid(str(o, "reporterUuid"));
        r.reporterName = name(str(o, "reporterName"));
        r.targetUuid = uuid(str(o, "targetUuid"));
        r.targetName = name(str(o, "targetName"));
        r.reason = str(o, "reason").trim().toLowerCase(Locale.ROOT);
        if (!Pattern.matches("[a-z_]{1,24}", r.reason)) throw new BadRequestResponse("bad reason");
        r.details = clip(str(o, "details"), 300);
        if (r.reporterUuid.equalsIgnoreCase(r.targetUuid)) throw new BadRequestResponse("self report");
        return r;
    }

    private static JsonObject body(String raw) {
        if (raw == null || raw.length() > MAX_BODY) throw new BadRequestResponse("body too large");
        JsonElement e;
        try {
            e = JsonParser.parseString(raw);
        } catch (RuntimeException ex) {
            throw new BadRequestResponse("bad json");
        }
        if (!e.isJsonObject()) throw new BadRequestResponse("body must be an object");
        return e.getAsJsonObject();
    }

    static String uuid(String s) {
        if (s == null || !UUID.matcher(s).matches()) throw new BadRequestResponse("bad uuid");
        return s;
    }

    static String name(String s) {
        if (s == null || !NAME.matcher(s).matches()) throw new BadRequestResponse("bad name");
        return s;
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    static String clip(String s, int max) {
        String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}]", " ").trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static long parseLong(String s, long def) {
        if (s == null) return def;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Ник для публичных страниц: существующий игрок или null. */
    static Player byNickname(String nick) throws java.sql.SQLException {
        return NAME.matcher(nick).matches() ? PlayerRepository.findByNickname(nick) : null;
    }
}

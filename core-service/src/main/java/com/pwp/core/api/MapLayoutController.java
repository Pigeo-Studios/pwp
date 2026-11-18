package com.pwp.core.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pwp.core.db.MapLayoutRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Версии раскладки карты из GUI-редактора (этап 3). Пишет только игровой сервер (Bearer-ключ сервера,
 * AuthMiddleware); право OWNER проверяет игровой сервер. Версии только добавляются: откат = новая копия.
 *
 * <ul>
 *   <li>{@code GET  /api/v1/maps/{map}/layouts} — список (без json), новые первыми, до 50</li>
 *   <li>{@code GET  /api/v1/maps/{map}/layouts/latest} — последняя (с json) или data=null</li>
 *   <li>{@code GET  /api/v1/maps/layouts/{id}} — версия по id (с json)</li>
 *   <li>{@code POST /api/v1/maps/{map}/layouts} {json, authorUuid, authorName, comment} — новая версия</li>
 *   <li>{@code POST /api/v1/maps/layouts/{id}/rollback} {authorUuid, authorName} — копия старой версии</li>
 * </ul>
 */
public class MapLayoutController {
    private static final Pattern MAP_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F-]{32,36}");
    private static final int MAX_JSON_BYTES = 1 << 20;
    private static final int LIST_LIMIT = 50;

    public MapLayoutController(Javalin app) {
        app.get("/api/v1/maps/{map}/layouts", ctx -> {
            ctx.json(ApiResponse.ok(MapLayoutRepository.list(mapName(ctx.pathParam("map")), LIST_LIMIT)));
        });

        app.get("/api/v1/maps/{map}/layouts/latest", ctx -> {
            ctx.json(ApiResponse.ok(MapLayoutRepository.latest(mapName(ctx.pathParam("map")))));
        });

        app.get("/api/v1/maps/layouts/{id}", ctx -> {
            MapLayoutRepository.Version v = MapLayoutRepository.byId(id(ctx.pathParam("id")));
            if (v == null) throw new NotFoundResponse();
            ctx.json(ApiResponse.ok(v));
        });

        app.post("/api/v1/maps/{map}/layouts", ctx -> {
            String map = mapName(ctx.pathParam("map"));
            SaveReq req = ctx.bodyAsClass(SaveReq.class);
            if (req == null) throw new BadRequestResponse("body required");
            String json = layoutJson(req.json);
            ctx.json(ApiResponse.ok(MapLayoutRepository.insert(
                    map, json, uuid(req.authorUuid), name(req.authorName), comment(req.comment))));
        });

        app.post("/api/v1/maps/layouts/{id}/rollback", ctx -> {
            MapLayoutRepository.Version old = MapLayoutRepository.byId(id(ctx.pathParam("id")));
            if (old == null) throw new NotFoundResponse();
            RollbackReq req = ctx.bodyAsClass(RollbackReq.class);
            if (req == null) throw new BadRequestResponse("body required");
            ctx.json(ApiResponse.ok(MapLayoutRepository.insert(
                    old.mapName, old.json, uuid(req.authorUuid), name(req.authorName), "rollback to #" + old.id)));
        });
    }

    private static String mapName(String s) {
        if (s == null || !MAP_NAME.matcher(s).matches()) throw new BadRequestResponse("bad map name");
        return s;
    }

    private static long id(String s) {
        try {
            long v = Long.parseLong(s);
            if (v <= 0) throw new BadRequestResponse("bad id");
            return v;
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("bad id");
        }
    }

    private static String uuid(String s) {
        if (s == null || !UUID.matcher(s).matches()) throw new BadRequestResponse("bad author uuid");
        return s;
    }

    private static String name(String s) {
        if (s == null || s.isBlank() || s.length() > 32) throw new BadRequestResponse("bad author name");
        return s;
    }

    private static String comment(String s) {
        if (s == null) return "";
        String t = s.strip();
        return t.length() > 200 ? t.substring(0, 200) : t;
    }

    /** JSON раскладки: объект, не больше 1 МБ. Содержимое проверяет игровой сервер (MapLayoutCodec). */
    private static String layoutJson(String s) {
        if (s == null || s.isEmpty()) throw new BadRequestResponse("json required");
        if (s.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) throw new BadRequestResponse("json too large");
        JsonElement el;
        try {
            el = JsonParser.parseString(s);
        } catch (RuntimeException e) {
            throw new BadRequestResponse("json is not valid");
        }
        if (!el.isJsonObject()) throw new BadRequestResponse("json must be an object");
        return s;
    }

    private static class SaveReq {
        public String json;
        public String authorUuid;
        public String authorName;
        public String comment;
    }

    private static class RollbackReq {
        public String authorUuid;
        public String authorName;
    }
}

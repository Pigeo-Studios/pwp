package com.pwp.core.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.core.db.DivisionRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Дивизии (составы) фракций — этап 6 ROADMAP. Читают игровые серверы (FactionDataLoader) и админка; пишут
 * только игровые серверы по API-ключу ({@code AccessPolicy.SERVER_ONLY}), право ADMIN проверяет сервер.
 *
 * <ul>
 *   <li>{@code GET    /api/v1/factions/{faction}/divisions} — все дивизии фракции</li>
 *   <li>{@code PUT    /api/v1/factions/{faction}/divisions/{id}} {displayName, modes, mapSizes, vehicles[]}</li>
 *   <li>{@code DELETE /api/v1/factions/{faction}/divisions/{id}}</li>
 * </ul>
 * Формат записи — как {@code Division.toJson} в pwp-logic: {@code faction, divisionId, displayName, modes (CSV),
 * mapSizes (CSV), vehicles: [{role, vehicleName, count, respawnSeconds}]}.
 */
public class DivisionController {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_]{1,32}");
    private static final Set<String> SIZES = Set.of("SMALL", "MEDIUM", "LARGE");
    private static final int MAX_BODY = 64 * 1024;
    private static final int MAX_ENTRIES = 64;

    public DivisionController(Javalin app) {
        app.get("/api/v1/factions/{faction}/divisions", ctx -> {
            String faction = id(ctx.pathParam("faction")).toLowerCase(Locale.ROOT);
            JsonArray arr = new JsonArray();
            for (DivisionRepository.Row r : DivisionRepository.byFaction(faction)) arr.add(toJson(r));
            ctx.json(ApiResponse.ok(arr));
        });

        app.put("/api/v1/factions/{faction}/divisions/{id}", ctx -> {
            if (ctx.body().getBytes(StandardCharsets.UTF_8).length > MAX_BODY) {
                throw new BadRequestResponse("body too large");
            }
            DivisionRepository.Row r = parse(
                    ctx.pathParam("faction"), ctx.pathParam("id"), JsonParser.parseString(ctx.body()));
            r.updatedAt = System.currentTimeMillis();
            DivisionRepository.save(r);
            ctx.json(ApiResponse.ok(toJson(r)));
        });

        app.delete("/api/v1/factions/{faction}/divisions/{id}", ctx -> {
            boolean removed = DivisionRepository.delete(
                    id(ctx.pathParam("faction")).toLowerCase(Locale.ROOT), id(ctx.pathParam("id")));
            ctx.json(removed ? ApiResponse.ok("division deleted") : ApiResponse.error("division not found"));
        });
    }

    /** Проверка и нормализация тела PUT. Исключение {@link BadRequestResponse} — с причиной для админа. */
    static DivisionRepository.Row parse(String faction, String divisionId, JsonElement body) {
        if (body == null || !body.isJsonObject()) throw new BadRequestResponse("body must be an object");
        JsonObject o = body.getAsJsonObject();
        DivisionRepository.Row r = new DivisionRepository.Row();
        r.faction = id(faction).toLowerCase(Locale.ROOT);
        r.divisionId = id(divisionId);
        String dn = str(o, "displayName").trim();
        r.displayName = dn.isEmpty() ? r.divisionId : dn.substring(0, Math.min(dn.length(), 128));
        r.modes = csv(str(o, "modes"), null);
        r.mapSizes = csv(str(o, "mapSizes"), SIZES);
        JsonElement ve = o.get("vehicles");
        if (ve == null || !ve.isJsonArray()) throw new BadRequestResponse("vehicles must be an array");
        JsonArray in = ve.getAsJsonArray();
        if (in.size() > MAX_ENTRIES) throw new BadRequestResponse("too many vehicles");
        JsonArray out = new JsonArray();
        for (JsonElement el : in) {
            if (!el.isJsonObject()) throw new BadRequestResponse("vehicle entry must be an object");
            JsonObject v = el.getAsJsonObject();
            String role = str(v, "role").trim().toUpperCase(Locale.ROOT);
            String name = str(v, "vehicleName").trim();
            if (!TOKEN.matcher(role).matches()) throw new BadRequestResponse("bad role: " + role);
            if (!ID.matcher(name).matches()) throw new BadRequestResponse("bad vehicleName: " + name);
            JsonObject n = new JsonObject();
            n.addProperty("role", role);
            n.addProperty("vehicleName", name);
            n.addProperty("count", clamp(num(v, "count", 1), 0, 20));
            n.addProperty("respawnSeconds", clamp(num(v, "respawnSeconds", 0), 0, 3600));
            out.add(n);
        }
        r.vehicles = out.toString();
        return r;
    }

    static JsonObject toJson(DivisionRepository.Row r) {
        JsonObject o = new JsonObject();
        o.addProperty("faction", r.faction);
        o.addProperty("divisionId", r.divisionId);
        o.addProperty("displayName", r.displayName);
        o.addProperty("modes", r.modes);
        o.addProperty("mapSizes", r.mapSizes);
        JsonElement v;
        try {
            v = JsonParser.parseString(r.vehicles == null ? "[]" : r.vehicles);
        } catch (RuntimeException e) {
            v = new JsonArray();
        }
        o.add("vehicles", v.isJsonArray() ? v : new JsonArray());
        o.addProperty("updatedAt", r.updatedAt);
        return o;
    }

    private static String id(String s) {
        if (s == null || !ID.matcher(s).matches()) throw new BadRequestResponse("bad id");
        return s;
    }

    /** CSV токенов в верхнем регистре; {@code allowed} != null — только из набора. */
    private static String csv(String raw, Set<String> allowed) {
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String t = part.trim().toUpperCase(Locale.ROOT);
            if (t.isEmpty()) continue;
            if (!TOKEN.matcher(t).matches() || (allowed != null && !allowed.contains(t))) {
                throw new BadRequestResponse("bad value: " + t);
            }
            out.add(t);
        }
        return String.join(",", List.copyOf(out));
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    private static int num(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : def;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

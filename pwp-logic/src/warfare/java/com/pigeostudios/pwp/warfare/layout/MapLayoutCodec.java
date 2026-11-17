package com.pigeostudios.pwp.warfare.layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.pigeostudios.pwp.warfare.layout.MapLayout.Border;
import com.pigeostudios.pwp.warfare.layout.MapLayout.CapturePointDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.MainZoneDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.TeamSpawn;
import com.pigeostudios.pwp.warfare.layout.MapLayout.VehicleSlot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * map_config.json ↔ {@link MapLayout}. JsonObject вручную (не Gson-POJO — ProGuard-keep не нужен). Чтение
 * снисходительное: битый элемент пропускается с {@link Issue.Code#PARSE_ELEMENT}, остальное читается. Запись
 * накладывает раскладку на копию исходного корня — ключи вне раскладки и неизвестные ключи элементов
 * сохраняются.
 *
 * <p>Ключи (как в прежнем парсере {@code WarfareMod.applyMapConfig}): {@code mode}, {@code capturePoints[]} или
 * {@code capturePointPatterns[capturePointPatternIndex].points[]}, {@code mainZones[]}, {@code
 * teams.{BLUE,RED}.spawn}, {@code vehicleSpawners.{BLUE,RED}[]}, {@code mapBounds}.
 */
public final class MapLayoutCodec {

    private static final Set<String> POINT_KEYS = Set.of(
            "id",
            "name",
            "shape",
            "pos1",
            "pos2",
            "bluePriority",
            "redPriority",
            "captureTimeMinutes",
            "ticketPenalty",
            "captureDeduction",
            "lockDurationMinutes",
            "gainNeutralizeTickets",
            "gainCaptureTickets");
    private static final Set<String> ZONE_KEYS = Set.of("team", "shape", "pos1", "pos2");
    private static final Set<String> SPAWN_KEYS = Set.of("dimension", "x", "y", "z");
    private static final Set<String> SLOT_KEYS = Set.of("role", "vehicleName", "x", "y", "z", "yaw", "fixed");
    private static final Set<String> BORDER_KEYS = Set.of("centerX", "centerZ", "sizeBlocks");

    private MapLayoutCodec() {}

    /** Результат чтения: раскладка + замечания разбора. */
    public record Parsed(MapLayout layout, List<Issue> issues) {
        public Parsed {
            issues = List.copyOf(issues);
        }
    }

    public static Parsed read(String json) {
        JsonElement el = JsonParser.parseString(json);
        if (!el.isJsonObject()) throw new IllegalArgumentException("map_config root is not an object");
        return read(el.getAsJsonObject());
    }

    public static Parsed read(JsonObject root) {
        List<Issue> issues = new ArrayList<>();
        String mode = str(root, "mode");

        // Точки: выбранный паттерн (если индекс валиден), иначе capturePoints — как прежний парсер
        List<CapturePointDef> points = new ArrayList<>();
        int patternIndex = -1;
        JsonArray patterns = arr(root, "capturePointPatterns");
        Integer idx = intOrNull(root, "capturePointPatternIndex");
        JsonArray pointArr = null;
        String pointPath = "capturePoints";
        if (patterns != null
                && idx != null
                && idx >= 0
                && idx < patterns.size()
                && patterns.get(idx).isJsonObject()) {
            patternIndex = idx;
            pointArr = arr(patterns.get(idx).getAsJsonObject(), "points");
            pointPath = "capturePointPatterns[" + idx + "].points";
        }
        if (pointArr == null && patternIndex < 0) pointArr = arr(root, "capturePoints");
        if (pointArr != null) {
            for (int i = 0; i < pointArr.size(); i++) {
                try {
                    points.add(readPoint(obj(pointArr.get(i))));
                } catch (RuntimeException e) {
                    issues.add(Issue.of(Issue.Code.PARSE_ELEMENT, pointPath + "[" + i + "]", e.getMessage()));
                }
            }
        }

        List<MainZoneDef> zones = new ArrayList<>();
        JsonArray zoneArr = arr(root, "mainZones");
        if (zoneArr != null) {
            for (int i = 0; i < zoneArr.size(); i++) {
                try {
                    JsonObject z = obj(zoneArr.get(i));
                    zones.add(new MainZoneDef(reqStr(z, "team"), str(z, "shape"), readBox(z), extra(z, ZONE_KEYS)));
                } catch (RuntimeException e) {
                    issues.add(Issue.of(Issue.Code.PARSE_ELEMENT, "mainZones[" + i + "]", e.getMessage()));
                }
            }
        }

        Map<Team, TeamSpawn> spawns = new EnumMap<>(Team.class);
        JsonObject teams = objOrNull(root, "teams");
        if (teams != null) {
            for (Team t : Team.values()) {
                JsonObject team = objOrNull(teams, t.name());
                JsonObject sp = team == null ? null : objOrNull(team, "spawn");
                if (sp == null) continue;
                try {
                    spawns.put(
                            t,
                            new TeamSpawn(
                                    str(sp, "dimension"),
                                    reqInt(sp, "x"),
                                    intOrNull(sp, "y"),
                                    reqInt(sp, "z"),
                                    extra(sp, SPAWN_KEYS)));
                } catch (RuntimeException e) {
                    issues.add(Issue.of(Issue.Code.PARSE_ELEMENT, "teams." + t + ".spawn", e.getMessage()));
                }
            }
        }

        List<VehicleSlot> slots = new ArrayList<>();
        JsonObject vs = objOrNull(root, "vehicleSpawners");
        if (vs != null) {
            for (Team t : Team.values()) {
                JsonArray a = arr(vs, t.name());
                if (a == null) continue;
                for (int i = 0; i < a.size(); i++) {
                    try {
                        JsonObject o = obj(a.get(i));
                        Double yaw = numOrNull(o, "yaw");
                        slots.add(new VehicleSlot(
                                t,
                                str(o, "role"),
                                str(o, "vehicleName"),
                                new Pos(reqInt(o, "x"), reqInt(o, "y"), reqInt(o, "z")),
                                yaw == null ? null : yaw.floatValue(),
                                boolOrNull(o, "fixed"),
                                extra(o, SLOT_KEYS)));
                    } catch (RuntimeException e) {
                        issues.add(Issue.of(
                                Issue.Code.PARSE_ELEMENT, "vehicleSpawners." + t + "[" + i + "]", e.getMessage()));
                    }
                }
            }
        }

        Border border = null;
        JsonObject b = objOrNull(root, "mapBounds");
        if (b != null) {
            border = new Border(
                    intOrNull(b, "centerX"),
                    intOrNull(b, "centerZ"),
                    intOrNull(b, "sizeBlocks"),
                    extra(b, BORDER_KEYS));
        }

        return new Parsed(new MapLayout(root, mode, points, patternIndex, zones, spawns, slots, border), issues);
    }

    /** Раскладка → JSON поверх {@link MapLayout#source()}. Пишется только явное (null-поля — без ключа). */
    public static JsonObject write(MapLayout layout) {
        JsonObject root = layout.source();

        if (layout.mode() != null) root.addProperty("mode", layout.mode());
        else root.remove("mode");

        JsonArray pts = new JsonArray();
        for (CapturePointDef p : layout.points()) pts.add(writePoint(p));
        JsonArray patterns = arr(root, "capturePointPatterns");
        if (layout.patternIndex() >= 0 && patterns != null && layout.patternIndex() < patterns.size()) {
            patterns.get(layout.patternIndex()).getAsJsonObject().add("points", pts);
        } else if (!pts.isEmpty() || root.has("capturePoints")) {
            root.add("capturePoints", pts);
        }

        if (!layout.mainZones().isEmpty() || root.has("mainZones")) {
            JsonArray zones = new JsonArray();
            for (MainZoneDef z : layout.mainZones()) {
                JsonObject o = new JsonObject();
                o.addProperty("team", z.team());
                if (z.shape() != null) o.addProperty("shape", z.shape());
                writeBox(o, z.box());
                merge(o, z.extra());
                zones.add(o);
            }
            root.add("mainZones", zones);
        }

        for (Team t : Team.values()) {
            TeamSpawn s = layout.spawn(t);
            JsonObject teams = objOrNull(root, "teams");
            JsonObject team = teams == null ? null : objOrNull(teams, t.name());
            if (s == null) {
                if (team != null) team.remove("spawn");
                continue;
            }
            if (teams == null) {
                teams = new JsonObject();
                root.add("teams", teams);
            }
            if (team == null) {
                team = new JsonObject();
                teams.add(t.name(), team);
            }
            JsonObject o = new JsonObject();
            if (s.dimension() != null) o.addProperty("dimension", s.dimension());
            o.addProperty("x", s.x());
            if (s.y() != null) o.addProperty("y", s.y());
            o.addProperty("z", s.z());
            merge(o, s.extra());
            team.add("spawn", o);
        }

        JsonObject vs = objOrNull(root, "vehicleSpawners");
        if (!layout.vehicleSlots().isEmpty() || vs != null) {
            if (vs == null) {
                vs = new JsonObject();
                root.add("vehicleSpawners", vs);
            }
            for (Team t : Team.values()) {
                List<VehicleSlot> list = layout.slotsOf(t);
                if (list.isEmpty() && !vs.has(t.name())) continue;
                JsonArray a = new JsonArray();
                for (VehicleSlot v : list) {
                    JsonObject o = new JsonObject();
                    if (v.role() != null) o.addProperty("role", v.role());
                    if (v.vehicleName() != null) o.addProperty("vehicleName", v.vehicleName());
                    o.addProperty("x", v.pos().x());
                    o.addProperty("y", v.pos().y());
                    o.addProperty("z", v.pos().z());
                    if (v.yaw() != null) o.add("yaw", number(v.yaw()));
                    if (v.fixed() != null) o.addProperty("fixed", v.fixed());
                    merge(o, v.extra());
                    a.add(o);
                }
                vs.add(t.name(), a);
            }
        }

        Border br = layout.border();
        if (br == null) {
            root.remove("mapBounds");
        } else {
            JsonObject o = new JsonObject();
            if (br.centerX() != null) o.addProperty("centerX", br.centerX());
            if (br.centerZ() != null) o.addProperty("centerZ", br.centerZ());
            if (br.sizeBlocks() != null) o.addProperty("sizeBlocks", br.sizeBlocks());
            merge(o, br.extra());
            root.add("mapBounds", o);
        }
        return root;
    }

    // --- элементы ---

    private static CapturePointDef readPoint(JsonObject cp) {
        return new CapturePointDef(
                str(cp, "id"),
                reqStr(cp, "name"),
                str(cp, "shape"),
                readBox(cp),
                intOrNull(cp, "bluePriority"),
                intOrNull(cp, "redPriority"),
                intOrNull(cp, "captureTimeMinutes"),
                intOrNull(cp, "ticketPenalty"),
                intOrNull(cp, "captureDeduction"),
                intOrNull(cp, "lockDurationMinutes"),
                intOrNull(cp, "gainNeutralizeTickets"),
                intOrNull(cp, "gainCaptureTickets"),
                extra(cp, POINT_KEYS));
    }

    private static JsonObject writePoint(CapturePointDef p) {
        JsonObject o = new JsonObject();
        if (p.id() != null) o.addProperty("id", p.id());
        o.addProperty("name", p.name());
        if (p.shape() != null) o.addProperty("shape", p.shape());
        writeBox(o, p.box());
        putInt(o, "bluePriority", p.bluePriority());
        putInt(o, "redPriority", p.redPriority());
        putInt(o, "captureTimeMinutes", p.captureTimeMinutes());
        putInt(o, "ticketPenalty", p.ticketPenalty());
        putInt(o, "captureDeduction", p.captureDeduction());
        putInt(o, "lockDurationMinutes", p.lockDurationMinutes());
        putInt(o, "gainNeutralizeTickets", p.gainNeutralizeTickets());
        putInt(o, "gainCaptureTickets", p.gainCaptureTickets());
        merge(o, p.extra());
        return o;
    }

    private static Box readBox(JsonObject o) {
        return new Box(readPos(o, "pos1"), readPos(o, "pos2"));
    }

    private static Pos readPos(JsonObject o, String key) {
        JsonObject p = objOrNull(o, key);
        if (p == null) throw new IllegalArgumentException("missing " + key);
        return new Pos(reqInt(p, "x"), reqInt(p, "y"), reqInt(p, "z"));
    }

    private static void writeBox(JsonObject o, Box b) {
        o.add("pos1", pos(b.pos1()));
        o.add("pos2", pos(b.pos2()));
    }

    private static JsonObject pos(Pos p) {
        JsonObject o = new JsonObject();
        o.addProperty("x", p.x());
        o.addProperty("y", p.y());
        o.addProperty("z", p.z());
        return o;
    }

    // --- JSON-утилиты (без исключений на отсутствии; требуемое — IllegalArgumentException с ключом) ---

    private static JsonObject extra(JsonObject o, Set<String> known) {
        JsonObject e = new JsonObject();
        for (var en : o.entrySet()) {
            if (!known.contains(en.getKey())) e.add(en.getKey(), en.getValue().deepCopy());
        }
        return e;
    }

    private static void merge(JsonObject into, JsonObject extra) {
        for (var en : extra.entrySet()) {
            if (!into.has(en.getKey())) into.add(en.getKey(), en.getValue());
        }
    }

    private static void putInt(JsonObject o, String k, Integer v) {
        if (v != null) o.addProperty(k, v);
    }

    /** Целое — без «.0» (как в рукописном конфиге). */
    private static JsonPrimitive number(float f) {
        if (f == Math.rint(f) && Math.abs(f) < 1e9) return new JsonPrimitive((int) f);
        return new JsonPrimitive(f);
    }

    private static JsonObject obj(JsonElement e) {
        if (e == null || !e.isJsonObject()) throw new IllegalArgumentException("not an object");
        return e.getAsJsonObject();
    }

    private static JsonObject objOrNull(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static JsonArray arr(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static String reqStr(JsonObject o, String k) {
        String s = str(o, k);
        if (s == null) throw new IllegalArgumentException("missing " + k);
        return s;
    }

    private static Double numOrNull(JsonObject o, String k) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        return e.getAsDouble();
    }

    private static Integer intOrNull(JsonObject o, String k) {
        Double d = numOrNull(o, k);
        // Отбрасывание дробной части — как JsonElement.getAsInt прежнего парсера
        return d == null ? null : d.intValue();
    }

    private static int reqInt(JsonObject o, String k) {
        Integer v = intOrNull(o, k);
        if (v == null) throw new IllegalArgumentException("missing or non-numeric " + k);
        return v;
    }

    private static Boolean boolOrNull(JsonObject o, String k) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) return null;
        return e.getAsBoolean();
    }
}

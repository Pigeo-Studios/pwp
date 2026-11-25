package com.pigeostudios.pwp.warfare.vehicles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Дивизия (состав) фракции — как в Squad: набор техники под режим и размер карты, а не общий пул фракции
 * (этап 6 ROADMAP). Слот техники на карте задаёт роль (TRANSPORT/APC/TANK…), дивизия — какие машины на эту роль
 * и сколько слотов роли занято: {@code count} — сколько машин этой записи; слоты роли сверх суммы — пустые.
 *
 * <p>Хранение: core-service, таблица {@code faction_divisions}; {@code vehicles} — JSON-массив {@link Entry}.
 * Пустые {@code modes}/{@code mapSizes} = подходит для всех.
 */
public record Division(
        String faction, String id, String displayName, Set<String> modes, Set<MapSize> mapSizes, List<Entry> vehicles) {

    /** Машина дивизии: {@code vehicleName} — имя из пула фракции ({@code faction_vehicles}). */
    public record Entry(String role, String vehicleName, int count, int respawnSeconds) {
        public Entry {
            role = norm(role);
            vehicleName = vehicleName == null ? "" : vehicleName.trim();
            count = Math.max(0, Math.min(MAX_COUNT, count));
            respawnSeconds = Math.max(0, Math.min(3600, respawnSeconds));
        }
    }

    /** Размер карты по стороне играбельной зоны (граница MapLayout). */
    public enum MapSize {
        SMALL,
        MEDIUM,
        LARGE;

        /** ≤ 300 блоков — SMALL, ≤ 800 — MEDIUM, больше — LARGE (по большей стороне). */
        public static MapSize of(int widthBlocks, int depthBlocks) {
            int side = Math.max(widthBlocks, depthBlocks);
            if (side <= 300) return SMALL;
            if (side <= 800) return MEDIUM;
            return LARGE;
        }

        static MapSize parse(String s) {
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException e) {
                return null;
            }
        }
    }

    public static final int MAX_COUNT = 20;

    public Division {
        faction = faction == null ? "" : faction.trim().toLowerCase(Locale.ROOT);
        id = id == null ? "" : id.trim();
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        modes = modes == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(modes));
        mapSizes = mapSizes == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(mapSizes));
        vehicles = vehicles == null ? List.of() : List.copyOf(vehicles);
    }

    /** Подходит для режима и размера карты (пустые списки — для всех; null-аргумент не ограничивает). */
    public boolean fits(String mode, MapSize size) {
        boolean modeOk = modes.isEmpty() || mode == null || modes.contains(norm(mode));
        boolean sizeOk = mapSizes.isEmpty() || size == null || mapSizes.contains(size);
        return modeOk && sizeOk;
    }

    static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    // ───────────────────────────── JSON (ответ core-service / файл) ─────────────────────────────

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("faction", faction);
        o.addProperty("divisionId", id);
        o.addProperty("displayName", displayName);
        o.addProperty("modes", String.join(",", modes));
        List<String> sizes = new ArrayList<>();
        for (MapSize s : mapSizes) sizes.add(s.name());
        o.addProperty("mapSizes", String.join(",", sizes));
        o.add("vehicles", vehiclesJson(vehicles));
        return o;
    }

    public static JsonArray vehiclesJson(List<Entry> vehicles) {
        JsonArray arr = new JsonArray();
        for (Entry e : vehicles) {
            JsonObject v = new JsonObject();
            v.addProperty("role", e.role());
            v.addProperty("vehicleName", e.vehicleName());
            v.addProperty("count", e.count());
            v.addProperty("respawnSeconds", e.respawnSeconds());
            arr.add(v);
        }
        return arr;
    }

    /**
     * Разбор одной дивизии; битые записи техники пропускаются с замечанием в {@code warnings}. null — объект
     * без {@code divisionId}.
     */
    public static Division fromJson(JsonObject o, List<String> warnings) {
        if (o == null) return null;
        String id = str(o, "divisionId");
        if (id.isBlank()) {
            warnings.add("division without divisionId skipped");
            return null;
        }
        Set<String> modes = new LinkedHashSet<>();
        for (String m : str(o, "modes").split(",")) if (!m.isBlank()) modes.add(norm(m));
        Set<MapSize> sizes = new LinkedHashSet<>();
        for (String m : str(o, "mapSizes").split(",")) {
            if (m.isBlank()) continue;
            MapSize s = MapSize.parse(m);
            if (s == null) warnings.add(id + ": unknown map size " + m);
            else sizes.add(s);
        }
        List<Entry> list = new ArrayList<>();
        JsonElement ve = o.get("vehicles");
        if (ve != null && ve.isJsonArray()) {
            for (JsonElement el : ve.getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    warnings.add(id + ": vehicle entry is not an object");
                    continue;
                }
                JsonObject v = el.getAsJsonObject();
                String role = str(v, "role");
                String name = str(v, "vehicleName");
                if (role.isBlank() || name.isBlank()) {
                    warnings.add(id + ": vehicle entry needs role and vehicleName");
                    continue;
                }
                list.add(new Entry(role, name, integer(v, "count", 1), integer(v, "respawnSeconds", 0)));
            }
        } else if (ve != null) {
            warnings.add(id + ": vehicles must be an array");
        }
        return new Division(str(o, "faction"), id, str(o, "displayName"), modes, sizes, list);
    }

    /** Список дивизий из {@code data} ответа core-service. */
    public static List<Division> listFromJson(JsonArray arr, List<String> warnings) {
        List<Division> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            Division d = fromJson(el.getAsJsonObject(), warnings);
            if (d != null) out.add(d);
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    private static int integer(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        return e.getAsInt();
    }
}

package com.pigeostudios.pwp.warfare.layout;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Чистая модель раскладки карты (map_config.json) — фундамент для команд и внутриигрового редактора.
 *
 * <p>Поля, которые можно вывести ({@link LayoutDerive}), хранятся nullable: null = «не задано в конфиге». Так
 * запись ({@link MapLayoutCodec#write}) пишет только явное, а решение «вывести или взять из конфига» принимает
 * {@link LayoutDerive#resolve}. Неизвестные ключи живут в {@code source} (корень) и {@code extra} (элементы) и
 * сохраняются при записи.
 *
 * @param source корень исходного JSON (копия; ключи вне раскладки — имя, фракции, тикеты, режимы…)
 * @param mode режим ({@code aas}/{@code invasion}); null — не задан (в игре = aas)
 * @param points точки захвата (из выбранного паттерна или {@code capturePoints})
 * @param patternIndex индекс выбранного {@code capturePointPatterns}, откуда взяты точки; -1 — из {@code
 *     capturePoints}
 * @param mainZones мейн-зоны (любое число на команду, как в игре)
 * @param spawns спавны мейна по командам ({@code teams.*.spawn}); отсутствие ключа = не задан
 * @param vehicleSlots слоты спавнеров техники обеих команд, в порядке BLUE, затем RED
 * @param border граница зоны ({@code mapBounds}); null — не задана
 */
public record MapLayout(
        JsonObject source,
        String mode,
        List<CapturePointDef> points,
        int patternIndex,
        List<MainZoneDef> mainZones,
        Map<Team, TeamSpawn> spawns,
        List<VehicleSlot> vehicleSlots,
        Border border) {

    public static final String MODE_AAS = "aas";
    public static final String MODE_INVASION = "invasion";

    public MapLayout {
        source = source == null ? new JsonObject() : source.deepCopy();
        points = List.copyOf(points);
        mainZones = List.copyOf(mainZones);
        EnumMap<Team, TeamSpawn> s = new EnumMap<>(Team.class);
        if (spawns != null) s.putAll(spawns);
        spawns = java.util.Collections.unmodifiableMap(s);
        vehicleSlots = List.copyOf(vehicleSlots);
    }

    @Override
    public JsonObject source() {
        return source.deepCopy();
    }

    /** Режим с дефолтом игры. */
    public String effectiveMode() {
        return mode == null ? MODE_AAS : mode;
    }

    public TeamSpawn spawn(Team team) {
        return spawns.get(team);
    }

    /** Первая мейн-зона команды (null — нет). Команда сравнивается без учёта регистра. */
    public MainZoneDef mainOf(Team team) {
        for (MainZoneDef z : mainZones) {
            if (z.teamEnum() == team) return z;
        }
        return null;
    }

    public List<VehicleSlot> slotsOf(Team team) {
        return vehicleSlots.stream().filter(v -> v.team() == team).toList();
    }

    public MapLayout withPoints(List<CapturePointDef> p) {
        return new MapLayout(source, mode, p, patternIndex, mainZones, spawns, vehicleSlots, border);
    }

    public MapLayout withSpawns(Map<Team, TeamSpawn> s) {
        return new MapLayout(source, mode, points, patternIndex, mainZones, s, vehicleSlots, border);
    }

    public MapLayout withBorder(Border b) {
        return new MapLayout(source, mode, points, patternIndex, mainZones, spawns, vehicleSlots, b);
    }

    public MapLayout withMainZones(List<MainZoneDef> z) {
        return new MapLayout(source, mode, points, patternIndex, z, spawns, vehicleSlots, border);
    }

    public MapLayout withVehicleSlots(List<VehicleSlot> v) {
        return new MapLayout(source, mode, points, patternIndex, mainZones, spawns, v, border);
    }

    public MapLayout withMode(String m) {
        return new MapLayout(source, m, points, patternIndex, mainZones, spawns, vehicleSlots, border);
    }

    static JsonObject copy(JsonObject o) {
        return o == null ? new JsonObject() : o.deepCopy();
    }

    /**
     * Точка захвата. Числовые поля nullable: null = ключа нет (в игре — дефолт парсера, см. {@code eff*}).
     *
     * @param id явный {@code id} из конфига (новое поле, необязательное); без него идентификатор = имя
     */
    public record CapturePointDef(
            String id,
            String name,
            String shape,
            Box box,
            Integer bluePriority,
            Integer redPriority,
            Integer captureTimeMinutes,
            Integer ticketPenalty,
            Integer captureDeduction,
            Integer lockDurationMinutes,
            Integer gainNeutralizeTickets,
            Integer gainCaptureTickets,
            JsonObject extra) {

        /** Дефолт приоритета прежнего парсера (ломал цепочку молча — теперь вместо него вывод). */
        public static final int LEGACY_DEFAULT_PRIORITY = 10;

        public CapturePointDef {
            if (box == null) throw new IllegalArgumentException("point box required");
            extra = copy(extra);
        }

        @Override
        public JsonObject extra() {
            return extra.deepCopy();
        }

        /** Идентификатор для ссылок/валидатора: явный id, иначе имя. */
        public String key() {
            return id != null && !id.isEmpty() ? id : name;
        }

        public Integer priority(Team team) {
            return team == Team.BLUE ? bluePriority : redPriority;
        }

        public int effCaptureTimeMinutes() {
            return captureTimeMinutes == null ? 2 : captureTimeMinutes;
        }

        public int effTicketPenalty() {
            return ticketPenalty == null ? 60 : ticketPenalty;
        }

        public int effCaptureDeduction() {
            return captureDeduction == null ? 0 : captureDeduction;
        }

        public int effLockDurationMinutes() {
            return lockDurationMinutes == null ? 0 : lockDurationMinutes;
        }

        public int effGainNeutralizeTickets() {
            return gainNeutralizeTickets == null ? 0 : gainNeutralizeTickets;
        }

        public int effGainCaptureTickets() {
            return gainCaptureTickets == null ? 0 : gainCaptureTickets;
        }

        public String effShape() {
            return shape == null ? Shapes.CYLINDER : shape;
        }

        public CapturePointDef withPriorities(Integer blue, Integer red) {
            return new CapturePointDef(
                    id,
                    name,
                    shape,
                    box,
                    blue,
                    red,
                    captureTimeMinutes,
                    ticketPenalty,
                    captureDeduction,
                    lockDurationMinutes,
                    gainNeutralizeTickets,
                    gainCaptureTickets,
                    extra);
        }
    }

    /** Мейн-зона. {@code team} — строка как в конфиге (валидатор ловит не-BLUE/RED). */
    public record MainZoneDef(String team, String shape, Box box, JsonObject extra) {
        public MainZoneDef {
            if (box == null) throw new IllegalArgumentException("main zone box required");
            extra = copy(extra);
        }

        @Override
        public JsonObject extra() {
            return extra.deepCopy();
        }

        public Team teamEnum() {
            return Team.parse(team);
        }

        public String effShape() {
            return shape == null ? Shapes.CYLINDER : shape;
        }

        /** Точка внутри зоны по XZ с учётом формы. */
        public boolean containsXZ(double x, double z) {
            if (!box.containsXZ(x, z)) return false;
            if (!Shapes.isCylinder(shape)) return true;
            double dx = x - box.centerX();
            double dz = z - box.centerZ();
            double r = box.radius();
            return dx * dx + dz * dz <= r * r;
        }
    }

    /** Спавн мейна. {@code y}/{@code dimension} nullable — дефолты парсера 64 / overworld. */
    public record TeamSpawn(String dimension, int x, Integer y, int z, JsonObject extra) {
        public static final String DEFAULT_DIMENSION = "minecraft:overworld";
        public static final int DEFAULT_Y = 64;

        public TeamSpawn {
            extra = copy(extra);
        }

        @Override
        public JsonObject extra() {
            return extra.deepCopy();
        }

        public String effDimension() {
            return dimension == null ? DEFAULT_DIMENSION : dimension;
        }

        public int effY() {
            return y == null ? DEFAULT_Y : y;
        }
    }

    /** Слот спавнера техники: {@code role} (пул фракции) или {@code vehicleName}. */
    public record VehicleSlot(
            Team team, String role, String vehicleName, Pos pos, Float yaw, Boolean fixed, JsonObject extra) {
        public VehicleSlot {
            if (team == null || pos == null) throw new IllegalArgumentException("slot team/pos required");
            extra = copy(extra);
        }

        @Override
        public JsonObject extra() {
            return extra.deepCopy();
        }

        public float effYaw() {
            return yaw == null ? 0f : yaw;
        }

        public boolean effFixed() {
            return fixed != null && fixed;
        }
    }

    /** Граница зоны ({@code mapBounds}): квадрат со стороной {@code sizeBlocks}. Поля nullable — нет ключа. */
    public record Border(Integer centerX, Integer centerZ, Integer sizeBlocks, JsonObject extra) {
        public Border {
            extra = copy(extra);
        }

        @Override
        public JsonObject extra() {
            return extra.deepCopy();
        }

        public boolean complete() {
            return centerX != null && centerZ != null && sizeBlocks != null;
        }

        public double half() {
            return sizeBlocks == null ? 0 : sizeBlocks / 2.0;
        }

        public boolean containsXZ(double x, double z) {
            if (!complete()) return true;
            double h = half();
            return Math.abs(x - centerX) <= h && Math.abs(z - centerZ) <= h;
        }

        public boolean containsBox(Box b) {
            return containsXZ(b.minX(), b.minZ()) && containsXZ(b.maxX(), b.maxZ());
        }
    }
}

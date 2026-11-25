package com.pigeostudios.pwp.warfare.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.layout.MapLayout.Border;
import com.pigeostudios.pwp.warfare.layout.MapLayout.CapturePointDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.MainZoneDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.TeamSpawn;
import com.pigeostudios.pwp.warfare.layout.MapLayout.VehicleSlot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MapLayoutTest {

    private static final Path TAKMACHKA = Path.of("..", "maps", "takmachka", "map_config.json");

    private static String takmachka() throws IOException {
        return Files.readString(TAKMACHKA, StandardCharsets.UTF_8);
    }

    // ---------- фикстуры: прямая линия BLUE (x=0) → RED (x=1000) ----------

    private static MainZoneDef main(String team, int cx) {
        return new MainZoneDef(team, "cylinder", Box.of(cx - 50, -64, -50, cx + 50, 200, 50), null);
    }

    private static CapturePointDef point(String name, int cx, int cz, Integer blue, Integer red) {
        return new CapturePointDef(
                null,
                name,
                "cylinder",
                Box.of(cx - 20, -64, cz - 20, cx + 20, 100, cz + 20),
                blue,
                red,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static MapLayout line(List<CapturePointDef> pts) {
        return new MapLayout(
                new JsonObject(),
                "aas",
                pts,
                -1,
                List.of(main("BLUE", 0), main("RED", 1000)),
                Map.of(),
                List.of(),
                null);
    }

    private static Set<Issue.Code> codes(List<Issue> issues) {
        return issues.stream().map(Issue::code).collect(Collectors.toSet());
    }

    private static List<Issue> resolveAndValidate(MapLayout l) {
        return LayoutValidator.validate(LayoutDerive.resolve(l).layout());
    }

    // ---------- кодек ----------

    @Test
    void readsTakmachka() throws IOException {
        MapLayoutCodec.Parsed parsed = MapLayoutCodec.read(takmachka());
        assertTrue(parsed.issues().isEmpty(), parsed.issues().toString());
        MapLayout l = parsed.layout();
        assertEquals("aas", l.mode());
        assertEquals(9, l.points().size());
        assertEquals(-1, l.patternIndex());
        assertEquals(2, l.mainZones().size());
        assertEquals(12, l.slotsOf(Team.BLUE).size());
        assertEquals(12, l.slotsOf(Team.RED).size());
        assertEquals(new Border(-10, -15, 2532, null), l.border());
        TeamSpawn blue = l.spawn(Team.BLUE);
        assertEquals(-1131, blue.x());
        assertEquals(67, blue.y());
        assertEquals("minecraft:overworld", blue.dimension());
        CapturePointDef bridge = l.points().get(0);
        assertEquals("Юго-восточный мост", bridge.name());
        assertEquals(9, bridge.bluePriority());
        assertEquals(1, bridge.redPriority());
        assertNull(bridge.gainCaptureTickets());
        assertEquals(0, bridge.effGainCaptureTickets());
        assertEquals(270f, l.slotsOf(Team.BLUE).get(0).yaw());
    }

    @Test
    void takmachkaRoundTripIsLossless() throws IOException {
        JsonObject original = JsonParser.parseString(takmachka()).getAsJsonObject();
        JsonObject written = MapLayoutCodec.write(MapLayoutCodec.read(original).layout());
        assertEquals(original, written);
        // и повторная запись стабильна
        assertEquals(written, MapLayoutCodec.write(MapLayoutCodec.read(written).layout()));
    }

    @Test
    void takmachkaBehaviourUnchangedAfterDerive() throws IOException {
        MapLayout parsed = MapLayoutCodec.read(takmachka()).layout();
        LayoutDerive.Resolved r = LayoutDerive.resolve(parsed);
        // явные приоритеты, спавны и граница не тронуты
        for (int i = 0; i < parsed.points().size(); i++) {
            assertEquals(parsed.points().get(i), r.layout().points().get(i));
        }
        assertEquals(parsed.spawns(), r.layout().spawns());
        assertEquals(parsed.border(), r.layout().border());
        assertTrue(r.notes().isEmpty());
        List<Issue> issues = LayoutValidator.validate(r.layout());
        assertFalse(Issue.hasErrors(issues), issues.toString());
    }

    @Test
    void unknownKeysSurvive() {
        String json = "{\"name\":\"m\",\"futureKey\":{\"a\":1},"
                + "\"capturePoints\":[{\"name\":\"A\",\"pos1\":{\"x\":0,\"y\":0,\"z\":0},"
                + "\"pos2\":{\"x\":10,\"y\":10,\"z\":10},\"color\":\"red\"}],"
                + "\"teams\":{\"BLUE\":{\"faction\":\"usa\",\"spawn\":{\"x\":1,\"z\":2,\"note\":\"n\"}}},"
                + "\"vehicleSpawners\":{\"BLUE\":[{\"role\":\"TANK\",\"x\":1,\"y\":2,\"z\":3,\"yaw\":45.5,"
                + "\"tag\":\"t\"}]},"
                + "\"mapBounds\":{\"centerX\":0,\"centerZ\":0,\"sizeBlocks\":100,\"shape\":\"square\"}}";
        JsonObject original = JsonParser.parseString(json).getAsJsonObject();
        MapLayoutCodec.Parsed p = MapLayoutCodec.read(original);
        assertTrue(p.issues().isEmpty());
        assertEquals("red", p.layout().points().get(0).extra().get("color").getAsString());
        assertNull(p.layout().spawn(Team.BLUE).y());
        assertEquals(original, MapLayoutCodec.write(p.layout()));
    }

    @Test
    void patternPointsReadAndWrittenInPlace() {
        String json = "{\"capturePointPatternIndex\":1,\"capturePoints\":[],\"capturePointPatterns\":["
                + "{\"name\":\"a\",\"points\":[]},"
                + "{\"name\":\"b\",\"points\":[{\"name\":\"P\",\"pos1\":{\"x\":0,\"y\":0,\"z\":0},"
                + "\"pos2\":{\"x\":5,\"y\":5,\"z\":5},\"bluePriority\":1}]}]}";
        JsonObject original = JsonParser.parseString(json).getAsJsonObject();
        MapLayout l = MapLayoutCodec.read(original).layout();
        assertEquals(1, l.patternIndex());
        assertEquals("P", l.points().get(0).name());
        assertEquals(original, MapLayoutCodec.write(l));
    }

    @Test
    void invalidPatternIndexFallsBackToCapturePoints() {
        String json = "{\"capturePointPatternIndex\":5,\"capturePointPatterns\":[],"
                + "\"capturePoints\":[{\"name\":\"Q\",\"pos1\":{\"x\":0,\"y\":0,\"z\":0},"
                + "\"pos2\":{\"x\":5,\"y\":5,\"z\":5}}]}";
        MapLayout l = MapLayoutCodec.read(json).layout();
        assertEquals(-1, l.patternIndex());
        assertEquals("Q", l.points().get(0).name());
    }

    @Test
    void brokenElementIsSkippedWithIssue() {
        String json = "{\"capturePoints\":[{\"name\":\"ok\",\"pos1\":{\"x\":0,\"y\":0,\"z\":0},"
                + "\"pos2\":{\"x\":5,\"y\":5,\"z\":5}},{\"name\":\"bad\",\"pos1\":{\"x\":0}}],"
                + "\"vehicleSpawners\":{\"RED\":[{\"role\":\"TANK\",\"x\":\"oops\",\"y\":1,\"z\":1}]}}";
        MapLayoutCodec.Parsed p = MapLayoutCodec.read(json);
        assertEquals(1, p.layout().points().size());
        assertEquals(2, p.issues().size());
        assertTrue(p.issues().stream().allMatch(i -> i.code() == Issue.Code.PARSE_ELEMENT && i.isError()));
        assertEquals("capturePoints[1]", p.issues().get(0).subject());
    }

    @Test
    void writesDerivedValuesWhenResolvedLayoutIsSaved() {
        MapLayout l = line(List.of(point("A", 300, 0, null, null), point("B", 600, 0, null, null)));
        JsonObject out = MapLayoutCodec.write(LayoutDerive.resolve(l).layout());
        assertEquals(
                1,
                out.getAsJsonArray("capturePoints")
                        .get(0)
                        .getAsJsonObject()
                        .get("bluePriority")
                        .getAsInt());
        assertTrue(out.has("mapBounds"));
        assertTrue(out.getAsJsonObject("teams").getAsJsonObject("RED").has("spawn"));
    }

    // ---------- вывод ----------

    @Test
    void derivesChainFromBlueToRed() {
        // порядок в списке перемешан — вывод сортирует по оси мейн→мейн
        MapLayout l = line(List.of(
                point("C", 700, 30, null, null), point("A", 200, -40, null, null), point("B", 450, 0, null, null)));
        List<CapturePointDef> pts = LayoutDerive.resolve(l).layout().points();
        assertEquals(3, pts.get(0).bluePriority()); // C
        assertEquals(1, pts.get(0).redPriority());
        assertEquals(1, pts.get(1).bluePriority()); // A
        assertEquals(3, pts.get(1).redPriority());
        assertEquals(2, pts.get(2).bluePriority()); // B
        assertEquals(2, pts.get(2).redPriority());
        assertFalse(Issue.hasErrors(resolveAndValidate(l)));
    }

    @Test
    void explicitPrioritiesWinAndMissingTakeFreeRanks() {
        // A явно 3 (хотя ближе всех к BLUE), остальные получают свободные 1 и 2 по порядку цепочки
        MapLayout l =
                line(List.of(point("A", 200, 0, 3, null), point("B", 450, 0, null, null), point("C", 700, 0, null, 7)));
        List<CapturePointDef> pts = LayoutDerive.resolve(l).layout().points();
        assertEquals(3, pts.get(0).bluePriority());
        assertEquals(1, pts.get(0).redPriority());
        assertEquals(1, pts.get(1).bluePriority());
        assertEquals(3, pts.get(1).redPriority());
        assertEquals(2, pts.get(2).bluePriority());
        assertEquals(7, pts.get(2).redPriority()); // явное не трогаем — валидатор поймает дыру
        Set<Issue.Code> c = codes(resolveAndValidate(l));
        assertTrue(c.contains(Issue.Code.PRIORITY_HOLE));
    }

    @Test
    void noMainMeansNoOrderDerivation() {
        MapLayout l = line(List.of(point("A", 200, 0, null, null), point("B", 400, 0, null, null)))
                .withMainZones(List.of(main("BLUE", 0)));
        LayoutDerive.Resolved r = LayoutDerive.resolve(l);
        assertNull(r.layout().points().get(0).bluePriority());
        assertNull(LayoutDerive.chainOrder(l));
        Set<Issue.Code> c = codes(LayoutValidator.validate(r.layout()));
        assertTrue(c.contains(Issue.Code.NO_MAIN));
        assertTrue(c.contains(Issue.Code.PRIORITY_MISSING));
    }

    @Test
    void derivesBorderAndSpawn() {
        MapLayout l = line(List.of(point("A", 300, 400, 1, 2), point("B", 600, 0, 2, 1)));
        LayoutDerive.Resolved r = LayoutDerive.resolve(l);
        Border b = r.layout().border();
        // X: -50..1050 (1100), Z: -50..420 (470) -> сторона 1100 + 2*отступ
        assertEquals(1100 + 2 * LayoutDerive.BORDER_MARGIN, b.sizeBlocks());
        assertEquals(500, b.centerX());
        assertEquals(185, b.centerZ());
        TeamSpawn red = r.layout().spawn(Team.RED);
        assertEquals(1000, red.x());
        assertEquals(0, red.z());
        assertNull(red.y());
        assertEquals(2, r.notes().size());
        assertTrue(r.notes().stream().allMatch(n -> n.code() == Issue.Code.SPAWN_DERIVED));
    }

    @Test
    void partialBorderKeepsExplicitFields() {
        MapLayout l = line(List.of(point("A", 300, 0, 1, 2), point("B", 600, 0, 2, 1)))
                .withBorder(new Border(null, null, 5000, null));
        Border b = LayoutDerive.resolve(l).layout().border();
        assertEquals(5000, b.sizeBlocks());
        assertEquals(500, b.centerX());
        assertEquals(0, b.centerZ());
    }

    @Test
    void explicitSpawnNotReplaced() {
        MapLayout l = line(List.of(point("A", 300, 0, 1, 2), point("B", 600, 0, 2, 1)))
                .withSpawns(Map.of(Team.BLUE, new TeamSpawn(null, 7, 70, 8, null)));
        LayoutDerive.Resolved r = LayoutDerive.resolve(l);
        assertEquals(7, r.layout().spawn(Team.BLUE).x());
        assertEquals(1, r.notes().size()); // только RED выведен
    }

    // ---------- валидатор ----------

    @Test
    void validLineHasNoIssuesExceptDerivedSpawns() {
        MapLayout l = line(List.of(point("A", 300, 0, null, null), point("B", 600, 0, null, null)));
        assertTrue(resolveAndValidate(l).isEmpty(), resolveAndValidate(l).toString());
    }

    @Test
    void detectsMissingMainsAndTooFewPoints() {
        MapLayout l =
                new MapLayout(null, null, List.of(point("A", 0, 0, 1, 1)), -1, List.of(), Map.of(), List.of(), null);
        Set<Issue.Code> c = codes(resolveAndValidate(l));
        assertTrue(c.contains(Issue.Code.NO_MAIN));
        assertTrue(c.contains(Issue.Code.TOO_FEW_POINTS));
    }

    @Test
    void detectsOverlappingMains() {
        MapLayout l = line(List.of(point("A", 300, 0, 1, 2), point("B", 600, 0, 2, 1)))
                .withMainZones(List.of(main("BLUE", 0), main("RED", 60)));
        assertTrue(codes(resolveAndValidate(l)).contains(Issue.Code.MAINS_OVERLAP));
        // круги, у которых пересекаются только углы боксов, — не пересечение
        MapLayout corner = line(List.of(point("A", 300, 0, 1, 2), point("B", 600, 0, 2, 1)))
                .withMainZones(List.of(
                        main("BLUE", 0), new MainZoneDef("RED", "cylinder", Box.of(40, 0, 40, 140, 10, 140), null)));
        assertFalse(codes(resolveAndValidate(corner)).contains(Issue.Code.MAINS_OVERLAP));
    }

    @Test
    void detectsPriorityDuplicatesHolesAndMirror() {
        MapLayout dup = line(List.of(point("A", 300, 0, 1, 2), point("B", 600, 0, 1, 1)));
        Set<Issue.Code> c = codes(resolveAndValidate(dup));
        assertTrue(c.contains(Issue.Code.PRIORITY_DUPLICATE));
        assertTrue(c.contains(Issue.Code.PRIORITY_HOLE));

        // прежний дефолт 10 у двух точек = дыра + дубль
        MapLayout legacy = line(List.of(point("A", 300, 0, 10, 10), point("B", 600, 0, 10, 10)));
        assertTrue(codes(resolveAndValidate(legacy)).contains(Issue.Code.PRIORITY_HOLE));

        MapLayout same = line(List.of(point("A", 300, 0, 1, 1), point("B", 600, 0, 2, 2)));
        List<Issue> issues = resolveAndValidate(same);
        assertFalse(Issue.hasErrors(issues));
        assertTrue(codes(issues).contains(Issue.Code.PRIORITY_NOT_MIRRORED));
    }

    @Test
    void detectsDuplicateIdsAndBorderViolations() {
        MapLayout l = line(List.of(point("A", 300, 0, 1, 3), point("A", 600, 0, 2, 2), point("Far", 5000, 0, 3, 1)))
                .withBorder(new Border(500, 0, 1200, null));
        Set<Issue.Code> c = codes(resolveAndValidate(l));
        assertTrue(c.contains(Issue.Code.DUPLICATE_POINT_ID));
        assertTrue(c.contains(Issue.Code.POINT_OUTSIDE_BORDER));
    }

    @Test
    void detectsSpawnerOutsideMainAndBorder() {
        List<VehicleSlot> slots = new ArrayList<>();
        slots.add(new VehicleSlot(Team.BLUE, "TANK", null, new Pos(0, 64, 0), 0f, null, null)); // в мейне
        slots.add(new VehicleSlot(Team.BLUE, "TANK", null, new Pos(500, 64, 0), 0f, null, null)); // вне мейна
        slots.add(new VehicleSlot(Team.RED, null, null, new Pos(99999, 64, 0), null, null, null)); // за границей
        MapLayout l = line(List.of(point("A", 300, 0, 1, 2), point("B", 600, 0, 2, 1)))
                .withVehicleSlots(slots);
        List<Issue> issues = resolveAndValidate(l);
        Set<Issue.Code> c = codes(issues);
        assertTrue(c.contains(Issue.Code.SPAWNER_OUTSIDE_MAIN));
        assertTrue(c.contains(Issue.Code.SPAWNER_OUTSIDE_BORDER));
        assertTrue(c.contains(Issue.Code.SPAWNER_NO_VEHICLE));
        assertEquals(
                "BLUE[1]",
                issues.stream()
                        .filter(i -> i.code() == Issue.Code.SPAWNER_OUTSIDE_MAIN)
                        .findFirst()
                        .orElseThrow()
                        .subject());
    }

    @Test
    void detectsUnknownTeamShapeModeAndPointInMain() {
        MapLayout l = line(List.of(point("A", 10, 0, 1, 2), point("B", 600, 0, 2, 1)))
                .withMode("conquest")
                .withMainZones(List.of(
                        main("BLUE", 0),
                        main("RED", 1000),
                        new MainZoneDef("GREEN", "hexagon", Box.of(2000, 0, 0, 2100, 10, 100), null)));
        Set<Issue.Code> c = codes(resolveAndValidate(l));
        assertTrue(c.contains(Issue.Code.MAIN_UNKNOWN_TEAM));
        assertTrue(c.contains(Issue.Code.UNKNOWN_SHAPE));
        assertTrue(c.contains(Issue.Code.UNKNOWN_MODE));
        assertTrue(c.contains(Issue.Code.POINT_IN_MAIN));
    }

    @Test
    void issueKeysAndFormat() {
        Issue i = Issue.of(Issue.Code.PRIORITY_HOLE, "BLUE", "no point with priority 3");
        assertEquals("pwpwarfare.layout.issue.priority_hole", i.langKey());
        assertEquals("ERROR PRIORITY_HOLE [BLUE]: no point with priority 3", i.format());
        assertNotNull(Issue.Code.valueOf("SPAWN_DERIVED"));
    }

    @Test
    void everyIssueCodeIsLocalized() throws IOException {
        for (String lang : List.of("en_us", "ru_ru", "uk_ua")) {
            Path p = Path.of(
                    "..", "pwp-game", "src", "main", "resources", "assets", "pwpwarfare", "lang", lang + ".json");
            JsonObject json = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (Issue.Code c : Issue.Code.values()) {
                assertTrue(json.has(c.langKey()), lang + " lacks " + c.langKey());
            }
        }
    }

    @Test
    void layoutIsImmutable() {
        JsonObject src = new JsonObject();
        src.addProperty("name", "x");
        MapLayout l = new MapLayout(src, null, List.of(), -1, List.of(), Map.of(), List.of(), null);
        src.addProperty("name", "changed");
        assertEquals("x", l.source().get("name").getAsString());
        l.source().addProperty("name", "y");
        assertEquals("x", l.source().get("name").getAsString());
    }
}

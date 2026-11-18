package com.pigeostudios.pwp.warfare.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Kind;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Ref;
import com.pigeostudios.pwp.warfare.layout.MapLayout.CapturePointDef;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LayoutEditsTest {
    private static final Path TAKMACHKA = Path.of("..", "maps", "takmachka", "map_config.json");

    private static MapLayout empty() {
        return new MapLayout(new JsonObject(), null, List.of(), -1, List.of(), Map.of(), List.of(), null);
    }

    /** Мейны BLUE x≈0, RED x≈1000 и три точки в перемешанном порядке. */
    private static MapLayout line() {
        MapLayout l = empty();
        l = LayoutEdits.setMainCorner(l, Team.BLUE, 1, new Pos(-50, 0, -50));
        l = LayoutEdits.setMainCorner(l, Team.BLUE, 2, new Pos(50, 100, 50));
        l = LayoutEdits.setMainCorner(l, Team.RED, 1, new Pos(950, 0, -50));
        l = LayoutEdits.setMainCorner(l, Team.RED, 2, new Pos(1050, 100, 50));
        l = LayoutEdits.addPoint(l, "C", Box.of(730, 0, -20, 770, 50, 20));
        l = LayoutEdits.addPoint(l, "A", Box.of(230, 0, -20, 270, 50, 20));
        l = LayoutEdits.addPoint(l, "B", Box.of(480, 0, -20, 520, 50, 20));
        return l;
    }

    private static List<String> names(MapLayout l) {
        return l.points().stream().map(CapturePointDef::name).toList();
    }

    @Test
    void mainCornersBuildBoxAndCreateOncePerTeam() {
        MapLayout l = line();
        assertEquals(2, l.mainZones().size());
        assertEquals(1050, l.mainOf(Team.RED).box().maxX());
        l = LayoutEdits.setMainCorner(l, Team.RED, 2, new Pos(1100, 100, 60));
        assertEquals(2, l.mainZones().size());
        assertEquals(1100, l.mainOf(Team.RED).box().maxX());
        assertEquals(950, l.mainOf(Team.RED).box().minX());
    }

    @Test
    void addRemoveAndMoveRenumberChain() {
        MapLayout l = line();
        assertEquals(List.of("C", "A", "B"), names(l));
        assertEquals(
                List.of(1, 2, 3),
                l.points().stream().map(CapturePointDef::bluePriority).toList());
        assertEquals(
                List.of(3, 2, 1),
                l.points().stream().map(CapturePointDef::redPriority).toList());
        l = LayoutEdits.movePoint(l, 0, 2);
        assertEquals(List.of("A", "B", "C"), names(l));
        assertEquals(1, l.points().get(0).bluePriority());
        assertEquals(3, l.points().get(0).redPriority());
        l = LayoutEdits.removePoint(l, 1);
        assertEquals(List.of("A", "C"), names(l));
        assertEquals(2, l.points().get(1).bluePriority());
        assertEquals(1, l.points().get(1).redPriority());
        // валидатор доволен цепочкой
        List<Issue> issues = LayoutValidator.validate(LayoutDerive.resolve(l).layout());
        assertFalse(issues.stream().anyMatch(i -> i.code().name().startsWith("PRIORITY")), issues.toString());
    }

    @Test
    void moveOutOfRangeIsClampedOrIgnored() {
        MapLayout l = line();
        assertEquals(names(l), names(LayoutEdits.movePoint(l, 7, 0)));
        assertEquals(List.of("A", "B", "C"), names(LayoutEdits.movePoint(l, 0, 99)));
    }

    @Test
    void autoOrderIsSuggestionAlongMainAxis() {
        MapLayout l = LayoutEdits.autoOrder(line());
        assertEquals(List.of("A", "B", "C"), names(l));
        // без мейна RED предложение не меняет раскладку
        MapLayout noRed = LayoutEdits.removeMain(line(), 1);
        assertEquals(names(noRed), names(LayoutEdits.autoOrder(noRed)));
    }

    @Test
    void sortByChainPutsUnprioritisedLast() {
        MapLayout l = line();
        List<CapturePointDef> pts = new java.util.ArrayList<>(l.points());
        pts.set(0, pts.get(0).withPriorities(null, null)); // C без приоритета
        MapLayout sorted = LayoutEdits.sortByChain(l.withPoints(pts));
        assertEquals(List.of("A", "B", "C"), names(sorted));
    }

    @Test
    void pointParamsAndCornersKeepPriorities() {
        MapLayout l = line();
        CapturePointDef p = LayoutEdits.withParams(l.points().get(1), "Альфа", Shapes.BOX, 3, 40, 5, 1);
        l = LayoutEdits.replacePoint(l, 1, p);
        l = LayoutEdits.setPointCorner(l, 1, 2, new Pos(300, 50, 30));
        l = LayoutEdits.translatePoint(l, 1, 10, -10);
        CapturePointDef q = l.points().get(1);
        assertEquals("Альфа", q.name());
        assertEquals(2, q.bluePriority());
        assertEquals(240, q.box().minX());
        assertEquals(310, q.box().maxX());
        assertEquals(3, q.effCaptureTimeMinutes());
    }

    @Test
    void uniqueNamesSkipExisting() {
        MapLayout l = LayoutEdits.addPoint(empty(), null, Box.of(0, 0, 0, 1, 1, 1));
        assertEquals("P1", l.points().get(0).name());
        l = LayoutEdits.addPoint(l, null, Box.of(0, 0, 0, 1, 1, 1));
        assertEquals("P2", l.points().get(1).name());
    }

    @Test
    void slotsStayGroupedBlueThenRed() {
        MapLayout l = empty();
        l = LayoutEdits.addSlot(l, Team.RED, new Pos(1, 2, 3), "TANK", 0);
        l = LayoutEdits.addSlot(l, Team.BLUE, new Pos(4, 5, 6), "APC", 90);
        l = LayoutEdits.addSlot(l, Team.BLUE, new Pos(7, 8, 9), "MRAP", 0);
        assertEquals(Team.BLUE, l.vehicleSlots().get(0).team());
        assertEquals(Team.BLUE, l.vehicleSlots().get(1).team());
        assertEquals(Team.RED, l.vehicleSlots().get(2).team());
        assertEquals(1, LayoutEdits.lastSlotIndex(l, Team.BLUE));
        l = LayoutEdits.setSlotProps(l, 0, " ", "sbw:t90", 270f);
        assertNull(l.vehicleSlots().get(0).role());
        assertEquals("sbw:t90", l.vehicleSlots().get(0).vehicleName());
        assertEquals(-90f, l.vehicleSlots().get(0).effYaw());
        l = LayoutEdits.moveSlot(l, 2, new Pos(10, 11, 12));
        assertEquals(10, l.vehicleSlots().get(2).pos().x());
        assertEquals(0f, LayoutEdits.normYaw(Float.NaN));
    }

    @Test
    void borderFromCornersIsSquare() {
        MapLayout l = LayoutEdits.setBorderFromCorners(empty(), -100, 0, 300, 100);
        assertEquals(100, l.border().centerX());
        assertEquals(50, l.border().centerZ());
        assertEquals(400, l.border().sizeBlocks());
        assertNull(LayoutEdits.clearBorder(l).border());
    }

    @Test
    void issuesPointAtObjects() {
        MapLayout l = LayoutEdits.setBorder(line(), 500, 0, 300); // мейны и крайние точки вне границы
        l = LayoutEdits.addSlot(l, Team.RED, new Pos(5000, 0, 0), null, 0);
        for (Issue i : LayoutValidator.validate(l)) {
            Ref r = LayoutEdits.refOf(l, i);
            switch (i.code()) {
                case POINT_OUTSIDE_BORDER, POINT_PARTLY_OUTSIDE_BORDER -> assertEquals(Kind.POINT, r.kind());
                case MAIN_OUTSIDE_BORDER -> assertEquals(Kind.MAIN, r.kind());
                case SPAWNER_OUTSIDE_BORDER, SPAWNER_NO_VEHICLE -> {
                    assertEquals(Kind.SLOT, r.kind());
                    assertEquals(0, r.index());
                }
                default -> {}
            }
            if (r != null) assertNotNull(LayoutEdits.locate(l, r), i.toString());
        }
        assertNull(LayoutEdits.refOf(l, Issue.of(Issue.Code.TOO_FEW_POINTS, "", "")));
        assertNull(LayoutEdits.refOf(l, Issue.of(Issue.Code.PRIORITY_HOLE, "BLUE", "")));
        assertEquals(
                Kind.SPAWN,
                LayoutEdits.refOf(l, Issue.of(Issue.Code.SPAWN_DERIVED, "RED", ""))
                        .kind());
        double[] spawn = LayoutEdits.locate(l, Ref.spawn(Team.RED));
        assertEquals(1000.0, spawn[0]);
        assertFalse(Ref.point(9).valid(l));
    }

    @Test
    void stripKeepsOnlyLayoutAndMergeKeepsBaseConfig() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(TAKMACHKA, StandardCharsets.UTF_8))
                .getAsJsonObject();
        MapLayout base = MapLayoutCodec.read(root).layout();
        JsonObject stripped = LayoutTransfer.strip(base, "takmachka");
        assertFalse(stripped.has("availableFactions"));
        assertTrue(stripped.has("capturePoints"));
        assertNull(LayoutTransfer.checkSize(stripped.toString()), "takmachka must fit in one packet");
        MapLayout incoming = MapLayoutCodec.read(stripped).layout();
        assertEquals(base.points(), incoming.points());
        assertEquals(base.vehicleSlots(), incoming.vehicleSlots());
        // правка в редакторе поверх базы: фракции/тикеты базы целы, точки — из правки
        MapLayout edited = LayoutEdits.removePoint(incoming, 0);
        MapLayout merged = LayoutTransfer.merge(base, edited);
        assertEquals(base.points().size() - 1, merged.points().size());
        JsonObject out = MapLayoutCodec.write(merged);
        assertEquals(
                root.get("teams")
                        .getAsJsonObject()
                        .get("BLUE")
                        .getAsJsonObject()
                        .get("faction"),
                out.get("teams").getAsJsonObject().get("BLUE").getAsJsonObject().get("faction"));
        assertEquals(root.get("availableFactions"), out.get("availableFactions"));
    }

    @Test
    void sizeCheck() {
        assertNotNull(LayoutTransfer.checkSize(""));
        assertNotNull(LayoutTransfer.checkSize("я".repeat(LayoutTransfer.MAX_BYTES / 2 + 1)));
        assertNull(LayoutTransfer.checkSize("{}"));
    }
}

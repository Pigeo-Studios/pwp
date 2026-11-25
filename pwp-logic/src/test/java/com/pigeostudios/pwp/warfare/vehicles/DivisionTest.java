package com.pigeostudios.pwp.warfare.vehicles;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.vehicles.Division.Entry;
import com.pigeostudios.pwp.warfare.vehicles.Division.MapSize;
import com.pigeostudios.pwp.warfare.vehicles.DivisionPlan.Pick;
import com.pigeostudios.pwp.warfare.vehicles.DivisionPlan.Slot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DivisionTest {

    private static final Map<String, String> POOL = new LinkedHashMap<>();

    static {
        POOL.put("usa_mrap", "MRAP");
        POOL.put("usa_truck", "TRANSPORT");
        POOL.put("usa_logi", "SUPPLY");
        POOL.put("usa_stryker", "APC");
        POOL.put("usa_bradley", "IFV");
        POOL.put("usa_abrams", "TANK");
        POOL.put("usa_humvee", "MRAP");
    }

    private static final List<Slot> MAP = List.of(
            new Slot("SUPPLY", null),
            new Slot("TRANSPORT", null),
            new Slot("MRAP", null),
            new Slot("MRAP", null),
            new Slot("APC", null),
            new Slot("TANK", null),
            new Slot(null, "usa_logi"));

    private static Division mech() {
        return new Division(
                "USA",
                "mech",
                "Mechanized",
                Set.of("AAS"),
                Set.of(MapSize.MEDIUM, MapSize.LARGE),
                List.of(
                        new Entry("supply", "usa_logi", 1, 0),
                        new Entry("MRAP", "usa_humvee", 1, 120),
                        new Entry("APC", "usa_stryker", 1, 0),
                        new Entry("IFV", "usa_bradley", 2, 0)));
    }

    @Test
    void withoutDivisionKeepsLegacyAlphabeticalPool() {
        List<Pick> p = DivisionPlan.assign(MAP, POOL, null);
        assertEquals("usa_logi", p.get(0).vehicleName());
        assertEquals("usa_truck", p.get(1).vehicleName());
        assertEquals("usa_humvee", p.get(2).vehicleName()); // humvee < mrap по алфавиту — как было
        assertEquals("usa_humvee", p.get(3).vehicleName());
        assertEquals("usa_stryker", p.get(4).vehicleName());
        assertEquals("usa_abrams", p.get(5).vehicleName());
        assertEquals("usa_logi", p.get(6).vehicleName()); // явная машина слота
    }

    @Test
    void divisionDecidesVehiclesAndCounts() {
        List<Pick> p = DivisionPlan.assign(MAP, POOL, mech());
        assertEquals("usa_logi", p.get(0).vehicleName());
        assertNull(p.get(1).vehicleName(), "транспорта в составе нет — слот пуст");
        assertEquals(new Pick("usa_humvee", 120), p.get(2));
        assertNull(p.get(3).vehicleName(), "MRAP один — второй слот пуст");
        assertEquals("usa_stryker", p.get(4).vehicleName());
        assertNull(p.get(5).vehicleName(), "танков в составе нет");
        assertEquals("usa_logi", p.get(6).vehicleName(), "явная машина слота не меняется дивизией");
    }

    @Test
    void problemsForAdmin() {
        Division d = new Division(
                "usa",
                "bad",
                "",
                Set.of(),
                Set.of(),
                List.of(new Entry("TANK", "usa_nope", 1, 0), new Entry("TANK", "usa_bradley", 3, 0)));
        List<String> pr = DivisionPlan.problems(MAP, POOL, d);
        assertTrue(pr.contains("missing:usa_nope"), pr.toString());
        assertTrue(pr.contains("role:usa_bradley:IFV"), pr.toString());
        assertTrue(pr.contains("slots:TANK:3>1"), pr.toString());
        assertTrue(DivisionPlan.problems(MAP, POOL, null).isEmpty());
    }

    @Test
    void chooseRespectsRequestModeAndSize() {
        Division light = new Division("usa", "light", "Light", Set.of(), Set.of(MapSize.SMALL), List.of());
        List<Division> all = List.of(mech(), light);
        Random rng = new Random(1);
        assertEquals(
                "light",
                DivisionPlan.choose(all, "LIGHT", "AAS", MapSize.LARGE, rng).id());
        assertEquals(
                "mech",
                DivisionPlan.choose(all, null, "AAS", MapSize.LARGE, rng).id());
        assertEquals(
                "light",
                DivisionPlan.choose(all, "", "INVASION", MapSize.SMALL, rng).id());
        // ничего не подходит — любая, а не «без техники»
        assertNotNull(DivisionPlan.choose(all, null, "INVASION", MapSize.LARGE, rng));
        assertNull(DivisionPlan.choose(List.of(), "mech", "AAS", MapSize.LARGE, rng));
    }

    @Test
    void mapSizeClasses() {
        assertEquals(MapSize.SMALL, MapSize.of(200, 300));
        assertEquals(MapSize.MEDIUM, MapSize.of(301, 100));
        assertEquals(MapSize.LARGE, MapSize.of(1000, 900));
    }

    @Test
    void jsonRoundTripAndBrokenEntries() {
        List<String> w = new ArrayList<>();
        Division d = mech();
        Division back = Division.fromJson(d.toJson(), w);
        assertEquals(d, back);
        assertTrue(w.isEmpty(), w.toString());

        JsonArray arr = JsonParser.parseString("[{\"divisionId\":\"x\",\"faction\":\"RU\",\"mapSizes\":\"HUGE,small\","
                        + "\"vehicles\":[{\"role\":\"TANK\"},5,{\"role\":\"apc\",\"vehicleName\":\"btr\",\"count\":99}]},"
                        + "{\"faction\":\"ru\"}]")
                .getAsJsonArray();
        List<Division> list = Division.listFromJson(arr, w);
        assertEquals(1, list.size());
        Division x = list.get(0);
        assertEquals("ru", x.faction());
        assertEquals("x", x.displayName());
        assertEquals(Set.of(MapSize.SMALL), x.mapSizes());
        assertEquals(List.of(new Entry("APC", "btr", Division.MAX_COUNT, 0)), x.vehicles());
        assertEquals(4, w.size(), w.toString()); // HUGE, без имени, не объект, без divisionId
        assertTrue(x.fits("aas", MapSize.SMALL));
        assertFalse(x.fits("aas", MapSize.LARGE));
    }
}

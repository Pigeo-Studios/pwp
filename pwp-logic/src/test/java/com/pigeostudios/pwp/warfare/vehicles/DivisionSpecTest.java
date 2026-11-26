package com.pigeostudios.pwp.warfare.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DivisionSpecTest {
    private final Map<String, String> pool = Map.of("tank", "TANK", "apc", "apc", "uaz", "");

    @Test
    void roleComesFromPoolNotFromClient() {
        List<Division.Entry> e = DivisionSpec.parse("tank:1;apc:2;uaz:3;", pool);
        assertEquals(3, e.size());
        assertEquals(new Division.Entry("TANK", "tank", 1, 0), e.get(0));
        assertEquals("APC", e.get(1).role());
        assertEquals("TRANSPORT", e.get(2).role(), "без категории — транспорт");
    }

    @Test
    void zerosSkippedEmptyAllowed() {
        assertEquals(1, DivisionSpec.parse("tank:0;apc:1", pool).size());
        assertTrue(DivisionSpec.parse("", pool).isEmpty());
    }

    @Test
    void unknownVehicleOrBadCountRejected() {
        assertNull(DivisionSpec.parse("t90:1", pool));
        assertNull(DivisionSpec.parse("tank:" + (DivisionSpec.MAX_PER_VEHICLE + 1), pool));
        assertNull(DivisionSpec.parse("tank:-1", pool));
        assertNull(DivisionSpec.parse("tank:x", pool));
        assertNull(DivisionSpec.parse("tank", pool));
    }
}

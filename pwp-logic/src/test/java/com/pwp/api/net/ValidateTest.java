package com.pwp.api.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ValidateTest {
    @Test
    void ids() {
        assertNull(Validate.id("skinId", "pwp:ak_gold-01", 64));
        assertNotNull(Validate.id("skinId", "", 64));
        assertNotNull(Validate.id("skinId", "a b", 64));
        assertNotNull(Validate.id("skinId", "x".repeat(65), 64));
        assertNotNull(Validate.id("skinId", "кириллица", 64));
    }

    @Test
    void rangesAndFinite() {
        assertNull(Validate.range("slot", 5, 0, 40));
        assertNotNull(Validate.range("slot", 41, 0, 40));
        assertNotNull(Validate.finite("x", Double.NaN, -1e7, 1e7));
        assertNotNull(Validate.finite("x", Double.POSITIVE_INFINITY, -1e7, 1e7));
        assertNull(Validate.finite("x", 12.5, -1e7, 1e7));
    }

    @Test
    void firstReturnsFirstError() {
        assertNull(Validate.first(null, null));
        assertEquals("b", Validate.first(null, "b", "c"));
        assertNull(Validate.oneOf("t", "BLUE", "BLUE", "RED"));
        assertNotNull(Validate.oneOf("t", "GREEN", "BLUE", "RED"));
        assertNotNull(Validate.oneOf("t", null, "BLUE"));
    }

    @Test
    void labels() {
        assertNull(Validate.label("k", "Drone Operator", 64));
        assertNull(Validate.label("k", "Rifleman (Optic)", 64));
        assertNotNull(Validate.label("k", " Medic", 64));
        assertNotNull(Validate.label("k", "Medic\t", 64));
        assertNotNull(Validate.label("k", "Медик", 64));
        assertNotNull(Validate.label("k", "", 64));
        assertNotNull(Validate.label("k", "x".repeat(65), 64));
    }
}

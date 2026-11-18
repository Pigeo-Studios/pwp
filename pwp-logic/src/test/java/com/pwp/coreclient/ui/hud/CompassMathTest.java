package com.pwp.coreclient.ui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompassMathTest {

    @Test
    void bearingFromMinecraftYaw() {
        // yaw 0 — взгляд на юг (+Z), 90 — запад, 180/-180 — север, -90 — восток
        assertEquals(180F, CompassMath.bearing(0F), 1e-4);
        assertEquals(270F, CompassMath.bearing(90F), 1e-4);
        assertEquals(0F, CompassMath.bearing(180F), 1e-4);
        assertEquals(0F, CompassMath.bearing(-180F), 1e-4);
        assertEquals(90F, CompassMath.bearing(-90F), 1e-4);
        // yaw копится без ограничения (несколько оборотов)
        assertEquals(90F, CompassMath.bearing(-90F - 720F), 1e-3);
        assertEquals(0F, CompassMath.bearing(Float.NaN), 1e-4);
    }

    @Test
    void bearingToTargetMatchesWorldAxes() {
        assertEquals(0F, CompassMath.bearingTo(0, -10), 1e-4); // север = -Z
        assertEquals(90F, CompassMath.bearingTo(10, 0), 1e-4); // восток = +X
        assertEquals(180F, CompassMath.bearingTo(0, 10), 1e-4);
        assertEquals(270F, CompassMath.bearingTo(-10, 0), 1e-4);
        assertEquals(45F, CompassMath.bearingTo(5, -5), 1e-4);
        assertEquals(0F, CompassMath.bearingTo(0, 0), 1e-4);
    }

    @Test
    void targetStraightAheadIsAtCenter() {
        // игрок смотрит на восток (yaw -90): цель на +X — по центру ленты
        float heading = CompassMath.bearing(-90F);
        assertEquals(0F, CompassMath.delta(CompassMath.bearingTo(20, 0), heading), 1e-4);
        // цель на севере — левее на 90°
        assertEquals(-90F, CompassMath.delta(CompassMath.bearingTo(0, -20), heading), 1e-4);
    }

    @Test
    void deltaWrapsAcrossNorth() {
        assertEquals(20F, CompassMath.delta(10F, 350F), 1e-4);
        assertEquals(-20F, CompassMath.delta(350F, 10F), 1e-4);
        assertEquals(-180F, CompassMath.delta(180F, 0F), 1e-4);
        assertEquals(0F, CompassMath.delta(720F, 0F), 1e-4);
    }

    @Test
    void offsetScalesWithRange() {
        assertEquals(50F, CompassMath.offset(45F, 200F, 180F), 1e-4);
        assertEquals(-100F, CompassMath.offset(-90F, 200F, 180F), 1e-4);
        assertEquals(0F, CompassMath.offset(10F, 200F, 0F), 1e-4);
    }

    @Test
    void edgeFadeIsSmoothAndMonotonic() {
        assertEquals(1F, CompassMath.edgeFade(0F), 1e-6);
        assertEquals(1F, CompassMath.edgeFade(CompassMath.FADE_START), 1e-6);
        assertEquals(0F, CompassMath.edgeFade(1F), 1e-6);
        assertEquals(0F, CompassMath.edgeFade(1.5F), 1e-6);
        assertEquals(CompassMath.edgeFade(0.8F), CompassMath.edgeFade(-0.8F), 1e-6);
        float prev = 1F;
        for (float f = 0F; f <= 1F; f += 0.01F) {
            float a = CompassMath.edgeFade(f);
            assertTrue(a <= prev + 1e-6, "фейд растёт к краю при " + f);
            assertTrue(a >= 0F && a <= 1F);
            prev = a;
        }
    }

    @Test
    void ticksCoverVisibleRangeAcrossZero() {
        // азимут 5, сектор ±45: первое 15-градусное деление -30 (= 330), последнее 45
        int first = CompassMath.firstTick(5F, 45F, CompassMath.MAJOR_STEP);
        assertEquals(-30, first);
        int count = 0;
        for (int d = first; d <= 5F + 45F; d += CompassMath.MAJOR_STEP) count++;
        assertEquals(6, count); // -30 -15 0 15 30 45
        assertEquals(330, CompassMath.wrap360(first));
        assertEquals(0, CompassMath.firstTick(0F, 0F, 15));
        assertEquals(0, CompassMath.firstTick(10F, 5F, 0));
    }

    @Test
    void tickKindsAndCardinals() {
        assertEquals(2, CompassMath.tickKind(0));
        assertEquals(2, CompassMath.tickKind(-15));
        assertEquals(1, CompassMath.tickKind(5));
        assertEquals(0, CompassMath.tickKind(7));
        assertEquals(0, CompassMath.cardinalIndex(0));
        assertEquals(0, CompassMath.cardinalIndex(360));
        assertEquals(2, CompassMath.cardinalIndex(90));
        assertEquals(7, CompassMath.cardinalIndex(-45));
        assertEquals(-1, CompassMath.cardinalIndex(30));
    }

    @Test
    void degreeStringsAreCachedAndPadded() {
        assertEquals("15", CompassMath.degreeText(15));
        assertEquals("345", CompassMath.degreeText(-15));
        assertSame(CompassMath.degreeText(30), CompassMath.degreeText(390), "та же строка из кэша");
        assertEquals("005", CompassMath.bearingText(5.2F));
        assertEquals("275", CompassMath.bearingText(274.6F));
        assertEquals("000", CompassMath.bearingText(359.7F)); // округление 360 -> 000
        assertSame(CompassMath.bearingText(90F), CompassMath.bearingText(90.1F));
    }
}

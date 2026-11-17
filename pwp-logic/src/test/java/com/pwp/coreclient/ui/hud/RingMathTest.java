package com.pwp.coreclient.ui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RingMathTest {

    @Test
    void filledAndPartialSplitFraction() {
        assertEquals(0, RingMath.filled(0F, 16));
        assertEquals(0F, RingMath.partial(0F, 16));
        assertEquals(4, RingMath.filled(0.25F, 16));
        assertEquals(0F, RingMath.partial(0.25F, 16), 1e-4);
        assertEquals(3, RingMath.filled(0.75F, 4));
        assertEquals(16, RingMath.filled(1F, 16));
        assertEquals(0F, RingMath.partial(1F, 16));
        assertEquals(1, RingMath.filled(0.1F, 10));
        assertEquals(2, RingMath.filled(0.25F, 10));
        assertEquals(0.5F, RingMath.partial(0.25F, 10), 1e-3);
    }

    @Test
    void garbageFractionIsClamped() {
        assertEquals(0, RingMath.filled(Float.NaN, 16));
        assertEquals(0, RingMath.filled(-3F, 16));
        assertEquals(16, RingMath.filled(7F, 16));
        assertEquals(0, RingMath.filled(0.5F, 0));
        assertEquals(0F, RingMath.partial(0.5F, 0));
    }

    @Test
    void dashLeavesGapAndNeverVanishes() {
        // r=20, 16 сегментов: дуга ≈ 7.85, минус зазор 2 → 6
        assertEquals(6, RingMath.dash(20, 16, 2));
        assertEquals(1, RingMath.dash(1, 64, 5));
        assertEquals(1, RingMath.dash(0, 16, 2));
        assertTrue(RingMath.dash(20, 16, 0) > RingMath.dash(20, 16, 2));
    }

    @Test
    void spinIsContinuousAndWraps() {
        assertEquals(0F, RingMath.spin(0F, 90F), 1e-5);
        assertEquals(Math.PI, RingMath.spin(45F, 90F), 1e-4);
        assertEquals(RingMath.spin(10F, 90F), RingMath.spin(100F, 90F), 1e-4);
        assertEquals(Math.PI, RingMath.spin(-45F, 90F), 1e-4);
        assertEquals(0F, RingMath.spin(Float.NaN, 90F));
        assertEquals(0F, RingMath.spin(5F, 0F));
    }

    @Test
    void secondsLeftRoundsUp() {
        assertEquals(4, RingMath.secondsLeft(0F, 80));
        assertEquals(2, RingMath.secondsLeft(0.5F, 80));
        assertEquals(1, RingMath.secondsLeft(0.99F, 80));
        assertEquals(0, RingMath.secondsLeft(1F, 80));
        assertEquals(1, RingMath.secondsLeft(0F, 0));
    }
}

package com.pigeostudios.pwp.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SuppressionTest {
    @Test
    void bulletsStackAndCap() {
        Suppression s = new Suppression();
        for (int i = 0; i < 20; i++) s.bullet();
        assertEquals(1F, s.value());
        assertEquals(Suppression.MAX_VIGNETTE, s.vignette(), 1e-6);
        assertEquals(Suppression.MAX_SWAY_DEG, s.sway(), 1e-6);
    }

    @Test
    void holdsThenDecaysToZero() {
        Suppression s = new Suppression();
        s.bullet();
        float v = s.value();
        for (int i = 0; i < Suppression.HOLD_TICKS; i++) s.tick();
        assertEquals(v, s.value());
        for (int i = 0; i < 200; i++) s.tick();
        assertEquals(0F, s.value());
    }

    @Test
    void lightFireIsMild() {
        Suppression s = new Suppression();
        s.bullet();
        assertTrue(s.sway() < 0.05F);
        assertTrue(s.vignette() < 0.15F);
    }
}

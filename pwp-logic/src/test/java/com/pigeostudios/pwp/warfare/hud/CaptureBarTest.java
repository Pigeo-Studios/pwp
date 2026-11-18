package com.pigeostudios.pwp.warfare.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pigeostudios.pwp.warfare.hud.CaptureBar.State;
import org.junit.jupiter.api.Test;

class CaptureBarTest {

    @Test
    void sidePrefersOwnerThenAttackerThenMemory() {
        assertEquals("RED", CaptureBar.side("RED", "BLUE", "BLUE"));
        assertEquals("BLUE", CaptureBar.side("NEUTRAL", "BLUE", "RED"));
        assertEquals("RED", CaptureBar.side("NEUTRAL", "NONE", "RED"));
        assertEquals("NONE", CaptureBar.side("NEUTRAL", "NONE", null));
        assertEquals("NONE", CaptureBar.side(null, null, ""));
    }

    @Test
    void signedPositionPutsBlueLeftRedRight() {
        assertEquals(-1F, CaptureBar.signed("BLUE", 1F), 1e-6);
        assertEquals(0.4F, CaptureBar.signed("RED", 0.4F), 1e-6);
        assertEquals(0F, CaptureBar.signed("NONE", 0.7F), 1e-6);
        assertEquals(1F, CaptureBar.signed("RED", 3F), 1e-6);
        assertEquals(0F, CaptureBar.signed("BLUE", Float.NaN), 1e-6);
        assertEquals(0F, CaptureBar.signed("BLUE", -1F), 1e-6);
    }

    @Test
    void attackerPullsMarkerToItsSide() {
        // BLUE захватывает нейтральную (rate>0) — к BLUE; BLUE сбивает RED-точку (rate<0) — тоже к BLUE
        assertEquals(-1, CaptureBar.direction("BLUE", 2));
        assertEquals(-1, CaptureBar.direction("BLUE", -1));
        assertEquals(1, CaptureBar.direction("RED", -3));
        assertEquals(0, CaptureBar.direction("RED", 0));
        assertEquals(0, CaptureBar.direction("NONE", 2));
        assertEquals(3, CaptureBar.strength(-3, 4));
        assertEquals(4, CaptureBar.strength(9, 4));
    }

    @Test
    void stateFollowsServerFlags() {
        assertEquals(State.LOCKED, CaptureBar.state("BLUE", 2, true, true));
        assertEquals(State.CONTESTED, CaptureBar.state("BLUE", 0, true, false));
        assertEquals(State.CAPTURING, CaptureBar.state("NEUTRAL", 1, false, false));
        assertEquals(State.SECURING, CaptureBar.state("RED", 1, false, false));
        assertEquals(State.NEUTRALIZING, CaptureBar.state("RED", -2, false, false));
        assertEquals(State.NEUTRALIZING, CaptureBar.state("NEUTRAL", -1, false, false));
        assertEquals(State.HELD, CaptureBar.state("BLUE", 0, false, false));
        assertEquals(State.NEUTRAL, CaptureBar.state("NEUTRAL", 0, false, false));
    }
}

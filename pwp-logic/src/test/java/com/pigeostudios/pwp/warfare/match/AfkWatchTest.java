package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AfkWatchTest {
    private static final int WARN = 60;
    private static final int KICK = 600;

    @Test
    void warnsOnceThenKicks() {
        AfkWatch w = new AfkWatch();
        assertEquals(AfkWatch.Action.NONE, w.update("p", 0, 64, 0, 0, 0, 0, WARN, KICK));
        assertEquals(AfkWatch.Action.NONE, w.update("p", 0, 64, 0, 0, 0, 539_000, WARN, KICK));
        assertEquals(AfkWatch.Action.WARN, w.update("p", 0, 64, 0, 0, 0, 540_000, WARN, KICK));
        assertEquals(60, w.secondsLeft("p", 540_000, KICK));
        assertEquals(AfkWatch.Action.NONE, w.update("p", 0, 64, 0, 0, 0, 541_000, WARN, KICK));
        assertEquals(AfkWatch.Action.KICK, w.update("p", 0, 64, 0, 0, 0, 600_000, WARN, KICK));
    }

    @Test
    void movementOrLookResetsAndAllowsNewWarning() {
        AfkWatch w = new AfkWatch();
        w.update("p", 0, 64, 0, 0, 0, 0, WARN, KICK);
        assertEquals(AfkWatch.Action.WARN, w.update("p", 0, 64, 0, 0, 0, 550_000, WARN, KICK));
        // повернул голову — счёт заново
        assertEquals(AfkWatch.Action.NONE, w.update("p", 0, 64, 0, 10, 0, 560_000, WARN, KICK));
        assertEquals(AfkWatch.Action.NONE, w.update("p", 0, 64, 0, 10, 0, 1_000_000, WARN, KICK));
        assertEquals(AfkWatch.Action.WARN, w.update("p", 0, 64, 0, 10, 0, 1_100_000, WARN, KICK));
        // мелкое дрожание (<0.1 блока, <1°) не считается
        assertEquals(AfkWatch.Action.KICK, w.update("p", 0.05, 64, 0, 10.5F, 0, 1_160_000, WARN, KICK));
    }

    @Test
    void touchCountsAsActivity() {
        AfkWatch w = new AfkWatch();
        w.update("p", 0, 0, 0, 0, 0, 0, WARN, KICK);
        w.touch("p", 590_000);
        assertEquals(AfkWatch.Action.NONE, w.update("p", 0, 0, 0, 0, 0, 600_000, WARN, KICK));
    }

    @Test
    void yawWrapAroundIsSmallTurn() {
        AfkWatch w = new AfkWatch();
        w.update("p", 0, 0, 0, 179.8F, 0, 0, WARN, KICK);
        // 179.8 → -179.9: поворот на 0.3°, не активность
        assertEquals(AfkWatch.Action.KICK, w.update("p", 0, 0, 0, -179.9F, 0, 600_000, WARN, KICK));
    }

    @Test
    void disabledNeverKicks() {
        AfkWatch w = new AfkWatch();
        w.update("p", 0, 0, 0, 0, 0, 0, WARN, 0);
        assertEquals(AfkWatch.Action.NONE, w.update("p", 0, 0, 0, 0, 0, 99_000_000, WARN, 0));
    }
}

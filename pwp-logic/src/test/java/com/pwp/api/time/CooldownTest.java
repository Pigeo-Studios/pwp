package com.pwp.api.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CooldownTest {
    @Test
    void restartOverwritesInsteadOfAdding() {
        long until = Cooldown.start(100, 900);
        // повторный старт через 10 тиков — метка от нового «сейчас», а не until + 900
        until = Cooldown.start(110, 900);
        assertEquals(1010, until);
    }

    @Test
    void activeAndRemaining() {
        long until = Cooldown.start(0, 900);
        assertTrue(Cooldown.active(until, 899));
        assertFalse(Cooldown.active(until, 900));
        assertEquals(1, Cooldown.remaining(until, 899));
        assertEquals(0, Cooldown.remaining(until, 5000));
    }

    @Test
    void sanitizeDropsFutureStampAfterClockReset() {
        // метка от старого счётчика (1_000_000), счётчик сброшен на 50 — кулдаун «вечный»
        assertEquals(50, Cooldown.sanitize(1_000_000, 50, 900));
        assertEquals(500, Cooldown.sanitize(500, 50, 900));
    }

    @Test
    void elapsedTreatsZeroAsNever() {
        assertTrue(Cooldown.elapsed(0, 10, 100));
        assertFalse(Cooldown.elapsed(5, 10, 100));
        assertTrue(Cooldown.elapsed(5, 105, 100));
    }
}

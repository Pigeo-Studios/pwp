package com.pwp.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BackoffTest {
    @Test
    void doublesUpToMax() {
        assertEquals(200, Backoff.delay(1, 200, 6000));
        assertEquals(400, Backoff.delay(2, 200, 6000));
        assertEquals(800, Backoff.delay(3, 200, 6000));
        assertEquals(6000, Backoff.delay(10, 200, 6000));
    }

    @Test
    void noOverflowOnHugeAttempt() {
        assertEquals(6000, Backoff.delay(Integer.MAX_VALUE, 200, 6000));
        assertEquals(Long.MAX_VALUE, Backoff.delay(1000, 1L << 40, Long.MAX_VALUE));
    }

    @Test
    void edgeInputs() {
        assertEquals(200, Backoff.delay(0, 200, 6000));
        assertEquals(200, Backoff.delay(-5, 200, 6000));
        assertEquals(0, Backoff.delay(3, 0, 6000));
        // max меньше base — не меньше base
        assertEquals(200, Backoff.delay(5, 200, 10));
    }

    @Test
    void loudOnPowersOfTwo() {
        assertTrue(Backoff.loud(1));
        assertTrue(Backoff.loud(2));
        assertFalse(Backoff.loud(3));
        assertTrue(Backoff.loud(4));
        assertFalse(Backoff.loud(6));
        assertTrue(Backoff.loud(64));
        assertFalse(Backoff.loud(0));
    }
}

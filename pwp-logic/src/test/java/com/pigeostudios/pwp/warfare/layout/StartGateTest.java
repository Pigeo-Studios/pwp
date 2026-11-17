package com.pigeostudios.pwp.warfare.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StartGateTest {
    private static final Issue ERR = Issue.of(Issue.Code.NO_MAIN, "BLUE", "");
    private static final Issue WARN = Issue.of(Issue.Code.POINTS_OVERLAP, "A", "");

    @Test
    void errorsBlockWithoutForce() {
        StartGate.Decision d = StartGate.check(List.of(ERR, WARN), true, false);
        assertTrue(d.blocked());
        assertEquals(List.of(ERR), d.errors());
    }

    @Test
    void forceAllowsAndIsMarked() {
        StartGate.Decision d = StartGate.check(List.of(ERR), true, true);
        assertTrue(d.allowed());
        assertTrue(d.forced());
    }

    @Test
    void warningsNeverBlock() {
        StartGate.Decision d = StartGate.check(List.of(WARN), true, false);
        assertTrue(d.allowed());
        assertFalse(d.forced());
        assertTrue(d.errors().isEmpty());
    }

    @Test
    void disabledFlagOrNoLayoutAllows() {
        assertTrue(StartGate.check(List.of(ERR), false, false).allowed());
        assertFalse(StartGate.check(List.of(ERR), false, false).forced());
        assertTrue(StartGate.check(null, true, false).allowed());
    }
}

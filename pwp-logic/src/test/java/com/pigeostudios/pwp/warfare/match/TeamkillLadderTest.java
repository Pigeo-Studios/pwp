package com.pigeostudios.pwp.warfare.match;

import static com.pigeostudios.pwp.warfare.match.TeamkillLadder.Step.BAN;
import static com.pigeostudios.pwp.warfare.match.TeamkillLadder.Step.RESTRICT;
import static com.pigeostudios.pwp.warfare.match.TeamkillLadder.Step.SPECTATE;
import static com.pigeostudios.pwp.warfare.match.TeamkillLadder.Step.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamkillLadderTest {
    @Test
    void oldThresholdsWithoutBan() {
        assertEquals(WARN, TeamkillLadder.step(1, 8, 0));
        assertEquals(WARN, TeamkillLadder.step(4, 8, 0));
        assertEquals(RESTRICT, TeamkillLadder.step(5, 8, 0));
        assertEquals(RESTRICT, TeamkillLadder.step(7, 8, 0));
        assertEquals(SPECTATE, TeamkillLadder.step(8, 8, 0));
    }

    @Test
    void banReplacesSpectatorWhenEnabled() {
        assertEquals(BAN, TeamkillLadder.step(8, 8, 60));
        assertEquals(RESTRICT, TeamkillLadder.step(6, 8, 60));
    }

    @Test
    void topNeverBelowRestriction() {
        // порог наблюдателя 3 в конфиге: верхняя ступень всё равно после ограничения
        assertEquals(RESTRICT, TeamkillLadder.step(5, 3, 60));
        assertEquals(BAN, TeamkillLadder.step(6, 3, 60));
    }

    @Test
    void autoReportOnceAtRestriction() {
        assertTrue(TeamkillLadder.autoReport(5));
        assertFalse(TeamkillLadder.autoReport(4));
        assertFalse(TeamkillLadder.autoReport(6));
    }
}

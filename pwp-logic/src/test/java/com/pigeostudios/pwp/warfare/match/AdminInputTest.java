package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.match.AdminInput.Phase;
import org.junit.jupiter.api.Test;

class AdminInputTest {
    @Test
    void modeRejectsTypos() {
        assertEquals("aas", AdminInput.mode("AAS"));
        assertEquals("invasion", AdminInput.mode(" Invasion "));
        assertNull(AdminInput.mode("invasoin"));
        assertNull(AdminInput.mode(null));
    }

    @Test
    void teamRejectsTypos() {
        assertEquals("BLUE", AdminInput.team("blue"));
        assertEquals("RED", AdminInput.team("Red"));
        assertNull(AdminInput.team("bleu"));
        assertNull(AdminInput.team(""));
    }

    @Test
    void factionIds() {
        assertEquals("usa", AdminInput.faction("USA"));
        assertEquals("none", AdminInput.faction("clear"));
        assertEquals("none", AdminInput.faction("none"));
        assertNull(AdminInput.faction("us a"));
        assertNull(AdminInput.faction(""));
        assertNull(AdminInput.faction("x".repeat(33)));
    }

    @Test
    void ranges() {
        assertTrue(AdminInput.ticketsValid(0));
        assertFalse(AdminInput.ticketsValid(-1));
        assertFalse(AdminInput.ticketsValid(AdminInput.TICKETS_MAX + 1));
        assertTrue(AdminInput.waitingValid(300));
        assertFalse(AdminInput.waitingValid(3601));
    }

    @Test
    void phasePriority() {
        assertEquals(Phase.PAUSED, AdminInput.phase(true, true, true, true, true, true));
        assertEquals(Phase.COUNTDOWN, AdminInput.phase(false, true, false, true, false, false));
        assertEquals(Phase.WAITING, AdminInput.phase(false, false, false, true, false, false));
        assertEquals(Phase.READY_VOTE, AdminInput.phase(false, false, false, false, true, false));
        assertEquals(Phase.PLAYING, AdminInput.phase(false, false, false, false, true, true));
        assertEquals(Phase.IDLE, AdminInput.phase(false, false, false, false, false, false));
    }
}

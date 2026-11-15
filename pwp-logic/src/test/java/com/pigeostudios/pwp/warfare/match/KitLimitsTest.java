package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.match.KitLimits.Input;
import org.junit.jupiter.api.Test;

class KitLimitsTest {
    private static Input in(int team, int squad, int minSq, String cat, int tc, int sc, int size, int fs) {
        return new Input(team, squad, minSq, cat, tc, sc, size, fs);
    }

    @Test
    void teamLimit() {
        assertNull(KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 5, 0, 4, 0)));
        assertEquals("Team Full (6/6)", KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 6, 0, 4, 0)));
    }

    @Test
    void squadLimitCheckedAfterTeam() {
        assertEquals("Squad Full (2/2)", KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 3, 2, 4, 0)));
        assertEquals("Team Full (6/6)", KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 6, 2, 4, 0)));
    }

    @Test
    void unlimitedWhenNonPositive() {
        assertNull(KitLimits.unavailableReason(in(-1, 0, 0, "SUPPORT", 100, 100, 9, 0)));
    }

    @Test
    void minSquadPlayers() {
        assertEquals("Need 4 players in Squad", KitLimits.unavailableReason(in(-1, 0, 4, "SPECIALIST", 0, 0, -1, 0)));
        assertEquals("Need 4 players in Squad", KitLimits.unavailableReason(in(-1, 0, 4, "SPECIALIST", 0, 0, 3, 0)));
        assertNull(KitLimits.unavailableReason(in(-1, 0, 4, "SPECIALIST", 0, 0, 4, 0)));
    }

    @Test
    void fireSupportCapPerSquad() {
        assertNull(KitLimits.unavailableReason(in(-1, 0, 0, "FIRE_SUPPORT", 0, 0, 9, 2)));
        assertEquals(
                "Max 3 Fire Support per Squad", KitLimits.unavailableReason(in(-1, 0, 0, "FIRE_SUPPORT", 0, 0, 9, 3)));
        assertTrue(KitLimits.fireSupportCapReached("FIRE_SUPPORT", 3));
        assertFalse(KitLimits.fireSupportCapReached("SUPPORT", 10), "лимит только для FIRE_SUPPORT");
    }
}

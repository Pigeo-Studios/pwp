package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamBalanceTest {
    @Test
    void smallerTeam() {
        assertEquals("Blue", TeamBalance.smaller(0, 0));
        assertEquals("Red", TeamBalance.smaller(3, 2));
        assertEquals("Blue", TeamBalance.smaller(2, 3));
    }

    @Test
    void joinLimit() {
        assertTrue(TeamBalance.canJoin("BLUE", 1, 0));
        assertFalse(TeamBalance.canJoin("blue", 2, 0));
        assertTrue(TeamBalance.canJoin("Red", 0, 1));
        assertFalse(TeamBalance.canJoin("Red", 5, 7));
    }

    @Test
    void resolveRequest() {
        assertEquals("Blue", TeamBalance.resolve("BLUE", 9, 0));
        assertEquals("Red", TeamBalance.resolve("red", 0, 9));
        assertEquals("Red", TeamBalance.resolve("AUTO", 4, 3));
        assertNull(TeamBalance.resolve("green", 0, 0));
        assertNull(TeamBalance.resolve(null, 0, 0));
    }

    @Test
    void autoAlwaysJoinable() {
        for (int b = 0; b < 6; b++) {
            for (int r = 0; r < 6; r++) {
                assertTrue(TeamBalance.canJoin(TeamBalance.resolve("AUTO", b, r), b, r));
            }
        }
    }
}

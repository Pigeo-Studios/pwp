package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamVisibilityTest {

    @Test
    void viewerKeyFromScoreboardTeam() {
        assertEquals("BLUE", TeamVisibility.viewerKey("Blue", false));
        assertEquals("RED", TeamVisibility.viewerKey("red", false));
        assertEquals(TeamVisibility.NONE, TeamVisibility.viewerKey(null, false));
        assertEquals(TeamVisibility.NONE, TeamVisibility.viewerKey("Spectators", false));
        assertEquals(TeamVisibility.ALL, TeamVisibility.viewerKey("Blue", true));
        assertEquals(TeamVisibility.ALL, TeamVisibility.viewerKey(null, true));
    }

    @Test
    void enemyStuffIsHidden() {
        assertTrue(TeamVisibility.visible("BLUE", "BLUE"));
        assertTrue(TeamVisibility.visible("blue", "BLUE"));
        assertFalse(TeamVisibility.visible("RED", "BLUE"));
        assertFalse(TeamVisibility.visible("BLUE", "RED"));
        assertFalse(TeamVisibility.visible("BLUE", TeamVisibility.NONE));
    }

    @Test
    void neutralAndObserverSeeAll() {
        assertTrue(TeamVisibility.visible("NEUTRAL", "BLUE"));
        assertTrue(TeamVisibility.visible(null, "RED"));
        assertTrue(TeamVisibility.visible("", TeamVisibility.NONE));
        assertTrue(TeamVisibility.visible("RED", TeamVisibility.ALL));
        assertTrue(TeamVisibility.visible("BLUE", TeamVisibility.ALL));
    }
}

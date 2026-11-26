package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.match.TacticalRose.Context;
import com.pigeostudios.pwp.warfare.match.TacticalRose.Item;
import java.util.List;
import org.junit.jupiter.api.Test;

class TacticalRoseTest {
    @Test
    void noTeamNoMenu() {
        assertTrue(TacticalRose.items(new Context(false, true, true, true, true, true))
                .isEmpty());
    }

    @Test
    void riflemanSpotsAndRequests() {
        assertEquals(
                List.of(Item.SPOT, Item.REQUESTS),
                TacticalRose.items(new Context(true, true, false, false, false, false)));
        assertEquals(List.of(Item.SPOT), TacticalRose.items(new Context(true, false, false, false, false, false)));
    }

    @Test
    void fireteamLeaderGivesOrdersButDoesNotBuild() {
        assertEquals(
                List.of(Item.SPOT, Item.REQUESTS, Item.ORDERS),
                TacticalRose.items(new Context(true, true, false, true, false, false)));
    }

    @Test
    void squadLeaderCommanderNearVehicleHasEverything() {
        assertEquals(
                List.of(Item.SPOT, Item.REQUESTS, Item.ORDERS, Item.BUILD, Item.VEHICLE, Item.COMMANDER),
                TacticalRose.items(new Context(true, true, true, false, true, true)));
    }

    @Test
    void requestParse() {
        assertEquals(TacticalRose.Request.MEDIC, TacticalRose.Request.parse("medic"));
        assertNull(TacticalRose.Request.parse("pizza"));
    }
}

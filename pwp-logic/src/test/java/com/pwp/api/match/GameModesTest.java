package com.pwp.api.match;

import static org.junit.jupiter.api.Assertions.*;

import com.pwp.api.match.GameModes.PointState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameModesTest {
    // цепочка из трёх точек: BLUE 1→2→3, RED 3→2→1
    private static PointState a(String owner) {
        return new PointState(1, 3, owner, false);
    }

    private static PointState b(String owner) {
        return new PointState(2, 2, owner, false);
    }

    private static PointState c(String owner) {
        return new PointState(3, 1, owner, false);
    }

    @Test
    void aasChainAsBefore() {
        List<PointState> all = List.of(a("NEUTRAL"), b("NEUTRAL"), c("NEUTRAL"));
        assertTrue(GameModes.canCapture(GameModes.AAS, all.get(0), "BLUE", all, "RED"));
        assertFalse(GameModes.canCapture(GameModes.AAS, all.get(1), "BLUE", all, "RED"));
        List<PointState> afterA = List.of(a("BLUE"), b("NEUTRAL"), c("NEUTRAL"));
        assertTrue(GameModes.canCapture(GameModes.AAS, afterA.get(1), "BLUE", afterA, "RED"));
        assertTrue(GameModes.canCapture(GameModes.AAS, afterA.get(2), "RED", afterA, "RED"));
    }

    @Test
    void invasionLocksAndDefenderRules() {
        PointState locked = new PointState(1, 3, "BLUE", true);
        List<PointState> all = List.of(locked, b("RED"), c("RED"));
        assertFalse(GameModes.canCapture(GameModes.INVASION, locked, "RED", all, "RED"), "отбить взятую нельзя");
        PointState neutral = new PointState(2, 2, "NEUTRAL", false);
        assertFalse(GameModes.canCapture(GameModes.INVASION, neutral, "RED", List.of(locked, neutral), "RED"));
    }

    @Test
    void skirmishAnyOrder() {
        List<PointState> all = List.of(a("NEUTRAL"), b("NEUTRAL"), c("RED"));
        for (PointState p : all) assertTrue(GameModes.canCapture(GameModes.SKIRMISH, p, "BLUE", all, "RED"));
        assertFalse(GameModes.SKIRMISH.vehicles());
    }

    @Test
    void registryAndVoting() {
        assertEquals(GameModes.INVASION, GameModes.byId(" Invasion "));
        assertNull(GameModes.byId("tdm"));
        assertEquals(GameModes.AAS, GameModes.orAas(null));
        assertEquals(List.of(GameModes.AAS, GameModes.INVASION), GameModes.votable(null));
        assertEquals(
                List.of(GameModes.AAS, GameModes.INVASION, GameModes.SKIRMISH),
                GameModes.votable(Set.of("invasion", "SKIRMISH")));
    }
}

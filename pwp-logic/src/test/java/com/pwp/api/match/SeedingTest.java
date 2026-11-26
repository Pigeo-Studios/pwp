package com.pwp.api.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SeedingTest {
    private final List<GameModes.Mode> withSkirmish = List.of(GameModes.AAS, GameModes.INVASION, GameModes.SKIRMISH);

    @Test
    void lowOnlineLeavesOnlySmallModes() {
        assertTrue(Seeding.active(5, 12));
        assertEquals(List.of(GameModes.SKIRMISH), Seeding.modes(withSkirmish, 5, 12));
    }

    @Test
    void enoughOnlineOrDisabledKeepsAll() {
        assertEquals(withSkirmish, Seeding.modes(withSkirmish, 12, 12));
        assertFalse(Seeding.active(1, 0));
        assertEquals(withSkirmish, Seeding.modes(withSkirmish, 1, 0));
    }

    @Test
    void mapWithoutSmallModeKeepsNormalModes() {
        List<GameModes.Mode> big = List.of(GameModes.AAS, GameModes.INVASION);
        assertEquals(big, Seeding.modes(big, 2, 12));
    }
}

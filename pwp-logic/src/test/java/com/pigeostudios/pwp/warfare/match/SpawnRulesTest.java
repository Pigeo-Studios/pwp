package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pigeostudios.pwp.warfare.match.SpawnRules.PlayerState;
import com.pigeostudios.pwp.warfare.match.SpawnRules.SpawnType;
import org.junit.jupiter.api.Test;

class SpawnRulesTest {
    @Test
    void parsesKnownTypes() {
        assertEquals(SpawnType.Kind.MAIN, SpawnRules.parse("MAIN").kind());
        assertEquals(SpawnType.Kind.RALLY, SpawnRules.parse("RALLY").kind());
        assertEquals(new SpawnType(SpawnType.Kind.HUB, 10, -64, -300), SpawnRules.parse("HUB:10:-64:-300"));
    }

    @Test
    void rejectsGarbage() {
        assertNull(SpawnRules.parse(null));
        assertNull(SpawnRules.parse(""));
        assertNull(SpawnRules.parse("main"));
        assertNull(SpawnRules.parse("HUB"));
        assertNull(SpawnRules.parse("HUB:1:2"));
        assertNull(SpawnRules.parse("HUB:1:2:3:4"));
        assertNull(SpawnRules.parse("HUB:a:2:3"));
        assertNull(SpawnRules.parse("HUB:+1:2:3"));
        assertNull(SpawnRules.parse("HUB:1::3"));
        assertNull(SpawnRules.parse("HUB:-:2:3"));
        assertNotNull(SpawnRules.validateType("x".repeat(65)));
    }

    @Test
    void rejectsOutOfWorld() {
        assertNull(SpawnRules.parse("HUB:30000001:0:0"));
        assertNull(SpawnRules.parse("HUB:0:0:-30000001"));
        assertNull(SpawnRules.parse("HUB:0:2048:0"));
        assertNull(SpawnRules.parse("HUB:0:-2049:0"));
        assertNull(SpawnRules.parse("HUB:9999999999:0:0"));
        assertNotNull(SpawnRules.parse("HUB:30000000:2047:-30000000"));
        assertNull(SpawnRules.validateType("HUB:1:2:3"));
    }

    @Test
    void aliveOnFieldIsDenied() {
        assertNotNull(SpawnRules.denyReason(new PlayerState(false, false, false, false)));
    }

    @Test
    void deadOrSpectatorMayDeploy() {
        assertNull(SpawnRules.denyReason(new PlayerState(true, false, false, false)));
        assertNull(SpawnRules.denyReason(new PlayerState(false, true, false, false)));
    }

    @Test
    void downedMayGiveUp() {
        assertNull(SpawnRules.denyReason(new PlayerState(false, false, true, false)));
    }

    @Test
    void teamKillSpectatorIsDenied() {
        assertNotNull(SpawnRules.denyReason(new PlayerState(false, true, false, true)));
    }
}

package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.match.CombatRules.ReviveStep;
import org.junit.jupiter.api.Test;

class CombatRulesTest {

    @Test
    void artilleryWaitsThenFiresOnInterval() {
        assertFalse(CombatRules.shellFires(400), "сразу после подтверждения — подлёт");
        assertFalse(CombatRules.shellFires(301));
        assertTrue(CombatRules.shellFires(300), "первый разрыв через 5 с");
        assertFalse(CombatRules.shellFires(299));
        assertTrue(CombatRules.shellFires(290));
        assertFalse(CombatRules.shellFires(0));
        assertEquals(30, CombatRules.shellsPerStrike());
    }

    @Test
    void shellsLandInsideRadius() {
        for (double u1 = 0; u1 < 1; u1 += 0.1) {
            for (double u2 = 0; u2 < 1; u2 += 0.1) {
                double[] d = CombatRules.shellOffset(u1, u2, 20);
                assertTrue(d[0] * d[0] + d[1] * d[1] <= 400 + 1e-9);
            }
        }
        double[] c = CombatRules.shellOffset(0, 0.3, 20);
        assertEquals(0, c[0], 1e-9);
        assertEquals(0, c[1], 1e-9);
    }

    @Test
    void onlyMedicRevives() {
        assertTrue(CombatRules.canRevive("Medic", false));
        assertTrue(CombatRules.canRevive("medic ", false));
        assertFalse(CombatRules.canRevive("Rifleman", false));
        assertFalse(CombatRules.canRevive("", false));
        assertFalse(CombatRules.canRevive(null, false));
        assertTrue(CombatRules.canRevive("Rifleman", true));
    }

    @Test
    void reviveChannel() {
        assertEquals(ReviveStep.CONTINUE, CombatRules.reviveStep(40, 100, 4, true));
        assertEquals(ReviveStep.DONE, CombatRules.reviveStep(100, 100, 4, true));
        assertEquals(ReviveStep.CANCEL_DISTANCE, CombatRules.reviveStep(40, 100, 9.01, true));
        assertEquals(ReviveStep.CANCEL_STATE, CombatRules.reviveStep(99, 100, 1, false));
        assertEquals(40, CombatRules.revivePercent(40, 100));
        assertEquals(100, CombatRules.revivePercent(500, 100));
    }
}

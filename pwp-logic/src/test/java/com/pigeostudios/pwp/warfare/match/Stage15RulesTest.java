package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.movement.Stamina;
import org.junit.jupiter.api.Test;

class Stage15RulesTest {
    @Test
    void scoreCountsTeamworkNotOnlyKills() {
        assertEquals(0, ScoreRules.score(0, 0, 0, 0, 0, 0));
        assertEquals(10 + 30 + 25 + 30 + 10, ScoreRules.score(1, 2, 1, 1, 250, 0));
        // медик без убийств может обогнать стрелка
        assertTrue(ScoreRules.score(0, 5, 0, 0, 0, 0) > ScoreRules.score(5, 0, 0, 0, 0, 0));
        assertEquals(0, ScoreRules.score(0, 0, 0, 0, 0, 3), "тимкиллы не уводят в минус");
        assertEquals(10, ScoreRules.score(3, 0, 0, 0, 0, 1));
    }

    @Test
    void commanderCooldownsPerTeamAndAbility() {
        CommanderRules.Cooldowns c = new CommanderRules.Cooldowns();
        var uav = CommanderRules.Ability.UAV;
        assertTrue(c.tryUse("BLUE", uav, 0));
        assertFalse(c.tryUse("BLUE", uav, 100));
        assertEquals(uav.cooldownTicks - 100, c.remaining("BLUE", uav, 100));
        assertTrue(c.tryUse("RED", uav, 100), "у врага свой кулдаун");
        assertTrue(c.tryUse("BLUE", CommanderRules.Ability.SUPPLY, 100), "у способностей свои кулдауны");
        assertTrue(c.tryUse("BLUE", uav, uav.cooldownTicks));
        assertEquals(CommanderRules.Ability.CAS, CommanderRules.Ability.parse(" cas "));
        assertNull(CommanderRules.Ability.parse("nuke"));
    }

    @Test
    void uavRadiusAndCasLine() {
        assertTrue(CommanderRules.revealed(100, 0, 0, 0, 120));
        assertFalse(CommanderRules.revealed(100, 100, 0, 0, 120));
        double[][] line = CommanderRules.casLine(0, 0, 90, 25, 3); // на восток
        assertEquals(-25, line[0][0], 1e-9);
        assertEquals(0, line[1][0], 1e-9);
        assertEquals(25, line[2][0], 1e-9);
        assertEquals(0, line[2][1], 1e-9);
        assertEquals(1, CommanderRules.casLine(5, 5, 0, 10, 1).length);
    }

    @Test
    void staminaDrainsRecoversAndSways() {
        Stamina s = new Stamina();
        assertEquals(0F, s.sway());
        int ticks = 0;
        while (s.value() > 0F && ticks < 20 * 30) {
            s.tick(true, false, false);
            ticks++;
        }
        assertEquals(0F, s.value());
        assertTrue(ticks > 20 * 15 && ticks < 20 * 20, "полный бег ~18 с, а не " + ticks + " тиков");
        assertFalse(s.canSprint(), "на нуле бежать нельзя");
        assertEquals(Stamina.MAX_SWAY_DEG, s.sway(), 1e-6);
        // восстановление до порога — бег снова разрешён
        for (int i = 0; i < 20 * 3; i++) s.tick(false, false, false);
        assertFalse(s.canSprint());
        for (int i = 0; i < 20 * 2; i++) s.tick(false, true, false);
        assertTrue(s.canSprint());
        s.reset();
        s.tick(false, false, true);
        assertTrue(s.value() < 1F, "прыжок тратит");
    }
}

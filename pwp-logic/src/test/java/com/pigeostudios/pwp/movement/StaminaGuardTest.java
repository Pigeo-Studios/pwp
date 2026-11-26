package com.pigeostudios.pwp.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StaminaGuardTest {
    @Test
    void honestRunnerIsNeverPunished() {
        StaminaGuard g = new StaminaGuard();
        // бежит, пока можно, затем (как честный клиент) идёт шагом до восстановления — и снова бежит
        Stamina mirror = new Stamina(); // модель честного клиента — непрерывная, как у сервера
        for (int cycle = 0; cycle < 3; cycle++) {
            for (int i = 0; i < 20 * 60; i++) {
                boolean sprint = mirror.canSprint();
                mirror.tick(sprint, false, false);
                assertFalse(g.tick(sprint, false, false), "tick " + i);
            }
        }
    }

    @Test
    void endlessSprintIsCaughtAfterGrace() {
        StaminaGuard g = new StaminaGuard();
        boolean caught = false;
        int ticks = 0;
        while (!caught && ticks < 20 * 60) {
            caught = g.tick(true, false, false);
            ticks++;
        }
        assertTrue(caught);
        // ~18 с бега + секунда запаса
        assertTrue(ticks >= 20 * 18 + StaminaGuard.GRACE_TICKS, "ticks " + ticks);
    }

    @Test
    void stoppingResetsOverrun() {
        StaminaGuard g = new StaminaGuard();
        for (int i = 0; i < 20 * 18 + 10; i++) g.tick(true, false, false);
        assertFalse(g.tick(false, false, false));
        assertFalse(g.tick(true, false, false));
    }
}

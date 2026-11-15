package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.match.CaptureLogic.Event;
import com.pigeostudios.pwp.warfare.match.CaptureLogic.Tickets;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData.CapturePoint;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class CaptureLogicTest {
    /** deduct=30, penalty=20, gainNeut=5, gainCap=10. */
    private static CapturePoint point() {
        return new CapturePoint("A", new AABB(0, 0, 0, 1, 1, 1), 1, 1, 1, 20, 30, "CUBE", 0, 5, 10);
    }

    @Test
    void neutralPointIsCapturedAfterEnoughTicks() {
        CapturePoint p = point();
        assertEquals(Event.NONE, CaptureLogic.step(p, "BLUE", 0.5F));
        assertEquals("BLUE", p.capturingTeam);
        assertEquals(Event.CAPTURED, CaptureLogic.step(p, "BLUE", 0.5F));
        assertEquals("BLUE", p.owner);
        assertEquals(1.0F, p.progress);
    }

    @Test
    void enemyProgressOnNeutralIsDrainedFirst() {
        CapturePoint p = point();
        CaptureLogic.step(p, "RED", 0.3F);
        assertEquals(Event.NONE, CaptureLogic.step(p, "BLUE", 0.2F));
        assertEquals("RED", p.capturingTeam, "пока прогресс красных не обнулён");
        CaptureLogic.step(p, "BLUE", 0.2F);
        assertEquals("NONE", p.capturingTeam);
        assertEquals(0.0F, p.progress);
    }

    @Test
    void ownedPointIsNeutralizedButOwnerResetIsDeferred() {
        CapturePoint p = point();
        p.owner = "RED";
        p.progress = 0.4F;
        assertEquals(Event.NONE, CaptureLogic.step(p, "BLUE", 0.2F));
        assertEquals(Event.NEUTRALIZED, CaptureLogic.step(p, "BLUE", 0.2F));
        assertEquals("RED", p.owner, "владелец сбрасывается после тикетов и проверки конца игры");
        CaptureLogic.resetToNeutral(p);
        assertEquals("NEUTRAL", p.owner);
        assertEquals("NONE", p.capturingTeam);
    }

    @Test
    void ownerReinforcesUpToFull() {
        CapturePoint p = point();
        p.owner = "BLUE";
        p.progress = 0.9F;
        assertEquals(Event.NONE, CaptureLogic.step(p, "BLUE", 0.5F));
        assertEquals(1.0F, p.progress);
    }

    @Test
    void ticketsAas() {
        CapturePoint p = point();
        assertEquals(new Tickets(0, -30), CaptureLogic.onCapturedPrimary(p, "BLUE", false, 0));
        assertTrue(CaptureLogic.checkGameOverAfterCapture(p, false));
        assertEquals(new Tickets(10, 0), CaptureLogic.onCapturedGain(p, "BLUE"));
        p.owner = "RED";
        assertEquals(new Tickets(0, -20), CaptureLogic.onNeutralizedPrimary(p, false));
        assertEquals(new Tickets(5, 0), CaptureLogic.onNeutralizedGain(p, "BLUE"));
    }

    @Test
    void ticketsInvasion() {
        CapturePoint p = point();
        assertEquals(new Tickets(0, 100), CaptureLogic.onCapturedPrimary(p, "RED", true, 0), "бонус по умолчанию 100");
        assertEquals(new Tickets(250, 0), CaptureLogic.onCapturedPrimary(p, "BLUE", true, 250));
        p.owner = "BLUE";
        assertEquals(Tickets.ZERO, CaptureLogic.onNeutralizedPrimary(p, true), "в Invasion нет штрафа за потерю");
    }

    @Test
    void noDeductionNoGameOverCheckInAas() {
        CapturePoint p = new CapturePoint("B", new AABB(0, 0, 0, 1, 1, 1), 1, 1, 1, 0, 0, "CUBE", 0, 0, 0);
        assertEquals(Tickets.ZERO, CaptureLogic.onCapturedPrimary(p, "BLUE", false, 0));
        assertFalse(CaptureLogic.checkGameOverAfterCapture(p, false));
        assertEquals(Tickets.ZERO, CaptureLogic.onCapturedGain(p, "BLUE"));
    }
}

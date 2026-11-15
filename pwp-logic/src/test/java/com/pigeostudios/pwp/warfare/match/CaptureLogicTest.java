package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.match.CaptureLogic.Event;
import com.pigeostudios.pwp.warfare.match.CaptureLogic.Tickets;
import org.junit.jupiter.api.Test;

class CaptureLogicTest {
    /** Точка для тестов (в игре — WarfareWorldData.CapturePoint). */
    static final class P implements CaptureLogic.Point {
        String owner = "NEUTRAL";
        String capturingTeam = "NONE";
        float progress;
        final int penalty;
        final int deduct;
        final int gainNeut;
        final int gainCap;

        P(int penalty, int deduct, int gainNeut, int gainCap) {
            this.penalty = penalty;
            this.deduct = deduct;
            this.gainNeut = gainNeut;
            this.gainCap = gainCap;
        }

        public String owner() {
            return owner;
        }

        public void owner(String o) {
            owner = o;
        }

        public String capturingTeam() {
            return capturingTeam;
        }

        public void capturingTeam(String t) {
            capturingTeam = t;
        }

        public float progress() {
            return progress;
        }

        public void progress(float v) {
            progress = v;
        }

        public int captureDeduction() {
            return deduct;
        }

        public int ticketPenalty() {
            return penalty;
        }

        public int ticketGainNeutralize() {
            return gainNeut;
        }

        public int ticketGainCapture() {
            return gainCap;
        }
    }

    /** penalty=20, deduct=30, gainNeut=5, gainCap=10. */
    private static P point() {
        return new P(20, 30, 5, 10);
    }

    @Test
    void neutralPointIsCapturedAfterEnoughTicks() {
        P p = point();
        assertEquals(Event.NONE, CaptureLogic.step(p, "BLUE", 0.5F));
        assertEquals("BLUE", p.capturingTeam);
        assertEquals(Event.CAPTURED, CaptureLogic.step(p, "BLUE", 0.5F));
        assertEquals("BLUE", p.owner);
        assertEquals(1.0F, p.progress);
    }

    @Test
    void enemyProgressOnNeutralIsDrainedFirst() {
        P p = point();
        CaptureLogic.step(p, "RED", 0.3F);
        assertEquals(Event.NONE, CaptureLogic.step(p, "BLUE", 0.2F));
        assertEquals("RED", p.capturingTeam, "пока прогресс красных не обнулён");
        CaptureLogic.step(p, "BLUE", 0.2F);
        assertEquals("NONE", p.capturingTeam);
        assertEquals(0.0F, p.progress);
    }

    @Test
    void ownedPointIsNeutralizedButOwnerResetIsDeferred() {
        P p = point();
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
        P p = point();
        p.owner = "BLUE";
        p.progress = 0.9F;
        assertEquals(Event.NONE, CaptureLogic.step(p, "BLUE", 0.5F));
        assertEquals(1.0F, p.progress);
    }

    @Test
    void ticketsAas() {
        P p = point();
        assertEquals(new Tickets(0, -30), CaptureLogic.onCapturedPrimary(p, "BLUE", false, 0));
        assertTrue(CaptureLogic.checkGameOverAfterCapture(p, false));
        assertEquals(new Tickets(10, 0), CaptureLogic.onCapturedGain(p, "BLUE"));
        p.owner = "RED";
        assertEquals(new Tickets(0, -20), CaptureLogic.onNeutralizedPrimary(p, false));
        assertEquals(new Tickets(5, 0), CaptureLogic.onNeutralizedGain(p, "BLUE"));
    }

    @Test
    void ticketsInvasion() {
        P p = point();
        assertEquals(new Tickets(0, 100), CaptureLogic.onCapturedPrimary(p, "RED", true, 0), "бонус по умолчанию 100");
        assertEquals(new Tickets(250, 0), CaptureLogic.onCapturedPrimary(p, "BLUE", true, 250));
        p.owner = "BLUE";
        assertEquals(Tickets.ZERO, CaptureLogic.onNeutralizedPrimary(p, true), "в Invasion нет штрафа за потерю");
    }

    @Test
    void noDeductionNoGameOverCheckInAas() {
        P p = new P(0, 0, 0, 0);
        assertEquals(Tickets.ZERO, CaptureLogic.onCapturedPrimary(p, "BLUE", false, 0));
        assertFalse(CaptureLogic.checkGameOverAfterCapture(p, false));
        assertEquals(Tickets.ZERO, CaptureLogic.onCapturedGain(p, "BLUE"));
    }
}

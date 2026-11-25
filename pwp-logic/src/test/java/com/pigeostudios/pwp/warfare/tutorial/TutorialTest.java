package com.pigeostudios.pwp.warfare.tutorial;

import static org.junit.jupiter.api.Assertions.*;

import com.pigeostudios.pwp.warfare.tutorial.Tutorial.Snapshot;
import com.pigeostudios.pwp.warfare.tutorial.Tutorial.Step;
import org.junit.jupiter.api.Test;

class TutorialTest {

    @Test
    void walksThroughFirstMatchInOrder() {
        Tutorial t = new Tutorial();
        assertEquals(Step.TEAM, t.update(new Snapshot(false, false, false, false, false, false)));
        assertEquals(Step.SQUAD, t.update(new Snapshot(true, false, false, false, false, false)));
        assertEquals(Step.ROLE, t.update(new Snapshot(true, true, false, false, false, false)));
        // кит взят в ожидании — деплой возможен только в бою
        assertEquals(Step.DEPLOY, t.update(new Snapshot(true, true, true, false, false, false)));
        assertEquals(Step.DEPLOY, t.update(new Snapshot(true, true, true, true, false, false)));
        assertEquals(Step.CAPTURE, t.update(new Snapshot(true, true, true, true, true, false)));
        assertEquals(Step.DONE, t.update(new Snapshot(true, true, true, true, true, true)));
        assertTrue(t.done());
    }

    @Test
    void progressNeverGoesBack() {
        Tutorial t = new Tutorial();
        t.update(new Snapshot(true, true, false, false, false, false));
        assertEquals(Step.ROLE, t.step());
        // вышел из отряда и сменил команду — обучение не откатывается
        assertEquals(Step.ROLE, t.update(new Snapshot(false, false, false, false, false, false)));
    }

    @Test
    void skipsSeveralStepsAtOnceForVeteranState() {
        Tutorial t = new Tutorial();
        assertEquals(Step.DEPLOY, t.update(new Snapshot(true, true, true, false, false, false)));
    }

    @Test
    void numbersAndKeys() {
        assertEquals(1, Step.TEAM.number());
        assertEquals(5, Step.CAPTURE.number());
        assertEquals(5, Step.DONE.number());
        assertEquals("gui.pwpwarfare.tutorial.deploy", Step.DEPLOY.langKey());
        Tutorial t = new Tutorial(Step.SQUAD);
        t.skip();
        assertTrue(t.done());
    }
}

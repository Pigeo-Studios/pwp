package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.*;

import com.pigeostudios.pwp.warfare.match.MatchWatchdog.Action;
import com.pigeostudios.pwp.warfare.match.MatchWatchdog.Input;
import org.junit.jupiter.api.Test;

class MatchWatchdogTest {
    private static final long MIN = 60_000;

    private static Input in(long now, int online) {
        return new Input(now, online, false, false, false, 0);
    }

    @Test
    void emptyBeforeFirstJoinIsFine() {
        MatchWatchdog w = new MatchWatchdog(3 * MIN, 15 * MIN, 0);
        for (long t = 0; t < 14 * MIN; t += 1000) assertEquals(Action.NONE, w.step(in(t, 0)));
    }

    @Test
    void emptyAfterPlayersLeftShutsDownOnce() {
        MatchWatchdog w = new MatchWatchdog(3 * MIN, 15 * MIN, 0);
        assertEquals(Action.NONE, w.step(in(0, 5)));
        assertEquals(Action.NONE, w.step(in(10_000, 0)));
        assertEquals(Action.NONE, w.step(in(10_000 + 3 * MIN - 1, 0)));
        assertEquals(Action.SHUTDOWN_EMPTY, w.step(in(10_000 + 3 * MIN, 0)));
        assertEquals(Action.NONE, w.step(in(20 * MIN, 0)));
    }

    @Test
    void returningPlayerResetsEmptyTimer() {
        MatchWatchdog w = new MatchWatchdog(3 * MIN, 0, 0);
        w.step(in(0, 1));
        w.step(in(1000, 0));
        w.step(in(2 * MIN, 1));
        assertEquals(Action.NONE, w.step(in(2 * MIN + 1000, 0)));
        assertEquals(Action.NONE, w.step(in(4 * MIN, 0)));
        assertEquals(Action.SHUTDOWN_EMPTY, w.step(in(2 * MIN + 1000 + 3 * MIN, 0)));
    }

    @Test
    void noShowCountsOnlyAfterLobbySettingsArrive() {
        MatchWatchdog w = new MatchWatchdog(3 * MIN, 15 * MIN, 0);
        // предпрогрев: сервер ждёт настройки лобби 30 минут — не считается
        for (long t = 0; t < 30 * MIN; t += MIN) {
            assertEquals(Action.NONE, w.step(new Input(t, 0, true, false, false, 0)));
        }
        long ready = 30 * MIN;
        assertEquals(Action.NONE, w.step(in(ready, 0)));
        assertEquals(Action.NONE, w.step(in(ready + 15 * MIN - 1, 0)));
        assertEquals(Action.SHUTDOWN_NO_SHOW, w.step(in(ready + 15 * MIN, 0)));
    }

    @Test
    void timeLimitEndsBattleOnceAndKeepsWatching() {
        MatchWatchdog w = new MatchWatchdog(3 * MIN, 0, 120 * MIN);
        long start = 5 * MIN;
        assertEquals(Action.NONE, w.step(new Input(start + 119 * MIN, 10, false, false, true, start)));
        assertEquals(Action.END_BY_TIME, w.step(new Input(start + 120 * MIN, 10, false, false, true, start)));
        assertEquals(Action.NONE, w.step(new Input(start + 121 * MIN, 10, false, false, true, start)));
        // после конца по времени пустой сервер всё ещё выключится
        w.step(in(start + 122 * MIN, 0));
        assertEquals(Action.SHUTDOWN_EMPTY, w.step(in(start + 125 * MIN, 0)));
    }

    @Test
    void shuttingDownAndDisabledRulesDoNothing() {
        MatchWatchdog off = new MatchWatchdog(0, 0, 0);
        off.step(in(0, 1));
        assertEquals(Action.NONE, off.step(in(100 * MIN, 0)));
        MatchWatchdog w = new MatchWatchdog(MIN, MIN, MIN);
        w.step(in(0, 1));
        assertEquals(Action.NONE, w.step(new Input(10 * MIN, 0, false, true, true, 1)));
    }

    @Test
    void winnerOnTime() {
        assertTrue(MatchWatchdog.blueWinsOnTime(100, 50));
        assertFalse(MatchWatchdog.blueWinsOnTime(10, 50));
        assertTrue(MatchWatchdog.blueWinsOnTime(40, 40));
    }
}

package com.pwp.api.lobby;

import static org.junit.jupiter.api.Assertions.*;

import com.pwp.api.lobby.LobbyCycle.Action;
import com.pwp.api.lobby.LobbyCycle.Observed;
import com.pwp.api.lobby.LobbyCycle.Phase;
import org.junit.jupiter.api.Test;

class LobbyCycleTest {
    private static final LobbyCycle.Config CFG = new LobbyCycle.Config(1000, 500, 300, 1);

    private static Observed booting() {
        return new Observed(true, false, false, 0, 0, null);
    }

    private static Observed loaded(boolean applied, int done, int total) {
        return new Observed(true, true, applied, done, total, null);
    }

    @Test
    void happyPathPrewarmThenVotesThenTransfer() {
        LobbyCycle c = new LobbyCycle(CFG);
        assertEquals(Action.NONE, c.step(0, booting(), false)); // IDLE ничего не делает
        c.launched(0, false);
        assertEquals(Action.NONE, c.step(100, booting(), false));
        assertEquals(Phase.PREPARING, c.phase());
        assertEquals(Action.NONE, c.step(200, loaded(false, 0, 10), false));
        assertEquals(Phase.WAITING_VOTES, c.phase());
        // голосования закончились — доставить настройки
        assertEquals(Action.DELIVER_SETTINGS, c.step(5000, loaded(false, 3, 10), true));
        c.settingsDelivered(5000);
        assertEquals(Phase.FINISHING, c.phase());
        assertEquals(Action.NONE, c.step(5100, loaded(true, 5, 10), true));
        assertEquals(Action.TRANSFER, c.step(5200, loaded(true, 10, 10), true));
        assertEquals(Phase.TRANSFERRING, c.phase());
        assertEquals(Action.NONE, c.step(5300, loaded(true, 10, 10), true));
    }

    @Test
    void prerenderGraceCapsWaiting() {
        LobbyCycle c = new LobbyCycle(CFG);
        c.launched(0, false);
        c.settingsDelivered(0);
        assertEquals(Action.NONE, c.step(100, loaded(true, 1, 100), true));
        assertEquals(Action.NONE, c.step(399, loaded(true, 2, 100), true));
        assertEquals(Action.TRANSFER, c.step(400, loaded(true, 3, 100), true));
    }

    @Test
    void noPrerenderMeansReadyImmediately() {
        LobbyCycle c = new LobbyCycle(CFG);
        c.launched(0, false);
        c.settingsDelivered(0);
        assertEquals(Action.TRANSFER, c.step(10, loaded(true, 0, 0), true));
    }

    @Test
    void deathRetriesOnceThenAborts() {
        LobbyCycle c = new LobbyCycle(CFG);
        c.launched(0, false);
        Observed dead = new Observed(false, false, false, 0, 0, null);
        assertEquals(Action.RETRY, c.step(10, dead, false));
        assertEquals(LobbyCycle.ERR_DIED, c.error());
        c.launched(20, true);
        assertNull(c.error());
        assertEquals(1, c.retries());
        assertEquals(Action.ABORT, c.step(30, dead, false));
        assertEquals(Phase.FAILED, c.phase());
        assertEquals(Action.NONE, c.step(40, dead, false));
        // новый цикл — повторы с нуля
        c.launched(50, false);
        assertEquals(0, c.retries());
    }

    @Test
    void bootTimeout() {
        LobbyCycle c = new LobbyCycle(new LobbyCycle.Config(1000, 500, 300, 0));
        c.launched(0, false);
        assertEquals(Action.NONE, c.step(1000, booting(), false));
        assertEquals(Action.ABORT, c.step(1001, booting(), false));
        assertEquals(LobbyCycle.ERR_BOOT_TIMEOUT, c.error());
    }

    @Test
    void applyTimeoutCountsFromLaterOfDeliveryAndLoad() {
        LobbyCycle c = new LobbyCycle(new LobbyCycle.Config(10_000, 500, 300, 0));
        c.launched(0, false);
        // настройки готовы раньше, чем мир — доставляем сразу
        assertEquals(Action.DELIVER_SETTINGS, c.step(10, booting(), true));
        c.settingsDelivered(10);
        assertEquals(Action.NONE, c.step(3000, booting(), true)); // мир ещё грузится — не таймаут
        assertEquals(Action.NONE, c.step(4000, loaded(false, 0, 0), true)); // загрузился в 4000
        assertEquals(Action.NONE, c.step(4500, loaded(false, 0, 0), true));
        assertEquals(Action.ABORT, c.step(4501, loaded(false, 0, 0), true));
        assertEquals(LobbyCycle.ERR_APPLY_TIMEOUT, c.error());
    }

    @Test
    void serverErrorFails() {
        LobbyCycle c = new LobbyCycle(new LobbyCycle.Config(1000, 500, 300, 0));
        c.launched(0, false);
        c.settingsDelivered(0);
        assertEquals(Action.ABORT, c.step(10, new Observed(true, true, false, 0, 0, "bad_settings"), true));
        assertEquals(LobbyCycle.ERR_SERVER, c.error());
    }

    @Test
    void autoVoteRespectsMinPlayersAndCooldown() {
        assertTrue(LobbyCycle.autoVoteAllowed(true, false, 2, 2, 100, 100));
        assertFalse(LobbyCycle.autoVoteAllowed(true, false, 1, 2, 100, 0));
        assertFalse(LobbyCycle.autoVoteAllowed(false, false, 5, 2, 100, 0));
        assertFalse(LobbyCycle.autoVoteAllowed(true, true, 5, 2, 100, 0));
        assertFalse(LobbyCycle.autoVoteAllowed(true, false, 5, 2, 99, 100));
        assertFalse(LobbyCycle.autoVoteAllowed(true, false, 0, 0, 100, 0)); // пустое лобби — никогда
    }

    @Test
    void percent() {
        assertEquals(0, LobbyCycle.percent(0, 1000, null));
        assertEquals(25, LobbyCycle.percent(500, 1000, booting()));
        assertEquals(49, LobbyCycle.percent(5000, 1000, booting())); // до загрузки мира не выше 49
        assertEquals(50, LobbyCycle.percent(0, 1000, loaded(false, 0, 10)));
        assertEquals(75, LobbyCycle.percent(0, 1000, loaded(false, 5, 10)));
        assertEquals(100, LobbyCycle.percent(0, 1000, loaded(false, 0, 0)));
        assertEquals(100, LobbyCycle.percent(0, 1000, loaded(false, 10, 10)));
    }
}

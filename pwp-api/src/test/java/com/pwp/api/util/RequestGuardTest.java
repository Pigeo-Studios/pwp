package com.pwp.api.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RequestGuardTest {
    private final AtomicLong now = new AtomicLong(1_000);
    private final RequestGuard guard = new RequestGuard(500, now::get);
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void secondRequestWhileInFlightIsRejected() {
        assertTrue(guard.tryAcquire(a, "equip"));
        now.addAndGet(10_000); // даже после кулдауна — пока первый не завершён
        assertFalse(guard.tryAcquire(a, "equip"));
        guard.release(a, "equip");
        assertTrue(guard.tryAcquire(a, "equip"));
    }

    @Test
    void cooldownAppliesAfterRelease() {
        assertTrue(guard.tryAcquire(a, "equip"));
        guard.release(a, "equip");
        now.addAndGet(499);
        assertFalse(guard.tryAcquire(a, "equip"));
        now.addAndGet(1);
        assertTrue(guard.tryAcquire(a, "equip"));
    }

    @Test
    void keysAndPlayersAreIndependent() {
        assertTrue(guard.tryAcquire(a, "equip"));
        assertTrue(guard.tryAcquire(a, "skins"));
        assertTrue(guard.tryAcquire(b, "equip"));
    }

    @Test
    void forgetClearsPlayer() {
        assertTrue(guard.tryAcquire(a, "equip"));
        guard.forget(a);
        assertTrue(guard.tryAcquire(a, "equip"));
    }
}

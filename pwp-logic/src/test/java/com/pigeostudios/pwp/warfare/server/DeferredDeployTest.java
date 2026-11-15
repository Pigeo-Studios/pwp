package com.pigeostudios.pwp.warfare.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DeferredDeployTest {
    private final DeferredDeploy waiting = new DeferredDeploy("MAIN", "BLUE", true, "Medic");

    @Test
    void sameStateDeploys() {
        assertNull(waiting.cancelReason(new DeferredDeploy("MAIN", "BLUE", true, "Medic")));
    }

    @Test
    void teamChangeCancels() {
        assertNotNull(waiting.cancelReason(new DeferredDeploy("MAIN", "RED", true, "Medic")));
    }

    @Test
    void alreadyOnFieldCancels() {
        assertNotNull(waiting.cancelReason(new DeferredDeploy("MAIN", "BLUE", false, "Medic")));
    }

    @Test
    void kitChangeCancels() {
        assertNotNull(waiting.cancelReason(new DeferredDeploy("MAIN", "BLUE", true, "Rifleman")));
    }

    @Test
    void nonSpectatorWaitingIsNotCancelledBySpectatorFlag() {
        DeferredDeploy alive = new DeferredDeploy("MAIN", "BLUE", false, "Medic");
        assertNull(alive.cancelReason(new DeferredDeploy("MAIN", "BLUE", false, "Medic")));
    }
}

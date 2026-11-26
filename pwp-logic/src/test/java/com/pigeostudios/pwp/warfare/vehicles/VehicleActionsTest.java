package com.pigeostudios.pwp.warfare.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.vehicles.VehicleActions.Action;
import com.pigeostudios.pwp.warfare.vehicles.VehicleActions.State;
import java.util.List;
import org.junit.jupiter.api.Test;

class VehicleActionsTest {
    private static State sl(int squad, int claim, boolean locked, boolean pending) {
        return new State(true, true, squad, claim, claim != 0, locked, false, pending);
    }

    @Test
    void leaderClaimsFreeVehicle() {
        assertEquals(List.of(Action.CLAIM), VehicleActions.available(sl(3, 0, false, false)));
    }

    @Test
    void ownerLeaderLocksAndReleases() {
        assertEquals(List.of(Action.LOCK, Action.RELEASE), VehicleActions.available(sl(3, 3, false, false)));
        assertEquals(List.of(Action.UNLOCK, Action.RELEASE), VehicleActions.available(sl(3, 3, true, false)));
    }

    @Test
    void otherLeaderCannotTouchClaimedVehicle() {
        assertTrue(VehicleActions.available(sl(4, 3, false, false)).isEmpty());
        assertFalse(VehicleActions.allowed(sl(4, 3, false, false), Action.RELEASE));
    }

    @Test
    void leaderSeesAnswersOnlyWithPendingRequests() {
        assertTrue(VehicleActions.available(sl(3, 3, false, true)).containsAll(List.of(Action.APPROVE, Action.DENY)));
        assertFalse(VehicleActions.available(sl(3, 3, false, false)).contains(Action.APPROVE));
    }

    @Test
    void memberRequestsDriveOnlyForFreshSpecialistVehicle() {
        State fresh = new State(true, false, 3, 0, false, false, true, false);
        assertEquals(List.of(Action.REQUEST_DRIVE), VehicleActions.available(fresh));
        State used = new State(true, false, 3, 0, false, false, false, false);
        assertTrue(VehicleActions.available(used).isEmpty());
        State foreignClaim = new State(true, false, 3, 5, true, false, true, false);
        assertTrue(VehicleActions.available(foreignClaim).isEmpty());
    }

    @Test
    void enemyVehicleOrNoSquadGivesNothing() {
        assertTrue(VehicleActions.available(new State(false, true, 3, 0, false, false, true, true))
                .isEmpty());
        assertTrue(VehicleActions.available(new State(true, true, 0, 0, false, false, true, true))
                .isEmpty());
    }

    @Test
    void parseIsCaseInsensitiveAndRejectsJunk() {
        assertEquals(Action.REQUEST_DRIVE, Action.parse("request_drive"));
        assertEquals(null, Action.parse("explode"));
        assertEquals("request_drive", Action.REQUEST_DRIVE.id());
    }

    @Test
    void lockedVehicleKeepsStrangersOutOfAnySeat() {
        assertEquals(VehicleClaim.Decision.DENY, VehicleClaim.decideSeat(3, true, 4, false, true, false));
        assertEquals(VehicleClaim.Decision.ALLOW, VehicleClaim.decideSeat(3, true, 3, false, true, false));
        assertEquals(VehicleClaim.Decision.ALLOW, VehicleClaim.decideSeat(3, true, 4, true, true, false));
        assertEquals(VehicleClaim.Decision.ALLOW, VehicleClaim.decideSeat(3, true, 4, false, false, false));
        // отряд распался — замок не действует
        assertEquals(VehicleClaim.Decision.ALLOW, VehicleClaim.decideSeat(3, false, 4, false, true, false));
        // водитель — прежнее правило
        assertEquals(VehicleClaim.Decision.DENY, VehicleClaim.decideSeat(3, true, 4, false, false, true));
    }

    @Test
    void driveRequestsExpireAndAreForgotten() {
        DriveRequests r = new DriveRequests();
        r.add("veh", "p1", "Alice", 0);
        r.add("veh", "p2", "Bob", 10_000);
        assertEquals(2, r.pending("veh", 20_000).size());
        assertEquals("Alice", r.pending("veh", 20_000).get(0).playerName());
        assertEquals(1, r.pending("veh", DriveRequests.TTL_MS + 1).size());
        assertTrue(r.has("veh", "p2", DriveRequests.TTL_MS + 1));
        r.remove("veh", "p2");
        assertTrue(r.pending("veh", 0).isEmpty());
        assertEquals(0, r.vehicles());
        r.add("a", "p1", "Alice", 0);
        r.add("b", "p1", "Alice", 0);
        r.forgetPlayer("p1");
        assertEquals(0, r.vehicles());
    }
}

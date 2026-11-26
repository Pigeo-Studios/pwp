package com.pigeostudios.pwp.warfare.vehicles;

import static com.pigeostudios.pwp.warfare.vehicles.VehicleClaim.Decision.ALLOW;
import static com.pigeostudios.pwp.warfare.vehicles.VehicleClaim.Decision.CLAIM;
import static com.pigeostudios.pwp.warfare.vehicles.VehicleClaim.Decision.DENY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VehicleClaimTest {
    @Test
    void freeVehicleIsClaimedBySquad() {
        assertEquals(CLAIM, VehicleClaim.decide(0, false, 3, false));
        assertEquals(ALLOW, VehicleClaim.decide(0, false, 0, false));
    }

    @Test
    void claimedVehicleOnlyForOwnerSquadOrStaff() {
        assertEquals(ALLOW, VehicleClaim.decide(3, true, 3, false));
        assertEquals(DENY, VehicleClaim.decide(3, true, 4, false));
        assertEquals(DENY, VehicleClaim.decide(3, true, 0, false));
        assertEquals(ALLOW, VehicleClaim.decide(3, true, 4, true));
    }

    @Test
    void disbandedOwnerFreesVehicle() {
        assertEquals(CLAIM, VehicleClaim.decide(3, false, 4, false));
        assertEquals(ALLOW, VehicleClaim.decide(3, false, 0, false));
    }
}

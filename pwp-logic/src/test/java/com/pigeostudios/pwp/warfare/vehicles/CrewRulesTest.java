package com.pigeostudios.pwp.warfare.vehicles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Те же ответы, что у прежних четырёх копий правила (InteractionEvents, VehicleLossEvents, запрос, HUD). */
class CrewRulesTest {
    @Test
    void specialistTypes() {
        for (String t : new String[] {"TANK", "apc", "Mobile ZU", "HELICOPTER", "CAS_FIGHTER", "Supply Helicopter"}) {
            assertTrue(CrewRules.isSpecialist(t), t);
        }
        for (String t : new String[] {"TRANSPORT", "DEFAULT", "", null}) assertFalse(CrewRules.isSpecialist(t));
    }

    @Test
    void kitsPerType() {
        assertTrue(CrewRules.hasCorrectKit("HELICOPTER", "Pilot"));
        assertTrue(CrewRules.hasCorrectKit("CAS_HELI", "Pilot Officer"));
        assertFalse(CrewRules.hasCorrectKit("HELICOPTER", "Mechanic"));
        assertTrue(CrewRules.hasCorrectKit("TANK", "Mechanic Officer"));
        assertFalse(CrewRules.hasCorrectKit("APC", "Rifleman"));
        assertFalse(CrewRules.hasCorrectKit("Mobile ZU", null));
        assertTrue(CrewRules.hasCorrectKit("TRANSPORT", "Rifleman"));
    }
}

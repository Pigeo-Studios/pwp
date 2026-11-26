package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pigeostudios.pwp.warfare.match.LookPrompt.Context;
import com.pigeostudios.pwp.warfare.match.LookPrompt.Key;
import com.pigeostudios.pwp.warfare.match.LookPrompt.Target;
import org.junit.jupiter.api.Test;

class LookPromptTest {
    private static final Context EMPTY = new Context(false, true, false, false);
    private static final Context SHOVEL = new Context(true, false, false, false);

    @Test
    void fobDependsOnHand() {
        assertEquals(Key.USE, LookPrompt.of(Target.OWN_FOB, EMPTY).key());
        assertEquals("dismantle", LookPrompt.of(Target.OWN_FOB, SHOVEL).action());
        assertNull(LookPrompt.of(Target.OWN_FOB, new Context(false, false, false, false))
                .key());
        assertEquals(Key.HOLD_USE, LookPrompt.of(Target.ENEMY_FOB, SHOVEL).key());
        assertNull(LookPrompt.of(Target.ENEMY_FOB, EMPTY).key());
    }

    @Test
    void structures() {
        assertEquals("build", LookPrompt.of(Target.OWN_BLUEPRINT, SHOVEL).action());
        assertEquals(
                "need_shovel_build", LookPrompt.of(Target.OWN_BLUEPRINT, EMPTY).action());
        assertNull(LookPrompt.of(Target.OWN_STRUCTURE, EMPTY));
        assertEquals("dig", LookPrompt.of(Target.ENEMY_STRUCTURE, SHOVEL).action());
    }

    @Test
    void vehicleAndAmmoHaveSecondAction() {
        var v = LookPrompt.of(Target.OWN_VEHICLE, EMPTY);
        assertEquals(Key.VEHICLE_MENU, v.alt2Key());
        assertEquals(Key.SHIFT_USE, LookPrompt.of(Target.AMMO_BAG, EMPTY).altKey());
    }

    @Test
    void downedAlly() {
        assertEquals(
                Key.HOLD_USE,
                LookPrompt.of(Target.DOWNED_ALLY, new Context(false, false, true, true))
                        .key());
        assertEquals(
                "take_medkit",
                LookPrompt.of(Target.DOWNED_ALLY, new Context(false, true, true, false))
                        .action());
        var notMedic = LookPrompt.of(Target.DOWNED_ALLY, EMPTY);
        assertNotNull(notMedic);
        assertEquals(Key.TACTICAL, notMedic.altKey());
        assertNull(LookPrompt.of(Target.NONE, EMPTY));
    }

    @Test
    void centralizedVehicleHints() {
        var own = LookPrompt.of(Target.OWN_VEHICLE, EMPTY);
        assertEquals("enter", own.action());
        assertEquals(Key.SHIFT_USE, own.altKey()); // прежняя подсказка «Шифт+ПКМ — пополнить БК»
        assertEquals(Key.VEHICLE_MENU, own.alt2Key());
        var noKit = LookPrompt.of(Target.OWN_VEHICLE, new Context(false, true, false, false, true));
        assertEquals("need_crew_kit", noKit.action());
        assertNull(noKit.key());
        org.junit.jupiter.api.Assertions.assertTrue(noKit.warn());
        org.junit.jupiter.api.Assertions.assertTrue(
                LookPrompt.of(Target.ENEMY_VEHICLE, EMPTY).warn());
    }

    @Test
    void materialsShownOnFobAndCrateOnly() {
        org.junit.jupiter.api.Assertions.assertTrue(
                LookPrompt.of(Target.OWN_FOB, SHOVEL).materials());
        org.junit.jupiter.api.Assertions.assertTrue(
                LookPrompt.of(Target.SUPPLY_CRATE, EMPTY).materials());
        org.junit.jupiter.api.Assertions.assertFalse(
                LookPrompt.of(Target.AMMO_BAG, EMPTY).materials());
        assertEquals("main_supply", LookPrompt.of(Target.MAIN_SUPPLY, EMPTY).action());
    }
}

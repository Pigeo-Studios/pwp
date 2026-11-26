package com.pwp.api.cosmetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class WeaponSkinTest {
    @Test
    void parsesDisplaySkinAndRejectsLegacyOrBroken() {
        WeaponSkin s = WeaponSkin.parse("ak_gold", "WEAPON", "cib:ak74", "display:cib:ak74_gold_display");
        assertEquals(new WeaponSkin("ak_gold", "cib:ak74", "cib:ak74_gold_display"), s);
        assertNull(WeaponSkin.parse("old", "PRIMARY", "ak47", "{id:\"tacz:modern_kinetic_gun\"}"), "предмет-замена");
        assertNull(WeaponSkin.parse("old2", "PRIMARY", "any", "tacz:ammo"));
        assertNull(WeaponSkin.parse("bad", "WEAPON", "AK 74", "display:cib:x"), "битый GunId");
        assertNull(WeaponSkin.parse("bad2", "WEAPON", "cib:ak74", "display:"), "пустой display");
        assertNull(WeaponSkin.parse(null, "WEAPON", "cib:ak74", "display:cib:x"));
        assertEquals("display:cib:x", WeaponSkin.modelPath("cib:x"));
    }

    @Test
    void idsAreResourceLocationsUpToLimit() {
        assertTrue(WeaponSkin.validId("maxstuff:m320t"));
        assertTrue(WeaponSkin.validId("cib:guns/ak-74_v2"));
        assertFalse(WeaponSkin.validId("noColon"));
        assertFalse(WeaponSkin.validId("Cib:AK"));
        assertFalse(WeaponSkin.validId("a:" + "x".repeat(WeaponSkin.MAX_ID)));
    }

    @Test
    void decideSetsClearsAndLeavesNativeDisplay() {
        Map<String, String> eq = Map.of("cib:ak74", "cib:ak74_gold_display");
        assertEquals(WeaponSkin.Action.SET, WeaponSkin.decide(eq, "cib:ak74", "", false));
        assertEquals(WeaponSkin.Action.NONE, WeaponSkin.decide(eq, "cib:ak74", "cib:ak74_gold_display", true));
        // тот же display, но поставлен не нами — пометить (SET), чтобы потом корректно снять
        assertEquals(WeaponSkin.Action.SET, WeaponSkin.decide(eq, "cib:ak74", "cib:ak74_gold_display", false));
        // другой ствол без скина: наш display снимаем, родной пака не трогаем
        assertEquals(WeaponSkin.Action.CLEAR, WeaponSkin.decide(eq, "cib:m4a1", "cib:ak74_gold_display", true));
        assertEquals(WeaponSkin.Action.NONE, WeaponSkin.decide(eq, "cib:m4a1", "cib:m4a1_tan_display", false));
        assertEquals(WeaponSkin.Action.CLEAR, WeaponSkin.decide(Map.of(), "cib:ak74", "x:y", true));
        assertEquals(WeaponSkin.Action.NONE, WeaponSkin.decide(eq, "", "", false));
    }
}

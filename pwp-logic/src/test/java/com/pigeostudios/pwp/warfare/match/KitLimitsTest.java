package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.match.KitLimits.Holder;
import com.pigeostudios.pwp.warfare.match.KitLimits.Input;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KitLimitsTest {
    private static Input in(int team, int squad, int minSq, String cat, int tc, int sc, int size, int fs) {
        return new Input("Rifleman", team, squad, minSq, cat, false, false, tc, sc, size, fs);
    }

    /** Проверка «как на сервере»: счётчики из списка других игроков. */
    private static String check(String kit, int maxTeam, boolean leaderOnly, boolean isLeader, List<Holder> others) {
        KitLimits.Occupancy occ = KitLimits.occupancy(others, kit);
        return KitLimits.unavailableReason(
                new Input(kit, maxTeam, -1, 0, "SUPPORT", leaderOnly, isLeader, occ.team(), occ.squad(), 4, 0));
    }

    @Test
    void teamLimit() {
        assertNull(KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 5, 0, 4, 0)));
        assertEquals("Team Full (6/6)", KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 6, 0, 4, 0)));
    }

    @Test
    void squadLimitCheckedAfterTeam() {
        assertEquals("Squad Full (2/2)", KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 3, 2, 4, 0)));
        assertEquals("Team Full (6/6)", KitLimits.unavailableReason(in(6, 2, 0, "DIRECT_COMBAT", 6, 2, 4, 0)));
    }

    @Test
    void unlimitedWhenNegative() {
        assertNull(KitLimits.unavailableReason(in(-1, 0, 0, "SUPPORT", 100, 100, 9, 0)));
    }

    @Test
    void zeroTeamLimitDisablesRole() {
        assertEquals("Role Disabled", KitLimits.unavailableReason(in(0, 0, 0, "SUPPORT", 0, 0, 9, 0)));
    }

    @Test
    void minSquadPlayers() {
        assertEquals("Need 4 players in Squad", KitLimits.unavailableReason(in(-1, 0, 4, "SPECIALIST", 0, 0, -1, 0)));
        assertEquals("Need 4 players in Squad", KitLimits.unavailableReason(in(-1, 0, 4, "SPECIALIST", 0, 0, 3, 0)));
        assertNull(KitLimits.unavailableReason(in(-1, 0, 4, "SPECIALIST", 0, 0, 4, 0)));
    }

    @Test
    void fireSupportCapPerSquad() {
        assertNull(KitLimits.unavailableReason(in(-1, 0, 0, "FIRE_SUPPORT", 0, 0, 9, 2)));
        assertEquals(
                "Max 3 Fire Support per Squad", KitLimits.unavailableReason(in(-1, 0, 0, "FIRE_SUPPORT", 0, 0, 9, 3)));
        assertTrue(KitLimits.fireSupportCapReached("FIRE_SUPPORT", 3));
        assertFalse(KitLimits.fireSupportCapReached("SUPPORT", 10), "лимит только для FIRE_SUPPORT");
    }

    @Test
    void leaderOnlyRole() {
        assertEquals("Squad Leader only", check("Officer", 6, true, false, List.of()));
        assertNull(check("Officer", 6, true, true, List.of()));
        assertNull(check("Rifleman", 6, false, false, List.of()), "обычная роль — без требования лидерства");
    }

    @Test
    void unassignedAlwaysAvailable() {
        Input full = new Input(KitLimits.UNASSIGNED, 0, 1, 9, "FIRE_SUPPORT", true, false, 99, 99, -1, 9);
        assertNull(KitLimits.unavailableReason(full));
    }

    @Test
    void occupancyCountsCurrentOrPending() {
        List<Holder> others = List.of(
                new Holder("Medic", "", true), // текущая роль
                new Holder("Rifleman", "Medic", false), // ожидает роль
                new Holder("Medic", "Rifleman", true), // уходит с роли, но место пока держит
                new Holder("", "", true));
        KitLimits.Occupancy occ = KitLimits.occupancy(others, "Medic");
        assertEquals(3, occ.team());
        assertEquals(2, occ.squad());
    }

    @Test
    void raceOfTwoPendingNeverExceedsLimit() {
        // Лимит 1: A выбрал роль первым — B при выборе видит занятое место
        assertNull(check("HAT", 1, false, false, List.of()), "A: место свободно");
        assertEquals("Team Full (1/1)", check("HAT", 1, false, false, List.of(new Holder("", "HAT", false))));

        // Оба уже ожидают роль (лимит снизили) — при спавне проходит не больше одного
        String a = check("HAT", 1, false, false, List.of(new Holder("", "HAT", false)));
        assertNotNull(a, "A при спавне видит ожидание B");
        // A получил Unassigned, ожидание снято — теперь B проходит
        String b = check("HAT", 1, false, false, List.of(new Holder(KitLimits.UNASSIGNED, "", false)));
        assertNull(b);
    }

    @Test
    void fireSupportUsesEffectiveKitOfSquadmates() {
        Map<String, String> cat = Map.of("LMG", "FIRE_SUPPORT", "HMG", "FIRE_SUPPORT", "Medic", "SUPPORT");
        List<Holder> others = List.of(
                new Holder("Medic", "LMG", true), // ожидает FIRE_SUPPORT
                new Holder("HMG", "", true),
                new Holder("LMG", "Medic", true), // уходит с FIRE_SUPPORT
                new Holder("LMG", "", false)); // другой отряд
        assertEquals(2, KitLimits.fireSupportInSquad(others, k -> cat.getOrDefault(k, "")));
    }

    @Test
    void reasonCarriesLangKeyAndArgs() {
        KitLimits.Reason r = KitLimits.reasonOf(in(6, 2, 0, "DIRECT_COMBAT", 6, 0, 4, 0));
        assertNotNull(r);
        assertEquals("pwpwarfare.kit.reason.team_full", r.key());
        assertEquals("pwpwarfare.kit.reason.team_full|6|6", r.encoded());
        String[] parts = KitLimits.Reason.decode(r.encoded());
        assertEquals(3, parts.length);
        assertEquals("pwpwarfare.kit.reason.team_full", parts[0]);
        assertEquals("6", parts[2]);
        assertNull(KitLimits.Reason.decode("Team Full (6/6)"));
        assertEquals("pwpwarfare.kit.reason.leader_only", new KitLimits.Reason("leader_only").encoded());
        assertEquals(
                "pwpwarfare.kit.reason.fire_support|3",
                KitLimits.reasonOf(in(-1, 0, 0, "FIRE_SUPPORT", 0, 0, 9, 3)).encoded());
    }
}

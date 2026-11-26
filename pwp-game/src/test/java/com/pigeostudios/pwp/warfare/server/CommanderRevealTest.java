package com.pigeostudios.pwp.warfare.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Разведка командира отдаёт врагов обезличенными: ни ника, ни UUID, ни отряда, ни техники (ревью №2). */
class CommanderRevealTest {
    private static MapPlayerInfo player(String name, String team, double x, double z) {
        return new MapPlayerInfo(name, UUID.randomUUID(), x, z, 45F, 3, true, true, 99L, true, 77, 1, team);
    }

    @Test
    void revealedEnemiesCarryOnlyPositionAndTeam() {
        MapPlayerInfo enemy = player("EnemyNick", "RED", 10, 10);
        MapPlayerInfo far = player("FarEnemy", "RED", 1000, 1000);
        MapPlayerInfo ally = player("Ally", "BLUE", 0, 0);
        var reveal = new CommanderAbilities.Reveal("BLUE", 0, 0, 120, 1000);

        List<MapPlayerInfo> out = CommanderAbilities.reveal("BLUE", List.of(enemy, far, ally), List.of(reveal));

        assertEquals(1, out.size());
        MapPlayerInfo r = out.get(0);
        assertEquals(10, r.x);
        assertEquals(10, r.z);
        assertEquals("RED", r.team);
        assertFalse(r.name.contains("Enemy"));
        assertNotEquals(enemy.uuid, r.uuid);
        assertEquals(-1, r.squadId);
        assertFalse(r.isLeader);
        assertFalse(r.inVehicle);
        assertFalse(r.isDowned);
    }

    @Test
    void namesAreUniqueAndEnemyCountedOnceForOverlappingReveals() {
        var a = new CommanderAbilities.Reveal("BLUE", 0, 0, 120, 1000);
        var b = new CommanderAbilities.Reveal("BLUE", 5, 5, 120, 1000);
        List<MapPlayerInfo> out = CommanderAbilities.reveal(
                "BLUE", List.of(player("e1", "RED", 1, 1), player("e2", "RED", 2, 2)), List.of(a, b));
        assertEquals(2, out.size());
        assertNotEquals(out.get(0).name, out.get(1).name);
        assertTrue(out.get(0).name.startsWith("?"));
    }
}

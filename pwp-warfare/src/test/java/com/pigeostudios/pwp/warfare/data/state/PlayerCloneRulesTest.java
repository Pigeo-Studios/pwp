package com.pigeostudios.pwp.warfare.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class PlayerCloneRulesTest {
    private static CompoundTag oldPlayer() {
        CompoundTag t = new CompoundTag();
        WarfareKeys.SQUAD_ID.write(t, 4);
        WarfareKeys.CURRENT_KIT.write(t, "Medic");
        WarfareKeys.LAST_FOB_RESUPPLY.write(t, 1234L);
        WarfareKeys.DRONE_DEPLOY_CD.write(t, 99_000L);
        WarfareKeys.TEAM_KILLS.write(t, 2);
        WarfareKeys.BASE_RESTRICTED_UNTIL.write(t, 50_000L);
        WarfareKeys.IS_DOWNED.write(t, true);
        t.putInt(WarfareMigration.MIGRATED_MARKER, 1);
        return t;
    }

    @Test
    void deathKeepsOnlySquadKitsAndResupply() {
        CompoundTag to = new CompoundTag();
        PlayerCloneRules.copy(oldPlayer(), to, true);
        assertEquals(4, WarfareKeys.SQUAD_ID.read(to));
        assertEquals("Medic", WarfareKeys.CURRENT_KIT.read(to));
        assertEquals(1234L, WarfareKeys.LAST_FOB_RESUPPLY.read(to));
        assertFalse(to.contains(WarfareKeys.IS_DOWNED.name()));
        assertFalse(to.contains(WarfareKeys.DRONE_DEPLOY_CD.name()));
        assertFalse(to.contains(WarfareKeys.TEAM_KILLS.name()));
        assertTrue(to.contains(WarfareMigration.MIGRATED_MARKER), "маркер миграции переживает смерть");
    }

    @Test
    void endReturnKeepsEverything() {
        CompoundTag to = new CompoundTag();
        PlayerCloneRules.copy(oldPlayer(), to, false);
        assertEquals(99_000L, WarfareKeys.DRONE_DEPLOY_CD.read(to), "кулдаун дрона не сбрасывается порталом");
        assertEquals(2, WarfareKeys.TEAM_KILLS.read(to));
        assertEquals(50_000L, WarfareKeys.BASE_RESTRICTED_UNTIL.read(to));
        assertEquals(4, WarfareKeys.SQUAD_ID.read(to));
    }

    @Test
    void endReturnDropsDroneChannelsAndMainZone() {
        CompoundTag from = oldPlayer();
        WarfareKeys.DRONE_DEPLOY_TYPE.write(from, "fpv");
        WarfareKeys.DRONE_DEPLOY_START.write(from, 500L);
        WarfareKeys.DRONE_DEPLOY_X.write(from, 1.5);
        WarfareKeys.DRONE_DEPLOY_ENTITY.write(from, 77);
        WarfareKeys.DRONE_RELOAD_TYPE.write(from, "mavic");
        WarfareKeys.DRONE_RELOAD_START.write(from, 600L);
        WarfareKeys.DRONE_RELOAD_Z.write(from, -3.0);
        WarfareKeys.DRONE_RELOAD_ENTITY.write(from, 78);
        WarfareKeys.IN_MAIN_ZONE.write(from, true);
        CompoundTag to = new CompoundTag();
        PlayerCloneRules.copy(from, to, false);
        for (WarfareKey<?> k : PlayerCloneRules.RESET_ON_END_RETURN) {
            assertFalse(to.contains(k.name()), k.name() + " не переносится из Энда");
        }
        assertEquals(99_000L, WarfareKeys.DRONE_DEPLOY_CD.read(to), "кулдаун деплоя дрона остаётся");
        assertEquals(4, WarfareKeys.SQUAD_ID.read(to));
    }

    @Test
    void deathMirrorsSurvivingKeysToLegacyPersistentData() {
        CompoundTag cap = new CompoundTag();
        PlayerCloneRules.copy(oldPlayer(), cap, true);
        CompoundTag persistent = new CompoundTag();
        PlayerCloneRules.mirrorToLegacy(cap, persistent);
        assertEquals(4, WarfareKeys.SQUAD_ID.read(persistent), "старый мод после отката видит отряд");
        assertEquals("Medic", WarfareKeys.CURRENT_KIT.read(persistent));
        assertEquals(1234L, WarfareKeys.LAST_FOB_RESUPPLY.read(persistent));
        assertFalse(persistent.contains(WarfareKeys.IS_DOWNED.name()));
        assertFalse(persistent.contains(WarfareMigration.MIGRATED_MARKER), "маркер — только в capability");
    }
}

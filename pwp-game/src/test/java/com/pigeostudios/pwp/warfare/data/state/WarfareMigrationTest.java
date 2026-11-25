package com.pigeostudios.pwp.warfare.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class WarfareMigrationTest {

    @Test
    void movesAllWarfareKeysAndKeepsForeignOnes() {
        CompoundTag persistent = new CompoundTag();
        persistent.putString("WARFARE_CurrentKit", "Medic");
        persistent.putInt("WARFARE_SquadID", 3);
        persistent.putLong("WARFARE_DriveRequestCooldown_42", 12_000L); // динамический ключ
        persistent.putBoolean("otherMod_flag", true);
        persistent.put("PlayerPersisted", new CompoundTag());

        CompoundTag cap = new CompoundTag();
        assertEquals(3, WarfareMigration.migratePlayer(persistent, cap));

        assertEquals("Medic", WarfareKeys.CURRENT_KIT.read(cap));
        assertEquals(3, WarfareKeys.SQUAD_ID.read(cap));
        assertEquals(12_000L, WarfareKeys.driveRequestCooldown(42).read(cap));
        // старые ключи остаются ещё на одну версию — для отката на старый мод
        assertTrue(persistent.contains("WARFARE_CurrentKit"));
        assertTrue(persistent.contains("WARFARE_DriveRequestCooldown_42"));
        assertTrue(cap.contains(WarfareMigration.MIGRATED_MARKER));
        assertFalse(cap.contains("otherMod_flag"));
        assertTrue(persistent.contains("otherMod_flag"));
        assertTrue(persistent.contains("PlayerPersisted"));
    }

    @Test
    void existingCapabilityValueWins() {
        CompoundTag persistent = new CompoundTag();
        persistent.putString("WARFARE_CurrentKit", "Old");
        CompoundTag cap = new CompoundTag();
        cap.putString("WARFARE_CurrentKit", "New");

        assertEquals(0, WarfareMigration.migratePlayer(persistent, cap));
        assertEquals("New", WarfareKeys.CURRENT_KIT.read(cap));
        assertTrue(persistent.contains("WARFARE_CurrentKit"), "старый ключ не удаляется");
    }

    @Test
    void staleLegacyValueDoesNotComeBackAfterRemoval() {
        CompoundTag persistent = new CompoundTag();
        persistent.putString("WARFARE_PendingKit", "Medic");
        CompoundTag cap = new CompoundTag();
        WarfareMigration.migratePlayer(persistent, cap);
        cap.remove("WARFARE_PendingKit"); // игрок применил кит — PendingKit снят
        assertEquals(0, WarfareMigration.migratePlayer(persistent, cap), "повторный вход");
        assertFalse(cap.contains("WARFARE_PendingKit"), "снимок из persistentData не возвращается");
    }

    @Test
    void removeLegacyForNextVersion() {
        CompoundTag persistent = new CompoundTag();
        persistent.putString("WARFARE_CurrentKit", "Medic");
        persistent.putBoolean("otherMod_flag", true);
        assertEquals(1, WarfareMigration.removeLegacy(persistent));
        assertFalse(persistent.contains("WARFARE_CurrentKit"));
        assertTrue(persistent.contains("otherMod_flag"));
    }

    @Test
    void secondMigrationIsNoop() {
        CompoundTag persistent = new CompoundTag();
        persistent.putBoolean("WARFARE_IsDowned", true);
        CompoundTag cap = new CompoundTag();
        WarfareMigration.migratePlayer(persistent, cap);
        assertEquals(0, WarfareMigration.migratePlayer(persistent, cap));
        assertTrue(WarfareKeys.IS_DOWNED.read(cap));
    }

    @Test
    void keyNamesMatchOldFormat() {
        // Имена ключей — часть формата сохранений: менять нельзя
        assertEquals("WARFARE_CurrentKit", WarfareKeys.CURRENT_KIT.name());
        assertEquals("WARFARE_SquadID", WarfareKeys.SQUAD_ID.name());
        assertEquals("WARFARE_DroneDeployCd", WarfareKeys.DRONE_DEPLOY_CD.name());
        assertEquals("WARFARE_VehicleTeam", WarfareKeys.VEHICLE_TEAM.name());
        assertEquals("WARFARE_LastMedicShoutTimeMS", WarfareKeys.LAST_MEDIC_SHOUT_TIME_MS.name());
        for (String name : WarfareKeys.all().keySet()) {
            assertTrue(name.startsWith(WarfareKeys.PREFIX), name);
        }
    }

    @Test
    void defaultsMatchCompoundTagDefaults() {
        CompoundTag empty = new CompoundTag();
        assertEquals("", WarfareKeys.CURRENT_KIT.read(empty));
        assertEquals(0, WarfareKeys.SQUAD_ID.read(empty));
        assertEquals(0L, WarfareKeys.LAST_FOB_RESUPPLY.read(empty));
        assertFalse(WarfareKeys.IS_DOWNED.read(empty));
        ListTag list = WarfareKeys.INITIAL_LOADOUT.read(empty);
        assertTrue(list.isEmpty());
    }

    @Test
    void entityViewWritesToPersistentDataUnchanged() {
        CompoundTag entityTag = new CompoundTag();
        WarfareData view = new WarfareData.EntityTags(entityTag);
        view.putString(WarfareKeys.VEHICLE_TEAM, "BLUE");
        view.putInt(WarfareKeys.VEHICLE_MATS, 150);
        assertEquals("BLUE", entityTag.getString("WARFARE_VehicleTeam"));
        assertEquals(150, entityTag.getInt("WARFARE_VehicleMats"));
        view.remove(WarfareKeys.VEHICLE_TEAM);
        assertFalse(view.contains(WarfareKeys.VEHICLE_TEAM));
    }
}

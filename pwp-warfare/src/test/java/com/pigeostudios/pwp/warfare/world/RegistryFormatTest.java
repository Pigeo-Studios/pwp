package com.pigeostudios.pwp.warfare.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

/**
 * Совместимость формата WarfareWorldData после разделения на реестры: каждый реестр читает
 * и пишет ключи в КОРНЕ тега под прежними именами — старые миры загружаются без миграции.
 */
class RegistryFormatTest {

    /** Фрагмент тега в старом формате (как его писал монолитный WarfareWorldData.save). */
    private static CompoundTag legacyTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("BlueTickets", 640);
        tag.putInt("RedTickets", 512);
        tag.putString("GameMode", "invasion");
        tag.putString("InvasionDefender", "BLUE");
        tag.putBoolean("IsGameStarted", true);
        tag.putInt("MapSizeBlocks", 4096);
        CompoundTag spawns = new CompoundTag();
        spawns.putLong("minecraft:overworld", new BlockPos(10, 64, -20).asLong());
        tag.put("BlueSpawnsMap", spawns);
        tag.putInt("BlueCMDId", 3);
        CompoundTag votes = new CompoundTag();
        votes.putBoolean(new UUID(1, 2).toString(), true);
        tag.put("BlueCmdVotesMap", votes);
        ListTag rallies = new ListTag();
        rallies.add(LongTag.valueOf(new BlockPos(1, 2, 3).asLong()));
        tag.put("BlueRallies", rallies);
        // старое имя станций техники — миграция внутри StructureRegistry.load
        ListTag stations = new ListTag();
        tag.put("MainSupplies", stations);
        return tag;
    }

    @Test
    void matchStateReadsLegacyRootKeys() {
        MatchState m = new MatchState();
        m.load(legacyTag());
        assertEquals(640, m.blueTickets);
        assertEquals(512, m.redTickets);
        assertEquals("invasion", m.gameMode);
        assertEquals("BLUE", m.invasionDefender);
        assertTrue(m.isGameStarted);
        assertEquals(4096, m.mapSizeBlocks);
        assertEquals(new BlockPos(10, 64, -20), m.blueSpawns.get("minecraft:overworld"));
    }

    @Test
    void matchStateRoundTrip() {
        MatchState a = new MatchState();
        a.blueTickets = 123;
        a.gameMode = "aas";
        a.isPaused = true;
        a.capturePoints.add(new WarfareWorldData.CapturePoint(
                "Alpha", new AABB(0, 0, 0, 10, 10, 10), 1, 2, 3, 4, 5, "CUBE", 0, 10, 20));
        CompoundTag tag = new CompoundTag();
        a.save(tag);
        assertEquals(123, tag.getInt("BlueTickets"), "ключ в корне тега, имя прежнее");

        MatchState b = new MatchState();
        b.load(tag);
        assertEquals(123, b.blueTickets);
        assertTrue(b.isPaused);
        assertEquals(1, b.capturePoints.size());
        assertEquals("Alpha", b.capturePoints.get(0).name);
        assertEquals(20, b.capturePoints.get(0).ticketGainCapture);
    }

    @Test
    void squadRegistryReadsCommanderState() {
        SquadRegistry s = new SquadRegistry();
        s.load(legacyTag());
        assertEquals(3, s.blueCMDId);
        assertEquals(Boolean.TRUE, s.blueCmdVotes.get(new UUID(1, 2)));
    }

    @Test
    void structureRegistryRoundTripAndLegacyStations() {
        StructureRegistry s = new StructureRegistry();
        s.load(legacyTag());
        assertEquals(new BlockPos(1, 2, 3), s.blueRallies.get(0));
        assertTrue(s.vehicleStations.isEmpty());

        s.hubs.add(new WarfareWorldData.HubInfo(new BlockPos(5, 6, 7), "RED", true, "minecraft:overworld"));
        CompoundTag tag = new CompoundTag();
        s.save(tag);
        assertTrue(tag.contains("HubsData"));
        assertTrue(tag.contains("VehicleStations"), "пишем уже новое имя");
        assertFalse(tag.contains("MainSupplies"));

        StructureRegistry r = new StructureRegistry();
        r.load(tag);
        assertEquals(1, r.hubs.size());
        assertEquals(new BlockPos(5, 6, 7), r.hubs.get(0).pos);
        assertEquals(1, r.blueRallies.size());
    }
}

package com.pigeostudios.pwp.warfare.world;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Постройки и зоны: ралли, хабы, станции техники, блоки старта игры, мейн-зоны. Часть {@link WarfareWorldData}. */
public final class StructureRegistry {
    public List<BlockPos> blueRallies = new ArrayList<>();
    public List<BlockPos> redRallies = new ArrayList<>();
    public List<WarfareWorldData.HubInfo> hubs = new ArrayList<>();
    public List<WarfareWorldData.VehicleStationInfo> vehicleStations = new ArrayList<>();
    public List<BlockPos> triggerBlocks = new ArrayList<>();
    public List<WarfareWorldData.MainProtectionZone> mainZones = new ArrayList<>();

    /** Пишет свои ключи в корень тега WarfareWorldData (формат прежний). */
    public void save(CompoundTag tag) {
        ListTag triggerList = new ListTag();

        for (BlockPos p : this.triggerBlocks) {
            triggerList.add(LongTag.valueOf(p.asLong()));
        }

        tag.put("TriggerBlocks", triggerList);
        ListTag blueList = new ListTag();

        for (BlockPos pos : this.blueRallies) {
            blueList.add(LongTag.valueOf(pos.asLong()));
        }

        tag.put("BlueRallies", blueList);
        ListTag mainZoneList = new ListTag();

        for (WarfareWorldData.MainProtectionZone zone : this.mainZones) {
            mainZoneList.add(zone.save());
        }

        tag.put("MainZones", mainZoneList);
        ListTag redList = new ListTag();

        for (BlockPos pos : this.redRallies) {
            redList.add(LongTag.valueOf(pos.asLong()));
        }

        tag.put("RedRallies", redList);
        ListTag hubList = new ListTag();

        for (WarfareWorldData.HubInfo h : this.hubs) {
            hubList.add(h.save());
        }

        tag.put("HubsData", hubList);
        ListTag supplyList = new ListTag();

        for (WarfareWorldData.VehicleStationInfo s : this.vehicleStations) {
            supplyList.add(s.save());
        }

        tag.put("VehicleStations", supplyList);
    }

    /** Читает свои ключи из корня тега WarfareWorldData. */
    public void load(CompoundTag tag) {
        if (tag.contains("TriggerBlocks")) {
            for (Tag t : tag.getList("TriggerBlocks", 4)) {
                this.triggerBlocks.add(BlockPos.of(((LongTag) t).getAsLong()));
            }
        }

        if (tag.contains("MainZones")) {
            ListTag list = tag.getList("MainZones", 10);

            for (int i = 0; i < list.size(); i++) {
                this.mainZones.add(WarfareWorldData.MainProtectionZone.load(list.getCompound(i)));
            }
        }

        if (tag.contains("BlueRallies")) {
            for (Tag t : tag.getList("BlueRallies", 4)) {
                this.blueRallies.add(BlockPos.of(((LongTag) t).getAsLong()));
            }
        }

        if (tag.contains("RedRallies")) {
            for (Tag t : tag.getList("RedRallies", 4)) {
                this.redRallies.add(BlockPos.of(((LongTag) t).getAsLong()));
            }
        }

        if (tag.contains("HubsData")) {
            ListTag list = tag.getList("HubsData", 10);

            for (int i = 0; i < list.size(); i++) {
                this.hubs.add(WarfareWorldData.HubInfo.load(list.getCompound(i)));
            }
        }

        if (tag.contains("VehicleStations")) {
            ListTag list = tag.getList("VehicleStations", 10);

            for (int i = 0; i < list.size(); i++) {
                this.vehicleStations.add(WarfareWorldData.VehicleStationInfo.load(list.getCompound(i)));
            }
        } else if (tag.contains("MainSupplies")) {
            // Миграция старых сохранений
            ListTag list = tag.getList("MainSupplies", 10);

            for (int i = 0; i < list.size(); i++) {
                this.vehicleStations.add(WarfareWorldData.VehicleStationInfo.load(list.getCompound(i)));
            }
        }
    }
}

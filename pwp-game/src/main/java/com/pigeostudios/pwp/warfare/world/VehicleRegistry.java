package com.pigeostudios.pwp.warfare.world;

import com.pigeostudios.pwp.warfare.data.FactionVehicleData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Техника: отмеченная техника, спавнеры, допущенные водители, слоты спавнеров из map_config, пулы техники фракций. Часть {@link WarfareWorldData}. */
public final class VehicleRegistry {
    public List<WarfareWorldData.VehicleRecord> markedVehicles = new ArrayList<>();
    public final List<WarfareWorldData.SpawnerInfo> spawnerInfos = new java.util.concurrent.CopyOnWriteArrayList<>();
    public Map<UUID, Set<UUID>> approvedDrivers = new HashMap<>();
    public Map<String, FactionVehicleData> blueFactionVehicles = new HashMap<>();
    public Map<String, FactionVehicleData> redFactionVehicles = new HashMap<>();
    public List<ConfigVehicleSpawner> configVehicleSpawners = new ArrayList<>();
    /** Дивизии фракций команд из core-service (этап 6 ROADMAP); в NBT не пишутся — приходят вместе с пулом. */
    public List<com.pigeostudios.pwp.warfare.vehicles.Division> blueDivisions = new ArrayList<>();

    public List<com.pigeostudios.pwp.warfare.vehicles.Division> redDivisions = new ArrayList<>();
    /** Выбранная дивизия команды; null — прежний общий пул фракции. */
    public com.pigeostudios.pwp.warfare.vehicles.Division blueDivision;

    public com.pigeostudios.pwp.warfare.vehicles.Division redDivision;
    public List<ConfigVehicleSpawner> pendingVehicleSpawners = new ArrayList<>();

    /** Пишет свои ключи в корень тега WarfareWorldData (формат прежний). */
    public void save(CompoundTag tag) {
        ListTag vehicleList = new ListTag();

        for (WarfareWorldData.VehicleRecord v : this.markedVehicles) {
            vehicleList.add(v.save());
        }

        tag.put("MarkedVehicles", vehicleList);
    }

    /** Читает свои ключи из корня тега WarfareWorldData. */
    public void load(CompoundTag tag) {
        if (tag.contains("MarkedVehicles")) {
            ListTag list = tag.getList("MarkedVehicles", 10);

            for (int i = 0; i < list.size(); i++) {
                this.markedVehicles.add(WarfareWorldData.VehicleRecord.load(list.getCompound(i)));
            }
        }
    }
}

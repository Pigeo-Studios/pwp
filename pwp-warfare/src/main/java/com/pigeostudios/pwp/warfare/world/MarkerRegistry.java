package com.pigeostudios.pwp.warfare.world;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Метки: тактические метки карты и снимки меток/путей MarkerManager/PathManager. Часть {@link WarfareWorldData}. */
public final class MarkerRegistry {
    public List<WarfareWorldData.MapMarker> activeMarkers = new ArrayList<>();
    public final List<MarkerSnapshot> savedMarkers = new ArrayList<>();
    public final List<PathSnapshot> savedPaths = new ArrayList<>();

    /** Пишет свои ключи в корень тега WarfareWorldData (формат прежний). */
    public void save(CompoundTag tag) {
        ListTag markerList = new ListTag();

        for (WarfareWorldData.MapMarker m : this.activeMarkers) {
            markerList.add(m.save());
        }

        tag.put("TacticalMarkers", markerList);
        ListTag pwpMarkerList = new ListTag();
        for (MarkerSnapshot m : this.savedMarkers) pwpMarkerList.add(m.save());
        tag.put("PwpMarkers", pwpMarkerList);
        ListTag pathList = new ListTag();
        for (PathSnapshot p : this.savedPaths) pathList.add(p.save());
        tag.put("PwpPaths", pathList);
    }

    /** Читает свои ключи из корня тега WarfareWorldData. */
    public void load(CompoundTag tag) {
        if (tag.contains("TacticalMarkers")) {
            ListTag list = tag.getList("TacticalMarkers", 10);

            for (int i = 0; i < list.size(); i++) {
                this.activeMarkers.add(WarfareWorldData.MapMarker.load(list.getCompound(i)));
            }
        }

        if (tag.contains("PwpMarkers")) {
            ListTag list = tag.getList("PwpMarkers", 10);
            for (int i = 0; i < list.size(); i++) this.savedMarkers.add(MarkerSnapshot.load(list.getCompound(i)));
        }
        if (tag.contains("PwpPaths")) {
            ListTag list = tag.getList("PwpPaths", 10);
            for (int i = 0; i < list.size(); i++) this.savedPaths.add(PathSnapshot.load(list.getCompound(i)));
        }
    }
}

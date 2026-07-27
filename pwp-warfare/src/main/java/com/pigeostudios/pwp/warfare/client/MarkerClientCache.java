package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.world.MapMarker;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MarkerClientCache {
    private static final List<MapMarker> list = new CopyOnWriteArrayList<>();
    public static void add(MapMarker m) { list.removeIf(x -> x.id.equals(m.id)); list.add(m); }
    public static void remove(UUID id) { list.removeIf(x -> x.id.equals(id)); }
    public static List<MapMarker> getAll() { return list; }
    public static void clear() { list.clear(); }
}

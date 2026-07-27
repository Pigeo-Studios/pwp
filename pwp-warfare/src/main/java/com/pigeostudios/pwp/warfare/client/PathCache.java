package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.world.PathPoint;
import java.util.ArrayList;
import java.util.List;

public class PathCache {
    public static final List<List<PathPoint>> groups = new ArrayList<>();
    public static final List<List<PathPoint>> groupsRed = new ArrayList<>();
    public static final List<List<PathPoint>> groupsYellow = new ArrayList<>();
    public static void clear() { groups.clear(); groupsRed.clear(); groupsYellow.clear(); }
}

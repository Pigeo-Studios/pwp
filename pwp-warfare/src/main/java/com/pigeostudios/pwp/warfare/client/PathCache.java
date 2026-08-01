package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.world.PathPoint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PathCache {
    public static final List<List<PathPoint>> groups = new ArrayList<>();
    public static final List<List<PathPoint>> groupsRed = new ArrayList<>();
    public static final List<List<PathPoint>> groupsYellow = new ArrayList<>();
    public static final Map<Integer, List<PathPoint>> groupsSquad = new LinkedHashMap<>();

    // Maps path key → squadId for display numbering
    public static final Map<Integer, Integer> squadNumForPath = new LinkedHashMap<>();
    // Maps path key/group index → creation tick
    public static final Map<Integer, Long> pathCreatedAt = new LinkedHashMap<>();

    // Server-synced paths (type: "squad","enemy","team","cmd_squads")
    public static final Map<UUID, ServerPath> serverPaths = new LinkedHashMap<>();
    public record ServerPath(String type, int squadNum, List<PathPoint> points, long createdAt) {}

    public static void clear() {
        groups.clear(); groupsRed.clear(); groupsYellow.clear(); groupsSquad.clear();
        squadNumForPath.clear(); serverPaths.clear();
    }
}

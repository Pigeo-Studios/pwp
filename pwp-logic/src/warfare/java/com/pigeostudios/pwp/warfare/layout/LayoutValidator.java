package com.pigeostudios.pwp.warfare.layout;

import com.pigeostudios.pwp.warfare.layout.MapLayout.Border;
import com.pigeostudios.pwp.warfare.layout.MapLayout.CapturePointDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.MainZoneDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.TeamSpawn;
import com.pigeostudios.pwp.warfare.layout.MapLayout.VehicleSlot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Проверка раскладки. Ожидает раскладку ПОСЛЕ {@link LayoutDerive#resolve} (иначе невыведенные приоритеты/граница
 * дадут ложные ошибки). ERROR = матч стартовать нельзя; сейчас вызывающий только логирует.
 *
 * <p>Цепочка приоритетов у каждой команды должна быть ровно 1..N без дыр и дублей: захват ищет точку с
 * приоритетом {@code prio − 1} ({@code GameLogicEvents.findRequiredPointName}), дыра молча блокирует цепочку.
 */
public final class LayoutValidator {

    /** Минимум точек для матча. */
    public static final int MIN_POINTS = 2;

    private LayoutValidator() {}

    public static List<Issue> validate(MapLayout l) {
        List<Issue> out = new ArrayList<>();
        Border border = l.border();

        if (l.mode() != null
                && !MapLayout.MODE_AAS.equalsIgnoreCase(l.mode())
                && !MapLayout.MODE_INVASION.equalsIgnoreCase(l.mode())) {
            out.add(Issue.of(Issue.Code.UNKNOWN_MODE, l.mode(), "game treats it as aas"));
        }
        if (border != null && !border.complete()) {
            out.add(Issue.of(Issue.Code.BORDER_INCOMPLETE, "mapBounds", "centerX/centerZ/sizeBlocks"));
        }

        // --- мейны ---
        List<MainZoneDef> zones = l.mainZones();
        for (int i = 0; i < zones.size(); i++) {
            MainZoneDef z = zones.get(i);
            String subj = "mainZones[" + i + "]";
            if (z.teamEnum() == null) out.add(Issue.of(Issue.Code.MAIN_UNKNOWN_TEAM, subj, "team=" + z.team()));
            if (z.box().isEmptyXZ()) out.add(Issue.of(Issue.Code.MAIN_EMPTY, subj, "zero XZ area"));
            if (!Shapes.isKnown(z.shape())) out.add(Issue.of(Issue.Code.UNKNOWN_SHAPE, subj, z.shape()));
            if (border != null && border.complete() && !border.containsBox(z.box())) {
                out.add(Issue.of(Issue.Code.MAIN_OUTSIDE_BORDER, subj, ""));
            }
        }
        for (Team t : Team.values()) {
            if (l.mainOf(t) == null) out.add(Issue.of(Issue.Code.NO_MAIN, t.name(), ""));
        }
        for (MainZoneDef a : zones) {
            if (a.teamEnum() != Team.BLUE) continue;
            for (MainZoneDef b : zones) {
                if (b.teamEnum() != Team.RED) continue;
                if (Box.overlaps(a.box(), a.effShape(), b.box(), b.effShape())) {
                    out.add(Issue.of(Issue.Code.MAINS_OVERLAP, "BLUE/RED", ""));
                }
            }
        }

        // --- точки ---
        List<CapturePointDef> pts = l.points();
        if (pts.size() < MIN_POINTS) {
            out.add(Issue.of(Issue.Code.TOO_FEW_POINTS, "", pts.size() + " < " + MIN_POINTS));
        }
        Map<String, Integer> seen = new HashMap<>();
        for (CapturePointDef p : pts) {
            String key = String.valueOf(p.key());
            if (seen.merge(key, 1, Integer::sum) == 2) out.add(Issue.of(Issue.Code.DUPLICATE_POINT_ID, key, ""));
            if (p.box().isEmptyXZ()) out.add(Issue.of(Issue.Code.POINT_EMPTY, key, "zero XZ area"));
            if (!Shapes.isKnown(p.shape())) out.add(Issue.of(Issue.Code.UNKNOWN_SHAPE, key, p.shape()));
            if (border != null && border.complete()) {
                if (!border.containsXZ(p.box().centerX(), p.box().centerZ())) {
                    out.add(Issue.of(Issue.Code.POINT_OUTSIDE_BORDER, key, ""));
                } else if (!border.containsBox(p.box())) {
                    out.add(Issue.of(Issue.Code.POINT_PARTLY_OUTSIDE_BORDER, key, ""));
                }
            }
            for (MainZoneDef z : zones) {
                if (Box.overlaps(p.box(), p.effShape(), z.box(), z.effShape())) {
                    out.add(Issue.of(Issue.Code.POINT_IN_MAIN, key, "main " + z.team()));
                }
            }
        }
        for (int i = 0; i < pts.size(); i++) {
            for (int j = i + 1; j < pts.size(); j++) {
                CapturePointDef a = pts.get(i);
                CapturePointDef b = pts.get(j);
                if (Box.overlaps(a.box(), a.effShape(), b.box(), b.effShape())) {
                    out.add(Issue.of(Issue.Code.POINTS_OVERLAP, a.key() + "/" + b.key(), ""));
                }
            }
        }
        if (!pts.isEmpty()) {
            boolean chainsOk = true;
            for (Team t : Team.values()) chainsOk &= checkChain(pts, t, out);
            if (chainsOk) {
                int n = pts.size();
                for (CapturePointDef p : pts) {
                    if (p.bluePriority() + p.redPriority() != n + 1) {
                        out.add(Issue.of(
                                Issue.Code.PRIORITY_NOT_MIRRORED,
                                p.key(),
                                "blue=" + p.bluePriority() + " red=" + p.redPriority() + " (expected red="
                                        + (n + 1 - p.bluePriority()) + ")"));
                    }
                }
            }
        }

        // --- спавны мейна ---
        for (Team t : Team.values()) {
            TeamSpawn s = l.spawn(t);
            if (s == null || l.mainOf(t) == null) continue;
            if (!inTeamMain(l, t, s.x() + 0.5, s.z() + 0.5)) {
                out.add(Issue.of(Issue.Code.SPAWN_OUTSIDE_MAIN, t.name(), s.x() + "," + s.z()));
            }
        }

        // --- слоты техники ---
        Map<Team, Integer> counter = new HashMap<>();
        for (VehicleSlot v : l.vehicleSlots()) {
            int i = counter.merge(v.team(), 1, Integer::sum) - 1;
            String subj = v.team() + "[" + i + "]";
            if (isBlank(v.role()) && isBlank(v.vehicleName())) {
                out.add(Issue.of(Issue.Code.SPAWNER_NO_VEHICLE, subj, ""));
            }
            double x = v.pos().x() + 0.5;
            double z = v.pos().z() + 0.5;
            if (border != null && border.complete() && !border.containsXZ(x, z)) {
                out.add(Issue.of(
                        Issue.Code.SPAWNER_OUTSIDE_BORDER,
                        subj,
                        v.pos().x() + "," + v.pos().z()));
            } else if (l.mainOf(v.team()) != null && !inTeamMain(l, v.team(), x, z)) {
                out.add(Issue.of(
                        Issue.Code.SPAWNER_OUTSIDE_MAIN,
                        subj,
                        v.pos().x() + "," + v.pos().z()));
            }
        }
        return out;
    }

    /** Цепочка команды = ровно 1..N; false — найдены проблемы (они уже в {@code out}). */
    private static boolean checkChain(List<CapturePointDef> pts, Team t, List<Issue> out) {
        int n = pts.size();
        boolean ok = true;
        TreeMap<Integer, List<String>> byPrio = new TreeMap<>();
        for (CapturePointDef p : pts) {
            Integer pr = p.priority(t);
            if (pr == null) {
                out.add(Issue.of(Issue.Code.PRIORITY_MISSING, p.key(), t.name()));
                ok = false;
                continue;
            }
            byPrio.computeIfAbsent(pr, k -> new ArrayList<>()).add(p.key());
        }
        for (var e : byPrio.entrySet()) {
            if (e.getValue().size() > 1) {
                out.add(Issue.of(
                        Issue.Code.PRIORITY_DUPLICATE, t.name(), "priority " + e.getKey() + ": " + e.getValue()));
                ok = false;
            }
            if (e.getKey() < 1 || e.getKey() > n) {
                out.add(Issue.of(Issue.Code.PRIORITY_HOLE, t.name(), "priority " + e.getKey() + " outside 1.." + n));
                ok = false;
            }
        }
        for (int r = 1; r <= n; r++) {
            if (!byPrio.containsKey(r)) {
                out.add(Issue.of(Issue.Code.PRIORITY_HOLE, t.name(), "no point with priority " + r));
                ok = false;
            }
        }
        return ok;
    }

    private static boolean inTeamMain(MapLayout l, Team t, double x, double z) {
        for (MainZoneDef zd : l.mainZones()) {
            if (zd.teamEnum() == t && zd.containsXZ(x, z)) return true;
        }
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

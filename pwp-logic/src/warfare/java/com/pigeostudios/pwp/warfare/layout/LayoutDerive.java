package com.pigeostudios.pwp.warfare.layout;

import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.layout.MapLayout.Border;
import com.pigeostudios.pwp.warfare.layout.MapLayout.CapturePointDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.MainZoneDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.TeamSpawn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Авто-вывод того, что не задано в map_config. Явно заданное всегда имеет приоритет: вывод заполняет только
 * null-поля.
 *
 * <ul>
 *   <li>Порядок AAS: точки сортируются по проекции центра на ось «центр мейна BLUE → центр мейна RED» (ближе к
 *       BLUE — раньше; равенство — по расстоянию до мейна BLUE, затем по ключу). Недостающим {@code
 *       bluePriority} раздаются свободные номера 1..N в этом порядке; {@code redPriority = N + 1 − blue}.
 *   <li>Спавн мейна = центр первой мейн-зоны команды по XZ, Y не задаётся (в игре дефолт 64 — {@link
 *       Issue.Code#SPAWN_DERIVED}).
 *   <li>Граница = квадрат вокруг бокса всех мейнов и точек по XZ + {@link #BORDER_MARGIN} с каждой стороны.
 * </ul>
 *
 * <p>Ограничение: проекция на ось мейн→мейн не повторяет «петляющие» маршруты, заданные руками (takmachka: вывод
 * дал бы другой порядок) — для таких карт приоритеты задаются явно.
 */
public final class LayoutDerive {

    /** Отступ границы от крайних мейнов/точек, блоков с каждой стороны. */
    public static final int BORDER_MARGIN = 32;

    private LayoutDerive() {}

    /** Результат вывода: полная раскладка + замечания о том, что выведено с допущениями. */
    public record Resolved(MapLayout layout, List<Issue> notes) {
        public Resolved {
            notes = List.copyOf(notes);
        }
    }

    public static Resolved resolve(MapLayout in) {
        List<Issue> notes = new ArrayList<>();
        MapLayout out = in.withPoints(derivePriorities(in));

        Map<Team, TeamSpawn> spawns = new EnumMap<>(Team.class);
        spawns.putAll(in.spawns());
        for (Team t : Team.values()) {
            if (spawns.get(t) != null) continue;
            MainZoneDef main = in.mainOf(t);
            if (main == null) continue;
            int x = (int) Math.floor(main.box().centerX());
            int z = (int) Math.floor(main.box().centerZ());
            spawns.put(t, new TeamSpawn(null, x, null, z, new JsonObject()));
            notes.add(Issue.of(Issue.Code.SPAWN_DERIVED, t.name(), "main center " + x + "," + z + ", y=default"));
        }
        out = out.withSpawns(spawns);

        Border border = in.border();
        if (border == null || !border.complete()) {
            Border d = deriveBorder(in);
            if (d != null) {
                if (border == null) border = d;
                else
                    border = new Border(
                            border.centerX() != null ? border.centerX() : d.centerX(),
                            border.centerZ() != null ? border.centerZ() : d.centerZ(),
                            border.sizeBlocks() != null ? border.sizeBlocks() : d.sizeBlocks(),
                            border.extra());
            }
        }
        out = out.withBorder(border);
        return new Resolved(out, notes);
    }

    /** Порядок цепочки AAS (индексы в {@code layout.points()}), от BLUE к RED; null — нет одного из мейнов. */
    public static List<Integer> chainOrder(MapLayout layout) {
        MainZoneDef blue = layout.mainOf(Team.BLUE);
        MainZoneDef red = layout.mainOf(Team.RED);
        if (blue == null || red == null) return null;
        double bx = blue.box().centerX();
        double bz = blue.box().centerZ();
        double ax = red.box().centerX() - bx;
        double az = red.box().centerZ() - bz;
        List<CapturePointDef> pts = layout.points();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) idx.add(i);
        Comparator<Integer> cmp = Comparator.comparingDouble(i -> {
            Box b = pts.get(i).box();
            return (b.centerX() - bx) * ax + (b.centerZ() - bz) * az;
        });
        cmp = cmp.thenComparingDouble(i -> {
                    Box b = pts.get(i).box();
                    double dx = b.centerX() - bx;
                    double dz = b.centerZ() - bz;
                    return dx * dx + dz * dz;
                })
                .thenComparing(i -> String.valueOf(pts.get(i).key()));
        idx.sort(cmp);
        return idx;
    }

    private static List<CapturePointDef> derivePriorities(MapLayout in) {
        List<CapturePointDef> pts = new ArrayList<>(in.points());
        int n = pts.size();
        if (n == 0) return pts;

        boolean blueMissing = pts.stream().anyMatch(p -> p.bluePriority() == null);
        Map<Integer, Integer> blue = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (pts.get(i).bluePriority() != null) blue.put(i, pts.get(i).bluePriority());
        }
        if (blueMissing) {
            List<Integer> order = chainOrder(in);
            if (order != null) {
                Set<Integer> used = new HashSet<>(blue.values());
                TreeSet<Integer> free = new TreeSet<>();
                for (int r = 1; r <= n; r++) if (!used.contains(r)) free.add(r);
                for (int i : order) {
                    if (blue.containsKey(i)) continue;
                    Integer r = free.pollFirst();
                    if (r == null) break; // свободных номеров нет — дубли поймает валидатор
                    blue.put(i, r);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            CapturePointDef p = pts.get(i);
            Integer b = p.bluePriority() != null ? p.bluePriority() : blue.get(i);
            Integer r = p.redPriority() != null ? p.redPriority() : (b == null ? null : n + 1 - b);
            if (!java.util.Objects.equals(b, p.bluePriority()) || !java.util.Objects.equals(r, p.redPriority())) {
                pts.set(i, p.withPriorities(b, r));
            }
        }
        return pts;
    }

    /** Граница вокруг мейнов и точек; null — геометрии нет. */
    public static Border deriveBorder(MapLayout in) {
        List<Box> boxes = new ArrayList<>();
        for (MainZoneDef z : in.mainZones()) boxes.add(z.box());
        for (CapturePointDef p : in.points()) boxes.add(p.box());
        if (boxes.isEmpty()) return null;
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Box b : boxes) {
            minX = Math.min(minX, b.minX());
            minZ = Math.min(minZ, b.minZ());
            maxX = Math.max(maxX, b.maxX());
            maxZ = Math.max(maxZ, b.maxZ());
        }
        int size = Math.max(maxX - minX, maxZ - minZ) + 2 * BORDER_MARGIN;
        int cx = Math.floorDiv(minX + maxX, 2);
        int cz = Math.floorDiv(minZ + maxZ, 2);
        return new Border(cx, cz, size, new JsonObject());
    }
}

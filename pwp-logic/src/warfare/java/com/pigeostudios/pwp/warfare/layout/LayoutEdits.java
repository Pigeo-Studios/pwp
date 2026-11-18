package com.pigeostudios.pwp.warfare.layout;

import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.layout.MapLayout.Border;
import com.pigeostudios.pwp.warfare.layout.MapLayout.CapturePointDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.MainZoneDef;
import com.pigeostudios.pwp.warfare.layout.MapLayout.TeamSpawn;
import com.pigeostudios.pwp.warfare.layout.MapLayout.VehicleSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Операции GUI-редактора карты (этап 3) над неизменяемой {@link MapLayout}: каждая возвращает новую раскладку.
 *
 * <p>Порядок цепочки AAS = порядок списка точек: после структурных правок (добавить/удалить/переставить/
 * авто-порядок) приоритеты переписываются {@link #renumber}: {@code blue = i+1}, {@code red = N−i}. Правка
 * углов и параметров точки приоритеты не трогает. Авто-порядок ({@link LayoutDerive#chainOrder}) — только
 * предложение: редактор применяет его по кнопке, дальше главнее ручной порядок.
 */
public final class LayoutEdits {
    private LayoutEdits() {}

    /** Что выбрано в редакторе. {@code index}: MAIN — в mainZones, POINT — в points, SPAWN — ordinal команды, SLOT — в vehicleSlots, BORDER — 0. */
    public enum Kind {
        MAIN,
        POINT,
        SPAWN,
        SLOT,
        BORDER
    }

    public record Ref(Kind kind, int index) {
        public static Ref main(int i) {
            return new Ref(Kind.MAIN, i);
        }

        public static Ref point(int i) {
            return new Ref(Kind.POINT, i);
        }

        public static Ref spawn(Team t) {
            return new Ref(Kind.SPAWN, t.ordinal());
        }

        public static Ref slot(int i) {
            return new Ref(Kind.SLOT, i);
        }

        public static Ref border() {
            return new Ref(Kind.BORDER, 0);
        }

        /** Объект ещё существует в раскладке. */
        public boolean valid(MapLayout l) {
            return switch (kind) {
                case MAIN -> index >= 0 && index < l.mainZones().size();
                case POINT -> index >= 0 && index < l.points().size();
                case SPAWN -> index >= 0 && index < Team.values().length;
                case SLOT -> index >= 0 && index < l.vehicleSlots().size();
                case BORDER -> true;
            };
        }
    }

    // ---------------------------------------------------------------- мейны

    /** Угол ({@code corner} 1 или 2) мейна команды; мейна нет — создаётся точечный (оба угла = pos). */
    public static MapLayout setMainCorner(MapLayout l, Team team, int corner, Pos pos) {
        List<MainZoneDef> zones = new ArrayList<>(l.mainZones());
        int idx = mainIndex(l, team);
        if (idx < 0) {
            zones.add(new MainZoneDef(team.name(), Shapes.BOX, new Box(pos, pos), new JsonObject()));
        } else {
            MainZoneDef z = zones.get(idx);
            zones.set(idx, new MainZoneDef(z.team(), z.shape(), withCorner(z.box(), corner, pos), z.extra()));
        }
        return l.withMainZones(zones);
    }

    public static MapLayout setMainShape(MapLayout l, int index, String shape) {
        List<MainZoneDef> zones = new ArrayList<>(l.mainZones());
        MainZoneDef z = zones.get(index);
        zones.set(index, new MainZoneDef(z.team(), shape, z.box(), z.extra()));
        return l.withMainZones(zones);
    }

    public static MapLayout translateMain(MapLayout l, int index, int dx, int dz) {
        List<MainZoneDef> zones = new ArrayList<>(l.mainZones());
        MainZoneDef z = zones.get(index);
        zones.set(index, new MainZoneDef(z.team(), z.shape(), translate(z.box(), dx, dz), z.extra()));
        return l.withMainZones(zones);
    }

    public static MapLayout removeMain(MapLayout l, int index) {
        List<MainZoneDef> zones = new ArrayList<>(l.mainZones());
        zones.remove(index);
        return l.withMainZones(zones);
    }

    /** Индекс первой мейн-зоны команды или -1. */
    public static int mainIndex(MapLayout l, Team team) {
        for (int i = 0; i < l.mainZones().size(); i++) {
            if (l.mainZones().get(i).teamEnum() == team) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------- точки

    /** Новая точка в конец цепочки (точечный бокс в pos — второй угол ставится следующим кликом). */
    public static MapLayout addPoint(MapLayout l, String name, Box box) {
        List<CapturePointDef> pts = new ArrayList<>(l.points());
        String n = name == null || name.isBlank() ? uniquePointName(l, "P") : name;
        pts.add(new CapturePointDef(
                null, n, Shapes.CYLINDER, box, null, null, null, null, null, null, null, null, new JsonObject()));
        return renumber(l.withPoints(pts));
    }

    public static MapLayout removePoint(MapLayout l, int index) {
        List<CapturePointDef> pts = new ArrayList<>(l.points());
        pts.remove(index);
        return renumber(l.withPoints(pts));
    }

    /** Перестановка в цепочке (перетаскивание в списке): точка {@code from} встаёт на место {@code to}. */
    public static MapLayout movePoint(MapLayout l, int from, int to) {
        List<CapturePointDef> pts = new ArrayList<>(l.points());
        if (from < 0 || from >= pts.size()) return l;
        int t = Math.max(0, Math.min(pts.size() - 1, to));
        CapturePointDef p = pts.remove(from);
        pts.add(t, p);
        return renumber(l.withPoints(pts));
    }

    public static MapLayout setPointCorner(MapLayout l, int index, int corner, Pos pos) {
        CapturePointDef p = l.points().get(index);
        return replacePoint(l, index, withBox(p, withCorner(p.box(), corner, pos)));
    }

    public static MapLayout translatePoint(MapLayout l, int index, int dx, int dz) {
        CapturePointDef p = l.points().get(index);
        return replacePoint(l, index, withBox(p, translate(p.box(), dx, dz)));
    }

    /** Замена точки целиком (имя/форма/параметры захвата из формы свойств). Приоритеты не трогаются. */
    public static MapLayout replacePoint(MapLayout l, int index, CapturePointDef p) {
        List<CapturePointDef> pts = new ArrayList<>(l.points());
        pts.set(index, p);
        return l.withPoints(pts);
    }

    /** Параметры захвата и имя (null = ключа нет — дефолт игры). */
    public static CapturePointDef withParams(
            CapturePointDef p,
            String name,
            String shape,
            Integer captureTimeMinutes,
            Integer ticketPenalty,
            Integer captureDeduction,
            Integer lockDurationMinutes) {
        return new CapturePointDef(
                p.id(),
                name,
                shape,
                p.box(),
                p.bluePriority(),
                p.redPriority(),
                captureTimeMinutes,
                ticketPenalty,
                captureDeduction,
                lockDurationMinutes,
                p.gainNeutralizeTickets(),
                p.gainCaptureTickets(),
                p.extra());
    }

    /** Приоритеты по порядку списка: blue = i+1, red = N−i. */
    public static MapLayout renumber(MapLayout l) {
        List<CapturePointDef> pts = new ArrayList<>();
        int n = l.points().size();
        for (int i = 0; i < n; i++) pts.add(l.points().get(i).withPriorities(i + 1, n - i));
        return l.withPoints(pts);
    }

    /**
     * Список точек по {@code bluePriority} (без приоритета — в конце, порядок файла сохраняется). Вызывается
     * при открытии редактора, чтобы порядок списка совпал с цепочкой; приоритеты не меняет.
     */
    public static MapLayout sortByChain(MapLayout l) {
        List<CapturePointDef> pts = new ArrayList<>(l.points());
        pts.sort(Comparator.comparingInt(p -> p.bluePriority() == null ? Integer.MAX_VALUE : p.bluePriority()));
        return l.withPoints(pts);
    }

    /** Предложение авто-порядка (проекция на ось мейн BLUE → RED); нет одного из мейнов — без изменений. */
    public static MapLayout autoOrder(MapLayout l) {
        List<Integer> order = LayoutDerive.chainOrder(l);
        if (order == null) return l;
        List<CapturePointDef> pts = new ArrayList<>();
        for (int i : order) pts.add(l.points().get(i));
        return renumber(l.withPoints(pts));
    }

    /** {@code base}, {@code base 2}, … — первое имя, которого ещё нет среди точек. */
    public static String uniquePointName(MapLayout l, String base) {
        Set<String> names = new HashSet<>();
        for (CapturePointDef p : l.points()) names.add(p.key());
        String b = base == null || base.isBlank() ? "P" : base.trim();
        for (int i = l.points().size() + 1; ; i++) {
            String c = b + i;
            if (!names.contains(c)) return c;
        }
    }

    // ---------------------------------------------------------------- спавны

    public static MapLayout setSpawn(MapLayout l, Team team, Pos pos) {
        Map<Team, TeamSpawn> s = new EnumMap<>(Team.class);
        s.putAll(l.spawns());
        TeamSpawn old = s.get(team);
        s.put(
                team,
                new TeamSpawn(
                        old == null ? null : old.dimension(),
                        pos.x(),
                        pos.y(),
                        pos.z(),
                        old == null ? new JsonObject() : old.extra()));
        return l.withSpawns(s);
    }

    /** Убрать явный спавн — в игре он снова выводится из центра мейна. */
    public static MapLayout clearSpawn(MapLayout l, Team team) {
        Map<Team, TeamSpawn> s = new EnumMap<>(Team.class);
        s.putAll(l.spawns());
        s.remove(team);
        return l.withSpawns(s);
    }

    // ---------------------------------------------------------------- техника

    /** Новый слот; слоты держатся в порядке BLUE, затем RED (как пишет кодек). Возвращает индекс через {@link #lastSlotIndex}. */
    public static MapLayout addSlot(MapLayout l, Team team, Pos pos, String role, float yaw) {
        List<VehicleSlot> v = new ArrayList<>(l.vehicleSlots());
        VehicleSlot s = new VehicleSlot(team, role, null, pos, yaw, null, new JsonObject());
        int at = v.size();
        if (team == Team.BLUE) {
            at = 0;
            while (at < v.size() && v.get(at).team() == Team.BLUE) at++;
        }
        v.add(at, s);
        return l.withVehicleSlots(v);
    }

    /** Индекс последнего слота команды (после {@link #addSlot}) или -1. */
    public static int lastSlotIndex(MapLayout l, Team team) {
        int idx = -1;
        for (int i = 0; i < l.vehicleSlots().size(); i++)
            if (l.vehicleSlots().get(i).team() == team) idx = i;
        return idx;
    }

    public static MapLayout removeSlot(MapLayout l, int index) {
        List<VehicleSlot> v = new ArrayList<>(l.vehicleSlots());
        v.remove(index);
        return l.withVehicleSlots(v);
    }

    public static MapLayout moveSlot(MapLayout l, int index, Pos pos) {
        VehicleSlot s = l.vehicleSlots().get(index);
        return replaceSlot(
                l, index, new VehicleSlot(s.team(), s.role(), s.vehicleName(), pos, s.yaw(), s.fixed(), s.extra()));
    }

    /** role/vehicleName — пустая строка = ключа нет; yaw нормализуется в [-180, 180). */
    public static MapLayout setSlotProps(MapLayout l, int index, String role, String vehicleName, float yaw) {
        VehicleSlot s = l.vehicleSlots().get(index);
        return replaceSlot(
                l,
                index,
                new VehicleSlot(
                        s.team(),
                        blankToNull(role),
                        blankToNull(vehicleName),
                        s.pos(),
                        normYaw(yaw),
                        s.fixed(),
                        s.extra()));
    }

    private static MapLayout replaceSlot(MapLayout l, int index, VehicleSlot s) {
        List<VehicleSlot> v = new ArrayList<>(l.vehicleSlots());
        v.set(index, s);
        return l.withVehicleSlots(v);
    }

    public static float normYaw(float yaw) {
        if (!Float.isFinite(yaw)) return 0f;
        float y = yaw % 360f;
        if (y >= 180f) y -= 360f;
        if (y < -180f) y += 360f;
        return y;
    }

    // ---------------------------------------------------------------- граница

    /** Квадрат по двум углам: центр — середина, сторона — большая из сторон прямоугольника. */
    public static MapLayout setBorderFromCorners(MapLayout l, int x1, int z1, int x2, int z2) {
        int size = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        int cx = (int) Math.floor((x1 + x2) / 2.0);
        int cz = (int) Math.floor((z1 + z2) / 2.0);
        return setBorder(l, cx, cz, size);
    }

    public static MapLayout setBorder(MapLayout l, int cx, int cz, int size) {
        JsonObject extra = l.border() == null ? new JsonObject() : l.border().extra();
        return l.withBorder(new Border(cx, cz, Math.max(1, size), extra));
    }

    /** Убрать явную границу — в игре она выводится (бокс мейнов+точек + отступ). */
    public static MapLayout clearBorder(MapLayout l) {
        return l.withBorder(null);
    }

    // ---------------------------------------------------------------- «показать где»

    /** Центр объекта по XZ или null (объекта нет / не задан). */
    public static double[] locate(MapLayout l, Ref r) {
        if (r == null || !r.valid(l)) return null;
        switch (r.kind()) {
            case MAIN -> {
                Box b = l.mainZones().get(r.index()).box();
                return new double[] {b.centerX(), b.centerZ()};
            }
            case POINT -> {
                Box b = l.points().get(r.index()).box();
                return new double[] {b.centerX(), b.centerZ()};
            }
            case SPAWN -> {
                Team t = Team.values()[r.index()];
                TeamSpawn s = l.spawn(t);
                if (s != null) return new double[] {s.x() + 0.5, s.z() + 0.5};
                MainZoneDef m = l.mainOf(t);
                return m == null
                        ? null
                        : new double[] {m.box().centerX(), m.box().centerZ()};
            }
            case SLOT -> {
                Pos p = l.vehicleSlots().get(r.index()).pos();
                return new double[] {p.x() + 0.5, p.z() + 0.5};
            }
            case BORDER -> {
                Border b = l.border();
                if (b == null || b.centerX() == null || b.centerZ() == null) return null;
                return new double[] {b.centerX(), b.centerZ()};
            }
        }
        return null;
    }

    /**
     * Объект, о котором замечание валидатора (по {@link Issue#subject()}), или null — замечание о раскладке
     * в целом (мало точек, дыра в цепочке).
     */
    public static Ref refOf(MapLayout l, Issue issue) {
        String s = issue.subject();
        if (s == null || s.isEmpty()) return null;
        if (s.equals("mapBounds")) return Ref.border();
        if (s.startsWith("mainZones[")) {
            int i = bracket(s);
            return i >= 0 && i < l.mainZones().size() ? Ref.main(i) : null;
        }
        if (s.equals("BLUE/RED")) {
            int i = mainIndex(l, Team.BLUE);
            return i >= 0 ? Ref.main(i) : null;
        }
        for (Team t : Team.values()) {
            if (s.startsWith(t.name() + "[")) {
                int n = bracket(s);
                int seen = 0;
                for (int i = 0; i < l.vehicleSlots().size(); i++) {
                    if (l.vehicleSlots().get(i).team() != t) continue;
                    if (seen++ == n) return Ref.slot(i);
                }
                return null;
            }
            if (s.equals(t.name())) {
                if (issue.code() == Issue.Code.PRIORITY_HOLE || issue.code() == Issue.Code.NO_MAIN) return null;
                return Ref.spawn(t);
            }
        }
        String key = s.contains("/") ? s.substring(0, s.indexOf('/')) : s;
        for (int i = 0; i < l.points().size(); i++) {
            if (key.equals(l.points().get(i).key())) return Ref.point(i);
        }
        return null;
    }

    // ---------------------------------------------------------------- геометрия

    static Box withCorner(Box b, int corner, Pos p) {
        return corner == 1 ? new Box(p, b.pos2()) : new Box(b.pos1(), p);
    }

    static Box translate(Box b, int dx, int dz) {
        return new Box(
                new Pos(b.pos1().x() + dx, b.pos1().y(), b.pos1().z() + dz),
                new Pos(b.pos2().x() + dx, b.pos2().y(), b.pos2().z() + dz));
    }

    private static CapturePointDef withBox(CapturePointDef p, Box b) {
        return new CapturePointDef(
                p.id(),
                p.name(),
                p.shape(),
                b,
                p.bluePriority(),
                p.redPriority(),
                p.captureTimeMinutes(),
                p.ticketPenalty(),
                p.captureDeduction(),
                p.lockDurationMinutes(),
                p.gainNeutralizeTickets(),
                p.gainCaptureTickets(),
                p.extra());
    }

    private static int bracket(String s) {
        int a = s.indexOf('[');
        int b = s.indexOf(']', a + 1);
        if (a < 0 || b < 0) return -1;
        return com.pwp.api.util.Parse.intOr(s.substring(a + 1, b), -1);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

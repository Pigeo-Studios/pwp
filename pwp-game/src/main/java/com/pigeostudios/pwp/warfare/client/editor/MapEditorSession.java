package com.pigeostudios.pwp.warfare.client.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.layout.Box;
import com.pigeostudios.pwp.warfare.layout.Issue;
import com.pigeostudios.pwp.warfare.layout.LayoutDerive;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Kind;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Ref;
import com.pigeostudios.pwp.warfare.layout.LayoutTransfer;
import com.pigeostudios.pwp.warfare.layout.LayoutValidator;
import com.pigeostudios.pwp.warfare.layout.MapLayout;
import com.pigeostudios.pwp.warfare.layout.MapLayoutCodec;
import com.pigeostudios.pwp.warfare.layout.Pos;
import com.pigeostudios.pwp.warfare.layout.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Черновик редактора карты на клиенте (этап 3). Все правки — локально над {@link MapLayout} операциями
 * {@link LayoutEdits}; серверу уходит только итог по кнопке «Применить на матч» / «Сохранить навсегда».
 * Замечания валидатора пересчитываются на каждую правку (живой пересчёт).
 *
 * <p>Режим «в мире» ({@link #worldMode}): экран закрыт, ЛКМ/ПКМ по блоку ставят углы выбранного объекта
 * ({@link MapEditorInput}), контуры рисует {@link MapEditorWorldRenderer} — только у редактора.
 */
@OnlyIn(Dist.CLIENT)
public final class MapEditorSession {
    private static final Logger LOG = LoggerFactory.getLogger(MapEditorSession.class);

    /** Версия из БД (строка списка). */
    public record VersionRow(long id, String author, long createdAt, String comment) {}

    private static MapLayout draft = empty();
    private static boolean loaded;
    private static boolean dirty;
    private static List<Issue> issues = List.of();
    private static Ref selected;
    private static boolean worldMode;
    /** Первый угол границы, поставленный в мире (граница — квадрат по двум кликам). */
    private static Pos borderCorner;

    private static String mapName = "";
    private static String source = "";
    private static boolean canPersist;
    private static boolean coreAvailable;
    private static boolean matchRunning;
    private static boolean matchEdit;
    private static long versionId;
    private static List<VersionRow> versions = List.of();
    private static boolean versionsAvailable;

    private static double[] highlight;
    private static long highlightUntil;

    private MapEditorSession() {}

    public static MapLayout emptyLayout() {
        return empty();
    }

    static MapLayout empty() {
        return new MapLayout(new JsonObject(), null, List.of(), -1, List.of(), Map.of(), List.of(), null);
    }

    // ---------- данные с сервера ----------

    /** Ответ {@code mapEditor}. Несохранённый черновик не затирается, если {@code force} = false. */
    public static void onEditorData(String json, boolean force) {
        JsonObject o = parseObject(json);
        if (o == null || o.has("error")) return;
        mapName = str(o, "mapName");
        source = str(o, "source");
        canPersist = bool(o, "canPersist");
        coreAvailable = bool(o, "coreAvailable");
        matchRunning = bool(o, "matchRunning");
        matchEdit = bool(o, "matchEdit");
        versionId = o.has("versionId") ? o.get("versionId").getAsLong() : 0L;
        if (!bool(o, "loaded") || !o.has("layout") || !o.get("layout").isJsonObject()) return;
        if (dirty && !force) return;
        setDraft(LayoutEdits.sortByChain(
                MapLayoutCodec.read(o.getAsJsonObject("layout")).layout()));
        dirty = false;
        loaded = true;
    }

    /** Ответ {@code mapLayoutVersions}. */
    public static void onVersions(String json) {
        JsonObject o = parseObject(json);
        if (o == null || o.has("error")) return;
        versionsAvailable = bool(o, "available");
        List<VersionRow> out = new ArrayList<>();
        JsonArray a =
                o.has("versions") && o.get("versions").isJsonArray() ? o.getAsJsonArray("versions") : new JsonArray();
        for (JsonElement e : a) {
            if (!e.isJsonObject()) continue;
            JsonObject v = e.getAsJsonObject();
            out.add(new VersionRow(
                    v.has("id") ? v.get("id").getAsLong() : 0L,
                    str(v, "authorName"),
                    v.has("createdAt") ? v.get("createdAt").getAsLong() : 0L,
                    str(v, "comment")));
        }
        versions = List.copyOf(out);
    }

    // ---------- черновик ----------

    /** Фикстура тест-стенда / сброс: черновик целиком. */
    public static void setDraft(MapLayout l) {
        draft = l;
        if (selected != null && !selected.valid(l)) selected = null;
        recompute();
    }

    public static void edit(UnaryOperator<MapLayout> op) {
        try {
            setDraft(op.apply(draft));
            dirty = true;
        } catch (RuntimeException e) {
            // индекс устарел (объект удалён) — правка не применяется
            LOG.warn("Map editor: edit rejected", e);
        }
    }

    private static void recompute() {
        List<Issue> out = new ArrayList<>();
        LayoutDerive.Resolved r = LayoutDerive.resolve(draft);
        out.addAll(r.notes());
        out.addAll(LayoutValidator.validate(r.layout()));
        // ошибки первыми
        out.sort((a, b) -> Boolean.compare(!a.isError(), !b.isError()));
        issues = List.copyOf(out);
    }

    /** JSON для пакета (только ключи раскладки). */
    public static String json() {
        return LayoutTransfer.strip(draft, mapName).toString();
    }

    public static void markSent() {
        dirty = false;
    }

    // ---------- действия редактора ----------

    /** Новая точка вокруг позиции (бокс ±half), выбрана; углы правятся дальше. */
    public static void addPointAt(int x, int y, int z, int half) {
        edit(l -> LayoutEdits.addPoint(l, null, Box.of(x - half, y - 8, z - half, x + half, y + 24, z + half)));
        selected = Ref.point(draft.points().size() - 1);
    }

    public static void addSlotAt(Team team, int x, int y, int z, float yaw) {
        edit(l -> LayoutEdits.addSlot(l, team, new Pos(x, y, z), "TRANSPORT", LayoutEdits.normYaw(yaw)));
        selected = Ref.slot(LayoutEdits.lastSlotIndex(draft, team));
    }

    /** Выбрать мейн команды; нет — завести точечный в {@code at} (углы ставятся кликами в мире). */
    public static void selectMain(Team team, Pos at) {
        int i = LayoutEdits.mainIndex(draft, team);
        if (i < 0) {
            edit(l -> LayoutEdits.setMainCorner(l, team, 1, at));
            i = LayoutEdits.mainIndex(draft, team);
            pendingNewMain = true;
        }
        selected = Ref.main(i);
    }

    /** Свежесозданный мейн без углов: первый клик ставит оба угла (а не тянет бокс от 0,0). */
    private static boolean pendingNewMain;

    /**
     * Клик по блоку в режиме мира. {@code primary} — ЛКМ (угол 1), иначе ПКМ (угол 2 / позиция). Возвращает
     * ключ подсказки для ленты или null, если ничего не выбрано.
     */
    public static String worldClick(boolean primary, int x, int y, int z, float yaw) {
        if (selected == null || !selected.valid(draft)) return "pwpwarfare.editor.world.nothing_selected";
        Pos p = new Pos(x, y, z);
        Ref r = selected;
        switch (r.kind()) {
            case MAIN -> {
                MapLayout.MainZoneDef zone = draft.mainZones().get(r.index());
                Team t = zone.teamEnum() == null ? Team.BLUE : zone.teamEnum();
                if (pendingNewMain) {
                    edit(l -> LayoutEdits.setMainCorner(LayoutEdits.setMainCorner(l, t, 1, p), t, 2, p));
                    pendingNewMain = false;
                } else {
                    edit(l -> LayoutEdits.setMainCorner(l, t, primary ? 1 : 2, p));
                }
                return primary ? "pwpwarfare.editor.world.corner1" : "pwpwarfare.editor.world.corner2";
            }
            case POINT -> {
                edit(l -> LayoutEdits.setPointCorner(l, r.index(), primary ? 1 : 2, p));
                return primary ? "pwpwarfare.editor.world.corner1" : "pwpwarfare.editor.world.corner2";
            }
            case SPAWN -> {
                edit(l -> LayoutEdits.setSpawn(l, Team.values()[r.index()], new Pos(x, y + 1, z)));
                return "pwpwarfare.editor.world.spawn_set";
            }
            case SLOT -> {
                if (primary) {
                    edit(l -> {
                        MapLayout.VehicleSlot s = l.vehicleSlots().get(r.index());
                        return LayoutEdits.setSlotProps(l, r.index(), s.role(), s.vehicleName(), yaw);
                    });
                    return "pwpwarfare.editor.world.yaw_set";
                }
                edit(l -> LayoutEdits.moveSlot(l, r.index(), new Pos(x, y + 1, z)));
                return "pwpwarfare.editor.world.slot_moved";
            }
            case BORDER -> {
                if (primary) {
                    borderCorner = p;
                    return "pwpwarfare.editor.world.border_corner1";
                }
                if (borderCorner == null) return "pwpwarfare.editor.world.border_need_corner1";
                Pos a = borderCorner;
                edit(l -> LayoutEdits.setBorderFromCorners(l, a.x(), a.z(), x, z));
                borderCorner = null;
                return "pwpwarfare.editor.world.border_set";
            }
        }
        return null;
    }

    public static void deleteSelected() {
        Ref r = selected;
        if (r == null || !r.valid(draft)) return;
        switch (r.kind()) {
            case MAIN -> edit(l -> LayoutEdits.removeMain(l, r.index()));
            case POINT -> edit(l -> LayoutEdits.removePoint(l, r.index()));
            case SPAWN -> edit(l -> LayoutEdits.clearSpawn(l, Team.values()[r.index()]));
            case SLOT -> edit(l -> LayoutEdits.removeSlot(l, r.index()));
            case BORDER -> edit(LayoutEdits::clearBorder);
        }
        selected = null;
    }

    /** «Показать где»: мировой столб на {@link com.pigeostudios.pwp.warfare.client.gui.ui.WTheme#EDITOR_HIGHLIGHT_MS}. */
    public static void highlight(double[] xz, long durationMs) {
        highlight = xz;
        highlightUntil = System.currentTimeMillis() + durationMs;
    }

    public static double[] highlight() {
        return highlight != null && System.currentTimeMillis() < highlightUntil ? highlight : null;
    }

    // ---------- состояние ----------

    public static MapLayout draft() {
        return draft;
    }

    public static boolean loaded() {
        return loaded;
    }

    public static boolean dirty() {
        return dirty;
    }

    public static List<Issue> issues() {
        return issues;
    }

    public static Ref selected() {
        return selected != null && selected.valid(draft) ? selected : null;
    }

    public static void select(Ref r) {
        if (r == null || r.kind() != Kind.MAIN) pendingNewMain = false;
        selected = r;
    }

    public static boolean worldMode() {
        return worldMode;
    }

    public static void setWorldMode(boolean on) {
        worldMode = on;
        if (!on) borderCorner = null;
    }

    public static Pos borderCorner() {
        return borderCorner;
    }

    public static String mapName() {
        return mapName;
    }

    public static String source() {
        return source;
    }

    public static boolean canPersist() {
        return canPersist;
    }

    public static boolean coreAvailable() {
        return coreAvailable;
    }

    public static boolean matchRunning() {
        return matchRunning;
    }

    public static boolean matchEdit() {
        return matchEdit;
    }

    public static long versionId() {
        return versionId;
    }

    public static List<VersionRow> versions() {
        return versions;
    }

    public static boolean versionsAvailable() {
        return versionsAvailable;
    }

    /** Фикстуры тест-стенда. */
    public static void previewFlags(String map, boolean persist, List<VersionRow> rows) {
        mapName = map;
        canPersist = persist;
        coreAvailable = true;
        versionsAvailable = true;
        versions = List.copyOf(rows);
        loaded = true;
        dirty = false;
    }

    // ---------- JSON ----------

    private static JsonObject parseObject(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            LOG.warn("Map editor: bad server JSON", e);
            return null;
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    private static boolean bool(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() && o.get(k).getAsBoolean();
    }
}

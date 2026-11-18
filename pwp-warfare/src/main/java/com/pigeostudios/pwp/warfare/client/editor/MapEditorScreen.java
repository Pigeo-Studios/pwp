package com.pigeostudios.pwp.warfare.client.editor;

import com.pigeostudios.pwp.warfare.admin.MapEditorPackets;
import com.pigeostudios.pwp.warfare.client.gui.map.MapLayers;
import com.pigeostudios.pwp.warfare.client.gui.map.MapView;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.layout.Box;
import com.pigeostudios.pwp.warfare.layout.Issue;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Kind;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Ref;
import com.pigeostudios.pwp.warfare.layout.LayoutTransfer;
import com.pigeostudios.pwp.warfare.layout.MapLayout;
import com.pigeostudios.pwp.warfare.layout.Pos;
import com.pigeostudios.pwp.warfare.layout.Shapes;
import com.pigeostudios.pwp.warfare.layout.Team;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pwp.api.util.Parse;
import com.pwp.coreclient.admin.AdminBackend;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * Экран редактора карты (этап 3, ADMIN+): список объектов | тактическая карта | свойства + замечания/версии.
 *
 * <p>Карта: ЛКМ — выбрать объект и тащить (точки/мейны сдвигаются, спавн/слот/центр границы — в курсор), ПКМ
 * — панорама, колесо — масштаб. Список: точки идут в порядке цепочки AAS, порядок меняется перетаскиванием
 * строки; «Авто-порядок» — только предложение. «В мир» закрывает экран и включает режим мира: ЛКМ/ПКМ по
 * блоку ставят углы выбранного ({@link MapEditorInput}). Сеть — только через {@link AdminBackend} (в превью
 * тест-стенда пакеты не уходят).
 */
@OnlyIn(Dist.CLIENT)
public class MapEditorScreen extends Screen {
    private static final String[] ROLES = {
        "SUPPLY", "TRANSPORT", "MRAP", "APC", "IFV", "TANK", "AA", "CAS_FIGHTER", "HELI_TRANSPORT", "HELI_ATTACK"
    };
    private static final int POINT_HALF = 16;

    private final Screen parent;
    private final AdminBackend backend;
    private final UILayer layer = new UILayer();

    private Rect header = Rect.of(0, 0, 0, 0);
    private Rect list = Rect.of(0, 0, 0, 0);
    private Rect mapArea = Rect.of(0, 0, 0, 0);
    private Rect props = Rect.of(0, 0, 0, 0);
    private Rect bottom = Rect.of(0, 0, 0, 0);

    private double viewX = Double.NaN;
    private double viewZ;
    private double bpp = 4;

    private enum Drag {
        NONE,
        PAN,
        MOVE,
        REORDER
    }

    private Drag drag = Drag.NONE;
    private int reorderFrom = -1;
    private double dragAccX;
    private double dragAccZ;
    private double listScroll;
    private double sideScroll;
    private boolean showVersions;
    private Ref builtFor;

    private final List<ListRow> rows = new ArrayList<>();
    private final List<HitRow> issueHits = new ArrayList<>();
    private final List<HitRow> versionHits = new ArrayList<>();
    private final List<EditBox> fields = new ArrayList<>();
    private EditBox comment;

    private record ListRow(Rect r, Ref ref, Component label, int color) {}

    private record HitRow(Rect r, Runnable action) {}

    public MapEditorScreen(Screen parent, AdminBackend backend) {
        super(Component.translatable("pwpwarfare.editor.title"));
        this.parent = parent;
        this.backend = backend;
        if (!backend.preview()) {
            backend.request(MapEditorPackets.DATA_EDITOR, "");
            backend.request(MapEditorPackets.DATA_VERSIONS, "");
        }
        MapEditorSession.setWorldMode(false);
    }

    // ------------------------------------------------------------------ лейаут

    @Override
    protected void init() {
        layer.clear();
        fields.clear();
        int m = Math.max(WTheme.MARGIN_MIN, Math.min(WTheme.MARGIN_MAX, width / 60));
        Rect all = Rect.of(m, m, width - 2 * m, height - 2 * m);
        header = all.top(WTheme.FIELD_H + Spacing.XS);
        Rect body = all.inset(0, header.h() + WTheme.GAP, 0, 0);
        list = body.left(WTheme.EDITOR_LIST_W);
        props = body.rightStrip(WTheme.EDITOR_SIDE_W);
        mapArea = Rect.of(list.right() + WTheme.GAP, body.y(), props.x() - list.right() - 2 * WTheme.GAP, body.h());
        if (Double.isNaN(viewX)) fitView();

        // шапка: в мир / закрыть
        Rect hb = header.rightStrip(Math.min(220, header.w() / 2));
        int bw = (hb.w() - WTheme.GAP) / 2;
        button(
                        Rect.of(hb.x(), hb.y(), bw, WTheme.SMALL_BUTTON_H),
                        "pwpwarfare.editor.to_world",
                        UIIcon.SETTINGS,
                        this::toWorld)
                .setEnabled(!backend.preview() && MapEditorSession.selected() != null);
        button(
                Rect.of(hb.x() + bw + WTheme.GAP, hb.y(), bw, WTheme.SMALL_BUTTON_H),
                "pwpwarfare.editor.close",
                null,
                this::onClose);

        // низ списка: добавить / авто-порядок
        int bh = WTheme.SMALL_BUTTON_H;
        Rect lb = list.bottomStrip(bh * 2 + WTheme.GAP);
        int half = (lb.w() - WTheme.GAP) / 2;
        button(Rect.of(lb.x(), lb.y(), half, bh), "pwpwarfare.editor.add_point", null, this::addPoint);
        button(Rect.of(lb.x() + half + WTheme.GAP, lb.y(), half, bh), "pwpwarfare.editor.auto_order", null, () -> {
                    MapEditorSession.edit(LayoutEdits::autoOrder);
                    status(Component.translatable("pwpwarfare.editor.auto_order.done"), PWPTheme.Text.MUTED);
                })
                .tooltip(com.pwp.coreclient.ui.UITooltip.Content.of(
                        Component.translatable("pwpwarfare.editor.auto_order.tip")));
        button(
                Rect.of(lb.x(), lb.y() + bh + WTheme.GAP, half, bh),
                "pwpwarfare.editor.add_slot_blue",
                null,
                () -> addSlot(Team.BLUE));
        button(
                Rect.of(lb.x() + half + WTheme.GAP, lb.y() + bh + WTheme.GAP, half, bh),
                "pwpwarfare.editor.add_slot_red",
                null,
                () -> addSlot(Team.RED));
        list = Rect.of(list.x(), list.y(), list.w(), lb.y() - list.y() - WTheme.GAP);

        // правая колонка: свойства (верх), переключатель замечания/версии, сохранение (низ)
        int saveH = WTheme.FIELD_H + bh * 2 + WTheme.GAP * 3 + WTheme.ROW_H;
        bottom = props.bottomStrip(saveH);
        Rect top = Rect.of(props.x(), props.y(), props.w(), props.h() - saveH - WTheme.GAP);
        int propsH = buildProps(top);
        props = Rect.of(top.x(), top.y(), top.w(), propsH);
        Rect tabs = Rect.of(top.x(), props.bottom() + WTheme.GAP, top.w(), bh);
        int tw = (tabs.w() - WTheme.GAP) / 2;
        button(Rect.of(tabs.x(), tabs.y(), tw, bh), "pwpwarfare.editor.tab.issues", null, () -> {
                    showVersions = false;
                    sideScroll = 0;
                })
                .selected(!showVersions);
        button(Rect.of(tabs.x() + tw + WTheme.GAP, tabs.y(), tw, bh), "pwpwarfare.editor.tab.versions", null, () -> {
                    showVersions = true;
                    sideScroll = 0;
                    if (!backend.preview()) backend.request(MapEditorPackets.DATA_VERSIONS, "");
                })
                .selected(showVersions);
        sideList = Rect.of(top.x(), tabs.bottom() + WTheme.GAP, top.w(), top.bottom() - tabs.bottom() - WTheme.GAP);

        // сохранение
        comment = new EditBox(
                font, bottom.x() + 2, bottom.y() + 2, bottom.w() - 4, WTheme.FIELD_H - 4, Component.empty());
        comment.setMaxLength(MapEditorPackets.COMMENT_MAX);
        comment.setBordered(false);
        comment.setHint(Component.translatable("pwpwarfare.editor.comment.hint"));
        addRenderableWidget(comment);
        int y = bottom.y() + WTheme.FIELD_H + WTheme.GAP;
        button(Rect.of(bottom.x(), y, bottom.w(), bh), "pwpwarfare.editor.apply_match", null, this::applyMatch)
                .style(UIButton.Style.PRIMARY);
        UIButton persist = button(
                Rect.of(bottom.x(), y + bh + WTheme.GAP, (bottom.w() - WTheme.GAP) * 2 / 3, bh),
                "pwpwarfare.editor.save_permanent",
                null,
                this::savePermanent);
        if (!MapEditorSession.canPersist() || !MapEditorSession.coreAvailable()) {
            persist.setEnabled(false);
            persist.tooltip(com.pwp.coreclient.ui.UITooltip.Content.of(Component.translatable(
                    MapEditorSession.canPersist() ? "pwpwarfare.editor.no_core" : "pwpwarfare.editor.owner_only")));
        }
        int rx = bottom.x() + (bottom.w() - WTheme.GAP) * 2 / 3 + WTheme.GAP;
        button(
                Rect.of(rx, y + bh + WTheme.GAP, bottom.right() - rx, bh),
                "pwpwarfare.editor.revert",
                null,
                this::revert);
        builtFor = MapEditorSession.selected();
    }

    private Rect sideList = Rect.of(0, 0, 0, 0);

    private UIButton button(Rect r, String key, UIIcon icon, Runnable action) {
        UIButton b = new UIButton(Component.translatable(key), action).style(UIButton.Style.DARK);
        if (icon != null) b.icon(icon);
        b.setBounds(r);
        return layer.add(b);
    }

    /** Форма свойств выбранного объекта; возвращает занятую высоту. */
    private int buildProps(Rect area) {
        Ref sel = MapEditorSession.selected();
        MapLayout l = MapEditorSession.draft();
        int head = WTheme.ROW_H + Spacing.XS;
        int y = area.y() + head;
        int rowH = WTheme.FIELD_H + WTheme.GAP;
        int labelW = area.w() * 2 / 5;
        if (sel == null) return head + WTheme.ROW_H * 3;
        switch (sel.kind()) {
            case POINT -> {
                MapLayout.CapturePointDef p = l.points().get(sel.index());
                y = field(area, y, labelW, "pwpwarfare.editor.prop.name", p.name(), v -> editPoint(sel, F_NAME, v));
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.capture_time",
                        num(p.captureTimeMinutes()),
                        v -> editPoint(sel, F_TIME, v));
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.ticket_penalty",
                        num(p.ticketPenalty()),
                        v -> editPoint(sel, F_PENALTY, v));
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.capture_deduction",
                        num(p.captureDeduction()),
                        v -> editPoint(sel, F_DEDUCTION, v));
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.lock",
                        num(p.lockDurationMinutes()),
                        v -> editPoint(sel, F_LOCK, v));
                y = shapeButton(area, y, p.effShape(), s -> editPoint(sel, F_SHAPE, s));
            }
            case MAIN -> {
                MapLayout.MainZoneDef z = l.mainZones().get(sel.index());
                y = shapeButton(
                        area,
                        y,
                        z.effShape(),
                        s -> MapEditorSession.edit(x -> LayoutEdits.setMainShape(x, sel.index(), s)));
            }
            case SLOT -> {
                MapLayout.VehicleSlot s = l.vehicleSlots().get(sel.index());
                String role = s.role() == null ? "" : s.role();
                UIButton rb = button(
                        Rect.of(area.x(), y, area.w(), WTheme.SMALL_BUTTON_H),
                        "pwpwarfare.editor.prop.role",
                        null,
                        () -> {
                            MapLayout.VehicleSlot cur =
                                    MapEditorSession.draft().vehicleSlots().get(sel.index());
                            MapEditorSession.edit(x -> LayoutEdits.setSlotProps(
                                    x, sel.index(), nextRole(cur.role()), cur.vehicleName(), cur.effYaw()));
                            rebuildWidgets();
                        });
                rb.label(Component.translatable("pwpwarfare.editor.prop.role", role.isEmpty() ? "—" : role));
                y += rowH;
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.vehicle",
                        s.vehicleName() == null ? "" : s.vehicleName(),
                        v -> {
                            MapLayout.VehicleSlot cur =
                                    MapEditorSession.draft().vehicleSlots().get(sel.index());
                            MapEditorSession.edit(
                                    x -> LayoutEdits.setSlotProps(x, sel.index(), cur.role(), v, cur.effYaw()));
                        });
                y = field(area, y, labelW, "pwpwarfare.editor.prop.yaw", String.valueOf(Math.round(s.effYaw())), v -> {
                    MapLayout.VehicleSlot cur =
                            MapEditorSession.draft().vehicleSlots().get(sel.index());
                    MapEditorSession.edit(x -> LayoutEdits.setSlotProps(
                            x, sel.index(), cur.role(), cur.vehicleName(), Parse.floatOr(v, cur.effYaw())));
                });
            }
            case BORDER -> {
                MapLayout.Border b = l.border();
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.center_x",
                        b == null ? "" : num(b.centerX()),
                        v -> editBorder(v, null, null));
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.center_z",
                        b == null ? "" : num(b.centerZ()),
                        v -> editBorder(null, v, null));
                y = field(
                        area,
                        y,
                        labelW,
                        "pwpwarfare.editor.prop.size",
                        b == null ? "" : num(b.sizeBlocks()),
                        v -> editBorder(null, null, v));
            }
            case SPAWN -> y += WTheme.ROW_H;
        }
        int bw = (area.w() - WTheme.GAP) / 2;
        button(Rect.of(area.x(), y, bw, WTheme.SMALL_BUTTON_H), "pwpwarfare.editor.delete", null, () -> {
            MapEditorSession.deleteSelected();
            rebuildWidgets();
        });
        button(
                Rect.of(area.x() + bw + WTheme.GAP, y, bw, WTheme.SMALL_BUTTON_H),
                "pwpwarfare.editor.show",
                null,
                () -> show(sel));
        y += WTheme.SMALL_BUTTON_H + WTheme.ROW_H + Spacing.XS;
        return y - area.y();
    }

    private int field(Rect area, int y, int labelW, String labelKey, String value, Consumer<String> onChange) {
        EditBox box = new EditBox(
                font,
                area.x() + labelW + 2,
                y + 3,
                area.w() - labelW - 4,
                WTheme.FIELD_H - 4,
                Component.translatable(labelKey));
        box.setBordered(false);
        box.setMaxLength(64);
        box.setValue(value == null ? "" : value);
        box.setResponder(onChange);
        box.setMessage(Component.translatable(labelKey));
        fields.add(box);
        addRenderableWidget(box);
        return y + WTheme.FIELD_H + WTheme.GAP;
    }

    private int shapeButton(Rect area, int y, String shape, Consumer<String> set) {
        boolean cyl = Shapes.isCylinder(shape);
        UIButton b = button(
                Rect.of(area.x(), y, area.w(), WTheme.SMALL_BUTTON_H), "pwpwarfare.editor.prop.shape", null, () -> {
                    set.accept(cyl ? Shapes.BOX : Shapes.CYLINDER);
                    rebuildWidgets();
                });
        b.label(Component.translatable(
                "pwpwarfare.editor.prop.shape",
                Component.translatable(cyl ? "pwpwarfare.editor.shape.cylinder" : "pwpwarfare.editor.shape.box")));
        return y + WTheme.SMALL_BUTTON_H + WTheme.GAP;
    }

    // ------------------------------------------------------------------ правки

    private static String num(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }

    /** Пустое поле = ключа нет (дефолт игры); мусор — тоже null (валидатор не спорит, игра берёт дефолт). */
    private static Integer opt(String v) {
        if (v == null || v.isBlank()) return null;
        int n = Parse.intOr(v.trim(), Integer.MIN_VALUE);
        return n == Integer.MIN_VALUE || n < 0 ? null : n;
    }

    private static final int F_NAME = 0, F_TIME = 1, F_PENALTY = 2, F_DEDUCTION = 3, F_LOCK = 4, F_SHAPE = 5;

    /** Правка одного поля точки; пустое числовое поле = ключа нет (дефолт игры). */
    private void editPoint(Ref sel, int which, String value) {
        MapEditorSession.edit(l -> {
            MapLayout.CapturePointDef p = l.points().get(sel.index());
            return LayoutEdits.replacePoint(
                    l,
                    sel.index(),
                    LayoutEdits.withParams(
                            p,
                            which == F_NAME ? (value.isBlank() ? p.name() : value.trim()) : p.name(),
                            which == F_SHAPE ? value : p.shape(),
                            which == F_TIME ? opt(value) : p.captureTimeMinutes(),
                            which == F_PENALTY ? opt(value) : p.ticketPenalty(),
                            which == F_DEDUCTION ? opt(value) : p.captureDeduction(),
                            which == F_LOCK ? opt(value) : p.lockDurationMinutes()));
        });
    }

    private void editBorder(String cx, String cz, String size) {
        MapEditorSession.edit(l -> {
            MapLayout.Border b = l.border();
            int x = cx != null
                    ? Parse.intOr(cx.trim(), b != null && b.centerX() != null ? b.centerX() : 0)
                    : b != null && b.centerX() != null ? b.centerX() : 0;
            int z = cz != null
                    ? Parse.intOr(cz.trim(), b != null && b.centerZ() != null ? b.centerZ() : 0)
                    : b != null && b.centerZ() != null ? b.centerZ() : 0;
            int s = size != null
                    ? Parse.intOr(size.trim(), b != null && b.sizeBlocks() != null ? b.sizeBlocks() : 1)
                    : b != null && b.sizeBlocks() != null ? b.sizeBlocks() : 1;
            return LayoutEdits.setBorder(l, x, z, s);
        });
    }

    private static String nextRole(String role) {
        for (int i = 0; i < ROLES.length; i++) {
            if (ROLES[i].equalsIgnoreCase(role == null ? "" : role)) return ROLES[(i + 1) % ROLES.length];
        }
        return ROLES[0];
    }

    private Pos playerPos() {
        var p = Minecraft.getInstance().player;
        if (p == null) return new Pos((int) Math.floor(viewX), 64, (int) Math.floor(viewZ));
        return new Pos(p.getBlockX(), p.getBlockY(), p.getBlockZ());
    }

    private void addPoint() {
        Pos p = playerPos();
        // в превью/без игрока — центр карты
        int x = Minecraft.getInstance().player == null ? (int) viewX : p.x();
        int z = Minecraft.getInstance().player == null ? (int) viewZ : p.z();
        MapEditorSession.addPointAt(x, p.y(), z, POINT_HALF);
        rebuildWidgets();
    }

    private void addSlot(Team team) {
        Pos p = playerPos();
        var pl = Minecraft.getInstance().player;
        MapEditorSession.addSlotAt(team, p.x(), p.y(), p.z(), pl == null ? 0f : pl.getYRot());
        rebuildWidgets();
    }

    private void select(Ref r) {
        if (r != null && r.kind() == Kind.MAIN && r.index() < 0) return;
        MapEditorSession.select(r);
        if (!java.util.Objects.equals(builtFor, MapEditorSession.selected())) rebuildWidgets();
    }

    private void show(Ref r) {
        double[] xz = LayoutEdits.locate(MapEditorSession.draft(), r);
        if (xz == null) return;
        viewX = xz[0];
        viewZ = xz[1];
        MapEditorSession.highlight(xz, WTheme.EDITOR_HIGHLIGHT_MS);
    }

    // ------------------------------------------------------------------ сеть

    private void applyMatch() {
        String json = MapEditorSession.json();
        String err = LayoutTransfer.checkSize(json);
        if (err != null) {
            status(Component.translatable("pwpwarfare.editor.error.too_large"), PWPTheme.Text.DANGER);
            return;
        }
        backend.perform(
                MapEditorPackets.APPLY_MATCH,
                () -> PacketHandler.INSTANCE.sendToServer(new MapEditorPackets.ApplyForMatch(json)));
        status(Component.translatable("pwpwarfare.editor.sent"), PWPTheme.Text.MUTED);
    }

    private void savePermanent() {
        String json = MapEditorSession.json();
        if (LayoutTransfer.checkSize(json) != null) {
            status(Component.translatable("pwpwarfare.editor.error.too_large"), PWPTheme.Text.DANGER);
            return;
        }
        String c = comment == null ? "" : comment.getValue();
        backend.perform(
                MapEditorPackets.SAVE_PERMANENT,
                () -> PacketHandler.INSTANCE.sendToServer(new MapEditorPackets.SavePermanent(json, c)));
        status(Component.translatable("pwpwarfare.editor.sent"), PWPTheme.Text.MUTED);
    }

    private void rollback(long id) {
        backend.perform(
                MapEditorPackets.ROLLBACK,
                () -> PacketHandler.INSTANCE.sendToServer(new MapEditorPackets.Rollback(id)));
        status(Component.translatable("pwpwarfare.editor.sent"), PWPTheme.Text.MUTED);
    }

    private void revert() {
        if (backend.preview()) return;
        MapEditorClient.forceNextLoad();
        backend.request(MapEditorPackets.DATA_EDITOR, "");
    }

    private void toWorld() {
        MapEditorSession.setWorldMode(true);
        Minecraft.getInstance().setScreen(null);
    }

    private static Component statusText;
    private static int statusColor;

    static void status(Component text, int color) {
        statusText = text;
        statusColor = color;
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    public void tick() {
        super.tick();
        if (!java.util.Objects.equals(builtFor, MapEditorSession.selected())) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        MapLayout l = MapEditorSession.draft();
        // шапка
        Component title = Component.translatable(
                "pwpwarfare.editor.header",
                MapEditorSession.mapName().isEmpty() ? "—" : MapEditorSession.mapName(),
                Component.translatable(
                        MapEditorSession.dirty() ? "pwpwarfare.editor.state.dirty" : "pwpwarfare.editor.state.saved"));
        WDraw.text(g, font, title, header.withWidth(header.w() / 2), Align.START, PWPTheme.Text.HEADING, true, 1f);
        if (statusText != null) {
            WDraw.text(
                    g,
                    font,
                    statusText,
                    Rect.of(header.x() + header.w() / 2 - 120, header.y(), 230, header.h()),
                    Align.END,
                    statusColor,
                    true,
                    1f);
        }

        renderList(g, l, mx, my);
        renderMap(g, l, mx, my);
        Rect content = WDraw.panel(
                g,
                font,
                Rect.of(props.x(), props.y(), props.w(), props.h()),
                WTheme.ROW_H,
                WarfareIcon.EDIT,
                describe(l, MapEditorSession.selected()),
                null,
                1f);
        Ref sel = MapEditorSession.selected();
        if (sel == null) {
            WDraw.paragraph(
                    g,
                    font,
                    Component.translatable("pwpwarfare.editor.select_hint"),
                    content,
                    3,
                    PWPTheme.Text.MUTED,
                    1f);
        } else {
            for (EditBox f : fields) {
                Rect fr = Rect.of(f.getX() - 2, f.getY() - 3, f.getWidth() + 4, WTheme.FIELD_H);
                WDraw.field(g, fr, f.isFocused(), 1f);
                WDraw.text(
                        g,
                        font,
                        f.getMessage(),
                        Rect.of(content.x(), fr.y(), fr.x() - content.x() - 2, fr.h()),
                        Align.START,
                        PWPTheme.Text.MUTED,
                        false,
                        1f);
            }
            if (sel.kind() == Kind.SPAWN || sel.kind() == Kind.MAIN) {
                double[] xz = LayoutEdits.locate(l, sel);
                if (xz != null) {
                    WDraw.text(
                            g,
                            font,
                            Component.literal((int) xz[0] + ", " + (int) xz[1]),
                            Rect.of(
                                    content.x(),
                                    props.bottom() - WTheme.SMALL_BUTTON_H - WTheme.ROW_H * 2,
                                    content.w(),
                                    WTheme.ROW_H),
                            Align.START,
                            PWPTheme.Text.MUTED,
                            false,
                            1f);
                }
            }
            WDraw.text(
                    g,
                    font,
                    Component.translatable(worldHintKey(sel)),
                    Rect.of(content.x(), props.bottom() - WTheme.ROW_H - 2, content.w(), WTheme.ROW_H),
                    Align.START,
                    PWPTheme.Text.CAPTION,
                    false,
                    1f);
        }
        if (showVersions) renderVersions(g, mx, my);
        else renderIssues(g, l, mx, my);
        // поле комментария
        Rect cr = Rect.of(bottom.x(), bottom.y(), bottom.w(), WTheme.FIELD_H);
        WDraw.field(g, cr, comment != null && comment.isFocused(), 1f);
        super.render(g, mx, my, pt);
        layer.render(g, mx, my, pt, width, height);
    }

    private void renderList(GuiGraphics g, MapLayout l, int mx, int my) {
        rows.clear();
        Rect c = WDraw.panel(
                g,
                font,
                list,
                WTheme.ROW_H,
                WarfareIcon.LAYERS,
                Component.translatable("pwpwarfare.editor.objects"),
                null,
                1f);
        int y = c.y() - (int) listScroll;
        int rh = WTheme.ROW_H + 2;
        List<Object[]> entries = new ArrayList<>();
        entries.add(new Object[] {null, Component.translatable("pwpwarfare.editor.section.mains")});
        for (Team t : Team.values()) {
            int i = LayoutEdits.mainIndex(l, t);
            entries.add(new Object[] {
                i < 0 ? new Ref(Kind.MAIN, -1 - t.ordinal()) : Ref.main(i),
                Component.translatable(i < 0 ? "pwpwarfare.editor.main.missing" : "pwpwarfare.editor.main", t.name())
            });
        }
        for (Team t : Team.values()) {
            entries.add(new Object[] {
                Ref.spawn(t),
                Component.translatable(
                        l.spawn(t) == null ? "pwpwarfare.editor.spawn.derived" : "pwpwarfare.editor.spawn", t.name())
            });
        }
        entries.add(new Object[] {
            Ref.border(),
            Component.translatable(l.border() == null ? "pwpwarfare.editor.border.derived" : "pwpwarfare.editor.border")
        });
        entries.add(new Object[] {
            null,
            Component.translatable(
                    "pwpwarfare.editor.section.points", l.points().size())
        });
        for (int i = 0; i < l.points().size(); i++) {
            entries.add(new Object[] {
                Ref.point(i),
                Component.literal((i + 1) + ". " + l.points().get(i).name())
            });
        }
        entries.add(new Object[] {
            null,
            Component.translatable(
                    "pwpwarfare.editor.section.slots", l.vehicleSlots().size())
        });
        for (int i = 0; i < l.vehicleSlots().size(); i++) {
            MapLayout.VehicleSlot s = l.vehicleSlots().get(i);
            String what = s.role() != null ? s.role() : s.vehicleName() != null ? s.vehicleName() : "?";
            entries.add(
                    new Object[] {Ref.slot(i), Component.literal(s.team().name().charAt(0) + " · " + what)});
        }
        g.enableScissor(c.x(), c.y(), c.right(), c.bottom());
        Ref sel = MapEditorSession.selected();
        for (Object[] e : entries) {
            Rect r = Rect.of(c.x(), y, c.w(), rh - 1);
            Ref ref = (Ref) e[0];
            Component label = (Component) e[1];
            if (ref == null) {
                WDraw.text(g, font, label, r, Align.START, PWPTheme.Text.ACCENT, false, 1f);
            } else {
                boolean hover = r.contains(mx, my) && c.contains(mx, my);
                boolean selected = ref.equals(sel);
                WDraw.tile(g, r, hover ? 1f : 0f, selected ? 1f : 0f, 1f, 1f);
                WDraw.text(g, font, label, r.inset(Spacing.XS, 0), Align.START, colorOf(l, ref), false, 1f);
                rows.add(new ListRow(r, ref, label, 0));
            }
            y += rh;
        }
        // перетаскивание точки: линия места вставки
        if (drag == Drag.REORDER) {
            int target = reorderTarget(my);
            for (ListRow r : rows) {
                if (r.ref.kind() == Kind.POINT && r.ref.index() == target) {
                    UIDraw.hline(
                            g,
                            r.r.x(),
                            r.r.right(),
                            target > reorderFrom ? r.r.bottom() : r.r.y(),
                            WTheme.EDITOR_HIGHLIGHT);
                }
            }
        }
        g.disableScissor();
        listContent = y + (int) listScroll - c.y();
        listView = c;
    }

    private int listContent;
    private Rect listView = Rect.of(0, 0, 0, 0);

    private int colorOf(MapLayout l, Ref ref) {
        return switch (ref.kind()) {
            case MAIN -> ref.index() < 0
                    ? PWPTheme.Text.DANGER
                    : l.mainZones().get(ref.index()).teamEnum() == Team.RED
                            ? WTheme.EDITOR_MAIN_RED
                            : WTheme.EDITOR_MAIN_BLUE;
            case SPAWN -> Team.values()[ref.index()] == Team.RED ? WTheme.EDITOR_MAIN_RED : WTheme.EDITOR_MAIN_BLUE;
            case SLOT -> l.vehicleSlots().get(ref.index()).team() == Team.RED
                    ? WTheme.EDITOR_MAIN_RED
                    : WTheme.EDITOR_MAIN_BLUE;
            case POINT -> WTheme.EDITOR_POINT;
            case BORDER -> WTheme.EDITOR_BORDER;
        };
    }

    private MapView view() {
        return new MapView(mapArea, viewX, viewZ, bpp);
    }

    private void fitView() {
        MapLayout l = MapEditorSession.draft();
        MapLayout.Border b = com.pigeostudios.pwp.warfare.layout.LayoutDerive.resolve(l)
                .layout()
                .border();
        int minSide = Math.max(1, Math.min(mapArea.w(), mapArea.h()));
        if (b != null && b.complete()) {
            viewX = b.centerX();
            viewZ = b.centerZ();
            bpp = Math.max(0.25, b.sizeBlocks() * 1.1 / minSide);
            return;
        }
        Pos p = playerPos();
        viewX = p.x();
        viewZ = p.z();
        bpp = 4;
    }

    private boolean fitted;

    private void renderMap(GuiGraphics g, MapLayout l, int mx, int my) {
        if (!fitted && MapEditorSession.loaded()) {
            fitView();
            fitted = true;
        }
        MapView v = view();
        g.enableScissor(mapArea.x(), mapArea.y(), mapArea.right(), mapArea.bottom());
        MapLayers.background(g, v);
        MapLayers.tiles(g, v);
        MapLayers.grid(g, v);
        Ref sel = MapEditorSession.selected();
        MapLayout.Border b = l.border();
        if (b != null && b.complete()) {
            double h = b.half();
            outline(
                    g,
                    v,
                    b.centerX() - h,
                    b.centerZ() - h,
                    b.centerX() + h,
                    b.centerZ() + h,
                    isSel(sel, Kind.BORDER, 0) ? WTheme.EDITOR_SELECTED : WTheme.EDITOR_BORDER);
        }
        for (int i = 0; i < l.mainZones().size(); i++) {
            MapLayout.MainZoneDef z = l.mainZones().get(i);
            int c = isSel(sel, Kind.MAIN, i)
                    ? WTheme.EDITOR_SELECTED
                    : z.teamEnum() == Team.RED ? WTheme.EDITOR_MAIN_RED : WTheme.EDITOR_MAIN_BLUE;
            zone(g, v, z.box(), z.shape(), c);
        }
        // цепочка AAS: линии между точками в порядке списка
        for (int i = 1; i < l.points().size(); i++) {
            Box a = l.points().get(i - 1).box();
            Box c = l.points().get(i).box();
            dotted(g, v.sx(a.centerX()), v.sy(a.centerZ()), v.sx(c.centerX()), v.sy(c.centerZ()), WTheme.EDITOR_CHAIN);
        }
        for (int i = 0; i < l.points().size(); i++) {
            MapLayout.CapturePointDef p = l.points().get(i);
            int c = isSel(sel, Kind.POINT, i) ? WTheme.EDITOR_SELECTED : WTheme.EDITOR_POINT;
            zone(g, v, p.box(), p.shape(), c);
            UIDraw.centered(
                    g,
                    font,
                    Component.literal(String.valueOf(i + 1)),
                    (int) v.sx(p.box().centerX()),
                    (int) v.sy(p.box().centerZ()),
                    c);
        }
        for (Team t : Team.values()) {
            MapLayout.TeamSpawn s = l.spawn(t);
            if (s == null) continue;
            mark(
                    g,
                    v.sx(s.x() + 0.5),
                    v.sy(s.z() + 0.5),
                    isSel(sel, Kind.SPAWN, t.ordinal())
                            ? WTheme.EDITOR_SELECTED
                            : t == Team.RED ? WTheme.EDITOR_MAIN_RED : WTheme.EDITOR_MAIN_BLUE,
                    true);
        }
        for (int i = 0; i < l.vehicleSlots().size(); i++) {
            MapLayout.VehicleSlot s = l.vehicleSlots().get(i);
            mark(
                    g,
                    v.sx(s.pos().x() + 0.5),
                    v.sy(s.pos().z() + 0.5),
                    isSel(sel, Kind.SLOT, i)
                            ? WTheme.EDITOR_SELECTED
                            : s.team() == Team.RED ? WTheme.EDITOR_MAIN_RED : WTheme.EDITOR_MAIN_BLUE,
                    false);
        }
        double[] hl = MapEditorSession.highlight();
        if (hl != null) {
            int hx = (int) v.sx(hl[0]);
            int hy = (int) v.sy(hl[1]);
            UIDraw.outline(g, Rect.of(hx - 8, hy - 8, 16, 16), WTheme.EDITOR_HIGHLIGHT);
        }
        var pl = Minecraft.getInstance().player;
        if (pl != null)
            UIDraw.rect(g, Rect.of((int) v.sx(pl.getX()) - 1, (int) v.sy(pl.getZ()) - 1, 3, 3), PWPTheme.Text.HEADING);
        if (mapArea.contains(mx, my)) {
            String kp = MapView.kp(v.worldX(mx), v.worldZ(my)) + "  " + (int) v.worldX(mx) + ", " + (int) v.worldZ(my);
            WDraw.text(
                    g,
                    font,
                    Component.literal(kp),
                    Rect.of(mapArea.x() + Spacing.XS, mapArea.bottom() - WTheme.ROW_H - 2, mapArea.w(), WTheme.ROW_H),
                    Align.START,
                    WTheme.MAP_LABEL_TEXT,
                    true,
                    1f);
        }
        g.disableScissor();
        UIDraw.outline(g, mapArea, WTheme.MAP_LABEL_BORDER);
    }

    private void renderIssues(GuiGraphics g, MapLayout l, int mx, int my) {
        issueHits.clear();
        List<Issue> issues = MapEditorSession.issues();
        long errors = issues.stream().filter(Issue::isError).count();
        Component title = Component.translatable("pwpwarfare.editor.issues", errors, issues.size() - errors);
        Rect c = WDraw.panel(g, font, sideList, WTheme.ROW_H, WarfareIcon.RULES, title, null, 1f);
        if (issues.isEmpty()) {
            WDraw.paragraph(g, font, Component.translatable("pwpwarfare.editor.ready"), c, 3, WTheme.STATUS_OK, 1f);
            return;
        }
        g.enableScissor(c.x(), c.y(), c.right(), c.bottom());
        int y = c.y() - (int) sideScroll;
        int showW = font.width(Component.translatable("pwpwarfare.editor.show")) + Spacing.SM;
        for (Issue i : issues) {
            Component t =
                    Component.translatable(i.langKey()).append(i.subject().isEmpty() ? "" : " [" + i.subject() + "]");
            int lines = Math.min(3, font.split(t, c.w() - showW - 2).size());
            int h = lines * (font.lineHeight + 1) + 2;
            Rect r = Rect.of(c.x(), y, c.w(), h);
            WDraw.paragraph(
                    g,
                    font,
                    t,
                    Rect.of(r.x(), r.y() + 1, r.w() - showW - 2, h),
                    3,
                    i.isError() ? WTheme.STATUS_BAD : WTheme.STATUS_WARN,
                    1f);
            Ref ref = LayoutEdits.refOf(l, i);
            if (ref != null && LayoutEdits.locate(l, ref) != null) {
                Rect sb = Rect.of(r.right() - showW, r.y(), showW, WTheme.ROW_H);
                WDraw.tile(g, sb, sb.contains(mx, my) ? 1f : 0f, 0f, 1f, 1f);
                WDraw.text(
                        g,
                        font,
                        Component.translatable("pwpwarfare.editor.show"),
                        sb,
                        Align.CENTER,
                        PWPTheme.Text.BODY,
                        false,
                        1f);
                issueHits.add(new HitRow(sb, () -> {
                    select(ref);
                    show(ref);
                }));
            }
            y += h + 2;
        }
        g.disableScissor();
        sideContent = y + (int) sideScroll - c.y();
        sideView = c;
    }

    private int sideContent;
    private Rect sideView = Rect.of(0, 0, 0, 0);

    private void renderVersions(GuiGraphics g, int mx, int my) {
        versionHits.clear();
        Rect c = WDraw.panel(
                g,
                font,
                sideList,
                WTheme.ROW_H,
                WarfareIcon.CLOCK,
                Component.translatable("pwpwarfare.editor.versions"),
                null,
                1f);
        List<MapEditorSession.VersionRow> vs = MapEditorSession.versions();
        if (!MapEditorSession.versionsAvailable() || vs.isEmpty()) {
            WDraw.paragraph(
                    g,
                    font,
                    Component.translatable(
                            MapEditorSession.versionsAvailable()
                                    ? "pwpwarfare.editor.versions.empty"
                                    : "pwpwarfare.editor.versions.unavailable"),
                    c,
                    4,
                    PWPTheme.Text.MUTED,
                    1f);
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm");
        g.enableScissor(c.x(), c.y(), c.right(), c.bottom());
        int y = c.y() - (int) sideScroll;
        int rbW = font.width(Component.translatable("pwpwarfare.editor.rollback")) + Spacing.SM;
        for (MapEditorSession.VersionRow v : vs) {
            int h = WTheme.ROW_H * 2;
            Rect r = Rect.of(c.x(), y, c.w(), h);
            boolean current = v.id() == MapEditorSession.versionId();
            WDraw.tile(g, r, 0f, current ? 1f : 0f, 1f, 1f);
            WDraw.text(
                    g,
                    font,
                    Component.literal("#" + v.id() + " · " + v.author() + " · " + fmt.format(new Date(v.createdAt()))),
                    Rect.of(r.x() + 2, r.y(), r.w() - rbW - 4, WTheme.ROW_H),
                    Align.START,
                    PWPTheme.Text.BODY,
                    false,
                    1f);
            WDraw.text(
                    g,
                    font,
                    Component.literal(v.comment().isEmpty() ? "—" : v.comment()),
                    Rect.of(r.x() + 2, r.y() + WTheme.ROW_H, r.w() - 4, WTheme.ROW_H),
                    Align.START,
                    PWPTheme.Text.MUTED,
                    false,
                    1f);
            if (MapEditorSession.canPersist()) {
                Rect rb = Rect.of(r.right() - rbW, r.y(), rbW, WTheme.ROW_H);
                WDraw.tile(g, rb, rb.contains(mx, my) ? 1f : 0f, 0f, 1f, 1f);
                WDraw.text(
                        g,
                        font,
                        Component.translatable("pwpwarfare.editor.rollback"),
                        rb,
                        Align.CENTER,
                        PWPTheme.Text.BODY,
                        false,
                        1f);
                long id = v.id();
                versionHits.add(new HitRow(rb, () -> rollback(id)));
            }
            y += h + 2;
        }
        g.disableScissor();
        sideContent = y + (int) sideScroll - c.y();
        sideView = c;
    }

    // ------------------------------------------------------------------ примитивы карты

    private static boolean isSel(Ref sel, Kind k, int i) {
        return sel != null && sel.kind() == k && sel.index() == i;
    }

    private void zone(GuiGraphics g, MapView v, Box b, String shape, int color) {
        double x0 = v.sx(b.minX()), y0 = v.sy(b.minZ()), x1 = v.sx(b.maxX() + 1), y1 = v.sy(b.maxZ() + 1);
        if (!Shapes.isCylinder(shape)) {
            Rect r = Rect.of((int) x0, (int) y0, Math.max(1, (int) (x1 - x0)), Math.max(1, (int) (y1 - y0)));
            UIDraw.rect(g, r, Colors.multiplyAlpha(color, WTheme.EDITOR_FILL_ALPHA));
            UIDraw.outline(g, r, color);
            return;
        }
        double cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        double r = Math.max(1, b.radius() / v.bpp());
        int steps = (int) Math.max(24, Math.min(720, r * 6));
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            int px = (int) Math.round(cx + Math.cos(a) * r);
            int py = (int) Math.round(cy + Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
        int rr = (int) (r * 0.7);
        UIDraw.rect(
                g,
                Rect.of((int) cx - rr, (int) cy - rr, rr * 2, rr * 2),
                Colors.multiplyAlpha(color, WTheme.EDITOR_FILL_ALPHA));
    }

    private void outline(GuiGraphics g, MapView v, double wx0, double wz0, double wx1, double wz1, int color) {
        int x0 = (int) v.sx(wx0), y0 = (int) v.sy(wz0), x1 = (int) v.sx(wx1), y1 = (int) v.sy(wz1);
        UIDraw.outline(g, Rect.of(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0)), color);
    }

    private static void mark(GuiGraphics g, double x, double y, int color, boolean diamond) {
        int s = WTheme.EDITOR_MARK;
        int ix = (int) x, iy = (int) y;
        if (diamond) {
            for (int d = 0; d <= s; d++) g.fill(ix - (s - d), iy - d, ix + (s - d) + 1, iy - d + 1, color);
            for (int d = 0; d <= s; d++) g.fill(ix - (s - d), iy + d, ix + (s - d) + 1, iy + d + 1, color);
        } else {
            UIDraw.rect(g, Rect.of(ix - s, iy - s, s * 2 + 1, s * 2 + 1), color);
        }
    }

    private static void dotted(GuiGraphics g, double x0, double y0, double x1, double y1, int color) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int n = (int) (len / 4);
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            int x = (int) (x0 + (x1 - x0) * t), y = (int) (y0 + (y1 - y0) * t);
            g.fill(x, y, x + 2, y + 2, color);
        }
    }

    /** Ближайший объект под курсором карты (маркеры важнее зон). */
    private Ref hitTest(double mx, double my) {
        MapLayout l = MapEditorSession.draft();
        MapView v = view();
        double best = WTheme.EDITOR_HIT_R;
        Ref hit = null;
        for (int i = 0; i < l.vehicleSlots().size(); i++) {
            Pos p = l.vehicleSlots().get(i).pos();
            double d = Math.hypot(v.sx(p.x() + 0.5) - mx, v.sy(p.z() + 0.5) - my);
            if (d <= best) {
                best = d;
                hit = Ref.slot(i);
            }
        }
        for (Team t : Team.values()) {
            MapLayout.TeamSpawn s = l.spawn(t);
            if (s == null) continue;
            double d = Math.hypot(v.sx(s.x() + 0.5) - mx, v.sy(s.z() + 0.5) - my);
            if (d <= best) {
                best = d;
                hit = Ref.spawn(t);
            }
        }
        if (hit != null) return hit;
        double wx = v.worldX(mx), wz = v.worldZ(my);
        for (int i = l.points().size() - 1; i >= 0; i--) {
            if (l.points().get(i).box().containsXZ(wx, wz)) return Ref.point(i);
        }
        for (int i = 0; i < l.mainZones().size(); i++) {
            if (l.mainZones().get(i).box().containsXZ(wx, wz)) return Ref.main(i);
        }
        MapLayout.Border b = l.border();
        if (b != null && b.complete()) {
            double h = b.half();
            double edge = Math.min(
                    Math.min(Math.abs(wx - (b.centerX() - h)), Math.abs(wx - (b.centerX() + h))),
                    Math.min(Math.abs(wz - (b.centerZ() - h)), Math.abs(wz - (b.centerZ() + h))));
            if (edge / v.bpp() <= WTheme.EDITOR_HIT_R && b.containsXZ(wx, wz)) return Ref.border();
        }
        return null;
    }

    private int reorderTarget(double my) {
        int last = -1;
        for (ListRow r : rows) {
            if (r.ref.kind() != Kind.POINT) continue;
            last = r.ref.index();
            if (my < r.r.bottom()) return r.ref.index();
        }
        return last;
    }

    // ------------------------------------------------------------------ ввод

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        for (HitRow h : showVersions ? versionHits : issueHits) {
            if (h.r.contains(mx, my) && sideView.contains(mx, my)) {
                h.action.run();
                return true;
            }
        }
        if (super.mouseClicked(mx, my, button)) return true;
        for (EditBox f : fields) f.setFocused(false);
        if (listView.contains(mx, my) && button == 0) {
            for (ListRow r : rows) {
                if (!r.r.contains(mx, my)) continue;
                if (r.ref.kind() == Kind.MAIN && r.ref.index() < 0) {
                    MapEditorSession.selectMain(Team.values()[-1 - r.ref.index()], playerPos());
                    rebuildWidgets();
                } else {
                    select(r.ref);
                    if (r.ref.kind() == Kind.POINT) {
                        drag = Drag.REORDER;
                        reorderFrom = r.ref.index();
                    }
                }
                return true;
            }
        }
        if (mapArea.contains(mx, my)) {
            if (button == 1) {
                drag = Drag.PAN;
                return true;
            }
            if (button == 0) {
                Ref hit = hitTest(mx, my);
                select(hit);
                drag = hit == null ? Drag.PAN : Drag.MOVE;
                dragAccX = 0;
                dragAccZ = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (layer.mouseDragged(mx, my, button, dx, dy)) return true;
        switch (drag) {
            case PAN -> {
                viewX -= dx * bpp;
                viewZ -= dy * bpp;
                return true;
            }
            case MOVE -> {
                moveSelected(mx, my, dx, dy);
                return true;
            }
            case REORDER -> {
                return true;
            }
            default -> {}
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private void moveSelected(double mx, double my, double dx, double dy) {
        Ref sel = MapEditorSession.selected();
        if (sel == null) return;
        MapView v = view();
        int wx = (int) Math.floor(v.worldX(mx));
        int wz = (int) Math.floor(v.worldZ(my));
        switch (sel.kind()) {
            case POINT, MAIN -> {
                dragAccX += dx * bpp;
                dragAccZ += dy * bpp;
                int ix = (int) dragAccX, iz = (int) dragAccZ;
                if (ix == 0 && iz == 0) return;
                dragAccX -= ix;
                dragAccZ -= iz;
                if (sel.kind() == Kind.POINT)
                    MapEditorSession.edit(l -> LayoutEdits.translatePoint(l, sel.index(), ix, iz));
                else MapEditorSession.edit(l -> LayoutEdits.translateMain(l, sel.index(), ix, iz));
            }
            case SPAWN -> {
                Team t = Team.values()[sel.index()];
                MapLayout.TeamSpawn s = MapEditorSession.draft().spawn(t);
                int y = s != null && s.y() != null ? s.y() : playerPos().y();
                MapEditorSession.edit(l -> LayoutEdits.setSpawn(l, t, new Pos(wx, y, wz)));
            }
            case SLOT -> {
                int y = MapEditorSession.draft()
                        .vehicleSlots()
                        .get(sel.index())
                        .pos()
                        .y();
                MapEditorSession.edit(l -> LayoutEdits.moveSlot(l, sel.index(), new Pos(wx, y, wz)));
            }
            case BORDER -> {
                MapLayout.Border b = MapEditorSession.draft().border();
                if (b == null || b.sizeBlocks() == null) return;
                MapEditorSession.edit(l -> LayoutEdits.setBorder(l, wx, wz, b.sizeBlocks()));
            }
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        if (drag == Drag.REORDER) {
            int to = reorderTarget(my);
            if (to >= 0 && to != reorderFrom) {
                int from = reorderFrom;
                MapEditorSession.edit(l -> LayoutEdits.movePoint(l, from, to));
                MapEditorSession.select(Ref.point(to));
            }
        }
        if (drag == Drag.MOVE) rebuildWidgets(); // свежие значения в полях свойств
        drag = Drag.NONE;
        reorderFrom = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (layer.mouseScrolled(mx, my, delta)) return true;
        if (mapArea.contains(mx, my)) {
            MapView v = view();
            double wx = v.worldX(mx), wz = v.worldZ(my);
            bpp = Math.max(0.125, Math.min(32, bpp * (delta > 0 ? 0.8 : 1.25)));
            // зум вокруг курсора
            MapView n = view();
            viewX += wx - n.worldX(mx);
            viewZ += wz - n.worldZ(my);
            return true;
        }
        if (listView.contains(mx, my)) {
            listScroll = clampScroll(listScroll - delta * WTheme.SCROLL_STEP, listContent, listView.h());
            return true;
        }
        if (sideView.contains(mx, my)) {
            sideScroll = clampScroll(sideScroll - delta * WTheme.SCROLL_STEP, sideContent, sideView.h());
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private static double clampScroll(double s, int content, int view) {
        return Math.max(0, Math.min(Math.max(0, content - view), s));
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // Esc — ДО делегирования в поле (EditBox на Esc возвращает false и иначе держит фокус)
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (layer.keyPressed(key, scan, mods)) return true;
        boolean typing = fields.stream().anyMatch(EditBox::isFocused) || (comment != null && comment.isFocused());
        if (!typing && key == GLFW.GLFW_KEY_DELETE) {
            MapEditorSession.deleteSelected();
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    /** Пришли данные сервера (флаги кнопок, черновик) — пересобрать форму. */
    void onServerData() {
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ------------------------------------------------------------------ подписи

    /** Подпись объекта (шапка свойств, HUD режима мира). */
    public static Component describe(MapLayout l, Ref r) {
        if (r == null || !r.valid(l)) return Component.translatable("pwpwarfare.editor.nothing");
        return switch (r.kind()) {
            case MAIN -> Component.translatable(
                    "pwpwarfare.editor.main",
                    String.valueOf(l.mainZones().get(r.index()).team()));
            case POINT -> Component.translatable(
                    "pwpwarfare.editor.point",
                    r.index() + 1,
                    l.points().get(r.index()).name());
            case SPAWN -> Component.translatable("pwpwarfare.editor.spawn", Team.values()[r.index()].name());
            case SLOT -> Component.translatable(
                    "pwpwarfare.editor.slot",
                    l.vehicleSlots().get(r.index()).team().name(),
                    r.index() + 1);
            case BORDER -> Component.translatable("pwpwarfare.editor.border");
        };
    }

    /** Подсказка «что делают ЛКМ/ПКМ в мире» для выбранного объекта. */
    public static String worldHintKey(Ref r) {
        if (r == null) return "pwpwarfare.editor.hint.none";
        return switch (r.kind()) {
            case MAIN, POINT -> "pwpwarfare.editor.hint.corners";
            case SPAWN -> "pwpwarfare.editor.hint.spawn";
            case SLOT -> "pwpwarfare.editor.hint.slot";
            case BORDER -> "pwpwarfare.editor.hint.border";
        };
    }
}

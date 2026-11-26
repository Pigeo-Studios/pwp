package com.pigeostudios.pwp.warfare.client.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.admin.DivisionAdmin;
import com.pigeostudios.pwp.warfare.admin.MatchAdminPackets;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pwp.coreclient.admin.AdminForm;
import com.pwp.coreclient.admin.AdminTab;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Вкладка «Дивизии» (ADMIN+, этап 6d): состав дивизий фракций текущего матча без SQL. Команда → дивизия
 * (или «новая») → по строке на каждую машину пула фракции со счётчиком 0..5 → «Сохранить» (в core, сразу
 * перечитывается) / «Удалить» / «Поставить в матч» (выбор дивизии команды, как в «Матч»). Состав — это сколько
 * каких машин выставит дивизия в слоты мейна; слоты без машины в составе стоят пустыми.
 */
@OnlyIn(Dist.CLIENT)
public class DivisionAdminTab implements AdminTab {
    private static final Logger LOG = LoggerFactory.getLogger(DivisionAdminTab.class);
    private static final int POLL_TICKS = 200;
    private static final String[] TEAMS = {"BLUE", "RED"};

    private record PoolVehicle(String name, String display, String role) {}

    private record Div(String id, String name, Map<String, Integer> counts) {}

    private JsonObject raw;
    private int team;
    private int division; // индекс в divisions(); == size — «новая»
    /** Правка текущей дивизии: машина → количество. */
    private final Map<String, Integer> edit = new LinkedHashMap<>();

    private String editKey = "";
    private int ticks;
    private Host host;
    private String shownKey = "";

    public DivisionAdminTab(JsonObject preset) {
        this.raw = preset;
    }

    public static JsonObject fixture(boolean empty) {
        JsonObject o = new JsonObject();
        for (String t : TEAMS) {
            JsonObject team = new JsonObject();
            team.addProperty("faction", t.equals("BLUE") ? "usa" : "russia");
            JsonArray pool = new JsonArray();
            if (!empty) {
                String[][] rows = {
                    {"supply", "Грузовик снабжения", "SUPPLY"},
                    {"mrap", "MRAP", "MRAP"},
                    {"apc", "БТР", "APC"},
                    {"tank", "Танк", "TANK"},
                    {"heli_transport", "Транспортный вертолёт", "HELI_TRANSPORT"}
                };
                for (String[] r : rows) {
                    JsonObject v = new JsonObject();
                    v.addProperty("name", r[0]);
                    v.addProperty("display", r[1]);
                    v.addProperty("role", r[2]);
                    pool.add(v);
                }
            }
            team.add("pool", pool);
            JsonArray divs = new JsonArray();
            if (!empty) {
                JsonObject d = new JsonObject();
                d.addProperty("divisionId", "mechanized");
                d.addProperty("displayName", "Механизированная");
                JsonArray veh = new JsonArray();
                for (String[] e :
                        new String[][] {{"SUPPLY", "supply", "2"}, {"APC", "apc", "2"}, {"TANK", "tank", "1"}}) {
                    JsonObject x = new JsonObject();
                    x.addProperty("role", e[0]);
                    x.addProperty("vehicleName", e[1]);
                    x.addProperty("count", Integer.parseInt(e[2]));
                    veh.add(x);
                }
                d.add("vehicles", veh);
                divs.add(d);
            }
            team.add("divisions", divs);
            o.add(t, team);
        }
        return o;
    }

    @Override
    public void onShown() {
        ticks = POLL_TICKS;
    }

    @Override
    public void tick() {
        if (host == null || host.preview()) return;
        if (++ticks >= POLL_TICKS) {
            ticks = 0;
            host.backend().request(DivisionAdmin.DATA_TYPE, "");
        }
    }

    @Override
    public void onData(String type, String json) {
        if (!DivisionAdmin.DATA_TYPE.equals(type)) return;
        try {
            raw = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            LOG.warn("DivisionAdminTab: битый ответ", e);
            return;
        }
        // набор строк зависит от пула и списка дивизий — пересобрать, если поменялись
        String key = raw.toString();
        if (!key.equals(shownKey) && host != null) {
            editKey = ""; // правка сбрасывается на сохранённое
            host.relayout();
        }
    }

    private JsonObject teamObj() {
        if (raw == null || !raw.has(TEAMS[team]) || !raw.get(TEAMS[team]).isJsonObject()) return new JsonObject();
        return raw.getAsJsonObject(TEAMS[team]);
    }

    private List<PoolVehicle> pool() {
        List<PoolVehicle> out = new ArrayList<>();
        JsonObject t = teamObj();
        if (t.has("pool") && t.get("pool").isJsonArray()) {
            for (JsonElement e : t.getAsJsonArray("pool")) {
                JsonObject v = e.getAsJsonObject();
                out.add(new PoolVehicle(str(v, "name"), str(v, "display"), str(v, "role")));
            }
        }
        return out;
    }

    private List<Div> divisions() {
        List<Div> out = new ArrayList<>();
        JsonObject t = teamObj();
        if (t.has("divisions") && t.get("divisions").isJsonArray()) {
            for (JsonElement e : t.getAsJsonArray("divisions")) {
                JsonObject d = e.getAsJsonObject();
                Map<String, Integer> counts = new LinkedHashMap<>();
                if (d.has("vehicles") && d.get("vehicles").isJsonArray()) {
                    for (JsonElement ve : d.getAsJsonArray("vehicles")) {
                        JsonObject v = ve.getAsJsonObject();
                        counts.merge(
                                str(v, "vehicleName"),
                                v.has("count") ? v.get("count").getAsInt() : 1,
                                Integer::sum);
                    }
                }
                out.add(new Div(str(d, "divisionId"), str(d, "displayName"), counts));
            }
        }
        return out;
    }

    /** Текущая дивизия (null — «новая»). */
    private Div current() {
        List<Div> list = divisions();
        return division < list.size() ? list.get(division) : null;
    }

    /** Загрузить правку из выбранной дивизии, если сменился выбор. */
    private void syncEdit() {
        Div d = current();
        String key = TEAMS[team] + "|" + (d == null ? "#new" : d.id());
        if (key.equals(editKey)) return;
        editKey = key;
        edit.clear();
        for (PoolVehicle v : pool())
            edit.put(v.name(), d == null ? 0 : d.counts().getOrDefault(v.name(), 0));
    }

    @Override
    public void init(Host host, Rect area) {
        this.host = host;
        shownKey = raw == null ? "" : raw.toString();
        int divCount = divisions().size();
        division = Math.min(division, divCount);
        syncEdit();
        AdminForm f = new AdminForm()
                .section(Component.translatable("pwpwarfare.admin.division.title"))
                .control(Component.translatable("pwpwarfare.admin.division.team"), new UIChoice(this::teamText, d -> {
                    team = Math.floorMod(team + d, TEAMS.length);
                    division = 0;
                    host.relayout();
                }))
                .control(
                        Component.translatable("pwpwarfare.admin.division.division"),
                        new UIChoice(this::divisionText, d -> {
                            division = Math.floorMod(division + d, divisions().size() + 1);
                            host.relayout();
                        }));
        List<PoolVehicle> pool = pool();
        if (raw == null) {
            f.text(() -> Component.translatable("pwp_core.ui.loading"), () -> PWPTheme.Text.MUTED);
        } else if (pool.isEmpty()) {
            f.text(() -> Component.translatable("pwpwarfare.admin.division.no_pool"), () -> PWPTheme.Text.MUTED);
        } else {
            f.gap().section(Component.translatable("pwpwarfare.admin.division.composition"));
            for (PoolVehicle v : pool) {
                f.control(
                        Component.literal(v.role() + " · " + v.display()),
                        new UIChoice(
                                () -> Component.literal(String.valueOf(edit.getOrDefault(v.name(), 0))),
                                d -> edit.put(
                                        v.name(),
                                        Math.floorMod(
                                                edit.getOrDefault(v.name(), 0) + d,
                                                DivisionAdmin.MAX_PER_VEHICLE + 1))));
            }
            f.text(this::totalText, () -> PWPTheme.Text.MUTED)
                    .gap()
                    .buttons(
                            new UIButton(Component.translatable("pwpwarfare.admin.division.save"), this::save)
                                    .style(UIButton.Style.ACCENT)
                                    .icon(UIIcon.CHECK),
                            new UIButton(Component.translatable("pwpwarfare.admin.division.apply"), this::applyToMatch)
                                    .style(UIButton.Style.DARK)
                                    .icon(UIIcon.PLAY))
                    .buttons(new UIButton(Component.translatable("pwpwarfare.admin.division.delete"), this::delete)
                            .style(UIButton.Style.DANGER)
                            .icon(UIIcon.CLOSE));
        }
        f.text(() -> Component.translatable("pwpwarfare.admin.division.hint"), () -> PWPTheme.Text.MUTED);
        host.add(f.build(area));
    }

    private Component teamText() {
        String faction = str(teamObj(), "faction");
        return Component.literal(TEAMS[team] + (faction.isEmpty() ? "" : " · " + faction.toUpperCase()));
    }

    private Component divisionText() {
        Div d = current();
        if (d == null) return Component.translatable("pwpwarfare.admin.division.new");
        return Component.literal(d.name().isEmpty() ? d.id() : d.name() + " (" + d.id() + ")");
    }

    private Component totalText() {
        int total = edit.values().stream().mapToInt(Integer::intValue).sum();
        return Component.translatable("pwpwarfare.admin.division.total", total);
    }

    private String spec() {
        StringBuilder b = new StringBuilder();
        edit.forEach((name, n) -> {
            if (n > 0) b.append(name).append(':').append(n).append(';');
        });
        return b.toString();
    }

    private void save() {
        Div d = current();
        String id;
        String name;
        if (d != null) {
            id = d.id();
            name = d.name().isEmpty() ? d.id() : d.name();
        } else {
            int n = divisions().size() + 1;
            id = "custom_" + n;
            name = Component.translatable("pwpwarfare.admin.division.custom_name", n)
                    .getString();
        }
        send(DivisionAdmin.SAVE, new DivisionAdmin.Save(TEAMS[team], id, name, spec()));
    }

    private void delete() {
        Div d = current();
        if (d == null) return;
        host.showModal(new UIModal(
                        Component.translatable("pwpwarfare.admin.division.delete"),
                        Component.translatable("pwpwarfare.admin.division.delete_confirm", d.name(), TEAMS[team]))
                .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                .button(
                        Component.translatable("pwpwarfare.admin.division.delete"),
                        UIButton.Style.DANGER,
                        () -> send(DivisionAdmin.DELETE, new DivisionAdmin.Delete(TEAMS[team], d.id()))));
    }

    private void applyToMatch() {
        Div d = current();
        if (d == null) return;
        send(MatchAdminPackets.SET_DIVISION, new MatchAdminPackets.SetDivision(TEAMS[team], d.id()));
    }

    private void send(String action, Object packet) {
        host.backend().perform(action, () -> PacketHandler.INSTANCE.sendToServer(packet));
        ticks = POLL_TICKS - 40; // свежий список через 2 с (core + перечитывание фракций)
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}

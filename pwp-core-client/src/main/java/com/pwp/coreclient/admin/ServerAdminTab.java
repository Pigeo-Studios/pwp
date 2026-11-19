package com.pwp.coreclient.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ServerAdmin;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.layout.Rect;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Вкладка «Сервер» (MODERATOR+): этот сервер (TPS/MSPT/память/игроки/аптайм), все серверы сети из
 * heartbeat core, объявление всем/команде, последние WARN/ERROR (очищены от секретов на сервере) и
 * аудит-лог админ-действий (ADMIN+). Данные — тип {@code adminServer}, опрос раз в 2 с.
 */
@OnlyIn(Dist.CLIENT)
public class ServerAdminTab implements AdminTab {
    private static final Logger LOG = LoggerFactory.getLogger(ServerAdminTab.class);
    private static final int POLL_TICKS = 40;
    private static final int SERVER_ROWS = 6;
    private static final int ERROR_ROWS = 10;
    private static final int AUDIT_ROWS = 10;
    private static final String[] TARGETS = {"ALL", "BLUE", "RED"};
    /** MSPT выше — строка красным (тик дольше 50 мс = сервер не успевает). */
    private static final double MSPT_BAD = 50.0;

    private final boolean admin;
    private JsonObject data;
    private int template;
    private int target;
    private int ticks;
    private Host host;

    public ServerAdminTab(boolean admin, JsonObject preset) {
        this.admin = admin;
        this.data = preset;
    }

    /** Фикстура тест-стенда (/pwpui open core.admin_panel). */
    public static JsonObject fixture(boolean empty) {
        JsonObject o = new JsonObject();
        JsonObject local = new JsonObject();
        local.addProperty("mspt", empty ? 0 : 23.4);
        local.addProperty("tps", 20.0);
        local.addProperty("heapUsedMb", 3120);
        local.addProperty("heapMaxMb", 8192);
        local.addProperty("online", empty ? 0 : 37);
        local.addProperty("max", 100);
        local.addProperty("uptimeSec", 5400);
        local.addProperty("port", 25566);
        o.add("local", local);
        JsonArray servers = new JsonArray();
        JsonArray errors = new JsonArray();
        JsonArray audit = new JsonArray();
        if (!empty) {
            String[][] rows = {{"lobby", "12", "4.1", "20.0"}, {"match_01", "37", "61.8", "16.2"}};
            for (String[] r : rows) {
                JsonObject s = new JsonObject();
                s.addProperty("name", r[0]);
                s.addProperty("online", Integer.parseInt(r[1]));
                s.addProperty("mspt", Double.parseDouble(r[2]));
                s.addProperty("tps", Double.parseDouble(r[3]));
                s.addProperty("heapUsedMb", 2048);
                s.addProperty("heapMaxMb", 6144);
                s.addProperty("ageSec", 4);
                servers.add(s);
            }
            JsonObject e = new JsonObject();
            e.addProperty("at", System.currentTimeMillis());
            e.addProperty("level", "WARN");
            e.addProperty("logger", "MinecraftServer");
            e.addProperty("text", "Can't keep up! Is the server overloaded? Running 2044ms behind");
            e.addProperty("repeats", 3);
            errors.add(e);
            audit.add("2026-11-19 12:00:00 | ADMIN Max | match.restart | Restart[] | OK");
        }
        o.add("servers", servers);
        o.addProperty("coreOk", !empty);
        o.add("errors", errors);
        o.add("audit", audit);
        JsonArray t = new JsonArray();
        ServerAdmin.TEMPLATES.forEach(t::add);
        o.add("templates", t);
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
            host.backend().request(ServerAdmin.TYPE, "");
        }
    }

    @Override
    public void onData(String type, String json) {
        if (!ServerAdmin.TYPE.equals(type)) return;
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (!o.has("error")) data = o;
        } catch (JsonParseException | IllegalStateException e) {
            LOG.warn("ServerAdminTab: битый ответ", e);
        }
    }

    @Override
    public void init(Host host, Rect area) {
        this.host = host;
        AdminForm f = new AdminForm()
                .section(Component.translatable("pwp_core.admin.server.this"))
                .value(Component.translatable("pwp_core.admin.server.perf"), this::perfText)
                .value(Component.translatable("pwp_core.admin.server.memory"), this::memoryText)
                .value(Component.translatable("pwp_core.admin.server.players"), this::playersText)
                .gap()
                .section(Component.translatable("pwp_core.admin.server.network"));
        for (int i = 0; i < SERVER_ROWS; i++) {
            final int idx = i;
            f.text(() -> serverLine(idx), () -> serverColor(idx));
        }
        f.gap()
                .section(Component.translatable("pwp_core.admin.server.announce"))
                .control(
                        Component.translatable("pwp_core.admin.server.announce.text"),
                        new UIChoice(this::templateText, d -> template = Math.floorMod(template + d, templates())))
                .control(
                        Component.translatable("pwp_core.admin.server.announce.target"),
                        new UIChoice(
                                () -> Component.translatable(
                                        "pwp_core.admin.server.target." + TARGETS[target].toLowerCase()),
                                d -> target = Math.floorMod(target + d, TARGETS.length)),
                        new UIButton(Component.translatable("pwp_core.admin.server.announce.send"), this::announce)
                                .style(UIButton.Style.ACCENT)
                                .icon(UIIcon.PLAY))
                .gap()
                .section(Component.translatable("pwp_core.admin.server.errors"));
        for (int i = 0; i < ERROR_ROWS; i++) {
            final int idx = i;
            f.text(() -> errorLine(idx), () -> errorColor(idx));
        }
        if (admin) {
            f.gap().section(Component.translatable("pwp_core.admin.server.audit"));
            for (int i = 0; i < AUDIT_ROWS; i++) {
                final int idx = i;
                f.text(() -> auditLine(idx), () -> PWPTheme.Text.MUTED);
            }
        }
        host.add(f.build(area));
    }

    private void announce() {
        String id = ServerAdmin.TEMPLATES.get(Math.min(template, ServerAdmin.TEMPLATES.size() - 1));
        String team = TARGETS[target];
        host.showModal(new UIModal(
                        Component.translatable("pwp_core.admin.server.announce"),
                        Component.translatable(
                                "pwp_core.admin.server.announce.confirm",
                                Component.translatable("pwp_core.admin.announce." + id),
                                Component.translatable("pwp_core.admin.server.target." + team.toLowerCase())))
                .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                .button(
                        Component.translatable("pwp_core.admin.server.announce.send"),
                        UIButton.Style.ACCENT,
                        () -> host.backend()
                                .perform(
                                        ServerAdmin.ANNOUNCE,
                                        () -> PacketHandler.INSTANCE.sendToServer(
                                                new ServerAdmin.Announce(id, team)))));
    }

    // ---------- строки ----------

    private JsonObject local() {
        return data != null && data.has("local") ? data.getAsJsonObject("local") : null;
    }

    private static double d(JsonObject o, String k) {
        return o != null && o.has(k) ? o.get(k).getAsDouble() : 0;
    }

    private Component perfText() {
        JsonObject l = local();
        if (l == null) return Component.literal("—");
        return Component.translatable("pwp_core.admin.server.perf.value", d(l, "tps"), d(l, "mspt"));
    }

    private Component memoryText() {
        JsonObject l = local();
        if (l == null) return Component.literal("—");
        return Component.translatable(
                "pwp_core.admin.server.memory.value", (long) d(l, "heapUsedMb"), (long) d(l, "heapMaxMb"));
    }

    private Component playersText() {
        JsonObject l = local();
        if (l == null) return Component.literal("—");
        long up = (long) d(l, "uptimeSec");
        return Component.translatable(
                "pwp_core.admin.server.players.value",
                (long) d(l, "online"),
                (long) d(l, "max"),
                up / 3600,
                (up / 60) % 60);
    }

    private JsonArray arr(String k) {
        return data != null && data.has(k) && data.get(k).isJsonArray() ? data.getAsJsonArray(k) : new JsonArray();
    }

    private Component serverLine(int i) {
        JsonArray s = arr("servers");
        if (i >= s.size()) {
            if (i != 0) return Component.empty();
            boolean coreOk =
                    data != null && data.has("coreOk") && data.get("coreOk").getAsBoolean();
            return Component.translatable(
                    coreOk ? "pwp_core.admin.server.no_servers" : "pwp_core.admin.server.no_core");
        }
        JsonObject o = s.get(i).getAsJsonObject();
        return Component.translatable(
                "pwp_core.admin.server.line",
                o.has("name") ? o.get("name").getAsString() : "?",
                (long) d(o, "online"),
                d(o, "tps"),
                d(o, "mspt"),
                (long) d(o, "heapUsedMb"),
                (long) d(o, "heapMaxMb"),
                (long) d(o, "ageSec"));
    }

    private int serverColor(int i) {
        JsonArray s = arr("servers");
        if (i >= s.size()) return PWPTheme.Text.MUTED;
        return d(s.get(i).getAsJsonObject(), "mspt") > MSPT_BAD ? PWPTheme.Text.DANGER : PWPTheme.Text.BODY;
    }

    private Component errorLine(int i) {
        JsonArray e = arr("errors");
        if (i >= e.size())
            return i == 0 ? Component.translatable("pwp_core.admin.server.no_errors") : Component.empty();
        JsonObject o = e.get(i).getAsJsonObject();
        String time =
                new SimpleDateFormat("HH:mm:ss").format(new Date(o.get("at").getAsLong()));
        int rep = o.has("repeats") ? o.get("repeats").getAsInt() : 1;
        String text = o.get("level").getAsString() + " " + time + " ["
                + o.get("logger").getAsString() + "] " + o.get("text").getAsString() + (rep > 1 ? " ×" + rep : "");
        return Component.literal(text);
    }

    private int errorColor(int i) {
        JsonArray e = arr("errors");
        if (i >= e.size()) return PWPTheme.Text.MUTED;
        return "ERROR".equals(e.get(i).getAsJsonObject().get("level").getAsString())
                ? PWPTheme.Text.DANGER
                : PWPTheme.Text.ACCENT;
    }

    private Component auditLine(int i) {
        JsonArray a = arr("audit");
        if (i >= a.size()) return i == 0 ? Component.translatable("pwp_core.admin.server.no_audit") : Component.empty();
        return Component.literal(a.get(i).getAsString());
    }

    private int templates() {
        return ServerAdmin.TEMPLATES.size();
    }

    private Component templateText() {
        return Component.translatable("pwp_core.admin.announce." + ServerAdmin.TEMPLATES.get(template));
    }
}

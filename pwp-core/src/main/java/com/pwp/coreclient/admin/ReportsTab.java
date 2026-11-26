package com.pwp.coreclient.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ModerationAction;
import com.pwp.coreclient.network.data.ReportData;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Вкладка «Жалобы» (MODERATOR+, этап 10a): открытые жалобы всех серверов из core (core недоступен — жалобы
 * этого сервера). Выбор жалобы стрелками; «К нарушителю» — телепорт (если он на этом сервере), «Наблюдать»,
 * «Закрыть» — жалоба обработана. Наказание — во вкладке «Игроки». Опрос раз в 5 с.
 */
@OnlyIn(Dist.CLIENT)
public class ReportsTab implements AdminTab {
    private static final Logger LOG = LoggerFactory.getLogger(ReportsTab.class);
    private static final int POLL_TICKS = 100;

    private record Report(
            long id, String server, String reporter, String target, String reason, String details, long createdAt) {}

    private final List<Report> reports = new ArrayList<>();
    private String source = "";
    private int selected;
    private int ticks;
    private AdminBackend backend;

    public ReportsTab() {}

    public ReportsTab(JsonObject preset) {
        accept(preset);
    }

    public static JsonObject fixture(boolean empty) {
        JsonObject o = new JsonObject();
        o.addProperty("source", "core");
        JsonArray arr = new JsonArray();
        if (!empty) {
            String[][] rows = {
                {"12", "match_01", "Steve", "Griefer_1", "teamkill", "убил 3 раза на мейне"},
                {"11", "lobby", "Alex", "ToxicGuy", "toxic", ""},
                {"10", "match_02", "PWP", "TKer_With_A_Very_Long_Name", "teamkill", "5 тимкиллов за матч"}
            };
            for (String[] r : rows) {
                JsonObject e = new JsonObject();
                e.addProperty("id", Long.parseLong(r[0]));
                e.addProperty("server", r[1]);
                e.addProperty("reporter", r[2]);
                e.addProperty("target", r[3]);
                e.addProperty("reason", r[4]);
                e.addProperty("details", r[5]);
                e.addProperty("createdAt", System.currentTimeMillis() - 600_000L);
                arr.add(e);
            }
        }
        o.add("reports", arr);
        return o;
    }

    @Override
    public void onShown() {
        ticks = POLL_TICKS;
    }

    @Override
    public void tick() {
        if (backend == null || backend.preview()) return;
        if (++ticks >= POLL_TICKS) {
            ticks = 0;
            backend.request(ReportData.REPORTS_TYPE, "");
        }
    }

    @Override
    public void onData(String type, String json) {
        if (!ReportData.REPORTS_TYPE.equals(type)) return;
        try {
            accept(JsonParser.parseString(json).getAsJsonObject());
        } catch (JsonParseException | IllegalStateException e) {
            LOG.warn("ReportsTab: битый ответ", e);
        }
    }

    private void accept(JsonObject o) {
        long keep = current() == null ? -1 : current().id();
        reports.clear();
        source = str(o, "source");
        if (o.has("reports") && o.get("reports").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("reports")) {
                JsonObject r = e.getAsJsonObject();
                reports.add(new Report(
                        r.has("id") ? r.get("id").getAsLong() : 0,
                        str(r, "server"),
                        str(r, "reporter"),
                        str(r, "target"),
                        str(r, "reason"),
                        str(r, "details"),
                        r.has("createdAt") ? r.get("createdAt").getAsLong() : 0));
            }
        }
        selected = 0;
        for (int i = 0; i < reports.size(); i++) if (reports.get(i).id() == keep) selected = i;
    }

    private Report current() {
        return selected < reports.size() ? reports.get(selected) : null;
    }

    @Override
    public void init(Host host, Rect area) {
        backend = host.backend();
        AdminForm f = new AdminForm()
                .section(Component.translatable("pwp_core.admin.reports.title"))
                .control(Component.translatable("pwp_core.admin.reports.report"), new UIChoice(this::headline, d -> {
                    if (!reports.isEmpty()) selected = Math.floorMod(selected + d, reports.size());
                }))
                .text(this::detailsLine, () -> PWPTheme.Text.BODY)
                .text(this::sourceLine, () -> PWPTheme.Text.MUTED)
                .gap()
                .buttons(
                        action(
                                host,
                                "goto",
                                UIIcon.PLAY,
                                r -> PacketHandler.INSTANCE.sendToServer(new ModerationAction.Teleport(r.target()))),
                        action(
                                host,
                                "spectate",
                                UIIcon.INFO,
                                r -> PacketHandler.INSTANCE.sendToServer(new ModerationAction.Spectate(r.target()))))
                .buttons(
                        action(host, "close", UIIcon.CHECK, r -> {
                            if (r.id() > 0) PacketHandler.INSTANCE.sendToServer(new ReportData.Close(r.id()));
                        }),
                        new UIButton(Component.translatable("pwp_core.admin.reports.refresh"), () -> ticks = POLL_TICKS)
                                .style(UIButton.Style.DARK)
                                .icon(UIIcon.REPLAY))
                .text(() -> Component.translatable("pwp_core.admin.reports.hint"), () -> PWPTheme.Text.MUTED);
        host.add(f.build(area));
    }

    private UIButton action(Host host, String id, UIIcon icon, java.util.function.Consumer<Report> send) {
        return new UIButton(Component.translatable("pwp_core.admin.reports." + id), () -> {
                    Report r = current();
                    if (r != null) host.backend().perform("report." + id, () -> send.accept(r));
                })
                .style(UIButton.Style.DARK)
                .icon(icon);
    }

    private Component headline() {
        Report r = current();
        if (r == null) return Component.translatable("pwp_core.admin.reports.none");
        return Component.translatable(
                "pwp_core.admin.reports.headline",
                selected + 1,
                reports.size(),
                r.target(),
                Component.translatable("pwp_core.report.reason." + r.reason()));
    }

    private Component detailsLine() {
        Report r = current();
        if (r == null) return Component.empty();
        long min = r.createdAt() <= 0 ? 0 : Math.max(0, (System.currentTimeMillis() - r.createdAt()) / 60_000L);
        return Component.translatable(
                "pwp_core.admin.reports.details",
                r.reporter(),
                r.server(),
                min,
                r.details().isEmpty() ? "—" : r.details());
    }

    private Component sourceLine() {
        return Component.translatable(
                "local".equals(source) ? "pwp_core.admin.reports.source_local" : "pwp_core.admin.reports.source_core");
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}

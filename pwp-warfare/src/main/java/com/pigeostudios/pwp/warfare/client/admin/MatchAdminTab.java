package com.pigeostudios.pwp.warfare.client.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.admin.MatchAdmin;
import com.pigeostudios.pwp.warfare.admin.MatchAdminPackets;
import com.pigeostudios.pwp.warfare.match.AdminInput;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pwp.coreclient.admin.AdminForm;
import com.pwp.coreclient.admin.AdminTab;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.data.DataRequests;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Вкладка «Матч» (ADMIN+): фаза и таймер, старт/стоп/пауза/«пропустить ожидание», режим и защитник,
 * фракции, тикеты, длительность ожидания. Изменения — выбор значения + «Применить» (одно действие =
 * один пакет). Старт, заблокированный ошибками раскладки, — модал со списком и «Стартовать всё равно».
 */
@OnlyIn(Dist.CLIENT)
public class MatchAdminTab implements AdminTab {
    private static final int POLL_TICKS = 20;
    private static final int MODAL_ERRORS = 8;

    private final boolean owner;
    private AdminMatchData data;
    /** Выбранные, но ещё не применённые значения (ключ — поле adminMatch). */
    private final Map<String, Object> pending = new HashMap<>();

    private int ticks;
    private Host host;

    /** @param preset фикстура тест-стенда или null */
    public MatchAdminTab(boolean owner, AdminMatchData preset) {
        this.owner = owner;
        this.data = preset;
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
            host.backend().request(AdminMatchData.TYPE, "");
        }
    }

    @Override
    public void onData(String type, String json) {
        if (AdminMatchData.TYPE.equals(type)) {
            AdminMatchData d = AdminMatchData.parse(json);
            if (d != null) data = d;
        } else if (DataRequests.ACTION_RESULT.equals(type)) {
            onResult(json);
        }
    }

    private void onResult(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String action = o.get("action").getAsString();
            String error = o.has("error") ? o.get("error").getAsString() : "";
            boolean start = MatchAdminPackets.START.equals(action) || MatchAdminPackets.SKIP_WAITING.equals(action);
            if (start && MatchAdmin.ERR_LAYOUT_BLOCKED.equals(error))
                showBlocked(MatchAdminPackets.SKIP_WAITING.equals(action));
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            // чужой/битый ответ — тост уже показал экран
        }
    }

    /** Модал: ошибки раскладки + «Стартовать всё равно» (force). */
    private void showBlocked(boolean skip) {
        if (host == null) return;
        List<AdminMatchData.IssueRow> errors = data == null ? List.of() : data.errors();
        MutableComponent msg = Component.translatable("pwpwarfare.admin.match.blocked_modal", errors.size());
        for (int i = 0; i < Math.min(MODAL_ERRORS, errors.size()); i++) {
            msg = msg.append("\n• ").append(errors.get(i).text());
        }
        if (errors.size() > MODAL_ERRORS) {
            msg = msg.append("\n")
                    .append(Component.translatable("pwpwarfare.admin.map.more", errors.size() - MODAL_ERRORS));
        }
        host.showModal(new UIModal(Component.translatable("pwpwarfare.admin.match.blocked_title"), msg)
                .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                .button(Component.translatable("pwpwarfare.admin.match.force_start"), UIButton.Style.DANGER, () -> {
                    if (skip) send(MatchAdminPackets.SKIP_WAITING, new MatchAdminPackets.SkipWaiting(true));
                    else send(MatchAdminPackets.START, new MatchAdminPackets.Start(true));
                }));
    }

    private void send(String id, Object packet) {
        host.backend().perform(id, () -> PacketHandler.INSTANCE.sendToServer(packet));
    }

    @Override
    public void init(Host host, Rect area) {
        this.host = host;
        UIButton stop = action(
                "stop",
                UIButton.Style.DANGER,
                UIIcon.CLOSE,
                () -> host.showModal(new UIModal(
                                Component.translatable("pwpwarfare.admin.match.stop"),
                                Component.translatable("pwpwarfare.admin.match.stop_confirm"))
                        .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                        .button(
                                Component.translatable("pwpwarfare.admin.match.stop"),
                                UIButton.Style.DANGER,
                                () -> send(MatchAdminPackets.STOP, new MatchAdminPackets.Stop()))));
        UIButton saveWaiting = new UIButton(
                        Component.translatable("pwpwarfare.admin.match.waiting_save"),
                        () -> send(
                                MatchAdminPackets.SET_WAITING,
                                new MatchAdminPackets.SetWaitingSeconds(waiting(), true)))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.CHECK);
        saveWaiting.setEnabled(owner);
        saveWaiting.tooltip(UITooltip.Content.of(Component.translatable(
                owner ? "pwpwarfare.admin.match.waiting_save.tip" : "pwp_core.admin.owner_only")));

        AdminForm f = new AdminForm()
                .section(Component.translatable("pwpwarfare.admin.match.status"))
                .value(Component.translatable("pwpwarfare.admin.match.phase"), this::phaseText)
                .value(
                        Component.translatable("pwpwarfare.admin.match.tickets"),
                        () -> Component.translatable(
                                "pwpwarfare.admin.match.tickets.value", num("blueTickets"), num("redTickets")))
                .value(Component.translatable("pwpwarfare.admin.match.layout"), this::layoutText)
                .buttons(
                        action(
                                "start",
                                UIButton.Style.ACCENT,
                                UIIcon.PLAY,
                                () -> send(MatchAdminPackets.START, new MatchAdminPackets.Start(false))),
                        action(
                                "skip_waiting",
                                UIButton.Style.DARK,
                                UIIcon.REPLAY,
                                () -> send(MatchAdminPackets.SKIP_WAITING, new MatchAdminPackets.SkipWaiting(false))))
                .buttons(
                        action(
                                "pause",
                                UIButton.Style.DARK,
                                UIIcon.SETTINGS,
                                () -> send(MatchAdminPackets.PAUSE, new MatchAdminPackets.TogglePause())),
                        stop)
                .gap()
                .section(Component.translatable("pwpwarfare.admin.match.settings"))
                .control(
                        Component.translatable("pwpwarfare.admin.match.mode"),
                        cycle(
                                "mode",
                                List.of(AdminInput.MODE_AAS, AdminInput.MODE_INVASION),
                                "pwpwarfare.admin.match.mode."),
                        apply(() -> new MatchAdminPackets.SetMode(pick("mode")), MatchAdminPackets.SET_MODE, "mode"))
                .control(
                        Component.translatable("pwpwarfare.admin.match.defender"),
                        cycle("defender", List.of("BLUE", "RED"), "pwpwarfare.admin.match.team."),
                        apply(
                                () -> new MatchAdminPackets.SetDefender(pick("defender")),
                                MatchAdminPackets.SET_DEFENDER,
                                "defender"))
                .control(
                        Component.translatable("pwpwarfare.admin.match.faction_blue"),
                        cycle("blueFaction", null, null),
                        apply(
                                () -> new MatchAdminPackets.SetFaction("BLUE", pick("blueFaction")),
                                MatchAdminPackets.SET_FACTION,
                                "blueFaction"))
                .control(
                        Component.translatable("pwpwarfare.admin.match.faction_red"),
                        cycle("redFaction", null, null),
                        apply(
                                () -> new MatchAdminPackets.SetFaction("RED", pick("redFaction")),
                                MatchAdminPackets.SET_FACTION,
                                "redFaction"))
                .control(
                        Component.translatable("pwpwarfare.admin.match.tickets_blue"),
                        stepper("blueTickets", 50, 0, AdminInput.TICKETS_MAX),
                        apply(
                                () -> new MatchAdminPackets.SetTickets("BLUE", number("blueTickets")),
                                MatchAdminPackets.SET_TICKETS,
                                "blueTickets"))
                .control(
                        Component.translatable("pwpwarfare.admin.match.tickets_red"),
                        stepper("redTickets", 50, 0, AdminInput.TICKETS_MAX),
                        apply(
                                () -> new MatchAdminPackets.SetTickets("RED", number("redTickets")),
                                MatchAdminPackets.SET_TICKETS,
                                "redTickets"))
                .control(
                        Component.translatable("pwpwarfare.admin.match.waiting"),
                        stepper("waitingDefault", 30, AdminInput.WAITING_MIN, AdminInput.WAITING_MAX),
                        apply(
                                () -> new MatchAdminPackets.SetWaitingSeconds(waiting(), false),
                                MatchAdminPackets.SET_WAITING,
                                "waitingDefault"))
                .buttons(saveWaiting)
                .text(() -> Component.translatable("pwpwarfare.admin.match.hint"), () -> PWPTheme.Text.MUTED);
        host.add(f.build(area));
    }

    private UIButton action(String id, UIButton.Style style, UIIcon icon, Runnable run) {
        return new UIButton(Component.translatable("pwpwarfare.admin.match." + id), run)
                .style(style)
                .icon(icon);
    }

    private UIButton apply(Supplier<Object> packet, String id, String field) {
        return new UIButton(Component.translatable("pwp_core.admin.apply"), () -> {
                    send(id, packet.get());
                    pending.remove(field);
                })
                .style(UIButton.Style.DARK);
    }

    /** Выбор из списка (null — фракции из данных); подпись: ключ prefix+значение или само значение. */
    private UIChoice cycle(String field, List<String> fixed, String labelPrefix) {
        return new UIChoice(
                () -> {
                    String v = pick(field);
                    Component c = labelPrefix == null
                            ? Component.literal(v)
                            : Component.translatable(labelPrefix + v.toLowerCase());
                    return pending.containsKey(field) ? c.copy().append(" *") : c;
                },
                d -> {
                    List<String> opts =
                            fixed != null ? fixed : (data == null ? AdminInput.DEFAULT_FACTIONS : data.factions());
                    int i = Math.max(0, opts.indexOf(pick(field)));
                    pending.put(field, opts.get(Math.floorMod(i + d, opts.size())));
                });
    }

    /** Число с шагом (Shift — ×10). */
    private UIChoice stepper(String field, int step, int min, int max) {
        return new UIChoice(() -> Component.literal(number(field) + (pending.containsKey(field) ? " *" : "")), d -> {
            int s = step * (Screen.hasShiftDown() ? 10 : 1);
            pending.put(field, Math.max(min, Math.min(max, number(field) + d * s)));
        });
    }

    private String pick(String field) {
        Object v = pending.get(field);
        if (v instanceof String s) return s;
        return data == null ? "" : data.str(field, "");
    }

    private int number(String field) {
        Object v = pending.get(field);
        if (v instanceof Integer i) return i;
        return num(field);
    }

    /** Значение ожидания: выбранное, иначе текущий таймер (идёт ожидание) или дефолт из конфига. */
    private int waiting() {
        if (pending.get("waitingDefault") instanceof Integer i) return i;
        if (data != null && data.flag("waiting")) return data.num("timer");
        return num("waitingDefault");
    }

    private int num(String key) {
        return data == null ? 0 : data.num(key);
    }

    private Component phaseText() {
        if (data == null) return Component.translatable("pwp_core.admin.lobby.phase.unknown");
        Component p = Component.translatable(
                "pwpwarfare.admin.match.phase." + data.str("phase", "IDLE").toLowerCase());
        int t = data.num("timer");
        return t > 0 ? Component.translatable("pwp_core.admin.lobby.phase.timer", p, t) : p;
    }

    private Component layoutText() {
        if (data == null) return Component.literal("—");
        if (!data.flag("layoutLoaded")) return Component.translatable("pwpwarfare.admin.map.not_loaded");
        int errors = data.errors().size();
        int warns = data.issues().size() - errors;
        String key = errors == 0
                ? "pwpwarfare.admin.map.summary.ok"
                : data.flag("blockEnabled")
                        ? "pwpwarfare.admin.map.summary.blocked"
                        : "pwpwarfare.admin.map.summary.errors";
        return Component.translatable(key, errors, warns);
    }
}

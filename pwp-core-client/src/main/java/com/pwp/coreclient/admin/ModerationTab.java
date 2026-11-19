package com.pwp.coreclient.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.api.security.AccessLevel;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ModerationAction;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Вкладка «Игроки» (MODERATOR+): игрок онлайн (команда, отряд, кит, лидер, пинг) → предупреждение, кик,
 * мут голоса, бан на время ({@code /pwpban}), перевод в команду, снятие наказания за тимкилл, телепорт к
 * игроку, наблюдение. Моды добавляют свои кнопки ({@link #addAction}: warfare — сброс кита/кулдаунов,
 * лидер); кнопка видна, только если сервер объявил действие ({@code actions} в {@code adminPlayers}) и
 * уровень достаточен. Причины — готовые варианты (в ките нет поля ввода). Опасные — с подтверждением.
 * Права и ник проверяет сервер.
 */
@OnlyIn(Dist.CLIENT)
public class ModerationTab implements AdminTab {
    private static final Logger LOG = LoggerFactory.getLogger(ModerationTab.class);
    private static final int POLL_TICKS = 40;
    private static final String[] REASONS = {"rules", "grief", "teamkill", "toxic", "spam", "cheat"};
    private static final String[] DURATIONS = {"30m", "1h", "6h", "1d", "7d", "30d", "perm"};
    private static final int[] MUTE_MINUTES = {5, 15, 30, 60, 1440};

    /** Кнопка мода: {@code send} получает ник цели и шлёт свой пакет. */
    public record ExtraAction(
            String id, String labelKey, UIIcon icon, boolean confirm, AccessLevel min, Consumer<String> send) {}

    private static final List<ExtraAction> EXTRA = new CopyOnWriteArrayList<>();

    /** Регистрация кнопки мода (клиент, при старте). {@code id} совпадает с объявленным сервером. */
    public static void addAction(ExtraAction a) {
        EXTRA.removeIf(e -> e.id().equals(a.id()));
        EXTRA.add(a);
    }

    private record Entry(
            String name, String team, String level, String squad, String kit, boolean leader, int ping, boolean spec) {}

    private final List<Entry> players = new ArrayList<>();
    private final Set<String> actions = new HashSet<>();
    private int player;
    private int reason;
    private int duration = 3;
    private int mute = 1;
    private int ticks;
    private AdminBackend backend;
    private Host host;

    public static JsonObject fixture(boolean empty) {
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        if (!empty) {
            String[][] rows = {
                {"Steve", "Blue", "PLAYER", "Alpha", "Rifleman", "0", "42"},
                {"Alex_Sniper_With_A_Very_Long_Name", "Red", "PLAYER", "Bravo", "Squad Leader", "1", "180"},
                {"ModerMax", "", "MODERATOR", "", "", "0", "7"}
            };
            for (String[] r : rows) {
                JsonObject e = new JsonObject();
                e.addProperty("name", r[0]);
                e.addProperty("team", r[1]);
                e.addProperty("level", r[2]);
                e.addProperty("squad", r[3]);
                e.addProperty("kit", r[4]);
                e.addProperty("leader", r[5].equals("1"));
                e.addProperty("ping", Integer.parseInt(r[6]));
                arr.add(e);
            }
        }
        o.add("players", arr);
        JsonArray acts = new JsonArray();
        EXTRA.forEach(a -> acts.add(a.id()));
        o.add("actions", acts);
        return o;
    }

    public ModerationTab() {}

    public ModerationTab(JsonObject preset) {
        accept(preset);
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
            backend.request(ModerationAction.PLAYERS_TYPE, "");
        }
    }

    @Override
    public void onData(String type, String json) {
        if (!ModerationAction.PLAYERS_TYPE.equals(type)) return;
        try {
            Set<String> before = new HashSet<>(actions);
            accept(JsonParser.parseString(json).getAsJsonObject());
            // набор кнопок модов зависит от сервера — пересобрать, если он поменялся
            if (host != null && !before.equals(actions)) host.relayout();
        } catch (JsonParseException | IllegalStateException e) {
            LOG.warn("ModerationTab: битый ответ", e);
        }
    }

    private void accept(JsonObject o) {
        String selected = current() == null ? null : current().name();
        players.clear();
        if (o.has("players") && o.get("players").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("players")) {
                JsonObject p = e.getAsJsonObject();
                players.add(new Entry(
                        str(p, "name"),
                        str(p, "team"),
                        str(p, "level"),
                        str(p, "squad"),
                        str(p, "kit"),
                        p.has("leader") && p.get("leader").getAsBoolean(),
                        p.has("ping") ? p.get("ping").getAsInt() : -1,
                        p.has("spectating") && p.get("spectating").getAsBoolean()));
            }
        }
        actions.clear();
        if (o.has("actions") && o.get("actions").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("actions")) actions.add(e.getAsString());
        }
        player = 0;
        for (int i = 0; i < players.size(); i++) if (players.get(i).name().equals(selected)) player = i;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private Entry current() {
        return player < players.size() ? players.get(player) : null;
    }

    @Override
    public void init(Host host, Rect area) {
        this.host = host;
        backend = host.backend();
        AdminForm f = new AdminForm()
                .section(Component.translatable("pwp_core.admin.mod.target"))
                .control(Component.translatable("pwp_core.admin.mod.player"), new UIChoice(this::playerText, d -> {
                    if (!players.isEmpty()) player = Math.floorMod(player + d, players.size());
                }))
                .text(this::detailsText, () -> PWPTheme.Text.BODY)
                .control(
                        Component.translatable("pwp_core.admin.mod.reason"),
                        new UIChoice(() -> reasonText(), d -> reason = Math.floorMod(reason + d, REASONS.length)))
                .gap()
                .section(Component.translatable("pwp_core.admin.mod.actions"))
                .buttons(
                        button(
                                host,
                                "teleport",
                                UIButton.Style.DARK,
                                UIIcon.PLAY,
                                false,
                                ModerationAction.Teleport::new),
                        button(
                                host,
                                "spectate",
                                UIButton.Style.DARK,
                                UIIcon.INFO,
                                false,
                                ModerationAction.Spectate::new))
                .buttons(
                        button(
                                host,
                                "warn",
                                UIButton.Style.DARK,
                                UIIcon.WARNING,
                                false,
                                n -> new ModerationAction.Warn(n, reasonStr())),
                        button(
                                host,
                                "clear_tk",
                                UIButton.Style.DARK,
                                UIIcon.CHECK,
                                false,
                                ModerationAction.ClearTeamkill::new))
                .buttons(
                        button(
                                host,
                                "team_blue",
                                UIButton.Style.DARK,
                                UIIcon.PROFILE,
                                false,
                                n -> new ModerationAction.TeamJoin(n, "BLUE")),
                        button(
                                host,
                                "team_red",
                                UIButton.Style.DARK,
                                UIIcon.PROFILE,
                                false,
                                n -> new ModerationAction.TeamJoin(n, "RED")));
        // кнопки модов — парами, только объявленные сервером и доступные по уровню
        List<UIButton> extra = new ArrayList<>();
        for (ExtraAction a : EXTRA) {
            if (!actions.contains(a.id()) || !AdminPanel.level().atLeast(a.min())) continue;
            extra.add(extraButton(host, a));
        }
        for (int i = 0; i < extra.size(); i += 2) {
            if (i + 1 < extra.size()) f.buttons(extra.get(i), extra.get(i + 1));
            else f.buttons(extra.get(i));
        }
        f.control(
                        Component.translatable("pwp_core.admin.mod.mute_minutes"),
                        new UIChoice(
                                () -> Component.translatable("pwp_core.admin.mod.minutes", MUTE_MINUTES[mute]),
                                d -> mute = Math.floorMod(mute + d, MUTE_MINUTES.length)),
                        button(
                                host,
                                "mute",
                                UIButton.Style.DARK,
                                UIIcon.CLOSE,
                                false,
                                n -> new ModerationAction.VoiceMute(n, MUTE_MINUTES[mute], reasonStr())))
                .control(
                        Component.translatable("pwp_core.admin.mod.ban_duration"),
                        new UIChoice(
                                () -> Component.translatable("pwp_core.admin.mod.duration." + DURATIONS[duration]),
                                d -> duration = Math.floorMod(duration + d, DURATIONS.length)),
                        button(
                                host,
                                "ban",
                                UIButton.Style.DANGER,
                                UIIcon.CLOSE,
                                true,
                                n -> new ModerationAction.Ban(n, DURATIONS[duration], reasonStr())))
                .buttons(button(
                        host,
                        "kick",
                        UIButton.Style.DANGER,
                        UIIcon.QUIT,
                        true,
                        n -> new ModerationAction.Kick(n, reasonStr())))
                .text(() -> Component.translatable("pwp_core.admin.mod.hint"), () -> PWPTheme.Text.MUTED);
        host.add(f.build(area));
    }

    private UIButton button(
            Host host, String id, UIButton.Style style, UIIcon icon, boolean confirm, Function<String, Object> packet) {
        Component label = Component.translatable("pwp_core.admin.mod." + id);
        return confirmed(
                        host,
                        "mod." + id,
                        label,
                        confirm,
                        name -> PacketHandler.INSTANCE.sendToServer(packet.apply(name)))
                .style(style)
                .icon(icon);
    }

    private UIButton extraButton(Host host, ExtraAction a) {
        return confirmed(host, a.id(), Component.translatable(a.labelKey()), a.confirm(), a.send())
                .style(a.confirm() ? UIButton.Style.DANGER : UIButton.Style.DARK)
                .icon(a.icon());
    }

    /** Кнопка над текущим игроком; {@code confirm} — через модальное окно с ником и причиной. */
    private UIButton confirmed(Host host, String actionId, Component label, boolean confirm, Consumer<String> send) {
        return new UIButton(label, () -> {
            Entry e = current();
            if (e == null) return;
            Runnable go = () -> host.backend().perform(actionId, () -> send.accept(e.name()));
            if (!confirm) {
                go.run();
                return;
            }
            host.showModal(new UIModal(
                            label, Component.translatable("pwp_core.admin.mod.confirm", label, e.name(), reasonText()))
                    .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                    .button(label, UIButton.Style.DANGER, go));
        });
    }

    private Component playerText() {
        Entry e = current();
        if (e == null) return Component.translatable("pwp_core.admin.mod.no_players");
        String team = e.team().isEmpty() ? "" : " [" + e.team() + "]";
        return Component.literal(e.name() + team);
    }

    /** Строка под игроком: отряд · кит · лидер · пинг · наблюдение. */
    private Component detailsText() {
        Entry e = current();
        if (e == null) return Component.empty();
        Component squad = e.squad().isEmpty()
                ? Component.translatable("pwp_core.admin.mod.no_squad")
                : Component.translatable(
                        e.leader() ? "pwp_core.admin.mod.squad_leader" : "pwp_core.admin.mod.squad", e.squad());
        Component kit =
                e.kit().isEmpty() ? Component.translatable("pwp_core.admin.mod.no_kit") : Component.literal(e.kit());
        Component ping =
                e.ping() < 0 ? Component.literal("—") : Component.translatable("pwp_core.admin.mod.ping", e.ping());
        return Component.translatable("pwp_core.admin.mod.details", squad, kit, ping, e.level());
    }

    private Component reasonText() {
        return Component.translatable("pwp_core.admin.mod.reason." + REASONS[reason]);
    }

    private String reasonStr() {
        return reasonText().getString();
    }
}

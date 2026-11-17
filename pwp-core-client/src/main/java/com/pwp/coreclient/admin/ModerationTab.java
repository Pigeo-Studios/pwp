package com.pwp.coreclient.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ModerationAction;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Вкладка «Модерация» (MODERATOR+): игрок онлайн → предупреждение, кик, мут голоса, бан на время
 * ({@code /pwpban}), перевод в команду, снятие наказания за тимкилл. Причины — готовые варианты (в ките
 * нет поля ввода); текст причины уходит на сервер в языке модератора и чистится сервером. Кик/бан — с
 * подтверждением. Права и ник проверяет сервер ({@link ModerationAction}).
 */
@OnlyIn(Dist.CLIENT)
public class ModerationTab implements AdminTab {
    private static final Logger LOG = LoggerFactory.getLogger(ModerationTab.class);
    private static final int POLL_TICKS = 40;
    private static final String[] REASONS = {"rules", "grief", "teamkill", "toxic", "spam", "cheat"};
    private static final String[] DURATIONS = {"30m", "1h", "6h", "1d", "7d", "30d", "perm"};
    private static final int[] MUTE_MINUTES = {5, 15, 30, 60, 1440};

    private record Entry(String name, String team, String level) {}

    private final List<Entry> players = new ArrayList<>();
    private int player;
    private int reason;
    private int duration = 3;
    private int mute = 1;
    private int ticks;
    private AdminBackend backend;

    public static JsonObject fixture(boolean empty) {
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        if (!empty) {
            String[][] rows = {
                {"Steve", "Blue", "PLAYER"}, {"Alex_Sniper", "Red", "PLAYER"}, {"ModerMax", "", "MODERATOR"}
            };
            for (String[] r : rows) {
                JsonObject e = new JsonObject();
                e.addProperty("name", r[0]);
                e.addProperty("team", r[1]);
                e.addProperty("level", r[2]);
                arr.add(e);
            }
        }
        o.add("players", arr);
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
            accept(JsonParser.parseString(json).getAsJsonObject());
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
                        p.get("name").getAsString(),
                        p.get("team").getAsString(),
                        p.get("level").getAsString()));
            }
        }
        player = 0;
        for (int i = 0; i < players.size(); i++) if (players.get(i).name().equals(selected)) player = i;
    }

    private Entry current() {
        return player < players.size() ? players.get(player) : null;
    }

    @Override
    public void init(Host host, Rect area) {
        backend = host.backend();
        AdminForm f = new AdminForm()
                .section(Component.translatable("pwp_core.admin.mod.target"))
                .control(Component.translatable("pwp_core.admin.mod.player"), new UIChoice(this::playerText, d -> {
                    if (!players.isEmpty()) player = Math.floorMod(player + d, players.size());
                }))
                .control(
                        Component.translatable("pwp_core.admin.mod.reason"),
                        new UIChoice(() -> reasonText(), d -> reason = Math.floorMod(reason + d, REASONS.length)))
                .gap()
                .section(Component.translatable("pwp_core.admin.mod.actions"))
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
                                n -> new ModerationAction.TeamJoin(n, "RED")))
                .control(
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
            Host host,
            String id,
            UIButton.Style style,
            UIIcon icon,
            boolean confirm,
            java.util.function.Function<String, Object> packet) {
        Component label = Component.translatable("pwp_core.admin.mod." + id);
        return new UIButton(label, () -> {
                    Entry e = current();
                    if (e == null) return;
                    Runnable send = () -> host.backend()
                            .perform("mod." + id, () -> PacketHandler.INSTANCE.sendToServer(packet.apply(e.name())));
                    if (!confirm) {
                        send.run();
                        return;
                    }
                    host.showModal(new UIModal(
                                    label,
                                    Component.translatable("pwp_core.admin.mod.confirm", label, e.name(), reasonText()))
                            .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                            .button(label, UIButton.Style.DANGER, send));
                })
                .style(style)
                .icon(icon);
    }

    private Component playerText() {
        Entry e = current();
        if (e == null) return Component.translatable("pwp_core.admin.mod.no_players");
        String team = e.team().isEmpty() ? "" : " [" + e.team() + "]";
        return Component.literal(e.name() + team);
    }

    private Component reasonText() {
        return Component.translatable("pwp_core.admin.mod.reason." + REASONS[reason]);
    }

    private String reasonStr() {
        return reasonText().getString();
    }
}

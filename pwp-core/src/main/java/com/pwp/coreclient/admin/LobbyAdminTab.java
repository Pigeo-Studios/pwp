package com.pwp.coreclient.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.api.config.LobbySettings;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIToggle;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Вкладка «Лобби» (только на лобби-сервере, раздел {@code lobby}): фаза голосования, старт/стоп
 * голосования, настройки автостарта/лимита матчей/минимума игроков. Настройки сохраняет сервер в
 * {@code config/pwp-server.json} (секция lobby). Данные — тип {@code adminLobby} (регистрирует pwp-lobby).
 *
 * <p>UI живёт в core-client (как и {@code PWPLobbyScreen}): pwp-lobby — серверный мод.
 */
@OnlyIn(Dist.CLIENT)
public class LobbyAdminTab implements AdminTab {
    private static final Logger LOG = LoggerFactory.getLogger(LobbyAdminTab.class);
    public static final String TYPE = "adminLobby";
    /** Опрос раз в секунду. */
    private static final int POLL_TICKS = 20;

    private JsonObject data;
    private LobbySettings pending;
    private boolean dirty;
    private int ticks;
    private AdminBackend backend;

    /** Фикстура тест-стенда. */
    public static JsonObject fixture(boolean empty) {
        JsonObject o = new JsonObject();
        o.addProperty("phase", empty ? "idle" : "map_vote");
        o.addProperty("remaining", empty ? 0 : 42);
        o.addProperty("lobbyPlayers", empty ? 0 : 7);
        o.addProperty("activeMatches", empty ? 0 : 1);
        o.add("settings", LobbySettings.DEFAULT.toJson());
        return o;
    }

    /** OWNER: доступна запись настроек в файл «навсегда». */
    private final boolean owner;

    /** @param preset данные фикстуры (тест-стенд) или null — ждать сервер */
    public LobbyAdminTab(boolean owner, JsonObject preset) {
        this.owner = owner;
        if (preset != null) accept(preset);
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
            backend.request(TYPE, "");
        }
    }

    @Override
    public void onData(String type, String json) {
        if (!TYPE.equals(type)) return;
        try {
            accept(JsonParser.parseString(json).getAsJsonObject());
        } catch (JsonParseException | IllegalStateException e) {
            LOG.warn("LobbyAdminTab: битый ответ {}", TYPE, e);
        }
    }

    private void accept(JsonObject o) {
        data = o;
        if (!dirty && o.has("settings") && o.get("settings").isJsonObject()) {
            pending = LobbySettings.fromJson(o.getAsJsonObject("settings"), new java.util.ArrayList<>());
        }
    }

    @Override
    public void init(Host host, Rect area) {
        backend = host.backend();
        if (pending == null) pending = LobbySettings.DEFAULT;
        UIButton apply = new UIButton(Component.translatable("pwp_core.admin.lobby.apply"), () -> send(host, false))
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.CHECK);
        UIButton save = new UIButton(Component.translatable("pwp_core.admin.lobby.save"), () -> send(host, true))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.CHECK);
        save.tooltip(com.pwp.coreclient.ui.UITooltip.Content.of(
                Component.translatable(owner ? "pwp_core.admin.lobby.save.tip" : "pwp_core.admin.owner_only")));
        save.setEnabled(owner);
        AdminForm f = new AdminForm()
                .section(Component.translatable("pwp_core.admin.lobby.status"))
                .value(Component.translatable("pwp_core.admin.lobby.phase"), this::phaseText)
                .value(
                        Component.translatable("pwp_core.admin.lobby.players"),
                        () -> Component.literal(str("lobbyPlayers")))
                .value(
                        Component.translatable("pwp_core.admin.lobby.matches"),
                        () -> Component.translatable(
                                "pwp_core.admin.lobby.matches.value", str("activeMatches"), pending.maxMatches()))
                .value(Component.translatable("pwp_core.admin.lobby.prep"), this::prepText)
                .buttons(
                        new UIButton(Component.translatable("pwp_core.admin.lobby.start_vote"), () -> host.backend()
                                        .perform(
                                                AdminAction.LOBBY_START_VOTE,
                                                () -> PacketHandler.INSTANCE.sendToServer(
                                                        new AdminAction.LobbyStartVote())))
                                .style(UIButton.Style.ACCENT)
                                .icon(UIIcon.PLAY),
                        new UIButton(Component.translatable("pwp_core.admin.lobby.stop_vote"), () -> host.backend()
                                        .perform(
                                                AdminAction.LOBBY_STOP_VOTE,
                                                () -> PacketHandler.INSTANCE.sendToServer(
                                                        new AdminAction.LobbyStopVote())))
                                .style(UIButton.Style.DANGER)
                                .icon(UIIcon.CLOSE))
                .gap()
                .section(Component.translatable("pwp_core.admin.lobby.settings"))
                .control(
                        Component.translatable("pwp_core.admin.lobby.autostart"),
                        new UIToggle(
                                () -> pending.autostart(), () -> edit(pending.withAutostart(!pending.autostart()))))
                .control(
                        Component.translatable("pwp_core.admin.lobby.max_matches"),
                        new UIChoice(
                                () -> Component.literal(String.valueOf(pending.maxMatches())),
                                dir -> edit(pending.withMaxMatches(pending.maxMatches() + dir))))
                .control(
                        Component.translatable("pwp_core.admin.lobby.min_players"),
                        new UIChoice(
                                () -> Component.literal(String.valueOf(pending.minPlayers())),
                                dir -> edit(pending.withMinPlayers(
                                        pending.minPlayers() + dir * (Screen.hasShiftDown() ? 10 : 1)))))
                .text(
                        () -> Component.translatable(
                                dirty
                                        ? "pwp_core.admin.lobby.unsaved"
                                        : persisted()
                                                ? "pwp_core.admin.lobby.saved_hint"
                                                : "pwp_core.admin.lobby.not_persisted"),
                        () -> dirty ? PWPTheme.Text.ACCENT : PWPTheme.Text.MUTED)
                .buttons(apply, save);
        host.add(f.build(area));
    }

    private void send(Host host, boolean persist) {
        LobbySettings s = pending;
        host.backend()
                .perform(
                        AdminAction.LOBBY_SAVE_SETTINGS,
                        () -> PacketHandler.INSTANCE.sendToServer(new AdminAction.LobbySaveSettings(
                                s.autostart(), s.maxMatches(), s.minPlayers(), persist)));
        dirty = false;
    }

    private void edit(LobbySettings s) {
        pending = s;
        dirty = true;
    }

    private Component phaseText() {
        String phase = data == null ? "unknown" : str("phase");
        Component p = Component.translatable("pwp_core.admin.lobby.phase." + phase);
        int remaining =
                data != null && data.has("remaining") ? data.get("remaining").getAsInt() : 0;
        return remaining > 0 ? Component.translatable("pwp_core.admin.lobby.phase.timer", p, remaining) : p;
    }

    private boolean persisted() {
        return data == null || !data.has("persisted") || data.get("persisted").getAsBoolean();
    }

    private String str(String key) {
        if (data == null || !data.has(key) || data.get(key).isJsonNull()) return "—";
        return data.get(key).getAsString();
    }

    /** Предпрогрев матч-сервера: тот же текст, что в HUD лобби (причина сбоя — переведённый ключ). */
    private Component prepText() {
        if (data == null || !data.has("prepState")) return Component.translatable("pwp_core.admin.lobby.prep.none");
        Component line = com.pwp.coreclient.lobby.LobbyHud.prepLine(
                data.get("prepState").getAsInt(),
                data.has("prepPercent") ? data.get("prepPercent").getAsInt() : 0,
                data.has("prepError") ? data.get("prepError").getAsString() : "");
        return line == null ? Component.translatable("pwp_core.admin.lobby.prep.none") : line;
    }
}

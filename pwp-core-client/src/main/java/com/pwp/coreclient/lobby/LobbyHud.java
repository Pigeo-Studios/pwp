package com.pwp.coreclient.lobby;

import com.mojang.blaze3d.platform.InputConstants;
import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.LobbyStatePacket;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIToast;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/**
 * Лобби на клиенте: клавиша «Меню лобби», подсказка в HUD с реально забинженной клавишей,
 * тост + автооткрытие при старте голосования, признак «мы на лобби-сервере» (для кнопки в
 * меню паузы). Всё состояние — только из {@link LobbyStatePacket} (сервер шлёт его раз в
 * секунду): «на лобби» = пакет приходил недавно; при отключении сбрасывается.
 */
@OnlyIn(Dist.CLIENT)
public final class LobbyHud {
    /** Дефолт J: свободна в ванили и в наших модах (K — деплой, M — карта, L — достижения). */
    public static final KeyMapping LOBBY_KEY = new KeyMapping(
            "key.pwp_core.lobby_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.pwp");

    /** Пакет лобби старше этого — считаем, что мы уже не на лобби-сервере. */
    private static final long LOBBY_STALE_MS = 5000L;

    private static final long TOAST_MS = 6000L;

    private static LobbyStatePacket last;
    private static long lastAt;
    private static int prevPhase = -1;

    private LobbyHud() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(LobbyHud::onRegisterKeys);
        modBus.addListener(LobbyHud::onRegisterOverlays);
        MinecraftForge.EVENT_BUS.addListener(LobbyHud::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(LobbyHud::onScreenRenderPost);
        MinecraftForge.EVENT_BUS.addListener(LobbyHud::onLoggingOut);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent e) {
        e.register(LOBBY_KEY);
    }

    private static void onRegisterOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("pwp_lobby_hint", (gui, g, pt, w, h) -> renderHud(g, w));
    }

    /** Приём состояния лобби (главный поток): тост/автооткрытие при старте голосования. */
    public static void onState(LobbyStatePacket pkt) {
        boolean voteStarted = prevPhase != -1 && isVotePhase(pkt.phase) && !isVotePhase(prevPhase);
        prevPhase = pkt.phase;
        last = pkt;
        lastAt = System.currentTimeMillis();
        PWPLobbyScreen.updateLobbyState(pkt);
        if (voteStarted) {
            UIToast.show(
                    "lobby_vote_started",
                    Component.translatable("pwp_core.lobby.hint.vote_started", keyName()),
                    UIToast.Kind.INFO,
                    TOAST_MS);
            // автооткрытие — только если игрок не в другом экране (чат, инвентарь, пауза…)
            if (Minecraft.getInstance().screen == null) PWPLobbyScreen.openManually(pkt);
        }
    }

    /** Мы на лобби-сервере (недавно приходил {@link LobbyStatePacket}). */
    public static boolean isOnLobby() {
        return last != null && System.currentTimeMillis() - lastAt < LOBBY_STALE_MS;
    }

    /** Последнее состояние лобби (null — не на лобби); для подсказок. */
    public static LobbyStatePacket lastState() {
        return isOnLobby() ? last : null;
    }

    public static boolean isVoting() {
        return isOnLobby() && isVotePhase(last.phase);
    }

    /** Открыть меню лобби с последним известным состоянием. */
    public static void openLobby() {
        if (isOnLobby()) PWPLobbyScreen.openManually(last);
    }

    public static Component keyName() {
        return LOBBY_KEY.getTranslatedKeyMessage();
    }

    private static boolean isVotePhase(int phase) {
        return phase == LobbyStatePacket.PHASE_MAP_VOTE
                || phase == LobbyStatePacket.PHASE_MODE_VOTE
                || phase == LobbyStatePacket.PHASE_FACTION_VOTE;
    }

    private static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        // consumeClick срабатывает только без открытого экрана; закрытие тем же ключом — в PWPLobbyScreen
        while (LOBBY_KEY.consumeClick()) {
            if (Minecraft.getInstance().screen == null) openLobby();
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        last = null;
        lastAt = 0L;
        prevPhase = -1;
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post e) {
        // тосты поверх любого экрана (в HUD-оверлее экран их перекрыл бы)
        UIToast.render(e.getGuiGraphics(), e.getScreen().width);
    }

    private static void renderHud(GuiGraphics g, int screenW) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // тосты поверх экрана рисует onScreenRenderPost
        UIToast.render(g, screenW);
        if (mc.options.hideGui || !isOnLobby()) return;
        boolean voting = isVotePhase(last.phase);
        Component text = voting
                ? Component.translatable("pwp_core.lobby.hint.voting", keyName(), last.remainingSeconds)
                : last.phase == LobbyStatePacket.PHASE_IDLE
                                && last.minPlayers > 0
                                && last.onlinePlayers < last.minPlayers
                        ? Component.translatable(
                                "pwp_core.lobby.hint.waiting_players", keyName(), last.onlinePlayers, last.minPlayers)
                        : Component.translatable("pwp_core.lobby.hint.open", keyName());
        var font = mc.font;
        int w = font.width(text) + 2 * Spacing.MD;
        int h = font.lineHeight + 2 * Spacing.SM;
        Rect r = Rect.of((screenW - w) / 2, Spacing.SM, w, h);
        UIDraw.glass(g, r, voting ? 1F : 0.7F, voting);
        if (voting) UIDraw.accentBarH(g, r.x(), r.bottom() - 1, r.w(), 1, PWPTheme.Colors.ACCENT);
        int color = voting ? PWPTheme.Text.ACCENT : PWPTheme.Text.MUTED;
        UIDraw.text(g, font, text, r.x() + Spacing.MD, r.y() + Spacing.SM + 1, color, true);
    }
}

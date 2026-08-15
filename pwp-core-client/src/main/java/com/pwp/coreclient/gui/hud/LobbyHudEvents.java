package com.pwp.coreclient.gui.hud;

import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * HUD-события лобби (FORGE-шина):
 *  - Pre:  подавление ванильного PLAYER_LIST, пока состояние лобби актуально;
 *  - Post: точка ◉ + баннер-подсказка голосования;
 *  - tick: обработка клавиши [TAB] (открытие PWPLobbyScreen);
 *  - logout: полная очистка lobby-состояния (никакого stale HUD после выхода).
 *
 * В матчах LobbyStatePacket не приходит — состояние протухает за 3 секунды,
 * и весь перехват автоматически отключается (ванильное поведение не меняется).
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT)
public class LobbyHudEvents {

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!PWPLobbyScreen.isLobbyStateFresh()) return;
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_LIST.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        // Рендерим строго на одном оверлее: событие срабатывает ПОСЛЕ КАЖДОГО
        // зарегистрированного оверлея (~10-15 раз за кадр) — без фильтра HUD
        // перерисовывался бы десятки раз (источник лагов)
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;
        if (!PWPLobbyScreen.isLobbyStateFresh()) return;
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() / mc.getWindow().getGuiScale();
        double my = mc.mouseHandler.ypos() / mc.getWindow().getGuiScale();
        VoteHintToast.renderDot(event.getGuiGraphics(), (int) mx, (int) my);
        VoteHintToast.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() != 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!PWPLobbyScreen.isLobbyStateFresh()) return;
        // Координаты InputEvent.MouseButton не несут позицию — берём текущую позицию мыши
        // (пиксели окна → GUI-масштаб)
        double sx = mc.mouseHandler.xpos() / mc.getWindow().getGuiScale();
        double sy = mc.mouseHandler.ypos() / mc.getWindow().getGuiScale();
        if (VoteHintToast.clickAt(sx, sy)) {
            PWPLobbyScreen.openScreen();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!PWPLobbyScreen.isLobbyStateFresh()) return;
        if (LobbyKeyMappings.OPEN_LOBBY_MENU_KEY.consumeClick()) {
            PWPLobbyScreen.openScreen();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PWPLobbyScreen.clearCachedState();
        PWPLobbyScreen.resetInstance();
        VoteHintToast.reset();
    }
}

package com.pwp.coreclient.hints;

import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import com.pwp.coreclient.lobby.LobbyHud;
import com.pwp.coreclient.network.LobbyStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Подсказки core: главное меню и лобби. Подсказки матча регистрирует warfare (WarfareHints). */
@OnlyIn(Dist.CLIENT)
public final class CoreHints {
    private CoreHints() {}

    public static void register() {
        // главное меню: новичку — «Как играть», затем «Играть»
        Hints.register(
                "menu.howto",
                20,
                true,
                () -> Minecraft.getInstance().screen instanceof PWPMainMenuScreen,
                () -> Component.translatable("pwp_core.hint.menu.howto"));
        Hints.register(
                "menu.play",
                10,
                true,
                () -> Minecraft.getInstance().screen instanceof PWPMainMenuScreen,
                () -> Component.translatable("pwp_core.hint.menu.play"));
        // лобби: голосование идёт, а меню закрыто
        Hints.register(
                "lobby.vote",
                40,
                false,
                () -> Minecraft.getInstance().screen == null && LobbyHud.isVoting(),
                () -> Component.translatable("pwp_core.hint.lobby.vote", LobbyHud.keyName()));
        // лобби: ждём игроков
        Hints.register(
                "lobby.waiting",
                30,
                false,
                () -> {
                    LobbyStatePacket st = LobbyHud.lastState();
                    return st != null
                            && st.phase == LobbyStatePacket.PHASE_IDLE
                            && st.minPlayers > 0
                            && st.onlinePlayers < st.minPlayers;
                },
                () -> {
                    LobbyStatePacket st = LobbyHud.lastState();
                    return Component.translatable(
                            "pwp_core.hint.lobby.waiting",
                            st != null ? st.onlinePlayers : 0,
                            st != null ? st.minPlayers : 0);
                });
        // лобби: где меню (разово)
        Hints.register(
                "lobby.menu",
                20,
                true,
                () -> LobbyHud.lastState() != null && Minecraft.getInstance().screen == null,
                () -> Component.translatable("pwp_core.hint.lobby.menu", LobbyHud.keyName()));
        // в меню лобби: как вернуться
        Hints.register(
                "lobby.screen",
                10,
                true,
                () -> Minecraft.getInstance().screen instanceof PWPLobbyScreen,
                () -> Component.translatable("pwp_core.hint.lobby.screen", LobbyHud.keyName()));
    }
}

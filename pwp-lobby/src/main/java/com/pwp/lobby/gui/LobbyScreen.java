package com.pwp.lobby.gui;

import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import com.pwp.coreclient.network.OpenMatchListScreenPacket;
import com.pwp.coreclient.network.OpenMatchScreenPacket;
import com.pwp.coreclient.network.OpenModeVotePacket;
import com.pwp.coreclient.network.OpenVotingScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Forwarding shim — вся логика теперь в PWPLobbyScreen (pwp-core-client).
 * Сохранён для обратной совместимости рефлексии и прямых импортов.
 */
@Deprecated
public class LobbyScreen extends Screen {

    private static LobbyScreen instance;

    public LobbyScreen() {
        super(Component.literal("PWP"));
        instance = this;
    }

    public static LobbyScreen get() { return instance; }

    public static void openMatch(OpenMatchScreenPacket pkt) {
        PWPLobbyScreen.openMatch(pkt);
    }

    public static void updateVote(OpenVotingScreenPacket pkt) {
        PWPLobbyScreen.updateVote(pkt);
    }

    public static void openModeVote(OpenModeVotePacket pkt) {
        PWPLobbyScreen.openModeVote(pkt);
    }

    public static void openVote(OpenVotingScreenPacket pkt) {
        PWPLobbyScreen.openVote(pkt);
    }

    public static void openList(OpenMatchListScreenPacket pkt) {
        PWPLobbyScreen.openList(pkt);
    }

    public static void updateList(OpenMatchListScreenPacket pkt) {
        PWPLobbyScreen.updateList(pkt);
    }

    public static void resetInstance() {
        PWPLobbyScreen.resetInstance();
        instance = null;
    }

    @Override
    protected void init() {
        Minecraft.getInstance().setScreen(new PWPLobbyScreen());
    }

    @Override
    public void onClose() {
        super.onClose();
        instance = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

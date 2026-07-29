package com.pwp.coreclient;

import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import com.pwp.coreclient.gui.screens.PWPDisconnectedScreen;
import com.pwp.coreclient.mixin.DisconnectedScreenAccessor;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.ScreenEvent;

public class ClientScreenHandler {

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientScreenHandler::onScreenOpen);
    }

    private static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen) {
            event.setNewScreen(new PWPMainMenuScreen());
        } else if (event.getNewScreen() instanceof JoinMultiplayerScreen) {
            event.setNewScreen(new PWPMainMenuScreen());
        } else if (event.getNewScreen() instanceof DisconnectedScreen) {
            Component reason = ((DisconnectedScreenAccessor) event.getNewScreen()).getReason();
            event.setNewScreen(new PWPDisconnectedScreen(reason));
        }
    }
}

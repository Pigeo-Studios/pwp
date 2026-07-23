package com.pwp.coreclient;

import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import net.minecraft.client.gui.screens.TitleScreen;
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
        }
    }
}

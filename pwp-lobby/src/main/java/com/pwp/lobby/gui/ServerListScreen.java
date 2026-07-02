package com.pwp.lobby.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ServerListScreen extends Screen {

    public ServerListScreen() {
        super(Component.literal("Active Matches"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = 40;

        addRenderableWidget(Button.builder(
                Component.literal("No active matches yet"),
                b -> {}).bounds(cx - 100, y, 200, 25).build());

        addRenderableWidget(Button.builder(
                Component.literal("Close"),
                b -> onClose()).bounds(cx - 40, height - 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);
        gui.drawCenteredString(font, "Active Matches", width / 2, 15, 0xFFFFFF);
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.setScreen(new ServerListScreen());
    }
}

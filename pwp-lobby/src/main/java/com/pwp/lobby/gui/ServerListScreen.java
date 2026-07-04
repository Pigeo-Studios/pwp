package com.pwp.lobby.gui;

import com.pwp.coreclient.gui.theme.PWPTheme;
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
        int cy = height / 2;

        addRenderableWidget(Button.builder(
                Component.literal("No active matches"),
                b -> {}).bounds(cx - 90, cy - 15, 180, 25).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Close"),
                b -> onClose()).bounds(cx - 40, height - 28, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);
        gui.drawCenteredString(font, "\u2694 Active Matches", width / 2, 15, PWPTheme.Colors.TEXT_ACCENT);
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.setScreen(new ServerListScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
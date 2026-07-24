package com.pwp.lobby.gui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ServerListScreen extends Screen {

    public ServerListScreen() {
        super(Component.literal("Активные матчи"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        addRenderableWidget(Button.builder(
                Component.literal("Нет активных матчей"),
                b -> {}).bounds(cx - 100, cy - 12, 200, 24).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Закрыть"),
                b -> onClose()).bounds(cx - 40, height - 30, 80, 22).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        int cx = width / 2;
        gui.drawCenteredString(font, Component.literal("АКТИВНЫЕ МАТЧИ"), cx, 14, PWPTheme.Colors.TEXT_ACCENT);
        gui.fill(cx - 70, 24, cx + 70, 25, PWPTheme.Colors.ACCENT);
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

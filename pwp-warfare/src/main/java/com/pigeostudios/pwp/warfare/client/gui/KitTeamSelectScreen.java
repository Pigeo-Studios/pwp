package com.pigeostudios.pwp.warfare.client.gui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KitTeamSelectScreen extends Screen {
    private static final int PANEL_W = 240;
    private static final int PANEL_H = 80;

    public KitTeamSelectScreen() {
        super(Component.translatable("gui.pwpwarfare.kit_team_select.title"));
    }

    @Override
    protected void init() {
        int cx = (width - PANEL_W) / 2;
        int cy = (height - PANEL_H) / 2;

        addRenderableWidget(
            Button.builder(
                Component.translatable("gui.pwpwarfare.kit_team_select.blue"),
                b -> minecraft.setScreen(new KitListScreen("BLUE"))
            ).bounds(cx + 10, cy + 40, 100, 20).build()
        );
        addRenderableWidget(
            Button.builder(
                Component.translatable("gui.pwpwarfare.kit_team_select.red"),
                b -> minecraft.setScreen(new KitListScreen("RED"))
            ).bounds(cx + PANEL_W - 110, cy + 40, 100, 20).build()
        );
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int cx = (width - PANEL_W) / 2;
        int cy = (height - PANEL_H) / 2;

        gui.fill(cx, cy, cx + PANEL_W, cy + PANEL_H, PWPTheme.Colors.SURFACE);
        gui.renderOutline(cx, cy, PANEL_W, PANEL_H, PWPTheme.Colors.BORDER);

        gui.drawCenteredString(font, title, width / 2, cy + 8, PWPTheme.Colors.TEXT_PRIMARY);
        gui.fill(cx + 8, cy + 20, cx + PANEL_W - 8, cy + 21, PWPTheme.Colors.ACCENT);

        super.render(gui, mx, my, pt);
    }
}

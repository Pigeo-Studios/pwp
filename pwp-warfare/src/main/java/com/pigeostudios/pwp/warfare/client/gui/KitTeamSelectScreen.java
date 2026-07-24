package com.pigeostudios.pwp.warfare.client.gui;

import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KitTeamSelectScreen extends Screen {
    private static final int PANEL_W = 240;
    private static final int PANEL_H = 100;

    public KitTeamSelectScreen() {
        super(Component.translatable("gui.pwpwarfare.kit_team_select.title"));
    }

    @Override
    protected void init() {
        int cx = PWPLayout.centerX(width, PANEL_W);
        int cy = (height - PANEL_H) / 2;

        addRenderableWidget(new PWPButton(
            cx + 10, cy + 50, 100, 22,
            Component.translatable("gui.pwpwarfare.kit_team_select.blue"),
            b -> minecraft.setScreen(new KitListScreen("BLUE")),
            PWPButton.Style.PRIMARY
        ));

        addRenderableWidget(new PWPButton(
            cx + PANEL_W - 110, cy + 50, 100, 22,
            Component.translatable("gui.pwpwarfare.kit_team_select.red"),
            b -> minecraft.setScreen(new KitListScreen("RED")),
            PWPButton.Style.DANGER
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int cx = PWPLayout.centerX(width, PANEL_W);
        int cy = (height - PANEL_H) / 2;

        PWPPanel.render(gui, cx, cy, PANEL_W, PANEL_H);

        gui.drawCenteredString(font, title, width / 2, cy + 12, PWPTheme.Colors.TEXT_PRIMARY);
        PWPLayout.renderDivider(gui, cx + 10, cy + 28, PANEL_W - 20);

        super.render(gui, mx, my, pt);
    }
}

package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.components.*;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PWPSettingsScreen extends Screen {

    public PWPSettingsScreen() {
        super(Component.literal("НАСТРОЙКИ"));
    }

    @Override
    protected void init() {
        super.init();

        int btnW = 140;
        int btnH = 28;
        int gap = 10;
        int cx = width / 2;

        addRenderableWidget(new PWPButton(
            cx - btnW - gap / 2, height - 50, btnW, btnH,
            Component.literal("Сохранить"),
            btn -> onClose(),
            PWPButton.Style.ACCENT
        ));

        addRenderableWidget(new PWPButton(
            cx + gap / 2, height - 50, btnW, btnH,
            Component.literal("Отмена"),
            btn -> onClose(),
            PWPButton.Style.GHOST
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        PWPLayout.renderHeader(gui, "НАСТРОЙКИ", width);

        int panelY = 24;
        int panelH = height - panelY - 60;
        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);

        PWPPanel.render(gui, cx, panelY, contentW, panelH);

        gui.drawCenteredString(font, Component.literal("Настройки (заглушка)"), width / 2, panelY + panelH / 2 - 4,
            PWPTheme.Colors.TEXT_DIM);

        PWPLayout.renderFooter(gui, "PWP v1.0.1", width, height);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

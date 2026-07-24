package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.components.PWPTabs;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PWPLobbyScreen extends Screen {

    private PWPTabs tabs;
    private int selectedTab;

    private static final int TAB_MATCHES = 0;
    private static final int TAB_VOTING = 1;
    private static final int TAB_STATS = 2;

    public PWPLobbyScreen() {
        super(Component.literal("ЛОББИ"));
    }

    @Override
    protected void init() {
        super.init();

        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);

        tabs = new PWPTabs(cx, PWPPanel.titleHeight() + 6, contentW, selectedTab, idx -> selectedTab = idx);
        tabs.setTabs(List.of("Матчи", "Голосование", "Статистика"));
        tabs.getWidgets().forEach(this::addRenderableWidget);

        int panelY = PWPPanel.titleHeight() + 32;
        int panelH = height - panelY - 30;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        PWPLayout.renderHeader(gui, "ЛОББИ", width);

        if (tabs != null) {
            tabs.render(gui, mouseX, mouseY, partialTick);
        }

        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);
        int panelY = PWPPanel.titleHeight() + 32;
        int panelH = height - panelY - 30;

        switch (selectedTab) {
            case TAB_MATCHES:
                renderMatches(gui, cx, panelY, contentW, panelH);
                break;
            case TAB_VOTING:
                renderVoting(gui, cx, panelY, contentW, panelH);
                break;
            case TAB_STATS:
                renderStats(gui, cx, panelY, contentW, panelH);
                break;
        }

        PWPLayout.renderFooter(gui, "Назад", width, height);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderMatches(GuiGraphics gui, int cx, int y, int w, int h) {
        PWPPanel.render(gui, cx, y, w, h);
        gui.drawCenteredString(font, Component.literal("Нет активных матчей"), width / 2, y + h / 2 - 4, PWPTheme.Colors.TEXT_DIM);
    }

    private void renderVoting(GuiGraphics gui, int cx, int y, int w, int h) {
        PWPPanel.render(gui, cx, y, w, h);
        gui.drawCenteredString(font, Component.literal("Голосование недоступно"), width / 2, y + h / 2 - 4, PWPTheme.Colors.TEXT_DIM);
    }

    private void renderStats(GuiGraphics gui, int cx, int y, int w, int h) {
        PWPPanel.render(gui, cx, y, w, h);
        gui.drawCenteredString(font, Component.literal("Загрузка статистики..."), width / 2, y + h / 2 - 4, PWPTheme.Colors.TEXT_DIM);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}

package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.components.*;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PWPInventoryScreen extends Screen {

    private int selectedCategory;
    private PWPTabs categoryTabs;

    private static final List<String> CATEGORIES = List.of("Все", "Основное", "Втор.", "Нож", "Снаряжение");

    public PWPInventoryScreen() {
        super(Component.literal("ИНВЕНТАРЬ"));
    }

    @Override
    protected void init() {
        super.init();
        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);

        categoryTabs = new PWPTabs(cx, 20, contentW, selectedCategory, idx -> selectedCategory = idx);
        categoryTabs.setTabs(CATEGORIES);
        categoryTabs.getWidgets().forEach(this::addRenderableWidget);

        addRenderableWidget(new PWPButton(
            PWPLayout.centerX(width, 100), height - 30, 100, 22,
            Component.literal("Назад"),
            btn -> onClose(),
            PWPButton.Style.GHOST
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        PWPLayout.renderHeader(gui, "ИНВЕНТАРЬ", width);
        if (categoryTabs != null) categoryTabs.render(gui, mouseX, mouseY, partialTick);

        int panelY = 42;
        int panelH = height - panelY - 36;
        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);

        PWPPanel.render(gui, cx, panelY, contentW, panelH);

        gui.drawCenteredString(font, Component.literal("Инвентарь (заглушка)"), width / 2, panelY + panelH / 2 - 4,
            PWPTheme.Colors.TEXT_DIM);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

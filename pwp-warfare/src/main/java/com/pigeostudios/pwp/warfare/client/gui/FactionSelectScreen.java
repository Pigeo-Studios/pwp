package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FactionSelectScreen extends Screen {
    private static final int CARD_W = 130;
    private static final int CARD_H = 50;
    private static final int GAP = 10;

    private List<String> factions = new ArrayList<>();
    private boolean loading = true;
    private int cols;
    private int panelW;
    private int panelH;

    public FactionSelectScreen() {
        super(Component.translatable("gui.pwpwarfare.faction_select.title"));
    }

    @Override
    protected void init() {
        cols = Math.max(1, Math.min(4, (width - 40) / (CARD_W + GAP)));
        panelW = cols * CARD_W + (cols - 1) * GAP + 20;
        panelW = Math.min(panelW, width - 20);
        int rows = Math.max(1, (factions.size() + cols - 1) / cols);
        panelH = Math.min(height - 60, rows * CARD_H + (rows - 1) * GAP + 56);

        loadFactions();
    }

    private void loadFactions() {
        new Thread(() -> {
            try {
                JsonObject result = CoreAPI.getFactions();
                List<String> loaded = new ArrayList<>();
                if (result != null && result.has("data")) {
                    JsonArray arr = result.get("data").getAsJsonArray();
                    for (JsonElement e : arr) {
                        loaded.add(e.getAsString());
                    }
                }
                if (loaded.isEmpty()) {
                    loaded.add("ukraine");
                    loaded.add("russia");
                    loaded.add("usa");
                }
                List<String> finalLoaded = loaded;
                Minecraft.getInstance().submit(() -> {
                    factions = finalLoaded;
                    loading = false;
                    recreateWidgets();
                });
            } catch (Exception ex) {
                Minecraft.getInstance().submit(() -> {
                    factions = new ArrayList<>();
                    factions.add("ukraine");
                    factions.add("russia");
                    loading = false;
                    recreateWidgets();
                });
            }
        }, "PWP-Faction-Load").start();
    }

    private void recreateWidgets() {
        clearWidgets();
        int cx = (width - panelW) / 2;
        int py = 10 + 34;

        for (int i = 0; i < factions.size(); i++) {
            String faction = factions.get(i);
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 10 + c * (CARD_W + GAP);
            int by = py + r * (CARD_H + GAP);

            addRenderableWidget(
                new PWPButton(bx, by, CARD_W, CARD_H,
                    Component.literal(faction.toUpperCase()),
                    b -> minecraft.setScreen(new FactionKitListScreen(faction)),
                    PWPButton.Style.PRIMARY
                )
            );
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int cx = (width - panelW) / 2;
        int py = 10;

        PWPPanel.render(gui, cx, py, panelW, panelH);
        gui.drawCenteredString(font, Component.literal(title.getString()), width / 2, py + 10, PWPTheme.Colors.TEXT_ACCENT);
        gui.fill(cx + 10, py + 22, cx + panelW - 10, py + 23, PWPTheme.Colors.ACCENT);

        if (loading) {
            gui.drawCenteredString(font, Component.translatable("gui.pwpwarfare.faction_select.loading"), width / 2, height / 2, PWPTheme.Colors.TEXT_DIM);
        }

        super.render(gui, mx, my, pt);
    }
}

package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DeployMapScreen extends Screen {

    private final DeployScreen parent;
    private final WarfareMapRenderer mapRenderer = new WarfareMapRenderer();

    public DeployMapScreen(DeployScreen parent) {
        super(Component.literal("MAP"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int mapSize = Math.min(width - 16, height - 60);
        int mapX = (width - mapSize) / 2;
        int mapY = 35;
        mapRenderer.init(mapX, mapY, mapSize);
        mapRenderer.centerOnPlayer();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        gui.fill(0, 0, width, height, 0xFF0A0C0E);
        var f = PWPTheme.Fonts.display();

        gui.fill(0, 0, width, 30, 0xE60E1117);
        gui.drawCenteredString(f, "TACTICAL MAP  —  " + DeployData.mapName, width / 2, 9,
            PWPTheme.Colors.TEXT_ACCENT);
        gui.drawString(f, "[M] Close  |  Click spawn to select",
            width - f.width("[M] Close  |  Click spawn to select") - 12, 9,
            PWPTheme.Colors.TEXT_SECONDARY, false);

        mapRenderer.render(gui, mx, my, pt);

        String hint = "Drag to pan  \u2022  Scroll to zoom  \u2022  Click spawn";
        gui.drawString(f, hint, width / 2 - f.width(hint) / 2, height - 12, PWPTheme.Colors.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mapRenderer.mouseClicked(mx, my, btn)) {
            if (parent != null) {
                parent.selectedSpawn = mapRenderer.selectedSpawnId;
            }
            if (parent != null) minecraft.setScreen(parent);
            return true;
        }
        if (btn == 0) {
            mapRenderer.mouseClicked(mx, my, btn);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (mapRenderer.mouseDragged(mx, my, btn, dx, dy)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        mapRenderer.mouseReleased(btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mapRenderer.mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 50 || key == 256) {
            if (parent != null) minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override public boolean isPauseScreen() { return false; }
}

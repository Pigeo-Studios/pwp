package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
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

        // Update map renderer size for zoom to work
        int mapSize = Math.min(width - 16, height - 60);
        int mapX = (width - mapSize) / 2;
        int mapY = 35;
        if (mapRenderer.mapSize != mapSize || mapRenderer.mapX != mapX) {
            mapRenderer.init(mapX, mapY, mapSize);
        }

        gui.fill(0, 0, width, 30, 0xE60E1117);
        gui.drawCenteredString(f, "TACTICAL MAP  —  " + DeployData.mapName, width / 2, 9,
            PWPTheme.Colors.TEXT_ACCENT);
        gui.drawString(f, "[M] Close  |  Click spawn to select",
            width - f.width("[M] Close  |  Click spawn to select") - 12, 9,
            PWPTheme.Colors.TEXT_SECONDARY, false);

        mapRenderer.render(gui, mx, my, pt);

        String hint = "Drag to pan  \u2022  Scroll to zoom  \u2022  Click spawn to select";
        gui.drawString(f, hint, width / 2 - f.width(hint) / 2, height - 12, PWPTheme.Colors.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            String spawnId = getSpawnAt(mx, my);
            if (spawnId != null) {
                if (parent != null) parent.selectedSpawn = spawnId;
                if (parent != null) minecraft.setScreen(parent);
                return true;
            }
            mapRenderer.mouseClicked(mx, my, btn);
            return true;
        }
        if (btn == 1) {
            mapRenderer.mouseClicked(mx, my, btn);
        }
        return false;
    }

    private String getSpawnAt(double mx, double my) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mapRenderer.isMouseOver(mx, my)) return null;
        double bpp = mapRenderer.getBlocksPerPixel();
        double cx = mapRenderer.getCenterX(mc.player);
        double cz = mapRenderer.getCenterZ(mc.player);
        int mapCX = mapRenderer.mapX + mapRenderer.mapSize / 2;
        int mapCY = mapRenderer.mapY + mapRenderer.mapSize / 2;

        String closestId = null;
        double closestDist = 15;
        for (DeployData.SpawnPoint sp : DeployData.spawns) {
            double sx = mapCX + (sp.pos().getX() - cx) / bpp;
            double sy = mapCY + (sp.pos().getZ() - cz) / bpp;
            double dist = Math.sqrt((mx - sx) * (mx - sx) + (my - sy) * (my - sy));
            boolean blocked = sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
            if (dist < closestDist && !blocked) {
                closestDist = dist;
                closestId = sp.id();
            }
        }
        return closestId;
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

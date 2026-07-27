package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DeployMapScreen extends Screen {

    private final DeployScreen parent;
    private final WarfareMapRenderer map = new WarfareMapRenderer();

    public DeployMapScreen(DeployScreen parent) {
        super(Component.literal("КАРТА"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int sz = Math.min(width - 10, height - 10);
        map.init((width - sz) / 2, (height - sz) / 2, sz);
        map.centerOnPlayer();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        gui.fill(0, 0, width, height, 0xFF0A0C0E);
        var f = PWPTheme.Fonts.display();

        map.render(gui, mx, my, pt);

        String hint = "Drag — \u2022 Scroll — \u2022 Click spawn \u2022 M/ESC —";
        int hw = f.width(hint);
        gui.fill(width/2 - hw/2 - 8, height-14, width/2 + hw/2 + 8, height, 0xCC000000);
        gui.drawString(f, hint, width/2 - hw/2, height-11, PWPTheme.Colors.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            String sid = getSpawnAt(mx, my);
            if (sid != null) {
                if (parent != null) parent.selectedSpawn = sid;
                if (minecraft != null && parent != null) minecraft.setScreen(parent);
                return true;
            }
            map.mouseClicked(mx, my, btn);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        map.mouseDragged(mx, my, btn, dx, dy);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        map.mouseReleased(btn);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        map.mouseScrolled(mx, my, delta);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 50 || key == 256) {
            if (parent != null) minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    private String getSpawnAt(double mx, double my) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !map.isMouseOver(mx, my)) return null;
        double bpp = map.getBlocksPerPixel();
        double cx = map.getCenterX(mc.player);
        double cz = map.getCenterZ(mc.player);
        int mcx = map.mapX + map.mapSize/2;
        int mcy = map.mapY + map.mapSize/2;

        String best = null;
        double bestD = 18;
        for (var sp : DeployData.spawns) {
            double sx = mcx + (sp.pos().getX()-cx)/bpp;
            double sy = mcy + (sp.pos().getZ()-cz)/bpp;
            double d = Math.sqrt((mx-sx)*(mx-sx)+(my-sy)*(my-sy));
            boolean blocked = sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
            if (d < bestD && !blocked) { bestD = d; best = sp.id(); }
        }
        return best;
    }

    @Override public boolean isPauseScreen() { return false; }
}

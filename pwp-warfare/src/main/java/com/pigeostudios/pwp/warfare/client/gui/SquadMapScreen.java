package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketRemoveMarker;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class SquadMapScreen extends Screen {

    private final SquadMapRenderer map = new SquadMapRenderer();
    private final SquadContextMenu ctx = new SquadContextMenu();
    private int mX, mY, mS;
    private boolean pathActive;
    private List<List<PathPoint>> activeGroups;

    public SquadMapScreen() { super(Component.literal("Map")); }

    @Override
    protected void init() {
        super.init();
        mS = Math.min(width - 8, height - 36);
        mX = (width - mS) / 2;
        mY = (height - mS) / 2 + 14;
        map.init(mX, mY, mS);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xCC06080A);
        map.render(g, mx, my, pt);
        if (ctx.visible) ctx.render(g, mx, my);
        if (pathActive) g.drawString(font, "LMB / Enter: place | ESC: cancel", 10, height - 14, 0x44FF44, true);
        String hint = "LMB: drag/spawn | RMB: marker | Scroll: zoom | ESC: back";
        g.drawString(font, hint, width / 2 - font.width(hint) / 2, height - 14, 0x888888, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (pathActive && map.previewStart != null && map.inMap(mx, my)) {
            map.previewEnd = new PathPoint(toWorldX(mx), toWorldZ(my));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (ctx.visible) return ctx.mouseClicked(mx, my, btn);
        if (!map.isMouseOver(mx, my)) return super.mouseClicked(mx, my, btn);

        int wx = toWorldX(mx), wz = toWorldZ(my);

        if (pathActive) {
            if (map.previewStart != null && map.previewEnd != null && !activeGroups.isEmpty()) {
                activeGroups.get(activeGroups.size() - 1).add(new PathPoint(wx, wz));
            }
            map.previewStart = null; map.previewEnd = null; pathActive = false;
            return true;
        }

        if (btn == 0) {
            String sid = getSpawnAt(mx, my);
            if (sid != null) { onDeploy(sid); return true; }
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) {
                if (map.hitPath((int)mx, (int)my, map.getCenterX(p), map.getCenterZ(p))) return true;
                MapMarker hit = map.hitMarker((int)mx, (int)my, map.getCenterX(p), map.getCenterZ(p));
                if (hit != null) { PacketHandler.INSTANCE.sendToServer(new PacketRemoveMarker(hit.id)); return true; }
            }
            return map.mouseClicked(mx, my, btn);
        }

        if (btn == 1) {
            ctx.open((int)mx, (int)my, (cat, icon) -> {
                int wx2 = toWorldX(mx), wz2 = toWorldZ(my);
                if ("arrow".equals(icon)) {
                    if ("squad".equals(cat)) startPath(wx2, wz2, map.pathGroups);
                    else if ("enemy".equals(cat)) startPath(wx2, wz2, map.pathGroupsRed);
                    else startPath(wx2, wz2, map.pathGroupsYellow);
                    ctx.close();
                } else {
                    place(cat, icon, wx2, wz2);
                }
            });
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        return !ctx.visible && !pathActive && map.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        map.mouseReleased(btn);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        return !ctx.visible && map.isMouseOver(mx, my) && map.mouseScrolled(mx, my, d);
    }

    @Override
    public boolean keyPressed(int k, int sc, int mod) {
        if (k == 256) {
            if (ctx.visible) { ctx.close(); return true; }
            if (pathActive) {
                if (!activeGroups.isEmpty()) activeGroups.get(activeGroups.size() - 1).clear();
                map.previewStart = null; map.previewEnd = null; pathActive = false;
                return true;
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if ((k == 257 || k == 335) && pathActive) {
            if (map.previewStart != null && map.previewEnd != null && !activeGroups.isEmpty()) {
                activeGroups.get(activeGroups.size() - 1).add(new PathPoint(map.previewEnd.x, map.previewEnd.z));
            }
            map.previewStart = null; map.previewEnd = null; pathActive = false;
            return true;
        }
        return super.keyPressed(k, sc, mod);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void place(String cat, String icon, int wx, int wz) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        String team = "enemy".equals(cat) ? "enemy" : "team".equals(cat) ? "team" : "squad";
        PacketHandler.INSTANCE.sendToServer(new PacketPlaceMarker(team, cat, icon, new BlockPos(wx, 64, wz)));
    }

    private void startPath(int wx, int wz, List<List<PathPoint>> groups) {
        List<PathPoint> sub = new java.util.ArrayList<>();
        sub.add(new PathPoint(wx, wz));
        groups.add(sub); activeGroups = groups;
        map.previewStart = new PathPoint(wx, wz); map.previewEnd = null;
        pathActive = true;
    }

    private String getSpawnAt(double mx, double my) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !map.isMouseOver(mx, my)) return null;
        double bpp = map.getBlocksPerPixel();
        double cx = map.getCenterX(mc.player);
        double cz = map.getCenterZ(mc.player);
        String best = null;
        double bestD = 18;
        for (var sp : DeployData.spawns) {
            double sx = map.mapX + map.mapSize / 2.0 + (sp.pos().getX() - cx) / bpp;
            double sy = map.mapY + map.mapSize / 2.0 + (sp.pos().getZ() - cz) / bpp;
            double d = Math.sqrt((mx - sx) * (mx - sx) + (my - sy) * (my - sy));
            boolean blocked = sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
            if (d < bestD && !blocked) { bestD = d; best = sp.id(); }
        }
        return best;
    }

    private void onDeploy(String spawnId) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof com.pigeostudios.pwp.warfare.client.gui.DeployScreen) {
            ((com.pigeostudios.pwp.warfare.client.gui.DeployScreen)screen).selectedSpawn = spawnId;
        }
        Minecraft.getInstance().setScreen(screen);
    }

    private int toWorldX(double mx) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(map.getCenterX(p) + (mx - (mX + mS / 2.0)) * map.getBlocksPerPixel());
    }

    private int toWorldZ(double my) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(map.getCenterZ(p) + (my - (mY + mS / 2.0)) * map.getBlocksPerPixel());
    }
}

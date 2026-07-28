package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketRemoveMarker;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SquadMapScreen extends Screen {

    private static final int LEFT_PANEL_W = 180;
    private static final int RIGHT_PANEL_W = 155;
    private static final int TOP_BAR_H = 32;
    private static final int BOTTOM_BAR_H = 18;

    private final SquadMapRenderer map = new SquadMapRenderer();
    private final SquadContextMenu ctx = new SquadContextMenu();
    private final PWPContextMenu contextMenu = new PWPContextMenu();
    private final Set<Integer> expandedSquads = new HashSet<>();
    private final Screen parent;
    private int mX, mY, mMapW, mMapH;
    private boolean pathActive;
    private List<List<PathPoint>> activeGroups;
    private PWPButton applyCmdBtn;
    private PWPButton createSquadBtn;
    private EditBox squadInput;
    private int mySquadId = -1;

    private static final ResourceLocation TICKET_ICON = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");

    public SquadMapScreen() { this(null); }

    public SquadMapScreen(Screen parent) {
        super(Component.literal("Map"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int availW = width - LEFT_PANEL_W - RIGHT_PANEL_W;
        int availH = height - TOP_BAR_H - BOTTOM_BAR_H;
        mMapW = availW - 4;
        mMapH = availH - 4;
        mX = LEFT_PANEL_W + 2;
        mY = TOP_BAR_H + 2;
        map.init(mX, mY, mMapW, mMapH);

        squadInput = addRenderableWidget(new EditBox(PWPTheme.Fonts.display(), 0, 0, 90, 16, Component.literal("")));
        squadInput.setMaxLength(12);
        squadInput.setVisible(false);

        createSquadBtn = addRenderableWidget(new PWPButton(0, 0, 50, 18,
            Component.literal("Создать"), b -> {
                String n = squadInput.getValue().trim();
                if (!n.isEmpty()) {
                    PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, n));
                    squadInput.setValue("");
                }
            }, PWPButton.Style.DARK));
        createSquadBtn.visible = false;

        applyCmdBtn = addRenderableWidget(new PWPButton(4, 2, LEFT_PANEL_W - 8, 16,
            Component.literal("Стать командиром"),
            b -> {
                PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD());
                b.visible = false;
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("Запрос отправлен командирам взводов").withStyle(ChatFormatting.GREEN), true);
            },
            PWPButton.Style.DARK));
        applyCmdBtn.visible = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (applyCmdBtn != null) applyCmdBtn.visible = SquadUIHelper.isApplyCmdVisible();
        mySquadId = -1;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            String n = p.getScoreboardName();
            for (var sq : ClientData.clientSquads) {
                if (sq.members.contains(n)) { mySquadId = sq.id; break; }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xCC06080A);
        renderLeftPanel(g, mx, my);
        renderTopBar(g, mx, my);
        map.render(g, mx, my, pt);
        renderRightPanel(g, mx, my);
        renderBottomBar(g);
        if (ctx.visible) ctx.render(g, mx, my);
        if (contextMenu.isVisible()) contextMenu.render(g, mx, my);
        if (pathActive)
            g.drawString(font, "LMB / Enter: place | ESC: cancel", 10, height - 14, 0x44FF44, true);
        super.render(g, mx, my, pt);
    }

    private void renderLeftPanel(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, LEFT_PANEL_W, height, 0xCC0A0C10);
        g.vLine(LEFT_PANEL_W - 1, 0, height, 0xFF444444);

        int listTop = 2;
        if (applyCmdBtn != null && applyCmdBtn.visible) listTop = 20;
        int listBottom = height - 30;

        g.enableScissor(0, listTop, LEFT_PANEL_W, listBottom);
        SquadUIHelper.renderSquadList(g, mx, my, expandedSquads, SquadUIHelper.isApplyCmdVisible());
        g.disableScissor();

        if (!SquadUIHelper.isPlayerInSquad()) {
            int by = height - 28;
            g.drawString(font, Component.literal("СОЗДАТЬ ОТРЯД"), 4, by, PWPTheme.Colors.ACCENT, false);
            squadInput.setX(4); squadInput.setWidth(110); squadInput.setY(by + 12); squadInput.setVisible(true);
            createSquadBtn.setX(120); createSquadBtn.setY(by + 11); createSquadBtn.visible = true;
        } else {
            squadInput.setVisible(false);
            createSquadBtn.visible = false;
        }
    }

    private void renderTopBar(GuiGraphics g, int mx, int my) {
        int barX = LEFT_PANEL_W;
        int barW = width - LEFT_PANEL_W - RIGHT_PANEL_W;
        g.fill(barX, 0, barX + barW, TOP_BAR_H, 0xCC0A0C10);
        g.hLine(barX, barX + barW - 1, TOP_BAR_H - 1, 0xFF444444);

        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;

        String kp = SquadMapRenderer.getKP(p.getX(), p.getZ());
        g.drawString(font, kp, barX + 6, TOP_BAR_H / 2 - 4, 0xFFAAAAAA, false);

        String team = SquadUIHelper.getPlayerTeam().toUpperCase();
        int tickets = team.contains("BLUE") ? ClientData.BLUE_TICKETS : ClientData.RED_TICKETS;
        String faction = team.contains("BLUE") ? ClientData.BLUE_FACTION : ClientData.RED_FACTION;
        ResourceLocation flagTex = faction != null && !faction.equalsIgnoreCase("none")
            ? new ResourceLocation("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png") : null;

        int flagW = 22, flagH = 13;
        String ticketStr = String.valueOf(tickets);
        int ticketW = font.width(ticketStr);
        int ctrX = barX + barW / 2 - (flagW + 6 + 12 + 6 + ticketW + 20 + font.width("00:00")) / 2;

        if (flagTex != null) g.blit(flagTex, ctrX, TOP_BAR_H / 2 - flagH / 2, flagW, flagH, 0, 0, 64, 36, 64, 36);
        RenderSystem.enableBlend();
        g.blit(TICKET_ICON, ctrX + flagW + 6, TOP_BAR_H / 2 - 6, 12, 12, 0, 0, 16, 16, 16, 16);
        g.drawString(font, ticketStr, ctrX + flagW + 6 + 12 + 6, TOP_BAR_H / 2 - 4, 0xFFFFFF, true);

        long elapsed = ClientData.matchStartTime > 0 ? (System.currentTimeMillis() - ClientData.matchStartTime) / 1000 : 0;
        int mins = (int)(elapsed / 60);
        int secs = (int)(elapsed % 60);
        String timer = String.format("%d:%02d", mins, secs);
        g.drawString(font, timer, ctrX + flagW + 6 + 12 + 6 + ticketW + 20, TOP_BAR_H / 2 - 4, 0xFFCCCCCC, false);
    }

    private void renderRightPanel(GuiGraphics g, int mx, int my) {
        int rightX = width - RIGHT_PANEL_W;
        g.fill(rightX, TOP_BAR_H, width, height - BOTTOM_BAR_H, 0xCC0A0C10);
        g.vLine(rightX, TOP_BAR_H, height - BOTTOM_BAR_H, 0xFF444444);
        map.renderVehicleLegend(g, rightX + 4, TOP_BAR_H + 2, RIGHT_PANEL_W - 8, mx, my);
    }

    private void renderBottomBar(GuiGraphics g) {
        int barX = LEFT_PANEL_W;
        int barY = height - BOTTOM_BAR_H;
        int barW = width - LEFT_PANEL_W - RIGHT_PANEL_W;
        g.fill(barX, barY, barX + barW, height, 0xCC0A0C10);
        g.hLine(barX, barX + barW - 1, barY, 0xFF444444);

        double bpp = ClientData.mapScale;
        int scaleDist = bpp < 1 ? 300 : bpp < 2.5 ? 200 : 100;
        int scalePx = (int)(scaleDist / bpp);
        int scY = barY + 4;
        int scX = barX + 10;
        g.fill(scX, scY, scX + scalePx, scY + 2, 0xFFFFFFFF);
        g.fill(scX, scY, scX + 1, scY + 6, 0xFFFFFFFF);
        g.fill(scX + scalePx, scY, scX + scalePx + 1, scY + 6, 0xFFFFFFFF);
        String scaleTxt = scaleDist + "m";
        g.drawString(font, scaleTxt, scX + scalePx / 2 - font.width(scaleTxt) / 2, scY + 4, 0xFFCCCCCC, false);

        String mapInfo = ClientData.currentMapImage + " | " + ClientData.gameMode.toUpperCase();
        g.drawString(font, mapInfo, barX + barW - font.width(mapInfo) - 10, scY, 0xFF888888, false);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (pathActive && map.previewStart != null && map.inMap(mx, my))
            map.previewEnd = new PathPoint(toWorldX(mx), toWorldZ(my));
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (ctx.visible) return ctx.mouseClicked(mx, my, btn);
        if (contextMenu.isVisible()) { contextMenu.mouseClicked(mx, my, btn); return true; }

        if (btn == 0 && mx < LEFT_PANEL_W) {
            if (applyCmdBtn != null && applyCmdBtn.visible && applyCmdBtn.mouseClicked(mx, my, btn)) return true;
            if (!SquadUIHelper.isPlayerInSquad()) {
                if (createSquadBtn != null && createSquadBtn.visible && createSquadBtn.mouseClicked(mx, my, btn)) return true;
                if (squadInput != null && squadInput.isVisible()) {
                    squadInput.setFocused(squadInput.isMouseOver(mx, my));
                }
            }
            SquadUIHelper.handleSquadClick(mx, my, expandedSquads, contextMenu, SquadUIHelper.isApplyCmdVisible());
            return true;
        }

        if (!map.isMouseOver(mx, my)) return super.mouseClicked(mx, my, btn);
        int wx = toWorldX(mx), wz = toWorldZ(my);

        if (pathActive) {
            if (map.previewStart != null && map.previewEnd != null && !activeGroups.isEmpty())
                activeGroups.get(activeGroups.size() - 1).add(new PathPoint(wx, wz));
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
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null && !SquadUIHelper.isSquadLeaderOrFTL(p)) return true;
            ctx.open((int)mx, (int)my, (cat, icon) -> {
                int wx2 = toWorldX(mx), wz2 = toWorldZ(my);
                if ("arrow".equals(icon)) {
                    if ("squad".equals(cat)) startPath(wx2, wz2, map.pathGroups());
                    else if ("enemy".equals(cat)) startPath(wx2, wz2, map.pathGroupsRed());
                    else startPath(wx2, wz2, map.pathGroupsYellow());
                    ctx.close();
                } else place(cat, icon, wx2, wz2);
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
        if (k == 256 || k == 50) {
            if (ctx.visible) { ctx.close(); return true; }
            if (contextMenu.isVisible()) { contextMenu.hide(); return true; }
            if (pathActive) {
                if (!activeGroups.isEmpty()) activeGroups.get(activeGroups.size() - 1).clear();
                map.previewStart = null; map.previewEnd = null; pathActive = false;
                return true;
            }
            if (parent != null) Minecraft.getInstance().setScreen(parent);
            else Minecraft.getInstance().setScreen(null);
            return true;
        }
        if ((k == 257 || k == 335) && pathActive) {
            if (map.previewStart != null && map.previewEnd != null && !activeGroups.isEmpty())
                activeGroups.get(activeGroups.size() - 1).add(new PathPoint(map.previewEnd.x, map.previewEnd.z));
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
        PacketHandler.INSTANCE.sendToServer(new PacketPlaceMarker(
            "enemy".equals(cat) ? "enemy" : "team".equals(cat) ? "team" : "squad",
            cat, icon, new BlockPos(wx, 64, wz)));
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
            double sx = mX + mMapW / 2.0 + (sp.pos().getX() - cx) / bpp;
            double sy = mY + mMapH / 2.0 + (sp.pos().getZ() - cz) / bpp;
            double d = Math.sqrt((mx - sx) * (mx - sx) + (my - sy) * (my - sy));
            if (d < bestD && sp.status() != DeployData.SpawnStatus.BLOCKED && sp.status() != DeployData.SpawnStatus.DESTROYED) {
                bestD = d; best = sp.id();
            }
        }
        return best;
    }

    private void onDeploy(String spawnId) {
        map.selectedSpawnId = spawnId;
        if (parent instanceof com.pigeostudios.pwp.warfare.client.gui.DeployScreen)
            ((com.pigeostudios.pwp.warfare.client.gui.DeployScreen)parent).selectedSpawn = spawnId;
        Minecraft.getInstance().setScreen(parent);
    }

    private int toWorldX(double mx) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(map.getCenterX(p) + (mx - (mX + mMapW / 2.0)) * map.getBlocksPerPixel());
    }

    private int toWorldZ(double my) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(map.getCenterZ(p) + (my - (mY + mMapH / 2.0)) * map.getBlocksPerPixel());
    }
}

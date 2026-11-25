package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.TeamRoster;
import com.pigeostudios.pwp.warfare.client.gui.map.VehicleLegend;
import com.pigeostudios.pwp.warfare.client.gui.ui.SquadList;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketPlacePath;
import com.pigeostudios.pwp.warfare.network.PacketRemoveMarker;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.squad.SquadPackets;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pwp.api.util.Parse;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
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

    /** Геометрия экрана: панели и область карты (одна функция для рисования и кликов). */
    private record Layout(Rect top, Rect bottom, Rect left, Rect squadList, Rect createRow, Rect right, Rect map) {}

    private final SquadMapRenderer map = new SquadMapRenderer();
    private final VehicleLegend legend = new VehicleLegend();
    private final SquadContextMenu ctx = new SquadContextMenu();
    private final PWPContextMenu contextMenu = new PWPContextMenu();
    private final Set<Integer> expandedSquads = new HashSet<>();
    private final Screen parent;
    private int mX, mY, mMapW, mMapH;
    private boolean pathActive;
    private List<List<PathPoint>> activeGroups;
    private int activeSquadId = -1;
    private int activeSquadKey = -1;
    private int pathCounter;
    private PWPButton applyCmdBtn;
    private PWPButton createSquadBtn;
    private EditBox squadInput;
    private int mySquadId = -1;

    private static final ResourceLocation TICKET_ICON =
            new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");

    public SquadMapScreen() {
        this(null);
    }

    public SquadMapScreen(Screen parent) {
        super(Component.translatable("gui.pwpwarfare.map.title"));
        this.parent = parent;
    }

    /** Последний зум карты M (блоков на пиксель) — переживает закрытие экрана; 0 — «вся карта». */
    private static double savedScale = 0.0;

    /**
     * Свой зум, как у карты деплоя: колесо приближает к курсору глубже «всей карты». Нижняя граница —
     * {@link WTheme#DEPLOY_MAP_MIN_BPP}, верхняя — вся карта с запасом. Повторный init (ресайз) зум не
     * сбрасывает; центрирование на игроке — только при первом открытии.
     */
    private void initZoom() {
        double size = ClientData.mapSizeBlocks;
        double fit = size > 0 ? Math.max(size / Math.max(1, mMapW), size / Math.max(1, mMapH)) : 4.0;
        boolean first = map.localScale <= 0;
        map.fitScale = 0;
        map.localMin = WTheme.DEPLOY_MAP_MIN_BPP;
        map.localMax = Math.max(fit * WTheme.DEPLOY_MAP_FIT_SLACK, WTheme.DEPLOY_MAP_MIN_BPP * 2);
        double want = first ? (savedScale > 0 ? savedScale : fit) : map.localScale;
        map.localScale = Math.max(map.localMin, Math.min(map.localMax, want));
        if (first) map.centerOnPlayer();
    }

    @Override
    public void removed() {
        savedScale = map.localScale;
        super.removed();
    }

    @Override
    protected void init() {
        super.init();
        Rect mapRect = layout().map();
        mX = mapRect.x();
        mY = mapRect.y();
        mMapW = mapRect.w();
        mMapH = mapRect.h();
        map.init(mX, mY, mMapW, mMapH);
        initZoom();

        squadInput = addRenderableWidget(new EditBox(font, 0, 0, 90, WTheme.FIELD_H, Component.empty()));
        squadInput.setMaxLength(12);
        squadInput.setBordered(false);
        squadInput.setHint(Component.translatable("gui.pwpwarfare.deploy.squad.name_hint"));
        squadInput.setVisible(false);

        createSquadBtn = addRenderableWidget(new PWPButton(
                0,
                0,
                50,
                18,
                Component.translatable("gui.pwpwarfare.deploy.squad.create"),
                b -> {
                    String n = squadInput.getValue().trim();
                    if (!n.isEmpty()) {
                        PacketHandler.INSTANCE.sendToServer(new SquadPackets.Create(n));
                        squadInput.setValue("");
                    }
                },
                PWPButton.Style.DARK));
        createSquadBtn.visible = false;

        applyCmdBtn = addRenderableWidget(new PWPButton(
                4,
                2,
                Spacing.BUTTON_WIDTH,
                16,
                Component.translatable("gui.pwpwarfare.deploy.squad.apply_cmd"),
                b -> {
                    PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD());
                    b.visible = false;
                    Minecraft.getInstance()
                            .player
                            .displayClientMessage(
                                    Component.translatable("gui.pwpwarfare.deploy.squad.apply_cmd_sent")
                                            .withStyle(ChatFormatting.GREEN),
                                    true);
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
                if (sq.members.contains(n)) {
                    mySquadId = sq.id;
                    break;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Layout l = layout();
        UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        renderTopBar(g, l);
        renderLeftPanel(g, l, mx, my);
        map.tickAnim();
        map.render(g, mx, my, pt);
        renderRightPanel(g, l, mx, my);
        renderBottomBar(g, l);
        if (ctx.visible) ctx.render(g, mx, my);
        if (contextMenu.isVisible()) contextMenu.render(g, mx, my);
        if (pathActive) {
            Rect hint =
                    l.map().bottomStrip(font.lineHeight + Spacing.SM).inset(Spacing.XXL, 0, Spacing.XXL, Spacing.XS);
            UIDraw.rect(g, hint, PWPTheme.Styles.Tooltip.BG);
            WDraw.text(
                    g,
                    font,
                    Component.translatable("gui.pwpwarfare.deploy.map.path_hint"),
                    hint,
                    Align.CENTER,
                    WTheme.STATUS_OK,
                    true,
                    1F);
        }
        super.render(g, mx, my, pt);
    }

    private Layout layout() {
        Fit.Density d = Fit.Density.of(width, height);
        int margin = Fit.fraction(width, WTheme.MARGIN_FRACTION, WTheme.MARGIN_MIN, WTheme.MARGIN_MAX);
        int topH = d.pick(Spacing.TOP_BAR_H_COMPACT, Spacing.TOP_BAR_H, Spacing.TOP_BAR_H_LARGE);
        int botH = d.pick(Spacing.FOOTER_H_COMPACT, Spacing.FOOTER_H, Spacing.FOOTER_H);
        Rect screen = Rect.screen(width, height);
        Rect top = screen.top(topH);
        Rect bottom = screen.bottomStrip(botH);
        Rect body = Rect.of(
                margin, top.bottom() + WTheme.GAP, width - 2 * margin, bottom.y() - top.bottom() - 2 * WTheme.GAP);
        int leftW = Fit.fraction(width, WTheme.MAP_LEFT_FRACTION, WTheme.MAP_SIDE_MIN_W, WTheme.MAP_LEFT_MAX_W);
        int rightW = Fit.fraction(width, WTheme.MAP_RIGHT_FRACTION, WTheme.MAP_SIDE_MIN_W, WTheme.MAP_RIGHT_MAX_W);
        Rect[] cols = Flow.row(body, WTheme.GAP, Flow.Justify.START, leftW, Flow.flex(1), rightW);
        int headerH = WDraw.headerHeight(height);
        Rect leftIn = cols[0].inset(Spacing.XS, headerH + Spacing.XS, Spacing.XS, Spacing.XS);
        boolean create = Minecraft.getInstance().player != null && !SquadUIHelper.isPlayerInSquad();
        Rect[] lc = Flow.column(leftIn, Spacing.XS, Flow.Justify.START, Flow.flex(1), create ? WTheme.FIELD_H : 0);
        // карта — квадрат по центру средней колонки
        int sz = Math.max(0, Math.min(cols[1].w(), cols[1].h()));
        Rect mapRect = cols[1].centered(sz, sz);
        return new Layout(top, bottom, cols[0], lc[0], lc[1], cols[2], mapRect);
    }

    private void renderLeftPanel(GuiGraphics g, Layout l, int mx, int my) {
        WDraw.panel(
                g,
                font,
                l.left(),
                WDraw.headerHeight(height),
                WarfareIcon.SQUAD,
                Component.translatable("gui.pwpwarfare.deploy.squads.title"),
                Component.literal(String.valueOf(ClientData.clientSquads.size())),
                1F);
        Rect list = l.squadList();
        boolean applyVisible = SquadUIHelper.isApplyCmdVisible();
        if (applyCmdBtn != null) {
            applyCmdBtn.setX(list.x());
            applyCmdBtn.setY(list.y() + Spacing.XXS);
            applyCmdBtn.setWidth(Math.min(list.w(), Spacing.BUTTON_WIDTH + Spacing.XXL));
        }
        int inset = applyVisible ? applyCmdBtn.getHeight() + 2 * Spacing.XXS : 0;
        g.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        SquadList.render(g, list, inset, mx, my, expandedSquads);
        g.disableScissor();

        Rect row = l.createRow();
        if (row.h() > 0) {
            int bw = Math.min(createSquadBtn.getWidth(), row.w() / 2);
            Rect[] parts = Flow.row(row, Spacing.XS, Flow.Justify.START, Flow.flex(1), bw);
            WDraw.field(g, parts[0], squadInput.isFocused(), 1F);
            int ty = parts[0].y() + (parts[0].h() - font.lineHeight) / 2 + 1;
            squadInput.setX(parts[0].x() + Spacing.XS);
            squadInput.setY(ty);
            squadInput.setWidth(Math.max(1, parts[0].w() - 2 * Spacing.XS));
            squadInput.setVisible(true);
            createSquadBtn.setX(parts[1].x());
            createSquadBtn.setY(parts[1].y());
            createSquadBtn.setWidth(parts[1].w());
            createSquadBtn.setHeight(parts[1].h());
            createSquadBtn.visible = true;
        } else {
            squadInput.setVisible(false);
            createSquadBtn.visible = false;
        }
    }

    private void renderTopBar(GuiGraphics g, Layout l) {
        Rect top = l.top();
        UIBackdrop.bar(g, top, true);
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        Rect inner = top.inset(l.left().x(), 0, l.left().x(), 0);
        // слева: квадрат игрока
        WDraw.icon(
                g,
                WarfareIcon.TARGET,
                inner.x(),
                inner.centerY() - WTheme.ICON_ROW / 2,
                WTheme.ICON_ROW,
                PWPTheme.Colors.ACCENT);
        WDraw.text(
                g,
                font,
                Component.literal(SquadMapRenderer.getKP(p.getX(), p.getZ())),
                inner.inset(WTheme.ICON_ROW + Spacing.XS, 0, inner.w() / 2, 0),
                Align.START,
                PWPTheme.Text.HEADING,
                true,
                1F);
        // центр: флаг, тикеты, время матча
        String team = SquadUIHelper.getPlayerTeam().toUpperCase();
        boolean blue = team.contains("BLUE");
        int tickets = blue ? ClientData.BLUE_TICKETS : ClientData.RED_TICKETS;
        ResourceLocation flag = TeamRoster.flag(blue ? ClientData.BLUE_FACTION : ClientData.RED_FACTION);
        long elapsed =
                ClientData.matchStartTime > 0 ? (System.currentTimeMillis() - ClientData.matchStartTime) / 1000 : 0;
        Component tk = Component.literal(String.valueOf(tickets));
        Component timer = Component.literal(String.format("%d:%02d", elapsed / 60, elapsed % 60));
        int fw = flag != null ? WTheme.FLAG_W + Spacing.SM : 0;
        int total = fw
                + WTheme.ICON_ROW
                + Spacing.XXS
                + font.width(tk)
                + Spacing.LG
                + WTheme.ICON_ROW
                + Spacing.XXS
                + font.width(timer);
        int x = top.centerX() - total / 2;
        int cy = top.centerY();
        if (flag != null) {
            TeamRoster.drawFlag(g, flag, Rect.of(x, cy - WTheme.FLAG_H / 2, WTheme.FLAG_W, WTheme.FLAG_H));
            x += fw;
        }
        WDraw.icon(g, WarfareIcon.TICKET, x, cy - WTheme.ICON_ROW / 2, WTheme.ICON_ROW, PWPTheme.Colors.TEXT_ACCENT);
        x += WTheme.ICON_ROW + Spacing.XXS;
        UIDraw.text(g, font, tk, x, cy - font.lineHeight / 2 + 1, blue ? WTheme.TEAM_BLUE : WTheme.TEAM_RED, true);
        x += font.width(tk) + Spacing.LG;
        WDraw.icon(g, WarfareIcon.CLOCK, x, cy - WTheme.ICON_ROW / 2, WTheme.ICON_ROW, PWPTheme.Text.MUTED);
        x += WTheme.ICON_ROW + Spacing.XXS;
        UIDraw.text(g, font, timer, x, cy - font.lineHeight / 2 + 1, PWPTheme.Text.BODY, true);
    }

    private void renderRightPanel(GuiGraphics g, Layout l, int mx, int my) {
        Rect c = WDraw.panel(
                g,
                font,
                l.right(),
                WDraw.headerHeight(height),
                WarfareIcon.VEHICLE,
                Component.translatable("gui.pwpwarfare.map.vehicles"),
                null,
                1F);
        if (VehicleLegend.isEmpty()) {
            WDraw.emptyState(
                    g,
                    font,
                    c,
                    WarfareIcon.VEHICLE,
                    Component.translatable("gui.pwpwarfare.map.no_vehicles"),
                    null,
                    1F);
            return;
        }
        g.enableScissor(c.x(), c.y(), c.right(), c.bottom());
        UITooltip.Content tip = legend.render(g, c, mx, my);
        g.disableScissor();
        if (tip != null) UITooltip.render(g, tip, mx, my, width, height, 1F);
    }

    private void renderBottomBar(GuiGraphics g, Layout l) {
        Rect bar = l.bottom();
        UIBackdrop.bar(g, bar, false);
        String mapInfo = ClientData.currentMapImage + "  ·  " + ClientData.gameMode.toUpperCase();
        Rect inner = bar.inset(l.left().x(), 0, l.left().x(), 0);
        WDraw.text(g, font, Component.literal(mapInfo), inner, Align.END, PWPTheme.Text.MUTED, false, 1F);
        WDraw.text(
                g,
                font,
                Component.translatable("gui.pwpwarfare.map.controls"),
                inner.withWidth(inner.w() / 2),
                Align.START,
                PWPTheme.Text.CAPTION,
                false,
                1F);
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
        if (contextMenu.isVisible()) {
            contextMenu.mouseClicked(mx, my, btn);
            return true;
        }

        Layout l = layout();
        if (btn == 0 && l.left().contains(mx, my)) {
            if (super.mouseClicked(mx, my, btn)) return true;
            Rect list = l.squadList();
            if (list.contains(mx, my)) {
                int inset = SquadUIHelper.isApplyCmdVisible() ? applyCmdBtn.getHeight() + 2 * Spacing.XXS : 0;
                SquadList.click(list, inset, mx, my, expandedSquads, contextMenu);
            }
            return true;
        }

        if (!map.isMouseOver(mx, my)) return super.mouseClicked(mx, my, btn);
        int wx = toWorldX(mx), wz = toWorldZ(my);

        if (pathActive) {
            if (map.previewStart != null && map.previewEnd != null) {
                if (activeSquadKey >= 0) {
                    var sq = map.pathGroupsSquad().get(activeSquadKey);
                    if (sq != null) sq.add(new PathPoint(wx, wz));
                } else if (activeGroups != null && !activeGroups.isEmpty()) {
                    activeGroups.get(activeGroups.size() - 1).add(new PathPoint(wx, wz));
                }
            }
            map.previewStart = null;
            map.previewEnd = null;
            pathActive = false;
            activeSquadId = -1;
            activeSquadKey = -1;
            return true;
        }

        if (btn == 0) {
            String sid = getSpawnAt(mx, my);
            if (sid != null) {
                onDeploy(sid);
                return true;
            }
            return map.mouseClicked(mx, my, btn);
        }

        if (btn == 1) {
            LocalPlayer p = Minecraft.getInstance().player;
            BiConsumer<String, String> pathCb = (cat, icon) -> {
                int wx2 = toWorldX(mx), wz2 = toWorldZ(my);
                if ("cmd_squads".equals(cat)) {
                    int squadId = Parse.intOr(icon, Integer.MIN_VALUE);
                    if (squadId != Integer.MIN_VALUE) startSquadPath(wx2, wz2, squadId);
                    ctx.close();
                } else if ("arrow".equals(icon) || "player_self".equals(icon)) {
                    if ("team".equals(cat)) startPath(wx2, wz2, map.pathGroups());
                    else if ("enemy".equals(cat)) startPath(wx2, wz2, map.pathGroupsRed());
                    else startPath(wx2, wz2, map.pathGroupsYellow());
                    ctx.close();
                } else place(cat, icon, wx2, wz2);
            };
            if (p != null) {
                MapMarker hit = map.hitMarker((int) mx, (int) my, map.getCenterX(p), map.getCenterZ(p));
                if (hit != null) {
                    Runnable del = () -> PacketHandler.INSTANCE.sendToServer(new PacketRemoveMarker(hit.id));
                    boolean cmd2 = p != null && isPlayerCMD(p);
                    ctx.open((int) mx, (int) my, cmd2, pathCb, del);
                    return true;
                }
            }
            if (p != null && !SquadUIHelper.isSquadLeaderOrFTL(p)) return true;
            boolean cmd = p != null && isPlayerCMD(p);
            ctx.open((int) mx, (int) my, cmd, pathCb);
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
            if (ctx.visible) {
                ctx.close();
                return true;
            }
            if (contextMenu.isVisible()) {
                contextMenu.hide();
                return true;
            }
            if (pathActive) {
                if (activeSquadKey >= 0) {
                    map.pathGroupsSquad().remove(activeSquadKey);
                } else if (activeGroups != null && !activeGroups.isEmpty()) {
                    activeGroups.get(activeGroups.size() - 1).clear();
                }
                map.previewStart = null;
                map.previewEnd = null;
                pathActive = false;
                activeSquadId = -1;
                activeSquadKey = -1;
                return true;
            }
            if (parent != null) Minecraft.getInstance().setScreen(parent);
            else Minecraft.getInstance().setScreen(null);
            return true;
        }
        if ((k == 257 || k == 335) && pathActive) {
            if (map.previewStart != null && map.previewEnd != null) {
                double dx = map.previewEnd.x - map.previewStart.x;
                double dz = map.previewEnd.z - map.previewStart.z;
                double segLen = Math.sqrt(dx * dx + dz * dz);
                if (segLen < SquadMapRenderer.PATH_MIN_LENGTH || segLen > SquadMapRenderer.PATH_MAX_LENGTH) {
                    if (Minecraft.getInstance().player != null)
                        Minecraft.getInstance()
                                .player
                                .displayClientMessage(
                                        Component.translatable(
                                                "gui.pwpwarfare.deploy.map.path_length",
                                                (int) segLen,
                                                (int) SquadMapRenderer.PATH_MIN_LENGTH,
                                                (int) SquadMapRenderer.PATH_MAX_LENGTH),
                                        true);
                    map.previewStart = null;
                    map.previewEnd = null;
                    pathActive = false;
                    activeSquadId = -1;
                    activeSquadKey = -1;
                    return true;
                }
                List<PathPoint> finalPoints;
                String pathType;
                int squadNum = -1;
                if (activeSquadKey >= 0) {
                    var sq = map.pathGroupsSquad().get(activeSquadKey);
                    if (sq == null) {
                        map.previewStart = null;
                        map.previewEnd = null;
                        pathActive = false;
                        activeSquadId = -1;
                        activeSquadKey = -1;
                        return true;
                    }
                    sq.add(new PathPoint(map.previewEnd.x, map.previewEnd.z));
                    finalPoints = new java.util.ArrayList<>(sq);
                    pathType = "cmd_squads";
                    squadNum = activeSquadId;
                } else if (activeGroups != null && !activeGroups.isEmpty()) {
                    var grp = activeGroups.get(activeGroups.size() - 1);
                    grp.add(new PathPoint(map.previewEnd.x, map.previewEnd.z));
                    finalPoints = new java.util.ArrayList<>(grp);
                    if (activeGroups == map.pathGroups()) pathType = "squad";
                    else if (activeGroups == map.pathGroupsRed()) pathType = "enemy";
                    else pathType = "team";
                } else {
                    map.previewStart = null;
                    map.previewEnd = null;
                    pathActive = false;
                    activeSquadId = -1;
                    activeSquadKey = -1;
                    return true;
                }
                // Track creation time for path fade
                long gameTime = Minecraft.getInstance().level != null
                        ? Minecraft.getInstance().level.getGameTime()
                        : 0;
                if (activeSquadKey >= 0) {
                    com.pigeostudios.pwp.warfare.client.PathCache.pathCreatedAt.put(activeSquadKey, gameTime);
                } else if (activeGroups != null && !activeGroups.isEmpty()) {
                    int idx = activeGroups.size() - 1;
                    com.pigeostudios.pwp.warfare.client.PathCache.pathCreatedAt.put(
                            System.identityHashCode(activeGroups.get(idx)), gameTime);
                }
                map.startPathAnim();
                // Sync to server
                LocalPlayer p = Minecraft.getInstance().player;
                String team = p != null && p.getTeam() != null ? p.getTeam().getName() : "";
                java.util.UUID pathId = java.util.UUID.randomUUID();
                PacketHandler.INSTANCE.sendToServer(new PacketPlacePath(pathId, team, pathType, squadNum, finalPoints));
            }
            map.previewStart = null;
            map.previewEnd = null;
            pathActive = false;
            activeSquadId = -1;
            return true;
        }
        return super.keyPressed(k, sc, mod);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void place(String cat, String icon, int wx, int wz) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        String cleanIcon = icon.endsWith("_m") ? icon.substring(0, icon.length() - 2) : icon;
        String type = "squad";
        if ("enemy".equals(cat)) type = "enemy";
        else if ("cmd_top".equals(cat)) type = icon.endsWith("_m") ? "team" : "squad";
        // LEFT (team) = always green
        PacketHandler.INSTANCE.sendToServer(new PacketPlaceMarker(type, cat, cleanIcon, new BlockPos(wx, 64, wz)));
    }

    private void startPath(int wx, int wz, List<List<PathPoint>> groups) {
        List<PathPoint> sub = new java.util.ArrayList<>();
        sub.add(new PathPoint(wx, wz));
        groups.add(sub);
        activeGroups = groups;
        activeSquadId = -1;
        map.previewStart = new PathPoint(wx, wz);
        map.previewEnd = null;
        pathActive = true;
    }

    private void startSquadPath(int wx, int wz, int squadId) {
        List<PathPoint> sub = new java.util.ArrayList<>();
        sub.add(new PathPoint(wx, wz));
        int key = pathCounter++;
        map.pathGroupsSquad().put(key, sub);
        com.pigeostudios.pwp.warfare.client.PathCache.squadNumForPath.put(key, squadId);
        activeGroups = null;
        activeSquadId = squadId;
        activeSquadKey = key;
        map.previewStart = new PathPoint(wx, wz);
        map.previewEnd = null;
        pathActive = true;
    }

    private static boolean isPlayerCMD(LocalPlayer p) {
        String name = p.getScoreboardName();
        String team = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
        int cmdId = team.contains("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
        if (cmdId == -1) return false;
        for (var sq : ClientData.clientSquads) {
            if (sq.id == cmdId && sq.members.contains(name)) return true;
        }
        return false;
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
            if (d < bestD
                    && sp.status() != DeployData.SpawnStatus.BLOCKED
                    && sp.status() != DeployData.SpawnStatus.DESTROYED) {
                bestD = d;
                best = sp.id();
            }
        }
        return best;
    }

    private void onDeploy(String spawnId) {
        map.selectedSpawnId = spawnId;
        if (parent instanceof com.pigeostudios.pwp.warfare.client.gui.DeployScreen)
            ((com.pigeostudios.pwp.warfare.client.gui.DeployScreen) parent).selectedSpawn = spawnId;
        Minecraft.getInstance().setScreen(parent);
    }

    private int toWorldX(double mx) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int) (map.getCenterX(p) + (mx - (mX + mMapW / 2.0)) * map.getBlocksPerPixel());
    }

    private int toWorldZ(double my) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int) (map.getCenterZ(p) + (my - (mY + mMapH / 2.0)) * map.getBlocksPerPixel());
    }
}

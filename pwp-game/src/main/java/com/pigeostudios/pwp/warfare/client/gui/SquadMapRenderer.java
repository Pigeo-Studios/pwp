package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.client.WarfareUiSettings;
import com.pigeostudios.pwp.warfare.client.gui.map.MapDraw;
import com.pigeostudios.pwp.warfare.client.gui.map.MapHud;
import com.pigeostudios.pwp.warfare.client.gui.map.MapLayers;
import com.pigeostudios.pwp.warfare.client.gui.map.MapView;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class SquadMapRenderer {

    /** Шаг зума колесом для {@link #localScale}. */
    private static final double LOCAL_ZOOM_STEP = 1.25;

    public int mapX, mapY, mapWidth, mapHeight;
    public String selectedSpawnId = "";
    // Минимальный скейл (блоков на пиксель) для вписывания всей карты в область рендера.
    // 0 = использовать глобальный ClientData.mapScale (поведение M-меню).
    public double fitScale = 0.0;
    /**
     * Свой масштаб этого рендера (блоков на пиксель), > 0 — колесо зумит его вокруг курсора в
     * пределах [{@link #localMin}, {@link #localMax}], глобальный ClientData.mapScale и fitScale не
     * используются (карта деплоя). 0 — поведение M-меню.
     */
    public double localScale = 0.0;

    public double localMin = 0.25, localMax = 8.0;
    private double panX, panZ;
    private boolean isDraggingMap;
    private double lastMouseX, lastMouseY;
    public PathPoint previewStart, previewEnd;

    public List<List<PathPoint>> pathGroups() {
        return com.pigeostudios.pwp.warfare.client.PathCache.groups;
    }

    public List<List<PathPoint>> pathGroupsRed() {
        return com.pigeostudios.pwp.warfare.client.PathCache.groupsRed;
    }

    public List<List<PathPoint>> pathGroupsYellow() {
        return com.pigeostudios.pwp.warfare.client.PathCache.groupsYellow;
    }

    public Map<Integer, List<PathPoint>> pathGroupsSquad() {
        return com.pigeostudios.pwp.warfare.client.PathCache.groupsSquad;
    }

    private static final Map<String, ResourceLocation> MAP_ICONS_CACHE = new HashMap<>();
    private static final Map<String, ResourceLocation> TEX = new HashMap<>();

    private static final ResourceLocation MATS_ICON = new ResourceLocation("pwpwarfare", "textures/gui/mats_icon.png");
    private static final ResourceLocation MARKER_MOVE =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move.png");
    private static final ResourceLocation MARKER_ATTACK =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_attack.png");
    private static final ResourceLocation MARKER_DEFEND =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_defend.png");
    private static final ResourceLocation MARKER_BUILD =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_build.png");
    private static final ResourceLocation HUB_ICON =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_icon.png");
    private static final ResourceLocation HUB_ICON_SELECTED =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_icon_selected.png");
    private static final ResourceLocation RALLY_ICON =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_icon.png");
    private static final ResourceLocation RALLY_ICON_SELECTED =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_icon_selected.png");
    private static final ResourceLocation MAIN_BASE_ICON =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/main_base.png");
    private static final ResourceLocation MAIN_BASE_ICON_SELECTED =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/main_base_selected.png");
    private static final ResourceLocation STATION_ICON =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/vehicle_station.png");
    private static final ResourceLocation FLAG_NEUTRAL =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/neutral.png");
    private static final ResourceLocation ICON_CIRCLE =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/player_circle.png");
    private static final ResourceLocation ICON_PLUS =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/medic_plus.png");
    private static final ResourceLocation ICON_SELF =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/player_self.png");
    private static final ResourceLocation ICON_COMPASS =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/compass.png");
    private static final ResourceLocation ICON_OBJ_ATTACK =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/objective_attack.png");
    private static final ResourceLocation ICON_OBJ_DEFEND =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/objective_defend.png");
    private static final Map<String, ResourceLocation> VEHICLE_ICONS = new HashMap<>();

    public void init(int x, int y, int w, int h) {
        mapX = x;
        mapY = y;
        mapWidth = w;
        mapHeight = h;
    }

    public void centerOnPlayer() {
        panX = 0;
        panZ = 0;
    }

    /** Центр вида на мировую точку (смещение от игрока). */
    public void centerOn(LocalPlayer p, double wx, double wz) {
        panX = wx - p.getX();
        panZ = wz - p.getZ();
    }

    /** Мировая точка видна в области карты (с отступом {@code marginPx}). */
    public boolean isVisible(LocalPlayer p, double wx, double wz, int marginPx) {
        MapView v = view(p);
        double sx = v.sx(wx), sy = v.sy(wz);
        return sx >= mapX + marginPx
                && sx <= mapX + mapWidth - marginPx
                && sy >= mapY + marginPx
                && sy <= mapY + mapHeight - marginPx;
    }

    public double effectiveScale() {
        if (localScale > 0) return localScale;
        return fitScale > 0 ? Math.max(ClientData.mapScale, fitScale) : ClientData.mapScale;
    }

    public double getBlocksPerPixel() {
        return effectiveScale();
    }

    public double getCenterX(LocalPlayer p) {
        return p.getX() + panX;
    }

    public double getCenterZ(LocalPlayer p) {
        return p.getZ() + panZ;
    }

    public boolean isMouseOver(double mx, double my) {
        return mx >= mapX && mx <= mapX + mapWidth && my >= mapY && my <= mapY + mapHeight;
    }

    /** Проекция текущего кадра (общая для всех слоёв). */
    public MapView view(LocalPlayer p) {
        double bpp = effectiveScale();
        if (bpp <= 0) bpp = 1.0;
        return new MapView(Rect.of(mapX, mapY, mapWidth, mapHeight), p.getX() + panX, p.getZ() + panZ, bpp);
    }

    /**
     * Карта по слоям (порядок фиксирован, каждый слой — один раз): подложка → текстура → сетка →
     * маска вне зоны и линия границы → подписи квадратов → постройки/техника/игроки/маркеры/пути →
     * HUD (рамка, координаты, масштаб, компас).
     */
    /** Подписи без наложений (этап 12): точки → материалы хабов → ники; не влезла — не рисуется. */
    private final com.pigeostudios.pwp.warfare.map.LabelPlacer labels =
            new com.pigeostudios.pwp.warfare.map.LabelPlacer();

    public void render(GuiGraphics g, int mx, int my, float pt) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        labels.reset(mapX, mapY, mapWidth, mapHeight);
        MapView v = view(p);
        double bpp = v.bpp();
        double cx = v.centerX();
        double cz = v.centerZ();
        var font = Minecraft.getInstance().font;

        g.enableScissor(mapX, mapY, mapX + mapWidth, mapY + mapHeight);
        MapLayers.background(g, v);
        MapLayers.tiles(g, v);
        MapLayers.grid(g, v);
        MapLayers.bounds(g, v, System.currentTimeMillis());
        MapLayers.labels(g, font, v);

        renderLatticeLines(g, cx, cz, bpp);
        renderOverlays(g, cx, cz, bpp);
        renderMainBases(g, cx, cz, bpp);
        renderArtilleryZones(g, cx, cz, bpp);
        renderStructures(g, cx, cz, bpp);
        renderVehicles(g, cx, cz, bpp);
        renderAllPlayers(g, p, cx, cz, bpp);
        renderSquadMarkerLogic(g, cx, cz, bpp);
        renderTacticalMarkers(g, cx, cz, bpp);
        renderSquadRhombusMarkers(g, cx, cz, bpp);
        renderSquadPings(g, cx, cz, bpp);
        drawMarkers(g, cx, cz);
        drawPath(g, cx, cz);
        drawPreview(g, cx, cz);

        MapLayers.noMap(g, font, v);
        MapLayers.loading(g, font, v);
        MapHud.frame(g, v);
        MapHud.readout(g, font, v, mx, my);
        MapHud.scaleBar(g, font, v);
        MapHud.compass(g, font, v);
        g.disableScissor();
    }

    private void setFilter(ResourceLocation tex, boolean smooth) {
        var t = Minecraft.getInstance().getTextureManager().getTexture(tex);
        if (t != null) t.setFilter(smooth, false);
    }

    private void renderLatticeLines(GuiGraphics g, double cx, double cz, double bpp) {
        if (ClientData.allCapturePoints == null || ClientData.allCapturePoints.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String currentDim = mc.level.dimension().location().toString();
        boolean isRed = mc.player != null
                && mc.player.getTeam() != null
                && mc.player.getTeam().getName().equalsIgnoreCase("Red");
        List<WarfareWorldData.CapturePoint> sortedPoints = new ArrayList<>(ClientData.allCapturePoints);
        if (isRed) sortedPoints.sort(Comparator.comparingInt(p -> p.redPriority));
        else sortedPoints.sort(Comparator.comparingInt(p -> p.bluePriority));
        List<Vec3> path = new ArrayList<>();
        if (isRed) {
            if (ClientData.redSpawns.containsKey(currentDim)) {
                BlockPos rPos = ClientData.redSpawns.get(currentDim);
                path.add(new Vec3(rPos.getX() + 0.5, rPos.getY(), rPos.getZ() + 0.5));
            }
        } else {
            if (ClientData.blueSpawns.containsKey(currentDim)) {
                BlockPos bPos = ClientData.blueSpawns.get(currentDim);
                path.add(new Vec3(bPos.getX() + 0.5, bPos.getY(), bPos.getZ() + 0.5));
            }
        }
        for (var cp : sortedPoints) path.add(cp.area.getCenter());
        if (isRed) {
            if (ClientData.blueSpawns.containsKey(currentDim)) {
                BlockPos bPos = ClientData.blueSpawns.get(currentDim);
                path.add(new Vec3(bPos.getX() + 0.5, bPos.getY(), bPos.getZ() + 0.5));
            }
        } else {
            if (ClientData.redSpawns.containsKey(currentDim)) {
                BlockPos rPos = ClientData.redSpawns.get(currentDim);
                path.add(new Vec3(rPos.getX() + 0.5, rPos.getY(), rPos.getZ() + 0.5));
            }
        }
        int lineColor = WTheme.MAP_LATTICE;
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 p1 = path.get(i);
            Vec3 p2 = path.get(i + 1);
            int x1 = toScreenX(p1.x, cx);
            int y1 = toScreenZ(p1.z, cz);
            int x2 = toScreenX(p2.x, cx);
            int y2 = toScreenZ(p2.z, cz);
            drawSolidLine(g, x1, y1, x2, y2, lineColor);
        }
    }

    private void drawSolidLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x1, y1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(dy, dx))));
        g.fill(0, 0, (int) len, 1, color);
        pose.popPose();
    }

    private void renderOverlays(GuiGraphics g, double cx, double cz, double bpp) {
        if (ClientData.allCapturePoints == null) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        boolean blinkOn = System.currentTimeMillis() / 400L % 2L == 0L;
        for (var cp : ClientData.allCapturePoints) {
            Vec3 center = cp.area.getCenter();
            int pX = toScreenX(center.x, cx);
            int pY = toScreenZ(center.z, cz);
            if (!inMap(pX, pY)) continue;
            String owner = cp.owner.toUpperCase();
            String capTeam = cp.capturingTeam.toUpperCase();
            float progress = cp.progress;
            float alpha = 1;
            String teamToRender;
            if (owner.equals("NEUTRAL")) {
                if (capTeam.equals("NONE") || capTeam.equals("NEUTRAL")) {
                    teamToRender = "NEUTRAL";
                } else if (blinkOn) {
                    teamToRender = capTeam;
                    alpha = 0.1F + progress * 0.9F;
                } else {
                    teamToRender = "NEUTRAL";
                }
            } else if (progress < 1) {
                teamToRender = owner;
                alpha = 0.1F + progress * 0.9F;
            } else {
                teamToRender = owner;
            }
            ResourceLocation flagTex = FLAG_NEUTRAL;
            int tintColor = -1;
            boolean useTint = false;
            if (teamToRender.equals("BLUE")) {
                flagTex = getFlagTexture(ClientData.BLUE_FACTION);
                if (flagTex == null) {
                    flagTex = FLAG_NEUTRAL;
                    tintColor = -11184641;
                    useTint = true;
                }
            } else if (teamToRender.equals("RED")) {
                flagTex = getFlagTexture(ClientData.RED_FACTION);
                if (flagTex == null) {
                    flagTex = FLAG_NEUTRAL;
                    tintColor = -43691;
                    useTint = true;
                }
            }
            float r = 1, g2 = 1, b = 1;
            if (useTint) {
                r = (tintColor >> 16 & 0xFF) / 255f;
                g2 = (tintColor >> 8 & 0xFF) / 255f;
                b = (tintColor & 0xFF) / 255f;
            }
            RenderSystem.setShaderColor(r, g2, b, alpha);
            setFilter(flagTex, true);
            g.blit(flagTex, pX - 8, pY - 4, 16, 9, 0, 0, 64, 36, 64, 36);
            setFilter(flagTex, false);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            // кольцо прогресса захвата цветом захватывающей/владеющей команды + подпись-плашка
            String ringTeam = !capTeam.equals("NONE") && !capTeam.equals("NEUTRAL") ? capTeam : owner;
            int ringColor = ringTeam.equals("BLUE")
                    ? WTheme.TEAM_BLUE
                    : (ringTeam.equals("RED") ? WTheme.TEAM_RED : WTheme.MAP_RING_NEUTRAL);
            MapDraw.progressRing(g, pX, pY, WTheme.MAP_OBJECTIVE_R, owner.equals("NEUTRAL") ? progress : 1F, ringColor);
            var lf = Minecraft.getInstance().font;
            Component name = Component.literal(cp.name);
            int r0 = WTheme.MAP_OBJECTIVE_R;
            labels.block(pX - r0, pY - r0, 2 * r0, 2 * r0);
            var spot = labels.place(pX, pY, r0, MapDraw.labelWidth(lf, name), MapDraw.labelHeight(lf), 1);
            if (spot != null) MapDraw.labelAt(g, lf, name, spot.x(), spot.y(), WTheme.MAP_LABEL_TEXT);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private ResourceLocation getFlagTexture(String faction) {
        return faction != null && !faction.equalsIgnoreCase("none")
                ? com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/flags/" + faction.toLowerCase() + ".png")
                : null;
    }

    private void renderMainBases(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String currentDim = mc.level.dimension().location().toString();
        RenderSystem.enableBlend();
        if (ClientData.blueSpawns.containsKey(currentDim))
            drawMainBaseIcon(g, ClientData.blueSpawns.get(currentDim), cx, cz, bpp, ClientData.BLUE_FACTION, -13408564);
        if (ClientData.redSpawns.containsKey(currentDim))
            drawMainBaseIcon(g, ClientData.redSpawns.get(currentDim), cx, cz, bpp, ClientData.RED_FACTION, -3394765);
    }

    private void drawMainBaseIcon(
            GuiGraphics g, BlockPos pos, double cx, double cz, double bpp, String faction, int fallbackColor) {
        int pX = toScreenX(pos.getX() + 0.5, cx);
        int pY = toScreenZ(pos.getZ() + 0.5, cz);
        if (!inMap(pX, pY)) return;
        ResourceLocation flagTex = getFlagTexture(faction);
        if (flagTex != null && !faction.equals("none")) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            setFilter(flagTex, true);
            g.blit(flagTex, pX - 8, pY - 4, 16, 9, 0, 0, 64, 36, 64, 36);
            setFilter(flagTex, false);
        } else {
            g.fill(pX - 8, pY - 4, pX + 8, pY + 5, fallbackColor);
        }
        boolean isSelected = selectedSpawnId.equals("MAIN");
        ResourceLocation mainTex = isSelected ? MAIN_BASE_ICON_SELECTED : MAIN_BASE_ICON;
        setFilter(mainTex, true);
        g.pose().pushPose();
        g.pose().translate(pX, pY, 150);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(mainTex, -6, -6, 12, 12, 0, 0, 16, 16, 16, 16);
        g.pose().popPose();
        setFilter(mainTex, false);
        g.pose().pushPose();
        g.pose().translate(pX, pY + 7, 151);
        g.pose().scale(0.6F, 0.6F, 1);
        g.drawCenteredString(PWPTheme.Fonts.display(), "MAIN", 0, 0, -1);
        g.pose().popPose();
    }

    private void renderArtilleryZones(GuiGraphics g, double cx, double cz, double bpp) {
        if (ClientData.activeStrikes == null || ClientData.activeStrikes.isEmpty()) return;
        float radius = 15;
        for (var strike : ClientData.activeStrikes) {
            float sx = (float) toScreenX(strike.pos.getX() + 0.5, cx);
            float sy = (float) toScreenZ(strike.pos.getZ() + 0.5, cz);
            drawSmoothCircle(g, sx, sy, radius / (float) bpp, -65536);
        }
    }

    private void drawSmoothCircle(GuiGraphics g, float cx, float cy, float radius, int color) {
        if (radius <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var tesselator = Tesselator.getInstance();
        var buffer = tesselator.getBuilder();
        Matrix4f matrix = g.pose().last().pose();
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g2 = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        buffer.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int seg = 128;
        for (int i = 0; i <= seg; i++) {
            float angle = i * (float) (Math.PI * 2) / seg;
            float x = cx + Mth.cos(angle) * radius;
            float y = cy + Mth.sin(angle) * radius;
            buffer.vertex(matrix, x, y, 0).color(r, g2, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
    }

    private void renderStructures(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        String myTeam = "NEUTRAL";
        if (mc.player.getTeam() != null) {
            String name = mc.player.getTeam().getName().toUpperCase();
            if (name.contains("BLUE")) myTeam = "BLUE";
            else if (name.contains("RED")) myTeam = "RED";
        }
        boolean isObserver = mc.player.isCreative() || mc.player.isSpectator();

        for (var hub : ClientData.clientHubs) {
            if (!hub.constructed) continue;
            if (!hub.team.equalsIgnoreCase(myTeam) && !isObserver) continue;
            float sx = (float) toScreenX(hub.pos.getX() + 0.5, cx);
            float sy = (float) toScreenZ(hub.pos.getZ() + 0.5, cz);
            g.pose().pushPose();
            g.pose().translate(0, 0, 50);
            drawSmoothCircle(g, sx, sy, 50 / (float) bpp, 1627389951);
            int teamCircleColor = hub.team.equalsIgnoreCase("BLUE") ? -2141891073 : -2130750123;
            drawSmoothCircle(g, sx, sy, 150 / (float) bpp, teamCircleColor);
            g.pose().popPose();
            if (inMap((int) sx, (int) sy)) {
                String hubPayload = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
                boolean isSelected = selectedSpawnId.equals(hubPayload);
                ResourceLocation hubTex = isSelected ? HUB_ICON_SELECTED : HUB_ICON;
                setFilter(hubTex, true);
                g.pose().pushPose();
                g.pose().translate(sx, sy, 160);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                g.blit(hubTex, -6, -6, 12, 12, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
                setFilter(hubTex, false);
                String matsText = String.valueOf(hub.materials);
                int matsW = PWPTheme.Fonts.display().width(matsText);
                labels.block((int) sx - 6, (int) sy - 6, 12, 12);
                var matsSpot = labels.place((int) sx, (int) sy, 6, matsW + 12, 10, 2);
                if (matsSpot == null) continue;
                int matsX = matsSpot.x() + 1;
                int matsY = matsSpot.y() + 1;
                g.pose().pushPose();
                g.pose().translate(0, 0, 165);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                UIDraw.rect(g, Rect.of(matsX - 1, matsY - 1, matsW + 12, 10), WTheme.MAP_LABEL_BG);
                setFilter(MATS_ICON, true);
                g.blit(MATS_ICON, matsX, matsY, 8, 8, 0, 0, 16, 16, 16, 16);
                setFilter(MATS_ICON, false);
                UIDraw.text(
                        g,
                        Minecraft.getInstance().font,
                        Component.literal(matsText),
                        matsX + 10,
                        matsY,
                        WTheme.MAP_MATS_TEXT,
                        false);
                g.pose().popPose();
            }
        }

        for (var supply : ClientData.clientvehicleStations) {
            if (!supply.team.equalsIgnoreCase(myTeam) && !isObserver) continue;
            float sx = (float) toScreenX(supply.pos.getX() + 0.5, cx);
            float sy = (float) toScreenZ(supply.pos.getZ() + 0.5, cz);
            if (inMap((int) sx, (int) sy)) {
                setFilter(STATION_ICON, true);
                g.pose().pushPose();
                g.pose().translate(sx, sy, 155);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                g.blit(STATION_ICON, -5, -5, 10, 10, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
                setFilter(STATION_ICON, false);
            }
        }

        for (var squad : ClientData.clientSquads) {
            if (squad.rallyPos == null) continue;
            if (!squad.team.equalsIgnoreCase(myTeam) && !isObserver) continue;
            float sx = (float) toScreenX(squad.rallyPos.getX() + 0.5, cx);
            float sy = (float) toScreenZ(squad.rallyPos.getZ() + 0.5, cz);
            if (inMap((int) sx, (int) sy)) {
                boolean isSelected = selectedSpawnId.equals("RALLY");
                ResourceLocation rallyTex = isSelected ? RALLY_ICON_SELECTED : RALLY_ICON;
                setFilter(rallyTex, true);
                g.pose().pushPose();
                g.pose().translate(sx, sy, 170);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                g.blit(rallyTex, -5, -5, 10, 10, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
                setFilter(rallyTex, false);
            }
        }
    }

    private void renderVehicles(GuiGraphics g, double cx, double cz, double bpp) {
        if (ClientData.clientVehicles == null || ClientData.clientVehicles.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        String myTeam = "NEUTRAL";
        if (mc.player.getTeam() != null) {
            String name = mc.player.getTeam().getName().toUpperCase();
            if (name.contains("BLUE")) myTeam = "BLUE";
            else if (name.contains("RED")) myTeam = "RED";
        }
        boolean isObserver = mc.player.isCreative() || mc.player.isSpectator();
        for (var record : ClientData.clientVehicles) {
            if (!record.team.equalsIgnoreCase(myTeam) && !isObserver) continue;
            int sx = toScreenX(record.x, cx);
            int sy = toScreenZ(record.z, cz);
            if (!inMap(sx, sy)) continue;
            ResourceLocation icon = VEHICLE_ICONS.getOrDefault(record.type, VEHICLE_ICONS.get("DEFAULT"));
            setFilter(icon, true);
            g.pose().pushPose();
            g.pose().translate(sx, sy, 150);
            if (!record.type.equals("Mine")) g.pose().mulPose(Axis.ZP.rotationDegrees(record.yaw + 180));
            float vs = WarfareUiSettings.MAP_ICON_SCALE.get();
            g.pose().scale(vs, vs, 1F);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableBlend();
            g.blit(icon, -6, -6, 12, 12, 0, 0, 16, 16, 16, 16);
            g.pose().popPose();
            setFilter(icon, false);
        }
    }

    /** Русское имя фракции для подписей (null — нет фракции). */
    public static String factionName(String f) {
        if (f == null || f.isEmpty() || "none".equals(f)) return null;
        return switch (f.toLowerCase()) {
            case "usa" -> "\u0421\u0428\u0410";
            case "russia", "russian" -> "\u0420\u043E\u0441\u0441\u0438\u044F";
            case "british",
                    "uk",
                    "gb" -> "\u0412\u0435\u043B\u0438\u043A\u043E\u0431\u0440\u0438\u0442\u0430\u043D\u0438\u044F";
            case "militia" -> "\u041E\u043F\u043E\u043B\u0447\u0435\u043D\u0438\u0435";
            case "insurgent", "insurgents" -> "\u041F\u043E\u0432\u0441\u0442\u0430\u043D\u0446\u044B";
            case "middle_eastern_alliance", "mea" -> "\u0411\u041B\u0412";
            case "canada", "ca" -> "\u041A\u0430\u043D\u0430\u0434\u0430";
            case "australia", "au" -> "\u0410\u0432\u0441\u0442\u0440\u0430\u043B\u0438\u044F";
            case "germany", "de" -> "\u0413\u0435\u0440\u043C\u0430\u043D\u0438\u044F";
            case "turkey", "tr" -> "\u0422\u0443\u0440\u0446\u0438\u044F";
            default -> f.toUpperCase();
        };
    }

    /** Иконка типа техники для легенды/карты (DEFAULT — если тип неизвестен). */
    public static ResourceLocation vehicleIcon(String type) {
        return VEHICLE_ICONS.getOrDefault(type, VEHICLE_ICONS.get("DEFAULT"));
    }

    private void renderAllPlayers(GuiGraphics g, LocalPlayer self, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        String myName = self.getScoreboardName();
        int myInternalSquadId = -1;
        String myTeamForNums = getPlayerTeamStrict();
        boolean isBlueForNums = myTeamForNums != null && myTeamForNums.contains("BLUE");
        int teamCMDIdForNums = isBlueForNums ? ClientData.blueCMDId : ClientData.redCMDId;
        List<WarfareWorldData.Squad> teamSquadsForNums = ClientData.clientSquads.stream()
                .filter(s -> s.team.equalsIgnoreCase(myTeamForNums))
                .collect(Collectors.toList());
        teamSquadsForNums.sort((s1, s2) -> {
            if (s1.id == teamCMDIdForNums && teamCMDIdForNums != -1) return -1;
            return s2.id == teamCMDIdForNums && teamCMDIdForNums != -1 ? 1 : Integer.compare(s1.id, s2.id);
        });
        Map<Integer, Integer> idToDisplayNum = new HashMap<>();
        for (int i = 0; i < teamSquadsForNums.size(); i++) {
            WarfareWorldData.Squad s = teamSquadsForNums.get(i);
            idToDisplayNum.put(s.id, i + 1);
            if (s.members.contains(myName)) myInternalSquadId = s.id;
        }
        boolean isShowNicksHeld = isShowNicknamesHeld();
        long currentTime = mc.level.getGameTime();
        boolean amIMedic = "Medic".equalsIgnoreCase(ClientData.myCurrentKit);
        Map<Integer, List<MapPlayerInfo>> vehicleGroups = new HashMap<>();
        for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
            if (info.inVehicle) {
                vehicleGroups
                        .computeIfAbsent(info.vehicleId, k -> new ArrayList<>())
                        .add(info);
                continue;
            }
            int sx = toScreenX(info.x, cx);
            int sy = toScreenZ(info.z, cz);
            if (!inMap(sx, sy)) continue;
            var f = PWPTheme.Fonts.display();
            // враг, раскрытый разведкой командира (этап 15): красная точка, без ника и номера отряда
            if (myTeamForNums != null
                    && info.team != null
                    && !info.team.equalsIgnoreCase(myTeamForNums)
                    && !self.isCreative()
                    && !self.isSpectator()) {
                RenderSystem.setShaderColor(1F, 0.25F, 0.2F, 1F);
                pushIconScale(g, sx, sy);
                g.blit(ICON_CIRCLE, sx - 3, sy - 3, 6, 6, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
                RenderSystem.setShaderColor(1, 1, 1, 1);
                continue;
            }
            if (isShowNicksHeld && !info.name.equals(myName) && !info.isDowned) {
                int nickColor = info.squadId != -1 && info.squadId == myInternalSquadId ? -11141291 : -1;
                drawNick(g, f, info.name, sx, sy, nickColor);
            }
            if (info.name.equals(myName)) continue;
            if (info.isDowned) {
                if (amIMedic || currentTime - info.lastShoutTime < 60) {
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    pushIconScale(g, sx, sy);
                    g.blit(ICON_PLUS, sx - 4, sy - 4, 8, 8, 0, 0, 16, 16, 16, 16);
                    g.pose().popPose();
                }
            } else {
                float r2 = 0.2F, g2 = 0.6F, b2 = 1.0F;
                if (info.squadId != -1 && info.squadId == myInternalSquadId) {
                    r2 = 0;
                    g2 = 1;
                    b2 = 0;
                }
                RenderSystem.setShaderColor(r2, g2, b2, 1);
                pushIconScale(g, sx, sy);
                g.blit(ICON_CIRCLE, sx - 3, sy - 3, 6, 6, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            Integer displayNum = idToDisplayNum.get(info.squadId);
            if (!info.isDowned && info.isLeader && info.squadId != -1 && displayNum != null) {
                String numStr = String.valueOf(displayNum);
                int textColor = info.squadId == myInternalSquadId ? -11141291 : -11184641;
                g.pose().pushPose();
                g.pose().translate(sx, sy, 350);
                float ns = 0.5F * WarfareUiSettings.MAP_ICON_SCALE.get();
                g.pose().scale(ns, ns, 1);
                int tw = f.width(numStr);
                drawSquadNumber(g, f, numStr, -(tw / 2), -4, textColor);
                g.pose().popPose();
            }
        }

        if (isShowNicksHeld) {
            for (var group : vehicleGroups.values()) {
                if (group.isEmpty()) continue;
                group.sort(Comparator.comparingInt(p -> p.seatIndex));
                var driver = group.get(0);
                int sx = toScreenX(driver.x, cx);
                int sy = toScreenZ(driver.z, cz);
                if (!inMap(sx, sy)) continue;
                // экипаж — одной подписью «водитель +N», чтобы столбик ников не закрывал карту
                int nickColor = -1;
                for (var pInfo : group) {
                    if (pInfo.squadId != -1 && pInfo.squadId == myInternalSquadId) nickColor = -11141291;
                }
                String crew = driver.name + (group.size() > 1 ? " +" + (group.size() - 1) : "");
                drawNick(g, PWPTheme.Fonts.display(), crew, sx, sy, nickColor);
            }
        }

        renderSelf(g, self, cx, cz, bpp, myInternalSquadId);
    }

    private void renderSelf(GuiGraphics g, LocalPlayer self, double cx, double cz, double bpp, int mySquadId) {
        boolean inVehicle = self.getVehicle() != null;
        int mySx = toScreenX(self.getX(), cx);
        int mySy = toScreenZ(self.getZ(), cz);
        if (!inMap(mySx, mySy) || inVehicle) return;
        g.pose().pushPose();
        g.pose().translate(mySx, mySy, 300);
        g.pose().mulPose(Axis.ZP.rotationDegrees(self.getYRot() + 180));
        float selfScale = WarfareUiSettings.MAP_ICON_SCALE.get();
        g.pose().scale(selfScale, selfScale, 1F);
        if (mySquadId != -1) RenderSystem.setShaderColor(0, 1, 0, 1);
        else RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(ICON_SELF, -5, -5, 10, 10, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.pose().popPose();
        if (isShowNicknamesHeld()) {
            int myColor = mySquadId != -1 ? -11141291 : -171;
            drawNick(g, PWPTheme.Fonts.display(), self.getScoreboardName(), mySx, mySy, myColor);
        }
    }

    /** Ник у иконки игрока, место — через {@link #labels} (над иконкой, иначе сбоку/снизу; нет места — нет). */
    private static final float NICK_SCALE = 0.6F;

    private void drawNick(GuiGraphics g, net.minecraft.client.gui.Font f, String name, int sx, int sy, int color) {
        int w = Math.max(1, Math.round(f.width(name) * NICK_SCALE));
        int h = Math.max(1, Math.round(f.lineHeight * NICK_SCALE));
        var spot = labels.place(sx, sy, 3, w, h, 1, com.pigeostudios.pwp.warfare.map.LabelPlacer.ABOVE_FIRST);
        if (spot == null) return;
        g.pose().pushPose();
        g.pose().translate(spot.x(), spot.y(), 450);
        g.pose().scale(NICK_SCALE, NICK_SCALE, 1);
        g.drawString(f, Component.literal(name), 0, 0, color, true);
        g.pose().popPose();
    }

    private void renderSquadMarkerLogic(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String myName = mc.player.getScoreboardName();
        WarfareWorldData.Squad mySquad = null;
        for (var s : ClientData.clientSquads) {
            if (s.members.contains(myName)) {
                mySquad = s;
                break;
            }
        }
        if (mySquad == null) return;
        if (mySquad.marker != null && mySquad.marker.type != 6)
            drawMapMarkerAndLine(
                    g,
                    cx,
                    cz,
                    bpp,
                    mySquad.marker,
                    getSquadMarkerIcon(mySquad.marker.type),
                    getSquadMarkerColor(mySquad.marker.type),
                    mySquad.marker.type != 0);
        if (mySquad.bravoMarker != null && mySquad.bravoMarker.type != 6)
            drawMapMarkerAndLine(
                    g,
                    cx,
                    cz,
                    bpp,
                    mySquad.bravoMarker,
                    getBravoMarkerIcon(mySquad.bravoMarker.type),
                    -65281,
                    mySquad.bravoMarker.type != 0);
        if (mySquad.charlieMarker != null && mySquad.charlieMarker.type != 6)
            drawMapMarkerAndLine(
                    g,
                    cx,
                    cz,
                    bpp,
                    mySquad.charlieMarker,
                    getCharlieMarkerIcon(mySquad.charlieMarker.type),
                    -16711766,
                    mySquad.charlieMarker.type != 0);
    }

    private void drawMapMarkerAndLine(
            GuiGraphics g,
            double cx,
            double cz,
            double bpp,
            WarfareWorldData.SquadMarker m,
            ResourceLocation icon,
            int color,
            boolean withDash) {
        Minecraft mc = Minecraft.getInstance();
        int mx = toScreenX(m.x, cx);
        int my = toScreenZ(m.z, cz);
        int px = toScreenX(mc.player.getX(), cx);
        int py = toScreenZ(mc.player.getZ(), cz);
        if (withDash) drawDashedLine(g, px, py, mx, my, color);
        if (inMap(mx, my)) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, markerAlpha(1F));
            pushIconScale(g, mx, my);
            g.blit(icon, mx - 6, my - 6, 0, 0, 12, 12, 12, 12);
            g.pose().popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            double distance = Math.sqrt(mc.player.distanceToSqr(m.x, mc.player.getY(), m.z));
            String distText = (int) distance + "m";
            g.pose().pushPose();
            g.pose().translate(mx, my + 8, 600);
            g.pose().scale(0.8F, 0.8F, 1);
            int tw = PWPTheme.Fonts.display().width(distText);
            g.drawString(PWPTheme.Fonts.display(), distText, -(tw / 2), 0, color, true);
            g.pose().popPose();
        }
    }

    private void drawDashedLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 5) return;
        for (int i = 0; i < len; i += 4) {
            double t = i / len;
            int lx = (int) (x1 + dx * t);
            int ly = (int) (y1 + dy * t);
            if (inMap(lx, ly)) g.fill(lx, ly, lx + 2, ly + 2, color);
        }
    }

    private int getSquadMarkerColor(int type) {
        return switch (type) {
            case 1 -> -22016;
            case 2 -> -11184641;
            case 3 -> -43521;
            case 4 -> -1;
            case 5 -> -65536;
            default -> -11141291;
        };
    }

    private ResourceLocation getSquadMarkerIcon(int type) {
        return switch (type) {
            case 1 -> MARKER_ATTACK;
            case 2 -> MARKER_DEFEND;
            case 3 -> MARKER_BUILD;
            default -> MARKER_MOVE;
        };
    }

    private ResourceLocation getBravoMarkerIcon(int type) {
        String pre =
                switch (type) {
                    case 1 -> "marker_attack_bravo";
                    case 2 -> "marker_defend_bravo";
                    case 3 -> "marker_build_bravo";
                    default -> "marker_move_bravo";
                };
        return com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/map_icons/" + pre + ".png");
    }

    private ResourceLocation getCharlieMarkerIcon(int type) {
        String pre =
                switch (type) {
                    case 1 -> "marker_attack_charlie";
                    case 2 -> "marker_defend_charlie";
                    case 3 -> "marker_build_charlie";
                    default -> "marker_move_charlie";
                };
        return com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/map_icons/" + pre + ".png");
    }

    private void renderTacticalMarkers(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        String myTeam = getPlayerTeamStrict();
        long currentTime = mc.level.getGameTime();
        for (var m : ClientData.activeMarkers) {
            if (!m.team.equalsIgnoreCase(myTeam)) continue;
            long timeLeft = m.expiryTick - currentTime;
            if (timeLeft <= 0) continue;
            float alpha = Mth.clamp((float) timeLeft / 3600f, 0, 1);
            int sx = toScreenX(m.pos.getX(), cx);
            int sy = toScreenZ(m.pos.getZ(), cz);
            if (!inMap(sx, sy)) continue;
            ResourceLocation icon = getMarkerIcon(m.type);
            setFilter(icon, true);
            g.pose().pushPose();
            g.pose().translate(sx, sy, 120);
            float ts = WarfareUiSettings.MAP_ICON_SCALE.get();
            g.pose().scale(ts, ts, 1F);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, markerAlpha(alpha));
            g.blit(icon, -8, -8, 16, 16, 0, 0, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.pose().popPose();
            setFilter(icon, false);
        }
    }

    private ResourceLocation getMarkerIcon(String type) {
        String path = type.toLowerCase().replace("enemy ", "").replace(" ", "_");
        return com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/map_icons/" + path + "_marker.png");
    }

    private void renderSquadRhombusMarkers(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        long time = mc.level.getGameTime();
        String myName = mc.player.getScoreboardName();
        String myTeam = getPlayerTeamStrict();
        boolean amISquadLeader = isSquadLeaderOrFTL(mc.player);
        boolean isBlueForNums = myTeam != null && myTeam.contains("BLUE");
        int teamCMDIdForNums = isBlueForNums ? ClientData.blueCMDId : ClientData.redCMDId;
        List<WarfareWorldData.Squad> teamSquadsForNums = ClientData.clientSquads.stream()
                .filter(s -> s.team.equalsIgnoreCase(myTeam))
                .collect(Collectors.toList());
        teamSquadsForNums.sort((s1, s2) -> {
            if (s1.id == teamCMDIdForNums && teamCMDIdForNums != -1) return -1;
            return s2.id == teamCMDIdForNums && teamCMDIdForNums != -1 ? 1 : Integer.compare(s1.id, s2.id);
        });
        var rhombusTex = com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/map_icons/squad_rhombus.png");
        var f = PWPTheme.Fonts.display();
        for (var squad : ClientData.clientSquads) {
            if (!squad.team.equalsIgnoreCase(myTeam)) continue;
            boolean canSee = squad.members.contains(myName) || amISquadLeader;
            if (!canSee) continue;
            for (var rm : squad.rhombusMarkers) {
                long timeLeft = rm.expiryTick - time;
                if (timeLeft <= 0) continue;
                float alpha = Mth.clamp((float) timeLeft / 3600f, 0.1f, 1);
                int mx = toScreenX(rm.x, cx);
                int my = toScreenZ(rm.z, cz);
                if (!inMap(mx, my)) continue;
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1, 1, 1, markerAlpha(alpha));
                pushIconScale(g, mx, my);
                g.blit(rhombusTex, mx - 8, my - 8, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
                g.pose().pushPose();
                g.pose().translate(mx, my, 500);
                g.pose().scale(0.5F, 0.5F, 1);
                int displayNum = 0;
                for (int i = 0; i < teamSquadsForNums.size(); i++) {
                    if (teamSquadsForNums.get(i).id == squad.id) {
                        displayNum = i + 1;
                        break;
                    }
                }
                String numStr = String.valueOf(displayNum);
                int whiteWithAlpha = (int) (alpha * 255) << 24 | 0xFFFFFF;
                drawSquadNumber(g, f, numStr, -(f.width(numStr) / 2), -4, whiteWithAlpha);
                g.pose().popPose();
            }
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private void renderSquadPings(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        String myName = mc.player.getScoreboardName();
        WarfareWorldData.Squad mySquad = null;
        for (var s : ClientData.clientSquads) {
            if (s.members.contains(myName)) {
                mySquad = s;
                break;
            }
        }
        if (mySquad == null) return;
        long time = mc.level.getGameTime();
        boolean isSL = mySquad.leader.equals(myName);
        boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
        boolean isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
        if (mySquad.pingPos != null && time < mySquad.pingExpiry)
            drawPingOnMap(
                    g,
                    mySquad.pingPos,
                    com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/map_icons/ping_eye.png"),
                    cx,
                    cz,
                    bpp);
        if (mySquad.bravoPingPos != null && time < mySquad.bravoPingExpiry && (isSL || isBravo))
            drawPingOnMap(
                    g,
                    mySquad.bravoPingPos,
                    com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/map_icons/ping_eye_bravo.png"),
                    cx,
                    cz,
                    bpp);
        if (mySquad.charliePingPos != null && time < mySquad.charliePingExpiry && (isSL || isCharlie))
            drawPingOnMap(
                    g,
                    mySquad.charliePingPos,
                    com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/map_icons/ping_eye_charlie.png"),
                    cx,
                    cz,
                    bpp);
    }

    private void drawPingOnMap(GuiGraphics g, BlockPos pos, ResourceLocation icon, double cx, double cz, double bpp) {
        int px = toScreenX(pos.getX() + 0.5, cx);
        int py = toScreenZ(pos.getZ() + 0.5, cz);
        if (inMap(px, py)) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, markerAlpha(1F));
            pushIconScale(g, px, py);
            g.blit(icon, px - 6, py - 6, 0, 0, 12, 12, 12, 12);
            g.pose().popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private String getPlayerTeamStrict() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getTeam() != null) {
            String name = mc.player.getTeam().getName().toUpperCase();
            if (name.contains("BLUE")) return "BLUE";
            return name.contains("RED") ? "RED" : name;
        }
        return "NEUTRAL";
    }

    private boolean isSquadLeaderOrFTL(LocalPlayer player) {
        if (player == null) return false;
        String pName = player.getScoreboardName();
        for (var s : ClientData.clientSquads) {
            if (s.leader.equals(pName) || s.bravoLeader.equals(pName) || s.charlieLeader.equals(pName)) return true;
        }
        return false;
    }

    private void drawSquadNumber(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x - 1, y, -16777216, false);
        g.drawString(font, text, x + 1, y, -16777216, false);
        g.drawString(font, text, x, y - 1, -16777216, false);
        g.drawString(font, text, x, y + 1, -16777216, false);
        g.drawString(font, text, x, y, color, false);
    }

    /** Масштаб иконки вокруг её центра (настройка «Размер иконок»); парный popPose обязателен. */
    private static void pushIconScale(GuiGraphics g, float cx, float cy) {
        float s = WarfareUiSettings.MAP_ICON_SCALE.get();
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(s, s, 1F);
        g.pose().translate(-cx, -cy, 0);
    }

    /** Прозрачность меток/маркеров (настройка «Прозрачность меток»). */
    private static float markerAlpha(float a) {
        return a * WarfareUiSettings.MAP_MARKER_OPACITY.get();
    }

    private boolean isShowNicknamesHeld() {
        // настройка «Подписи имён»: всегда / никогда / по клавише (как было)
        switch (WarfareUiSettings.MAP_NAMES.get()) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            default:
                break;
        }
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        Key nickKey = ModKeyBindings.SHOW_NICKNAMES_KEY.getKey();
        return nickKey.getType() == Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, nickKey.getValue()) == 1
                : InputConstants.isKeyDown(window, nickKey.getValue());
    }

    public MapMarker hitMarker(int mx, int my, double cx, double cz) {
        for (var m : com.pigeostudios.pwp.warfare.client.MarkerClientCache.getAll()) {
            int px = toScreenX(m.pos.getX() + 0.5, cx), py = toScreenZ(m.pos.getZ() + 0.5, cz);
            if (Math.abs(mx - px) < 12 && Math.abs(my - py) < 12) return m;
        }
        return null;
    }

    private void drawMarkers(GuiGraphics g, double cx, double cz) {
        var f = Minecraft.getInstance().font;
        long now = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime()
                : 0;
        for (var m : com.pigeostudios.pwp.warfare.client.MarkerClientCache.getAll()) {
            int px = toScreenX(m.pos.getX() + 0.5, cx), py = toScreenZ(m.pos.getZ() + 0.5, cz);
            if (!inMap(px, py)) continue;
            float maxTicks = "enemy".equals(m.team) ? 6000f : 12000f;
            float t = (now - m.createdAt) / maxTicks;
            float alpha;
            if (t < 0.2f) alpha = 1.0f;
            else alpha = Math.max(0, 1.0f - (float) Math.pow((t - 0.2f) / 0.8f, 1.5f));
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, markerAlpha(alpha * 0.9F));
            pushIconScale(g, px, py);
            g.blit(icon(m.iconType, m.team), px - 10, py - 10, 0, 0, 20, 20, 20, 20);
            g.pose().popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (("hat".equals(m.iconType) || "rally".equals(m.iconType)) && "squad".equals(m.team)) {
                var p = Minecraft.getInstance().player;
                double dist = Math.sqrt(p.distanceToSqr(m.pos.getX() + 0.5, p.getY(), m.pos.getZ() + 0.5));
                String d = distStr(dist);
                int tw = f.width(d);
                g.fill(px - tw / 2 - 2, py + 11, px + tw / 2 + 3, py + 22, (int) (0xAA * alpha) << 24 | 0x000000);
                g.drawString(f, d, px - tw / 2, py + 13, (int) (0xFF * alpha) << 24 | 0xFFFFFF, false);
            }
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    static final float PATH_MIN_LENGTH = 50.0F;
    static final float PATH_MAX_LENGTH = 1500.0F;

    public float pathAnimProgress = 1.0F;
    public float pathFadeAlpha = 1.0F;
    private long animStart = -1;

    public void startPathAnim() {
        animStart = System.currentTimeMillis();
        pathAnimProgress = 0;
    }

    public void tickAnim() {
        if (animStart < 0) return;
        pathAnimProgress = Math.min(1, (System.currentTimeMillis() - animStart) / 1000.0F);
        if (pathAnimProgress >= 1) animStart = -1;
    }

    private float pathFadeFor(List<PathPoint> pts) {
        if (pts == null || pts.isEmpty()) return 1.0f;
        long now = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime()
                : 0;
        int key = System.identityHashCode(pts);
        Long created = com.pigeostudios.pwp.warfare.client.PathCache.pathCreatedAt.get(key);
        if (created == null) {
            // Check if this is a server path with createdAt in ServerPath record
            for (var ep : com.pigeostudios.pwp.warfare.client.PathCache.serverPaths.entrySet()) {
                if (ep.getValue().points() == pts) {
                    created = ep.getValue().createdAt();
                    break;
                }
            }
        }
        if (created == null || created <= 0) return 1.0f;
        float elapsed = now - created;
        float maxTicks = 12000f;
        if (elapsed > maxTicks) {
            return 0f;
        }
        float t = elapsed / maxTicks;
        if (t < 0.2f) return 1.0f;
        return Math.max(0, 1.0f - (float) Math.pow((t - 0.2f) / 0.8f, 1.5f));
    }

    /** Прозрачность путей (настройка «Прозрачность путей»); вне drawPath — 1 (превью). */
    private float pathOpacity = 1F;

    private void drawPath(GuiGraphics g, double cx, double cz) {
        pathOpacity = WarfareUiSettings.MAP_PATH_OPACITY.get();
        pathFadeAlpha = 1.0f;
        drawGroup(g, cx, cz, pathGroups(), 0xFF44FF44);
        drawGroup(g, cx, cz, pathGroupsRed(), 0xFFFF4444);
        drawGroup(g, cx, cz, pathGroupsYellow(), 0xFFFFDD00);
        drawSquadPaths(g, cx, cz);
        // Server-synced paths (no animation, full visibility)
        pathFadeAlpha = 1.0F;
        float savedAnim = pathAnimProgress;
        pathAnimProgress = 1.0F;
        for (var entry : com.pigeostudios.pwp.warfare.client.PathCache.serverPaths.entrySet()) {
            var sp = entry.getValue();
            if ("cmd_squads".equals(sp.type())) {
                // Temporarily add to squad paths map for rendering
                var saved = pathGroupsSquad().put(sp.squadNum(), sp.points());
                drawSquadPaths(g, cx, cz);
                if (saved != null) pathGroupsSquad().put(sp.squadNum(), saved);
                else pathGroupsSquad().remove(sp.squadNum());
            } else {
                int c = "squad".equals(sp.type()) ? 0xFF44FF44 : "enemy".equals(sp.type()) ? 0xFFFF4444 : 0xFFFFDD00;
                drawGroup(g, cx, cz, List.of(sp.points()), c);
            }
        }
        pathAnimProgress = savedAnim;
        pathOpacity = 1F;
    }

    private void drawSquadPaths(GuiGraphics g, double cx, double cz) {
        var font = Minecraft.getInstance().font;
        for (var entry : pathGroupsSquad().entrySet()) {
            int pathKey = entry.getKey();
            int squadNum = com.pigeostudios.pwp.warfare.client.PathCache.squadNumForPath.getOrDefault(pathKey, pathKey);
            var pts = entry.getValue();
            if (pts.size() < 2) continue;
            pathFadeAlpha = pathFadeFor(pts);
            if (pathFadeAlpha <= 0.001f) continue;

            int col = 0xFFFFDD00;
            int lineCol = (col & 0x00FFFFFF) | 0xE6000000;
            float r = ((col >> 16) & 0xFF) / 255.0F;
            float gr = ((col >> 8) & 0xFF) / 255.0F;
            float b = (col & 0xFF) / 255.0F;

            int n = pts.size();
            double[] segX = new double[n], segY = new double[n];
            double[] segStarts = new double[n - 1];
            double totalLen = 0;
            for (int i = 0; i < n; i++) {
                segX[i] = toScreenX(pts.get(i).x, cx);
                segY[i] = toScreenZ(pts.get(i).z, cz);
                if (i > 0) {
                    segStarts[i - 1] = totalLen;
                    totalLen += Math.hypot(segX[i] - segX[i - 1], segY[i] - segY[i - 1]);
                }
            }
            double prog = pathAnimProgress * totalLen;

            for (int i = 0; i < n - 1; i++) {
                if (prog <= segStarts[i]) break;
                double lx1 = segX[i], ly1 = segY[i];
                if (i == 0 && n > 1) {
                    double dx = segX[1] - segX[0], dy = segY[1] - segY[0];
                    double segLen = Math.hypot(dx, dy);
                    if (segLen > 11) {
                        lx1 += dx * 11 / segLen;
                        ly1 += dy * 11 / segLen;
                    }
                }
                double t = Math.min(
                        1,
                        (prog - segStarts[i])
                                / (segStarts[i]
                                        + Math.hypot(segX[i + 1] - segX[i], segY[i + 1] - segY[i])
                                        - segStarts[i]));
                int lx2 = (int) (lx1 + (segX[i + 1] - lx1) * t);
                int ly2 = (int) (ly1 + (segY[i + 1] - ly1) * t);
                var pose = g.pose();
                pose.pushPose();
                pose.translate((int) lx1, (int) ly1, 200);
                float angle = (float) Math.toDegrees(Math.atan2(ly2 - (int) ly1, lx2 - (int) lx1));
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                float clen = (float) Math.hypot(lx2 - lx1, ly2 - ly1);
                g.fill(0, -1, (int) clen, 2, UIDraw.fade(0x66000000, pathOpacity));
                g.fill(0, 0, (int) clen, 1, UIDraw.fade(lineCol, pathOpacity));
                pose.popPose();
                if (t < 1) break;
            }

            if (pathAnimProgress > 0.05F) {
                int sx = (int) segX[0], sy = (int) segY[0];
                RenderSystem.setShaderColor(r, gr, b, 0.9F * pathFadeAlpha * pathOpacity);
                g.blit(ICON_CIRCLE, sx - 9, sy - 9, 18, 18, 0, 0, 16, 16, 16, 16);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                String numStr = String.valueOf(squadNum);
                int tw = font.width(numStr);
                int tx = sx - tw / 2, ty = sy - 4;
                g.drawString(font, numStr, tx - 1, ty, 0xFF000000, false);
                g.drawString(font, numStr, tx + 1, ty, 0xFF000000, false);
                g.drawString(font, numStr, tx, ty - 1, 0xFF000000, false);
                g.drawString(font, numStr, tx, ty + 1, 0xFF000000, false);
                g.drawString(font, numStr, tx, ty, 0xFFFFFFFF, false);
            }

            if (pathAnimProgress > 0.8F) {
                int ex2 = (int) segX[n - 1], ey2 = (int) segY[n - 1];
                int px = (int) segX[n - 2], py = (int) segY[n - 2];
                float edx = ex2 - px, edy = ey2 - py;
                float elen = (float) Math.hypot(edx, edy);
                if (elen >= 5) {
                    var pose = g.pose();
                    pose.pushPose();
                    pose.translate(ex2, ey2, 200);
                    float eAngle = (float) Math.toDegrees(Math.atan2(edy, edx)) + 90;
                    pose.mulPose(Axis.ZP.rotationDegrees(eAngle));
                    RenderSystem.setShaderColor(r, gr, b, 0.9F * pathFadeAlpha * pathOpacity);
                    g.blit(ICON_SELF, -9, -9, 18, 18, 0, 0, 16, 16, 16, 16);
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    pose.popPose();
                }
            }
        }
    }

    private void drawGroup(GuiGraphics g, double cx, double cz, List<List<PathPoint>> groups, int col) {
        float r = ((col >> 16) & 0xFF) / 255.0F;
        float gr2 = ((col >> 8) & 0xFF) / 255.0F;
        float b = (col & 0xFF) / 255.0F;
        int lineCol = (col & 0x00FFFFFF) | 0xE6000000;
        for (var pts : groups) {
            if (pts.size() < 2) continue;
            pathFadeAlpha = pathFadeFor(pts);
            if (pathFadeAlpha <= 0.001f) continue;
            double totalLen = 0;
            int n = pts.size();
            double[] segX = new double[n], segY = new double[n];
            double[] segStarts = new double[n - 1];
            for (int i = 0; i < n; i++) {
                segX[i] = toScreenX(pts.get(i).x, cx);
                segY[i] = toScreenZ(pts.get(i).z, cz);
                if (i > 0) {
                    segStarts[i - 1] = totalLen;
                    totalLen += Math.hypot(segX[i] - segX[i - 1], segY[i] - segY[i - 1]);
                }
            }
            double prog = pathAnimProgress * totalLen;

            // Draw lines up to progress, with 14px gap from circle center
            for (int i = 0; i < n - 1; i++) {
                if (prog <= segStarts[i]) break;
                double lx1 = segX[i], ly1 = segY[i];
                // Offset first line start 14px from circle center
                if (i == 0 && n > 1) {
                    double dx = segX[1] - segX[0], dy = segY[1] - segY[0];
                    double segLen = Math.hypot(dx, dy);
                    if (segLen > 11) {
                        lx1 += dx * 11 / segLen;
                        ly1 += dy * 11 / segLen;
                    }
                }
                double lSegLen = Math.hypot(segX[i + 1] - lx1, segY[i + 1] - ly1);
                if (prog <= segStarts[i]) break;
                double t = Math.min(
                        1,
                        (prog - segStarts[i])
                                / (segStarts[i]
                                        + Math.hypot(segX[i + 1] - segX[i], segY[i + 1] - segY[i])
                                        - segStarts[i]));
                int lx2 = (int) (lx1 + (segX[i + 1] - lx1) * t);
                int ly2 = (int) (ly1 + (segY[i + 1] - ly1) * t);
                drawPathLine(g, (int) lx1, (int) ly1, lx2, ly2, lineCol);
                if (t < 1) break;
            }

            // Start circle (appears at progress > 5% of first segment)
            if (pathAnimProgress > 0.05F) {
                int sx = (int) segX[0], sy = (int) segY[0];
                RenderSystem.setShaderColor(r, gr2, b, 0.9F * pathFadeAlpha * pathOpacity);
                g.blit(ICON_CIRCLE, sx - 8, sy - 8, 16, 16, 0, 0, 16, 16, 16, 16);
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }

            // End arrow (appears at progress > 80%)
            if (pathAnimProgress > 0.8F) {
                int ex2 = (int) segX[n - 1], ey2 = (int) segY[n - 1];
                int px = (int) segX[n - 2], py = (int) segY[n - 2];
                float edx = ex2 - px, edy = ey2 - py;
                float elen = (float) Math.hypot(edx, edy);
                if (elen >= 5) {
                    var pose = g.pose();
                    pose.pushPose();
                    pose.translate(ex2, ey2, 200);
                    float eAngle = (float) Math.toDegrees(Math.atan2(edy, edx)) + 90;
                    pose.mulPose(Axis.ZP.rotationDegrees(eAngle));
                    RenderSystem.setShaderColor(r, gr2, b, 0.9F * pathFadeAlpha * pathOpacity);
                    g.blit(ICON_SELF, -8, -8, 16, 16, 0, 0, 16, 16, 16, 16);
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    pose.popPose();
                }
            }
        }
    }

    private void drawPathLine(GuiGraphics g, int x1, int y1, int x2, int y2, int col) {
        float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x1, y1, 200);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        g.fill(0, -1, (int) len, 2, UIDraw.fade(0x66000000, pathOpacity));
        g.fill(0, 0, (int) len, 1, UIDraw.fade(col, pathOpacity));
        pose.popPose();
    }

    private void drawPreview(GuiGraphics g, double cx, double cz) {
        if (previewStart == null || previewEnd == null) return;
        int x1 = toScreenX(previewStart.x, cx), y1 = toScreenZ(previewStart.z, cz);
        int x2 = toScreenX(previewEnd.x, cx), y2 = toScreenZ(previewEnd.z, cz);
        drawPathLine(g, x1, y1, x2, y2, 0x55FFFFFF);
        RenderSystem.setShaderColor(1, 1, 1, 0.5F);
        g.blit(ICON_CIRCLE, x1 - 8, y1 - 8, 16, 16, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static String getKP(double wx, double wz) {
        return MapView.kp(wx, wz);
    }

    public static String distStr(double m) {
        if (m <= 30) {
            int d = (int) Math.round(m);
            return Math.max(1, d - 1) + "-" + (d + 1) + "m";
        }
        if (m <= 100) {
            int d = (int) (Math.round(m / 5) * 5);
            return Math.max(1, d - 5) + "-" + (d + 5) + "m";
        }
        if (m <= 300) {
            int d = (int) (Math.round(m / 10) * 10);
            return Math.max(1, d - 10) + "-" + (d + 10) + "m";
        }
        int d = (int) (Math.round(m / 50) * 50);
        return Math.max(1, d - 50) + "-" + (d + 50) + "m";
    }

    private int toScreenX(double wx, double cx) {
        return (int) (mapX + mapWidth / 2.0 + (wx - cx) / effectiveScale());
    }

    private int toScreenZ(double wz, double cz) {
        return (int) (mapY + mapHeight / 2.0 + (wz - cz) / effectiveScale());
    }

    private boolean inMap(int sx, int sy) {
        return sx >= mapX && sx <= mapX + mapWidth && sy >= mapY && sy <= mapY + mapHeight;
    }

    public boolean inMap(double mx, double my) {
        return inMap((int) mx, (int) my);
    }

    private ResourceLocation icon(String type, String team) {
        String s = "enemy".equals(team) ? "_r" : "team".equals(team) ? "_y" : "_g";
        return TEX.computeIfAbsent(
                type + s, k -> new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + k + ".png"));
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) return false;
        if (btn == 0) {
            isDraggingMap = true;
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        return false;
    }

    public void mouseReleased(int btn) {
        if (btn == 0) isDraggingMap = false;
    }

    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (!isDraggingMap || btn != 0) return false;
        panX -= (mx - lastMouseX) * effectiveScale();
        panZ -= (my - lastMouseY) * effectiveScale();
        lastMouseX = mx;
        lastMouseY = my;
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my)) return false;
        if (localScale > 0) {
            // зум вокруг курсора: мировая точка под мышью остаётся на месте
            double cxs = mapX + mapWidth / 2.0, cys = mapY + mapHeight / 2.0;
            double ns = delta > 0 ? localScale / LOCAL_ZOOM_STEP : localScale * LOCAL_ZOOM_STEP;
            ns = Math.max(localMin, Math.min(localMax, ns));
            panX += (mx - cxs) * (localScale - ns);
            panZ += (my - cys) * (localScale - ns);
            localScale = ns;
            return true;
        }
        ClientData.zoomMap(delta);
        return true;
    }

    static {
        VEHICLE_ICONS.put("APC", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/apc.png"));
        VEHICLE_ICONS.put("TANK", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/tank.png"));
        VEHICLE_ICONS.put("HELICOPTER", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/helicopter.png"));
        VEHICLE_ICONS.put(
                "CAS Helicopter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_helicopter.png"));
        VEHICLE_ICONS.put("CAS Fighter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_fighter.png"));
        VEHICLE_ICONS.put(
                "Combat Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/combat_vehicle.png"));
        VEHICLE_ICONS.put(
                "Infantry Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/infantry_vehicle.png"));
        VEHICLE_ICONS.put(
                "Supply Truck", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_truck.png"));
        VEHICLE_ICONS.put(
                "Supply Helicopter",
                new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_helicopter.png"));
        VEHICLE_ICONS.put("DEFAULT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/default.png"));
        VEHICLE_ICONS.put("Static ZU", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/static_zu.png"));
        VEHICLE_ICONS.put("Mobile ZU", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/mobile_zu.png"));
        VEHICLE_ICONS.put("BOAT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/boat.png"));
        VEHICLE_ICONS.put("Motorcycle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/motorcycle.png"));
        VEHICLE_ICONS.put(
                "Light Supply", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/light_supply.png"));
        VEHICLE_ICONS.put(
                "Heavy Supply", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/heavy_supply.png"));
        VEHICLE_ICONS.put("SPG", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/spg.png"));
        VEHICLE_ICONS.put("Mine", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/skull_marker.png"));
    }
}

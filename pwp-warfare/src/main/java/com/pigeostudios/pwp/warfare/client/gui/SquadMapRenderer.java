package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pwp.coreclient.gui.theme.PWPTheme;
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

    public int mapX, mapY, mapWidth, mapHeight;
    public String selectedSpawnId = "";
    // Минимальный скейл (блоков на пиксель) для вписывания всей карты в область рендера.
    // 0 = использовать глобальный ClientData.mapScale (поведение M-меню).
    public double fitScale = 0.0;

    private double panX, panZ;
    private boolean isDraggingMap;
    private double lastMouseX, lastMouseY;

    public PathPoint previewStart, previewEnd;

    public List<List<PathPoint>> pathGroups() { return com.pigeostudios.pwp.warfare.client.PathCache.groups; }
    public List<List<PathPoint>> pathGroupsRed() { return com.pigeostudios.pwp.warfare.client.PathCache.groupsRed; }
    public List<List<PathPoint>> pathGroupsYellow() { return com.pigeostudios.pwp.warfare.client.PathCache.groupsYellow; }
    public Map<Integer, List<PathPoint>> pathGroupsSquad() { return com.pigeostudios.pwp.warfare.client.PathCache.groupsSquad; }

    private static final Map<String, ResourceLocation> MAP_ICONS_CACHE = new HashMap<>();
    private static final Map<String, ResourceLocation> TEX = new HashMap<>();

    private static final ResourceLocation MATS_ICON = new ResourceLocation("pwpwarfare", "textures/gui/mats_icon.png");
    private static final ResourceLocation MARKER_MOVE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move.png");
    private static final ResourceLocation MARKER_ATTACK = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_attack.png");
    private static final ResourceLocation MARKER_DEFEND = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_defend.png");
    private static final ResourceLocation MARKER_BUILD = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_build.png");
    private static final ResourceLocation HUB_ICON = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_icon.png");
    private static final ResourceLocation HUB_ICON_SELECTED = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/hub_icon_selected.png");
    private static final ResourceLocation RALLY_ICON = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_icon.png");
    private static final ResourceLocation RALLY_ICON_SELECTED = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/rally_icon_selected.png");
    private static final ResourceLocation MAIN_BASE_ICON = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/main_base.png");
    private static final ResourceLocation MAIN_BASE_ICON_SELECTED = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/main_base_selected.png");
    private static final ResourceLocation STATION_ICON = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/vehicle_station.png");
    private static final ResourceLocation FLAG_NEUTRAL = new ResourceLocation("pwpwarfare", "textures/gui/flags/neutral.png");
    private static final ResourceLocation ICON_CIRCLE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/player_circle.png");
    private static final ResourceLocation ICON_PLUS = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/medic_plus.png");
    private static final ResourceLocation ICON_SELF = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/player_self.png");
    private static final ResourceLocation ICON_COMPASS = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/compass.png");
    private static final ResourceLocation ICON_OBJ_ATTACK = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/objective_attack.png");
    private static final ResourceLocation ICON_OBJ_DEFEND = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/objective_defend.png");
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

    public double effectiveScale() {
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

    public void render(GuiGraphics g, int mx, int my, float pt) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        double bpp = effectiveScale();
        if (bpp <= 0) bpp = 1.0;
        double cx = p.getX() + panX;
        double cz = p.getZ() + panZ;

        renderTopBar(g, mx, my, cx, cz);
        drawFrame(g);
        g.fill(mapX, mapY, mapX + mapWidth, mapY + mapHeight, 0xFF1A1E1A);
        g.enableScissor(mapX, mapY, mapX + mapWidth, mapY + mapHeight);

        renderMapTexture(g, cx, cz, bpp);
        drawGrid(g, cx, cz);
        drawLabels(g, cx, cz);
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

        g.disableScissor();
    }

    private void renderMapTexture(GuiGraphics g, double cx, double cz, double bpp) {
        ResourceLocation tex = getCurrentMapTexture();
        int s = ClientData.mapSizeBlocks;
        float drawX = (float)(mapX + mapWidth / 2.0 + (ClientData.mapCenterX - s / 2.0 - cx) / bpp);
        float drawY = (float)(mapY + mapHeight / 2.0 + (ClientData.mapCenterZ - s / 2.0 - cz) / bpp);
        int texSize = (int)(s / bpp);
        setFilter(tex, true);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(tex, (int)drawX, (int)drawY, texSize, texSize, 0, 0, 1024, 1024, 1024, 1024);
        setFilter(tex, false);
    }

    private ResourceLocation getCurrentMapTexture() {
        String img = ClientData.currentMapImage;
        if (img == null || img.isEmpty()) img = "map1";
        return MAP_ICONS_CACHE.computeIfAbsent(img,
            k -> new ResourceLocation("pwpwarfare", "textures/gui/maps/" + k + ".png"));
    }

    private void setFilter(ResourceLocation tex, boolean smooth) {
        var t = Minecraft.getInstance().getTextureManager().getTexture(tex);
        if (t != null) t.setFilter(smooth, false);
    }

    private void renderTopBar(GuiGraphics g, int mx, int my, double cx, double cz) {
        var f = Minecraft.getInstance().font;
        String kp = "--";
        if (inMap(mx, my)) {
        double wx = cx + (mx - (mapX + mapWidth / 2.0)) * effectiveScale();
        double wz = cz + (my - (mapY + mapHeight / 2.0)) * effectiveScale();
            kp = getKP(wx, wz);
        }
        String zt = String.format("Z:%.1f", effectiveScale());
        String text = kp + "  " + zt;
        int tw = f.width(text) + 8;
        int th = 14;
        g.fill(mapX + 2, mapY + 2, mapX + 2 + tw, mapY + 2 + th, 0xAA06080A);
        g.drawString(f, text, mapX + 4, mapY + 4, 0xFFFFFF, false);
    }

    private void drawFrame(GuiGraphics g) {
        int c = 0xFF555555;
        g.hLine(mapX - 1, mapX + mapWidth, mapY - 1, c);
        g.hLine(mapX - 1, mapX + mapWidth, mapY + mapHeight, c);
        g.vLine(mapX - 1, mapY - 1, mapY + mapHeight, c);
        g.vLine(mapX + mapWidth, mapY - 1, mapY + mapHeight, c);
    }

    private void drawGrid(GuiGraphics g, double cx, double cz) {
        double halfX = mapWidth / 2.0 * effectiveScale();
        double halfZ = mapHeight / 2.0 * effectiveScale();
        long xS = (long)(cx - halfX) - 1200;
        long xE = (long)(cx + halfX) + 1200;
        long zS = (long)(cz - halfZ) - 1200;
        long zE = (long)(cz + halfZ) + 1200;
        long gOriginX = Math.floorDiv((long)(ClientData.mapCenterX - ClientData.mapSizeBlocks / 2.0), 300L) * 300L;
        long gOriginZ = Math.floorDiv((long)(ClientData.mapCenterZ - ClientData.mapSizeBlocks / 2.0), 300L) * 300L;
        gridLines(g, 300, 2, 0x30FFFFFF, xS, xE, zS, zE, cx, cz, gOriginX, gOriginZ);
        if (effectiveScale() <= 2.2) {
            int a100 = (int)(24 * Math.min(1, Math.max(0, (2.2 - effectiveScale()) / 0.4)));
            gridLines(g, 100, 1, (a100 << 24) | 0xFFFFFF, xS, xE, zS, zE, cx, cz, gOriginX, gOriginZ);
        }
    }

    private void gridLines(GuiGraphics g, long step, int w, int col, long xS, long xE, long zS, long zE, double cx, double cz, long gOriginX, long gOriginZ) {
        long s = Math.floorDiv(xS - gOriginX, step) * step + gOriginX;
        for (long v = s; v < xE; v += step) {
            int sx = toScreenX(v, cx);
            if (sx >= mapX && sx <= mapX + mapWidth) g.fill(sx, mapY, sx + w, mapY + mapHeight, col);
        }
        s = Math.floorDiv(zS - gOriginZ, step) * step + gOriginZ;
        for (long v = s; v < zE; v += step) {
            int sy = toScreenZ(v, cz);
            if (sy >= mapY && sy <= mapY + mapHeight) g.fill(mapX, sy, mapX + mapWidth, sy + w, col);
        }
    }

    private void drawLabels(GuiGraphics g, double cx, double cz) {
        drawTopLabels(g, cx, cz);
        drawLeftLabels(g, cx, cz);
    }

    private void drawTopLabels(GuiGraphics g, double cx, double cz) {
        var f = Minecraft.getInstance().font;
        long gOriginX = Math.floorDiv((long)(ClientData.mapCenterX - ClientData.mapSizeBlocks / 2.0), 300L) * 300L;
        double halfX = mapWidth / 2.0 * effectiveScale();
        long s = Math.floorDiv((long)(cx - halfX) - gOriginX, 300L);
        long e = Math.floorDiv((long)(cx + halfX) - gOriginX, 300L);
        for (long i = s; i <= e; i++) {
            int px = toScreenX(gOriginX + i * 300L + 150, cx);
            if (px < mapX + 10 || px > mapX + mapWidth - 10) continue;
            String t = toAlpha(i);
            if ("?".equals(t)) continue;
            int bw = f.width(t) + 6;
            g.fill(px - bw / 2, mapY + 1, px + bw / 2, mapY + 13, 0xCC000000);
            g.drawString(f, t, px - f.width(t) / 2, mapY + 3, 0xCCFFFFFF, false);
        }
    }

    private void drawLeftLabels(GuiGraphics g, double cx, double cz) {
        var f = Minecraft.getInstance().font;
        long gOriginZ = Math.floorDiv((long)(ClientData.mapCenterZ - ClientData.mapSizeBlocks / 2.0), 300L) * 300L;
        double halfZ = mapHeight / 2.0 * effectiveScale();
        long s = Math.floorDiv((long)(cz - halfZ) - gOriginZ, 300L);
        long e = Math.floorDiv((long)(cz + halfZ) - gOriginZ, 300L);
        for (long i = s; i <= e; i++) {
            int py = toScreenZ(gOriginZ + i * 300L + 150, cz);
            if (py < mapY + 10 || py > mapY + mapHeight - 10) continue;
            String t = String.valueOf(i + 1);
            int bw = f.width(t) + 6;
            g.fill(mapX + 2, py - 6, mapX + 2 + bw, py + 6, 0xCC000000);
            g.drawString(f, t, mapX + 5, py - 4, 0xCCFFFFFF, false);
        }
    }

    private void renderLatticeLines(GuiGraphics g, double cx, double cz, double bpp) {
        if (ClientData.allCapturePoints == null || ClientData.allCapturePoints.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String currentDim = mc.level.dimension().location().toString();
        boolean isRed = mc.player != null && mc.player.getTeam() != null && mc.player.getTeam().getName().equalsIgnoreCase("Red");
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
        int lineColor = 1728053247;
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
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x1, y1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float)Math.toDegrees(Math.atan2(dy, dx))));
        g.fill(0, 0, (int)len, 1, color);
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
                if (flagTex == null) { flagTex = FLAG_NEUTRAL; tintColor = -11184641; useTint = true; }
            } else if (teamToRender.equals("RED")) {
                flagTex = getFlagTexture(ClientData.RED_FACTION);
                if (flagTex == null) { flagTex = FLAG_NEUTRAL; tintColor = -43691; useTint = true; }
            }
            float r = 1, g2 = 1, b = 1;
            if (useTint) { r = (tintColor >> 16 & 0xFF) / 255f; g2 = (tintColor >> 8 & 0xFF) / 255f; b = (tintColor & 0xFF) / 255f; }
            RenderSystem.setShaderColor(r, g2, b, alpha);
            setFilter(flagTex, true);
            g.blit(flagTex, pX - 8, pY - 4, 16, 9, 0, 0, 64, 36, 64, 36);
            setFilter(flagTex, false);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            var f = PWPTheme.Fonts.display();
            g.pose().pushPose();
            g.pose().translate(pX, pY + 7, 101);
            g.pose().scale(0.6F, 0.6F, 1);
            g.drawCenteredString(f, cp.name, 0, 0, 0xFFFFFF);
            g.pose().popPose();
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private ResourceLocation getFlagTexture(String faction) {
        return faction != null && !faction.equalsIgnoreCase("none")
            ? new ResourceLocation("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png") : null;
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

    private void drawMainBaseIcon(GuiGraphics g, BlockPos pos, double cx, double cz, double bpp, String faction, int fallbackColor) {
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
            float sx = (float)toScreenX(strike.pos.getX() + 0.5, cx);
            float sy = (float)toScreenZ(strike.pos.getZ() + 0.5, cz);
            drawSmoothCircle(g, sx, sy, radius / (float)bpp, -65536);
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
            float angle = i * (float)(Math.PI * 2) / seg;
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
            float sx = (float)toScreenX(hub.pos.getX() + 0.5, cx);
            float sy = (float)toScreenZ(hub.pos.getZ() + 0.5, cz);
            g.pose().pushPose();
            g.pose().translate(0, 0, 50);
            drawSmoothCircle(g, sx, sy, 50 / (float)bpp, 1627389951);
            int teamCircleColor = hub.team.equalsIgnoreCase("BLUE") ? -2141891073 : -2130750123;
            drawSmoothCircle(g, sx, sy, 150 / (float)bpp, teamCircleColor);
            g.pose().popPose();
            if (inMap((int)sx, (int)sy)) {
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
                int matsX = (int)sx + 8;
                int matsY = (int)sy - 2;
                g.pose().pushPose();
                g.pose().translate(0, 0, 165);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                setFilter(MATS_ICON, true);
                g.blit(MATS_ICON, matsX, matsY, 8, 8, 0, 0, 16, 16, 16, 16);
                setFilter(MATS_ICON, false);
                g.drawString(PWPTheme.Fonts.display(), matsText, matsX + 10, matsY + 1, -22016, false);
                g.pose().popPose();
            }
        }

        for (var supply : ClientData.clientvehicleStations) {
            if (!supply.team.equalsIgnoreCase(myTeam) && !isObserver) continue;
            float sx = (float)toScreenX(supply.pos.getX() + 0.5, cx);
            float sy = (float)toScreenZ(supply.pos.getZ() + 0.5, cz);
            if (inMap((int)sx, (int)sy)) {
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
            float sx = (float)toScreenX(squad.rallyPos.getX() + 0.5, cx);
            float sy = (float)toScreenZ(squad.rallyPos.getZ() + 0.5, cz);
            if (inMap((int)sx, (int)sy)) {
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
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableBlend();
            g.blit(icon, -6, -6, 12, 12, 0, 0, 16, 16, 16, 16);
            g.pose().popPose();
            setFilter(icon, false);
        }
    }

    private static String factionName(String f) {
        if (f == null || f.isEmpty() || "none".equals(f)) return null;
        return switch (f.toLowerCase()) {
            case "usa" -> "\u0421\u0428\u0410";
            case "russia", "russian" -> "\u0420\u043E\u0441\u0441\u0438\u044F";
            case "british", "uk", "gb" -> "\u0412\u0435\u043B\u0438\u043A\u043E\u0431\u0440\u0438\u0442\u0430\u043D\u0438\u044F";
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

    public void renderVehicleLegend(GuiGraphics g, int panelX, int panelY, int panelW, int mx, int my) {
        if (ClientData.clientSpawners == null || ClientData.clientSpawners.isEmpty()) return;
        var f = Minecraft.getInstance().font;
        int currentY = panelY + 4;
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF555555);
        g.drawString(f, "\u0422\u0415\u0425\u041D\u0418\u041A\u0410", panelX + 4, currentY, 0xFFCCCCCC, false);
        currentY += 12;

        String hoverType = null;
        String hoverTeam = null;
        int hoverY = 0;
        int rowH = 12;

        for (String team : new String[]{"BLUE", "RED"}) {
            boolean hasAny = false;
            java.util.Map<String, int[]> groups = new java.util.LinkedHashMap<>();
            java.util.Map<String, Integer> penalties = new java.util.LinkedHashMap<>();
            java.util.Map<String, Boolean> allSpawned = new java.util.LinkedHashMap<>();

            for (var s : ClientData.clientSpawners) {
                if (!s.team.equalsIgnoreCase(team)) continue;
                int[] counts = groups.computeIfAbsent(s.type, k -> new int[2]);
                counts[1]++; // total
                if (s.isAlive) counts[0]++; // alive
                penalties.putIfAbsent(s.type, s.ticketPenalty);
                allSpawned.merge(s.type, s.hasSpawnedOnce, Boolean::logicalAnd);
            }

            for (var entry : groups.entrySet()) {
                String type = entry.getKey();
                int[] counts = entry.getValue();
                int alive = counts[0], total = counts[1];
                int penalty = penalties.getOrDefault(type, 0);
                boolean allHaveSpawned = allSpawned.getOrDefault(type, false);

                if (!hasAny) {
                    int teamColor = team.equals("BLUE") ? 0xFF4488FF : 0xFFFF4444;
                    String faction = team.equals("BLUE") ? com.pigeostudios.pwp.warfare.client.ClientData.BLUE_FACTION : com.pigeostudios.pwp.warfare.client.ClientData.RED_FACTION;
                    String displayName = factionName(faction);
                    if (displayName == null || displayName.isEmpty() || "none".equals(faction))
                        displayName = team.equals("BLUE") ? "\u0421\u0418\u041D\u0418\u0415" : "\u041A\u0420\u0410\u0421\u041D\u042B\u0415";
                    g.drawString(f, displayName, panelX + 4, currentY, teamColor, false);
                    currentY += 10;
                    hasAny = true;
                }

                int rowBg = 0;
                if (mx >= panelX && mx <= panelX + panelW && my >= currentY && my < currentY + rowH) {
                    rowBg = 0x44FFFFFF;
                    hoverType = type;
                    hoverTeam = team;
                    hoverY = currentY;
                }
                if (rowBg != 0) g.fill(panelX, currentY, panelX + panelW, currentY + rowH, rowBg);

                ResourceLocation icon = VEHICLE_ICONS.getOrDefault(type, VEHICLE_ICONS.get("DEFAULT"));
                setFilter(icon, true);
                g.blit(icon, panelX + 4, currentY + 1, 10, 10, 0, 0, 16, 16, 16, 16);
                setFilter(icon, false);

                String name = type;
                int maxNameW = panelW - 70;
                if (f.width(name) > maxNameW) {
                    name = f.plainSubstrByWidth(name, maxNameW - 4) + "..";
                }
                g.drawString(f, name, panelX + 18, currentY + 2, 0xFFFFFF, false);

                String countStr = alive + "/" + total;
                int countColor;
                if (alive == 0) {
                    countColor = allHaveSpawned ? 0xFFFF4444 : 0xFF4488FF;
                } else {
                    countColor = alive < total ? 0xFFFFAA00 : 0xFF88FF88;
                }
                g.drawString(f, countStr, panelX + panelW - f.width(countStr) - 4, currentY + 2, countColor, false);

                currentY += rowH;
            }
        }

        if (hoverType != null) {
            var p2 = Minecraft.getInstance().player;
            String myTeam = p2 != null && p2.getTeam() != null ? p2.getTeam().getName() : "";
            boolean isMyTeam = hoverTeam != null && myTeam.equalsIgnoreCase(hoverTeam);
            long currentTick = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
            java.util.List<String> timerLines = new java.util.ArrayList<>();
            if (isMyTeam) {
                for (var s : ClientData.clientSpawners) {
                    if (s.team.equalsIgnoreCase(hoverTeam) && s.type.equals(hoverType) && !s.isAlive && s.targetSpawnTick > currentTick) {
                        long sec = (s.targetSpawnTick - currentTick) / 20;
                        String timeStr = sec >= 60 ? (sec / 60) + "\u043C " + (sec % 60) + "\u0441" : sec + "\u0441";
                        String label = s.hasSpawnedOnce ? "\u0420\u0435\u0441\u043F\u0430\u0432\u043D" : "\u041F\u043E\u044F\u0432\u0438\u0442\u0441\u044F";
                        timerLines.add(label + ": " + timeStr);
                    }
                }
            }
            timerLines.sort(null);

            int aliveCount = 0;
            boolean anyNeverSpawned = false;
            for (var s : ClientData.clientSpawners) {
                if (s.team.equalsIgnoreCase(hoverTeam) && s.type.equals(hoverType)) {
                    if (s.isAlive) aliveCount++;
                    if (!s.hasSpawnedOnce) anyNeverSpawned = true;
                }
            }

            String aliveTxt = "\uD83D\uDFE2 \u0411\u043E\u0435\u0433\u043E\u0442\u043E\u0432: " + aliveCount;
            int maxW = f.width(hoverType);
            maxW = Math.max(maxW, f.width(aliveTxt));
            for (String l : timerLines) maxW = Math.max(maxW, f.width("  " + l));
            int tipW = Math.min(maxW + 12, 140);

            int tooltipX = panelX + panelW + 4;
            int tooltipY = hoverY - 4;
            if (tooltipX + tipW > Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
                tooltipX = panelX - tipW;
            }

            int tipH = 24 + (timerLines.isEmpty() ? 0 : 14 + timerLines.size() * 10);
            g.fill(tooltipX, tooltipY, tooltipX + tipW, tooltipY + tipH, 0xCC0A0C10);
            g.drawString(f, hoverType, tooltipX + 4, tooltipY + 2, 0xFFFFFF, false);
            g.drawString(f, aliveTxt, tooltipX + 4, tooltipY + 14, anyNeverSpawned ? 0xFF4488FF : 0xFF88FF88, false);
            if (!timerLines.isEmpty()) {
                int ty = tooltipY + 24;
                g.drawString(f, "\u23F3 \u041E\u0436\u0438\u0434\u0430\u043D\u0438\u0435:", tooltipX + 4, ty, 0xFFFFAA00, false);
                ty += 10;
                for (String line : timerLines) {
                    g.drawString(f, "  " + line, tooltipX + 4, ty, 0xFFCCCCCC, false);
                    ty += 10;
                    if (ty - tooltipY > 100) { g.drawString(f, "  ...", tooltipX + 4, ty, 0xFF888888, false); break; }
                }
            }
        }
    }

    private void renderAllPlayers(GuiGraphics g, LocalPlayer self, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        String myName = self.getScoreboardName();
        int myInternalSquadId = -1;
        String myTeamForNums = getPlayerTeamStrict();
        boolean isBlueForNums = myTeamForNums != null && myTeamForNums.contains("BLUE");
        int teamCMDIdForNums = isBlueForNums ? ClientData.blueCMDId : ClientData.redCMDId;
        List<WarfareWorldData.Squad> teamSquadsForNums = ClientData.clientSquads.stream()
            .filter(s -> s.team.equalsIgnoreCase(myTeamForNums)).collect(Collectors.toList());
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
                vehicleGroups.computeIfAbsent(info.vehicleId, k -> new ArrayList<>()).add(info);
                continue;
            }
            int sx = toScreenX(info.x, cx);
            int sy = toScreenZ(info.z, cz);
            if (!inMap(sx, sy)) continue;
            var f = PWPTheme.Fonts.display();
            if (isShowNicksHeld && !info.name.equals(myName) && !info.isDowned) {
                g.pose().pushPose();
                g.pose().translate(sx, sy - 8, 450);
                g.pose().scale(0.6F, 0.6F, 1);
                int nickColor = info.squadId != -1 && info.squadId == myInternalSquadId ? -11141291 : -1;
                g.drawCenteredString(f, Component.literal(info.name), 0, 0, nickColor);
                g.pose().popPose();
            }
            if (info.name.equals(myName)) continue;
            if (info.isDowned) {
                if (amIMedic || currentTime - info.lastShoutTime < 60) {
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    g.blit(ICON_PLUS, sx - 4, sy - 4, 8, 8, 0, 0, 16, 16, 16, 16);
                }
            } else {
                float r2 = 0.2F, g2 = 0.6F, b2 = 1.0F;
                if (info.squadId != -1 && info.squadId == myInternalSquadId) { r2 = 0; g2 = 1; b2 = 0; }
                RenderSystem.setShaderColor(r2, g2, b2, 1);
                g.blit(ICON_CIRCLE, sx - 3, sy - 3, 6, 6, 0, 0, 16, 16, 16, 16);
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            Integer displayNum = idToDisplayNum.get(info.squadId);
            if (!info.isDowned && info.isLeader && info.squadId != -1 && displayNum != null) {
                String numStr = String.valueOf(displayNum);
                int textColor = info.squadId == myInternalSquadId ? -11141291 : -11184641;
                g.pose().pushPose();
                g.pose().translate(sx, sy, 350);
                g.pose().scale(0.5F, 0.5F, 1);
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
                int yOffset = sy - 10 - (group.size() - 1) * 8;
                for (var pInfo : group) {
                    g.pose().pushPose();
                    g.pose().translate(sx, yOffset, 450);
                    g.pose().scale(0.6F, 0.6F, 1);
                    int nickColor = -1;
                    if (pInfo.squadId != -1 && pInfo.squadId == myInternalSquadId) nickColor = -11141291;
                    if (pInfo.name.equals(myName)) nickColor = -171;
                    g.drawCenteredString(PWPTheme.Fonts.display(), Component.literal(pInfo.name), 0, 0, nickColor);
                    g.pose().popPose();
                    yOffset += 8;
                }
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
        if (mySquadId != -1) RenderSystem.setShaderColor(0, 1, 0, 1);
        else RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(ICON_SELF, -5, -5, 10, 10, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.pose().popPose();
        if (isShowNicknamesHeld()) {
            g.pose().pushPose();
            g.pose().translate(mySx, mySy - 10, 450);
            g.pose().scale(0.6F, 0.6F, 1);
            int myColor = mySquadId != -1 ? -11141291 : -171;
            g.drawCenteredString(PWPTheme.Fonts.display(), Component.literal(self.getScoreboardName()), 0, 0, myColor);
            g.pose().popPose();
        }
    }

    private void renderSquadMarkerLogic(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String myName = mc.player.getScoreboardName();
        WarfareWorldData.Squad mySquad = null;
        for (var s : ClientData.clientSquads) {
            if (s.members.contains(myName)) { mySquad = s; break; }
        }
        if (mySquad == null) return;
        if (mySquad.marker != null && mySquad.marker.type != 6)
            drawMapMarkerAndLine(g, cx, cz, bpp, mySquad.marker, getSquadMarkerIcon(mySquad.marker.type), getSquadMarkerColor(mySquad.marker.type), mySquad.marker.type != 0);
        if (mySquad.bravoMarker != null && mySquad.bravoMarker.type != 6)
            drawMapMarkerAndLine(g, cx, cz, bpp, mySquad.bravoMarker, getBravoMarkerIcon(mySquad.bravoMarker.type), -65281, mySquad.bravoMarker.type != 0);
        if (mySquad.charlieMarker != null && mySquad.charlieMarker.type != 6)
            drawMapMarkerAndLine(g, cx, cz, bpp, mySquad.charlieMarker, getCharlieMarkerIcon(mySquad.charlieMarker.type), -16711766, mySquad.charlieMarker.type != 0);
    }

    private void drawMapMarkerAndLine(GuiGraphics g, double cx, double cz, double bpp, WarfareWorldData.SquadMarker m, ResourceLocation icon, int color, boolean withDash) {
        Minecraft mc = Minecraft.getInstance();
        int mx = toScreenX(m.x, cx);
        int my = toScreenZ(m.z, cz);
        int px = toScreenX(mc.player.getX(), cx);
        int py = toScreenZ(mc.player.getZ(), cz);
        if (withDash) drawDashedLine(g, px, py, mx, my, color);
        if (inMap(mx, my)) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.blit(icon, mx - 6, my - 6, 0, 0, 12, 12, 12, 12);
            double distance = Math.sqrt(mc.player.distanceToSqr(m.x, mc.player.getY(), m.z));
            String distText = (int)distance + "m";
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
            int lx = (int)(x1 + dx * t);
            int ly = (int)(y1 + dy * t);
            if (inMap(lx, ly)) g.fill(lx, ly, lx + 2, ly + 2, color);
        }
    }

    private int getSquadMarkerColor(int type) {
        return switch (type) { case 1 -> -22016; case 2 -> -11184641; case 3 -> -43521; case 4 -> -1; case 5 -> -65536; default -> -11141291; };
    }

    private ResourceLocation getSquadMarkerIcon(int type) {
        return switch (type) { case 1 -> MARKER_ATTACK; case 2 -> MARKER_DEFEND; case 3 -> MARKER_BUILD; default -> MARKER_MOVE; };
    }

    private ResourceLocation getBravoMarkerIcon(int type) {
        String pre = switch (type) { case 1 -> "marker_attack_bravo"; case 2 -> "marker_defend_bravo"; case 3 -> "marker_build_bravo"; default -> "marker_move_bravo"; };
        return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + pre + ".png");
    }

    private ResourceLocation getCharlieMarkerIcon(int type) {
        String pre = switch (type) { case 1 -> "marker_attack_charlie"; case 2 -> "marker_defend_charlie"; case 3 -> "marker_build_charlie"; default -> "marker_move_charlie"; };
        return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + pre + ".png");
    }

    private void renderTacticalMarkers(GuiGraphics g, double cx, double cz, double bpp) {
        Minecraft mc = Minecraft.getInstance();
        String myTeam = getPlayerTeamStrict();
        long currentTime = mc.level.getGameTime();
        for (var m : ClientData.activeMarkers) {
            if (!m.team.equalsIgnoreCase(myTeam)) continue;
            long timeLeft = m.expiryTick - currentTime;
            if (timeLeft <= 0) continue;
            float alpha = Mth.clamp((float)timeLeft / 3600f, 0, 1);
            int sx = toScreenX(m.pos.getX(), cx);
            int sy = toScreenZ(m.pos.getZ(), cz);
            if (!inMap(sx, sy)) continue;
            ResourceLocation icon = getMarkerIcon(m.type);
            setFilter(icon, true);
            g.pose().pushPose();
            g.pose().translate(sx, sy, 120);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, alpha);
            g.blit(icon, -8, -8, 16, 16, 0, 0, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.pose().popPose();
            setFilter(icon, false);
        }
    }

    private ResourceLocation getMarkerIcon(String type) {
        String path = type.toLowerCase().replace("enemy ", "").replace(" ", "_");
        return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + path + "_marker.png");
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
            .filter(s -> s.team.equalsIgnoreCase(myTeam)).collect(Collectors.toList());
        teamSquadsForNums.sort((s1, s2) -> {
            if (s1.id == teamCMDIdForNums && teamCMDIdForNums != -1) return -1;
            return s2.id == teamCMDIdForNums && teamCMDIdForNums != -1 ? 1 : Integer.compare(s1.id, s2.id);
        });
        var rhombusTex = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/squad_rhombus.png");
        var f = PWPTheme.Fonts.display();
        for (var squad : ClientData.clientSquads) {
            if (!squad.team.equalsIgnoreCase(myTeam)) continue;
            boolean canSee = squad.members.contains(myName) || amISquadLeader;
            if (!canSee) continue;
            for (var rm : squad.rhombusMarkers) {
                long timeLeft = rm.expiryTick - time;
                if (timeLeft <= 0) continue;
                float alpha = Mth.clamp((float)timeLeft / 3600f, 0.1f, 1);
                int mx = toScreenX(rm.x, cx);
                int my = toScreenZ(rm.z, cz);
                if (!inMap(mx, my)) continue;
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1, 1, 1, alpha);
                g.blit(rhombusTex, mx - 8, my - 8, 0, 0, 16, 16, 16, 16);
                g.pose().pushPose();
                g.pose().translate(mx, my, 500);
                g.pose().scale(0.5F, 0.5F, 1);
                int displayNum = 0;
                for (int i = 0; i < teamSquadsForNums.size(); i++) {
                    if (teamSquadsForNums.get(i).id == squad.id) { displayNum = i + 1; break; }
                }
                String numStr = String.valueOf(displayNum);
                int whiteWithAlpha = (int)(alpha * 255) << 24 | 0xFFFFFF;
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
            if (s.members.contains(myName)) { mySquad = s; break; }
        }
        if (mySquad == null) return;
        long time = mc.level.getGameTime();
        boolean isSL = mySquad.leader.equals(myName);
        boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
        boolean isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
        if (mySquad.pingPos != null && time < mySquad.pingExpiry)
            drawPingOnMap(g, mySquad.pingPos, new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye.png"), cx, cz, bpp);
        if (mySquad.bravoPingPos != null && time < mySquad.bravoPingExpiry && (isSL || isBravo))
            drawPingOnMap(g, mySquad.bravoPingPos, new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_bravo.png"), cx, cz, bpp);
        if (mySquad.charliePingPos != null && time < mySquad.charliePingExpiry && (isSL || isCharlie))
            drawPingOnMap(g, mySquad.charliePingPos, new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_charlie.png"), cx, cz, bpp);
    }

    private void drawPingOnMap(GuiGraphics g, BlockPos pos, ResourceLocation icon, double cx, double cz, double bpp) {
        int px = toScreenX(pos.getX() + 0.5, cx);
        int py = toScreenZ(pos.getZ() + 0.5, cz);
        if (inMap(px, py)) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.blit(icon, px - 6, py - 6, 0, 0, 12, 12, 12, 12);
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

    private boolean isShowNicknamesHeld() {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        Key nickKey = ModKeyBindings.SHOW_NICKNAMES_KEY.getKey();
        return nickKey.getType() == Type.MOUSE
            ? GLFW.glfwGetMouseButton(window, nickKey.getValue()) == 1
            : InputConstants.isKeyDown(window, nickKey.getValue());
    }

    public boolean hitPath(int mx, int my, double cx, double cz) {
        int r = 12;
        for (var pts : pathGroups()) {
            if (!pts.isEmpty()) { var p = pts.get(0); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { pathGroups().remove(pts); return true; } }
            if (pts.size()>=2) { var p = pts.get(pts.size()-1); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { pathGroups().remove(pts); return true; } }
        }
        for (var pts : pathGroupsRed()) {
            if (!pts.isEmpty()) { var p = pts.get(0); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { pathGroupsRed().remove(pts); return true; } }
            if (pts.size()>=2) { var p = pts.get(pts.size()-1); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { pathGroupsRed().remove(pts); return true; } }
        }
        for (var pts : pathGroupsYellow()) {
            if (!pts.isEmpty()) { var p = pts.get(0); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { pathGroupsYellow().remove(pts); return true; } }
            if (pts.size()>=2) { var p = pts.get(pts.size()-1); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { pathGroupsYellow().remove(pts); return true; } }
        }
        var sqIt = pathGroupsSquad().entrySet().iterator();
        while (sqIt.hasNext()) {
            var pts = sqIt.next().getValue();
            if (!pts.isEmpty()) { var p = pts.get(0); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { sqIt.remove(); return true; } }
            if (pts.size()>=2) { var p = pts.get(pts.size()-1); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) { sqIt.remove(); return true; } }
        }
        return false;
    }

    public java.util.UUID hitServerPath(int mx, int my, double cx, double cz) {
        int r = 12;
        for (var entry : com.pigeostudios.pwp.warfare.client.PathCache.serverPaths.entrySet()) {
            var pts = entry.getValue().points();
            if (!pts.isEmpty()) { var p = pts.get(0); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) return entry.getKey(); }
            if (pts.size()>=2) { var p = pts.get(pts.size()-1); if (Math.abs(mx-toScreenX(p.x,cx))<r&&Math.abs(my-toScreenZ(p.z,cz))<r) return entry.getKey(); }
        }
        return null;
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
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        for (var m : com.pigeostudios.pwp.warfare.client.MarkerClientCache.getAll()) {
            int px = toScreenX(m.pos.getX() + 0.5, cx), py = toScreenZ(m.pos.getZ() + 0.5, cz);
            if (!inMap(px, py)) continue;
            float maxTicks = "enemy".equals(m.team) ? 6000f : 12000f;
            float t = (now - m.createdAt) / maxTicks;
            float alpha;
            if (t < 0.2f) alpha = 1.0f;
            else alpha = Math.max(0, 1.0f - (float)Math.pow((t - 0.2f) / 0.8f, 1.5f));
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, alpha * 0.9F);
            g.blit(icon(m.iconType, m.team), px - 10, py - 10, 0, 0, 20, 20, 20, 20);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (("hat".equals(m.iconType) || "rally".equals(m.iconType)) && "squad".equals(m.team)) {
                var p = Minecraft.getInstance().player;
                double dist = Math.sqrt(p.distanceToSqr(m.pos.getX() + 0.5, p.getY(), m.pos.getZ() + 0.5));
                String d = distStr(dist);
                int tw = f.width(d);
                g.fill(px - tw / 2 - 2, py + 11, px + tw / 2 + 3, py + 22, (int)(0xAA * alpha) << 24 | 0x000000);
                g.drawString(f, d, px - tw / 2, py + 13, (int)(0xFF * alpha) << 24 | 0xFFFFFF, false);
            }
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    static final float PATH_MIN_LENGTH = 50.0F;
    static final float PATH_MAX_LENGTH = 1500.0F;

    public float pathAnimProgress = 1.0F;
    public float pathFadeAlpha = 1.0F;
    private long animStart = -1;

    public void startPathAnim() { animStart = System.currentTimeMillis(); pathAnimProgress = 0; }
    public void tickAnim() {
        if (animStart < 0) return;
        pathAnimProgress = Math.min(1, (System.currentTimeMillis() - animStart) / 1000.0F);
        if (pathAnimProgress >= 1) animStart = -1;
    }

    private float pathFadeFor(List<PathPoint> pts) {
        if (pts == null || pts.isEmpty()) return 1.0f;
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        int key = System.identityHashCode(pts);
        Long created = com.pigeostudios.pwp.warfare.client.PathCache.pathCreatedAt.get(key);
        if (created == null) {
            // Check if this is a server path with createdAt in ServerPath record
            for (var ep : com.pigeostudios.pwp.warfare.client.PathCache.serverPaths.entrySet()) {
                if (ep.getValue().points() == pts) { created = ep.getValue().createdAt(); break; }
            }
        }
        if (created == null || created <= 0) return 1.0f;
        float elapsed = now - created;
        float maxTicks = 12000f;
        if (elapsed > maxTicks) { return 0f; }
        float t = elapsed / maxTicks;
        if (t < 0.2f) return 1.0f;
        return Math.max(0, 1.0f - (float)Math.pow((t - 0.2f) / 0.8f, 1.5f));
    }

    private void drawPath(GuiGraphics g, double cx, double cz) {
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
            double[] segStarts = new double[n-1];
            double totalLen = 0;
            for (int i = 0; i < n; i++) {
                segX[i] = toScreenX(pts.get(i).x, cx);
                segY[i] = toScreenZ(pts.get(i).z, cz);
                if (i > 0) { segStarts[i-1] = totalLen; totalLen += Math.hypot(segX[i]-segX[i-1], segY[i]-segY[i-1]); }
            }
            double prog = pathAnimProgress * totalLen;

            for (int i = 0; i < n-1; i++) {
                if (prog <= segStarts[i]) break;
                double lx1 = segX[i], ly1 = segY[i];
                if (i == 0 && n > 1) {
                    double dx = segX[1] - segX[0], dy = segY[1] - segY[0];
                    double segLen = Math.hypot(dx, dy);
                    if (segLen > 11) { lx1 += dx * 11 / segLen; ly1 += dy * 11 / segLen; }
                }
                double t = Math.min(1, (prog - segStarts[i]) / (segStarts[i] + Math.hypot(segX[i+1]-segX[i], segY[i+1]-segY[i]) - segStarts[i]));
                int lx2 = (int)(lx1 + (segX[i+1] - lx1) * t);
                int ly2 = (int)(ly1 + (segY[i+1] - ly1) * t);
                var pose = g.pose();
                pose.pushPose(); pose.translate((int)lx1, (int)ly1, 200);
                float angle = (float)Math.toDegrees(Math.atan2(ly2 - (int)ly1, lx2 - (int)lx1));
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                float clen = (float)Math.hypot(lx2 - lx1, ly2 - ly1);
                g.fill(0, -1, (int)clen, 2, 0x66000000);
                g.fill(0, 0, (int)clen, 1, lineCol);
                pose.popPose();
                if (t < 1) break;
            }

            if (pathAnimProgress > 0.05F) {
                int sx = (int)segX[0], sy = (int)segY[0];
                RenderSystem.setShaderColor(r, gr, b, 0.9F * pathFadeAlpha);
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
                int ex2 = (int)segX[n-1], ey2 = (int)segY[n-1];
                int px = (int)segX[n-2], py = (int)segY[n-2];
                float edx = ex2 - px, edy = ey2 - py;
                float elen = (float)Math.hypot(edx, edy);
                if (elen >= 5) {
                    var pose = g.pose();
                    pose.pushPose(); pose.translate(ex2, ey2, 200);
                    float eAngle = (float)Math.toDegrees(Math.atan2(edy, edx)) + 90;
                    pose.mulPose(Axis.ZP.rotationDegrees(eAngle));
                    RenderSystem.setShaderColor(r, gr, b, 0.9F * pathFadeAlpha);
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
            double[] segStarts = new double[n-1];
            for (int i = 0; i < n; i++) {
                segX[i] = toScreenX(pts.get(i).x, cx);
                segY[i] = toScreenZ(pts.get(i).z, cz);
                if (i > 0) {
                    segStarts[i-1] = totalLen;
                    totalLen += Math.hypot(segX[i]-segX[i-1], segY[i]-segY[i-1]);
                }
            }
            double prog = pathAnimProgress * totalLen;

            // Draw lines up to progress, with 14px gap from circle center
            for (int i = 0; i < n-1; i++) {
                if (prog <= segStarts[i]) break;
                double lx1 = segX[i], ly1 = segY[i];
                // Offset first line start 14px from circle center
                if (i == 0 && n > 1) {
                    double dx = segX[1] - segX[0], dy = segY[1] - segY[0];
                    double segLen = Math.hypot(dx, dy);
                    if (segLen > 11) { lx1 += dx * 11 / segLen; ly1 += dy * 11 / segLen; }
                }
                double lSegLen = Math.hypot(segX[i+1] - lx1, segY[i+1] - ly1);
                if (prog <= segStarts[i]) break;
                double t = Math.min(1, (prog - segStarts[i]) / (segStarts[i] + Math.hypot(segX[i+1]-segX[i], segY[i+1]-segY[i]) - segStarts[i]));
                int lx2 = (int)(lx1 + (segX[i+1] - lx1) * t);
                int ly2 = (int)(ly1 + (segY[i+1] - ly1) * t);
                drawPathLine(g, (int)lx1, (int)ly1, lx2, ly2, lineCol);
                if (t < 1) break;
            }

            // Start circle (appears at progress > 5% of first segment)
            if (pathAnimProgress > 0.05F) {
                int sx = (int)segX[0], sy = (int)segY[0];
                RenderSystem.setShaderColor(r, gr2, b, 0.9F * pathFadeAlpha);
                g.blit(ICON_CIRCLE, sx - 8, sy - 8, 16, 16, 0, 0, 16, 16, 16, 16);
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }

            // End arrow (appears at progress > 80%)
            if (pathAnimProgress > 0.8F) {
                int ex2 = (int)segX[n-1], ey2 = (int)segY[n-1];
                int px = (int)segX[n-2], py = (int)segY[n-2];
                float edx = ex2 - px, edy = ey2 - py;
                float elen = (float)Math.hypot(edx, edy);
                if (elen >= 5) {
                    var pose = g.pose();
                    pose.pushPose(); pose.translate(ex2, ey2, 200);
                    float eAngle = (float)Math.toDegrees(Math.atan2(edy, edx)) + 90;
                    pose.mulPose(Axis.ZP.rotationDegrees(eAngle));
                    RenderSystem.setShaderColor(r, gr2, b, 0.9F * pathFadeAlpha);
                    g.blit(ICON_SELF, -8, -8, 16, 16, 0, 0, 16, 16, 16, 16);
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    pose.popPose();
                }
            }
        }
    }

    private void drawPathLine(GuiGraphics g, int x1, int y1, int x2, int y2, int col) {
        float dx = x2 - x1, dy = y2 - y1, len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        var pose = g.pose();
        pose.pushPose(); pose.translate(x1, y1, 200);
        float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        g.fill(0, -1, (int)len, 2, 0x66000000);
        g.fill(0, 0, (int)len, 1, col);
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

    private void drawCompassRose(GuiGraphics g) {
        int cx = mapX + mapWidth - 70, cy = mapY + mapHeight - 66;
        g.blit(ICON_COMPASS, cx, cy, 0, 0, 64, 64, 64, 64);
    }

    private void drawCompass(GuiGraphics g, double cx, double cz) {
        int y = mapY + mapHeight + 4, mid = mapX + mapWidth / 2;
        var p = Minecraft.getInstance().player; if (p == null) return;
        g.fill(mapX, y, mapX + mapWidth, y + 16, 0xCC06080A);
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        for (var m : com.pigeostudios.pwp.warfare.client.MarkerClientCache.getAll()) {
            double dx = m.pos.getX() + 0.5 - p.getX(), dz = m.pos.getZ() + 0.5 - p.getZ();
            double bearing = Math.toDegrees(Math.atan2(dx, dz));
            double rel = (bearing - p.getYRot() + 540) % 360 - 180;
            if (rel < -90 || rel > 90) continue;
            int cx2 = mid + (int)(rel / 90.0 * (mapWidth / 2.0));
            float alpha = Math.max(0, (6000 - (now - m.createdAt)) / 6000f);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, alpha);
            g.blit(icon(m.iconType, m.team), cx2 - 5, y + 3, 0, 0, 10, 10, 10, 10);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    public static String toAlpha(long n) {
        if (n < 0) return "?";
        var sb = new StringBuilder();
        while (true) { sb.insert(0, (char)('A' + (int)(n % 26))); if (n < 26) break; n = n / 26 - 1; }
        return sb.toString();
    }

    public static String getKP(double wx, double wz) {
        long gOriginX = Math.floorDiv((long)(ClientData.mapCenterX - ClientData.mapSizeBlocks / 2.0), 300L) * 300L;
        long gOriginZ = Math.floorDiv((long)(ClientData.mapCenterZ - ClientData.mapSizeBlocks / 2.0), 300L) * 300L;
        long col = Math.floorDiv((long)wx - gOriginX, 300L);
        long row = Math.floorDiv((long)wz - gOriginZ, 300L) + 1;
        return toAlpha(col) + row;
    }

    public static String distStr(double m) {
        if (m <= 30) { int d = (int)Math.round(m); return Math.max(1, d-1) + "-" + (d+1) + "m"; }
        if (m <= 100) { int d = (int)(Math.round(m/5)*5); return Math.max(1,d-5) + "-" + (d+5) + "m"; }
        if (m <= 300) { int d = (int)(Math.round(m/10)*10); return Math.max(1,d-10) + "-" + (d+10) + "m"; }
        int d = (int)(Math.round(m/50)*50); return Math.max(1,d-50) + "-" + (d+50) + "m";
    }

    private int toScreenX(double wx, double cx) {
        return (int)(mapX + mapWidth / 2.0 + (wx - cx) / effectiveScale());
    }

    private int toScreenZ(double wz, double cz) {
        return (int)(mapY + mapHeight / 2.0 + (wz - cz) / effectiveScale());
    }

    private boolean inMap(int sx, int sy) {
        return sx >= mapX && sx <= mapX + mapWidth && sy >= mapY && sy <= mapY + mapHeight;
    }

    public boolean inMap(double mx, double my) {
        return inMap((int)mx, (int)my);
    }

    private ResourceLocation icon(String type, String team) {
        String s = "enemy".equals(team) ? "_r" : "team".equals(team) ? "_y" : "_g";
        return TEX.computeIfAbsent(type + s, k -> new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + k + ".png"));
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) return false;
        if (btn == 0) { isDraggingMap = true; lastMouseX = mx; lastMouseY = my; return true; }
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
        ClientData.zoomMap(delta);
        return true;
    }

    static {
        VEHICLE_ICONS.put("APC", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/apc.png"));
        VEHICLE_ICONS.put("TANK", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/tank.png"));
        VEHICLE_ICONS.put("HELICOPTER", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/helicopter.png"));
        VEHICLE_ICONS.put("CAS Helicopter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_helicopter.png"));
        VEHICLE_ICONS.put("CAS Fighter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/cas_fighter.png"));
        VEHICLE_ICONS.put("Combat Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/combat_vehicle.png"));
        VEHICLE_ICONS.put("Infantry Vehicle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/infantry_vehicle.png"));
        VEHICLE_ICONS.put("Supply Truck", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_truck.png"));
        VEHICLE_ICONS.put("Supply Helicopter", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/supply_helicopter.png"));
        VEHICLE_ICONS.put("DEFAULT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/default.png"));
        VEHICLE_ICONS.put("Static ZU", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/static_zu.png"));
        VEHICLE_ICONS.put("Mobile ZU", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/mobile_zu.png"));
        VEHICLE_ICONS.put("BOAT", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/boat.png"));
        VEHICLE_ICONS.put("Motorcycle", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/motorcycle.png"));
        VEHICLE_ICONS.put("Light Supply", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/light_supply.png"));
        VEHICLE_ICONS.put("Heavy Supply", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/heavy_supply.png"));
        VEHICLE_ICONS.put("SPG", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/spg.png"));
        VEHICLE_ICONS.put("Mine", new ResourceLocation("pwpwarfare", "textures/gui/map_icons/skull_marker.png"));
    }
}

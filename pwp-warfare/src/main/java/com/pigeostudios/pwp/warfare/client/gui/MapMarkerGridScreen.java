package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMapMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MapMarkerGridScreen extends Screen {
    private final int worldX;
    private final int worldZ;
    private final Map<String, ResourceLocation> markers;
    private final List<String> markerTypes = new ArrayList<>();
    private final Screen previousScreen;
    private static final int CARD_W = 90;
    private static final int CARD_H = 82;
    private static final int GAP = 8;

    private int cols;
    private int panelW;
    private int panelH;
    private int contentH;
    private int scrollOff;
    private int maxScroll;
    private int cx;
    private final List<PWPButton> markerButtons = new ArrayList<>();

    public MapMarkerGridScreen(int x, int z, String titleKey, Map<String, ResourceLocation> markers, Screen previousScreen) {
        super(Component.translatable(titleKey));
        this.worldX = x;
        this.worldZ = z;
        this.markers = markers;
        this.markerTypes.addAll(markers.keySet());
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        cols = Math.max(1, Math.min(4, (width - 40) / (CARD_W + GAP)));
        cols = Math.min(cols, markerTypes.size());
        panelW = cols * CARD_W + (cols - 1) * GAP + 16;
        panelW = Math.min(panelW, width - 20);
        cx = (width - panelW) / 2;

        int rows = (markerTypes.size() + cols - 1) / cols;
        contentH = rows * CARD_H + (rows - 1) * GAP;
        panelH = Math.min(height - 50, contentH + 50);
        maxScroll = Math.max(0, contentH + 50 - panelH);
        if (maxScroll == 0) scrollOff = 0;
        if (scrollOff > maxScroll) scrollOff = maxScroll;

        markerButtons.clear();
        for (int i = 0; i < markerTypes.size(); i++) {
            String type = markerTypes.get(i);
            PWPButton btn = new PWPButton(0, 0, 0, 0,
                Component.translatable("pwpwarfare.marker." + type.toLowerCase().replace(" ", "_")),
                b -> {
                    PacketHandler.INSTANCE.sendToServer(new PacketPlaceMapMarker(worldX, worldZ, type));
                    minecraft.setScreen(previousScreen);
                },
                PWPButton.Style.PRIMARY
            );
            addRenderableWidget(btn);
            markerButtons.add(btn);
        }
        repositionButtons();
    }

    private void repositionButtons() {
        int baseY = 46 - scrollOff;
        for (int i = 0; i < markerTypes.size(); i++) {
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 8 + c * (CARD_W + GAP);
            int by = baseY + r * (CARD_H + GAP);

            markerButtons.get(i).setX(bx + 5);
            markerButtons.get(i).setY(by + 52);
            markerButtons.get(i).setWidth(CARD_W - 10);
            markerButtons.get(i).setHeight(18);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int py = 10;
        PWPPanel.render(gui, cx, py, panelW, panelH);

        gui.drawCenteredString(PWPTheme.Fonts.display(), title, width / 2, py + 4, PWPTheme.Colors.TEXT_PRIMARY);
        gui.fill(cx + 4, py + 14, cx + panelW - 4, py + 15, PWPTheme.Colors.ACCENT);

        int clipY = py + 16;
        int clipH = panelH - 16;
        gui.enableScissor(cx, clipY, cx + panelW, clipY + clipH);

        int baseY = 46 - scrollOff;
        for (int i = 0; i < markerTypes.size(); i++) {
            String type = markerTypes.get(i);
            ResourceLocation icon = markers.get(type);
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 8 + c * (CARD_W + GAP);
            int by = baseY + r * (CARD_H + GAP);

            gui.fill(bx, by, bx + CARD_W, by + CARD_H, PWPTheme.Colors.SURFACE_LIGHT);
            gui.renderOutline(bx, by, CARD_W, CARD_H, PWPTheme.Colors.BORDER);

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int iconX = bx + (CARD_W - 32) / 2;
            int iconY = by + 6;
            gui.blit(icon, iconX, iconY, 0.0F, 0.0F, 32, 32, 32, 32);

            Component name = Component.translatable("pwpwarfare.marker." + type.toLowerCase().replace(" ", "_"));
            gui.drawCenteredString(PWPTheme.Fonts.display(), name, bx + CARD_W / 2, by + 42, PWPTheme.Colors.TEXT_SECONDARY);
        }

        super.render(gui, mx, my, pt);
        gui.disableScissor();

        if (maxScroll > 0) {
            if (scrollOff > 0)
                gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("\u25B2"), width / 2, py + 2, PWPTheme.Colors.TEXT_DIM);
            if (scrollOff < maxScroll)
                gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("\u25BC"), width / 2, py + panelH - 4, PWPTheme.Colors.TEXT_DIM);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScroll > 0 && mx >= cx && mx <= cx + panelW) {
            int prev = scrollOff;
            scrollOff = (int) Math.max(0, Math.min(maxScroll, scrollOff - delta * 20));
            if (prev != scrollOff) repositionButtons();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.ui.AnimMap;
import com.pigeostudios.pwp.warfare.client.gui.ui.Scroll;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMapMarker;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Сетка тактических маркеров (команда/враг): карточка = иконка маркера + подпись, клик ставит
 * маркер ({@link PacketPlaceMapMarker}, как раньше) и возвращает на предыдущий экран. Сетка —
 * Grid по ширине панели, прокрутка колесом.
 */
public class MapMarkerGridScreen extends Screen {
    private final int worldX;
    private final int worldZ;
    private final Map<String, ResourceLocation> markers;
    private final List<String> markerTypes = new ArrayList<>();
    private final Screen previousScreen;
    private final Scroll scroll = new Scroll();
    private final AnimMap hover = new AnimMap();

    public MapMarkerGridScreen(
            int x, int z, String titleKey, Map<String, ResourceLocation> markers, Screen previousScreen) {
        super(Component.translatable(titleKey));
        this.worldX = x;
        this.worldZ = z;
        this.markers = markers;
        this.markerTypes.addAll(markers.keySet());
        this.previousScreen = previousScreen;
    }

    private static Component name(String type) {
        return Component.translatable("pwpwarfare.marker." + type.toLowerCase().replace(" ", "_"));
    }

    private Rect panel() {
        int w = Fit.fraction(
                width,
                WTheme.MARKER_PANEL_FRACTION,
                Math.min(width - 2 * Spacing.SM, WTheme.MARKER_PANEL_MIN_W),
                WTheme.MARKER_PANEL_MAX_W);
        int h = Math.max(0, height - 2 * Spacing.LG);
        return Rect.of((width - w) / 2, Spacing.LG, w, h);
    }

    private Rect content(Rect panel) {
        return panel.inset(Spacing.SM, WDraw.headerHeight(height) + Spacing.SM, Spacing.SM, Spacing.SM);
    }

    private Rect[] cells(Rect c) {
        int cols = Grid.columnsFor(
                c.w() - WTheme.SCROLLBAR_W, WTheme.MARKER_CARD_MIN_W, Spacing.XS, WTheme.MARKER_COLS_MAX);
        Rect grid = Rect.of(c.x(), c.y() - scroll.offset(), c.w() - WTheme.SCROLLBAR_W - Spacing.XS, 0);
        return Grid.cells(grid, markerTypes.size(), cols, WTheme.MARKER_CARD_H, Spacing.XS);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        Rect p = panel();
        WDraw.panel(
                g,
                font,
                p,
                WDraw.headerHeight(height),
                WarfareIcon.MARKER,
                title,
                Component.literal(String.valueOf(markerTypes.size())),
                1F);
        Rect c = content(p);
        if (markerTypes.isEmpty()) {
            WDraw.emptyState(
                    g, font, c, WarfareIcon.MARKER, Component.translatable("gui.pwpwarfare.map.no_markers"), null, 1F);
            return;
        }
        Rect[] cells = cells(c);
        int contentH = cells.length == 0 ? 0 : cells[cells.length - 1].bottom() + scroll.offset() - c.y();
        scroll.clamp(contentH, c.h());
        g.enableScissor(c.x(), c.y(), c.right(), c.bottom());
        for (int i = 0; i < cells.length; i++) {
            Rect r = cells[i];
            if (r.bottom() < c.y() || r.y() > c.bottom()) continue;
            String type = markerTypes.get(i);
            float h = hover.get(type, c.contains(mx, my) && r.contains(mx, my));
            WDraw.tile(g, r, h, 0F, 1F, 1F);
            int is = Spacing.ICON_SIZE_LG;
            int ix = r.centerX() - is / 2;
            int iy = r.y() + Spacing.XS;
            ResourceLocation icon = markers.get(type);
            UIDraw.blitTinted(g, 0xFFFFFFFF, () -> g.blit(icon, ix, iy, 0F, 0F, is, is, is, is));
            Rect label = Rect.of(
                    r.x() + Spacing.XXS,
                    iy + is + Spacing.XXS,
                    r.w() - 2 * Spacing.XXS,
                    r.bottom() - iy - is - Spacing.XXS);
            WDraw.text(
                    g,
                    font,
                    name(type),
                    label,
                    Align.CENTER,
                    PWPTheme.Colors.lerp(PWPTheme.Text.MUTED, PWPTheme.Text.HEADING, h),
                    false,
                    1F);
        }
        g.disableScissor();
        WDraw.scrollbar(g, c, contentH, scroll.offset(), 1F);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            Rect c = content(panel());
            if (c.contains(mx, my)) {
                Rect[] cells = cells(c);
                for (int i = 0; i < cells.length; i++) {
                    if (cells[i].contains(mx, my)) {
                        PacketHandler.INSTANCE.sendToServer(
                                new PacketPlaceMapMarker(worldX, worldZ, markerTypes.get(i)));
                        minecraft.setScreen(previousScreen);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (panel().contains(mx, my)) {
            scroll.wheel(delta);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

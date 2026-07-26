package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

/** Left column: spawn point cards */
public class SpawnPanel {

    private static final int CARD_H = 42, GAP = 4;
    public int scrollOff;

    public int render(GuiGraphics gui, int x, int y, int w, int maxH, int mx, int my, String selectedSpawn) {
        var f = PWPTheme.Fonts.display();
        gui.drawString(f, "SPAWN POINTS", x + 2, y, PWPTheme.Colors.TEXT_SECONDARY, false);

        int totalH = DeployData.spawns.size() * (CARD_H + GAP);
        int maxScroll = Math.max(0, totalH - (maxH - 14));
        if (scrollOff > maxScroll) scrollOff = maxScroll;
        if (scrollOff < 0) scrollOff = 0;

        gui.enableScissor(x, y + 14, x + w, y + maxH);

        int cy = y + 14 - scrollOff;
        for (DeployData.SpawnPoint sp : DeployData.spawns) {
            if (cy + CARD_H > y + maxH) break;
            if (cy + CARD_H >= y + 14) {
                renderCard(gui, x, cy, w, CARD_H, sp, mx, my, selectedSpawn);
            }
            cy += CARD_H + GAP;
        }

        gui.disableScissor();

        int used = 14 + Math.min(totalH, maxH - 14);
        return y + used;
    }

    private void renderCard(GuiGraphics gui, int x, int y, int w, int h,
                            DeployData.SpawnPoint sp, int mx, int my, String sel) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean selected = sel.equals(sp.id());
        boolean blocked = sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;

        int bg = selected ? 0x44C8812A : (hover && !blocked ? 0x22FFFFFF : 0);
        int border = selected ? PWPTheme.Colors.ACCENT : (hover && !blocked ? PWPTheme.Colors.BORDER_FOCUS : 0);
        RoundedRect.fill(gui, x, y, w, h, 5, bg);
        if (selected || hover) RoundedRect.border(gui, x, y, w, h, 5, 1, border);

        int iconS = 28, ix = x + 6, iy = y + (h - iconS) / 2;
        gui.fill(ix, iy, ix + iconS, iy + iconS, 0xFF1A1E26);
        gui.renderOutline(ix, iy, iconS, iconS, PWPTheme.Colors.BORDER);
        String letter = sp.id().startsWith("HUB") ? "F" : sp.id().startsWith("RALLY") ? "R" : "M";
        gui.drawCenteredString(PWPTheme.Fonts.display(), letter, ix + iconS / 2, iy + iconS / 2 - 5,
            blocked ? PWPTheme.Colors.TEXT_DIM : PWPTheme.Colors.ACCENT);

        int tx = ix + iconS + 10;
        int tc = blocked ? PWPTheme.Colors.TEXT_DIM : (selected ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.TEXT_PRIMARY);
        gui.drawString(PWPTheme.Fonts.display(), sp.name(), tx, y + 4, tc, false);

        String status; int sc;
        switch (sp.status()) {
            case SAFE, HEALTHY -> { status = "Safe"; sc = PWPTheme.Colors.SUCCESS; }
            case COOLDOWN -> { status = "Cooldown"; sc = PWPTheme.Colors.WARNING; }
            case BLOCKED -> { status = "Blocked"; sc = PWPTheme.Colors.DANGER; }
            case DESTROYED -> { status = "Destroyed"; sc = PWPTheme.Colors.DANGER; }
            default -> { status = ""; sc = PWPTheme.Colors.TEXT_DIM; }
        }
        if (sp.supplies() > 0) status += "  \u2022  Supplies: " + sp.supplies();
        if (sp.distance() > 0) status += "  \u2022  " + sp.distance() + "m";
        gui.drawString(PWPTheme.Fonts.display(), status, tx, y + 24, sc, false);
    }

    public String mouseClicked(double mx, double my, int btn, int x, int y, int w) {
        int cy = y + 14 - scrollOff;
        for (DeployData.SpawnPoint sp : DeployData.spawns) {
            if (mx >= x && mx <= x + w && my >= cy && my <= cy + CARD_H && btn == 0) {
                boolean blocked = sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
                if (!blocked) return sp.id();
                return "";
            }
            cy += CARD_H + GAP;
        }
        return null;
    }
}

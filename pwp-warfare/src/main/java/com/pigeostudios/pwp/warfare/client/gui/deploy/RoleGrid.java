package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class RoleGrid {

    public int scrollOff;
    public String hoveredKit;
    public int hoveredCellX, hoveredCellY, hoveredCellSize, popupGridX, popupGridY, popupGridW;
    private Map<String, List<DeployData.KitRecord>> cachedCats;
    private int cacheVersion = -1;

    private Map<String, List<DeployData.KitRecord>> getCategories() {
        if (cachedCats == null || DeployData.kits.hashCode() != cacheVersion) {
            cachedCats = new LinkedHashMap<>();
            for (DeployData.KitRecord k : DeployData.kits)
                cachedCats.computeIfAbsent(k.category(), c -> new ArrayList<>()).add(k);
            cacheVersion = DeployData.kits.hashCode();
        }
        return cachedCats;
    }

    public void render(GuiGraphics gui, int x, int y, int w, int h, int mx, int my, String selectedKit) {
        var f = PWPTheme.Fonts.display();
        int cols = 4, gap = 2;
        int cell = Math.min(42, Math.max(32, (w - (cols - 1) * gap) / cols));
        int iconS = cell - 12;

        popupGridX = x; popupGridY = y; popupGridW = w;

        int totalH = 0;
        var cats = getCategories();
        for (var e : cats.entrySet()) {
            var list = e.getValue();
            int rows = (list.size() + cols - 1) / cols;
            totalH += 12 + rows * (cell + gap) + 4;
        }
        int maxScroll = Math.max(0, totalH - h);
        if (scrollOff > maxScroll) scrollOff = maxScroll;
        if (scrollOff < 0) scrollOff = 0;

        gui.enableScissor(x, y, x + w, y + h);

        hoveredKit = null;
        int cy = y - scrollOff;
        for (var e : cats.entrySet()) {
            var list = e.getValue();
            int rows = (list.size() + cols - 1) / cols;
            if (cy + 12 + rows * (cell + gap) + 4 >= y && cy < y + h) {
                gui.drawString(f, e.getKey(), x, cy, PWPTheme.Colors.TEXT_ACCENT, false);
            }
            cy += 12;

            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++) {
                    int idx = r * cols + c;
                    if (idx >= list.size()) break;
                    int cx2 = x + c * (cell + gap), cy2 = cy + r * (cell + gap);
                    if (cy2 + cell >= y && cy2 < y + h) {
                        boolean hovered = mx >= cx2 && mx <= cx2 + cell && my >= cy2 && my <= cy2 + cell;
                        if (hovered) {
                            hoveredKit = list.get(idx).name();
                            hoveredCellX = cx2; hoveredCellY = cy2; hoveredCellSize = cell;
                        }
                        renderCell(gui, cx2, cy2, cell, iconS,
                            list.get(idx), mx, my, selectedKit, hovered);
                    }
                }
            cy += rows * (cell + gap) + 4;
        }

        gui.disableScissor();

        // Popup
        if (hoveredKit != null) {
            renderPopup(gui, mx, my, selectedKit);
        }
    }

    private void renderCell(GuiGraphics gui, int x, int y, int size, int iconS,
                            DeployData.KitRecord kit, int mx, int my, String sel, boolean hovered) {
        boolean selected = sel.equals(kit.name());
        boolean avail = kit.available();
        var f = PWPTheme.Fonts.display();

        int bg = selected ? 0x44C8812A : (hovered && avail ? PWPTheme.Colors.SURFACE_LIGHT :
            (avail ? PWPTheme.Colors.SURFACE : 0xFF181818));
        int bd = selected ? PWPTheme.Colors.ACCENT : (hovered && avail ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);
        RoundedRect.fill(gui, x, y, size, size, 4, bg);
        RoundedRect.border(gui, x, y, size, size, 4, 1, bd);

        String iconName = kit.name().toLowerCase().replace(" ", "_");
        ResourceLocation ic = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + iconName + ".png");
        RenderSystem.enableBlend();
        if (!avail) RenderSystem.setShaderColor(0.45f, 0.45f, 0.45f, 1f);
        int ix = x + (size - iconS) / 2, iy = y + (size - iconS) / 2 - 2;
        // Try blit the icon, if texture is missing the render will just be blank
        try {
            gui.blit(ic, ix, iy, 0, 0, iconS, iconS, iconS, iconS);
        } catch (Exception e) {
            // Fallback: draw first letter
            String letter = kit.name().isEmpty() ? "?" : kit.name().substring(0, 1).toUpperCase();
            gui.drawCenteredString(PWPTheme.Fonts.display(), letter, x + size / 2, iy + 2, PWPTheme.Colors.TEXT_PRIMARY);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (kit.maxInTeam() > 0) {
            String cnt = kit.inTeamCount() + "/" + kit.maxInTeam();
            int cc = kit.inTeamCount() >= kit.maxInTeam() ? PWPTheme.Colors.DANGER : PWPTheme.Colors.TEXT_SECONDARY;
            gui.drawCenteredString(f, cnt, x + size / 2, y + size - 8, cc);
        }

        // Tooltip below cell for unavailable
        if (hovered && !avail && !kit.reason().isEmpty()) {
            String reason = kit.reason();
            int rw = f.width(reason) + 8;
            int tx = x + (size - rw) / 2;
            if (tx < popupGridX) tx = popupGridX;
            gui.fill(tx, y + size + 2, tx + rw, y + size + 14, 0xDD000000);
            gui.drawString(f, reason, tx + 4, y + size + 4, PWPTheme.Colors.DANGER, false);
        }
    }

    private void renderPopup(GuiGraphics gui, int mx, int my, String selectedKit) {
        var f = PWPTheme.Fonts.display();
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(hoveredKit)).findFirst();
        if (kitO.isEmpty()) return;
        var kit = kitO.get();

        for (var slot : kit.loadout()) {
            if (!slot.label().equals("PRIMARY")) continue;
            if (!slot.hasAlternatives()) continue;

            int pw = 120;
            int ph = Math.min(slot.options().size() * 22 + 6, 150);
            int px = Math.min(hoveredCellX + hoveredCellSize + 4, popupGridX + popupGridW - pw);
            int py = Math.min(hoveredCellY, popupGridY + 200 - ph);
            if (px < popupGridX) px = popupGridX;

            gui.fill(px, py, px + pw, py + ph, 0xDD15191E);
            gui.renderOutline(px, py, pw, ph, PWPTheme.Colors.ACCENT);

            int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
            int optY = py + 3;
            for (int ai = 0; ai < slot.options().size(); ai++) {
                var opt = slot.options().get(ai);
                boolean optHover = mx >= px && mx <= px + pw && my >= optY && my <= optY + 20;
                boolean optSel = ai == sel;
                int bg = optSel ? 0x44C8812A : (optHover ? 0x22FFFFFF : 0);
                gui.fill(px + 2, optY, px + pw - 2, optY + 20, bg);
                if (optSel) gui.renderOutline(px + 1, optY, pw - 2, 20, PWPTheme.Colors.ACCENT);
                gui.renderFakeItem(opt.stack(), px + 3, optY + 2);
                gui.drawString(f, opt.name(), px + 22, optY + 5, PWPTheme.Colors.TEXT_PRIMARY, false);
                optY += 22;
            }
            break;
        }
    }

    public String mouseClicked(double mx, double my, int btn, int x, int y, int w, int h) {
        if (btn != 0) return null;
        int cols = 4, gap = 2;
        int cell = Math.min(42, Math.max(32, (w - (cols - 1) * gap) / cols));

        // Check popup click first
        if (hoveredKit != null) {
            var kitO = DeployData.kits.stream().filter(k -> k.name().equals(hoveredKit)).findFirst();
            if (kitO.isPresent()) {
                var kit = kitO.get();
                for (var slot : kit.loadout()) {
                    if (!slot.label().equals("PRIMARY") || !slot.hasAlternatives()) continue;
                    int pw = 120;
                    int ph = Math.min(slot.options().size() * 22 + 6, 150);
                    int px = Math.min(hoveredCellX + hoveredCellSize + 4, popupGridX + popupGridW - pw);
                    int py = Math.min(hoveredCellY, popupGridY + 200 - ph);
                    if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
                        int optIdx = (int)((my - py - 3) / 22);
                        if (optIdx >= 0 && optIdx < slot.options().size()) {
                            DeployData.setSelectedIndex(kit.name(), slot.label(), optIdx);
                            return kit.name();
                        }
                        return "";
                    }
                    break;
                }
            }
        }

        var cats = getCategories();
        int cy = y - scrollOff;
        for (var e : cats.entrySet()) {
            var list = e.getValue();
            int rows = (list.size() + cols - 1) / cols;
            cy += 12;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++) {
                    int idx = r * cols + c;
                    if (idx >= list.size()) break;
                    int cx2 = x + c * (cell + gap), cy2 = cy + r * (cell + gap);
                    if (mx >= cx2 && mx <= cx2 + cell && my >= cy2 && my <= cy2 + cell) {
                        var kit = list.get(idx);
                        if (kit.available()) { DeployData.expandedSlots.clear(); return kit.name(); }
                        return "";
                    }
                }
            cy += rows * (cell + gap) + 4;
        }
        return null;
    }
}

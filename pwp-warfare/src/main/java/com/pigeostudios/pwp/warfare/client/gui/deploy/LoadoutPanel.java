package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class LoadoutPanel {

    public void render(GuiGraphics gui, int x, int y, int w, int maxH, int mx, int my, String selectedKit) {
        var f = PWPTheme.Fonts.display();
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return;
        var kit = kitO.get();

        int pad = 6, cy = y;
        boolean avail = kit.available();

        // HEADER: LOADOUT / UNAVAILABLE
        if (!avail) {
            gui.drawString(f, "НЕДОСТУПНО", x + pad, cy, PWPTheme.Colors.DANGER, false);
        } else {
            gui.drawString(f, "СНАРЯЖЕНИЕ", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
        }
        cy += 10;
        int headerBg = avail ? PWPTheme.Colors.SURFACE : 0x44FF0000;
        int headerFg = avail ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.DANGER;
        RoundedRect.fill(gui, x, cy, w, 20, 4, headerBg);
        RoundedRect.border(gui, x, cy, w, 20, 4, 1, avail ? PWPTheme.Colors.BORDER : PWPTheme.Colors.DANGER);
        gui.drawString(f, DeployData.getDisplayName(kit.name()), x + pad + 2, cy + 5, headerFg, false);
        cy += 24;

        // Slots: PRIMARY, SECONDARY, THROWABLE, EQUIPMENT → mapped to Squad: PRIMARY WEAPON, SIDE ARM, SPECIAL, BACKPACK
        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (cy + 40 > y + maxH) break;

            int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
            if (sel >= slot.options().size()) sel = slot.defaultIndex();
            DeployData.LoadoutOption opt = slot.options().get(sel);
            boolean hasAlt = slot.hasAlternatives();
            boolean hoverSlot = mx >= x && mx <= x + w && my >= cy && my <= cy + 32 + 2;
            int altH = (hasAlt && hoverSlot) ? slot.options().size() * 17 + 4 : 0;
            boolean expanded = hasAlt && (hoverSlot || (my >= cy + 32 + 2 && my <= cy + 32 + 2 + altH));

            String squadLabel = switch (slot.label()) {
                case "PRIMARY" -> "СТВОЛ";
                case "SECONDARY" -> "ВТОРИЧКА";
                case "THROWABLE" -> "ГРАНАТЫ";
                case "SPECIAL" -> "СПЕЦ";
                case "BACKPACK" -> "РЮКЗАК";
                default -> slot.label();
            };

            gui.drawString(f, squadLabel, x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
            cy += 10;

            int barH = 20;
            RoundedRect.fill(gui, x, cy, w, barH, 4, PWPTheme.Colors.SURFACE);
            RoundedRect.border(gui, x, cy, w, barH, 4, 1, PWPTheme.Colors.BORDER);
            gui.renderFakeItem(opt.stack(), x + pad, cy + 2);
            gui.drawString(f, opt.name(), x + pad + 20, cy + 5, PWPTheme.Colors.TEXT_PRIMARY, false);

            // Ammo count for weapons
            int count = opt.stack().getCount();
            if (count > 0 && (slot.label().equals("PRIMARY") || slot.label().equals("SECONDARY"))) {
                gui.drawString(f, "[" + count + "]", x + w - pad - 28, cy + 5, PWPTheme.Colors.TEXT_ACCENT, false);
            }

            if (hasAlt) {
                String arr = expanded ? "\u25B2" : "\u25BC";
                gui.drawString(f, arr, x + w - pad - 12, cy + 5,
                    expanded ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_DIM, false);
            }
            cy += barH + 2;

            if (expanded && hasAlt) {
                for (int ai = 0; ai < slot.options().size(); ai++) {
                    DeployData.LoadoutOption alt = slot.options().get(ai);
                    boolean altHover = mx >= x + pad && mx <= x + w && my >= cy && my <= cy + 16;
                    boolean altSel = ai == sel;
                    int abg = altSel ? 0x44C8812A : (altHover ? 0x22FFFFFF : 0);
                    RoundedRect.fill(gui, x + 8, cy, w - 16, 16, 3, abg);
                    if (altSel) RoundedRect.border(gui, x + 8, cy, w - 16, 16, 3, 1, PWPTheme.Colors.ACCENT);
                    gui.renderFakeItem(alt.stack(), x + 12, cy);
                    gui.drawString(f, alt.name(), x + 26, cy + 3,
                        altSel ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY, false);
                    cy += 17;
                }
                cy += 2;
            }
            cy += 2;
        }

        // Stats block (simplified)
        if (cy + 50 < y + maxH) {
            cy += 6;
            gui.fill(x, cy, x + w, cy + 1, PWPTheme.Colors.BORDER);
            cy += 4;
            gui.drawString(f, "Magazine Capacity: 30", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
            cy += 10;
            gui.drawString(f, "Caliber: 7.62mm", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
            cy += 10;
            gui.drawString(f, "Rate of Fire: 600", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
            cy += 10;
            gui.drawString(f, "Fire Mode: Semi/Auto", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w, int maxH, String selectedKit) {
        if (btn != 0) return false;
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return false;
        var kit = kitO.get();

        int pad = 6, cy = y;
        cy += 10 + 20 + 24;

        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (cy + 40 > y + maxH) break;
            boolean hasAlt = slot.hasAlternatives();
            boolean hoverSlot = mx >= x && mx <= x + w && my >= cy && my <= cy + 32 + 2;
            int altH = (hasAlt && hoverSlot) ? slot.options().size() * 17 + 4 : 0;
            boolean expanded = hasAlt && (hoverSlot || (my >= cy + 32 + 2 && my <= cy + 32 + 2 + altH));
            cy += 10;
            cy += 20 + 2;
            if (expanded) {
                int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
                if (sel >= slot.options().size()) sel = slot.defaultIndex();
                for (int ai = 0; ai < slot.options().size(); ai++) {
                    if (mx >= x + 8 && mx <= x + w && my >= cy && my <= cy + 16) {
                        DeployData.setSelectedIndex(kit.name(), slot.label(), ai);
                        return true;
                    }
                    cy += 17;
                }
                cy += 2;
            }
            cy += 2;
        }
        return false;
    }
}

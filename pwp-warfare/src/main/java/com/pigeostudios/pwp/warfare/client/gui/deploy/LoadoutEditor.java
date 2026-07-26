package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Right column top: 3D weapon preview + weapon slots with expandable dropdowns */
public class LoadoutEditor {

    public void render(GuiGraphics gui, int x, int y, int w, int maxH, int mx, int my, String selectedKit) {
        var f = PWPTheme.Fonts.display();
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return;
        DeployData.KitRecord kit = kitO.get();

        int pad = 6, cy = y;

        // ── 3D Weapon Preview ──
        ItemStack weapon = getPrimary(kit);
        if (!weapon.isEmpty()) {
            int previewH = 70;
            gui.drawString(f, "WEAPON", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false); cy += 10;
            RoundedRect.fill(gui, x, cy, w, previewH, 4, 0xFF0E1117);
            RoundedRect.border(gui, x, cy, w, previewH, 4, 1, PWPTheme.Colors.BORDER);

            gui.pose().pushPose();
            gui.pose().translate(x + w / 2f - 8, cy + previewH / 2f - 8, 200);
            gui.renderFakeItem(weapon, 0, 0);
            gui.pose().popPose();
            cy += previewH + 6;
        }

        // ── ROLE header ──
        gui.drawString(f, "ROLE", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false); cy += 10;
        RoundedRect.fill(gui, x, cy, w, 20, 4, PWPTheme.Colors.SURFACE);
        RoundedRect.border(gui, x, cy, w, 20, 4, 1, PWPTheme.Colors.BORDER);
        gui.drawString(f, kit.name().toUpperCase(), x + pad + 2, cy + 5, PWPTheme.Colors.ACCENT, false);
        cy += 24;

        // ── Slots ──
        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (cy + 40 > y + maxH) break;

            int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
            if (sel >= slot.options().size()) sel = slot.defaultIndex();
            DeployData.LoadoutOption opt = slot.options().get(sel);
            boolean expanded = DeployData.isSlotExpanded(kit.name(), slot.label());
            boolean hasAlt = slot.hasAlternatives();

            gui.drawString(f, slot.label(), x + pad, cy, PWPTheme.Colors.TEXT_DIM, false); cy += 10;

            int barH = 20;
            RoundedRect.fill(gui, x, cy, w, barH, 4, PWPTheme.Colors.SURFACE);
            RoundedRect.border(gui, x, cy, w, barH, 4, 1, PWPTheme.Colors.BORDER);
            gui.renderFakeItem(opt.stack(), x + pad, cy + 2);
            gui.drawString(f, opt.name(), x + pad + 20, cy + 5, PWPTheme.Colors.TEXT_PRIMARY, false);
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
    }

    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w, int maxH, String selectedKit) {
        if (btn != 0) return false;
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return false;
        var kit = kitO.get();

        int pad = 6, cy = y;
        if (!getPrimary(kit).isEmpty()) cy += 10 + 70 + 6;
        cy += 10 + 20 + 24;

        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (cy + 40 > y + maxH) break;
            boolean hasAlt = slot.hasAlternatives(), expanded = DeployData.isSlotExpanded(kit.name(), slot.label());
            cy += 10;
            if (hasAlt && mx >= x + w - pad - 18 && mx <= x + w && my >= cy && my <= cy + 20) {
                DeployData.toggleSlotExpanded(kit.name(), slot.label()); return true;
            }
            cy += 20 + 2;
            if (expanded && hasAlt) {
                int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
                if (sel >= slot.options().size()) sel = slot.defaultIndex();
                for (int ai = 0; ai < slot.options().size(); ai++) {
                    if (mx >= x + 8 && mx <= x + w && my >= cy && my <= cy + 16) {
                        DeployData.setSelectedIndex(kit.name(), slot.label(), ai);
                        DeployData.toggleSlotExpanded(kit.name(), slot.label()); return true;
                    }
                    cy += 17;
                }
                cy += 2;
            }
            cy += 2;
        }
        return false;
    }

    private ItemStack getPrimary(DeployData.KitRecord kit) {
        for (var slot : kit.loadout()) {
            if (slot.label().equals("PRIMARY")) {
                int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
                if (sel >= 0 && sel < slot.options().size()) return slot.options().get(sel).stack();
            }
        }
        return ItemStack.EMPTY;
    }
}

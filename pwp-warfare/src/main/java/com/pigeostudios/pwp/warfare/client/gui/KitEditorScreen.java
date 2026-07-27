package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSaveFactionKit;
import com.pigeostudios.pwp.warfare.network.PacketSaveKit;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class KitEditorScreen extends AbstractContainerScreen<KitEditorMenu> {

    private EditBox descBox, maxTeamBox, maxSquadBox, minPlayersBox;
    private boolean keepContainerOpen;

    private static final String ALT_PREFIX = "__ALT__";
    private static final String[] ALT_CATS = {"PRIMARY", "SECONDARY", "THROWABLE", "SPECIAL", "BACKPACK"};
    private static final String[] ALT_LABELS = {"P", "S", "T", "X", "B"};

    public KitEditorScreen(KitEditorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 286;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;
    }

    @Override
    public void removed() {
        if (!keepContainerOpen) super.removed();
    }

    @Override
    protected void init() {
        super.init();
        keepContainerOpen = false;
        int x = leftPos, y = topPos;

        descBox = new EditBox(PWPTheme.Fonts.display(), x + 60, y + 20, 170, 14, Component.literal(""));
        descBox.setValue(menu.description);
        addRenderableWidget(descBox);

        int r2y = y + 56;
        maxTeamBox = new EditBox(PWPTheme.Fonts.display(), x + 86, r2y, 34, 14, Component.literal("-1"));
        maxTeamBox.setValue(String.valueOf(menu.maxPerTeam));
        addRenderableWidget(maxTeamBox);

        maxSquadBox = new EditBox(PWPTheme.Fonts.display(), x + 138, r2y, 34, 14, Component.literal("-1"));
        maxSquadBox.setValue(String.valueOf(menu.maxPerSquad));
        addRenderableWidget(maxSquadBox);

        minPlayersBox = new EditBox(PWPTheme.Fonts.display(), x + 190, r2y, 28, 14, Component.literal("0"));
        minPlayersBox.setValue(String.valueOf(menu.minSquadPlayers));
        addRenderableWidget(minPlayersBox);

        addRenderableWidget(new PWPButton(x + imageWidth - 52, y + 2, 46, 16,
            Component.literal("Сохр."),
            b -> saveKit(), PWPButton.Style.ACCENT));
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        int x = leftPos, y = topPos;
        var f = PWPTheme.Fonts.display();

        RoundedRect.fill(gui, x, y, imageWidth, imageHeight, 6, PWPTheme.Colors.SURFACE);
        RoundedRect.border(gui, x, y, imageWidth, imageHeight, 6, 1, PWPTheme.Colors.BORDER);

        gui.fill(x, y, x + imageWidth, y + 16, 0xE60E1117);
        gui.drawCenteredString(f, "KIT: " + menu.kitName, x + imageWidth / 2, y + 4, PWPTheme.Colors.TEXT_ACCENT);

        // Settings area
        RoundedRect.fill(gui, x + 2, y + 18, imageWidth - 4, 60, 4, 0x2212151A);
        RoundedRect.border(gui, x + 2, y + 18, imageWidth - 4, 60, 4, 1, PWPTheme.Colors.BORDER);

        gui.drawString(f, "Имя:", x + 6, y + 20, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, menu.kitName, x + 40, y + 20, PWPTheme.Colors.ACCENT, false);

        gui.drawString(f, "Описание:", x + 6, y + 34, PWPTheme.Colors.TEXT_DIM, false);

        String cat = menu.category != null && !menu.category.isEmpty() ? menu.category : "INFANTRY";
        gui.drawString(f, "Кат: " + cat, x + 6, y + 48, PWPTheme.Colors.TEXT_ACCENT, false);

        // Leader toggle
        int lx = x + 6, ly = y + 61;
        gui.fill(lx, ly, lx + 12, ly + 12, menu.isLeaderOnly ? 0x44C8812A : 0x2212151A);
        gui.renderOutline(lx, ly, 12, 12, menu.isLeaderOnly ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.BORDER);
        if (menu.isLeaderOnly) gui.drawString(f, "\u2713", lx + 2, ly, PWPTheme.Colors.ACCENT, false);
        gui.drawString(f, "Только лидер", lx + 16, ly + 1, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, "Team:", lx + 110, ly + 1, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, "Squad:", lx + 160, ly + 1, PWPTheme.Colors.TEXT_DIM, false);

        // [Skins]
        gui.drawString(f, "[Скины]", x + imageWidth - 56, ly + 1, PWPTheme.Colors.TEXT_ACCENT, false);

        // Slot area
        RoundedRect.fill(gui, x + 7, y + 84, 164, 96, 4, 0xFF15191E);
        RoundedRect.border(gui, x + 7, y + 84, 164, 96, 4, 1, PWPTheme.Colors.BORDER);
        for (int si = 0; si < 9; si++) {
            Slot s = menu.slots.get(si);
            int sx = leftPos + s.x, sy = topPos + s.y;
            String lbl;
            if (si < 4) {
                lbl = new String[]{"PR", "SE", "TH", "SP"}[si];
            } else {
                String altCat = getAltCategory(si);
                lbl = altCat != null ? categoryLetter(altCat) : (isWeaponSlot(si) ? "P" : "B");
            }
            int lw2 = f.width(lbl) + 4;
            int bc = lbl.equals("P") ? PWPTheme.Colors.ACCENT : (lbl.equals("S") ? PWPTheme.Colors.INFO
                : (lbl.equals("T") ? PWPTheme.Colors.WARNING : (lbl.equals("X") ? 0xFFFF8800 : PWPTheme.Colors.TEXT_DIM)));
            if (si >= 4) {
                gui.fill(sx - 2, sy - 8, sx + lw2 + 2, sy - 8 + 10, 0x2212151A);
                gui.renderOutline(sx - 2, sy - 8, lw2 + 4, 10, bc);
            }
            gui.drawString(f, lbl, sx, sy - 7, bc, false);
        }
        gui.drawString(f, "БРОНЯ \u2192", x + 8, y + 156, PWPTheme.Colors.TEXT_ACCENT, false);

        // Right panel: АЛЬТЕРНАТИВЫ
        int rx = x + 174, rw = imageWidth - 180;
        RoundedRect.fill(gui, rx, y + 84, rw, 76, 4, 0xFF15191E);
        RoundedRect.border(gui, rx, y + 84, rw, 76, 4, 1, PWPTheme.Colors.BORDER);
        gui.drawString(f, "АЛЬТ", rx + 4, y + 86, PWPTheme.Colors.TEXT_DIM, false);
        for (int si = 41; si <= 48; si++) {
            Slot s = menu.slots.get(si);
            int sx = leftPos + s.x, sy = topPos + s.y;
            String altCat = getAltCategory(si);
            String lbl = altCat != null ? categoryLetter(altCat) : "B";
            int lw2 = f.width(lbl) + 4;
            int bc = lbl.equals("P") ? PWPTheme.Colors.ACCENT : (lbl.equals("S") ? PWPTheme.Colors.INFO
                : (lbl.equals("T") ? PWPTheme.Colors.WARNING : (lbl.equals("X") ? 0xFFFF8800 : PWPTheme.Colors.TEXT_DIM)));
            gui.fill(sx - 2, sy - 8, sx + lw2 + 2, sy - 8 + 10, 0x2212151A);
            gui.renderOutline(sx - 2, sy - 8, lw2 + 4, 10, bc);
            gui.drawString(f, lbl, sx, sy - 7, bc, false);
        }

        // Player inventory bg
        RoundedRect.fill(gui, x + 7, y + 180, 164, 80, 4, 0xFF15191E);
        RoundedRect.border(gui, x + 7, y + 180, 164, 80, 4, 1, PWPTheme.Colors.BORDER);
        gui.drawString(f, "ИНВЕНТАРЬ", x + 10, y + 182, PWPTheme.Colors.TEXT_DIM, false);

        super.render(gui, mx, my, pt);
        renderTooltip(gui, mx, my);

        // Slot overlays (resupply/saveNBT)
        for (Slot slot : menu.slots) {
            if (slot.container == menu.kitInventory) {
                int idx = slot.getContainerSlot();
                if (idx >= 0 && idx < 49) {
                    int sx = leftPos + slot.x, sy = topPos + slot.y;
                    if (menu.saveNbtFlags[idx]) gui.fill(sx, sy, sx + 16, sy + 16, 0x6000C8FF);
                    else if (menu.resupplyFlags[idx]) gui.fill(sx, sy, sx + 16, sy + 16, 0x6000FF40);
                }
            }
        }

        // Legend
        int ly2 = y + imageHeight - 14;
        gui.fill(x + 8, ly2, x + 14, ly2 + 6, 0x6000FF40);
        gui.drawString(f, "Resupply", x + 16, ly2, PWPTheme.Colors.SUCCESS, false);
        gui.fill(x + 80, ly2, x + 86, ly2 + 6, 0x6000C8FF);
        gui.drawString(f, "SaveNBT", x + 88, ly2, PWPTheme.Colors.INFO, false);
        gui.drawString(f, "СКМ = Resupply, Shift+СКМ = SaveNBT",
            x + 8, y + imageHeight - 26, PWPTheme.Colors.TEXT_DIM, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float pt, int mx, int my) {}
    @Override
    protected void renderLabels(GuiGraphics gui, int mx, int my) {}

    private void saveKit() {
        try { menu.maxPerTeam = Integer.parseInt(maxTeamBox.getValue()); } catch (Exception ignored) {}
        try { menu.maxPerSquad = Integer.parseInt(maxSquadBox.getValue()); } catch (Exception ignored) {}
        try { menu.minSquadPlayers = Integer.parseInt(minPlayersBox.getValue()); } catch (Exception ignored) {}
        menu.category = menu.category != null ? menu.category : "INFANTRY";
        menu.description = descBox.getValue();

        String team = menu.team;
        if (team.equals("BLUE") || team.equals("RED")) {
            PacketHandler.INSTANCE.sendToServer(new PacketSaveKit(
                team, menu.kitName, menu.category, menu.description,
                menu.isLeaderOnly, menu.maxPerTeam, menu.maxPerSquad, menu.minSquadPlayers,
                menu.resupplyFlags, menu.saveNbtFlags, menu.slotSkins
            ));
        } else {
            PacketHandler.INSTANCE.sendToServer(new PacketSaveFactionKit(
                menu.faction, menu.kitName, team, menu.category, menu.description,
                menu.isLeaderOnly, menu.maxPerTeam, menu.maxPerSquad, menu.minSquadPlayers,
                menu.resupplyFlags, menu.saveNbtFlags, menu.slotSkins
            ));
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("Кит сохранён!"), true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int lx = leftPos + 6, ly = topPos + 61;
            if (mx >= lx && mx <= lx + 12 && my >= ly && my <= ly + 12) {
                menu.isLeaderOnly = !menu.isLeaderOnly;
                return true;
            }
            int sx = leftPos + imageWidth - 56;
            if (mx >= sx && mx <= sx + 50 && my >= ly && my <= ly + 12) {
                keepContainerOpen = true;
                Minecraft.getInstance().setScreen(new KitSkinSelectScreen(menu, this));
                return true;
            }
            // Category label clicks on slots 4-8
            for (int si = 4; si <= 8; si++) {
                Slot s = menu.slots.get(si);
                int slx = leftPos + s.x - 2, sly = topPos + s.y - 8;
                if (mx >= slx && mx <= slx + 16 && my >= sly && my <= sly + 10) {
                    cycleAltCategory(si);
                    return true;
                }
            }
            // Category label clicks on slots 41-48 (right panel)
            for (int si = 41; si <= 48; si++) {
                if (si >= menu.slots.size()) break;
                Slot s = menu.slots.get(si);
                int slx = leftPos + s.x - 2, sly = topPos + s.y - 8;
                if (mx >= slx && mx <= slx + 16 && my >= sly && my <= sly + 10) {
                    cycleAltCategory(si);
                    return true;
                }
            }
        }
        if (button == 2) {
            Slot slot = hoveredSlot;
            if (slot != null && slot.container == menu.kitInventory) {
                int idx = slot.getContainerSlot();
                if (idx >= 0 && idx < 49) {
                    if (hasShiftDown()) menu.saveNbtFlags[idx] = !menu.saveNbtFlags[idx];
                    else menu.resupplyFlags[idx] = !menu.resupplyFlags[idx];
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private String getAltCategory(int slot) {
        java.util.List<String> skins = menu.slotSkins.get(slot);
        if (skins != null) {
            for (String cat : ALT_CATS) {
                if (skins.contains(ALT_PREFIX + cat)) return cat;
            }
        }
        return null;
    }

    private void cycleAltCategory(int slot) {
        String cur = getAltCategory(slot);
        int idx = 0;
        if (cur != null) {
            for (int i = 0; i < ALT_CATS.length; i++) {
                if (ALT_CATS[i].equals(cur)) { idx = i; break; }
            }
        }
        idx = (idx + 1) % ALT_CATS.length;
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add(ALT_PREFIX + ALT_CATS[idx]);
        menu.slotSkins.put(slot, list);
    }

    private String categoryLetter(String cat) {
        for (int i = 0; i < ALT_CATS.length; i++) {
            if (ALT_CATS[i].equals(cat)) return ALT_LABELS[i];
        }
        return "B";
    }

    private boolean isWeaponSlot(int slot) {
        net.minecraft.world.item.ItemStack stack = menu.kitInventory.getItem(slot);
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        String id = item.getDescriptionId();
        return id.contains("gun") || id.contains("rifle") || id.contains("pistol") || id.contains("smg")
            || id.contains("shotgun") || id.contains("sniper") || id.contains("lmg") || id.contains("rocket")
            || id.contains("launcher") || id.contains("sword") || id.contains("axe")
            || item instanceof net.minecraft.world.item.SwordItem
            || item instanceof net.minecraft.world.item.AxeItem;
    }
}

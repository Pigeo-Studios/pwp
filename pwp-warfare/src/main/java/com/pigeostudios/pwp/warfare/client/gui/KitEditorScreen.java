package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSaveFactionKit;
import com.pigeostudios.pwp.warfare.network.PacketSaveKit;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.*;

public class KitEditorScreen extends AbstractContainerScreen<KitEditorMenu> {

    private static final List<String> CATEGORIES = List.of("INFANTRY", "SPECIALIST", "CREWMAN", "COMMANDER");

    private EditBox descBox, maxTeamBox, maxSquadBox, minPlayersBox;
    private int categoryIndex;
    private boolean keepContainerOpen;

    public KitEditorScreen(KitEditorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 240;
        this.imageHeight = 286;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;
        this.categoryIndex = Math.max(0, CATEGORIES.indexOf(menu.category));
    }

    @Override
    public void removed() {
        if (!keepContainerOpen) super.removed();
    }

    @Override
    protected void init() {
        super.init();
        keepContainerOpen = false;
        int x = leftPos;
        int y = topPos;

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

        addRenderableWidget(new PWPButton(x + imageWidth - 44, y + 2, 38, 16,
            Component.literal("Save"),
            b -> saveKit(), PWPButton.Style.ACCENT));
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        int x = leftPos;
        int y = topPos;
        var f = PWPTheme.Fonts.display();

        gui.fill(x, y, x + imageWidth, y + imageHeight, PWPTheme.Colors.SURFACE);
        gui.renderOutline(x, y, imageWidth, imageHeight, PWPTheme.Colors.BORDER);

        gui.fill(x, y, x + imageWidth, y + 16, 0xE60E1117);
        gui.drawCenteredString(f, "KIT: " + menu.kitName, x + imageWidth / 2, y + 4, PWPTheme.Colors.TEXT_ACCENT);

        // Fields area
        gui.fill(x + 2, y + 18, x + imageWidth - 2, y + 78, 0x2212151A);
        gui.renderOutline(x + 2, y + 18, imageWidth - 4, 60, PWPTheme.Colors.BORDER);

        gui.drawString(f, "Name:", x + 6, y + 20, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, menu.kitName, x + 44, y + 20, PWPTheme.Colors.ACCENT, false);

        gui.drawString(f, "Desc:", x + 6, y + 34, PWPTheme.Colors.TEXT_DIM, false);

        // Category display (read-only)
        String catLabel = menu.category != null && !menu.category.isEmpty() ? menu.category : "INFANTRY";
        gui.drawString(f, "Cat: " + catLabel, x + 6, y + 48, PWPTheme.Colors.TEXT_ACCENT, false);

        // Leader toggle
        int lx = x + 6;
        int ly = y + 60;
        gui.fill(lx, ly, lx + 12, ly + 12, menu.isLeaderOnly ? 0x44C8812A : 0x2212151A);
        gui.renderOutline(lx, ly, 12, 12, menu.isLeaderOnly ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.BORDER);
        if (menu.isLeaderOnly) gui.drawString(f, "\u2713", lx + 2, ly, PWPTheme.Colors.ACCENT, false);
        gui.drawString(f, "Leader", lx + 16, ly + 1, PWPTheme.Colors.TEXT_DIM, false);

        gui.drawString(f, "Team:", lx + 70, ly + 1, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, "Squad:", lx + 124, ly + 1, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, "MinP:", lx + 176, ly + 1, PWPTheme.Colors.TEXT_DIM, false);

        // [Skins] button
        gui.drawString(f, "[Skins]", x + imageWidth - 56, ly + 1, PWPTheme.Colors.TEXT_ACCENT, false);

        // Slot area
        gui.fill(x + 7, y + 82, x + 171, y + 166, 0xFF15191E);
        gui.renderOutline(x + 7, y + 82, 164, 84, PWPTheme.Colors.BORDER);
        gui.drawString(f, "HOTBAR", x + 8, y + 120, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, "ARMOR", x + 8, y + 138, PWPTheme.Colors.TEXT_DIM, false);

        // Extra slots
        gui.fill(x - 34, y + 82, x + 2, y + 160, 0xFF15191E);
        gui.renderOutline(x - 34, y + 82, 36, 78, PWPTheme.Colors.BORDER);
        gui.drawString(f, "EXTRA", x - 28, y + 84, PWPTheme.Colors.TEXT_DIM, false);

        // Player inv area
        gui.fill(x + 7, y + 170, x + 171, y + 258, 0xFF15191E);
        gui.renderOutline(x + 7, y + 170, 164, 88, PWPTheme.Colors.BORDER);
        gui.drawString(f, "PLAYER INVENTORY", x + 10, y + 172, PWPTheme.Colors.TEXT_DIM, false);

        super.render(gui, mx, my, pt);
        renderTooltip(gui, mx, my);

        // Slot overlays
        for (Slot slot : menu.slots) {
            if (slot.container == menu.kitInventory) {
                int idx = slot.getContainerSlot();
                if (idx >= 0 && idx < 49) {
                    int sx = leftPos + slot.x;
                    int sy = topPos + slot.y;
                    if (menu.saveNbtFlags[idx]) {
                        gui.fill(sx, sy, sx + 16, sy + 16, 0x6000C8FF);
                    } else if (menu.resupplyFlags[idx]) {
                        gui.fill(sx, sy, sx + 16, sy + 16, 0x6000FF40);
                    }
                }
            }
        }

        // Legend
        gui.fill(x + 8, y + imageHeight - 14, x + 14, y + imageHeight - 8, 0x6000FF40);
        gui.drawString(f, "Resupply", x + 16, y + imageHeight - 14, PWPTheme.Colors.SUCCESS, false);
        gui.fill(x + 80, y + imageHeight - 14, x + 86, y + imageHeight - 8, 0x6000C8FF);
        gui.drawString(f, "SaveNBT", x + 88, y + imageHeight - 14, PWPTheme.Colors.INFO, false);
        gui.drawString(f, "MC = Resupply toggle, Shift+MC = SaveNBT toggle",
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
                team, menu.kitName, menu.category, menu.description,
                menu.isLeaderOnly, menu.maxPerTeam, menu.maxPerSquad, menu.minSquadPlayers,
                menu.resupplyFlags, menu.saveNbtFlags, menu.slotSkins
            ));
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("Kit saved!"), true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            // Leader toggle
            int lx = leftPos + 6;
            int ly = topPos + 67;
            if (mx >= lx && mx <= lx + 12 && my >= ly && my <= ly + 12) {
                menu.isLeaderOnly = !menu.isLeaderOnly;
                return true;
            }
            // Skins button
            int sx = leftPos + imageWidth - 56;
            if (mx >= sx && mx <= sx + 50 && my >= ly && my <= ly + 12) {
                keepContainerOpen = true;
                Minecraft.getInstance().setScreen(new KitSkinSelectScreen(menu, this));
                return true;
            }
        }
        if (button == 2) {
            Slot slot = hoveredSlot;
            if (slot != null && slot.container == menu.kitInventory) {
                int idx = slot.getContainerSlot();
                if (idx >= 0 && idx < 49) {
                    if (hasShiftDown()) {
                        menu.saveNbtFlags[idx] = !menu.saveNbtFlags[idx];
                    } else {
                        menu.resupplyFlags[idx] = !menu.resupplyFlags[idx];
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}

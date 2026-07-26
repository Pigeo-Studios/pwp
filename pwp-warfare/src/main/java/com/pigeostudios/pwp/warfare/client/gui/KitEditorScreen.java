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
        this.imageWidth = 230;
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

        // Description
        descBox = new EditBox(PWPTheme.Fonts.display(), x + 10, y + 28, 170, 14, Component.literal(""));
        descBox.setValue(menu.description);
        addRenderableWidget(descBox);

        // Limits
        int r2y = y + 60;
        addRenderableWidget(new PWPButton(x + 8, r2y, 54, 16,
            menu.isLeaderOnly ? Component.literal("LD:ON") : Component.literal("LD:OFF"),
            b -> {
                menu.isLeaderOnly = !menu.isLeaderOnly;
                b.setMessage(menu.isLeaderOnly ? Component.literal("LD:ON") : Component.literal("LD:OFF"));
            }, menu.isLeaderOnly ? PWPButton.Style.ACCENT : PWPButton.Style.DARK));

        maxTeamBox = new EditBox(PWPTheme.Fonts.display(), x + 66, r2y, 32, 14, Component.literal("-1"));
        maxTeamBox.setValue(String.valueOf(menu.maxPerTeam));
        addRenderableWidget(maxTeamBox);

        maxSquadBox = new EditBox(PWPTheme.Fonts.display(), x + 102, r2y, 32, 14, Component.literal("-1"));
        maxSquadBox.setValue(String.valueOf(menu.maxPerSquad));
        addRenderableWidget(maxSquadBox);

        minPlayersBox = new EditBox(PWPTheme.Fonts.display(), x + 138, r2y, 28, 14, Component.literal("0"));
        minPlayersBox.setValue(String.valueOf(menu.minSquadPlayers));
        addRenderableWidget(minPlayersBox);

        addRenderableWidget(new PWPButton(x + 170, r2y - 1, 20, 18,
            Component.literal("S"),
            b -> {
                keepContainerOpen = true;
                Minecraft.getInstance().setScreen(new KitSkinSelectScreen(menu, this));
            }, PWPButton.Style.GHOST));

        addRenderableWidget(new PWPButton(x + imageWidth - 48, y + 2, 42, 16,
            Component.literal("Save"),
            b -> saveKit(), PWPButton.Style.ACCENT));
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        int x = leftPos;
        int y = topPos;
        var f = PWPTheme.Fonts.display();

        // Panel bg
        gui.fill(x, y, x + imageWidth, y + imageHeight, PWPTheme.Colors.SURFACE);
        gui.renderOutline(x, y, imageWidth, imageHeight, PWPTheme.Colors.BORDER);

        // Title
        gui.fill(x, y, x + imageWidth, y + 16, 0xE60E1117);
        gui.drawCenteredString(f, "KIT: " + menu.kitName, x + imageWidth / 2, y + 4, PWPTheme.Colors.TEXT_ACCENT);

        // Field bg
        gui.fill(x + 2, y + 18, x + imageWidth - 2, y + 78, 0x2212151A);
        gui.renderOutline(x + 2, y + 18, imageWidth - 4, 60, PWPTheme.Colors.BORDER);

        // Labels
        gui.drawString(f, "Desc", x + 10, y + 18, PWPTheme.Colors.TEXT_DIM, false);

        // Category buttons
        gui.drawString(f, "Cat", x + 10, y + 46, PWPTheme.Colors.TEXT_DIM, false);
        for (int ci = 0; ci < CATEGORIES.size(); ci++) {
            boolean sel = ci == categoryIndex;
            int bx = x + 34 + ci * 48;
            int bw = 46;
            gui.fill(bx, y + 46, bx + bw, y + 58, sel ? 0x44C8812A : 0x2212151A);
            gui.renderOutline(bx, y + 46, bw, 12, sel ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.BORDER);
            gui.drawCenteredString(f, CATEGORIES.get(ci), bx + bw / 2, y + 47,
                sel ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY);
        }

        gui.drawString(f, "Team", x + 66, r2y() + y - 12, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, "Sqd", x + 102, r2y() + y - 12, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, "Min", x + 138, r2y() + y - 12, PWPTheme.Colors.TEXT_DIM, false);

        // Slot area bg
        gui.fill(x + 7, y + 82, x + 171, y + 166, 0xFF15191E);
        gui.renderOutline(x + 7, y + 82, 164, 84, PWPTheme.Colors.BORDER);

        // Extra slots panel
        gui.fill(x - 34, y + 82, x + 2, y + 160, 0xFF15191E);
        gui.renderOutline(x - 34, y + 82, 36, 78, PWPTheme.Colors.BORDER);

        // Player inv area bg
        gui.fill(x + 7, y + 170, x + 171, y + 258, 0xFF15191E);
        gui.renderOutline(x + 7, y + 170, 164, 88, PWPTheme.Colors.BORDER);

        // Let super draw slots & items
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
        gui.drawString(f, "Resup", x + 16, y + imageHeight - 14, PWPTheme.Colors.SUCCESS, false);
        gui.fill(x + 70, y + imageHeight - 14, x + 76, y + imageHeight - 8, 0x6000C8FF);
        gui.drawString(f, "NBT", x + 78, y + imageHeight - 14, PWPTheme.Colors.INFO, false);
        gui.drawString(f, "MC=Resup  SC=NBT", x + 8, y + imageHeight - 26, PWPTheme.Colors.TEXT_DIM, false);
    }

    private int r2y() { return 60; }

    @Override
    protected void renderBg(GuiGraphics gui, float pt, int mx, int my) {}

    @Override
    protected void renderLabels(GuiGraphics gui, int mx, int my) {}

    private void saveKit() {
        try { menu.maxPerTeam = Integer.parseInt(maxTeamBox.getValue()); } catch (Exception ignored) {}
        try { menu.maxPerSquad = Integer.parseInt(maxSquadBox.getValue()); } catch (Exception ignored) {}
        try { menu.minSquadPlayers = Integer.parseInt(minPlayersBox.getValue()); } catch (Exception ignored) {}
        menu.category = CATEGORIES.get(categoryIndex);
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
            int catY = topPos + 46;
            if (my >= catY && my <= catY + 12) {
                for (int ci = 0; ci < CATEGORIES.size(); ci++) {
                    int bx = leftPos + 34 + ci * 48;
                    if (mx >= bx && mx <= bx + 46) {
                        categoryIndex = ci;
                        return true;
                    }
                }
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

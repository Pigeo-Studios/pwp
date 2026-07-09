/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.Slot
 */
package com.example.aas.client.gui;

import com.example.aas.menu.KitEditorMenu;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSaveKit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class KitEditorScreen
extends AbstractContainerScreen<KitEditorMenu> {
    private EditBox maxTeamBox;
    private EditBox maxSquadBox;
    private EditBox minPlayersBox;

    public KitEditorScreen(KitEditorMenu menu, Inventory inv, Component title) {
        super((AbstractContainerMenu)menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 262;
        this.inventoryLabelY = 168;
        this.titleLabelY = 4;
    }

    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)(((KitEditorMenu)this.menu).isLeaderOnly ? "[X] Squad Ld Only" : "[ ] Squad Ld Only")), b -> {
            ((KitEditorMenu)this.menu).isLeaderOnly = !((KitEditorMenu)this.menu).isLeaderOnly;
            b.setMessage((Component)Component.literal((String)(((KitEditorMenu)this.menu).isLeaderOnly ? "[X] Squad Ld Only" : "[ ] Squad Ld Only")));
        }).bounds(x + 8, y + 14, 80, 18).build());
        this.maxTeamBox = new EditBox(this.font, x + 144, y + 14, 24, 14, (Component)Component.empty());
        this.maxTeamBox.setValue(String.valueOf(((KitEditorMenu)this.menu).maxPerTeam));
        this.addRenderableWidget((GuiEventListener)this.maxTeamBox);
        this.maxSquadBox = new EditBox(this.font, x + 144, y + 32, 24, 14, (Component)Component.empty());
        this.maxSquadBox.setValue(String.valueOf(((KitEditorMenu)this.menu).maxPerSquad));
        this.addRenderableWidget((GuiEventListener)this.maxSquadBox);
        this.minPlayersBox = new EditBox(this.font, x + 144, y + 50, 24, 14, (Component)Component.empty());
        this.minPlayersBox.setValue(String.valueOf(((KitEditorMenu)this.menu).minSquadPlayers));
        this.addRenderableWidget((GuiEventListener)this.minPlayersBox);
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"SAVE"), b -> this.saveKit()).bounds(x + 120, y + 148, 48, 20).build());
    }

    private void saveKit() {
        try {
            ((KitEditorMenu)this.menu).maxPerTeam = Integer.parseInt(this.maxTeamBox.getValue());
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            ((KitEditorMenu)this.menu).maxPerSquad = Integer.parseInt(this.maxSquadBox.getValue());
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            ((KitEditorMenu)this.menu).minSquadPlayers = Integer.parseInt(this.minPlayersBox.getValue());
        }
        catch (Exception exception) {
            // empty catch block
        }
        PacketHandler.INSTANCE.sendToServer((Object)new PacketSaveKit(((KitEditorMenu)this.menu).team, ((KitEditorMenu)this.menu).kitName, ((KitEditorMenu)this.menu).isLeaderOnly, ((KitEditorMenu)this.menu).maxPerTeam, ((KitEditorMenu)this.menu).maxPerSquad, ((KitEditorMenu)this.menu).minSquadPlayers, ((KitEditorMenu)this.menu).resupplyFlags, ((KitEditorMenu)this.menu).saveNbtFlags));
        this.minecraft.player.displayClientMessage((Component)Component.literal((String)"Kit Saved!"), true);
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        this.renderBackground(gui);
        super.render(gui, mx, my, pt);
        this.renderTooltip(gui, mx, my);
        int x = this.leftPos;
        int y = this.topPos;
        int labelColor = 0xCCCCCC;
        gui.drawString(this.font, "Max/team", x + 98, y + 17, labelColor, false);
        gui.drawString(this.font, "Max/Sqd", x + 103, y + 35, labelColor, false);
        gui.drawString(this.font, "Min/Sqd", x + 103, y + 53, labelColor, false);
        gui.pose().pushPose();
        gui.pose().scale(0.9f, 0.9f, 1.0f);
        int scaledX = (int)((float)(x + 8) / 0.9f);
        int scaledY = (int)((float)(y + 36) / 0.9f);
        gui.drawString(this.font, "MMB: Toggle Resupply", scaledX, scaledY, 0x80FF80, false);
        gui.drawString(this.font, "Shift + MMB: Save NBT", scaledX, scaledY + 10, 0x80FFFF, false);
        gui.pose().popPose();
        for (int i = 0; i < ((KitEditorMenu)this.menu).slots.size(); ++i) {
            int idx;
            Slot slot = (Slot)((KitEditorMenu)this.menu).slots.get(i);
            if (slot.container != ((KitEditorMenu)this.menu).kitInventory || (idx = slot.getContainerSlot()) < 0 || idx >= 49) continue;
            if (((KitEditorMenu)this.menu).saveNbtFlags[idx]) {
                gui.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0x600000FF);
                continue;
            }
            if (!((KitEditorMenu)this.menu).resupplyFlags[idx]) continue;
            gui.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0x60FFFF00);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        int idx;
        Slot slot;
        if (button == 2 && (slot = this.hoveredSlot) != null && slot.container == ((KitEditorMenu)this.menu).kitInventory && (idx = slot.getContainerSlot()) >= 0 && idx < 49) {
            if (KitEditorScreen.hasShiftDown()) {
                ((KitEditorMenu)this.menu).saveNbtFlags[idx] = !((KitEditorMenu)this.menu).saveNbtFlags[idx];
            } else {
                ((KitEditorMenu)this.menu).resupplyFlags[idx] = !((KitEditorMenu)this.menu).resupplyFlags[idx];
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    protected void renderBg(GuiGraphics gui, float pt, int mx, int my) {
        gui.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -13421773);
        gui.fill(this.leftPos - 42, this.topPos + 62, this.leftPos - 2, this.topPos + 142, -14540254);
        gui.renderOutline(this.leftPos - 42, this.topPos + 62, 40, 80, -16777216);
        for (Slot slot : ((KitEditorMenu)this.menu).slots) {
            gui.fill(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, this.leftPos + slot.x + 17, this.topPos + slot.y + 17, -16777216);
            gui.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, -7631989);
        }
    }
}


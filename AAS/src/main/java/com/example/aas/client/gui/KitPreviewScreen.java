/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.ItemStack
 */
package com.example.aas.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class KitPreviewScreen
extends Screen {
    private final Screen parent;
    private final String kitName;
    private final List<ItemStack> items;

    public KitPreviewScreen(Screen parent, String kitName, List<ItemStack> items) {
        super((Component)Component.literal((String)("Preview: " + kitName)));
        this.parent = parent;
        this.kitName = kitName;
        this.items = items;
    }

    protected void init() {
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"BACK"), b -> this.minecraft.setScreen(this.parent)).bounds(this.width / 2 - 40, this.height - 30, 80, 20).build());
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        int y;
        int x;
        int i;
        if (!this.minecraft.player.isAlive()) {
            gui.fill(0, 0, this.width, this.height, -16777216);
        } else {
            this.renderBackground(gui);
        }
        gui.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        int startX = this.width / 2 - 81;
        int startY = 50;
        for (i = 0; i < 27; ++i) {
            x = startX + i % 9 * 18;
            y = startY + i / 9 * 18;
            this.drawSlot(gui, x, y, this.items.get(i + 9));
        }
        for (i = 0; i < 9; ++i) {
            x = startX + i * 18;
            y = startY + 60;
            this.drawSlot(gui, x, y, this.items.get(i));
        }
        for (i = 0; i < 5; ++i) {
            x = startX + i * 18;
            y = startY + 85;
            this.drawSlot(gui, x, y, this.items.get(i + 36));
        }
        super.render(gui, mx, my, pt);
    }

    public void onClose() {
        if (this.minecraft.player != null && !this.minecraft.player.isAlive()) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    private void drawSlot(GuiGraphics gui, int x, int y, ItemStack stack) {
        gui.fill(x, y, x + 17, y + 17, 0x50FFFFFF);
        gui.renderFakeItem(stack, x + 1, y + 1);
        gui.renderItemDecorations(this.font, stack, x + 1, y + 1);
    }
}


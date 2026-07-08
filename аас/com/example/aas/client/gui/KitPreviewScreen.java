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
        super((Component)Component.m_237113_((String)("Preview: " + kitName)));
        this.parent = parent;
        this.kitName = kitName;
        this.items = items;
    }

    protected void m_7856_() {
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"BACK"), b -> this.f_96541_.m_91152_(this.parent)).m_252987_(this.f_96543_ / 2 - 40, this.f_96544_ - 30, 80, 20).m_253136_());
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        int y;
        int x;
        int i;
        if (!this.f_96541_.f_91074_.m_6084_()) {
            gui.m_280509_(0, 0, this.f_96543_, this.f_96544_, -16777216);
        } else {
            this.m_280273_(gui);
        }
        gui.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 20, 0xFFFFFF);
        int startX = this.f_96543_ / 2 - 81;
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
        super.m_88315_(gui, mx, my, pt);
    }

    public void m_7379_() {
        if (this.f_96541_.f_91074_ != null && !this.f_96541_.f_91074_.m_6084_()) {
            this.f_96541_.m_91152_(this.parent);
        } else {
            super.m_7379_();
        }
    }

    private void drawSlot(GuiGraphics gui, int x, int y, ItemStack stack) {
        gui.m_280509_(x, y, x + 17, y + 17, 0x50FFFFFF);
        gui.m_280203_(stack, x + 1, y + 1);
        gui.m_280370_(this.f_96547_, stack, x + 1, y + 1);
    }
}


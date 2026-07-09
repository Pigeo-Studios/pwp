/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 */
package com.example.aas.client.gui;

import com.example.aas.client.gui.KitListScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KitTeamSelectScreen
extends Screen {
    public KitTeamSelectScreen() {
        super((Component)Component.literal((String)"Select Team for Kits"));
    }

    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"BLUE TEAM KITS"), b -> this.minecraft.setScreen((Screen)new KitListScreen("BLUE"))).bounds(cx - 105, cy - 10, 100, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"RED TEAM KITS"), b -> this.minecraft.setScreen((Screen)new KitListScreen("RED"))).bounds(cx + 5, cy - 10, 100, 20).build());
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        this.renderBackground(gui);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(gui, mx, my, pt);
    }
}


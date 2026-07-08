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
        super((Component)Component.m_237113_((String)"Select Team for Kits"));
    }

    protected void m_7856_() {
        int cx = this.f_96543_ / 2;
        int cy = this.f_96544_ / 2;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"BLUE TEAM KITS"), b -> this.f_96541_.m_91152_((Screen)new KitListScreen("BLUE"))).m_252987_(cx - 105, cy - 10, 100, 20).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"RED TEAM KITS"), b -> this.f_96541_.m_91152_((Screen)new KitListScreen("RED"))).m_252987_(cx + 5, cy - 10, 100, 20).m_253136_());
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        this.m_280273_(gui);
        gui.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 20, 0xFFFFFF);
        super.m_88315_(gui, mx, my, pt);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package com.example.aas.client.gui;

import com.example.aas.client.AASDeathScreen;
import com.example.aas.client.gui.SquadSelectionScreen;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketPlaceMapMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MapMarkerGridScreen
extends Screen {
    private final int worldX;
    private final int worldZ;
    private final Map<String, ResourceLocation> markers;
    private static final int COLS = 4;
    private static final int CELL_WIDTH = 90;
    private static final int CELL_HEIGHT = 90;
    private static final int BTN_WIDTH = 80;

    public MapMarkerGridScreen(int x, int z, String title, Map<String, ResourceLocation> markers) {
        super((Component)Component.m_237113_((String)title));
        this.worldX = x;
        this.worldZ = z;
        this.markers = markers;
    }

    protected void m_7856_() {
        int totalWidth = 360;
        int startX = (this.f_96543_ - totalWidth) / 2;
        int startY = 40;
        int i = 0;
        for (String type : this.markers.keySet()) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * 90 + 5;
            int y = startY + row * 90;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)type), b -> {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketPlaceMapMarker(this.worldX, this.worldZ, type));
                if (!this.f_96541_.f_91074_.m_6084_()) {
                    this.f_96541_.m_91152_((Screen)new AASDeathScreen(null, false));
                } else {
                    this.f_96541_.m_91152_((Screen)new SquadSelectionScreen());
                }
            }).m_252987_(x, y + 45, 80, 20).m_253136_());
            ++i;
        }
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        if (!this.f_96541_.f_91074_.m_6084_()) {
            gui.m_280509_(0, 0, this.f_96543_, this.f_96544_, -16777216);
        } else {
            this.m_280273_(gui);
        }
        gui.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 15, 0xFFFFFF);
        int totalWidth = 360;
        int startX = (this.f_96543_ - totalWidth) / 2;
        int startY = 40;
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.enableBlend();
        int i = 0;
        for (ResourceLocation icon : this.markers.values()) {
            int row = i / 4;
            int col = i % 4;
            int iconX = startX + col * 90 + 29;
            int iconY = startY + row * 90 + 5;
            gui.m_280163_(icon, iconX, iconY, 0.0f, 0.0f, 32, 32, 32, 32);
            ++i;
        }
        super.m_88315_(gui, mx, my, pt);
    }

    public boolean m_7043_() {
        return false;
    }
}


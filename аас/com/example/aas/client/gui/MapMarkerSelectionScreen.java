/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package com.example.aas.client.gui;

import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketPlaceMapMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MapMarkerSelectionScreen
extends Screen {
    private final BlockPos targetPos;
    private static final Map<String, ResourceLocation> MARKERS = new LinkedHashMap<String, ResourceLocation>();

    public MapMarkerSelectionScreen(BlockPos pos) {
        super((Component)Component.m_237113_((String)"Select Marker"));
        this.targetPos = pos;
    }

    protected void m_7856_() {
        int startX = (this.f_96543_ - 340) / 2;
        int startY = 40;
        int i = 0;
        for (String type : MARKERS.keySet()) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * 85;
            int y = startY + row * 80;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)type), b -> {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketPlaceMapMarker(this.targetPos.m_123341_(), this.targetPos.m_123343_(), type));
                this.m_7379_();
            }).m_252987_(x, y + 35, 80, 20).m_253136_());
            ++i;
        }
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        this.m_280273_(gui);
        gui.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 15, 0xFFFFFF);
        int i = 0;
        int startX = (this.f_96543_ - 340) / 2;
        int startY = 40;
        for (ResourceLocation icon : MARKERS.values()) {
            int row = i / 4;
            int col = i % 4;
            int iconX = startX + col * 85 + 24;
            int iconY = startY + row * 80;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            gui.m_280163_(icon, iconX, iconY, 0.0f, 0.0f, 32, 32, 32, 32);
            ++i;
        }
        super.m_88315_(gui, mx, my, pt);
    }

    static {
        MARKERS.put("Infantry", new ResourceLocation("aas", "textures/gui/map_icons/infantry_marker.png"));
        MARKERS.put("Sniper", new ResourceLocation("aas", "textures/gui/map_icons/sniper_marker.png"));
        MARKERS.put("HAT", new ResourceLocation("aas", "textures/gui/map_icons/hat_marker.png"));
        MARKERS.put("APC", new ResourceLocation("aas", "textures/gui/map_icons/apc_marker.png"));
        MARKERS.put("Tank", new ResourceLocation("aas", "textures/gui/map_icons/tank_marker.png"));
        MARKERS.put("Enemy HUB", new ResourceLocation("aas", "textures/gui/map_icons/hub_marker.png"));
        MARKERS.put("Enemy Rally", new ResourceLocation("aas", "textures/gui/map_icons/rally_marker.png"));
        MARKERS.put("Supply Request", new ResourceLocation("aas", "textures/gui/map_icons/supply_request_marker.png"));
    }
}


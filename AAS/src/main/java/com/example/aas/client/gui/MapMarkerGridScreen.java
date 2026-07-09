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
    static private final int COLS = 4;
    static private final int CELL_WIDTH = 90;
    static private final int CELL_HEIGHT = 90;
    static private final int BTN_WIDTH = 80;

    public MapMarkerGridScreen(int x, int z, String title, Map<String, ResourceLocation> markers) {
        super((Component)Component.literal((String)title));
        this.worldX = x;
        this.worldZ = z;
        this.markers = markers;
    }

    protected void init() {
        int totalWidth = 360;
        int startX = (this.width - totalWidth) / 2;
        int startY = 40;
        int i = 0;
        for (String type : this.markers.keySet()) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * 90 + 5;
            int y = startY + row * 90;
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)type), b -> {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketPlaceMapMarker(this.worldX, this.worldZ, type));
                if (!this.minecraft.player.isAlive()) {
                    this.minecraft.setScreen((Screen)new AASDeathScreen(null, false));
                } else {
                    this.minecraft.setScreen((Screen)new SquadSelectionScreen());
                }
            }).bounds(x, y + 45, 80, 20).build());
            ++i;
        }
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        if (!this.minecraft.player.isAlive()) {
            gui.fill(0, 0, this.width, this.height, -16777216);
        } else {
            this.renderBackground(gui);
        }
        gui.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        int totalWidth = 360;
        int startX = (this.width - totalWidth) / 2;
        int startY = 40;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        int i = 0;
        for (ResourceLocation icon : this.markers.values()) {
            int row = i / 4;
            int col = i % 4;
            int iconX = startX + col * 90 + 29;
            int iconY = startY + row * 90 + 5;
            gui.blit(icon, iconX, iconY, 0.0f, 0.0f, 32, 32, 32, 32);
            ++i;
        }
        super.render(gui, mx, my, pt);
    }

    public boolean isPauseScreen() {
        return false;
    }
}


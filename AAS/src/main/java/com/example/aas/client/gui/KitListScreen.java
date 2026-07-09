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

import com.example.aas.client.AASClipboard;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketOpenKitEditor;
import com.example.aas.network.PacketPasteKit;
import com.example.aas.network.PacketPasteTeam;
import com.example.aas.network.PacketRequestKitData;
import com.example.aas.world.AASWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class KitListScreen
extends Screen {
    private final String team;
    static private final int COLUMNS = 4;
    static private final int ROW_HEIGHT = 60;
    static private final int BUTTON_WIDTH = 90;

    public KitListScreen(String team) {
        super((Component)Component.literal((String)(team + " Kits")));
        this.team = team;
    }

    protected void init() {
        int startX = (this.width - 440) / 2;
        int startY = 40;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"COPY ALL"), b -> PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestKitData(this.team, "ALL"))).bounds(startX, 10, 80, 20).build());
        Button pasteAllBtn = Button.builder((Component)Component.literal((String)"PASTE ALL"), b -> {
            if (AASClipboard.teamKitsData != null) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketPasteTeam(this.team, AASClipboard.teamKitsData));
            }
        }).bounds(startX + 85, 10, 80, 20).build();
        pasteAllBtn.active = AASClipboard.teamKitsData != null;
        this.addRenderableWidget((GuiEventListener)pasteAllBtn);
        for (int i = 0; i < AASWorldData.KIT_NAMES.length; ++i) {
            String kitName = AASWorldData.KIT_NAMES[i];
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * 110;
            int y = startY + row * 60 + 26;
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)kitName), b -> PacketHandler.INSTANCE.sendToServer((Object)new PacketOpenKitEditor(this.team, kitName))).bounds(x, y, 70, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"C"), b -> PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestKitData(this.team, kitName))).bounds(x + 72, y, 15, 20).build());
            Button pBtn = Button.builder((Component)Component.literal((String)"P"), b -> {
                if (AASClipboard.kitData != null) {
                    PacketHandler.INSTANCE.sendToServer((Object)new PacketPasteKit(this.team, kitName, AASClipboard.kitData));
                }
            }).bounds(x + 89, y, 15, 20).build();
            pBtn.active = AASClipboard.kitData != null;
            this.addRenderableWidget((GuiEventListener)pBtn);
        }
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        this.renderBackground(gui);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        int startX = (this.width - 440) / 2;
        int startY = 40;
        for (int i = 0; i < AASWorldData.KIT_NAMES.length; ++i) {
            String kitName = AASWorldData.KIT_NAMES[i];
            int row = i / 4;
            int col = i % 4;
            String iconPath = kitName.toLowerCase().replace(" ", "_").replace("-", "_");
            ResourceLocation iconLoc = new ResourceLocation("aas", "textures/gui/kits/" + iconPath + ".png");
            int iconX = startX + col * 110 + 35 - 12;
            int iconY = startY + row * 60;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.blit(iconLoc, iconX, iconY, 0.0f, 0.0f, 24, 24, 24, 24);
        }
        super.render(gui, mx, my, pt);
        if (mx > (this.width - 440) / 2) {
            // empty if block
        }
    }
}


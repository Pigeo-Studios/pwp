/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Renderable
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package com.example.aas.client.gui;

import com.example.aas.client.AASDeathScreen;
import com.example.aas.client.gui.KitPreviewScreen;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketOpenPlayerKitMenu;
import com.example.aas.network.PacketSelectKit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PlayerKitSelectScreen
extends Screen {
    private final List<PacketOpenPlayerKitMenu.KitDTO> kits;
    static private final int COLUMNS = 4;
    static private final int BUTTON_WIDTH = 90;
    static private final int ROW_HEIGHT = 60;
    static private final Component EYE_ICON = Component.literal((String)"\ud83d\udc41");

    public PlayerKitSelectScreen(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
        super((Component)Component.literal((String)"Select Class"));
        this.kits = kits;
    }

    protected void init() {
        int startX = (this.width - 400) / 2;
        int startY = 40;
        int i = 0;
        for (PacketOpenPlayerKitMenu.KitDTO kit : this.kits) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * 100;
            int y = startY + row * 60 + 26;
            Button btn = Button.builder((Component)Component.literal((String)kit.name), b -> {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSelectKit(kit.name));
                this.onClose();
            }).bounds(x, y, 70, 20).build();
            btn.active = kit.available;
            this.addRenderableWidget((GuiEventListener)btn);
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)EYE_ICON, b -> this.minecraft.setScreen((Screen)new KitPreviewScreen(this, kit.name, kit.items))).bounds(x + 90 - 18, y, 20, 20).build());
            ++i;
        }
    }

    public void onClose() {
        if (this.minecraft.player != null && !this.minecraft.player.isAlive()) {
            this.minecraft.setScreen((Screen)new AASDeathScreen(null, false));
        } else {
            super.onClose();
        }
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        if (!this.minecraft.player.isAlive()) {
            gui.fill(0, 0, this.width, this.height, -16777216);
        } else {
            this.renderBackground(gui);
        }
        gui.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        int startX = (this.width - 400) / 2;
        int startY = 40;
        for (int i = 0; i < this.kits.size(); ++i) {
            PacketOpenPlayerKitMenu.KitDTO kit = this.kits.get(i);
            int row = i / 4;
            int col = i % 4;
            String iconName = kit.name.toLowerCase().replace(" ", "_").replace("-", "_");
            ResourceLocation iconLoc = new ResourceLocation("aas", "textures/gui/kits/" + iconName + ".png");
            int iconX = startX + col * 100 + 45 - 12;
            int iconY = startY + row * 60;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float)(kit.available ? 1.0f : 0.4f));
            gui.blit(iconLoc, iconX, iconY, 0.0f, 0.0f, 24, 24, 24, 24);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        super.render(gui, mx, my, pt);
        block1: for (Renderable widget : this.renderables) {
            Button btn;
            if (!(widget instanceof Button) || !(btn = (Button)widget).isHovered() || btn.active) continue;
            for (PacketOpenPlayerKitMenu.KitDTO kit : this.kits) {
                if (!btn.getMessage().getString().equals(kit.name)) continue;
                gui.renderTooltip(this.font, (Component)Component.literal((String)kit.reason), mx, my);
                continue block1;
            }
        }
    }
}


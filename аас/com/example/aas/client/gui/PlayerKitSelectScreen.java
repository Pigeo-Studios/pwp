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
    private static final int COLUMNS = 4;
    private static final int BUTTON_WIDTH = 90;
    private static final int ROW_HEIGHT = 60;
    private static final Component EYE_ICON = Component.m_237113_((String)"\ud83d\udc41");

    public PlayerKitSelectScreen(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
        super((Component)Component.m_237113_((String)"Select Class"));
        this.kits = kits;
    }

    protected void m_7856_() {
        int startX = (this.f_96543_ - 400) / 2;
        int startY = 40;
        int i = 0;
        for (PacketOpenPlayerKitMenu.KitDTO kit : this.kits) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * 100;
            int y = startY + row * 60 + 26;
            Button btn = Button.m_253074_((Component)Component.m_237113_((String)kit.name), b -> {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSelectKit(kit.name));
                this.m_7379_();
            }).m_252987_(x, y, 70, 20).m_253136_();
            btn.f_93623_ = kit.available;
            this.m_142416_((GuiEventListener)btn);
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)EYE_ICON, b -> this.f_96541_.m_91152_((Screen)new KitPreviewScreen(this, kit.name, kit.items))).m_252987_(x + 90 - 18, y, 20, 20).m_253136_());
            ++i;
        }
    }

    public void m_7379_() {
        if (this.f_96541_.f_91074_ != null && !this.f_96541_.f_91074_.m_6084_()) {
            this.f_96541_.m_91152_((Screen)new AASDeathScreen(null, false));
        } else {
            super.m_7379_();
        }
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        if (!this.f_96541_.f_91074_.m_6084_()) {
            gui.m_280509_(0, 0, this.f_96543_, this.f_96544_, -16777216);
        } else {
            this.m_280273_(gui);
        }
        gui.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 10, 0xFFFFFF);
        int startX = (this.f_96543_ - 400) / 2;
        int startY = 40;
        for (int i = 0; i < this.kits.size(); ++i) {
            PacketOpenPlayerKitMenu.KitDTO kit = this.kits.get(i);
            int row = i / 4;
            int col = i % 4;
            String iconName = kit.name.toLowerCase().replace(" ", "_").replace("-", "_");
            ResourceLocation iconLoc = new ResourceLocation("aas", "textures/gui/kits/" + iconName + ".png");
            int iconX = startX + col * 100 + 45 - 12;
            int iconY = startY + row * 60;
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)(kit.available ? 1.0f : 0.4f));
            gui.m_280163_(iconLoc, iconX, iconY, 0.0f, 0.0f, 24, 24, 24, 24);
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        super.m_88315_(gui, mx, my, pt);
        block1: for (Renderable widget : this.f_169369_) {
            Button btn;
            if (!(widget instanceof Button) || !(btn = (Button)widget).m_274382_() || btn.f_93623_) continue;
            for (PacketOpenPlayerKitMenu.KitDTO kit : this.kits) {
                if (!btn.m_6035_().getString().equals(kit.name)) continue;
                gui.m_280557_(this.f_96547_, (Component)Component.m_237113_((String)kit.reason), mx, my);
                continue block1;
            }
        }
    }
}


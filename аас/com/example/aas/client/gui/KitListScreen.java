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
    private static final int COLUMNS = 4;
    private static final int ROW_HEIGHT = 60;
    private static final int BUTTON_WIDTH = 90;

    public KitListScreen(String team) {
        super((Component)Component.m_237113_((String)(team + " Kits")));
        this.team = team;
    }

    protected void m_7856_() {
        int startX = (this.f_96543_ - 440) / 2;
        int startY = 40;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"COPY ALL"), b -> PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestKitData(this.team, "ALL"))).m_252987_(startX, 10, 80, 20).m_253136_());
        Button pasteAllBtn = Button.m_253074_((Component)Component.m_237113_((String)"PASTE ALL"), b -> {
            if (AASClipboard.teamKitsData != null) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketPasteTeam(this.team, AASClipboard.teamKitsData));
            }
        }).m_252987_(startX + 85, 10, 80, 20).m_253136_();
        pasteAllBtn.f_93623_ = AASClipboard.teamKitsData != null;
        this.m_142416_((GuiEventListener)pasteAllBtn);
        for (int i = 0; i < AASWorldData.KIT_NAMES.length; ++i) {
            String kitName = AASWorldData.KIT_NAMES[i];
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * 110;
            int y = startY + row * 60 + 26;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)kitName), b -> PacketHandler.INSTANCE.sendToServer((Object)new PacketOpenKitEditor(this.team, kitName))).m_252987_(x, y, 70, 20).m_253136_());
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"C"), b -> PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestKitData(this.team, kitName))).m_252987_(x + 72, y, 15, 20).m_253136_());
            Button pBtn = Button.m_253074_((Component)Component.m_237113_((String)"P"), b -> {
                if (AASClipboard.kitData != null) {
                    PacketHandler.INSTANCE.sendToServer((Object)new PacketPasteKit(this.team, kitName, AASClipboard.kitData));
                }
            }).m_252987_(x + 89, y, 15, 20).m_253136_();
            pBtn.f_93623_ = AASClipboard.kitData != null;
            this.m_142416_((GuiEventListener)pBtn);
        }
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        this.m_280273_(gui);
        gui.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 10, 0xFFFFFF);
        int startX = (this.f_96543_ - 440) / 2;
        int startY = 40;
        for (int i = 0; i < AASWorldData.KIT_NAMES.length; ++i) {
            String kitName = AASWorldData.KIT_NAMES[i];
            int row = i / 4;
            int col = i % 4;
            String iconPath = kitName.toLowerCase().replace(" ", "_").replace("-", "_");
            ResourceLocation iconLoc = new ResourceLocation("aas", "textures/gui/kits/" + iconPath + ".png");
            int iconX = startX + col * 110 + 35 - 12;
            int iconY = startY + row * 60;
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            gui.m_280163_(iconLoc, iconX, iconY, 0.0f, 0.0f, 24, 24, 24, 24);
        }
        super.m_88315_(gui, mx, my, pt);
        if (mx > (this.f_96543_ - 440) / 2) {
            // empty if block
        }
    }
}


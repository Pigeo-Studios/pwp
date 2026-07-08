/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package com.example.aas.client.gui;

import com.example.aas.block.HubBlockEntity;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRequestAmmo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public class HubAmmoScreen
extends Screen {
    private final BlockPos hubPos;
    private final int boxSize = 60;
    private final int gap = 20;
    private int cachedCdAGS = 0;
    private int cachedCdM2 = 0;

    public HubAmmoScreen(BlockPos pos) {
        super((Component)Component.m_237113_((String)"Hub Supply"));
        this.hubPos = pos;
    }

    public boolean m_7043_() {
        return false;
    }

    private void updateCooldowns() {
        BlockEntity be;
        if (Minecraft.m_91087_().f_91073_ != null && (be = Minecraft.m_91087_().f_91073_.m_7702_(this.hubPos)) instanceof HubBlockEntity) {
            HubBlockEntity hub = (HubBlockEntity)be;
            this.cachedCdAGS = hub.cooldownAGS;
            this.cachedCdM2 = hub.cooldownM2;
        }
    }

    public void m_88315_(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(gui);
        this.updateCooldowns();
        int centerX = this.f_96543_ / 2;
        int centerY = this.f_96544_ / 2;
        int leftX = centerX - 60 - 10;
        int leftY = centerY - 30;
        boolean hoverLeft = mouseX >= leftX && mouseX <= leftX + 60 && mouseY >= leftY && mouseY <= leftY + 60;
        int colorLeft = -1;
        if (this.cachedCdAGS > 0) {
            colorLeft = -43691;
        } else if (hoverLeft) {
            colorLeft = -11141291;
        }
        this.renderBox(gui, leftX, leftY, 60, colorLeft, "AGS-30", this.cachedCdAGS);
        int rightX = centerX + 10;
        int rightY = centerY - 30;
        boolean hoverRight = mouseX >= rightX && mouseX <= rightX + 60 && mouseY >= rightY && mouseY <= rightY + 60;
        int colorRight = -1;
        if (this.cachedCdM2 > 0) {
            colorRight = -43691;
        } else if (hoverRight) {
            colorRight = -11141291;
        }
        this.renderBox(gui, rightX, rightY, 60, colorRight, "M2 Ammo", this.cachedCdM2);
        super.m_88315_(gui, mouseX, mouseY, partialTick);
    }

    private void renderBox(GuiGraphics gui, int x, int y, int size, int color, String label, int cooldown) {
        gui.m_280509_(x - 2, y - 2, x + size + 2, y + size + 2, -16777216);
        int bg = color & 0xFFFFFF | Integer.MIN_VALUE;
        gui.m_280509_(x, y, x + size, y + size, bg);
        gui.m_280637_(x, y, size, size, color);
        int labelWidth = this.f_96547_.m_92895_(label);
        gui.m_280056_(this.f_96547_, label, x + (size - labelWidth) / 2, y + size / 2 - 10, color, true);
        if (cooldown > 0) {
            String time = cooldown / 20 + "s";
            int timeW = this.f_96547_.m_92895_(time);
            gui.m_280056_(this.f_96547_, time, x + (size - timeW) / 2, y + size / 2 + 5, -171, true);
        }
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.updateCooldowns();
            int centerX = this.f_96543_ / 2;
            int centerY = this.f_96544_ / 2;
            int leftX = centerX - 60 - 10;
            int rightX = centerX + 10;
            int boxY = centerY - 30;
            if (mouseX >= (double)leftX && mouseX <= (double)(leftX + 60) && mouseY >= (double)boxY && mouseY <= (double)(boxY + 60) && this.cachedCdAGS == 0) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestAmmo(this.hubPos, 0));
                this.m_7379_();
                return true;
            }
            if (mouseX >= (double)rightX && mouseX <= (double)(rightX + 60) && mouseY >= (double)boxY && mouseY <= (double)(boxY + 60) && this.cachedCdM2 == 0) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestAmmo(this.hubPos, 1));
                this.m_7379_();
                return true;
            }
        }
        return super.m_6375_(mouseX, mouseY, button);
    }
}


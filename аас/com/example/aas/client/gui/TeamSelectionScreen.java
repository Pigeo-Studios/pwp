/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package com.example.aas.client.gui;

import com.example.aas.client.ClientData;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketTeamSelect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TeamSelectionScreen
extends Screen {
    private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("aas", "textures/gui/flags/ukraine.png");
    private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("aas", "textures/gui/flags/russia.png");
    private static final ResourceLocation FLAG_USA = new ResourceLocation("aas", "textures/gui/flags/usa.png");
    private static final ResourceLocation FLAG_NATO = new ResourceLocation("aas", "textures/gui/flags/nato.png");
    private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
    private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("aas", "textures/gui/flags/redfor.png");
    private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("aas", "textures/gui/flags/insurgency.png");
    private static final ResourceLocation FLAG_PMC = new ResourceLocation("aas", "textures/gui/flags/pmc.png");

    public TeamSelectionScreen() {
        super((Component)Component.m_237113_((String)"Choose Team"));
    }

    public void m_88315_(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(gui);
        gui.m_280509_(0, 0, this.f_96543_, this.f_96544_, Integer.MIN_VALUE);
        int centerX = this.f_96543_ / 2;
        int centerY = this.f_96544_ / 2;
        int flagWidth = 64;
        int flagHeight = 36;
        int offset = 60;
        int blueX = centerX - offset - flagWidth;
        int blueY = centerY - flagHeight / 2;
        boolean isHoveringBlue = mouseX >= blueX && mouseX <= blueX + flagWidth && mouseY >= blueY && mouseY <= blueY + flagHeight + 20;
        ResourceLocation blueFlag = this.getFlagTexture(ClientData.BLUE_FACTION);
        if (blueFlag != null) {
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.m_280163_(blueFlag, blueX, blueY, 0.0f, 0.0f, flagWidth, flagHeight, flagWidth, flagHeight);
        } else {
            gui.m_280509_(blueX, blueY, blueX + flagWidth, blueY + flagHeight, -16777046);
        }
        if (isHoveringBlue) {
            gui.m_280637_(blueX - 1, blueY - 1, flagWidth + 2, flagHeight + 2, -1);
        }
        String blueTeamName = ClientData.customBlueName;
        if (ClientData.BLUE_FACTION != null && !ClientData.BLUE_FACTION.equals("none") && !ClientData.BLUE_FACTION.equals("bluefor")) {
            blueTeamName = ClientData.BLUE_FACTION.replace("_", " ").toUpperCase();
        }
        int blueJoinColor = isHoveringBlue ? -1 : -22016;
        gui.m_280653_(this.f_96547_, (Component)Component.m_237113_((String)"JOIN").m_130940_(ChatFormatting.GOLD), blueX + flagWidth / 2, blueY + flagHeight + 10, blueJoinColor);
        gui.m_280653_(this.f_96547_, (Component)Component.m_237113_((String)blueTeamName).m_130940_(ChatFormatting.BLUE), blueX + flagWidth / 2, blueY - 15, 0xFFFFFF);
        int redX = centerX + offset;
        int redY = centerY - flagHeight / 2;
        boolean isHoveringRed = mouseX >= redX && mouseX <= redX + flagWidth && mouseY >= redY && mouseY <= redY + flagHeight + 20;
        ResourceLocation redFlag = this.getFlagTexture(ClientData.RED_FACTION);
        if (redFlag != null) {
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.m_280163_(redFlag, redX, redY, 0.0f, 0.0f, flagWidth, flagHeight, flagWidth, flagHeight);
        } else {
            gui.m_280509_(redX, redY, redX + flagWidth, redY + flagHeight, -5636096);
        }
        if (isHoveringRed) {
            gui.m_280637_(redX - 1, redY - 1, flagWidth + 2, flagHeight + 2, -1);
        }
        String redTeamName = ClientData.customRedName;
        if (ClientData.RED_FACTION != null && !ClientData.RED_FACTION.equals("none") && !ClientData.RED_FACTION.equals("redfor")) {
            redTeamName = ClientData.RED_FACTION.replace("_", " ").toUpperCase();
        }
        int redJoinColor = isHoveringRed ? -1 : -22016;
        gui.m_280653_(this.f_96547_, (Component)Component.m_237113_((String)"JOIN").m_130940_(ChatFormatting.GOLD), redX + flagWidth / 2, redY + flagHeight + 10, redJoinColor);
        gui.m_280653_(this.f_96547_, (Component)Component.m_237113_((String)redTeamName).m_130940_(ChatFormatting.RED), redX + flagWidth / 2, redY - 15, 0xFFFFFF);
        super.m_88315_(gui, mouseX, mouseY, partialTick);
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = this.f_96543_ / 2;
            int centerY = this.f_96544_ / 2;
            int flagWidth = 64;
            int flagHeight = 36;
            int offset = 60;
            int blueX = centerX - offset - flagWidth;
            int blueY = centerY - flagHeight / 2;
            if (mouseX >= (double)blueX && mouseX <= (double)(blueX + flagWidth) && mouseY >= (double)blueY && mouseY <= (double)(blueY + flagHeight + 20)) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketTeamSelect("BLUE"));
                this.m_7379_();
                return true;
            }
            int redX = centerX + offset;
            int redY = centerY - flagHeight / 2;
            if (mouseX >= (double)redX && mouseX <= (double)(redX + flagWidth) && mouseY >= (double)redY && mouseY <= (double)(redY + flagHeight + 20)) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketTeamSelect("RED"));
                this.m_7379_();
                return true;
            }
        }
        return super.m_6375_(mouseX, mouseY, button);
    }

    public boolean m_7043_() {
        return false;
    }

    private ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equals("none")) {
            return null;
        }
        switch (faction.toLowerCase()) {
            case "ukraine": {
                return FLAG_UKRAINE;
            }
            case "russia": {
                return FLAG_RUSSIA;
            }
            case "usa": {
                return FLAG_USA;
            }
            case "bluefor": {
                return new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
            }
            case "redfor": {
                return new ResourceLocation("aas", "textures/gui/flags/redfor.png");
            }
            case "nato": {
                return FLAG_NATO;
            }
            case "insurgency": {
                return FLAG_INSURGENCY;
            }
            case "pmc": {
                return FLAG_PMC;
            }
        }
        return null;
    }
}


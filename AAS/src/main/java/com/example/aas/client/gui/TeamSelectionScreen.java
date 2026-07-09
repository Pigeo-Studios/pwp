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
    static private final ResourceLocation FLAG_UKRAINE = new ResourceLocation("aas", "textures/gui/flags/ukraine.png");
    static private final ResourceLocation FLAG_RUSSIA = new ResourceLocation("aas", "textures/gui/flags/russia.png");
    static private final ResourceLocation FLAG_USA = new ResourceLocation("aas", "textures/gui/flags/usa.png");
    static private final ResourceLocation FLAG_NATO = new ResourceLocation("aas", "textures/gui/flags/nato.png");
    static private final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
    static private final ResourceLocation FLAG_REDFOR = new ResourceLocation("aas", "textures/gui/flags/redfor.png");
    static private final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("aas", "textures/gui/flags/insurgency.png");
    static private final ResourceLocation FLAG_PMC = new ResourceLocation("aas", "textures/gui/flags/pmc.png");

    public TeamSelectionScreen() {
        super((Component)Component.literal((String)"Choose Team"));
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        gui.fill(0, 0, this.width, this.height, Integer.MIN_VALUE);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int flagWidth = 64;
        int flagHeight = 36;
        int offset = 60;
        int blueX = centerX - offset - flagWidth;
        int blueY = centerY - flagHeight / 2;
        boolean isHoveringBlue = mouseX >= blueX && mouseX <= blueX + flagWidth && mouseY >= blueY && mouseY <= blueY + flagHeight + 20;
        ResourceLocation blueFlag = this.getFlagTexture(ClientData.BLUE_FACTION);
        if (blueFlag != null) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.blit(blueFlag, blueX, blueY, 0.0f, 0.0f, flagWidth, flagHeight, flagWidth, flagHeight);
        } else {
            gui.fill(blueX, blueY, blueX + flagWidth, blueY + flagHeight, -16777046);
        }
        if (isHoveringBlue) {
            gui.renderOutline(blueX - 1, blueY - 1, flagWidth + 2, flagHeight + 2, -1);
        }
        String blueTeamName = ClientData.customBlueName;
        if (ClientData.BLUE_FACTION != null && !ClientData.BLUE_FACTION.equals("none") && !ClientData.BLUE_FACTION.equals("bluefor")) {
            blueTeamName = ClientData.BLUE_FACTION.replace("_", " ").toUpperCase();
        }
        int blueJoinColor = isHoveringBlue ? -1 : -22016;
        gui.drawCenteredString(this.font, (Component)Component.literal((String)"JOIN").withStyle(ChatFormatting.GOLD), blueX + flagWidth / 2, blueY + flagHeight + 10, blueJoinColor);
        gui.drawCenteredString(this.font, (Component)Component.literal((String)blueTeamName).withStyle(ChatFormatting.BLUE), blueX + flagWidth / 2, blueY - 15, 0xFFFFFF);
        int redX = centerX + offset;
        int redY = centerY - flagHeight / 2;
        boolean isHoveringRed = mouseX >= redX && mouseX <= redX + flagWidth && mouseY >= redY && mouseY <= redY + flagHeight + 20;
        ResourceLocation redFlag = this.getFlagTexture(ClientData.RED_FACTION);
        if (redFlag != null) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.blit(redFlag, redX, redY, 0.0f, 0.0f, flagWidth, flagHeight, flagWidth, flagHeight);
        } else {
            gui.fill(redX, redY, redX + flagWidth, redY + flagHeight, -5636096);
        }
        if (isHoveringRed) {
            gui.renderOutline(redX - 1, redY - 1, flagWidth + 2, flagHeight + 2, -1);
        }
        String redTeamName = ClientData.customRedName;
        if (ClientData.RED_FACTION != null && !ClientData.RED_FACTION.equals("none") && !ClientData.RED_FACTION.equals("redfor")) {
            redTeamName = ClientData.RED_FACTION.replace("_", " ").toUpperCase();
        }
        int redJoinColor = isHoveringRed ? -1 : -22016;
        gui.drawCenteredString(this.font, (Component)Component.literal((String)"JOIN").withStyle(ChatFormatting.GOLD), redX + flagWidth / 2, redY + flagHeight + 10, redJoinColor);
        gui.drawCenteredString(this.font, (Component)Component.literal((String)redTeamName).withStyle(ChatFormatting.RED), redX + flagWidth / 2, redY - 15, 0xFFFFFF);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int flagWidth = 64;
            int flagHeight = 36;
            int offset = 60;
            int blueX = centerX - offset - flagWidth;
            int blueY = centerY - flagHeight / 2;
            if (mouseX >= (double)blueX && mouseX <= (double)(blueX + flagWidth) && mouseY >= (double)blueY && mouseY <= (double)(blueY + flagHeight + 20)) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketTeamSelect("BLUE"));
                this.onClose();
                return true;
            }
            int redX = centerX + offset;
            int redY = centerY - flagHeight / 2;
            if (mouseX >= (double)redX && mouseX <= (double)(redX + flagWidth) && mouseY >= (double)redY && mouseY <= (double)(redY + flagHeight + 20)) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketTeamSelect("RED"));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean isPauseScreen() {
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


package com.pigeostudios.pwp.warfare.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class SquadButton extends Button {
    public SquadButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        int borderColor = this.isHovered() ? -1 : -6710887;
        if (!this.active) {
            borderColor = -12303292;
        }
        gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -871296751);
        gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
        int textColor = this.active ? -1 : -8947849;
        gui.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
        if (this.active && this.isHovered()) {
            gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height, -1);
        }
    }
}

package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.BooleanSupplier;

public class PWPModal {

    private final int screenW, screenH;
    private final int panelW, panelH;
    private final int panelX, panelY;
    private boolean open;

    public PWPModal(int screenW, int screenH, int panelW, int panelH) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.panelW = panelW;
        this.panelH = panelH;
        this.panelX = (screenW - panelW) / 2;
        this.panelY = (screenH - panelH) / 2;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isOpen() {
        return open;
    }

    public int getPanelX() { return panelX; }
    public int getPanelY() { return panelY; }
    public int getPanelW() { return panelW; }
    public int getPanelH() { return panelH; }

    public void renderDim(GuiGraphics gui) {
        if (!open) return;
        gui.fill(0, 0, screenW, screenH, PWPTheme.Colors.BACKGROUND_DIM);
    }

    public void renderPanel(GuiGraphics gui) {
        if (!open) return;
        PWPPanel.render(gui, panelX, panelY, panelW, panelH, PWPPanel.Variant.SURFACE, true);
    }

    public boolean isInsidePanel(int mouseX, int mouseY) {
        return mouseX >= panelX && mouseX <= panelX + panelW
            && mouseY >= panelY && mouseY <= panelY + panelH;
    }

    public boolean blockClickIfOutside(int mouseX, int mouseY, int button) {
        if (!open) return false;
        if (!isInsidePanel(mouseX, mouseY) && button == 0) {
            open = false;
            return true;
        }
        return false;
    }
}

package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PWPCard {

    public enum State {
        DEFAULT, HOVER, SELECTED
    }

    private PWPCard() {}

    public static void render(GuiGraphics gui, int x, int y, int w, int h, State state) {
        int bg, borderColor;

        switch (state) {
            case SELECTED:
                bg = PWPTheme.Styles.Card.BG_SELECTED;
                borderColor = PWPTheme.Styles.Card.BORDER_SELECTED;
                break;
            case HOVER:
                bg = PWPTheme.Styles.Card.BG_HOVER;
                borderColor = PWPTheme.Styles.Card.BORDER_HOVER;
                break;
            default:
                bg = PWPTheme.Styles.Card.BG;
                borderColor = PWPTheme.Styles.Card.BORDER;
                break;
        }

        int r = PWPTheme.Spacing.RADIUS_SMALL;

        gui.fill(x + r, y, x + w - r, y + h, bg);
        gui.fill(x, y + r, x + r, y + h - r, bg);
        gui.fill(x + w - r, y + r, x + w, y + h - r, bg);
        gui.fill(x + r, y + r, x + w - r, y + h - r, bg);

        gui.fill(x + r, y, x + w - r, y + 1, borderColor);
        gui.fill(x + r, y + h - 1, x + w - r, y + h, borderColor);
        gui.fill(x, y + r, x + 1, y + h - r, borderColor);
        gui.fill(x + w - 1, y + r, x + w, y + h - r, borderColor);
        gui.fill(x + r, y, x + r + 1, y + 1, borderColor);
        gui.fill(x + w - r - 1, y, x + w - r, y + 1, borderColor);
        gui.fill(x + r, y + h - 1, x + r + 1, y + h, borderColor);
        gui.fill(x + w - r - 1, y + h - 1, x + w - r, y + h, borderColor);
    }

    public static boolean isHovered(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public static State getState(boolean selected, boolean hovered) {
        if (selected) return State.SELECTED;
        if (hovered) return State.HOVER;
        return State.DEFAULT;
    }
}

package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

public class PWPCard {

    public enum State {
        DEFAULT, HOVER, SELECTED, DISABLED
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
            case DISABLED:
                bg = PWPTheme.Colors.SURFACE_DIM;
                borderColor = PWPTheme.Colors.BORDER;
                break;
            default:
                bg = PWPTheme.Styles.Card.BG;
                borderColor = PWPTheme.Styles.Card.BORDER;
                break;
        }

        int r = PWPTheme.Spacing.RADIUS_MEDIUM;

        if (state == State.HOVER) {
            RoundedRect.fill(gui, x, y + 2, w, h, r, PWPTheme.Styles.Card.SHADOW);
        } else if (state == State.SELECTED) {
            RoundedRect.glow(gui, x, y, w, h, r, 2, PWPTheme.Styles.Card.GLOW_SELECTED);
        }

        RoundedRect.fill(gui, x, y, w, h, r, bg);
        RoundedRect.border(gui, x, y, w, h, r, 1, borderColor);
    }

    public static boolean isHovered(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public static State getState(boolean selected, boolean hovered) {
        return getState(selected, hovered, false);
    }

    public static State getState(boolean selected, boolean hovered, boolean disabled) {
        if (disabled) return State.DISABLED;
        if (selected) return State.SELECTED;
        if (hovered) return State.HOVER;
        return State.DEFAULT;
    }
}

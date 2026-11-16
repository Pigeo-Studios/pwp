package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Легаси-фон карточки (API прежний: render/isHovered/getState) в тактическом стиле UI-кита:
 * полупрозрачная заливка + блик + 1px обводка, у выбранной — акцентная полоса слева, у
 * наведённой — тень. Новые экраны — {@link com.pwp.coreclient.ui.UICard}.
 */
public class PWPCard {

    public enum State {
        DEFAULT,
        HOVER,
        SELECTED,
        DISABLED
    }

    private PWPCard() {}

    public static void render(GuiGraphics gui, int x, int y, int w, int h, State state) {
        Rect r = Rect.of(x, y, w, h);
        int bg, border;
        switch (state) {
            case SELECTED -> {
                bg = Styles.Control.BG_SELECTED;
                border = Styles.Control.BORDER_SELECTED;
            }
            case HOVER -> {
                bg = Styles.Control.BG_HOVER;
                border = Styles.Control.BORDER_HOVER;
            }
            case DISABLED -> {
                bg = Styles.Control.BG_DISABLED;
                border = Styles.Control.BORDER_DISABLED;
            }
            default -> {
                bg = Styles.Control.BG;
                border = Styles.Control.BORDER;
            }
        }
        if (state == State.HOVER || state == State.SELECTED) UIDraw.shadow(gui, r, 0.6F);
        UIDraw.rect(gui, r, bg);
        if (state != State.DISABLED) UIDraw.gradientV(gui, r, Styles.Control.SHEEN_TOP, Styles.Control.SHEEN_BOTTOM);
        UIDraw.outline(gui, r, border);
        if (state == State.SELECTED) {
            UIDraw.accentBar(gui, x, y, h, Spacing.ACCENT_BAR_SELECTED, Styles.Control.ACCENT_BAR);
        } else if (state == State.HOVER) {
            UIDraw.accentBar(gui, x, y, h, Spacing.ACCENT_BAR, Styles.Control.ACCENT_BAR);
        }
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

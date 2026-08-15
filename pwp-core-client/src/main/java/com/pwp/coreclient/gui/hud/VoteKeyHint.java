package com.pwp.coreclient.gui.hud;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Кейкап-клавиша [TAB] — отдельный переиспользуемый компонент.
 * Только визуальный элемент: измерение текста, отрисовка, layout.
 * Ширина рассчитывается по фактической ширине текста — не ломается при GUI Scale.
 */
public final class VoteKeyHint {

    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 3;

    private VoteKeyHint() {}

    public static int width(Font font, String label) {
        return font.width(label) + PADDING_X * 2;
    }

    public static int height(Font font) {
        return font.lineHeight + PADDING_Y * 2;
    }

    public static void render(GuiGraphics gui, Font font, int x, int y, String label, int textColor) {
        int w = width(font, label);
        int h = height(font);
        RoundedRect.fill(gui, x, y, w, h, PWPTheme.Spacing.RADIUS_SMALL, PWPTheme.Colors.BACKGROUND);
        RoundedRect.border(gui, x, y, w, h, PWPTheme.Spacing.RADIUS_SMALL, 1, PWPTheme.Colors.BORDER_LIGHT);
        gui.drawString(font, Component.literal(label), x + PADDING_X, y + PADDING_Y + 1, textColor, false);
    }
}

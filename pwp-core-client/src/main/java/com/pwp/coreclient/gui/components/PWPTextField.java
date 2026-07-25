package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class PWPTextField {

    private PWPTextField() {}

    public static EditBox create(Font font, int x, int y, int w, int h, String placeholder) {
        EditBox box = new EditBox(font, x, y, w, h, Component.literal(placeholder));
        box.setBordered(false);
        box.setTextColor(PWPTheme.Colors.TEXT_PRIMARY);
        return box;
    }

    public static void renderBackground(GuiGraphics gui, EditBox box) {
        int x = box.getX();
        int y = box.getY();
        int w = box.getWidth();
        int h = box.getHeight();
        int r = PWPTheme.Spacing.RADIUS_SMALL;

        int bg = box.isFocused() ? PWPTheme.Styles.Input.BG_FOCUS : PWPTheme.Styles.Input.BG;
        int border = box.isFocused() ? PWPTheme.Styles.Input.BORDER_FOCUS : PWPTheme.Styles.Input.BORDER;

        RoundedRect.fill(gui, x, y, w, h, r, bg);
        RoundedRect.border(gui, x, y, w, h, r, 1, border);

        if (box.getValue().isEmpty() && !box.isFocused()) {
            var font = PWPTheme.Fonts.display();
            String msg = box.getMessage().getString();
            gui.drawString(font, Component.literal(msg), x + 4, y + (h - 8) / 2, PWPTheme.Styles.Input.PLACEHOLDER);
        }
    }
}

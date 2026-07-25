package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class PWPContextMenu {

    private int x, y, w, h;
    private List<String> options;
    private Consumer<Integer> onClick;
    private boolean visible;

    public PWPContextMenu() {
        this.visible = false;
    }

    public void show(int mx, int my, List<String> options, Consumer<Integer> onClick) {
        this.x = mx;
        this.y = my;
        this.options = options;
        this.onClick = onClick;
        this.w = 110;
        this.h = options.size() * 14 + 6;
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || button != 0) return false;
        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            int idx = (int) ((my - y - 3) / 14);
            if (idx >= 0 && idx < options.size() && onClick != null) {
                onClick.accept(idx);
            }
            visible = false;
            return true;
        }
        visible = false;
        return false;
    }

    public void render(GuiGraphics gui, int mx, int my) {
        if (!visible || options == null) return;

        gui.fill(x, y, x + w, y + h, 0xCC0E1117);
        RoundedRect.border(gui, x, y, w, h, PWPTheme.Spacing.RADIUS_SMALL, 1, PWPTheme.Colors.BORDER_LIGHT);

        var font = PWPTheme.Fonts.display();
        for (int i = 0; i < options.size(); i++) {
            int iy = y + 3 + i * 14;
            boolean hover = mx >= x && mx <= x + w && my >= iy && my <= iy + 12;
            if (hover) {
                gui.fill(x + 1, iy, x + w - 1, iy + 12, PWPTheme.Colors.SURFACE_LIGHT);
            }
            int color = hover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY;
            gui.drawString(font, Component.literal(options.get(i)), x + 6, iy + 2, color, false);
        }
    }
}

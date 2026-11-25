package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Легаси-контекстное меню (API прежний) в стиле UI-кита: стеклянная плашка, пункт под курсором —
 * подсветка + акцентная полоса. Ширина — по самому длинному пункту (не меньше 110), меню
 * не выходит за край экрана. Новые экраны — {@link com.pwp.coreclient.ui.UIContextMenu}.
 */
public class PWPContextMenu {

    private static final int ROW_H = 14;
    private static final int PAD = 3;
    private static final int MIN_W = 110;

    private int x, y, w, h;
    private List<String> options;
    private Consumer<Integer> onClick;
    private boolean visible;

    public PWPContextMenu() {
        this.visible = false;
    }

    public void show(int mx, int my, List<String> options, Consumer<Integer> onClick) {
        this.options = options;
        this.onClick = onClick;
        var font = PWPTheme.Fonts.display();
        int maxText = 0;
        for (String o : options) maxText = Math.max(maxText, font.width(o));
        var win = Minecraft.getInstance().getWindow();
        int sw = win.getGuiScaledWidth(), sh = win.getGuiScaledHeight();
        this.w = Math.min(Math.max(MIN_W, maxText + 2 * Spacing.SM + Spacing.ACCENT_BAR), sw - 2 * Spacing.XS);
        this.h = options.size() * ROW_H + 2 * PAD;
        this.x = UIMath.keepInside(mx, w, sw, Spacing.XS);
        this.y = UIMath.keepInside(my, h, sh, Spacing.XS);
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
            int idx = (int) ((my - y - PAD) / ROW_H);
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
        Rect r = Rect.of(x, y, w, h);
        UIDraw.glass(gui, r, 1F, true);
        var font = PWPTheme.Fonts.display();
        for (int i = 0; i < options.size(); i++) {
            Rect row = Rect.of(x + 1, y + PAD + i * ROW_H, w - 2, ROW_H);
            boolean hover = row.contains(mx, my);
            if (hover) {
                UIDraw.rect(gui, row, Styles.Control.BG_HOVER);
                UIDraw.accentBar(gui, row.x(), row.y(), row.h(), Spacing.ACCENT_BAR, Styles.Control.ACCENT_BAR);
            }
            int color = hover ? Styles.Control.TEXT_HOVER : Styles.Control.TEXT;
            Component label = UIDraw.ellipsize(font, Component.literal(options.get(i)), row.w() - 2 * Spacing.SM);
            UIDraw.text(
                    gui, font, label, row.x() + Spacing.SM, row.y() + (ROW_H - font.lineHeight) / 2 + 1, color, false);
        }
    }
}

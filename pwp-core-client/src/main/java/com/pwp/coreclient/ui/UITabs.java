package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Горизонтальные вкладки равной ширины; подчёркивание активной плавно переезжает. */
@OnlyIn(Dist.CLIENT)
public class UITabs extends UIWidget {
    private final List<Component> labels = new ArrayList<>();
    private final IntConsumer onSelect;
    private final UIAnim indicator = new UIAnim(0F, Animations.DURATION_NORMAL, UIAnim.EASE_OUT_CUBIC);
    private int selected;

    public UITabs(int x, int y, int width, List<Component> labels, IntConsumer onSelect) {
        super(x, y, width, Spacing.BUTTON_TAB_HEIGHT);
        this.labels.addAll(labels);
        this.onSelect = onSelect;
    }

    public int selected() {
        return selected;
    }

    public void select(int index, boolean animate) {
        if (index < 0 || index >= labels.size()) return;
        selected = index;
        if (animate) indicator.to(index);
        else indicator.snap(index);
    }

    private int tabWidth() {
        return labels.isEmpty() ? width : width / labels.size();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int tw = tabWidth();
        for (int i = 0; i < labels.size(); i++) {
            int tx = x + i * tw;
            boolean hovered = mouseX >= tx && mouseX < tx + tw && mouseY >= y && mouseY < y + height;
            int bg = i == selected ? Styles.Tab.BG_ACTIVE : hovered ? Styles.Tab.BG_HOVER : Styles.Tab.BG;
            g.fill(tx, y, tx + tw, y + height, bg);
            int color = i == selected ? Styles.Tab.TEXT_ACTIVE : Styles.Tab.TEXT;
            Component shown = UIDraw.ellipsize(font(), labels.get(i), tw - 2 * Spacing.XS);
            UIDraw.centered(g, font(), shown, tx + tw / 2, y + height / 2 + 1, color);
        }
        g.fill(x, y + height - 1, x + width, y + height, Styles.Tab.BORDER);
        int ix = x + Math.round(indicator.value() * tw);
        g.fill(ix, y + height - 2, ix + tw, y + height, Styles.Tab.BORDER_ACTIVE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || button != 0 || !isMouseOver(mx, my) || labels.isEmpty()) return false;
        int index = Math.min(labels.size() - 1, (int) ((mx - x) / tabWidth()));
        if (index != selected) {
            select(index, true);
            if (onSelect != null) onSelect.accept(index);
        }
        return true;
    }
}

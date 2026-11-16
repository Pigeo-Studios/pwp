package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Горизонтальные вкладки равной ширины в тактическом стиле: без плашек, при наведении —
 * блик, у активной — акцентный текст и подчёркивание, которое плавно переезжает; снизу 1px
 * линия. Подписи обрезаются «…» по ширине вкладки.
 */
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

    /** Ширина, при которой все подписи помещаются без обрезки. */
    public int preferredWidth() {
        int max = 0;
        for (Component c : labels) max = Math.max(max, font().width(c));
        return labels.size() * (max + 2 * Spacing.LOBBY_TAB_PAD);
    }

    private int tabWidth() {
        return labels.isEmpty() ? width : width / labels.size();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int tw = tabWidth();
        for (int i = 0; i < labels.size(); i++) {
            int tx = x + i * tw;
            Rect tab = Rect.of(tx, y, tw, height);
            boolean hovered = enabled && tab.contains(mouseX, mouseY);
            boolean active = i == selected;
            if (hovered || active) {
                UIDraw.gradientV(
                        g,
                        tab,
                        UIDraw.fade(Styles.Control.SHEEN_TOP, opacity),
                        UIDraw.fade(active ? Styles.Control.BG_SELECTED : Styles.Control.BG_HOVER, opacity * 0.6F));
            }
            int color = active ? Styles.Tab.TEXT_ACTIVE : hovered ? Styles.Control.TEXT_HOVER : Styles.Tab.TEXT;
            Component shown = UIDraw.ellipsize(font(), labels.get(i), tw - 2 * Spacing.XS);
            UIDraw.centered(g, font(), shown, tx + tw / 2, y + height / 2 + 1, UIDraw.fade(color, opacity));
        }
        UIDraw.hline(
                g,
                x,
                x + width,
                y + height - 1,
                UIDraw.fade(Colors.withAlpha(Styles.Glass.HEADER_DIVIDER, 0x33), opacity));
        int ix = x + Math.round(indicator.value() * tw);
        UIDraw.accentBarH(
                g,
                ix,
                y + height - Spacing.ACCENT_BAR,
                tw,
                Spacing.ACCENT_BAR,
                UIDraw.fade(Styles.Tab.BORDER_ACTIVE, opacity));
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

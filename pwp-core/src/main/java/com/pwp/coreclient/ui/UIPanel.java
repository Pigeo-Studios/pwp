package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Стеклянная панель ({@link UIDraw#glass}) с необязательной шапкой (иконка + заголовок +
 * акцентная полоса). Контент рисует владелец в {@link #content()}.
 */
@OnlyIn(Dist.CLIENT)
public class UIPanel extends UIWidget {
    private Component title;
    private UIIcon icon;
    private int titleColor = PWPTheme.Text.HEADING;
    private boolean strong;
    private int headerHeight = Spacing.PANEL_HEADER;

    public UIPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public UIPanel() {
        this(0, 0, 0, 0);
    }

    public UIPanel title(Component title) {
        this.title = title;
        return this;
    }

    public UIPanel icon(UIIcon icon) {
        this.icon = icon;
        return this;
    }

    public UIPanel titleColor(int color) {
        this.titleColor = color;
        return this;
    }

    /** Плотная заливка (диалог поверх яркого фона). */
    public UIPanel strong(boolean strong) {
        this.strong = strong;
        return this;
    }

    public UIPanel headerHeight(int h) {
        this.headerHeight = Math.max(0, h);
        return this;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Rect r = bounds();
        UIDraw.glass(g, r, opacity, strong);
        if (title != null) {
            UIDraw.header(g, font(), header(), icon, title, titleColor, opacity);
        }
    }

    /** Область шапки (пустая, если заголовка нет). */
    public Rect header() {
        if (title == null) return Rect.of(x, y, width, 0);
        return Rect.of(x, y, width, headerHeight).inset(Spacing.HAIRLINE, Spacing.HAIRLINE, Spacing.HAIRLINE, 0);
    }

    /** Область контента под шапкой с внутренними полями. */
    public Rect content() {
        int top = title == null ? 0 : headerHeight;
        return bounds().inset(
                        Spacing.PANEL_PADDING,
                        top + Spacing.PANEL_PADDING,
                        Spacing.PANEL_PADDING,
                        Spacing.PANEL_PADDING);
    }

    /** Высота панели под контент заданной высоты. */
    public int heightFor(int contentHeight) {
        int top = title == null ? 0 : headerHeight;
        return top + 2 * Spacing.PANEL_PADDING + Math.max(0, contentHeight);
    }

    /** Верх области контента (под заголовком). */
    public int contentY() {
        return content().y();
    }

    public int contentX() {
        return content().x();
    }

    public int contentWidth() {
        return content().w();
    }
}

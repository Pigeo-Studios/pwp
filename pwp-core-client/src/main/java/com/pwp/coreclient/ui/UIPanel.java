package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Панель: фон + обводка + необязательный заголовок. Контент рисует владелец в {@link #contentY()}. */
@OnlyIn(Dist.CLIENT)
public class UIPanel extends UIWidget {
    private Component title;

    public UIPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public UIPanel title(Component title) {
        this.title = title;
        return this;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UIDraw.box(g, x, y, width, height, Spacing.RADIUS_SMALL, Styles.Panel.BG, Styles.Panel.BORDER);
        if (title != null) {
            int p = Spacing.PANEL_PADDING;
            g.drawString(font(), title, x + p, y + p, PWPTheme.Colors.TEXT_ACCENT, true);
            int lineY = y + p + font().lineHeight + Spacing.XS;
            g.fill(x + p, lineY, x + width - p, lineY + 1, Styles.Divider.COLOR);
        }
    }

    /** Верх области контента (под заголовком). */
    public int contentY() {
        int p = Spacing.PANEL_PADDING;
        return title == null ? y + p : y + p + font().lineHeight + Spacing.XS + 1 + Spacing.SM;
    }

    public int contentX() {
        return x + Spacing.PANEL_PADDING;
    }

    public int contentWidth() {
        return width - 2 * Spacing.PANEL_PADDING;
    }
}

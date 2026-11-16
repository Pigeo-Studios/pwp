package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Предпросмотр кита: рюкзак (слоты 9–35), хотбар (0–8) и экипировка (36–40) в стеклянной
 * панели, тултип предмета под курсором. Список короче 41 — недостающие слоты пустые.
 */
public class KitPreviewScreen extends Screen {
    private final Screen parent;
    private final List<ItemStack> items;
    private UIButton back;

    public KitPreviewScreen(Screen parent, String kitName, List<ItemStack> items) {
        super(Component.literal(DeployData.getDisplayName(kitName)));
        this.parent = parent;
        this.items = items == null ? List.of() : items;
    }

    @Override
    protected void init() {
        back = new UIButton(
                        Component.translatable("gui.pwpwarfare.kit_preview.back"), () -> minecraft.setScreen(parent))
                .icon(com.pwp.coreclient.ui.UIIcon.BACK);
    }

    private ItemStack item(int i) {
        return i >= 0 && i < items.size() && items.get(i) != null ? items.get(i) : ItemStack.EMPTY;
    }

    private Rect panel() {
        int s = WTheme.PREVIEW_SLOT;
        int w = 9 * s + 2 * Spacing.SM;
        int headerH = WDraw.headerHeight(height);
        int lh = font.lineHeight + Spacing.XS;
        int h = headerH
                + Spacing.SM
                + 3 * (lh)
                + 5 * s
                + 2 * Spacing.XS
                + Spacing.SM
                + Spacing.BUTTON_HEIGHT
                + Spacing.SM;
        return Rect.of(
                (width - w) / 2, Math.max(Spacing.XS, (height - h) / 2), w, Math.min(h, height - 2 * Spacing.XS));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        Rect p = panel();
        Rect c = WDraw.panel(g, font, p, WDraw.headerHeight(height), WarfareIcon.BACKPACK, title, null, 1F);
        int s = WTheme.PREVIEW_SLOT;
        int lh = font.lineHeight + Spacing.XS;
        int y = c.y();
        ItemStack hovered = ItemStack.EMPTY;
        WDraw.section(
                g,
                font,
                Rect.of(c.x(), y, c.w(), lh),
                Component.translatable("gui.pwpwarfare.kit_preview.inventory"),
                PWPTheme.Colors.ACCENT,
                1F);
        y += lh;
        for (int i = 0; i < 27; i++) hovered = slot(g, c.x() + i % 9 * s, y + i / 9 * s, item(i + 9), mx, my, hovered);
        y += 3 * s + Spacing.XS;
        WDraw.section(
                g,
                font,
                Rect.of(c.x(), y, c.w(), lh),
                Component.translatable("gui.pwpwarfare.kit_preview.hotbar"),
                PWPTheme.Colors.ACCENT,
                1F);
        y += lh;
        for (int i = 0; i < 9; i++) hovered = slot(g, c.x() + i * s, y, item(i), mx, my, hovered);
        y += s + Spacing.XS;
        WDraw.section(
                g,
                font,
                Rect.of(c.x(), y, c.w(), lh),
                Component.translatable("gui.pwpwarfare.kit_preview.equipment"),
                PWPTheme.Colors.ACCENT,
                1F);
        y += lh;
        for (int i = 0; i < 5; i++) hovered = slot(g, c.x() + i * s, y, item(i + 36), mx, my, hovered);
        int bw = Math.min(c.w(), back.preferredWidth() + Spacing.LG);
        back.setBounds(Rect.of(
                p.centerX() - bw / 2, p.bottom() - Spacing.SM - Spacing.BUTTON_HEIGHT, bw, Spacing.BUTTON_HEIGHT));
        back.render(g, mx, my, pt);
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mx, my);
    }

    private ItemStack slot(GuiGraphics g, int x, int y, ItemStack stack, int mx, int my, ItemStack hovered) {
        Rect r = Rect.of(x, y, WTheme.PREVIEW_SLOT - 1, WTheme.PREVIEW_SLOT - 1);
        boolean over = r.contains(mx, my);
        WDraw.well(g, r, 1F);
        if (over) UIDraw.rect(g, r.inset(1), WTheme.ROW_HOVER);
        g.renderFakeItem(stack, x + 1, y + 1);
        g.renderItemDecorations(font, stack, x + 1, y + 1);
        return over && !stack.isEmpty() ? stack : hovered;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (back != null && back.mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (back != null) back.mouseReleased(mx, my, btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

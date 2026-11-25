package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Легаси-панель (API прежний) в виде стеклянной панели UI-кита ({@link UIDraw#glass}).
 * ACCENT_BORDER — с акцентной полосой слева, SURFACE_DIM — прозрачнее, elevated — с тенью
 * (у стекла тень есть всегда). Заголовок — шапка с акцентной полосой высотой {@link #titleHeight()}.
 * Новые экраны — {@link com.pwp.coreclient.ui.UIPanel}.
 */
public class PWPPanel {

    public enum Variant {
        SURFACE,
        SURFACE_DIM,
        ACCENT_BORDER
    }

    private PWPPanel() {}

    public static void render(GuiGraphics gui, int x, int y, int w, int h) {
        render(gui, x, y, w, h, Variant.SURFACE, false);
    }

    public static void render(GuiGraphics gui, int x, int y, int w, int h, Variant variant) {
        render(gui, x, y, w, h, variant, false);
    }

    public static void render(GuiGraphics gui, int x, int y, int w, int h, Variant variant, boolean elevated) {
        Rect r = Rect.of(x, y, w, h);
        UIDraw.glass(gui, r, variant == Variant.SURFACE_DIM ? 0.7F : 1F, elevated);
        if (variant == Variant.ACCENT_BORDER) {
            UIDraw.accentBar(gui, x, y, h, Spacing.ACCENT_BAR, Styles.Control.ACCENT_BAR);
        }
    }

    public static void renderWithTitle(GuiGraphics gui, int x, int y, int w, int h, String title, int titleColor) {
        render(gui, x, y, w, h);
        if (title != null && !title.isEmpty()) {
            Rect header =
                    Rect.of(x, y, w, titleHeight()).inset(Spacing.HAIRLINE, Spacing.HAIRLINE, Spacing.HAIRLINE, 0);
            UIDraw.header(gui, PWPTheme.Fonts.display(), header, null, Component.literal(title), titleColor, 1F);
        }
    }

    public static int titleHeight() {
        return PWPTheme.Spacing.XS + PWPTheme.Fonts.SIZE_NORMAL + PWPTheme.Spacing.XS;
    }
}

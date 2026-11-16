package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Скелетон загрузки (API прежний) в стиле UI-кита: квадратная плашка с пульсирующей
 * прозрачностью, 1px обводкой и «строками»-заглушками.
 */
public class PWPSkeleton {

    private static final long PERIOD_MS = 1200;

    private PWPSkeleton() {}

    private static float pulse(long ageMs) {
        float t = (float) (ageMs % PERIOD_MS) / PERIOD_MS;
        return 0.55F + 0.45F * (float) Math.sin(t * Math.PI * 2);
    }

    public static void render(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        Rect r = Rect.of(x, y, w, h);
        UIDraw.rect(gui, r, UIDraw.fade(Styles.Control.BG, 0.6F + 0.4F * pulse(ageMs)));
        UIDraw.outline(gui, r, Styles.Control.BORDER);
    }

    public static void renderCard(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        render(gui, x, y, w, h, ageMs);
        int pad = 10;
        int lineH = 6;
        int lineGap = 4;
        int lineX = x + pad;
        int lineW = w - pad * 2;
        renderLine(gui, lineX, y + pad, lineW * 6 / 10, lineH, ageMs);
        renderLine(gui, lineX, y + pad + lineH + lineGap, lineW * 4 / 10, lineH, ageMs);
        renderLine(gui, lineX, y + pad + (lineH + lineGap) * 2, lineW, lineH, ageMs);
    }

    private static void renderLine(GuiGraphics gui, int x, int y, int w, int h, long ageMs) {
        UIDraw.rect(gui, Rect.of(x, y, w, h), UIDraw.fade(Styles.Control.BORDER_HOVER, 0.4F * pulse(ageMs)));
    }
}

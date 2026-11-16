package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Всплывающие уведомления (справа сверху, до {@link #MAX_TOASTS}): стеклянная плашка с цветной
 * акцентной полосой и иконкой по типу, текст с переносом (до 2 строк, лишнее «…»), въезд
 * справа и плавное угасание. API прежний: {@code show(String[, ToastType])}, {@code render}.
 */
public class PWPToastManager {

    private static final Deque<ToastEntry> toasts = new ArrayDeque<>();
    private static final int MAX_TOASTS = 3;
    private static final int TOAST_LIFE_MS = 3000;
    private static final int FADE_MS = 300;

    private PWPToastManager() {}

    public static void show(String message) {
        show(message, ToastType.INFO);
    }

    public static void show(String message, ToastType type) {
        toasts.addLast(new ToastEntry(message, type, System.currentTimeMillis()));
        if (toasts.size() > MAX_TOASTS) {
            toasts.pollFirst();
        }
    }

    public static void render(GuiGraphics gui) {
        if (toasts.isEmpty()) return;
        long now = System.currentTimeMillis();
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        var font = PWPTheme.Fonts.display();
        int margin = Spacing.SM;
        int maxW = Math.min(Spacing.TOAST_MAX_W, screenW - 2 * margin);
        int y = Spacing.TOAST_TOP;

        Iterator<ToastEntry> it = toasts.iterator();
        while (it.hasNext()) {
            ToastEntry t = it.next();
            long age = now - t.startTime;
            if (age > TOAST_LIFE_MS + FADE_MS) {
                it.remove();
                continue;
            }
            float in = UIMath.stagger(age, 0, 0, 0, FADE_MS);
            float out = age > TOAST_LIFE_MS ? 1F - (age - TOAST_LIFE_MS) / (float) FADE_MS : 1F;
            float alpha = Math.max(0F, Math.min(in, out));

            int iconBlock = Spacing.ICON_SIZE + Spacing.SM;
            int pad = Spacing.SM;
            int textMax = maxW - 2 * pad - iconBlock - Spacing.ACCENT_BAR;
            List<FormattedCharSequence> lines = UIDraw.wrap(font, Component.literal(t.message), textMax, 2);
            int textW = 0;
            for (FormattedCharSequence l : lines) textW = Math.max(textW, font.width(l));
            int w = Math.min(maxW, textW + 2 * pad + iconBlock + Spacing.ACCENT_BAR);
            int h = Math.max(
                    Spacing.ICON_SIZE + 2 * Spacing.XS, UIDraw.linesHeight(font, lines.size()) + 2 * Spacing.XS + 2);
            int slide = Math.round((1F - in) * Spacing.XL);
            Rect r = Rect.of(screenW - margin - w + slide, y, w, h);

            int accent = color(t.type);
            UIDraw.glass(gui, r, alpha, true);
            UIDraw.accentBar(gui, r.x(), r.y(), r.h(), Spacing.ACCENT_BAR_SELECTED, UIDraw.fade(accent, alpha));
            int ix = r.x() + Spacing.ACCENT_BAR_SELECTED + pad;
            UIDraw.icon(
                    gui,
                    icon(t.type),
                    ix,
                    r.y() + (r.h() - Spacing.ICON_SIZE) / 2,
                    Spacing.ICON_SIZE,
                    UIDraw.fade(accent, alpha));
            Rect text = Rect.of(
                    ix + iconBlock, r.y() + (r.h() - UIDraw.linesHeight(font, lines.size())) / 2 + 1, textMax, r.h());
            UIDraw.lines(gui, font, lines, text, false, UIDraw.fade(PWPTheme.Styles.Toast.TEXT, alpha), false);
            y += h + Spacing.XS;
        }
    }

    private static int color(ToastType type) {
        return switch (type) {
            case SUCCESS -> PWPTheme.Colors.SUCCESS_LIGHT;
            case ERROR -> PWPTheme.Text.DANGER;
            case XP -> PWPTheme.Colors.ACCENT;
            default -> PWPTheme.Styles.Role.SPECIALIST;
        };
    }

    private static UIIcon icon(ToastType type) {
        return switch (type) {
            case SUCCESS -> UIIcon.CHECK;
            case ERROR -> UIIcon.WARNING;
            case XP -> UIIcon.PLAY;
            default -> UIIcon.INFO;
        };
    }

    public static void clear() {
        toasts.clear();
    }

    public static class ToastEntry {
        final String message;
        final ToastType type;
        final long startTime;

        ToastEntry(String message, ToastType type, long startTime) {
            this.message = message;
            this.type = type;
            this.startTime = startTime;
        }
    }

    public enum ToastType {
        INFO,
        SUCCESS,
        ERROR,
        XP
    }
}

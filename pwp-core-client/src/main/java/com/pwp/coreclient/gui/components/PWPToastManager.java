package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

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
        var font = Minecraft.getInstance().font;

        Iterator<ToastEntry> it = toasts.iterator();
        int yOffset = 10;

        while (it.hasNext()) {
            ToastEntry t = it.next();
            long age = now - t.startTime;

            if (age > TOAST_LIFE_MS + FADE_MS) {
                it.remove();
                continue;
            }

            float alpha;
            if (age < FADE_MS) {
                alpha = (float) age / FADE_MS;
            } else if (age > TOAST_LIFE_MS) {
                alpha = 1.0F - (float) (age - TOAST_LIFE_MS) / FADE_MS;
            } else {
                alpha = 1.0F;
            }

            int textW = font.width(t.message);
            int toastW = textW + 20;
            int toastH = 20;
            int x = screenW - toastW - 10;
            int y = yOffset;

            int bgColor = switch (t.type) {
                case SUCCESS -> PWPTheme.Styles.Toast.BG_SUCCESS;
                case ERROR -> PWPTheme.Styles.Toast.BG_ERROR;
                case XP -> PWPTheme.Styles.Toast.BG_XP;
                default -> PWPTheme.Styles.Toast.BG_INFO;
            };

            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            int bg = (a << 24) | (bgColor & 0x00FFFFFF);
            int text = (a << 24) | (PWPTheme.Styles.Toast.TEXT & 0x00FFFFFF);

            gui.fill(x, y, x + toastW, y + toastH, bg);
            gui.drawString(font, Component.literal(t.message), x + 10, y + 6, text);

            yOffset += toastH + 6;
        }
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
        INFO, SUCCESS, ERROR, XP
    }
}

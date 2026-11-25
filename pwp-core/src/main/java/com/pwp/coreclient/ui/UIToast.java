package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Всплывающие уведомления в правом верхнем углу: въезжают справа, живут {@code durationMs},
 * уезжают. Одинаковый ключ продлевает существующий тост вместо дубля (как NotificationFeed).
 */
@OnlyIn(Dist.CLIENT)
public final class UIToast {
    public enum Kind {
        SUCCESS(Styles.Toast.BG_SUCCESS),
        ERROR(Styles.Toast.BG_ERROR),
        INFO(Styles.Toast.BG_INFO),
        XP(Styles.Toast.BG_XP);

        final int bg;

        Kind(int bg) {
            this.bg = bg;
        }
    }

    private static final int MAX_VISIBLE = 4;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private UIToast() {}

    private static final class Entry {
        final String key;
        Component text;
        Kind kind;
        long until;
        final UIAnim slide = new UIAnim(0F, Animations.DURATION_NORMAL, UIAnim.EASE_OUT_CUBIC);

        Entry(String key) {
            this.key = key;
        }
    }

    /** Показать (или продлить по ключу) тост. */
    public static void show(String key, Component text, Kind kind, long durationMs) {
        long now = UIAnim.REAL_TIME.getAsLong();
        for (Entry e : ENTRIES) {
            if (e.key.equals(key)) {
                e.text = text;
                e.kind = kind;
                e.until = now + durationMs;
                e.slide.to(1F);
                return;
            }
        }
        Entry e = new Entry(key);
        e.text = text;
        e.kind = kind;
        e.until = now + durationMs;
        e.slide.to(1F);
        ENTRIES.add(0, e);
        while (ENTRIES.size() > MAX_VISIBLE) ENTRIES.remove(ENTRIES.size() - 1);
    }

    /** Рисовать поверх экрана/HUD каждый кадр. */
    public static void render(GuiGraphics g, int screenW) {
        long now = UIAnim.REAL_TIME.getAsLong();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int y = Spacing.SM;
        int h = font.lineHeight + 2 * Spacing.SM;
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (now > e.until) e.slide.to(0F);
            float t = e.slide.value();
            if (now > e.until && e.slide.done()) {
                it.remove();
                continue;
            }
            int w = font.width(e.text) + 2 * Spacing.MD;
            int x = screenW - Spacing.SM - Math.round(w * t);
            UIDraw.box(g, x, y, w, h, Spacing.RADIUS_SMALL, e.kind.bg, Styles.Tooltip.BORDER);
            g.drawString(font, e.text, x + Spacing.MD, y + Spacing.SM, Styles.Toast.TEXT, false);
            y += Math.round((h + Spacing.XS) * t);
        }
    }
}

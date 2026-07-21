package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Random;

public class PWPTipsWidget {

    private static final List<String> TIPS = List.of(
        "Совет: используйте тактическое оборудование для победы",
        "Совет: связь с отрядом — ключ к успеху",
        "Совет: следите за уровнем брони и здоровья",
        "Совет: захватывайте точки чтобы получить преимущество",
        "Совет: техника уязвима с тыла и флангов",
        "Совет: используйте укрытия чтобы избежать огня",
        "Совет: аптечки восстанавливают здоровье",
        "Совет: боеприпасы можно пополнить на точке",
        "Совет: внимательно слушайте шаги противника",
        "Совет: не забывайте перезаряжаться перед боем",
        "Совет: используйте дымовые гранаты для прикрытия",
        "Совет: маркеры на карте помогут координации",
        "Совет: экипировка влияет на скорость передвижения",
        "Совет: ночное время снижает видимость — используйте ПНВ",
        "Совет: поддерживайте союзников огнём"
    );

    private static final long SHOW_MS = 4000;
    private static final long FADE_MS = 300;

    private final Random random = new Random();
    private final int maxTextWidth;

    private int currentIndex;
    private long stateStartTime;
    private boolean fading;

    public PWPTipsWidget(int maxTextWidth) {
        this.maxTextWidth = maxTextWidth;
        this.currentIndex = random.nextInt(TIPS.size());
        this.stateStartTime = System.currentTimeMillis();
        this.fading = false;
    }

    public void tick(long now) {
        long elapsed = now - stateStartTime;

        if (!fading && elapsed > SHOW_MS) {
            fading = true;
            stateStartTime = now;
        } else if (fading && elapsed > FADE_MS) {
            fading = false;
            int next;
            do {
                next = random.nextInt(TIPS.size());
            } while (next == currentIndex);
            currentIndex = next;
            stateStartTime = now;
        }
    }

    public void render(GuiGraphics gui, int cx, int y) {
        long now = System.currentTimeMillis();
        tick(now);

        float alpha;
        if (fading) {
            float fadeProgress = Math.min((now - stateStartTime) / (float) FADE_MS, 1);
            alpha = 1.0f - Easing.easeOutCubic(fadeProgress);
        } else {
            alpha = 1.0f;
        }

        var font = Minecraft.getInstance().font;
        int color = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, (int) (alpha * 180));
        String text = TIPS.get(currentIndex);

        if (font.width(text) > maxTextWidth) {
            var lines = font.split(Component.literal(text), maxTextWidth);
            int lineCount = Math.min(lines.size(), 2);
            for (int i = 0; i < lineCount; i++) {
                gui.drawString(font, lines.get(i), cx - maxTextWidth / 2, y + i * 10, color);
            }
        } else {
            gui.drawCenteredString(font, Component.literal(text), cx, y, color);
        }
    }
}

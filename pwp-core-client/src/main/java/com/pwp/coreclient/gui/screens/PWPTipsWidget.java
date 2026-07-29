package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Random;

public class PWPTipsWidget {

    public static final List<String> TIPS = List.of(
        "Используйте тактическое оборудование для победы",
        "Связь с отрядом — ключ к успеху",
        "Следите за уровнем брони и здоровья",
        "Захватывайте точки, чтобы получить преимущество",
        "Техника уязвима с тыла и флангов",
        "Совет: используйте укрытия чтобы избежать огня",
        "Аптечки восстанавливают здоровье",
        "Боеприпасы можно пополнить на точке",
        "Совет: внимательно слушайте шаги противника",
        "Не забывайте перезаряжаться перед боем",
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
    private int nextIndex;
    private long stateStartTime;
    private int fadePhase; // 0=show, 1=fade-out, 2=fade-in
    private int prevBoxWidth;
    private int nextBoxWidth;
    private int animatedBoxWidth;
    private long fadeStartTime;

    public PWPTipsWidget(int maxTextWidth) {
        this.maxTextWidth = maxTextWidth;
        this.currentIndex = random.nextInt(TIPS.size());
        this.stateStartTime = System.currentTimeMillis();
        this.fadePhase = 0;
    }

    private void setNextIndex() {
        int next;
        do {
            next = random.nextInt(TIPS.size());
        } while (next == currentIndex);
        nextIndex = next;
    }

    private void tick(long now, int currentTextWidth, int nextTextWidth) {
        long elapsed = now - stateStartTime;

        switch (fadePhase) {
            case 0: // showing
                if (elapsed > SHOW_MS) {
                    fadePhase = 1; // start fade-out
                    fadeStartTime = now;
                    stateStartTime = now;
                    prevBoxWidth = currentTextWidth;
                    setNextIndex();
                    nextBoxWidth = nextTextWidth;
                }
                break;
            case 1: // fading out old text
                if (elapsed > FADE_MS) {
                    currentIndex = nextIndex;
                    fadePhase = 2; // start fade-in
                    prevBoxWidth = animatedBoxWidth;
                    stateStartTime = now;
                }
                break;
            case 2: // fading in new text
                if (elapsed > FADE_MS) {
                    fadePhase = 0; // showing
                    stateStartTime = now;
                }
                break;
        }
    }

    public void render(GuiGraphics gui, int cx, int y) {
        long now = System.currentTimeMillis();
        var font = PWPTheme.Fonts.display();

        String currentText = TIPS.get(currentIndex);
        int currentTextW = Math.min(font.width(currentText), maxTextWidth);

        String nextText = (nextIndex >= 0 && nextIndex < TIPS.size()) ? TIPS.get(nextIndex) : currentText;
        int nextTextW = Math.min(font.width(nextText), maxTextWidth);

        tick(now, currentTextW, nextTextW);

        float alpha;
        String displayText;
        int targetWidth;

        switch (fadePhase) {
            case 0: // showing
                alpha = 1.0f;
                animatedBoxWidth = currentTextW;
                displayText = currentText;
                targetWidth = currentTextW;
                break;
            case 1: // fading out — old text shrinks to 0
                {
                    float progress = Math.min((now - stateStartTime) / (float) FADE_MS, 1);
                    alpha = 1.0f - Easing.easeOutCubic(progress);
                    animatedBoxWidth = (int) (prevBoxWidth * (1.0f - Easing.easeOutCubic(progress)));
                    displayText = currentText;
                    targetWidth = animatedBoxWidth;
                }
                break;
            case 2: // fading in — new text grows from 0
                {
                    float progress = Math.min((now - stateStartTime) / (float) FADE_MS, 1);
                    alpha = Easing.easeOutCubic(progress);
                    animatedBoxWidth = (int) (nextBoxWidth * Easing.easeOutCubic(progress));
                    displayText = currentText;
                    targetWidth = animatedBoxWidth;
                }
                break;
            default:
                alpha = 1.0f;
                displayText = currentText;
                targetWidth = currentTextW;
        }

        // Draw box background
        int boxW = Math.max(targetWidth + 16, 20);
        int boxH = font.lineHeight + 8;
        int boxX = cx - boxW / 2;
        int boxY = y - 4;

        int bgAlpha = (int) (alpha * 160);
        int bgColor = (bgAlpha << 24);
        com.pwp.coreclient.gui.components.RoundedRect.fill(gui, boxX, boxY, boxW, boxH, PWPTheme.Spacing.RADIUS_SMALL, bgColor);
        if (alpha > 0.05f) {
            int borderAlpha = (int) (alpha * 40);
            int borderColor = (borderAlpha << 24) | 0xFFFFFF;
            com.pwp.coreclient.gui.components.RoundedRect.border(gui, boxX, boxY, boxW, boxH, PWPTheme.Spacing.RADIUS_SMALL, 1, borderColor);
        }

        // Draw text
        int textAlpha = (int) (alpha * 200);
        int textColor = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, textAlpha);

        if (font.width(displayText) > maxTextWidth) {
            var lines = font.split(Component.literal(displayText), maxTextWidth);
            int lineCount = Math.min(lines.size(), 2);
            for (int i = 0; i < lineCount; i++) {
                gui.drawString(font, lines.get(i), cx - maxTextWidth / 2, y + i * 10, textColor, false);
            }
        } else {
            gui.drawString(font, Component.literal(displayText), cx - font.width(displayText) / 2, y + 2, textColor, false);
        }
    }
}

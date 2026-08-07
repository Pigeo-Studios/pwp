package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
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
    private static final long FADE_MS = 350;
    private static final int SLIDE_PX = 4;

    private record TipLayout(List<FormattedCharSequence> lines, int width) {
        int lineCount() {
            return lines.size();
        }
    }

    private final Random random = new Random();
    private final List<TipLayout> layouts = new ArrayList<>();

    private int currentIndex;
    private int nextIndex;
    private long stateStartTime;
    private boolean transitioning;
    private long transitionStart;

    public PWPTipsWidget(int maxTextWidth) {
        var font = PWPTheme.Fonts.display();
        for (String tip : TIPS) {
            List<FormattedCharSequence> lines = font.split(Component.literal(tip), maxTextWidth);
            int width = 0;
            for (FormattedCharSequence line : lines) {
                width = Math.max(width, font.width(line));
            }
            layouts.add(new TipLayout(lines, width));
        }
        this.currentIndex = random.nextInt(TIPS.size());
        this.stateStartTime = System.currentTimeMillis();
    }

    private void pickNext() {
        int next;
        do {
            next = random.nextInt(TIPS.size());
        } while (next == currentIndex);
        nextIndex = next;
    }

    public void render(GuiGraphics gui, int cx, int y) {
        long now = System.currentTimeMillis();
        var font = PWPTheme.Fonts.display();

        if (!transitioning && now - stateStartTime > SHOW_MS) {
            pickNext();
            transitioning = true;
            transitionStart = now;
        }

        float progress = transitioning ? Math.min((now - transitionStart) / (float) FADE_MS, 1f) : 1f;
        float eased = Easing.easeInOutCubic(progress);

        if (transitioning && progress >= 1f) {
            currentIndex = nextIndex;
            transitioning = false;
            stateStartTime = now;
        }

        TipLayout current = layouts.get(currentIndex);
        TipLayout next = transitioning ? layouts.get(nextIndex) : current;

        // Ширина плавно перестраивается под новый текст, высота — под максимум строк
        int boxW = current.width + 16 + (int) ((next.width - current.width) * eased);
        int lineCount = Math.max(current.lineCount(), next.lineCount());
        int boxH = lineCount * font.lineHeight + 8;
        int boxX = cx - boxW / 2;
        int boxY = y - 4;

        RoundedRect.fill(gui, boxX, boxY, boxW, boxH, PWPTheme.Spacing.RADIUS_SMALL, 0xA0000000);
        RoundedRect.border(gui, boxX, boxY, boxW, boxH, PWPTheme.Spacing.RADIUS_SMALL, 1, 0x28FFFFFF);

        // Последовательный фейд: первая половина перехода — старый текст гаснет
        // и уезжает вверх, вторая — новый текст проявляется снизу. Тексты никогда
        // не рисуются одновременно (раньше кроссфейд наслаивал оба почти в одной
        // позиции — «белый» мерцающий сдвоенный текст между сменами).
        float outT = Math.min(progress * 2f, 1f);
        float inT = Math.max(progress * 2f - 1f, 0f);
        if (outT < 1f) {
            drawTip(gui, font, current, boxX, boxY, boxH, cx, (int) (200f * (1f - outT)), -outT * SLIDE_PX);
        }
        if (inT > 0f) {
            drawTip(gui, font, next, boxX, boxY, boxH, cx, (int) (200f * inT), (1f - inT) * SLIDE_PX);
        }
    }

    private void drawTip(GuiGraphics gui, Font font, TipLayout layout, int boxX, int boxY, int boxH, int cx, int alpha, float slideY) {
        if (alpha <= 0) return;
        int textColor = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, alpha);
        int textBlockH = layout.lineCount() * font.lineHeight;
        int startY = boxY + (boxH - textBlockH) / 2 + (int) slideY;
        for (int i = 0; i < layout.lineCount(); i++) {
            FormattedCharSequence line = layout.lines().get(i);
            gui.drawString(font, line, cx - font.width(line) / 2, startY + i * font.lineHeight, textColor, false);
        }
    }
}

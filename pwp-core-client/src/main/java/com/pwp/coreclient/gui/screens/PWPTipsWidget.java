package com.pwp.coreclient.gui.screens;

import java.util.Random;
import net.minecraft.network.chat.Component;

/**
 * Ротация советов экрана загрузки: ключи {@code pwp_core.tip.N} (en/ru/uk), смена раз в
 * {@link #SHOW_MS}. Переход последовательный: старый совет гаснет, затем проявляется новый —
 * два текста никогда не рисуются одновременно (раньше кроссфейд давал «сдвоенный» текст).
 * Рисует {@link PWPLoadingView}: тут только какой совет и с какой альфой.
 */
public class PWPTipsWidget {

    /** Число ключей pwp_core.tip.N в lang-файлах. */
    public static final int TIP_COUNT = 15;

    private static final long SHOW_MS = 5000;
    private static final long FADE_MS = 400;

    private final Random random = new Random();
    private int currentIndex;
    private int nextIndex;
    private long stateStartTime;
    private boolean transitioning;
    private long transitionStart;
    private float alpha = 1F;
    private float slide;

    public PWPTipsWidget() {
        this.currentIndex = random.nextInt(TIP_COUNT);
        this.stateStartTime = System.currentTimeMillis();
    }

    /** Обновить состояние на кадр; вызвать перед {@link #text()}/{@link #alpha()}. */
    public void tick() {
        long now = System.currentTimeMillis();
        if (!transitioning && now - stateStartTime > SHOW_MS) {
            int next;
            do {
                next = random.nextInt(TIP_COUNT);
            } while (next == currentIndex);
            nextIndex = next;
            transitioning = true;
            transitionStart = now;
        }
        if (!transitioning) {
            alpha = 1F;
            slide = 0F;
            return;
        }
        float p = Math.min((now - transitionStart) / (float) FADE_MS, 1F);
        if (p < 0.5F) {
            alpha = 1F - p * 2F;
            slide = -(1F - alpha);
        } else {
            currentIndex = nextIndex;
            alpha = p * 2F - 1F;
            slide = 1F - alpha;
        }
        if (p >= 1F) {
            transitioning = false;
            stateStartTime = now;
            alpha = 1F;
            slide = 0F;
        }
    }

    public Component text() {
        return Component.translatable("pwp_core.tip." + currentIndex);
    }

    /** 0..1 — прозрачность текущего совета. */
    public float alpha() {
        return alpha;
    }

    /** -1..1 — сдвиг текста при смене (вверх при уходе, снизу при появлении). */
    public float slide() {
        return slide;
    }
}

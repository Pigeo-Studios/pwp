package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.ui.UIAnim;
import com.pwp.coreclient.ui.UIMath;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Состояние прокрутки области: цель (целые px) + плавное смещение для рисования. Границы
 * пересчитываются каждый кадр по высоте контента ({@link #clamp}).
 */
@OnlyIn(Dist.CLIENT)
public final class Scroll {
    private double target;
    private final UIAnim anim = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);

    /** Ограничить цель диапазоном [0, content - view]. */
    public void clamp(double content, double view) {
        double c = UIMath.clampScroll(target, content, view);
        if (c != target) {
            target = c;
            anim.to((float) c);
        }
    }

    /** Прокрутить колесом (delta > 0 — вверх). */
    public void wheel(double delta) {
        target = Math.max(0, target - delta * WTheme.SCROLL_STEP);
        anim.to((float) target);
    }

    public void reset() {
        target = 0;
        anim.snap(0F);
    }

    /** Смещение для рисования (плавное), целые px. */
    public int offset() {
        return Math.round(anim.value());
    }

    /** Смещение для hit-test — то же, что рисуется. */
    public double value() {
        return offset();
    }
}

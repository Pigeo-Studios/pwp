package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.ui.UIAnim;
import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Плавные состояния (hover/selected/expand) для элементов списков, которые пересобираются
 * каждый кадр: анимация хранится по ключу элемента (имя кита, id спавна), реальное время.
 */
@OnlyIn(Dist.CLIENT)
public final class AnimMap {
    private final Map<Object, UIAnim> anims = new HashMap<>();
    private final long durationMs;

    public AnimMap() {
        this(Animations.DURATION_FAST);
    }

    public AnimMap(long durationMs) {
        this.durationMs = durationMs;
    }

    /** Значение анимации ключа, ведомой к 1 (on) или 0. */
    public float get(Object key, boolean on) {
        UIAnim a = anims.computeIfAbsent(key, k -> new UIAnim(0F, durationMs, UIAnim.EASE_OUT_CUBIC));
        return a.to(on ? 1F : 0F).value();
    }

    public void clear() {
        anims.clear();
    }
}

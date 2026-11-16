package com.pwp.coreclient.ui.layout;

import java.util.ArrayList;
import java.util.List;

/** Повтор тайла вдоль оси: целые тайлы + обрезанный последний (без растяжения). */
public final class Tiles {
    private Tiles() {}

    /** Список {offset, length}: покрывает [0, length) кусками не длиннее {@code tile}. */
    public static List<int[]> spans(int length, int tile) {
        List<int[]> out = new ArrayList<>();
        if (length <= 0 || tile <= 0) return out;
        for (int off = 0; off < length; off += tile) out.add(new int[] {off, Math.min(tile, length - off)});
        return out;
    }

    /**
     * Сдвиг UV, чтобы тайл был «прибит» к экрану, а не к панели: шум/скан-лайны соседних
     * панелей стыкуются, а при движении панели узор не «плывёт».
     */
    public static int anchoredOffset(int screenPos, int tile) {
        if (tile <= 0) return 0;
        return Math.floorMod(screenPos, tile);
    }
}

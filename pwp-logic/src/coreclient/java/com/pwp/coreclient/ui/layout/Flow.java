package com.pwp.coreclient.ui.layout;

/**
 * Авто-лейаут «строка/столбец» (аналог flex): раскладывает элементы вдоль оси с зазором,
 * выравниванием и гибкими элементами. Размер элемента: {@code >= 0} — фиксированный,
 * {@code < 0} — гибкий с весом {@code -size} (-1 = вес 1), делит остаток.
 *
 * <p>Переполнение: если фиксированные элементы с зазорами не влезают, они ПРОПОРЦИОНАЛЬНО
 * сжимаются (гибкие получают 0) — элементы не вылезают за область и не накладываются.
 * Поперечная ось — вся ширина/высота области (выравнивание внутри — {@link Rect#align}).
 */
public final class Flow {
    private Flow() {}

    public enum Justify {
        START,
        CENTER,
        END,
        /** Первый у начала, последний у конца, зазоры растут (минимум — {@code gap}). */
        SPACE_BETWEEN
    }

    /** Гибкий размер с весом {@code weight} (для массивов размеров). */
    public static int flex(int weight) {
        return -Math.max(1, weight);
    }

    /** Строка: элементы слева направо, высота каждого = высота области. */
    public static Rect[] row(Rect area, int gap, Justify justify, int... widths) {
        int[][] spans = spans(area.w(), gap, justify, widths);
        Rect[] out = new Rect[widths.length];
        for (int i = 0; i < out.length; i++) out[i] = new Rect(area.x() + spans[i][0], area.y(), spans[i][1], area.h());
        return out;
    }

    /** Столбец: элементы сверху вниз, ширина каждого = ширина области. */
    public static Rect[] column(Rect area, int gap, Justify justify, int... heights) {
        int[][] spans = spans(area.h(), gap, justify, heights);
        Rect[] out = new Rect[heights.length];
        for (int i = 0; i < out.length; i++) out[i] = new Rect(area.x(), area.y() + spans[i][0], area.w(), spans[i][1]);
        return out;
    }

    /** Сумма фиксированных размеров с зазорами — «естественная» длина контента. */
    public static int naturalLength(int gap, int... sizes) {
        int sum = 0;
        for (int s : sizes) sum += Math.max(0, s);
        return sum + Math.max(0, gap) * Math.max(0, sizes.length - 1);
    }

    /**
     * Одномерная раскладка: для каждого элемента {offset, length} от начала оси.
     * Чистая функция — основа row/column.
     */
    public static int[][] spans(int space, int gap, Justify justify, int... sizes) {
        int n = sizes.length;
        int[][] out = new int[n][2];
        if (n == 0) return out;
        space = Math.max(0, space);
        gap = Math.max(0, gap);
        int gaps = gap * (n - 1);
        if (gaps > space) gaps = 0; // зазоры не влезают — убираем, чтобы не вылезти за ось
        int effGap = n > 1 ? gaps / (n - 1) : 0;

        int fixed = 0;
        int weights = 0;
        for (int s : sizes) {
            if (s >= 0) fixed += s;
            else weights += -s;
        }
        int avail = space - gaps;
        int[] len = new int[n];
        if (fixed > avail) {
            // сжать фиксированные пропорционально, остаток пикселей — первым
            int used = 0;
            for (int i = 0; i < n; i++) {
                len[i] = sizes[i] >= 0 ? (int) ((long) sizes[i] * avail / Math.max(1, fixed)) : 0;
                used += len[i];
            }
            distributeRemainder(len, sizes, avail - used, true);
        } else {
            int rest = avail - fixed;
            int used = 0;
            for (int i = 0; i < n; i++) {
                if (sizes[i] >= 0) len[i] = sizes[i];
                else {
                    len[i] = weights > 0 ? (int) ((long) rest * -sizes[i] / weights) : 0;
                    used += len[i];
                }
            }
            if (weights > 0) distributeRemainder(len, sizes, rest - used, false);
        }

        int total = effGap * (n - 1);
        for (int l : len) total += l;
        int free = Math.max(0, space - total);
        int start;
        int extraGap = 0;
        int extraRem = 0;
        switch (justify) {
            case CENTER -> start = free / 2;
            case END -> start = free;
            case SPACE_BETWEEN -> {
                start = 0;
                if (n > 1) {
                    extraGap = free / (n - 1);
                    extraRem = free % (n - 1);
                }
            }
            default -> start = 0;
        }
        int pos = start;
        for (int i = 0; i < n; i++) {
            out[i][0] = pos;
            out[i][1] = len[i];
            pos += len[i] + effGap + extraGap + (i < extraRem ? 1 : 0);
        }
        return out;
    }

    private static void distributeRemainder(int[] len, int[] sizes, int remainder, boolean fixedOnes) {
        for (int i = 0; i < len.length && remainder > 0; i++) {
            boolean match = fixedOnes ? sizes[i] > 0 : sizes[i] < 0;
            if (match) {
                len[i]++;
                remainder--;
            }
        }
    }
}

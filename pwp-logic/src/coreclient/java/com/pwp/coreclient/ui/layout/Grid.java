package com.pwp.coreclient.ui.layout;

/**
 * Сетка карточек: число колонок из минимальной ширины ячейки, ячейки делят ширину области
 * поровну (остаток пикселей — первым колонкам), строки фиксированной высоты.
 */
public final class Grid {
    private Grid() {}

    /** Сколько колонок шириной не меньше {@code minCellW} влезает (1..maxCols). */
    public static int columnsFor(int availW, int minCellW, int gap, int maxCols) {
        int cols = (Math.max(0, availW) + Math.max(0, gap)) / Math.max(1, minCellW + Math.max(0, gap));
        return Math.max(1, Math.min(Math.max(1, maxCols), cols));
    }

    /** Ячейки {@code count} элементов в {@code cols} колонок; строки высотой {@code cellH}. */
    public static Rect[] cells(Rect area, int count, int cols, int cellH, int gap) {
        cols = Math.max(1, cols);
        Rect[] out = new Rect[Math.max(0, count)];
        int[] widths = new int[cols];
        java.util.Arrays.fill(widths, -1);
        int[][] col = Flow.spans(area.w(), gap, Flow.Justify.START, widths);
        for (int i = 0; i < out.length; i++) {
            int c = i % cols;
            int r = i / cols;
            out[i] = new Rect(area.x() + col[c][0], area.y() + r * (cellH + gap), col[c][1], cellH);
        }
        return out;
    }

    /** Полная высота сетки из {@code count} элементов. */
    public static int height(int count, int cols, int cellH, int gap) {
        if (count <= 0) return 0;
        int rows = (count + Math.max(1, cols) - 1) / Math.max(1, cols);
        return rows * cellH + (rows - 1) * Math.max(0, gap);
    }
}

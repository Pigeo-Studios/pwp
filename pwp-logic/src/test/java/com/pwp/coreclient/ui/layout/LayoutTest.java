package com.pwp.coreclient.ui.layout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class LayoutTest {
    /** Моноширинный «шрифт»: 6 px на символ (как ванильный в среднем), «…» = 6 px. */
    private static final ToIntFunction<String> MONO = s -> s.codePointCount(0, s.length()) * 6;

    // ---------------- Rect ----------------

    @Test
    void rectInsetNeverNegative() {
        Rect r = Rect.of(10, 10, 20, 8).inset(12);
        assertEquals(0, r.w());
        assertEquals(0, r.h());
        assertTrue(r.x() <= 30 && r.y() <= 18, "не уезжает за правый/нижний край");
    }

    @Test
    void rectSlicing() {
        Rect r = Rect.of(0, 0, 100, 50);
        assertEquals(Rect.of(0, 0, 100, 10), r.top(10));
        assertEquals(Rect.of(0, 14, 100, 36), r.below(10, 4));
        assertEquals(Rect.of(0, 40, 100, 10), r.bottomStrip(10));
        assertEquals(Rect.of(0, 0, 100, 36), r.above(10, 4));
        assertEquals(Rect.of(30, 0, 70, 50), r.rightOf(26, 4));
        assertEquals(0, r.below(80, 0).h(), "срез больше области — пустой остаток");
    }

    @Test
    void rectAlignClampsToArea() {
        Rect r = Rect.of(10, 20, 100, 40);
        assertEquals(Rect.of(35, 30, 50, 20), r.centered(50, 20));
        assertEquals(Rect.of(10, 20, 100, 40), r.centered(500, 500), "больше области — обрезается до области");
        assertEquals(Rect.of(60, 20, 50, 40), r.align(50, 10, Align.END, Align.STRETCH));
    }

    // ---------------- Flow ----------------

    @Test
    void flowFixedWithGapStart() {
        int[][] s = Flow.spans(100, 4, Flow.Justify.START, 20, 30);
        assertArrayEquals(new int[] {0, 20}, s[0]);
        assertArrayEquals(new int[] {24, 30}, s[1]);
    }

    @Test
    void flowCenterAndEnd() {
        int[][] c = Flow.spans(100, 0, Flow.Justify.CENTER, 20, 20);
        assertEquals(30, c[0][0]);
        assertEquals(50, c[1][0]);
        int[][] e = Flow.spans(100, 10, Flow.Justify.END, 20);
        assertEquals(80, e[0][0]);
    }

    @Test
    void flowFlexSharesRemainderExactly() {
        int[][] s = Flow.spans(101, 3, Flow.Justify.START, 20, Flow.flex(1), Flow.flex(2));
        // 101 - 20 - 6 = 75 -> 25 / 50
        assertEquals(20, s[0][1]);
        assertEquals(25, s[1][1]);
        assertEquals(50, s[2][1]);
        assertEquals(101, s[2][0] + s[2][1], "последний гибкий упирается ровно в конец");
    }

    @Test
    void flowFlexRemainderPixelsNotLost() {
        int[][] s = Flow.spans(10, 0, Flow.Justify.START, -1, -1, -1);
        assertEquals(10, s[0][1] + s[1][1] + s[2][1]);
        assertEquals(s[0][1] + s[1][1], s[2][0], "нет щелей между гибкими");
    }

    @Test
    void flowOverflowShrinksProportionallyAndStaysInside() {
        int[][] s = Flow.spans(50, 10, Flow.Justify.START, 40, 40);
        int end = s[1][0] + s[1][1];
        assertTrue(end <= 50, "не вылезает за ось: " + end);
        assertTrue(s[0][0] + s[0][1] <= s[1][0], "не накладываются");
        assertEquals(s[0][1], s[1][1]);
    }

    @Test
    void flowGapsDroppedWhenTheyDoNotFit() {
        int[][] s = Flow.spans(5, 10, Flow.Justify.START, 2, 2);
        assertTrue(s[1][0] + s[1][1] <= 5);
    }

    @Test
    void flowSpaceBetween() {
        int[][] s = Flow.spans(100, 2, Flow.Justify.SPACE_BETWEEN, 10, 10, 10);
        assertEquals(0, s[0][0]);
        assertEquals(100, s[2][0] + s[2][1]);
    }

    @Test
    void rowAndColumnUseArea() {
        Rect[] r = Flow.row(Rect.of(5, 7, 100, 20), 4, Flow.Justify.START, 30, Flow.flex(1));
        assertEquals(Rect.of(5, 7, 30, 20), r[0]);
        assertEquals(Rect.of(39, 7, 66, 20), r[1]);
        Rect[] c = Flow.column(Rect.of(0, 0, 50, 60), 5, Flow.Justify.CENTER, 10, 10);
        assertEquals(Rect.of(0, 17, 50, 10), c[0]);
        assertEquals(Rect.of(0, 32, 50, 10), c[1]);
        assertEquals(25, Flow.naturalLength(5, 10, 10));
    }

    @Test
    void emptyFlow() {
        assertEquals(0, Flow.spans(100, 4, Flow.Justify.START).length);
    }

    // ---------------- Grid ----------------

    @Test
    void gridColumnsAndCellsFillWidth() {
        assertEquals(3, Grid.columnsFor(320, 100, 8, 6));
        assertEquals(1, Grid.columnsFor(40, 100, 8, 6), "минимум одна колонка");
        assertEquals(2, Grid.columnsFor(10_000, 100, 8, 2), "не больше maxCols");
        Rect[] cells = Grid.cells(Rect.of(0, 0, 101, 500), 5, 2, 20, 1);
        assertEquals(5, cells.length);
        assertEquals(101, cells[1].right(), "правая колонка до края");
        assertEquals(21, cells[2].y());
        assertEquals(20 * 3 + 2, Grid.height(5, 2, 20, 1));
        assertEquals(0, Grid.height(0, 2, 20, 1));
    }

    // ---------------- TextFit ----------------

    @Test
    void ellipsizeKeepsFittingText() {
        assertEquals("ИГРАТЬ", TextFit.ellipsize("ИГРАТЬ", 36, MONO));
    }

    @Test
    void ellipsizeCutsToWidth() {
        String out = TextFit.ellipsize("ОДИНОЧНАЯ ИГРА", 40, MONO);
        assertTrue(out.endsWith(TextFit.ELLIPSIS));
        assertTrue(MONO.applyAsInt(out) <= 40, out);
        assertEquals("ОДИНО…", out);
    }

    @Test
    void ellipsizeStripsTrailingSpaceBeforeDots() {
        assertEquals("AB…", TextFit.ellipsize("AB CDEF", 24, MONO));
    }

    @Test
    void ellipsizeTooNarrowGivesEmpty() {
        assertEquals("", TextFit.ellipsize("Hello", 3, MONO));
        assertEquals("…", TextFit.ellipsize("Hello", 6, MONO));
    }

    @Test
    void ellipsizeDoesNotSplitSurrogates() {
        String s = "a😀😀😀";
        String out = TextFit.ellipsize(s, 18, MONO);
        assertTrue(out.endsWith(TextFit.ELLIPSIS));
        for (int i = 0; i < out.length(); i++) {
            if (Character.isHighSurrogate(out.charAt(i))) assertTrue(Character.isLowSurrogate(out.charAt(i + 1)));
        }
    }

    @Test
    void wrapByWords() {
        List<String> lines = TextFit.wrap("связь с отрядом ключ к успеху", 60, 0, MONO);
        for (String l : lines) assertTrue(MONO.applyAsInt(l) <= 60, l);
        assertEquals("связь с отрядом ключ к успеху", String.join(" ", lines));
    }

    @Test
    void wrapBreaksLongWord() {
        List<String> lines = TextFit.wrap("Превышеновремяожидания", 30, 0, MONO);
        assertTrue(lines.size() > 1);
        for (String l : lines) assertTrue(MONO.applyAsInt(l) <= 30, l);
        assertEquals("Превышеновремяожидания", String.join("", lines));
    }

    @Test
    void wrapRespectsNewlinesAndMaxLines() {
        List<String> lines = TextFit.wrap("один\nдва три четыре пять шесть", 30, 2, MONO);
        assertEquals(2, lines.size());
        assertEquals("один", lines.get(0));
        assertTrue(lines.get(1).endsWith(TextFit.ELLIPSIS));
        assertTrue(MONO.applyAsInt(lines.get(1)) <= 30);
    }

    @Test
    void wrapEmpty() {
        assertTrue(TextFit.wrap("", 30, 2, MONO).isEmpty());
    }

    // ---------------- NineSlice / Tiles ----------------

    @Test
    void tilesCoverWithoutStretch() {
        List<int[]> s = Tiles.spans(50, 16);
        assertEquals(4, s.size());
        assertArrayEquals(new int[] {48, 2}, s.get(3));
        assertTrue(Tiles.spans(0, 16).isEmpty());
        assertEquals(3, Tiles.anchoredOffset(-61, 64));
    }

    @Test
    void nineSliceTileCoversExactlyWithoutGapsOrOverlap() {
        Rect dst = Rect.of(3, 5, 77, 41);
        List<NineSlice.Piece> pieces = NineSlice.pieces(dst, 64, 64, 8, NineSlice.Mode.TILE, true);
        Set<Long> covered = new HashSet<>();
        for (NineSlice.Piece p : pieces) {
            assertTrue(p.oneToOne(), "TILE: только 1:1 куски: " + p);
            assertTrue(p.u() >= 0 && p.v() >= 0 && p.u() + p.uw() <= 64 && p.v() + p.vh() <= 64, "UV в текстуре");
            for (int x = p.x(); x < p.x() + p.w(); x++)
                for (int y = p.y(); y < p.y() + p.h(); y++)
                    assertTrue(covered.add(((long) x << 32) | (y & 0xffffffffL)), "наложение в " + x + "," + y);
        }
        assertEquals((long) dst.w() * dst.h(), covered.size(), "щелей нет");
    }

    @Test
    void nineSliceCornersUseCornerUv() {
        List<NineSlice.Piece> p = NineSlice.pieces(Rect.of(0, 0, 40, 40), 32, 32, 6, NineSlice.Mode.TILE, false);
        NineSlice.Piece br =
                p.stream().filter(x -> x.x() == 34 && x.y() == 34).findFirst().orElseThrow();
        assertEquals(26, br.u());
        assertEquals(26, br.v());
        assertTrue(p.stream().noneMatch(x -> x.x() >= 6 && x.x() < 34 && x.y() >= 6 && x.y() < 34), "без центра");
    }

    @Test
    void nineSliceShrinksCornersOnTinyArea() {
        List<NineSlice.Piece> p = NineSlice.pieces(Rect.of(0, 0, 5, 30), 16, 16, 4, NineSlice.Mode.TILE, true);
        int maxRight = p.stream().mapToInt(x -> x.x() + x.w()).max().orElse(0);
        assertEquals(5, maxRight);
        NineSlice.Piece right =
                p.stream().filter(x -> x.x() == 2 && x.y() == 0).findFirst().orElseThrow();
        assertEquals(13, right.u(), "правый угол берёт внешнюю часть текстуры");
    }

    @Test
    void nineSliceStretchIsNinePieces() {
        List<NineSlice.Piece> p =
                NineSlice.pieces(Rect.of(0, 0, 300, 200), 64, 64, 8, NineSlice.Mode.STRETCH_EDGES, true);
        assertEquals(9, p.size());
    }

    // ---------------- Fit ----------------

    @Test
    void coverCropsSidesForWideSource() {
        int[] r = Fit.cover(1672, 941, 320, 240); // 4:3 экран, 16:9 картинка
        assertEquals(941, r[3]);
        assertTrue(r[2] < 1672);
        assertEquals((1672 - r[2]) / 2, r[0]);
        double ratio = r[2] / (double) r[3];
        assertEquals(320 / 240.0, ratio, 0.01);
    }

    @Test
    void coverCropsTopBottomForTallScreen() {
        int[] r = Fit.cover(1672, 941, 2560, 1080); // 21:9
        assertEquals(1672, r[2]);
        assertTrue(r[3] < 941);
        assertEquals(2560 / 1080.0, r[2] / (double) r[3], 0.01);
    }

    @Test
    void densityBreakpoints() {
        assertEquals(Fit.Density.COMPACT, Fit.Density.of(320, 180)); // 720p @ scale 4
        assertEquals(Fit.Density.COMPACT, Fit.Density.of(427, 240)); // 720p @ scale 3
        assertEquals(Fit.Density.NORMAL, Fit.Density.of(640, 360)); // 720p @ 2 / 1440p @ 4
        assertEquals(Fit.Density.NORMAL, Fit.Density.of(960, 540)); // 1080p @ 2
        assertEquals(Fit.Density.LARGE, Fit.Density.of(1920, 1080));
        assertEquals(Fit.Density.LARGE, Fit.Density.of(2560, 1440));
        assertEquals(7, Fit.Density.LARGE.pick(1, 4, 7));
    }

    @Test
    void fractionMaxWins() {
        assertEquals(100, Fit.fraction(1000, 0.1F, 50, 200));
        assertEquals(30, Fit.fraction(100, 0.1F, 50, 30), "min > max — не вылезаем");
    }

    // ---------------- UIMath: вход экрана / индикатор ----------------

    @Test
    void staggerOrdersElements() {
        assertEquals(0F, com.pwp.coreclient.ui.UIMath.stagger(100, 0, 200, 50, 250));
        assertEquals(1F, com.pwp.coreclient.ui.UIMath.stagger(10_000, 3, 200, 50, 250));
        float first = com.pwp.coreclient.ui.UIMath.stagger(400, 0, 200, 50, 250);
        float second = com.pwp.coreclient.ui.UIMath.stagger(400, 1, 200, 50, 250);
        assertTrue(first > second && second > 0F && first < 1F);
    }

    @Test
    void pingPongStaysInRangeAndReturns() {
        for (long t = -500; t < 3000; t += 37) {
            float v = com.pwp.coreclient.ui.UIMath.pingPong(t, 1400);
            assertTrue(v >= 0F && v <= 1F, "t=" + t + " v=" + v);
        }
        assertEquals(0F, com.pwp.coreclient.ui.UIMath.pingPong(0, 1400), 1e-6);
        assertEquals(1F, com.pwp.coreclient.ui.UIMath.pingPong(700, 1400), 1e-6);
    }
}

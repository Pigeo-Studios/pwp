package com.pwp.coreclient.ui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбиение 9-slice текстуры на куски для blit. Углы рисуются 1:1, края и центр — либо
 * ПОВТОРОМ кусков 1:1 ({@link Mode#TILE}: никакого растяжения, последний кусок обрезается
 * по UV), либо растяжением ({@link Mode#STRETCH_EDGES}: допустимо только для текстур, у
 * которых края однородны вдоль своей длины, — результат тот же, но в 1 blit).
 *
 * <p>Координаты целые (GUI scaled px): соседние куски стыкуются ровно, без щелей и
 * наложений при любом GUI scale (1 тексель = 1 scaled px). Если область меньше двух углов,
 * углы ужимаются (берётся внешняя часть угла), середина исчезает.
 */
public final class NineSlice {
    private NineSlice() {}

    public enum Mode {
        TILE,
        STRETCH_EDGES
    }

    /**
     * Кусок: куда рисовать (x, y, w, h) и откуда брать (u, v, uw, vh) в пикселях текстуры.
     * {@code w == uw && h == vh} — blit 1:1.
     */
    public record Piece(int x, int y, int w, int h, int u, int v, int uw, int vh) {
        public boolean oneToOne() {
            return w == uw && h == vh;
        }
    }

    /**
     * @param corner размер угла (одинаковый со всех сторон) в пикселях текстуры
     * @param drawCenter false — центр не рисуется (рамка поверх своей заливки)
     */
    public static List<Piece> pieces(Rect dst, int texW, int texH, int corner, Mode mode, boolean drawCenter) {
        List<Piece> out = new ArrayList<>();
        if (dst.w() <= 0 || dst.h() <= 0 || texW <= 0 || texH <= 0) return out;
        int c = Math.max(0, Math.min(corner, Math.min(texW, texH) / 2));
        int[] cols = axis(dst.w(), c, texW); // {left, middle, right}
        int[] rows = axis(dst.h(), c, texH);
        int midTexW = texW - 2 * c;
        int midTexH = texH - 2 * c;

        int[] dx = {dst.x(), dst.x() + cols[0], dst.x() + cols[0] + cols[1]};
        int[] dy = {dst.y(), dst.y() + rows[0], dst.y() + rows[0] + rows[1]};
        int[] su = {0, c, texW - cols[2]};
        int[] sv = {0, c, texH - rows[2]};

        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 3; col++) {
                int w = cols[col];
                int h = rows[r];
                if (w <= 0 || h <= 0) continue;
                boolean midX = col == 1;
                boolean midY = r == 1;
                if (midX && midY && !drawCenter) continue;
                int srcW = midX ? midTexW : w;
                int srcH = midY ? midTexH : h;
                if (srcW <= 0 || srcH <= 0) continue;
                if (!midX && !midY) {
                    out.add(new Piece(dx[col], dy[r], w, h, su[col], sv[r], w, h));
                } else if (mode == Mode.STRETCH_EDGES) {
                    out.add(new Piece(dx[col], dy[r], w, h, su[col], sv[r], srcW, srcH));
                } else {
                    for (int[] sx : Tiles.spans(w, midX ? srcW : w)) {
                        for (int[] sy : Tiles.spans(h, midY ? srcH : h)) {
                            out.add(new Piece(
                                    dx[col] + sx[0], dy[r] + sy[0], sx[1], sy[1], su[col], sv[r], sx[1], sy[1]));
                        }
                    }
                }
            }
        }
        return out;
    }

    /** Длины {начало, середина, конец} оси с ужатием углов. */
    static int[] axis(int length, int corner, int texLength) {
        if (length >= 2 * corner) return new int[] {corner, length - 2 * corner, corner};
        int first = length / 2;
        return new int[] {first, 0, length - first};
    }
}

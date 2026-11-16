package com.pwp.coreclient.ui.layout;

/** Вписывание картинок и адаптив экрана. */
public final class Fit {
    private Fit() {}

    /**
     * Область исходной картинки {u, v, uw, vh} для режима «cover»: картинка заполняет
     * {@code dstW x dstH} целиком без искажения пропорций, лишнее срезается поровну с краёв.
     */
    public static int[] cover(int srcW, int srcH, int dstW, int dstH) {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0)
            return new int[] {0, 0, Math.max(0, srcW), Math.max(0, srcH)};
        // сравнение пропорций в long без float: srcW/srcH vs dstW/dstH
        long lhs = (long) srcW * dstH;
        long rhs = (long) dstW * srcH;
        if (lhs > rhs) {
            // источник шире — режем бока
            int uw = (int) Math.max(1, rhs / dstH);
            return new int[] {(srcW - uw) / 2, 0, uw, srcH};
        }
        int vh = (int) Math.max(1, (long) srcW * dstH / dstW);
        return new int[] {0, (srcH - vh) / 2, srcW, Math.min(srcH, vh)};
    }

    /** Плотность вёрстки по размеру экрана в GUI-координатах. */
    public enum Density {
        /** Маленький экран (GUI scale 3–4 на 720p: ~320x180..427x240) — ужатые размеры. */
        COMPACT,
        NORMAL,
        /** Большой экран (scale 1–2 на 1440p) — крупнее отступы/логотип. */
        LARGE;

        public static final int COMPACT_MAX_H = 270;
        public static final int COMPACT_MAX_W = 480;
        public static final int LARGE_MIN_H = 600;
        public static final int LARGE_MIN_W = 1000;

        public static Density of(int screenW, int screenH) {
            if (screenH < COMPACT_MAX_H || screenW < COMPACT_MAX_W) return COMPACT;
            if (screenH >= LARGE_MIN_H && screenW >= LARGE_MIN_W) return LARGE;
            return NORMAL;
        }

        /** Значение для текущей плотности. */
        public int pick(int compact, int normal, int large) {
            return switch (this) {
                case COMPACT -> compact;
                case NORMAL -> normal;
                case LARGE -> large;
            };
        }
    }

    /**
     * Clamp(value * fraction) в [min, max] — доля экрана с пределами. При {@code min > max}
     * побеждает {@code max} (обычно max = «сколько реально есть места»: не вылезти важнее).
     */
    public static int fraction(int value, float fraction, int min, int max) {
        int v = Math.round(value * fraction);
        return Math.max(0, Math.min(max, Math.max(min, v)));
    }
}

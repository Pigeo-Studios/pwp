package com.pwp.coreclient.ui.layout;

/**
 * Неизменяемый прямоугольник в GUI-координатах (scaled px). Основа авто-лейаута UI-кита:
 * экран режет свою область на куски ({@link #inset}, {@link #top}, {@link #below}...) вместо
 * магических x/y. Ширина/высота никогда не отрицательные. Чистая логика без Minecraft.
 */
public record Rect(int x, int y, int w, int h) {

    public Rect {
        w = Math.max(0, w);
        h = Math.max(0, h);
    }

    public static Rect of(int x, int y, int w, int h) {
        return new Rect(x, y, w, h);
    }

    /** Весь экран. */
    public static Rect screen(int w, int h) {
        return new Rect(0, 0, w, h);
    }

    public int right() {
        return x + w;
    }

    public int bottom() {
        return y + h;
    }

    public int centerX() {
        return x + w / 2;
    }

    public int centerY() {
        return y + h / 2;
    }

    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    /** Пересекается ли с {@code o} по площади (касание краями — не пересечение; пустые — нет). */
    public boolean intersects(Rect o) {
        if (w <= 0 || h <= 0 || o.w <= 0 || o.h <= 0) return false;
        return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h;
    }

    /** Внутренний отступ со всех сторон (не схлопывается в отрицательный размер). */
    public Rect inset(int all) {
        return inset(all, all, all, all);
    }

    public Rect inset(int horizontal, int vertical) {
        return inset(horizontal, vertical, horizontal, vertical);
    }

    public Rect inset(int left, int top, int right, int bottom) {
        int nw = Math.max(0, w - left - right);
        int nh = Math.max(0, h - top - bottom);
        return new Rect(x + Math.min(left, w), y + Math.min(top, h), nw, nh);
    }

    /** Внешнее расширение (тень/свечение вокруг панели). */
    public Rect outset(int all) {
        return new Rect(x - all, y - all, w + 2 * all, h + 2 * all);
    }

    /** Верхняя полоса высотой {@code height} (не больше самой области). */
    public Rect top(int height) {
        return new Rect(x, y, w, Math.min(h, Math.max(0, height)));
    }

    public Rect bottomStrip(int height) {
        int hh = Math.min(h, Math.max(0, height));
        return new Rect(x, y + h - hh, w, hh);
    }

    public Rect left(int width) {
        return new Rect(x, y, Math.min(w, Math.max(0, width)), h);
    }

    public Rect rightStrip(int width) {
        int ww = Math.min(w, Math.max(0, width));
        return new Rect(x + w - ww, y, ww, h);
    }

    /** Остаток под верхней полосой высотой {@code height} плюс зазор {@code gap}. */
    public Rect below(int height, int gap) {
        int cut = Math.min(h, Math.max(0, height) + Math.max(0, gap));
        return new Rect(x, y + cut, w, h - cut);
    }

    /** Остаток над нижней полосой высотой {@code height} плюс зазор {@code gap}. */
    public Rect above(int height, int gap) {
        int cut = Math.min(h, Math.max(0, height) + Math.max(0, gap));
        return new Rect(x, y, w, h - cut);
    }

    /** Остаток правее левой полосы шириной {@code width} плюс зазор. */
    public Rect rightOf(int width, int gap) {
        int cut = Math.min(w, Math.max(0, width) + Math.max(0, gap));
        return new Rect(x + cut, y, w - cut, h);
    }

    /** Блок {@code cw x ch}, выровненный внутри (размер ограничен областью). */
    public Rect align(int cw, int ch, Align horizontal, Align vertical) {
        int ww = horizontal == Align.STRETCH ? w : Math.min(w, Math.max(0, cw));
        int hh = vertical == Align.STRETCH ? h : Math.min(h, Math.max(0, ch));
        return new Rect(x + horizontal.offset(w, ww), y + vertical.offset(h, hh), ww, hh);
    }

    public Rect centered(int cw, int ch) {
        return align(cw, ch, Align.CENTER, Align.CENTER);
    }

    /** Тот же размер, сдвиг на (dx, dy). */
    public Rect translate(int dx, int dy) {
        return new Rect(x + dx, y + dy, w, h);
    }

    public Rect withHeight(int height) {
        return new Rect(x, y, w, height);
    }

    public Rect withWidth(int width) {
        return new Rect(x, y, width, h);
    }
}

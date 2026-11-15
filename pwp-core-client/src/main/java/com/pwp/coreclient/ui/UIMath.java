package com.pwp.coreclient.ui;

/** Чистая геометрия UI-кита (радиалка, скролл, адаптив) — без Minecraft, покрыта тестами. */
public final class UIMath {
    private UIMath() {}

    /**
     * Сектор радиального меню под курсором: 0 — сектор, центр которого смотрит вверх, далее по
     * часовой стрелке. -1 — курсор в мёртвой зоне центра или за внешним радиусом.
     */
    public static int radialSector(
            double cx, double cy, double mx, double my, int sectors, double innerRadius, double outerRadius) {
        if (sectors <= 0) return -1;
        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < innerRadius || dist > outerRadius) return -1;
        // угол от «вверх» по часовой стрелке (ось Y экрана направлена вниз)
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0) angle += 360;
        double step = 360.0 / sectors;
        return (int) Math.floor(((angle + step / 2) % 360) / step);
    }

    /** Угол центра сектора (градусы от «вверх» по часовой стрелке). */
    public static double sectorCenterDeg(int index, int sectors) {
        return index * (360.0 / sectors);
    }

    /** Ограничение прокрутки: 0..max(0, content - viewport). */
    public static double clampScroll(double scroll, double contentHeight, double viewportHeight) {
        double max = Math.max(0, contentHeight - viewportHeight);
        return Math.max(0, Math.min(max, scroll));
    }

    /** Высота ползунка скроллбара (не меньше minThumb). */
    public static int thumbHeight(double contentHeight, double viewportHeight, int minThumb) {
        if (contentHeight <= viewportHeight || contentHeight <= 0) return (int) viewportHeight;
        return (int) Math.max(minThumb, viewportHeight * viewportHeight / contentHeight);
    }

    /** Позиция ползунка (от верха трека). */
    public static int thumbOffset(double scroll, double contentHeight, double viewportHeight, int thumb) {
        double max = contentHeight - viewportHeight;
        if (max <= 0) return 0;
        return (int) ((viewportHeight - thumb) * (scroll / max));
    }

    /**
     * Сдвигает прямоугольник (x, w) внутрь [0, limit) — для тултипов/контекстных меню у края
     * экрана. Возвращает новую координату.
     */
    public static int keepInside(int pos, int size, int limit, int margin) {
        if (pos + size > limit - margin) pos = limit - margin - size;
        return Math.max(margin, pos);
    }

    /** Пропорциональный размер от меньшей стороны экрана с ограничением [min, max]. */
    public static int scaled(int screenW, int screenH, float fraction, int min, int max) {
        int v = Math.round(Math.min(screenW, screenH) * fraction);
        return Math.max(min, Math.min(max, v));
    }
}

package com.pwp.coreclient.ui.hud;

/**
 * Проекция точки мира на GUI-экран для HUD-маркеров (этап 5: маркер над дроном). Та же формула,
 * что была в {@code DroneDeployOverlay.projectToScreen}: «вправо» = вперёд × вверх, полуугол =
 * настройка FOV / 2 в пределах 30..110°. Без аллокаций: результат — в изменяемый объект ({@link
 * #x}, {@link #y}, {@link #visible}).
 */
public final class ScreenProjection {
    /** Ближе этого (вдоль взгляда) — точка «за камерой», маркер не рисуется. */
    public static final double NEAR = 0.05;

    public static final double HALF_FOV_MIN = 30.0;
    public static final double HALF_FOV_MAX = 110.0;

    public int x;
    public int y;
    public boolean visible;

    /**
     * Спроецировать {@code (rx, ry, rz)} — точку относительно камеры; {@code l*}/{@code u*} —
     * единичные векторы камеры «вперёд»/«вверх»; {@code fovDeg} — настройка FOV; {@code w,h} —
     * GUI-размер экрана.
     *
     * @return {@link #visible}
     */
    public boolean project(
            double rx,
            double ry,
            double rz,
            float lx,
            float ly,
            float lz,
            float ux,
            float uy,
            float uz,
            double fovDeg,
            int w,
            int h) {
        double forward = rx * lx + ry * ly + rz * lz;
        if (!(forward >= NEAR)) {
            visible = false;
            return false;
        }
        // right = look × up
        double sx = (double) ly * uz - (double) lz * uy;
        double sy = (double) lz * ux - (double) lx * uz;
        double sz = (double) lx * uy - (double) ly * ux;
        double half = Math.toRadians(Math.max(HALF_FOV_MIN, Math.min(HALF_FOV_MAX, fovDeg / 2.0)));
        double tan = Math.tan(half);
        double right = rx * sx + ry * sy + rz * sz;
        double upDist = rx * ux + ry * uy + rz * uz;
        x = (int) (w / 2.0 + (right / forward) * (w / 2.0) / tan);
        y = (int) (h / 2.0 - (upDist / forward) * (h / 2.0) / tan);
        visible = true;
        return true;
    }
}

package com.pigeostudios.pwp.warfare.client.gui.map;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Проекция тактической карты на кадр: область экрана, мировой центр и масштаб (блоков на
 * пиксель). Одна на кадр — все слои считают координаты через неё (никаких своих формул).
 *
 * <p>Сетка координат: квадраты {@link #CELL} блоков от левого-верхнего угла карты матча,
 * колонки A–Z, AA… (как {@code SquadMapRenderer.getKP}), строки 1–N.
 */
@OnlyIn(Dist.CLIENT)
public record MapView(Rect area, double centerX, double centerZ, double bpp) {
    /** Размер квадрата сетки, блоков. */
    public static final long CELL = 300L;
    /** Мелкая сетка, блоков. */
    public static final long SUB_CELL = 100L;

    public double sx(double wx) {
        return area.x() + area.w() / 2.0 + (wx - centerX) / bpp;
    }

    public double sy(double wz) {
        return area.y() + area.h() / 2.0 + (wz - centerZ) / bpp;
    }

    public double worldX(double sx) {
        return centerX + (sx - (area.x() + area.w() / 2.0)) * bpp;
    }

    public double worldZ(double sy) {
        return centerZ + (sy - (area.y() + area.h() / 2.0)) * bpp;
    }

    /** Мировые X/Z начала сетки (кратно CELL, от угла карты матча). */
    public static long gridOriginX() {
        return Math.floorDiv((long) (ClientData.mapCenterX - ClientData.mapSizeBlocks / 2.0), CELL) * CELL;
    }

    public static long gridOriginZ() {
        return Math.floorDiv((long) (ClientData.mapCenterZ - ClientData.mapSizeBlocks / 2.0), CELL) * CELL;
    }

    /** Колонка сетки буквами: 0 → A, 25 → Z, 26 → AA; отрицательная — «?». */
    public static String column(long n) {
        if (n < 0) return "?";
        var sb = new StringBuilder();
        while (true) {
            sb.insert(0, (char) ('A' + (int) (n % 26)));
            if (n < 26) break;
            n = n / 26 - 1;
        }
        return sb.toString();
    }

    /** Квадрат карты (KP) точки мира, напр. «C4». */
    public static String kp(double wx, double wz) {
        long col = Math.floorDiv((long) wx - gridOriginX(), CELL);
        long row = Math.floorDiv((long) wz - gridOriginZ(), CELL) + 1;
        return column(col) + row;
    }

    /**
     * Играбельная зона в мировых координатах {minX, minZ, maxX, maxZ}: квадрат карты матча
     * (центр/размер из синхронизации ClientData), пересечённый с границей мира. Без карты матча —
     * только граница мира.
     */
    public static double[] playableWorld() {
        double minX = -Double.MAX_VALUE, minZ = -Double.MAX_VALUE, maxX = Double.MAX_VALUE, maxZ = Double.MAX_VALUE;
        if (ClientData.mapSizeBlocks > 0) {
            double h = ClientData.mapSizeBlocks / 2.0;
            minX = ClientData.mapCenterX - h;
            maxX = ClientData.mapCenterX + h;
            minZ = ClientData.mapCenterZ - h;
            maxZ = ClientData.mapCenterZ + h;
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            WorldBorder b = level.getWorldBorder();
            minX = Math.max(minX, b.getMinX());
            maxX = Math.min(maxX, b.getMaxX());
            minZ = Math.max(minZ, b.getMinZ());
            maxZ = Math.min(maxZ, b.getMaxZ());
        }
        return new double[] {minX, minZ, maxX, maxZ};
    }

    /** Играбельная зона на экране (целые px, может выходить за область карты). */
    public Rect playableScreen() {
        double[] w = playableWorld();
        double x0 = clampPx(sx(w[0])), y0 = clampPx(sy(w[1]));
        double x1 = clampPx(sx(w[2])), y1 = clampPx(sy(w[3]));
        int ix = (int) Math.floor(x0), iy = (int) Math.floor(y0);
        return Rect.of(ix, iy, Math.max(0, (int) Math.floor(x1) - ix), Math.max(0, (int) Math.floor(y1) - iy));
    }

    /** Ограничить координату разумным диапазоном (бесконечная граница не переполняет int). */
    private double clampPx(double v) {
        double lim = 4.0 * Math.max(area.w(), area.h()) + 10_000;
        double lo = Math.min(area.x(), area.y()) - lim;
        double hi = Math.max(area.right(), area.bottom()) + lim;
        return Math.max(lo, Math.min(hi, v));
    }
}

package com.pigeostudios.pwp.warfare.layout;

/**
 * Бокс зоны из map_config: {@code pos1}/{@code pos2} хранятся как заданы (порядок углов не нормализуется — для
 * round-trip), геометрия считается через min/max. Цилиндр = вписанный в бокс круг по XZ (как
 * {@code WarfareWorldData.MainProtectionZone}).
 */
public record Box(Pos pos1, Pos pos2) {

    public Box {
        if (pos1 == null || pos2 == null) throw new IllegalArgumentException("box corners required");
    }

    public static Box of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Box(new Pos(x1, y1, z1), new Pos(x2, y2, z2));
    }

    public int minX() {
        return Math.min(pos1.x(), pos2.x());
    }

    public int maxX() {
        return Math.max(pos1.x(), pos2.x());
    }

    public int minY() {
        return Math.min(pos1.y(), pos2.y());
    }

    public int maxY() {
        return Math.max(pos1.y(), pos2.y());
    }

    public int minZ() {
        return Math.min(pos1.z(), pos2.z());
    }

    public int maxZ() {
        return Math.max(pos1.z(), pos2.z());
    }

    public double centerX() {
        return (minX() + maxX()) / 2.0;
    }

    public double centerZ() {
        return (minZ() + maxZ()) / 2.0;
    }

    /** Радиус вписанного круга по XZ (для цилиндра). */
    public double radius() {
        return Math.min(maxX() - minX(), maxZ() - minZ()) / 2.0;
    }

    /** Нулевая площадь по XZ — зона вырождена. */
    public boolean isEmptyXZ() {
        return maxX() == minX() || maxZ() == minZ();
    }

    public boolean containsXZ(double x, double z) {
        return x >= minX() && x <= maxX() && z >= minZ() && z <= maxZ();
    }

    /** Пересечение по XZ (касание гранью не считается). */
    public boolean intersectsXZ(Box o) {
        return minX() < o.maxX() && o.minX() < maxX() && minZ() < o.maxZ() && o.minZ() < maxZ();
    }

    /** Пересечение с учётом формы: два цилиндра — по кругам, иначе по боксам. */
    public static boolean overlaps(Box a, String shapeA, Box b, String shapeB) {
        if (!a.intersectsXZ(b)) return false;
        if (Shapes.isCylinder(shapeA) && Shapes.isCylinder(shapeB)) {
            double dx = a.centerX() - b.centerX();
            double dz = a.centerZ() - b.centerZ();
            double r = a.radius() + b.radius();
            return dx * dx + dz * dz < r * r;
        }
        return true;
    }
}

package com.pigeostudios.pwp.warfare.layout;

/** Формы зон map_config. Неизвестная строка в игре трактуется как бокс (валидатор предупреждает). */
public final class Shapes {
    public static final String CYLINDER = "cylinder";
    public static final String BOX = "box";

    private Shapes() {}

    /** null/отсутствие ключа = cylinder (дефолт парсера WarfareMod). */
    public static boolean isCylinder(String shape) {
        return shape == null || CYLINDER.equalsIgnoreCase(shape);
    }

    public static boolean isKnown(String shape) {
        return shape == null || CYLINDER.equalsIgnoreCase(shape) || BOX.equalsIgnoreCase(shape);
    }
}

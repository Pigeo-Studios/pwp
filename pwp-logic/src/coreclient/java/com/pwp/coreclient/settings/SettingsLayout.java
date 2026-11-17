package com.pwp.coreclient.settings;

/**
 * Геометрия экрана настроек без Minecraft: ширина колонки категорий и контрола в ряду
 * «подпись — контрол». Подписи не короче {@code labelMin}: на узком экране (GUI scale 4)
 * сжимается контрол, а не подпись до нуля. Покрыто тестами.
 */
public final class SettingsLayout {
    private SettingsLayout() {}

    /** Доля ширины ряда под контрол. */
    public static final float CONTROL_FRACTION = 0.45F;
    /** Доля ширины панели под колонку категорий. */
    public static final float CATEGORY_FRACTION = 0.24F;

    /** Ширина контрола в ряду шириной rowW (зазор gap между подписью и контролом). */
    public static int controlWidth(int rowW, int gap, int min, int max, int labelMin) {
        int want = Math.round(rowW * CONTROL_FRACTION);
        want = Math.max(min, Math.min(max, want));
        int room = rowW - gap - labelMin;
        return Math.max(0, Math.min(want, room));
    }

    /** Ширина колонки категорий; не больше половины панели. */
    public static int categoryWidth(int panelW, int min, int max) {
        int want = Math.round(panelW * CATEGORY_FRACTION);
        want = Math.max(min, Math.min(max, want));
        return Math.max(0, Math.min(want, panelW / 2));
    }

    /** Сколько рядов высоты rowH с зазором gap влезает в высоту h (минимум 1). */
    public static int visibleRows(int h, int rowH, int gap) {
        if (rowH <= 0) return 1;
        return Math.max(1, (h + gap) / (rowH + gap));
    }
}

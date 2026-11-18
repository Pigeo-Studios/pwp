package com.pwp.coreclient.ui.hud;

/**
 * Сколько строк списка помещается в боковой панели HUD (голосования, ноки, голос): панель =
 * постоянная часть (шапка, полоса, подвал) + строки. Если все строки не влезают, последняя видимая
 * строка отдаётся под «+N» (сколько скрыто). Чистая логика без Minecraft, без аллокаций.
 */
public final class SideList {
    private SideList() {}

    /** Панель не помещается даже без строк. */
    public static final int NONE = -1;

    /**
     * Сколько строк показать.
     *
     * @param space свободное место по высоте (GUI-пиксели, {@code Stack.remaining()})
     * @param fixed высота постоянной части панели
     * @param row высота строки
     * @param count сколько строк в списке
     * @param minRows минимум строк, без которых панель не имеет смысла (0 — достаточно шапки)
     * @return число видимых строк данных (без строки «+N»); {@link #NONE} — панель не рисуется.
     *     Гарантия: {@link #height} для результата не больше {@code space}
     */
    public static int visible(int space, int fixed, int row, int count, int minRows) {
        int n = Math.max(0, count);
        int need = Math.min(Math.max(0, minRows), n);
        if (fixed < 0 || space < fixed) return NONE;
        if (row <= 0) return n;
        int slots = (space - fixed) / row;
        if (slots >= n) return n;
        // нет места даже под строку «+N» — панель со списком без списка не рисуется
        if (slots == 0) return NONE;
        // не влезают все: одна строка — «+N»
        int shown = Math.max(0, slots - 1);
        if (shown < need) return NONE;
        return shown;
    }

    /** Сколько строк займёт панель по высоте: видимые + строка «+N», если что-то скрыто. */
    public static int rows(int visible, int count) {
        if (visible < 0) return 0;
        return visible < count ? visible + 1 : visible;
    }

    /** Высота панели для {@code visible} видимых строк из {@code count}. */
    public static int height(int fixed, int row, int visible, int count) {
        return fixed + row * rows(visible, count);
    }
}

package com.pwp.api.util;

/**
 * Разбор чисел из пользовательского ввода без исключений.
 *
 * <p>Поля ввода (редакторы, настройки) получают промежуточные значения вроде «-» или «»
 * на каждом нажатии клавиши — это не ошибка, а нормальный ввод, поэтому вместо пустого
 * {@code catch (NumberFormatException)} возвращается значение по умолчанию.
 */
public final class Parse {
    private Parse() {}

    public static int intOr(String s, int fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (!t.matches("[-+]?\\d{1,10}")) return fallback;
        long v = Long.parseLong(t);
        return v < Integer.MIN_VALUE || v > Integer.MAX_VALUE ? fallback : (int) v;
    }

    public static float floatOr(String s, float fallback) {
        double v = doubleOr(s, Double.NaN);
        return Double.isNaN(v) ? fallback : (float) v;
    }

    public static double doubleOr(String s, double fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (!t.matches("[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?")) return fallback;
        double v = Double.parseDouble(t);
        return Double.isFinite(v) ? v : fallback;
    }
}

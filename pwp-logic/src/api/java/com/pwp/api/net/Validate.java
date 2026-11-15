package com.pwp.api.net;

/**
 * Проверки полей C→S пакетов для {@link Net.C2S#validate()}. Каждый метод возвращает null, если
 * значение корректно, иначе текст причины. Чистая логика — покрыта тестами.
 *
 * <p>Пример: {@code return Validate.first(Validate.id("skinId", skinId, 64), Validate.range("slot", slot, 0, 40));}
 */
public final class Validate {
    private Validate() {}

    /** Первая непустая причина или null. */
    public static String first(String... reasons) {
        for (String r : reasons) {
            if (r != null) return r;
        }
        return null;
    }

    /** Непустая строка не длиннее max символов. */
    public static String text(String field, String value, int max) {
        if (value == null || value.isEmpty()) return field + ": пусто";
        if (value.length() > max) return field + ": длиннее " + max;
        return null;
    }

    /** Строка (может быть пустой) не длиннее max символов. */
    public static String optionalText(String field, String value, int max) {
        if (value == null) return field + ": null";
        if (value.length() > max) return field + ": длиннее " + max;
        return null;
    }

    /**
     * Идентификатор: непустой, не длиннее max, только латиница/цифры и {@code _-.:/}
     * (ID ресурсов, имена скинов/техники/фракций).
     */
    public static String id(String field, String value, int max) {
        String t = text(field, value, max);
        if (t != null) return t;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '-'
                    || c == '.'
                    || c == ':'
                    || c == '/';
            if (!ok) return field + ": недопустимый символ '" + c + "'";
        }
        return null;
    }

    /**
     * Отображаемое имя-идентификатор (имена китов: «Drone Operator», «Rifleman (Optic)»): как
     * {@link #id}, плюс пробел и круглые скобки; без пробелов по краям.
     */
    public static String label(String field, String value, int max) {
        String t = text(field, value, max);
        if (t != null) return t;
        if (value.charAt(0) == ' ' || value.charAt(value.length() - 1) == ' ') return field + ": пробел по краям";
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ' ' || c == '(' || c == ')') continue;
            String one = id(field, String.valueOf(c), 1);
            if (one != null) return one;
        }
        return null;
    }

    public static String range(String field, long value, long min, long max) {
        if (value < min || value > max) return field + ": " + value + " вне [" + min + ", " + max + "]";
        return null;
    }

    /** Конечное число (не NaN/Infinity) в диапазоне. */
    public static String finite(String field, double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return field + ": не число";
        if (value < min || value > max) return field + ": " + value + " вне [" + min + ", " + max + "]";
        return null;
    }

    /** Значение из фиксированного набора. */
    public static String oneOf(String field, String value, String... allowed) {
        if (value != null) {
            for (String a : allowed) {
                if (a.equals(value)) return null;
            }
        }
        return field + ": недопустимое значение " + value;
    }
}

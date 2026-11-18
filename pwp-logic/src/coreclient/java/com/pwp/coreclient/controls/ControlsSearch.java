package com.pwp.coreclient.controls;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Поиск по экрану «Управление». Запрос режется по пробелам на слова; строка подходит, если КАЖДОЕ
 * слово нашлось хотя бы в одном поле (название, категория, имя клавиши, мод). Сравнение без
 * регистра посимвольно ({@link Character#toLowerCase(char)} — не зависит от локали и не меняет
 * длину строки, поэтому индексы подсветки совпадают с исходным текстом); «ё» приравнена к «е».
 * Пустой запрос — всё без изменений.
 */
public final class ControlsSearch {
    private ControlsSearch() {}

    /** Нормализация для сравнения: та же длина, что у исходной строки. */
    public static String fold(String s) {
        if (s == null) return "";
        char[] out = new char[s.length()];
        for (int i = 0; i < out.length; i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if (c == 'ё') c = 'е';
            out[i] = c;
        }
        return new String(out);
    }

    /** Слова запроса (нормализованные, без пустых). */
    public static List<String> tokens(String query) {
        List<String> out = new ArrayList<>();
        if (query == null) return out;
        for (String t : fold(query).trim().split("\\s+")) if (!t.isEmpty()) out.add(t);
        return out;
    }

    /** Подходит ли строка под запрос. */
    public static boolean matches(ControlsRow row, String query) {
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) return true;
        String label = fold(row.label());
        String cat = fold(row.categoryLabel());
        String key = fold(row.keyLabel());
        String mod = fold(row.mod());
        for (String t : tokens) {
            if (!label.contains(t) && !cat.contains(t) && !key.contains(t) && !mod.contains(t)) return false;
        }
        return true;
    }

    /**
     * Фильтр групп: остаются подходящие строки в прежнем порядке, пустые группы выкидываются. Если
     * запрос совпал с названием категории — строки этой категории подходят по этому слову.
     */
    public static List<ControlsGroup> filter(List<ControlsGroup> groups, String query) {
        if (tokens(query).isEmpty()) return groups;
        List<ControlsGroup> out = new ArrayList<>();
        for (ControlsGroup g : groups) {
            List<ControlsRow> rows = new ArrayList<>();
            for (ControlsRow r : g.rows()) if (matches(r, query)) rows.add(r);
            if (!rows.isEmpty()) out.add(new ControlsGroup(g.category(), g.title(), rows));
        }
        return out;
    }

    /**
     * Диапазоны подсветки в тексте: все непересекающиеся вхождения каждого слова запроса,
     * отсортированы и слиты; {@code int[]{start, end}} — end исключительно.
     */
    public static List<int[]> highlight(String text, String query) {
        List<int[]> raw = new ArrayList<>();
        if (text == null || text.isEmpty()) return raw;
        String hay = fold(text);
        for (String t : tokens(query)) {
            int from = 0;
            while (from <= hay.length() - t.length()) {
                int at = hay.indexOf(t, from);
                if (at < 0) break;
                raw.add(new int[] {at, at + t.length()});
                from = at + t.length();
            }
        }
        raw.sort(Comparator.<int[]>comparingInt(a -> a[0]).thenComparingInt(a -> a[1]));
        List<int[]> merged = new ArrayList<>();
        for (int[] r : raw) {
            int[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && r[0] <= last[1]) last[1] = Math.max(last[1], r[1]);
            else merged.add(new int[] {r[0], r[1]});
        }
        return merged;
    }
}

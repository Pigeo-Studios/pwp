package com.pwp.coreclient.ui.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Подгонка текста под ширину без Minecraft: ширину строки считает переданная функция
 * (в игре — {@code font::width}). Обрезка с «…» и перенос по словам с лимитом строк.
 * Работает по границам code point (суррогатные пары не рвутся).
 */
public final class TextFit {
    public static final String ELLIPSIS = "…";

    private TextFit() {}

    /**
     * Строка, укладывающаяся в {@code maxWidth}: как есть, либо самый длинный префикс + «…»
     * (хвостовые пробелы перед «…» срезаются). Если не влезает даже «…» — пустая строка.
     */
    public static String ellipsize(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || text.isEmpty()) return "";
        if (width.applyAsInt(text) <= maxWidth) return text;
        return forceEllipsis(text, maxWidth, width);
    }

    /** Самый длинный префикс + «…» (многоточие ставится всегда); не влезает «…» — пусто. */
    public static String forceEllipsis(String text, int maxWidth, ToIntFunction<String> width) {
        if (width.applyAsInt(ELLIPSIS) > maxWidth) return "";
        int lo = 0; // гарантированно влезает (только «…»)
        int hi = text.codePointCount(0, text.length());
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (width.applyAsInt(withEllipsis(text, mid)) <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return withEllipsis(text, lo);
    }

    private static String withEllipsis(String text, int codePoints) {
        int end = text.offsetByCodePoints(0, codePoints);
        return text.substring(0, end).stripTrailing() + ELLIPSIS;
    }

    /**
     * Перенос по словам в строки не шире {@code maxWidth}; '\n' — принудительный перенос;
     * слово длиннее строки режется по символам. Строк не больше {@code maxLines}
     * ({@code <= 0} — без лимита): лишнее уходит в последнюю строку с «…».
     */
    public static List<String> wrap(String text, int maxWidth, int maxLines, ToIntFunction<String> width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        maxWidth = Math.max(1, maxWidth);
        for (String paragraph : text.split("\n", -1)) {
            wrapParagraph(paragraph, maxWidth, width, lines);
        }
        if (maxLines > 0 && lines.size() > maxLines) {
            StringBuilder tail = new StringBuilder(lines.get(maxLines - 1));
            for (int i = maxLines; i < lines.size(); i++) tail.append(' ').append(lines.get(i));
            List<String> cut = new ArrayList<>(lines.subList(0, maxLines - 1));
            String fitted = forceEllipsis(tail.toString().stripTrailing(), maxWidth, width);
            cut.add(fitted);
            return cut;
        }
        return lines;
    }

    private static void wrapParagraph(String paragraph, int maxWidth, ToIntFunction<String> width, List<String> out) {
        String[] words = paragraph.trim().split(" +");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (width.applyAsInt(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (line.length() > 0) {
                out.add(line.toString());
                line.setLength(0);
            }
            // слово само по себе шире строки — режем по символам
            String rest = word;
            while (width.applyAsInt(rest) > maxWidth) {
                int take = longestPrefix(rest, maxWidth, width);
                out.add(rest.substring(0, take));
                rest = rest.substring(take);
            }
            line.append(rest);
        }
        if (line.length() > 0 || out.isEmpty() || paragraph.isBlank()) out.add(line.toString());
    }

    /** Длина (в char) самого длинного префикса шириной <= maxWidth; минимум один code point. */
    private static int longestPrefix(String s, int maxWidth, ToIntFunction<String> width) {
        int cpCount = s.codePointCount(0, s.length());
        int lo = 1;
        int hi = cpCount;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (width.applyAsInt(s.substring(0, s.offsetByCodePoints(0, mid))) <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.offsetByCodePoints(0, lo);
    }
}

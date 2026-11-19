package com.pwp.api.admin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Аудит-лог админ-действий (чистая логика): «кто, что, над кем, когда, чем кончилось». Хранит последние
 * {@code capacity} записей в памяти (для вкладки «Сервер») и форматирует строку для файла
 * {@code logs/pwp-admin-audit.log}. Запись в файл и потоки — у вызывающего (pwp-core-client
 * {@code AdminAudit}). Потокобезопасен (запись с главного потока, чтение — им же и фоновым писателем).
 */
public final class AuditTrail {
    /** Итог действия. */
    public enum Result {
        OK,
        /** Действие выполнено, но с ошибкой бизнес-логики (ключ ошибки в {@link Entry#error}). */
        FAILED,
        /** Нет прав — попытка тоже пишется. */
        DENIED
    }

    /**
     * @param epochMs время
     * @param actor ник того, кто сделал
     * @param level уровень доступа актёра (PLAYER/MODERATOR/ADMIN/OWNER)
     * @param action id действия ({@code match.stop}, {@code mod.kick}…)
     * @param details цель/параметры (ник, команда, значение), может быть пустым
     * @param result итог
     * @param error ключ ошибки или текст, пустой при OK
     */
    public record Entry(
            long epochMs, String actor, String level, String action, String details, Result result, String error) {}

    /** Максимум символов в полях (защита файла от мусора из пакета). */
    public static final int FIELD_MAX = 160;

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final int capacity;
    private final Deque<Entry> ring = new ArrayDeque<>();

    public AuditTrail(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }

    /** Добавить запись (поля обрезаются и чистятся от переводов строк). Возвращает сохранённую запись. */
    public synchronized Entry add(
            long epochMs, String actor, String level, String action, String details, Result result, String error) {
        Entry e = new Entry(epochMs, clean(actor), clean(level), clean(action), clean(details), result, clean(error));
        ring.addLast(e);
        while (ring.size() > capacity) ring.removeFirst();
        return e;
    }

    /** Последние {@code n} записей, новые первыми. */
    public synchronized List<Entry> recent(int n) {
        List<Entry> out = new ArrayList<>(Math.min(n, ring.size()));
        var it = ring.descendingIterator();
        while (it.hasNext() && out.size() < n) out.add(it.next());
        return out;
    }

    public synchronized int size() {
        return ring.size();
    }

    /** Строка файла: {@code 2026-11-19 12:00:00 | ADMIN Max | match.stop | details | OK}. */
    public static String format(Entry e) {
        StringBuilder sb = new StringBuilder(96);
        sb.append(TS.format(Instant.ofEpochMilli(e.epochMs())))
                .append(" | ")
                .append(e.level())
                .append(' ')
                .append(e.actor())
                .append(" | ")
                .append(e.action())
                .append(" | ")
                .append(e.details().isEmpty() ? "-" : e.details())
                .append(" | ")
                .append(e.result());
        if (!e.error().isEmpty()) sb.append(' ').append(e.error());
        return sb.toString();
    }

    /** null → "", переводы строк и «|» → пробел (одна запись = одна строка файла), обрезка до FIELD_MAX. */
    static String clean(String s) {
        if (s == null) return "";
        String t = s.replace('\n', ' ').replace('\r', ' ').replace('|', '/').strip();
        return t.length() > FIELD_MAX ? t.substring(0, FIELD_MAX) + "…" : t;
    }
}

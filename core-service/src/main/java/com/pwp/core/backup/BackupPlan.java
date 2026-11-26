package com.pwp.core.backup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Расписание и ротация бэкапов БД (этап 10a) — чистая логика без файлов и процессов. Имя файла:
 * {@code <db>-YYYYMMDD-HHmm.sql.gz}. Бэкап «пора», если сейчас час {@code hour} или позже и за сегодня бэкапа
 * ещё нет. Хранятся последние {@code keepDays} дней, но не меньше {@code keepMin} файлов (если сервер долго
 * лежал, последние бэкапы не удаляются только потому, что они старые).
 */
public final class BackupPlan {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final Pattern NAME = Pattern.compile("^(.+)-(\\d{8})-(\\d{4})\\.sql\\.gz$");

    private BackupPlan() {}

    public static String fileName(String db, LocalDateTime at) {
        return db + "-" + STAMP.format(at) + ".sql.gz";
    }

    /** Дата бэкапа из имени файла этой БД или null (чужой/битый файл). */
    public static LocalDate dateOf(String db, String name) {
        Matcher m = NAME.matcher(name);
        if (!m.matches() || !m.group(1).equals(db)) return null;
        try {
            return LocalDate.parse(m.group(2), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static boolean isDue(LocalDateTime now, int hour, List<String> existing, String db) {
        if (now.getHour() < hour) return false;
        for (String n : existing) if (now.toLocalDate().equals(dateOf(db, n))) return false;
        return true;
    }

    /** Какие файлы удалить: старше {@code keepDays}, но всегда остаются {@code keepMin} самых новых. */
    public static List<String> toDelete(List<String> existing, String db, LocalDate today, int keepDays, int keepMin) {
        List<String> ours = new ArrayList<>();
        for (String n : existing) if (dateOf(db, n) != null) ours.add(n);
        ours.sort(null); // имя = дата-время, сортировка по возрастанию = от старых к новым
        List<String> out = new ArrayList<>();
        int removable = Math.max(0, ours.size() - Math.max(1, keepMin));
        LocalDate border = today.minusDays(Math.max(1, keepDays));
        for (int i = 0; i < removable; i++) {
            if (dateOf(db, ours.get(i)).isBefore(border)) out.add(ours.get(i));
        }
        return out;
    }
}

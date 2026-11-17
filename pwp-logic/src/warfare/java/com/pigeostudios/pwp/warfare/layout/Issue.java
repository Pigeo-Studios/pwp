package com.pigeostudios.pwp.warfare.layout;

import java.util.List;
import java.util.Locale;

/**
 * Замечание к раскладке карты. ERROR — матч на такой раскладке стартовать нельзя (пока только лог), WARN —
 * подозрительно, но играбельно.
 *
 * @param subject объект замечания (id точки, команда, {@code BLUE[3]} для слота техники…), может быть пустым
 * @param detail человекочитаемая подробность для лога (английский/цифры), может быть пустой
 */
public record Issue(Severity severity, Code code, String subject, String detail) {

    public enum Severity {
        ERROR,
        WARN
    }

    /** Коды замечаний. Имя константы = суффикс ключа локализации — не переименовывать. */
    public enum Code {
        /** Элемент JSON не разобран (нет координат, не число) — пропущен. */
        PARSE_ELEMENT(Severity.ERROR),
        NO_MAIN(Severity.ERROR),
        MAIN_UNKNOWN_TEAM(Severity.ERROR),
        MAINS_OVERLAP(Severity.ERROR),
        MAIN_EMPTY(Severity.ERROR),
        TOO_FEW_POINTS(Severity.ERROR),
        DUPLICATE_POINT_ID(Severity.ERROR),
        POINT_EMPTY(Severity.ERROR),
        PRIORITY_MISSING(Severity.ERROR),
        PRIORITY_DUPLICATE(Severity.ERROR),
        PRIORITY_HOLE(Severity.ERROR),
        POINT_OUTSIDE_BORDER(Severity.ERROR),
        POINT_PARTLY_OUTSIDE_BORDER(Severity.WARN),
        POINT_IN_MAIN(Severity.WARN),
        POINTS_OVERLAP(Severity.WARN),
        PRIORITY_NOT_MIRRORED(Severity.WARN),
        SPAWN_OUTSIDE_MAIN(Severity.WARN),
        SPAWN_DERIVED(Severity.WARN),
        SPAWNER_OUTSIDE_BORDER(Severity.ERROR),
        SPAWNER_OUTSIDE_MAIN(Severity.WARN),
        SPAWNER_NO_VEHICLE(Severity.WARN),
        MAIN_OUTSIDE_BORDER(Severity.WARN),
        UNKNOWN_SHAPE(Severity.WARN),
        UNKNOWN_MODE(Severity.WARN),
        BORDER_INCOMPLETE(Severity.WARN);

        public final Severity severity;

        Code(Severity severity) {
            this.severity = severity;
        }

        /** Ключ локализации {@code pwpwarfare.layout.issue.<code>} (en/ru/uk в lang pwp-warfare). */
        public String langKey() {
            return "pwpwarfare.layout.issue." + name().toLowerCase(Locale.ROOT);
        }
    }

    public Issue {
        subject = subject == null ? "" : subject;
        detail = detail == null ? "" : detail;
    }

    public static Issue of(Code code, String subject, String detail) {
        return new Issue(code.severity, code, subject, detail);
    }

    public String langKey() {
        return code.langKey();
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public static boolean hasErrors(List<Issue> issues) {
        return issues.stream().anyMatch(Issue::isError);
    }

    /** Строка для серверного лога. */
    public String format() {
        StringBuilder sb = new StringBuilder().append(severity).append(' ').append(code);
        if (!subject.isEmpty()) sb.append(" [").append(subject).append(']');
        if (!detail.isEmpty()) sb.append(": ").append(detail);
        return sb.toString();
    }
}

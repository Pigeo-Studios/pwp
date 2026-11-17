package com.pigeostudios.pwp.warfare.match;

import java.util.List;
import java.util.Locale;

/**
 * Разбор и проверка ввода админ-действий матча (панель и команды {@code /pwpwarfare}). Раньше опечатка
 * молча становилась дефолтом (режим → aas, команда → RED) и команда отвечала «успех» — здесь неверный ввод
 * = null/false, вызывающий отвечает ошибкой.
 */
public final class AdminInput {
    private AdminInput() {}

    public static final String MODE_AAS = "aas";
    public static final String MODE_INVASION = "invasion";

    public static final int TICKETS_MAX = 100_000;
    public static final int WAITING_MIN = 0;
    public static final int WAITING_MAX = 3600;

    /** Фолбэк списка фракций, если core-service недоступен (фракции китов, docs/KITS_SQUAD_DESIGN.md). */
    public static final List<String> DEFAULT_FACTIONS =
            List.of("usa", "russia", "ukraine", "nato", "insurgency", "pmc");

    /** Фаза матча для панели (порядок проверки = приоритет). */
    public enum Phase {
        PAUSED,
        COUNTDOWN,
        INVASION_SETUP,
        WAITING,
        READY_VOTE,
        PLAYING,
        IDLE
    }

    /** "aas"/"invasion" в любом регистре; иначе null. */
    public static String mode(String s) {
        if (s == null) return null;
        String m = s.trim().toLowerCase(Locale.ROOT);
        return m.equals(MODE_AAS) || m.equals(MODE_INVASION) ? m : null;
    }

    /** "blue"/"red" в любом регистре → BLUE/RED; иначе null. */
    public static String team(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(Locale.ROOT);
        return t.equals("BLUE") || t.equals("RED") ? t : null;
    }

    /** id фракции: [a-z0-9_-]{1,32}, «clear»/«none» → "none"; иначе null. */
    public static String faction(String s) {
        if (s == null) return null;
        String f = s.trim().toLowerCase(Locale.ROOT);
        if (f.equals("clear") || f.equals("none")) return "none";
        if (f.isEmpty() || f.length() > 32) return null;
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-')) return null;
        }
        return f;
    }

    public static boolean ticketsValid(int n) {
        return n >= 0 && n <= TICKETS_MAX;
    }

    public static boolean waitingValid(int seconds) {
        return seconds >= WAITING_MIN && seconds <= WAITING_MAX;
    }

    public static Phase phase(
            boolean paused,
            boolean countdown,
            boolean invasionSetup,
            boolean waiting,
            boolean readyVote,
            boolean started) {
        if (paused) return Phase.PAUSED;
        if (countdown) return Phase.COUNTDOWN;
        if (invasionSetup) return Phase.INVASION_SETUP;
        if (waiting) return Phase.WAITING;
        if (readyVote && !started) return Phase.READY_VOTE;
        if (started) return Phase.PLAYING;
        return Phase.IDLE;
    }
}

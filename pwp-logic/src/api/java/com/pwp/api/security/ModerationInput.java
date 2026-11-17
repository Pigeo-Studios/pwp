package com.pwp.api.security;

import java.util.regex.Pattern;

/**
 * Ввод модерации из админ-панели → строка существующей серверной команды ({@code /kick}, {@code /pwpban},
 * {@code /pwpwarfare warn|voicemute|teamjoin|cleartenkill}). Клиент присылает только поля; сервер их
 * проверяет здесь и сам собирает команду — произвольный текст команды от клиента не принимается.
 */
public final class ModerationInput {
    private ModerationInput() {}

    public static final int TEXT_MAX = 200;
    /** Символ кода форматирования Minecraft (U+00A7). */
    private static final char SECTION = (char) 0xA7;

    public static final int MUTE_MINUTES_MAX = 10_080; // неделя

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern DURATION = Pattern.compile("([1-9][0-9]{0,4}[mhd])|perm");

    /** Ник Minecraft: латиница/цифры/_, 1–16. */
    public static boolean validName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Длительность бана для /pwpban: {@code 30m}, {@code 6h}, {@code 7d}, {@code perm}. */
    public static boolean validDuration(String d) {
        return d != null && DURATION.matcher(d).matches();
    }

    /**
     * Текст причины/сообщения: без управляющих символов и кодов цвета §, пробелы схлопнуты, обрезка до
     * {@link #TEXT_MAX}. Пустой → {@code fallback}.
     */
    public static String cleanText(String text, String fallback) {
        if (text == null) return fallback;
        StringBuilder sb = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < text.length() && sb.length() < TEXT_MAX; i++) {
            char c = text.charAt(i);
            if (c == SECTION) {
                i++; // код цвета: «§» + символ формата
                continue;
            }
            if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                if (sb.length() > 0) space = true;
                continue;
            }
            if (space) {
                sb.append(' ');
                space = false;
            }
            sb.append(c);
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? fallback : out;
    }

    /** Ник в кавычках для StringArgumentType.string() (у /pwpban); ник уже проверен {@link #validName}. */
    public static String kick(String name, String reason) {
        return "kick " + name + " " + cleanText(reason, "-");
    }

    public static String ban(String name, String duration, String reason) {
        return "pwpban " + name + " " + duration + " " + cleanText(reason, "-");
    }

    public static String warn(String name, String message) {
        return "pwpwarfare warn " + name + " " + cleanText(message, "-");
    }

    public static String voiceMute(String name, int minutes, String reason) {
        return "pwpwarfare voicemute " + name + " " + minutes + " " + cleanText(reason, "-");
    }

    /** team — BLUE/RED (уже проверено). */
    public static String teamJoin(String name, String team) {
        return "pwpwarfare teamjoin " + team.toLowerCase(java.util.Locale.ROOT) + " " + name;
    }

    public static String clearTeamkill(String name) {
        return "pwpwarfare cleartenkill " + name;
    }
}

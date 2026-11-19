package com.pwp.api.admin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Чистка строк лога перед показом в админ-панели: секреты не должны уйти клиенту, даже модератору.
 * Маскирует значения ключей/токенов/паролей ({@code key=…}, {@code "password": "…"}, {@code Bearer …},
 * заголовки {@code X-API-Key}/{@code X-Launch-Token}), IPv4-адреса (ПДн игроков) и длинные hex/base64
 * последовательности (подписи, хэши сессий). Строка обрезается до {@link #MAX_LEN}.
 */
public final class LogScrub {
    public static final int MAX_LEN = 300;
    static final String MASK = "***";

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)((?:api[_-]?key|apikey|token|secret|password|passwd|pwd|p5secret|authorization|x-api-key|"
                    + "x-launch-token|x-pwp-sign|session|cookie)\"?\\s*[:=]\\s*\"?)([^\"\\s,;}&]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+");
    private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern LONG_BLOB = Pattern.compile("\\b[A-Za-z0-9+/_-]{32,}={0,2}");

    private LogScrub() {}

    public static String scrub(String line) {
        if (line == null) return "";
        String s = line.replace('\n', ' ').replace('\r', ' ');
        // Bearer — раньше key=value: иначе «Authorization: Bearer X» маскировало слово Bearer, а токен оставался
        s = BEARER.matcher(s).replaceAll("$1" + MASK);
        s = KEY_VALUE.matcher(s).replaceAll("$1" + MASK);
        s = replaceIps(s);
        s = LONG_BLOB.matcher(s).replaceAll(MASK);
        return s.length() > MAX_LEN ? s.substring(0, MAX_LEN) + "…" : s;
    }

    /** IPv4 → маска, кроме loopback 127.x (адрес core в логах полезен и не ПДн). */
    private static String replaceIps(String s) {
        Matcher m = IPV4.matcher(s);
        StringBuilder sb = new StringBuilder(s.length());
        while (m.find()) {
            String ip = m.group();
            m.appendReplacement(sb, ip.startsWith("127.") ? ip : "x.x.x.x");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}

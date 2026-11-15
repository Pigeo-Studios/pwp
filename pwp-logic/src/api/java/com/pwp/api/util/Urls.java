package com.pwp.api.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Экранирование сегментов пути и параметров запроса для URL core-service. */
public final class Urls {
    private Urls() {}

    /** Экранирует значение для сегмента пути или параметра запроса (пробел → %20). */
    public static String enc(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

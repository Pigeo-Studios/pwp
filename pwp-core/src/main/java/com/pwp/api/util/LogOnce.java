package com.pwp.api.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Предупреждение в лог один раз на ключ — для кода, который выполняется каждый кадр/тик
 * (рендер, совместимость с чужими модами через рефлексию): ошибка видна в логе со стеком,
 * но не забивает его тысячами одинаковых строк.
 */
public final class LogOnce {
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    private LogOnce() {}

    public static void warn(Logger log, String key, String message, Throwable error) {
        if (SEEN.add(key)) {
            log.warn(message + " (дальнейшие повторы не логируются)", error);
        }
    }
}

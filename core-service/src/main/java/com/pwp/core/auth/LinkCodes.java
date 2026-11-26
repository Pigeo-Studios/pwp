package com.pwp.core.auth;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Одноразовые коды привязки Discord (этап 10d): игрок в игре получает 6 цифр, вводит их боту {@code /link}.
 * Код живёт {@link #TTL_MS}, один активный код на игрока (новый заменяет старый), после использования удаляется.
 * Только память: рестарт core = коды недействительны (игрок просто запросит новый).
 */
public final class LinkCodes {
    public static final long TTL_MS = 10 * 60_000L;

    private record Entry(String uuid, long expiresAt) {}

    private final Map<String, Entry> byCode = new ConcurrentHashMap<>();
    private final SecureRandom random;

    public LinkCodes(SecureRandom random) {
        this.random = random;
    }

    public synchronized String issue(String uuid, long now) {
        purge(now);
        byCode.values().removeIf(e -> e.uuid.equals(uuid));
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (byCode.containsKey(code));
        byCode.put(code, new Entry(uuid, now + TTL_MS));
        return code;
    }

    /** uuid владельца кода или null (нет, истёк). Код одноразовый. */
    public synchronized String redeem(String code, long now) {
        if (code == null) return null;
        Entry e = byCode.remove(code.trim());
        if (e == null || e.expiresAt < now) return null;
        return e.uuid;
    }

    public synchronized int size(long now) {
        purge(now);
        return byCode.size();
    }

    private void purge(long now) {
        for (Iterator<Entry> it = byCode.values().iterator(); it.hasNext(); ) {
            if (it.next().expiresAt < now) it.remove();
        }
    }
}

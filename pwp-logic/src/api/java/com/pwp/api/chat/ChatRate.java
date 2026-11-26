package com.pwp.api.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Антиспам чата (этап 10 ROADMAP): не больше {@code perWindow} сообщений за {@link #WINDOW_MS} и без повтора
 * того же текста за {@link #REPEAT_MS}. Ключ — игрок (UUID строкой). Время передаёт вызывающий (мс).
 */
public final class ChatRate {
    public static final long WINDOW_MS = 5_000;
    public static final long REPEAT_MS = 30_000;

    /** Отказ: null — можно; иначе ключ lang причины. */
    public static final String SPAM = "pwp_core.chat.spam";

    public static final String REPEAT = "pwp_core.chat.repeat";

    private record Last(String text, long at) {}

    private final Map<String, Deque<Long>> sent = new HashMap<>();
    private final Map<String, Last> last = new HashMap<>();

    /** Проверить и (если можно) учесть сообщение. {@code perWindow <= 0} — без лимита частоты. */
    public synchronized String check(String who, String text, long nowMs, int perWindow) {
        String norm = text == null ? "" : ChatFilter.normalize(text);
        Last prev = last.get(who);
        if (!norm.isEmpty() && prev != null && prev.text.equals(norm) && nowMs - prev.at < REPEAT_MS) return REPEAT;
        Deque<Long> q = sent.computeIfAbsent(who, k -> new ArrayDeque<>());
        while (!q.isEmpty() && nowMs - q.peekFirst() >= WINDOW_MS) q.pollFirst();
        if (perWindow > 0 && q.size() >= perWindow) return SPAM;
        q.addLast(nowMs);
        last.put(who, new Last(norm, nowMs));
        return null;
    }

    /** Игрок вышел — забыть. */
    public synchronized void forget(String who) {
        sent.remove(who);
        last.remove(who);
    }
}

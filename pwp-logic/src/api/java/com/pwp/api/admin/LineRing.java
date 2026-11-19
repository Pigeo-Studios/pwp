package com.pwp.api.admin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Кольцо последних строк (WARN/ERROR сервера для вкладки «Сервер»). Повтор той же строки подряд не
 * дублируется — увеличивается счётчик ({@code ×N}), иначе один спам-цикл вытесняет всё полезное.
 * Потокобезопасно: пишет поток логгера, читает главный поток сервера.
 */
public final class LineRing {
    public record Line(long epochMs, String level, String logger, String text, int repeats) {}

    private final int capacity;
    private final Deque<Line> ring = new ArrayDeque<>();

    public LineRing(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }

    public synchronized void add(long epochMs, String level, String logger, String text) {
        Line last = ring.peekLast();
        if (last != null && last.text().equals(text) && last.level().equals(level)) {
            ring.removeLast();
            ring.addLast(new Line(epochMs, level, logger, text, last.repeats() + 1));
            return;
        }
        ring.addLast(new Line(epochMs, level, logger, text, 1));
        while (ring.size() > capacity) ring.removeFirst();
    }

    /** Последние {@code n}, новые первыми. */
    public synchronized List<Line> recent(int n) {
        List<Line> out = new ArrayList<>(Math.min(n, ring.size()));
        var it = ring.descendingIterator();
        while (it.hasNext() && out.size() < n) out.add(it.next());
        return out;
    }

    public synchronized void clear() {
        ring.clear();
    }
}

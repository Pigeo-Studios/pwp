package com.pwp.api.lobby;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Очередь в полный матч (этап 10 ROADMAP). Игроки с приоритетом (модераторы и выше — резервные слоты) идут
 * вперёд остальных, внутри группы — по времени входа. Позиция считается от 1. Время — мс вызывающего.
 */
public final class JoinQueue {

    private record Entry(boolean priority, long since) {}

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    /** Встать в очередь (повторно — позиция сохраняется, приоритет обновляется). Возвращает позицию. */
    public synchronized int join(String id, boolean priority, long nowMs) {
        Entry e = entries.get(id);
        entries.put(id, new Entry(priority, e == null ? nowMs : e.since));
        return position(id);
    }

    public synchronized boolean leave(String id) {
        return entries.remove(id) != null;
    }

    public synchronized boolean contains(String id) {
        return entries.containsKey(id);
    }

    public synchronized int size() {
        return entries.size();
    }

    /** Позиция (1…), 0 — не в очереди. */
    public synchronized int position(String id) {
        int i = 1;
        for (String k : ordered()) {
            if (k.equals(id)) return i;
            i++;
        }
        return 0;
    }

    /**
     * Свободно {@code publicFree} обычных слотов и {@code reservedFree} резервных: кого пустить. Пущенные
     * из очереди удаляются. Приоритетные сначала занимают резерв, потом общие слоты.
     */
    public synchronized List<String> admit(int publicFree, int reservedFree) {
        List<String> out = new ArrayList<>();
        int pub = Math.max(0, publicFree);
        int res = Math.max(0, reservedFree);
        for (String k : ordered()) {
            boolean prio = entries.get(k).priority;
            if (prio && res > 0) {
                res--;
                out.add(k);
            } else if (pub > 0) {
                pub--;
                out.add(k);
            }
        }
        out.forEach(entries::remove);
        return out;
    }

    /** Убрать стоящих дольше {@code maxWaitMs}; вернуть их. */
    public synchronized List<String> expire(long nowMs, long maxWaitMs) {
        List<String> out = new ArrayList<>();
        entries.forEach((k, e) -> {
            if (nowMs - e.since > maxWaitMs) out.add(k);
        });
        out.forEach(entries::remove);
        return out;
    }

    private List<String> ordered() {
        List<String> prio = new ArrayList<>();
        List<String> rest = new ArrayList<>();
        // LinkedHashMap хранит порядок первого входа; since не меняется при повторном join
        entries.entrySet().stream()
                .sorted((a, b) -> Long.compare(a.getValue().since, b.getValue().since))
                .forEach(e -> (e.getValue().priority ? prio : rest).add(e.getKey()));
        prio.addAll(rest);
        return prio;
    }
}

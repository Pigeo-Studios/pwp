package com.pigeostudios.sbwchunkload.ticket;

import java.util.HashMap;
import java.util.Map;

/**
 * Кэш записей тикетов: ChunkPos.toLong() -> TicketEntry. Один тикет на чанк
 * при любой плотности огня (очередь из 50 пуль в одних чанках = 1 запись).
 *
 * Записи с refCount=0 «спят» до истечения TTL — после этого удаляются
 * полностью (тикет ванили к тому времени уже умер сам).
 */
public final class TicketCache {

    private final Map<Long, TicketEntry> entries = new HashMap<>();

    /** Число активных (refCount > 0) записей — кэшируется для дешёвых лимит-проверок. */
    private int activeCount = 0;

    public TicketEntry getOrCreate(long chunkKey, int tick) {
        return entries.computeIfAbsent(chunkKey, key -> new TicketEntry(key, tick));
    }

    public TicketEntry get(long chunkKey) {
        return entries.get(chunkKey);
    }

    /** Запись активировалась (refCount 0 -> 1). */
    public void incrementActive() {
        activeCount++;
    }

    /** Запись заснула (refCount 1 -> 0). */
    public void decrementActive() {
        if (activeCount > 0) activeCount--;
    }

    public int activeCount() {
        return activeCount;
    }

    /** Удаляет «спящие» записи, у которых TTL истёк. Возвращает число удалённых. */
    public int cleanup(int tick, int ttlTicks) {
        int removed = 0;
        var it = entries.entrySet().iterator();
        while (it.hasNext()) {
            TicketEntry entry = it.next().getValue();
            if (entry.refCount <= 0 && tick - entry.expirationTick > ttlTicks) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public int size() {
        return entries.size();
    }
}

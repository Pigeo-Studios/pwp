package com.pigeostudios.sbwchunkload.ticket;

/**
 * Запись тикет-кэша на один чанк. Считает ссылки: N снарядов летят через
 * один чанк -> один тикет, refCount = N. Когда refCount упал до 0, запись
 * «засыпает» (expirationTick выставляется), продление прекращается —
 * тикет ванили умирает сам за ~5 сек, чанк выгружается.
 */
public final class TicketEntry {

    /** ChunkPos.toLong(). */
    final long chunkKey;

    /** Сколько коридоров снарядов покрывают этот чанк. */
    int refCount;

    /** Последний тик, когда реально вызывался addRegionTicket (дедуп продлений). */
    int lastRefresh;

    /** Тик, когда refCount упал до 0 (-1 = активен, чанк ещё держится). */
    int expirationTick = -1;

    TicketEntry(long chunkKey, int tick) {
        this.chunkKey = chunkKey;
        this.lastRefresh = tick - 1_000_000; // первый ensure всегда обновляет
    }
}

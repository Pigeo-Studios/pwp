package com.pigeostudios.sbwchunkload.corridor;

/**
 * Результат сравнения старого и нового коридоров снаряда: чанки, которые
 * снаряд покинул (нужно release-нуть тикеты), чанки, в которые вошёл
 * (нужно ensure-нуть), и чанки, которые остались (нужно только продлить).
 */
public final class ChunkCorridorDiff {

    private final long[] added;
    private final long[] retained;
    private final long[] removed;

    ChunkCorridorDiff(long[] added, long[] retained, long[] removed) {
        this.added = added;
        this.retained = retained;
        this.removed = removed;
    }

    /** Чанки, которых не было в старом коридоре (ensure: инкремент + тикет). */
    public long[] added() {
        return added;
    }

    /** Чанки, которые были и остались (refresh: только продление тикета). */
    public long[] retained() {
        return retained;
    }

    /** Чанки, которых нет в новом коридоре (release). */
    public long[] removed() {
        return removed;
    }
}

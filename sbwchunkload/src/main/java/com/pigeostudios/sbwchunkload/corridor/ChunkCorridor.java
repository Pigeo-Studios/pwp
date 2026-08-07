package com.pigeostudios.sbwchunkload.corridor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Логика коридора снаряда: старый список чанков -> новый список чанков.
 * Считает, что ушло, что добавилось и что осталось. Чистая математика над
 * long[]-ключами ChunkPos — приложение тикетов делает ChunkTicketManager
 * через Tracker.
 */
public final class ChunkCorridor {

    private ChunkCorridor() {
    }

    /** Сравнивает старый и новый коридоры. */
    public static ChunkCorridorDiff diff(long[] oldCorridor, long[] newCorridor) {
        Set<Long> oldSet = toSet(oldCorridor);
        Set<Long> newSet = toSet(newCorridor);

        long[] added = subtract(newSet, oldSet);
        long[] removed = subtract(oldSet, newSet);
        long[] retained = intersect(oldSet, newSet);

        return new ChunkCorridorDiff(added, retained, removed);
    }

    private static Set<Long> toSet(long[] corridor) {
        Set<Long> set = new HashSet<>();
        for (long key : corridor) {
            set.add(key);
        }
        return set;
    }

    private static long[] subtract(Set<Long> from, Set<Long> what) {
        List<Long> result = new ArrayList<>();
        for (Long key : from) {
            if (!what.contains(key)) {
                result.add(key);
            }
        }
        return toArray(result);
    }

    private static long[] intersect(Set<Long> a, Set<Long> b) {
        List<Long> result = new ArrayList<>();
        for (Long key : a) {
            if (b.contains(key)) {
                result.add(key);
            }
        }
        return toArray(result);
    }

    private static long[] toArray(List<Long> list) {
        long[] result = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}

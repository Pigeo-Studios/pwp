package com.pigeostudios.sbwchunkload.scheduler;

import com.pigeostudios.sbwchunkload.state.ProjectileState;

/**
 * Timing Wheel: 20 бакетов, по одному на тик. Снаряд сам «говорит», когда
 * его обновить: при регистрации/после обработки кладётся в бакет
 * (currentTick + refreshInterval) % 20. Каждый тик обрабатывается ровно
 * один бакет — CPU на обработку тратится только на снаряды, которым
 * реально пора.
 *
 * Такой подход заменяет старый скан «каждый тик по всем снарядам»
 * с глобальным счётчиком фаз.
 */
public final class TimingWheel {

    /** Количество бакетов = максимальный период обновления (тики). */
    public static final int BUCKETS = 20;

    private final UpdateBucket[] buckets = new UpdateBucket[BUCKETS];

    public TimingWheel() {
        for (int i = 0; i < BUCKETS; i++) {
            buckets[i] = new UpdateBucket();
        }
    }

    /** Планирует состояние через ticksAhead тиков (минимум 1, максимум BUCKETS). */
    public void schedule(ProjectileState state, int ticksAhead, int currentTick) {
        int delta = Math.max(1, Math.min(ticksAhead, BUCKETS));
        int bucket = (currentTick + delta) % BUCKETS;
        buckets[bucket].add(state);
    }

    /** Возвращает бакет для текущего тика (обработчик забирает все состояния). */
    public UpdateBucket bucketFor(int tick) {
        return buckets[tick % BUCKETS];
    }

    /** Полное число запланированных состояний (для debug). */
    public int scheduledCount() {
        int total = 0;
        for (UpdateBucket bucket : buckets) {
            total += bucket.size();
        }
        return total;
    }
}

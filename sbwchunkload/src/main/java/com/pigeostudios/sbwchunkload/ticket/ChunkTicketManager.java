package com.pigeostudios.sbwchunkload.ticket;

import com.pigeostudios.sbwchunkload.config.ChunkLoadingConfig;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Единственное место, которое знает про Minecraft-тикеты. Tracker/Corridor
 * не имеют понятия, КАК чанки загружаются — они лишь говорят
 * ensureLoaded(chunk) / refresh(chunk) / release(chunk).
 *
 * Механика:
 * - Свой TicketType<Integer> (НЕ PORTAL! Ticket.compareTo при равных типе и
 *   уровне сравнивает значения — ванильные портальные тикеты типизированы
 *   BlockPos, сравнение Integer с BlockPos = ClassCastException, краш матч-
 *   сервера 05.08.2026). Чужие типы не доходят до сравнения значений.
 * - Общее значение на чанк (константа мода): N снарядов в одном чанке =
 *   1 запись тикета. Повторный addRegionTicket с тем же value продлевает
 *   существующий тикет, а не плодит новые.
 * - Уровень тикета = min(3, simulationDistance) — сущности в 1.20.1 тикаются
 *   только в чанках с distance <= simulationDistance; уровень выше = чанк
 *   загружен, но снаряд не тикается (инцидент 05.08.2026).
 * - Smart-проверка isPositionEntityTicking, НЕ isLoaded: между view и sim
 *   чанки загружены, но сущности в них не тикаются (инцидент 05.08.2026).
 * - World border: тикеты ЗА границей мира не ставятся — иначе ваниль
 *   генерирует чанки до бесконечности за снарядом, летящим за границу
 *   (источник «снаряд летит почти бесконечно»).
 * - Освобождение по TTL: removeRegionTicket не вызывается никогда — при
 *   refCount=0 продление прекращается, ванильный тикет умирает сам за ~5 сек.
 * - Лимит активных тикетов (maxActiveTickets): защита от взрывного роста
 *   уникальных чанков при массовой стрельбе вдаль.
 */
public final class ChunkTicketManager {

    /** Общее значение тикета — одна запись на чанк при любой плотности огня. */
    private static final Object SHARED_TICKET_VALUE = 0x5B7C;

    private static final TicketType<Integer> PROJECTILE_TICKET_TYPE =
        TicketType.create("sbwchunkload:projectile", Integer::compareTo);

    private final TicketCache cache = new TicketCache();

    /** Уровень тикета (задаётся SimulationDistanceWatcher'ом). */
    private int ticketLevel = 3;

    /** Счётчики для debug. */
    private long totalRefreshes = 0;
    private long totalSkipsSmart = 0;
    private long totalSkipsDedup = 0;
    private long totalSkipsBorder = 0;
    private long totalSkipsLimit = 0;

    private static final Map<ServerLevel, ChunkTicketManager> INSTANCES = new HashMap<>();

    private ChunkTicketManager() {
    }

    /** Менеджер на уровень (тикеты ставятся в чанк-кэш уровня). */
    public static ChunkTicketManager of(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, l -> new ChunkTicketManager());
    }

    /** Уровень тикета: min(3, simulationDistance). */
    public void setTicketLevel(int ticketLevel) {
        this.ticketLevel = Math.min(3, Math.max(1, ticketLevel));
    }

    public int ticketLevel() {
        return ticketLevel;
    }

    /**
     * Гарантирует, что чанк держится тикетом. Вызывается для НОВЫХ чанков
     * коридора. Повторные вызовы из разных снарядов лишь увеличивают
     * refCount — тикет один.
     */
    public void ensureLoaded(ServerLevel level, long chunkKey, int tick) {
        // Smart-режим: чанк уже тикается — тикет не нужен вообще.
        if (ChunkLoadingConfig.SMART_CHUNK_LOADING.get()
            && level.isPositionEntityTicking(new ChunkPos(chunkKey).getWorldPosition())) {
            totalSkipsSmart++;
            return;
        }
        // World border: за границей тикеты не ставим (бесконечная генерация).
        if (!isWithinBorder(level, chunkKey)) {
            totalSkipsBorder++;
            return;
        }

        TicketEntry entry = cache.getOrCreate(chunkKey, tick);

        // Активация (новая запись или «воскрешение» спящей): под лимитом
        // активных тикетов. Спящий тикет ванили, возможно, уже умер —
        // ставим заново без оглядки на дедуп.
        if (entry.refCount == 0) {
            if (cache.activeCount() >= ChunkLoadingConfig.MAX_ACTIVE_TICKETS.get()) {
                totalSkipsLimit++;
                return;
            }
            entry.refCount = 1;
            entry.expirationTick = -1;
            entry.lastRefresh = tick - 1_000_000;
            cache.incrementActive();
        } else {
            entry.refCount++;
        }

        // Дедуп продлений: на один чанк не чаще ticketIntervalTicks.
        int interval = ChunkLoadingConfig.TICKET_INTERVAL_TICKS.get();
        if (tick - entry.lastRefresh < interval) {
            totalSkipsDedup++;
            return;
        }

        refreshTicket(level, entry, tick);
    }

    /**
     * Продлевает тикет на чанк БЕЗ изменения refCount. Вызывается для чанков,
     * которые уже покрыты коридором снаряда (retained при пересчёте коридора
     * и при «ничего не изменилось»). Если записи нет или она «спит» — переходит
     * в ensureLoaded (восстановление).
     */
    public void refresh(ServerLevel level, long chunkKey, int tick) {
        TicketEntry entry = cache.get(chunkKey);
        if (entry == null || entry.refCount == 0) {
            ensureLoaded(level, chunkKey, tick);
            return;
        }
        // Коридор смотрит вперёд: чанк может оказаться за границей, даже когда
        // сам снаряд ещё в пределах — тикет за границу не продлеваем.
        if (!isWithinBorder(level, chunkKey)) {
            totalSkipsBorder++;
            return;
        }
        int interval = ChunkLoadingConfig.TICKET_INTERVAL_TICKS.get();
        if (tick - entry.lastRefresh < interval) {
            totalSkipsDedup++;
            return;
        }
        refreshTicket(level, entry, tick);
    }

    /**
     * Снаряд покинул чанк (или умер): ссылка снимается. При refCount=0
     * запись «засыпает» — продление прекращается, тикет умирает сам.
     */
    public void release(long chunkKey, int tick) {
        TicketEntry entry = cache.get(chunkKey);
        if (entry == null || entry.refCount <= 0) return;
        entry.refCount--;
        if (entry.refCount == 0) {
            entry.expirationTick = tick;
            cache.decrementActive();
        }
    }

    /** World border: чанк внутри границы мира? */
    private static boolean isWithinBorder(ServerLevel level, long chunkKey) {
        return level.getWorldBorder().isWithinBounds(new ChunkPos(chunkKey).getWorldPosition());
    }

    private void refreshTicket(ServerLevel level, TicketEntry entry, int tick) {
        ServerChunkCache chunkSource = level.getChunkSource();
        ChunkPos pos = new ChunkPos(entry.chunkKey);
        chunkSource.addRegionTicket(PROJECTILE_TICKET_TYPE, pos, ticketLevel, (Integer) SHARED_TICKET_VALUE);
        entry.lastRefresh = tick;
        totalRefreshes++;
    }

    /** Ежедневная чистка спящих записей. */
    public void cleanup(int tick) {
        cache.cleanup(tick, ChunkLoadingConfig.TICKET_TTL_TICKS.get());
    }

    public TicketCache cache() {
        return cache;
    }

    // ===== Debug-статистика =====

    public long totalRefreshes() {
        return totalRefreshes;
    }

    public long totalSkipsSmart() {
        return totalSkipsSmart;
    }

    public long totalSkipsDedup() {
        return totalSkipsDedup;
    }

    public long totalSkipsBorder() {
        return totalSkipsBorder;
    }

    public long totalSkipsLimit() {
        return totalSkipsLimit;
    }
}

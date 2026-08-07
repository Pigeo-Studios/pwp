package com.pigeostudios.sbwchunkload.drone;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Состояние отслеживаемого дрона: уровень (для освобождения тикетов
 * после смерти сущности) и текущий чанк-коридор под тикетами.
 * Чистое состояние — никакой логики тикетов и чанков.
 */
public final class DroneTrackState {

    private final int entityId;
    private final ServerLevel level;
    private long[] corridor = new long[0];
    /** Сколько интервалов подряд дрон стоит на земле без управления. */
    private int groundTicks = 0;

    public DroneTrackState(Entity entity, ServerLevel level) {
        this.entityId = entity.getId();
        this.level = level;
    }

    public int entityId() {
        return entityId;
    }

    /** Уровень, в котором летит дрон. */
    public ServerLevel level() {
        return level;
    }

    /** Текущий коридор: чанки (toLong), покрытые тикетами от имени дрона. */
    public long[] corridor() {
        return corridor;
    }

    public void setCorridor(long[] corridor) {
        this.corridor = corridor;
    }

    public int groundTicks() {
        return groundTicks;
    }

    public void incrementGroundTicks() {
        this.groundTicks++;
    }

    public void resetGroundTicks() {
        this.groundTicks = 0;
    }
}

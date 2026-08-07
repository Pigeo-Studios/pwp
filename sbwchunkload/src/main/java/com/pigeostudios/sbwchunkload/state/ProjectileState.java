package com.pigeostudios.sbwchunkload.state;

import com.pigeostudios.sbwchunkload.classifier.ProjectileProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;

/**
 * Состояние отслеживаемого снаряда. Чистое состояние — не знает ни про
 * тикеты, ни про чанк-коридоры: всё, что ему нужно для решений, —
 * позиция/скорость/чанк и профиль поведения.
 *
 * Сущность хранится через WeakReference: трекер не должен удерживать
 * снаряд в памяти после смерти; живость проверяется через isRemoved()
 * на стороне Tracker. Уровень хранится сильной ссылкой — только чтобы
 * освободить тикеты коридора, когда сущность уже собрана GC.
 */
public final class ProjectileState {

    /** Мягкая ссылка на сущность (не держит её в памяти). */
    private final WeakReference<Entity> entityRef;
    private final int entityId;

    /** Уровень, в котором летит снаряд (для освобождения тикетов после GC). */
    private final ServerLevel level;

    private final ProjectileProfile profile;

    private Vec3 lastVelocity;
    /** ChunkPos.toLong() текущего чанка — для smart-update (не изменился = ничего не делать). */
    private long lastChunk;

    /** Текущий коридор: чанки (toLong), покрытые тикетами от имени этого снаряда. */
    private long[] corridor = new long[0];

    /** Точка спавна (для лимита дальности трека). */
    private final Vec3 spawnPos;

    /** Тик спавна (для лимита времени трека). */
    private final int spawnTick;

    /** Подряд идущие обновления с нулевой скоростью (стационарный детект). */
    private int stationaryTicks;

    public ProjectileState(Entity entity, ServerLevel level, ProjectileProfile profile, int tick) {
        this.entityRef = new WeakReference<>(entity);
        this.entityId = entity.getId();
        this.level = level;
        this.profile = profile;
        this.lastVelocity = entity.getDeltaMovement();
        this.lastChunk = entity.chunkPosition().toLong();
        this.spawnPos = entity.position();
        this.spawnTick = tick;
    }

    public WeakReference<Entity> entityRef() {
        return entityRef;
    }

    public int entityId() {
        return entityId;
    }

    /** Уровень, в котором летит снаряд. */
    public ServerLevel level() {
        return level;
    }

    public ProjectileProfile profile() {
        return profile;
    }

    public Vec3 lastVelocity() {
        return lastVelocity;
    }

    public long lastChunk() {
        return lastChunk;
    }

    public long[] corridor() {
        return corridor;
    }

    public void setCorridor(long[] corridor) {
        this.corridor = corridor;
    }

    /** Точка спавна (лимит дальности трека). */
    public Vec3 spawnPos() {
        return spawnPos;
    }

    /** Тик спавна (лимит времени трека). */
    public int spawnTick() {
        return spawnTick;
    }

    /** Подряд идущие обновления с нулевой скоростью. */
    public int stationaryTicks() {
        return stationaryTicks;
    }

    public void setStationaryTicks(int stationaryTicks) {
        this.stationaryTicks = stationaryTicks;
    }

    /** Обновляет скорость/чанк после обработки. */
    public void updateSnapshot(Entity entity) {
        this.lastVelocity = entity.getDeltaMovement();
        this.lastChunk = entity.chunkPosition().toLong();
    }
}

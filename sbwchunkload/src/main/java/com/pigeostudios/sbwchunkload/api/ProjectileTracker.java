package com.pigeostudios.sbwchunkload.api;

import com.pigeostudios.sbwchunkload.classifier.ProjectileClassifier;
import com.pigeostudios.sbwchunkload.classifier.ProjectileProfile;
import com.pigeostudios.sbwchunkload.config.ChunkLoadingConfig;
import com.pigeostudios.sbwchunkload.corridor.ChunkCorridor;
import com.pigeostudios.sbwchunkload.corridor.ChunkCorridorDiff;
import com.pigeostudios.sbwchunkload.corridor.ChunkRasterizer;
import com.pigeostudios.sbwchunkload.scheduler.TimingWheel;
import com.pigeostudios.sbwchunkload.state.ProjectileState;
import com.pigeostudios.sbwchunkload.ticket.ChunkTicketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Публичный API трекинга снарядов. Единственные обязанности:
 * register / unregister / update. Никаких тикетов, никаких instanceof,
 * никакой логики чанков — всё делегируется подсистемам.
 *
 * API для сторонних модов: любой мод может зарегистрировать свои снаряды
 * без миксинов — register(entity) с автоклассификацией или
 * register(entity, profile) с явным профилем.
 *
 * Поток: регистрация приходит из миксина ServerLevel.addFreshEntity (server
 * thread); API публичный — внутренние мапы конкурентные.
 */
@Mod.EventBusSubscriber(modid = "sbwchunkload", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectileTracker {

    /** entityId -> состояние (WeakReference внутри — память не держим). */
    private static final ConcurrentMap<Integer, ProjectileState> TRACKED = new ConcurrentHashMap<>();

    /** Расписание обновлений: 1 бакет в тик, снаряды сами говорят «через N тиков». */
    private static final TimingWheel WHEEL = new TimingWheel();

    private static int cleanupCounter = 0;
    private static long updatesProcessed = 0;
    private static long updatesSkipped = 0;
    private static long removals = 0;
    private static long limitSkips = 0;
    private static long droppedBorder = 0;
    private static long droppedDistance = 0;
    private static long droppedAge = 0;
    private static long droppedStationary = 0;

    /** Причины снятия снаряда с трекинга (для debug). */
    private enum DropCause {
        NONE, REMOVED, BORDER, DISTANCE, AGE, STATIONARY
    }

    private ProjectileTracker() {
    }

    // ===== Публичный API =====

    /** Регистрирует снаряд с автоматической классификацией профиля. */
    public static void register(Entity entity) {
        register(entity, ProjectileClassifier.classify(entity));
    }

    /**
     * Регистрирует снаряд с явным профилем (для сторонних модов, чьи
     * сущности не наследуют известные базовые классы).
     */
    public static void register(Entity entity, ProjectileProfile profile) {
        if (!ChunkLoadingConfig.ENABLED.get()) return;
        if (entity == null || profile == null) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        if (profile == ProjectileProfile.STATIC) return;

        // Защита от спама: лимит одновременно трекаемых снарядов.
        if (TRACKED.size() >= ChunkLoadingConfig.MAX_TRACKED_PROJECTILES.get()) {
            limitSkips++;
            return;
        }

        int tick = serverLevel.getServer().getTickCount();
        ProjectileState state = new ProjectileState(entity, serverLevel, profile, tick);
        TRACKED.put(entity.getId(), state);
        // Немедленный первый коридор: пуля может умереть раньше первого
        // расписания, а ракете прогревать путь вперёд выгодно уже со спавна.
        update(state, serverLevel, tick, true);
        scheduleNext(state, profile, tick);
    }

    /** Снимает снаряд с трекинга (вызывается и при смерти сущности). */
    public static void unregister(Entity entity) {
        if (entity == null) return;
        ProjectileState state = TRACKED.remove(entity.getId());
        if (state != null) {
            releaseCorridor(state, state.level().getServer().getTickCount());
            removals++;
        }
    }

    // ===== Цикл =====

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!ChunkLoadingConfig.ENABLED.get()) {
            if (!TRACKED.isEmpty()) {
                TRACKED.clear();
            }
            return;
        }
        if (TRACKED.isEmpty()) return;

        int tick = event.getServer().getTickCount();

        // Обрабатываем ровно один бакет: только те снаряды, которым пора.
        var bucket = WHEEL.bucketFor(tick);
        ProjectileState state;
        while ((state = bucket.poll()) != null) {
            // Состояние уже снято (перерегистрация/мусорный дубль) — пропуск.
            if (TRACKED.get(state.entityId()) != state) continue;

            Entity entity = state.entityRef().get();
            if (entity == null || entity.isRemoved() || entity.level().isClientSide) {
                drop(state, tick, DropCause.REMOVED);
                continue;
            }
            if (entity.level() instanceof ServerLevel serverLevel) {
                DropCause cause = checkLimits(state, entity, serverLevel, tick);
                if (cause != DropCause.NONE) {
                    drop(state, tick, cause);
                    continue;
                }
                update(state, serverLevel, tick, false);
            }
            scheduleNext(state, state.profile(), tick);
        }

        // Чистка кэша тикетов (спящие записи по TTL).
        if (++cleanupCounter >= 100) {
            cleanupCounter = 0;
            for (ServerLevel level : event.getServer().getAllLevels()) {
                ChunkTicketManager.of(level).cleanup(tick);
            }
        }
    }

    // ===== Внутренняя логика =====

    /**
     * Проверяет лимиты трекинга. Стационарный детект мутирует state
     * (счётчик подряд идущих нулевых скоростей), остальные — read-only.
     */
    private static DropCause checkLimits(ProjectileState state, Entity entity, ServerLevel level, int tick) {
        Vec3 pos = entity.position();

        // World border: снаряд за границей мира — трек не нужен (чанки за
        // границей всё равно не грузятся, снаряд там замирает как раньше).
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(pos))) {
            return DropCause.BORDER;
        }

        // Лимит дальности от точки спавна (по XZ).
        double dx = pos.x - state.spawnPos().x;
        double dz = pos.z - state.spawnPos().z;
        double maxDist = ChunkLoadingConfig.MAX_TRACK_DISTANCE_BLOCKS.get();
        if (dx * dx + dz * dz > maxDist * maxDist) {
            return DropCause.DISTANCE;
        }

        // Лимит времени жизни трека.
        if (tick - state.spawnTick() > ChunkLoadingConfig.MAX_TRACK_AGE_TICKS.get()) {
            return DropCause.AGE;
        }

        // Стационарный детект: лежащие гранаты (M18 дымит 30+ сек), зависшие
        // ракеты и дроны держали бы тикет на один чанк вечно.
        if (entity.getDeltaMovement().lengthSqr() < 0.0001) {
            int stationary = state.stationaryTicks() + 1;
            state.setStationaryTicks(stationary);
            if (stationary >= ChunkLoadingConfig.STATIONARY_THRESHOLD.get()) {
                return DropCause.STATIONARY;
            }
        } else {
            state.setStationaryTicks(0);
        }
        return DropCause.NONE;
    }

    /** Снимает снаряд: тикеты коридора отпускаются, state удаляется. */
    private static void drop(ProjectileState state, int tick, DropCause cause) {
        releaseCorridor(state, tick);
        TRACKED.remove(state.entityId());
        removals++;
        switch (cause) {
            case BORDER -> droppedBorder++;
            case DISTANCE -> droppedDistance++;
            case AGE -> droppedAge++;
            case STATIONARY -> droppedStationary++;
            default -> {
            }
        }
    }

    private static void update(ProjectileState state, ServerLevel level, int tick, boolean force) {
        Entity entity = state.entityRef().get();
        if (entity == null) return;

        long currentChunk = entity.chunkPosition().toLong();
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 position = entity.position();
        double speed = velocity.length();

        ChunkTicketManager ticketManager = ChunkTicketManager.of(level);

        // Smart-update: пересчёт коридора только если что-то реально
        // изменилось. Иначе — простое продление существующих тикетов.
        boolean needsRecalc = force
            || currentChunk != state.lastChunk()
            || !contains(state.corridor(), currentChunk)          // снаряд вылетел из коридора
            || speedChanged(speed, state.lastVelocity().length())
            || directionChanged(state.lastVelocity(), velocity);

        if (!needsRecalc) {
            // Ничего не изменилось: продлить тикеты текущего коридора
            // (дедуп продлений внутри TicketManager, refCount не трогаем).
            for (long chunkKey : state.corridor()) {
                ticketManager.refresh(level, chunkKey, tick);
            }
            updatesSkipped++;
            state.updateSnapshot(entity);
            return;
        }
        updatesProcessed++;

        long[] newCorridor = buildCorridor(state, position, velocity, speed, tick);
        ChunkCorridorDiff diff = ChunkCorridor.diff(state.corridor(), newCorridor);

        for (long chunkKey : diff.removed()) {
            ticketManager.release(chunkKey, tick);
        }
        for (long chunkKey : diff.retained()) {
            ticketManager.refresh(level, chunkKey, tick);
        }
        for (long chunkKey : diff.added()) {
            ticketManager.ensureLoaded(level, chunkKey, tick);
        }

        state.setCorridor(newCorridor);
        state.updateSnapshot(entity);
    }

    /** Коридор от текущей позиции до упреждённой точки. */
    private static long[] buildCorridor(ProjectileState state, Vec3 position, Vec3 velocity,
                                        double speed, int tick) {
        Vec3 target;
        if (speed < 0.01) {
            // Не движется (бомба в первые тики) — держим текущий чанк.
            target = position;
        } else {
            int lookahead = lookahead(state.profile(), speed);
            target = position.add(velocity.normalize().scale(lookahead));
        }
        return ChunkRasterizer.buildCorridor(position, target,
            ChunkLoadingConfig.CORRIDOR_WIDTH.get());
    }

    /**
     * Упреждение по профилю: с adaptiveLookahead — скорость x интервал x
     * predictionMultiplier, зажатое в [minimumLookahead, maximumLookahead];
     * без него — база LOOKAHEAD_BLOCKS (не выше профильного максимума).
     */
    private static int lookahead(ProjectileProfile profile, double speed) {
        int max = profile.maximumLookahead();
        if (!ChunkLoadingConfig.ADAPTIVE_LOOKAHEAD.get()) {
            return Math.min(ChunkLoadingConfig.LOOKAHEAD_BLOCKS.get(), max);
        }
        int adaptive = (int) Math.ceil(speed * profile.refreshInterval()
            * ChunkLoadingConfig.PREDICTION_MULTIPLIER.get());
        return Math.max(profile.minimumLookahead(), Math.min(adaptive, max));
    }

    private static boolean speedChanged(double newSpeed, double oldSpeed) {
        return Math.abs(newSpeed - oldSpeed) / Math.max(0.1, oldSpeed) > 0.10;
    }

    private static boolean directionChanged(Vec3 oldVelocity, Vec3 newVelocity) {
        if (oldVelocity.lengthSqr() < 0.0001 || newVelocity.lengthSqr() < 0.0001) {
            return oldVelocity.lengthSqr() != newVelocity.lengthSqr();
        }
        double cos = oldVelocity.normalize().dot(newVelocity.normalize());
        // Угол > 20 градусов (cos < cos(20°) ~ 0.94)
        return cos < 0.94;
    }

    private static boolean contains(long[] corridor, long chunkKey) {
        for (long key : corridor) {
            if (key == chunkKey) return true;
        }
        return false;
    }

    private static void releaseCorridor(ProjectileState state, int tick) {
        for (long chunkKey : state.corridor()) {
            ChunkTicketManager.of(state.level()).release(chunkKey, tick);
        }
    }

    private static void scheduleNext(ProjectileState state, ProjectileProfile profile, int tick) {
        WHEEL.schedule(state, profile.refreshInterval(), tick);
    }

    // ===== Статистика для debug =====

    public static int trackedCount() {
        return TRACKED.size();
    }

    public static int scheduledCount() {
        return WHEEL.scheduledCount();
    }

    public static long updatesProcessed() {
        return updatesProcessed;
    }

    public static long updatesSkipped() {
        return updatesSkipped;
    }

    public static long removals() {
        return removals;
    }

    public static long limitSkips() {
        return limitSkips;
    }

    public static long droppedBorder() {
        return droppedBorder;
    }

    public static long droppedDistance() {
        return droppedDistance;
    }

    public static long droppedAge() {
        return droppedAge;
    }

    public static long droppedStationary() {
        return droppedStationary;
    }
}

package com.pigeostudios.sbwchunkload.drone;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.pigeostudios.sbwchunkload.config.ChunkLoadingConfig;
import com.pigeostudios.sbwchunkload.corridor.ChunkCorridor;
import com.pigeostudios.sbwchunkload.corridor.ChunkCorridorDiff;
import com.pigeostudios.sbwchunkload.corridor.ChunkRasterizer;
import com.pigeostudios.sbwchunkload.ticket.ChunkTicketManager;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Трекер дронов (v2.1). Отдельный путь от снарядов: дроны — медленные
 * сущности, но без чанк-тикетов сигнальная связь (uncomplicated-fpv
 * FPV/Mavic и любые наследники SBW DroneEntity) «молча умирает» на границе
 * прогруза — дрон замирает в непрогруженном чанке, управление пропадает.
 *
 * Переиспользует тот же ChunkTicketManager (уровень min(3, simulationDistance),
 * refcount, TTL, дедуп, world border) — здесь только правила «когда и куда»:
 * - Трек ПОКА ДРОН В ВОЗДУХЕ: приземлившийся дрон не держит чанки
 *   (анти-абьюз: дрон не может быть бесплатным чанк-лоадером на земле;
 *   приземлённые «тушки» не удерживают прогруз).
 * - Упреждение по скорости движения; при зависшем дроне (скорость ~0) —
 *   по направлению взгляда.
 * - Полный скан уровней раз в SCAN_INTERVAL_TICKS (поиск новых дронов),
 *   обновление трекаемых — раз в drone.interval.
 *
 * Покрывает и fail-safe падение после обрыва сигнала: падающий дрон ещё
 * в воздухе, тикет держит его чанк — дрон не застывает в воздухе навсегда.
 */
@Mod.EventBusSubscriber(modid = "sbwchunkload", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DroneTracker {

    /** Интервал полного скана уровней (поиск новых дронов), в тиках.
     *  20 тиков (1с), а не 100: дрон, снятый с трека по grace (постоял на земле
     *  без управления >5с), после взлёта должен подхватиться БЫСТРО — за 10с
     *  (старое значение) он вылетает за пределы прогруза, его чанк выгружается
     *  и дрон замерзает до прихода скана (инцидент 05.08.2026: «дрон не грузит
     *  чанки» в сценарии «поставил -> отошёл -> вернулся и взлетел»). За 1с
     *  дрон улетает максимум на ~40 блоков — в пределах прогруза игрока (view 6). */
    private static final int SCAN_INTERVAL_TICKS = 20;

    /** Grace для дрона на земле: сколько интервалов (по drone.interval тиков
     *  каждый) дрон может стоять без управления, прежде чем трек снимется.
     *  10 интервалов = 100 тиков = 5с — канальный деплой PWP (дрон стоит на
     *  земле до взлёта) не рвёт трек, а лежащая «тушка» не держит чанки. */
    private static final int GROUND_GRACE_UPDATES = 10;

    /** entityId -> состояние дрона. */
    private static final ConcurrentMap<Integer, DroneTrackState> TRACKED = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static int scanCounter = 0;

    private DroneTracker() {
    }

    /**
     * Мгновенная регистрация дрона (вызывается из миксина addFreshEntity).
     * Без неё новый дрон ждал бы полного скана (раз в 100 тиков), а он летит
     * 20-40 блоков/сек и может вылететь за зону прогруза раньше, чем скан его
     * найдёт — чанк выгрузится и дрон застынет навсегда. Дрон на земле тоже
     * регистрируется — update сам снимет его при первом проходе (onGround).
     */
    public static void register(Entity entity) {
        if (entity == null || !ChunkLoadingConfig.ENABLED.get() || !ChunkLoadingConfig.TRACK_DRONES.get()) return;
        if (!(entity instanceof DroneEntity)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (TRACKED.containsKey(entity.getId())) return;
        if (TRACKED.size() >= ChunkLoadingConfig.MAX_TRACKED_DRONES.get()) return;
        TRACKED.put(entity.getId(), new DroneTrackState(entity, level));
        if (ChunkLoadingConfig.DEBUG.get()) {
            LOGGER.info("[sbwchunkload] drone #{} tracked at {} {} {}", entity.getId(),
                (int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!ChunkLoadingConfig.ENABLED.get() || !ChunkLoadingConfig.TRACK_DRONES.get()) {
            if (!TRACKED.isEmpty()) {
                TRACKED.clear();
            }
            return;
        }

        int tick = event.getServer().getTickCount();
        if (tick % ChunkLoadingConfig.DRONE_INTERVAL.get() != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickLevel(level, tick);
        }
    }

    private static void tickLevel(ServerLevel level, int tick) {
        int interval = ChunkLoadingConfig.DRONE_INTERVAL.get();
        boolean scan = ++scanCounter >= Math.max(1, SCAN_INTERVAL_TICKS / interval);
        if (scan) {
            scanCounter = 0;
            discover(level);
        }

        ChunkTicketManager tickets = ChunkTicketManager.of(level);

        TRACKED.entrySet().removeIf(entry -> {
            DroneTrackState state = entry.getValue();
            if (!state.level().equals(level)) return false;

            Entity entity = level.getEntity(state.entityId());
            if (entity == null || entity.isRemoved() || !(entity instanceof DroneEntity)) {
                releaseCorridor(state, tickets, tick);
                return true;
            }
            // Grace-правило: дрон на земле без управления снимается только после
            // GROUND_GRACE_UPDATES интервалов (см. javadoc класса) — иначе канальный
            // деплой рвёт трек и после взлёта дрон ждёт полный скан (10с) и успевает
            // вылететь за прогруз. Контролируемый дрон трекается всегда.
            if (entity.onGround() && !isControlled(entity)) {
                if (state.groundTicks() >= GROUND_GRACE_UPDATES) {
                    releaseCorridor(state, tickets, tick);
                    return true;
                }
                state.incrementGroundTicks();
            } else {
                state.resetGroundTicks();
            }
            update(state, entity, tickets, tick);
            return false;
        });
    }

    /** Полный скан уровня: новые дроны в воздухе, в пределах лимита. */
    private static void discover(ServerLevel level) {
        int limit = ChunkLoadingConfig.MAX_TRACKED_DRONES.get();
        if (TRACKED.size() >= limit) return;
        for (Entity entity : level.getEntities().getAll()) {
            if (TRACKED.size() >= limit) return;
            if (entity instanceof DroneEntity && !entity.isRemoved() && !entity.onGround()
                && !TRACKED.containsKey(entity.getId())) {
                TRACKED.put(entity.getId(), new DroneTrackState(entity, level));
            }
        }
    }

    /** Smart-update: без смены чанка — простое продление, иначе — diff коридора. */
    private static void update(DroneTrackState state, Entity entity,
                               ChunkTicketManager tickets, int tick) {
        ServerLevel level = state.level();
        Vec3 position = entity.position();
        Vec3 direction = entity.getDeltaMovement();
        double speed = direction.length();
        if (speed < 0.05) {
            direction = entity.getForward();
        } else {
            direction = direction.normalize();
        }
        // Адаптивное упреждение: дрон летит до 2 блоков/тик, интервал обновления
        // drone.interval (10 тиков) -> за интервал пролетает до 20 блоков, а
        // фиксированный lookahead 16 не покрывает пролёт — дрон влетает в
        // непрогретый чанк и на ~10 тиков застывает (UFPV может рвать связь).
        int interval = ChunkLoadingConfig.DRONE_INTERVAL.get();
        int lookahead = Math.max(ChunkLoadingConfig.DRONE_LOOKAHEAD.get(),
            (int) Math.ceil(speed * interval * 1.5));
        Vec3 target = position.add(direction.scale(lookahead));

        long[] newCorridor = ChunkRasterizer.buildCorridor(position, target, 1);
        long[] oldCorridor = state.corridor();

        if (sameCorridor(oldCorridor, newCorridor)) {
            for (long chunkKey : newCorridor) {
                tickets.refresh(level, chunkKey, tick);
            }
            return;
        }

        ChunkCorridorDiff diff = ChunkCorridor.diff(oldCorridor, newCorridor);
        for (long chunkKey : diff.removed()) {
            tickets.release(chunkKey, tick);
        }
        for (long chunkKey : diff.retained()) {
            tickets.refresh(level, chunkKey, tick);
        }
        for (long chunkKey : diff.added()) {
            tickets.ensureLoaded(level, chunkKey, tick);
        }
        state.setCorridor(newCorridor);
    }

    private static boolean sameCorridor(long[] a, long[] b) {
        if (a.length != b.length) return false;
        outer:
        for (long key : a) {
            for (long other : b) {
                if (other == key) continue outer;
            }
            return false;
        }
        return true;
    }

    /** Дрон под активным управлением (монитор привязан, CONTROLLER заполнен). */
    private static boolean isControlled(Entity entity) {
        if (entity instanceof DroneEntity de) {
            String c = de.getEntityData().get(DroneEntity.CONTROLLER);
            return c != null && !c.isEmpty() && !c.equals("undefined") && !c.equals("none");
        }
        return false;
    }

    /** Дрон снят (умер/приземлился/выгрузился): тикеты отпускаются. */
    private static void releaseCorridor(DroneTrackState state, ChunkTicketManager tickets, int tick) {
        for (long chunkKey : state.corridor()) {
            tickets.release(chunkKey, tick);
        }
        state.setCorridor(new long[0]);
    }

    // ===== Статистика для debug =====

    public static int trackedCount() {
        return TRACKED.size();
    }
}

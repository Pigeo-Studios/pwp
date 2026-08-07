package com.pigeostudios.sbwchunkload.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Серверный конфиг (world/serverconfig/sbwchunkload-server.toml).
 *
 * Ключи, существовавшие до рефакторинга 2.0.0, сохранены БЕЗ изменений
 * (enabled, trackPointblankProjectiles, trackSbwProjectiles,
 * smartChunkLoading, adaptiveLookahead, ticketIntervalTicks, lookaheadBlocks) —
 * чтобы прод-серверы не сбросили настройки при обновлении. Новые ключи:
 * профильные интервалы/упреждения, ticketTTL, predictionMultiplier,
 * corridorWidth, debug.
 *
 * scanIntervalTicks и projectileLookaheadBlocks удалены: интервалы обновления
 * и упреждения теперь определяются профилем снаряда (см. ProjectileProfile),
 * а не глобальным сканом.
 */
public class ChunkLoadingConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SPEC;

    /** Мастер-выключатель. */
    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
        .comment("Master toggle: chunk loading for projectiles of supported mods")
        .define("enabled", true);

    /** Трекинг pointblank (FCL-трубы). */
    public static final ForgeConfigSpec.BooleanValue TRACK_POINTBLANK = BUILDER
        .comment(
            "Track PointBlank (FCL) projectile entities: RPG-7V2, RPG-26, M72, AT4,",
            "SMAW, Carl Gustaf rockets etc. (anything implementing ProjectileLike)"
        )
        .define("trackPointblankProjectiles", true);

    /** Трекинг SBW-снарядов. */
    public static final ForgeConfigSpec.BooleanValue TRACK_SBW = BUILDER
        .comment(
            "Track SuperbWarfare projectiles: NURS rockets, guided missiles, bombs,",
            "auto-cannon shells and 7.62/12.7 bullets (incl. FCP/VVP/DragonRise vehicles)"
        )
        .define("trackSbwProjectiles", true);

    /** Smart-режим: тикет только если чанк реально не тикается. */
    public static final ForgeConfigSpec.BooleanValue SMART_CHUNK_LOADING = BUILDER
        .comment(
            "Smart mode: place chunk tickets ONLY when the chunk ahead is not entity-ticking",
            "(isPositionEntityTicking, NOT isLoaded - between view and sim distance chunks are",
            "loaded but entities in them do not tick, projectiles freeze there).",
            "Projectiles flying through already-ticking terrain never touch tickets."
        )
        .define("smartChunkLoading", true);

    /** Адаптивное упреждение по скорости. */
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_LOOKAHEAD = BUILDER
        .comment(
            "Adaptive lookahead: lookahead grows with projectile speed",
            "(speed x refreshInterval x predictionMultiplier), clamped to profile min/max"
        )
        .define("adaptiveLookahead", true);

    /** Дедуп продлений тикета на один чанк. */
    public static final ForgeConfigSpec.IntValue TICKET_INTERVAL_TICKS = BUILDER
        .comment(
            "Dedup: place a ticket on the same chunk at most once per N ticks",
            "(chunk-level, shared ticket value - one record per chunk regardless of projectile count)"
        )
        .defineInRange("ticketIntervalTicks", 2, 1, 20);

    /** База упреждения для «обычных» снарядов (минимум для ракет). */
    public static final ForgeConfigSpec.IntValue LOOKAHEAD_BLOCKS = BUILDER
        .comment(
            "Base lookahead: how far ahead of the projectile the chunk gets loaded (blocks).",
            "With adaptiveLookahead=true this is the minimum for SLOW_LINEAR/BALLISTIC."
        )
        .defineInRange("lookaheadBlocks", 16, 4, 128);

    /** ТТЛ тикет-записи в кэше (тикеты ванили умирают сами ~5 сек без продления). */
    public static final ForgeConfigSpec.IntValue TICKET_TTL_TICKS = BUILDER
        .comment(
            "How long a TicketEntry with refCount=0 stays in the cache before being dropped.",
            "Vanilla region tickets expire by themselves ~5s after the last refresh,",
            "so a dropped entry means the chunk gets unloaded soon anyway."
        )
        .defineInRange("ticketTTLTicks", 200, 40, 1200);

    /** Множитель упреждения: скорость x интервал x predictionMultiplier. */
    public static final ForgeConfigSpec.DoubleValue PREDICTION_MULTIPLIER = BUILDER
        .comment(
            "Prediction multiplier: lookahead = speed x refreshInterval x predictionMultiplier",
            "(when adaptiveLookahead=true), so the projectile never outflies the loaded corridor"
        )
        .defineInRange("predictionMultiplier", 1.5, 1.0, 4.0);

    /** Ширина коридора в чанках (1 = линия, 2 = линия + соседний слой). */
    public static final ForgeConfigSpec.IntValue CORRIDOR_WIDTH = BUILDER
        .comment(
            "Corridor width in chunks: 1 = single-chunk line along the trajectory,",
            "2 = line plus one adjacent chunk layer (helps fast shells that sway)"
        )
        .defineInRange("corridorWidth", 1, 1, 3);

    /** Debug-режим: периодический лог статистики в консоль. */
    public static final ForgeConfigSpec.BooleanValue DEBUG = BUILDER
        .comment("Debug: periodic statistics log + verbose tracking messages")
        .define("debug", false);

    // ===== Защиты (лимиты трекинга) =====

    /** Максимальная дальность трека от точки спавна (блоки, по XZ). */
    public static final ForgeConfigSpec.IntValue MAX_TRACK_DISTANCE_BLOCKS = BUILDER
        .comment(
            "Hard cap: how far from the spawn point one projectile may load chunks (blocks, XZ).",
            "Beyond this the projectile is dropped from tracking and its tickets expire."
        )
        .defineInRange("maxTrackDistanceBlocks", 2000, 256, 8192);

    /** Максимальное время трека (тики). */
    public static final ForgeConfigSpec.IntValue MAX_TRACK_AGE_TICKS = BUILDER
        .comment(
            "Hard cap: how long one projectile may be tracked (ticks), even if it is still",
            "inside the distance cap (drifting drones, circling guided missiles)."
        )
        .defineInRange("maxTrackAgeTicks", 1200, 100, 72000);

    /** Максимум одновременно трекаемых снарядов. */
    public static final ForgeConfigSpec.IntValue MAX_TRACKED_PROJECTILES = BUILDER
        .comment(
            "Hard cap: max concurrently tracked projectiles. New ones are ignored beyond",
            "this (spam protection: machine-gun bursts into the horizon)."
        )
        .defineInRange("maxTrackedProjectiles", 300, 1, 1000);

    /** Максимум активных тикетов (уникальных чанков под тикетом). */
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_TICKETS = BUILDER
        .comment(
            "Hard cap: max chunks held by tickets at once. New tickets are not placed",
            "beyond this (existing ones are still refreshed)."
        )
        .defineInRange("maxActiveTickets", 512, 1, 4096);

    /** Стационарный детект: сколько обновлений подряд скорость ~0, чтобы снять снаряд. */
    public static final ForgeConfigSpec.IntValue STATIONARY_THRESHOLD = BUILDER
        .comment(
            "Stationary drop: after this many consecutive updates with ~zero speed the",
            "projectile is dropped (landed grenades, hovering drones, stuck rockets).",
            "Grenades lie on the ground for 30+ seconds and would hold a ticket forever."
        )
        .defineInRange("stationaryThresholdUpdates", 10, 2, 100);

    // ===== Профильные настройки (FAST_LINEAR = пули, SLOW_LINEAR = ракеты, HEAVY = снаряды) =====

    /** Интервал обновления пуль (тики). */
    public static final ForgeConfigSpec.IntValue BULLET_INTERVAL = BUILDER
        .comment("Bullets (FAST_LINEAR): update interval in ticks - bullets fly ~24 blocks/tick,",
            "so they MUST be updated every tick or they outfly the loaded corridor")
        .defineInRange("bullet.interval", 1, 1, 5);

    /** Минимум упреждения пуль. */
    public static final ForgeConfigSpec.IntValue BULLET_MIN_LOOKAHEAD = BUILDER
        .comment("Bullets: minimum lookahead (blocks)")
        .defineInRange("bullet.minimumLookahead", 24, 8, 128);

    /** Максимум упреждения пуль. */
    public static final ForgeConfigSpec.IntValue BULLET_MAX_LOOKAHEAD = BUILDER
        .comment("Bullets: maximum lookahead (blocks)")
        .defineInRange("bullet.maximumLookahead", 128, 16, 256);

    /** Интервал обновления ракет (тики). */
    public static final ForgeConfigSpec.IntValue ROCKET_INTERVAL = BUILDER
        .comment("Rockets/guided missiles (SLOW_LINEAR): update interval in ticks")
        .defineInRange("rocket.interval", 4, 1, 20);

    /** Минимум упреждения ракет. */
    public static final ForgeConfigSpec.IntValue ROCKET_MIN_LOOKAHEAD = BUILDER
        .comment("Rockets: minimum lookahead (blocks)")
        .defineInRange("rocket.minimumLookahead", 16, 4, 128);

    /** Максимум упреждения ракет. */
    public static final ForgeConfigSpec.IntValue ROCKET_MAX_LOOKAHEAD = BUILDER
        .comment("Rockets: maximum lookahead (blocks)")
        .defineInRange("rocket.maximumLookahead", 96, 16, 256);

    /** Интервал обновления тяжёлых снарядов (тики). */
    public static final ForgeConfigSpec.IntValue HEAVY_INTERVAL = BUILDER
        .comment("Heavy shells/auto-cannon rounds (HEAVY): update interval in ticks")
        .defineInRange("heavy.interval", 2, 1, 10);

    /** Минимум упреждения тяжёлых снарядов. */
    public static final ForgeConfigSpec.IntValue HEAVY_MIN_LOOKAHEAD = BUILDER
        .comment("Heavy shells: minimum lookahead (blocks)")
        .defineInRange("heavy.minimumLookahead", 24, 8, 128);

    /** Максимум упреждения тяжёлых снарядов. */
    public static final ForgeConfigSpec.IntValue HEAVY_MAX_LOOKAHEAD = BUILDER
        .comment("Heavy shells: maximum lookahead (blocks)")
        .defineInRange("heavy.maximumLookahead", 128, 16, 256);

    /** Интервал обновления бомб (тики). */
    public static final ForgeConfigSpec.IntValue BALLISTIC_INTERVAL = BUILDER
        .comment("Bombs/dropped ordnance (BALLISTIC): update interval in ticks")
        .defineInRange("ballistic.interval", 3, 1, 20);

    /** Минимум упреждения бомб. */
    public static final ForgeConfigSpec.IntValue BALLISTIC_MIN_LOOKAHEAD = BUILDER
        .comment("Bombs: minimum lookahead (blocks)")
        .defineInRange("ballistic.minimumLookahead", 16, 4, 128);

    /** Максимум упреждения бомб. */
    public static final ForgeConfigSpec.IntValue BALLISTIC_MAX_LOOKAHEAD = BUILDER
        .comment("Bombs: maximum lookahead (blocks)")
        .defineInRange("ballistic.maximumLookahead", 64, 16, 256);

    // ===== Дроны (v2.1): SBW-совместимые DroneEntity (uncomplicated-fpv FPV/Mavic и т.п.) =====

    /** Трекинг дронов. */
    public static final ForgeConfigSpec.BooleanValue TRACK_DRONES = BUILDER
        .comment(
            "Track SuperbWarfare-compatible drone entities (uncomplicated-fpv FPV/Mavic,",
            "SBW drones): keep their chunks loaded while AIRBORNE so the operator's",
            "signal link does not silently die at the edge of the loaded area."
        )
        .define("trackDrones", true);

    /** Интервал обновления дронов (тики). */
    public static final ForgeConfigSpec.IntValue DRONE_INTERVAL = BUILDER
        .comment("Drones: update interval in ticks - drones are slow entities, 10 ticks is plenty")
        .defineInRange("drone.interval", 10, 2, 40);

    /** Упреждение дрона (блоки). */
    public static final ForgeConfigSpec.IntValue DRONE_LOOKAHEAD = BUILDER
        .comment("Drones: how far ahead of the drone the chunk gets loaded (blocks)")
        .defineInRange("drone.lookahead", 16, 8, 128);

    /** Максимум одновременно трекаемых дронов. */
    public static final ForgeConfigSpec.IntValue MAX_TRACKED_DRONES = BUILDER
        .comment("Hard cap: max concurrently tracked drones (new ones are ignored beyond this)")
        .defineInRange("maxTrackedDrones", 8, 1, 64);

    static {
        SPEC = BUILDER.build();
    }

    private ChunkLoadingConfig() {
    }
}

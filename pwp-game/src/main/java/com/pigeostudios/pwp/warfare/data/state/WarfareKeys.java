package com.pigeostudios.pwp.warfare.data.state;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Реестр ВСЕХ ключей состояния PWP у игроков и сущностей. Строковые ключи {@code "WARFARE_*"}
 * вне этого класса запрещены — только эти константы через {@link WarfareData#of}.
 *
 * <p>Имена совпадают со старыми ключами persistentData: у игроков они при входе переезжают
 * в capability {@link PlayerWarfareData}, у прочих сущностей остаются в persistentData
 * (формат не меняется).
 */
public final class WarfareKeys {
    public static final String PREFIX = "WARFARE_";

    /** Префикс динамических ключей {@link #driveRequestCooldown}. */
    public static final String DRIVE_REQUEST_COOLDOWN_PREFIX = PREFIX + "DriveRequestCooldown_";

    private static final Map<String, WarfareKey<?>> ALL = new LinkedHashMap<>();

    // ===== Киты, отряд, роль =====
    public static final WarfareKey<String> CURRENT_KIT = reg(WarfareKey.string(PREFIX + "CurrentKit"));
    public static final WarfareKey<String> PENDING_KIT = reg(WarfareKey.string(PREFIX + "PendingKit"));
    public static final WarfareKey<CompoundTag> SLOT_SELECTIONS = reg(WarfareKey.compound(PREFIX + "SlotSelections"));
    public static final WarfareKey<Integer> SQUAD_ID = reg(WarfareKey.integer(PREFIX + "SquadID"));
    public static final WarfareKey<Boolean> IS_SQUAD_LEADER = reg(WarfareKey.bool(PREFIX + "IsSquadLeader"));

    // ===== Нок / смерть / тимкилл =====
    public static final WarfareKey<Boolean> IS_DOWNED = reg(WarfareKey.bool(PREFIX + "IsDowned"));
    public static final WarfareKey<Boolean> GIVING_UP = reg(WarfareKey.bool(PREFIX + "GivingUp"));
    public static final WarfareKey<Long> DOWNED_TICK = reg(WarfareKey.longKey(PREFIX + "DownedTick"));
    public static final WarfareKey<Float> DOWNED_YAW = reg(WarfareKey.floatKey(PREFIX + "DownedYaw"));
    public static final WarfareKey<Float> DOWNED_PITCH = reg(WarfareKey.floatKey(PREFIX + "DownedPitch"));
    public static final WarfareKey<String> KNOCKED_BY = reg(WarfareKey.string(PREFIX + "KnockedBy"));
    public static final WarfareKey<Long> LAST_REVIVE_TIME = reg(WarfareKey.longKey(PREFIX + "LastReviveTime"));
    public static final WarfareKey<Long> LAST_MEDIC_SHOUT = reg(WarfareKey.longKey(PREFIX + "LastMedicShout"));
    public static final WarfareKey<Long> LAST_MEDIC_SHOUT_TIME_MS =
            reg(WarfareKey.longKey(PREFIX + "LastMedicShoutTimeMS"));
    public static final WarfareKey<Integer> TEAM_KILLS = reg(WarfareKey.integer(PREFIX + "TeamKills"));
    public static final WarfareKey<Boolean> TEAM_KILL_SPECTATOR = reg(WarfareKey.bool(PREFIX + "TeamKillSpectator"));
    public static final WarfareKey<Long> BASE_RESTRICTED_UNTIL =
            reg(WarfareKey.longKey(PREFIX + "BaseRestrictedUntil"));
    public static final WarfareKey<Integer> TICKET_PENALTY = reg(WarfareKey.integer(PREFIX + "TicketPenalty"));

    // ===== Спавн, мейн-зона, ресапплай =====
    public static final WarfareKey<Long> LAST_RESPAWN_COMMAND = reg(WarfareKey.longKey(PREFIX + "LastRespawnCommand"));
    public static final WarfareKey<Long> LAST_FOB_RESUPPLY = reg(WarfareKey.longKey(PREFIX + "LastFobResupply"));
    public static final WarfareKey<Long> LAST_MAIN_RESUPPLY = reg(WarfareKey.longKey(PREFIX + "LastMainResupply"));
    public static final WarfareKey<Long> LAST_SUPPLY_TIME = reg(WarfareKey.longKey(PREFIX + "LastSupplyTime"));
    public static final WarfareKey<Long> NEXT_SUPPLY_TIME = reg(WarfareKey.longKey(PREFIX + "NextSupplyTime"));
    public static final WarfareKey<Integer> MAIN_ZONE_TIMER = reg(WarfareKey.integer(PREFIX + "MainZoneTimer"));
    public static final WarfareKey<Long> MAIN_ZONE_EXIT_TICK = reg(WarfareKey.longKey(PREFIX + "MainZoneExitTick"));
    public static final WarfareKey<Boolean> IN_MAIN_ZONE = reg(WarfareKey.bool(PREFIX + "InMainZone"));
    public static final WarfareKey<Long> BLOCK_MSG_TICK = reg(WarfareKey.longKey(PREFIX + "BlockMsgTick"));
    public static final WarfareKey<Integer> SUPPLY_AMMO = reg(WarfareKey.integer(PREFIX + "SupplyAmmo"));
    public static final WarfareKey<Integer> CRATE_TIMER = reg(WarfareKey.integer(PREFIX + "CrateTimer"));

    // ===== Техника (сущности техники; часть — у игроков-водителей) =====
    public static final WarfareKey<String> VEHICLE_TEAM = reg(WarfareKey.string(PREFIX + "VehicleTeam"));
    public static final WarfareKey<String> VEHICLE_TYPE = reg(WarfareKey.string(PREFIX + "VehicleType"));
    public static final WarfareKey<Integer> VEHICLE_MATS = reg(WarfareKey.integer(PREFIX + "VehicleMats"));
    public static final WarfareKey<Integer> VEHICLE_MAX_MATS = reg(WarfareKey.integer(PREFIX + "VehicleMaxMats"));
    public static final WarfareKey<Long> SPAWNER_POS = reg(WarfareKey.longKey(PREFIX + "SpawnerPos"));
    public static final WarfareKey<Long> SPAWN_GRACE_TICK = reg(WarfareKey.longKey(PREFIX + "SpawnGraceTick"));
    public static final WarfareKey<Boolean> IS_SUPPLY_TRUCK = reg(WarfareKey.bool(PREFIX + "IsSupplyTruck"));
    public static final WarfareKey<Boolean> FRESH_VEHICLE = reg(WarfareKey.bool(PREFIX + "FreshVehicle"));
    public static final WarfareKey<ListTag> INITIAL_LOADOUT = reg(WarfareKey.compoundList(PREFIX + "InitialLoadout"));
    public static final WarfareKey<String> LAST_DAMAGER = reg(WarfareKey.string(PREFIX + "LastDamager"));
    public static final WarfareKey<Integer> LAST_VEHICLE_ID = reg(WarfareKey.integer(PREFIX + "LastVehicleId"));
    public static final WarfareKey<Integer> REPAIR_TIMER = reg(WarfareKey.integer(PREFIX + "RepairTimer"));
    public static final WarfareKey<Integer> TRUCK_RELOAD_TIMER = reg(WarfareKey.integer(PREFIX + "TruckReloadTimer"));
    public static final WarfareKey<Integer> DRIVE_KICK_TIMER = reg(WarfareKey.integer(PREFIX + "DriveKickTimer"));
    public static final WarfareKey<Integer> BOARDING_GRACE = reg(WarfareKey.integer(PREFIX + "BoardingGrace"));

    // ===== Дроны: канал деплоя/перезарядки игрока (серверный тик) и кулдаун деплоя (мировое время) =====
    public static final WarfareKey<String> DRONE_DEPLOY_TYPE = reg(WarfareKey.string(PREFIX + "DroneDeployType"));
    public static final WarfareKey<Long> DRONE_DEPLOY_START = reg(WarfareKey.longKey(PREFIX + "DroneDeployStart"));
    public static final WarfareKey<Double> DRONE_DEPLOY_X = reg(WarfareKey.doubleKey(PREFIX + "DroneDeployX"));
    public static final WarfareKey<Double> DRONE_DEPLOY_Y = reg(WarfareKey.doubleKey(PREFIX + "DroneDeployY"));
    public static final WarfareKey<Double> DRONE_DEPLOY_Z = reg(WarfareKey.doubleKey(PREFIX + "DroneDeployZ"));
    public static final WarfareKey<Integer> DRONE_DEPLOY_ENTITY = reg(WarfareKey.integer(PREFIX + "DroneDeployEntity"));
    public static final WarfareKey<Long> DRONE_DEPLOY_CD = reg(WarfareKey.longKey(PREFIX + "DroneDeployCd"));
    public static final WarfareKey<String> DRONE_RELOAD_TYPE = reg(WarfareKey.string(PREFIX + "DroneReloadType"));
    public static final WarfareKey<Long> DRONE_RELOAD_START = reg(WarfareKey.longKey(PREFIX + "DroneReloadStart"));
    public static final WarfareKey<Double> DRONE_RELOAD_X = reg(WarfareKey.doubleKey(PREFIX + "DroneReloadX"));
    public static final WarfareKey<Double> DRONE_RELOAD_Y = reg(WarfareKey.doubleKey(PREFIX + "DroneReloadY"));
    public static final WarfareKey<Double> DRONE_RELOAD_Z = reg(WarfareKey.doubleKey(PREFIX + "DroneReloadZ"));
    public static final WarfareKey<Integer> DRONE_RELOAD_ENTITY = reg(WarfareKey.integer(PREFIX + "DroneReloadEntity"));

    // ===== Дроны: метки сущности дрона =====
    public static final WarfareKey<Boolean> DRONE_DEPLOYING = reg(WarfareKey.bool(PREFIX + "DroneDeploying"));
    public static final WarfareKey<String> DRONE_OWNER = reg(WarfareKey.string(PREFIX + "DroneOwner"));
    public static final WarfareKey<Integer> DRONE_IDLE_TICKS = reg(WarfareKey.integer(PREFIX + "DroneIdleTicks"));
    /** Закрепление техники за отрядом (этап 10c): id отряда и его команда; 0/пусто — свободна. */
    public static final WarfareKey<Integer> VEHICLE_CLAIM_SQUAD = reg(WarfareKey.integer(PREFIX + "VehicleClaimSquad"));

    public static final WarfareKey<String> VEHICLE_CLAIM_TEAM = reg(WarfareKey.string(PREFIX + "VehicleClaimTeam"));
    /** Замок «только мой отряд» (этап 17a): чужие не садятся даже пассажирами, пока отряд-владелец жив. */
    public static final WarfareKey<Boolean> VEHICLE_LOCKED = reg(WarfareKey.bool(PREFIX + "VehicleLocked"));

    public static final WarfareKey<Integer> DRONE_FALLBACK_TICKS =
            reg(WarfareKey.integer(PREFIX + "DroneFallbackTicks"));

    private WarfareKeys() {}

    /** Кулдаун повторного запроса на вождение конкретной техники (мировое время, «до»). */
    public static WarfareKey<Long> driveRequestCooldown(int vehicleId) {
        return WarfareKey.longKey(DRIVE_REQUEST_COOLDOWN_PREFIX + vehicleId);
    }

    /** Все статические ключи (для миграции и тестов). */
    public static Map<String, WarfareKey<?>> all() {
        return Collections.unmodifiableMap(ALL);
    }

    private static <T> WarfareKey<T> reg(WarfareKey<T> key) {
        if (ALL.put(key.name(), key) != null) throw new IllegalStateException("Дубль ключа " + key.name());
        return key;
    }
}

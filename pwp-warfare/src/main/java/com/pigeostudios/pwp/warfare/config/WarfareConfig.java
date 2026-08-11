package com.pigeostudios.pwp.warfare.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

// Конфигурация мода
// Содержит все настройки игрового процесса, баланса и клиента
public class WarfareConfig {
   public static final Builder BUILDER = new Builder();
   public static final Builder CLIENT_BUILDER = new Builder();
   public static ForgeConfigSpec SPEC;
   public static ForgeConfigSpec CLIENT_SPEC;
   public static final IntValue COMPASS_SCALE = CLIENT_BUILDER.comment("Scale of the HUD compass (1 = Small, 2 = Normal, 3 = Large)")
      .defineInRange("compassScale", 2, 1, 3);
   public static final BooleanValue LOW_TICKETS_SIREN = BUILDER.comment("Play siren at 50 tickets").define("lowTicketsSiren", true);
   public static final BooleanValue AGS_PROJECTILE_DESTRUCTION = BUILDER.comment("Grenade destruction").define("agsProjectileDestruction", true);
   public static final BooleanValue AMMO_STACK_DESTRUCTION = BUILDER.comment("Ammo explosion destruction").define("ammoStackDestruction", true);
   public static final DoubleValue DIGGING_SPEED_MULTIPLIER = BUILDER.comment("Digging speed multiplier")
      .defineInRange("diggingSpeedMultiplier", 1.0, 0.1, 10.0);
   public static final BooleanValue PREVENT_BLOCK_BREAKING = BUILDER.comment("Prevent players from breaking blocks").define("preventBlockBreaking", true);
   public static final BooleanValue PREVENT_ALL_ITEM_DROPS = BUILDER.comment("Prevent item dropping in survival when game is started")
      .define("preventAllItemDrops", true);
   public static final IntValue HUB_RESUPPLY_COST = BUILDER.comment("Cost for kit resupply").defineInRange("hubResupplyCost", 10, 0, 1000);
   public static final BooleanValue ENABLE_KNOCKOUT = BUILDER.comment("Enable knockout mechanic").define("enableKnockout", true);
    public static final ConfigValue<String> REVIVE_ITEM = BUILDER.comment("Registry name of the item used to revive (e.g. 'pwp_medicine:medkit')")
       .define("reviveItem", "pwp_medicine:medkit");
   public static final IntValue REVIVE_COOLDOWN_SECONDS = BUILDER.comment("Time in seconds where dying again results in instant death")
      .defineInRange("reviveCooldownSeconds", 120, 0, 600);
   public static final BooleanValue HUB_SPAWN_COSTS_MATERIALS = BUILDER.comment("Does spawning at FOB cost materials?")   .define("hubSpawnCostsMaterials", true);
   public static final IntValue HUB_SPAWN_MATERIAL_COST = BUILDER.comment("Material cost to spawn at FOB").defineInRange("hubSpawnMaterialCost", 10, 0, 1000);
   public static final BooleanValue MAIN_SUPPLY_HEALING = BUILDER.comment("Give regeneration near Main Base?").define("mainSupplyHealing", true);
   public static final IntValue MAIN_SUPPLY_HEAL_RADIUS = BUILDER.comment("Radius for Main Base healing").defineInRange("mainSupplyHealRadius", 5, 1, 50);
   public static final BooleanValue ALLOW_BREAKING_DEFENSES = BUILDER.comment(
         "Allow players to break walls and barbed wire even if PREVENT_BLOCK_BREAKING is true"
      )
      .define("allowBreakingDefenses", true);
    public static final BooleanValue AUTO_GIVE_SL_RADIO = BUILDER.comment("Automatically give radio to new Squad Leaders")   .define("autoGiveSlRadio", false);
   public static final BooleanValue HUB_PLACEMENT_REQUIRES_CRATE = BUILDER.comment(
         "Does placing a FOB require a Supply Crate nearby? (Consumes the crate, crate gives 0 mats to FOB)"
      )
      .define("hubPlacementRequiresCrate", true);
   public static final IntValue MAX_DOWNED_TIME_SECONDS = BUILDER.comment("Max time in downed state before bleeding out (seconds)")
      .defineInRange("maxDownedTimeSeconds", 180, 5, 3600);
   public static final IntValue VOTE_AUTO_START_TIME = BUILDER.comment("Time in minutes until game starts automatically during voting")
      .defineInRange("voteAutoStartTime", 10, 1, 60);
   public static final IntValue VOTE_REQUIRED_PERCENTAGE = BUILDER.comment("Percentage of team members needed to be 'Ready'")
      .defineInRange("voteRequiredPercentage", 100, 1, 100);
   public static final BooleanValue PREVENT_VEHICLE_INVENTORY_ACCESS = BUILDER.comment(
         "Completely block access to the vehicle inventory (from the outside via Shift+RMB and from the inside by pressing E)"
      )
      .define("preventVehicleInventoryAccess", true);
   public static final BooleanValue REQUIRE_OFFICER_FOR_SL = BUILDER.comment(
         "If the squad leader does not have the \"Officer\" kit, the squad will disband after 120 seconds."
      )
      .define("requireOfficerForSL", true);
   public static final BooleanValue PREVENT_ENEMY_VEHICLE_ENTRY = BUILDER.comment("Prevent players from entering vehicles claimed by the enemy team")
      .define("preventEnemyVehicleEntry", true);
public static final BooleanValue REQUIRE_SPECIALIST_TO_DRIVE = BUILDER.comment(
          "If true, only Pilots can fly and only Mechanics can drive heavy vehicles. Others are kicked from driver seat after 5 seconds."
       )
       .define("requireSpecialistToDrive", true);
    public static final BooleanValue REQUIRE_SL_PERMISSION_TO_DRIVE = BUILDER.comment(
          "Require Squad Leader permission to drive fresh vehicles. Non-SL players with correct kit must request access."
       )
       .define("requireSlPermissionToDrive", true);
    public static final IntValue RESPAWN_COMMAND_COOLDOWN_SECONDS = BUILDER.comment("Cooldown for /pwp respawn command in seconds")
       .defineInRange("respawnCommandCooldown", 240, 0, 3600);
    public static final IntValue ART_STRIKE_COOLDOWN_MINUTES = BUILDER.comment("Cooldown for Artillery Strike in minutes")
       .defineInRange("artStrikeCooldownMinutes", 30, 1, 120);
   public static final IntValue ART_STRIKE_RADIUS = BUILDER.comment("Artillery strike impact radius").defineInRange("artStrikeRadius", 20, 5, 100);
   public static final IntValue MIN_HUB_DISTANCE = BUILDER.defineInRange("minHubDistance", 150, 0, 10000);
   public static final IntValue MIN_RALLY_POINT_DISTANCE = BUILDER.defineInRange("minRallyPointDistance", 150, 0, 10000);
   public static final IntValue MAX_HUBS_PER_TEAM = BUILDER.defineInRange("maxHubsPerTeam", 8, 1, 100);
   public static final IntValue HUB_BLOCK_RADIUS = BUILDER.defineInRange("hubBlockRadius", 40, 5, 200);
   public static final IntValue RALLY_BLOCK_RADIUS = BUILDER.defineInRange("rallyBlockRadius", 40, 5, 200);
   public static final IntValue HUB_SOUND_RADIUS = BUILDER.defineInRange("hubSoundRadius", 15, 1, 128);
   public static final IntValue HUB_BUILD_RADIUS = BUILDER.defineInRange("hubBuildRadius", 50, 10, 200);
   public static final IntValue CRATE_BUILD_RADIUS = BUILDER.defineInRange("crateBuildRadius", 50, 5, 200);
   public static final IntValue HUB_BLOCK_ENEMY_COUNT = BUILDER.defineInRange("hubBlockEnemyCount", 3, 1, 20);
   public static final IntValue RALLY_BLOCK_ENEMY_COUNT = BUILDER.defineInRange("rallyBlockEnemyCount", 1, 1, 20);
   public static final IntValue SUPPLY_TRUCK_CRATES = BUILDER.comment("Max supply crates in a truck").defineInRange("supplyTruckCrates", 2, 1, 20);
   public static final IntValue SUPPLY_CRATE_MATERIALS = BUILDER.comment("Materials per dropped supply crate")
      .defineInRange("supplyCrateMaterials", 50, 10, 1000);
    public static final ConfigValue<String> BLUE_TEAM_CUSTOM_NAME = BUILDER.define("blueTeamCustomName", "BLUEFOR");
    public static final ConfigValue<String> RED_TEAM_CUSTOM_NAME = BUILDER.define("redTeamCustomName", "REDFOR");

    // ===== Дроны (деплой каналом, uncomplicated-fpv) =====

    public static final IntValue DRONE_DEPLOY_TIME_TICKS = BUILDER.comment("Drone deployment channel time (ticks)")
        .defineInRange("droneDeployTimeTicks", 80, 20, 400);
    public static final IntValue DRONE_DEPLOY_CD_TICKS = BUILDER.comment(
            "Drone deployment cooldown (ticks). Counted from the START of the deployment,",
            "so losing the drone never extends it - the class stays playable."
        )
        .defineInRange("droneDeployCooldownTicks", 900, 100, 7200);
    public static final IntValue DRONE_DEPLOY_MAX_RANGE = BUILDER.comment(
            "Max horizontal distance (blocks) from the player to pull the drone placement",
            "back on a far click (accidental clicks at reach edge must not spawn the drone",
            "beyond the stand-still threshold and instantly cancel, burning the cooldown).",
            "Must stay below ~2.5 (the stand-still cancel threshold) to prevent instant cancel."
        )
        .defineInRange("droneDeployMaxRange", 2, 1, 2);
    public static final IntValue DRONE_IDLE_DESPAWN_TICKS = BUILDER.comment(
            "A drone that landed and has no active control link despawns after N ticks",
            "(abandoned drones must not litter the map or hold the active limit)"
        )
        .defineInRange("droneIdleDespawnTicks", 1200, 40, 2400);
    public static final IntValue DRONE_MAX_ACTIVE_PER_PLAYER = BUILDER.comment("Max active drones per player (total)")
        .defineInRange("droneMaxActivePerPlayer", 2, 1, 8);
    public static final IntValue DRONE_MAX_ACTIVE_PER_TYPE = BUILDER.comment("Max active drones of the SAME type per player (e.g. 1 FPV + 1 Mavic)")
        .defineInRange("droneMaxActivePerType", 1, 1, 4);
    public static final IntValue DRONE_RELOAD_TIME_TICKS = BUILDER.comment(
            "Mavic grenade reload channel time (ticks) via drone_ammo_pouch"
        )
        .defineInRange("droneReloadTimeTicks", 40, 10, 200);

    // ===== РЭБ (uncomplicated-fpv reb / reb_mini через стройку) =====

    public static final IntValue REB_BUILD_TIME_TICKS = BUILDER.comment("REB construction progress (ticks of digging, ~25s at 1/s)")
        .defineInRange("rebBuildTimeTicks", 500, 100, 3600);
    public static final IntValue REB_MINI_BUILD_TIME_TICKS = BUILDER.comment("Mini REB construction progress (ticks of digging)")
        .defineInRange("rebMiniBuildTimeTicks", 250, 50, 3600);
    public static final IntValue REB_BUILD_COST = BUILDER.comment("Materials cost to build an REB")
        .defineInRange("rebBuildCost", 150, 10, 1000);
    public static final IntValue REB_MINI_BUILD_COST = BUILDER.comment("Materials cost to build a mini REB")
        .defineInRange("rebMiniBuildCost", 60, 10, 1000);
    public static final IntValue REB_MAX_PER_TEAM = BUILDER.comment("Max REB/mini-REB entities per team (anti-spam)")
        .defineInRange("rebMaxPerTeam", 3, 1, 20);

   static {
      BUILDER.push("Gameplay Settings");
      BUILDER.pop();
      BUILDER.push("Balance Settings");
      BUILDER.pop();
      BUILDER.push("Faction Settings");
      BUILDER.pop();
      CLIENT_BUILDER.push("Client Visuals");
      CLIENT_BUILDER.pop();
      SPEC = BUILDER.build();
      CLIENT_SPEC = CLIENT_BUILDER.build();
   }
}

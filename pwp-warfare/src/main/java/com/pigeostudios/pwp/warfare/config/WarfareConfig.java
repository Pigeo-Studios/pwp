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
    public static final BooleanValue AUTO_GIVE_WALKIETALKIE = BUILDER.comment("Automatically give walkietalkie to all players on match servers")
       .define("autoGiveWalkieTalkie", false);
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

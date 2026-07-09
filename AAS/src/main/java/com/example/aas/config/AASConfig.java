/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.ForgeConfigSpec
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$Builder
 *  net.minecraftforge.common.ForgeConfigSpec$ConfigValue
 *  net.minecraftforge.common.ForgeConfigSpec$DoubleValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package com.example.aas.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class AASConfig {
    static final public ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    static final public ForgeConfigSpec SPEC;
    static final public ForgeConfigSpec.Builder CLIENT_BUILDER;
    static final public ForgeConfigSpec CLIENT_SPEC;
    static final public ForgeConfigSpec.IntValue COMPASS_SCALE;
    static final public ForgeConfigSpec.BooleanValue LOW_TICKETS_SIREN;
    static final public ForgeConfigSpec.BooleanValue AGS_PROJECTILE_DESTRUCTION;
    static final public ForgeConfigSpec.BooleanValue AMMO_STACK_DESTRUCTION;
    static final public ForgeConfigSpec.DoubleValue DIGGING_SPEED_MULTIPLIER;
    static final public ForgeConfigSpec.BooleanValue PREVENT_BLOCK_BREAKING;
    static final public ForgeConfigSpec.BooleanValue PREVENT_ALL_ITEM_DROPS;
    static final public ForgeConfigSpec.IntValue HUB_RESUPPLY_COST;
    static final public ForgeConfigSpec.BooleanValue ENABLE_KNOCKOUT;
    static final public ForgeConfigSpec.ConfigValue<String> REVIVE_ITEM;
    static final public ForgeConfigSpec.IntValue REVIVE_COOLDOWN_SECONDS;
    static final public ForgeConfigSpec.BooleanValue HUB_SPAWN_COSTS_MATERIALS;
    static final public ForgeConfigSpec.IntValue HUB_SPAWN_MATERIAL_COST;
    static final public ForgeConfigSpec.BooleanValue MAIN_SUPPLY_HEALING;
    static final public ForgeConfigSpec.IntValue MAIN_SUPPLY_HEAL_RADIUS;
    static final public ForgeConfigSpec.BooleanValue ALLOW_BREAKING_DEFENSES;
    static final public ForgeConfigSpec.BooleanValue AUTO_GIVE_SL_RADIO;
    static final public ForgeConfigSpec.BooleanValue HUB_PLACEMENT_REQUIRES_CRATE;
    static final public ForgeConfigSpec.IntValue MAX_DOWNED_TIME_SECONDS;
    static final public ForgeConfigSpec.IntValue VOTE_AUTO_START_TIME;
    static final public ForgeConfigSpec.IntValue VOTE_REQUIRED_PERCENTAGE;
    static final public ForgeConfigSpec.BooleanValue PREVENT_VEHICLE_INVENTORY_ACCESS;
    static final public ForgeConfigSpec.BooleanValue REQUIRE_OFFICER_FOR_SL;
    static final public ForgeConfigSpec.BooleanValue PREVENT_ENEMY_VEHICLE_ENTRY;
    static final public ForgeConfigSpec.BooleanValue REQUIRE_SPECIALIST_TO_DRIVE;
    static final public ForgeConfigSpec.IntValue ART_STRIKE_COOLDOWN_MINUTES;
    static final public ForgeConfigSpec.IntValue ART_STRIKE_RADIUS;
    static final public ForgeConfigSpec.IntValue MIN_HUB_DISTANCE;
    static final public ForgeConfigSpec.IntValue MIN_RALLY_POINT_DISTANCE;
    static final public ForgeConfigSpec.IntValue MAX_HUBS_PER_TEAM;
    static final public ForgeConfigSpec.IntValue HUB_BLOCK_RADIUS;
    static final public ForgeConfigSpec.IntValue RALLY_BLOCK_RADIUS;
    static final public ForgeConfigSpec.IntValue HUB_SOUND_RADIUS;
    static final public ForgeConfigSpec.IntValue HUB_BUILD_RADIUS;
    static final public ForgeConfigSpec.IntValue CRATE_BUILD_RADIUS;
    static final public ForgeConfigSpec.IntValue HUB_BLOCK_ENEMY_COUNT;
    static final public ForgeConfigSpec.IntValue RALLY_BLOCK_ENEMY_COUNT;
    static final public ForgeConfigSpec.IntValue SUPPLY_TRUCK_CRATES;
    static final public ForgeConfigSpec.IntValue SUPPLY_CRATE_MATERIALS;
    static final public ForgeConfigSpec.ConfigValue<String> BLUE_TEAM_CUSTOM_NAME;
    static final public ForgeConfigSpec.ConfigValue<String> RED_TEAM_CUSTOM_NAME;

    static {
        CLIENT_BUILDER = new ForgeConfigSpec.Builder();
        BUILDER.push("Gameplay Settings");
        AGS_PROJECTILE_DESTRUCTION = BUILDER.comment("Grenade destruction").define("agsProjectileDestruction", true);
        AMMO_STACK_DESTRUCTION = BUILDER.comment("Ammo explosion destruction").define("ammoStackDestruction", true);
        DIGGING_SPEED_MULTIPLIER = BUILDER.comment("Digging speed multiplier").defineInRange("diggingSpeedMultiplier", 1.0, 0.1, 10.0);
        PREVENT_BLOCK_BREAKING = BUILDER.comment("Prevent players from breaking blocks").define("preventBlockBreaking", false);
        ALLOW_BREAKING_DEFENSES = BUILDER.comment("Allow players to break walls and barbed wire even if PREVENT_BLOCK_BREAKING is true").define("allowBreakingDefenses", true);
        PREVENT_ALL_ITEM_DROPS = BUILDER.comment("Prevent item dropping in survival when game is started").define("preventAllItemDrops", false);
        HUB_RESUPPLY_COST = BUILDER.comment("Cost for kit resupply").defineInRange("hubResupplyCost", 10, 0, 1000);
        HUB_PLACEMENT_REQUIRES_CRATE = BUILDER.comment("Does placing a FOB require a Supply Crate nearby? (Consumes the crate, crate gives 0 mats to FOB)").define("hubPlacementRequiresCrate", false);
        HUB_SPAWN_COSTS_MATERIALS = BUILDER.comment("Does spawning at FOB cost materials?").define("hubSpawnCostsMaterials", false);
        HUB_SPAWN_MATERIAL_COST = BUILDER.comment("Material cost to spawn at FOB").defineInRange("hubSpawnMaterialCost", 10, 0, 1000);
        MAIN_SUPPLY_HEALING = BUILDER.comment("Give regeneration near Main Base?").define("mainSupplyHealing", true);
        MAIN_SUPPLY_HEAL_RADIUS = BUILDER.comment("Radius for Main Base healing").defineInRange("mainSupplyHealRadius", 5, 1, 50);
        AUTO_GIVE_SL_RADIO = BUILDER.comment("Automatically give radio to new Squad Leaders").define("autoGiveSlRadio", false);
        LOW_TICKETS_SIREN = BUILDER.comment("Play siren at 50 tickets").define("lowTicketsSiren", true);
        VOTE_AUTO_START_TIME = BUILDER.comment("Time in minutes until game starts automatically during voting").defineInRange("voteAutoStartTime", 10, 1, 60);
        VOTE_REQUIRED_PERCENTAGE = BUILDER.comment("Percentage of team members needed to be 'Ready'").defineInRange("voteRequiredPercentage", 100, 1, 100);
        ENABLE_KNOCKOUT = BUILDER.comment("Enable knockout mechanic").define("enableKnockout", true);
        REVIVE_ITEM = BUILDER.comment("Registry name of the item used to revive (e.g. 'minecraft:paper')").define("reviveItem", (Object)"minecraft:paper");
        REVIVE_COOLDOWN_SECONDS = BUILDER.comment("Time in seconds where dying again results in instant death").defineInRange("reviveCooldownSeconds", 120, 0, 600);
        MAX_DOWNED_TIME_SECONDS = BUILDER.comment("Max time in downed state before bleeding out (seconds)").defineInRange("maxDownedTimeSeconds", 180, 5, 3600);
        PREVENT_VEHICLE_INVENTORY_ACCESS = BUILDER.comment("Completely block access to the vehicle inventory (from the outside via Shift+RMB and from the inside by pressing E)").define("preventVehicleInventoryAccess", true);
        REQUIRE_OFFICER_FOR_SL = BUILDER.comment("If the squad leader does not have the \"Officer\" kit, the squad will disband after 120 seconds.").define("requireOfficerForSL", false);
        PREVENT_ENEMY_VEHICLE_ENTRY = BUILDER.comment("Prevent players from entering vehicles claimed by the enemy team").define("preventEnemyVehicleEntry", true);
        REQUIRE_SPECIALIST_TO_DRIVE = BUILDER.comment("If true, only Pilots can fly and only Mechanics can drive heavy vehicles. Others are kicked from driver seat after 5 seconds.").define("requireSpecialistToDrive", false);
        BUILDER.pop();
        BUILDER.push("Balance Settings");
        MIN_HUB_DISTANCE = BUILDER.defineInRange("minHubDistance", 150, 0, 10000);
        MIN_RALLY_POINT_DISTANCE = BUILDER.defineInRange("minRallyPointDistance", 150, 0, 10000);
        MAX_HUBS_PER_TEAM = BUILDER.defineInRange("maxHubsPerTeam", 8, 1, 100);
        HUB_BLOCK_RADIUS = BUILDER.defineInRange("hubBlockRadius", 40, 5, 200);
        RALLY_BLOCK_RADIUS = BUILDER.defineInRange("rallyBlockRadius", 40, 5, 200);
        HUB_SOUND_RADIUS = BUILDER.defineInRange("hubSoundRadius", 15, 1, 128);
        HUB_BUILD_RADIUS = BUILDER.defineInRange("hubBuildRadius", 50, 10, 200);
        CRATE_BUILD_RADIUS = BUILDER.defineInRange("crateBuildRadius", 50, 5, 200);
        HUB_BLOCK_ENEMY_COUNT = BUILDER.defineInRange("hubBlockEnemyCount", 3, 1, 20);
        RALLY_BLOCK_ENEMY_COUNT = BUILDER.defineInRange("rallyBlockEnemyCount", 1, 1, 20);
        SUPPLY_TRUCK_CRATES = BUILDER.comment("Max supply crates in a truck").defineInRange("supplyTruckCrates", 2, 1, 20);
        SUPPLY_CRATE_MATERIALS = BUILDER.comment("Materials per dropped supply crate").defineInRange("supplyCrateMaterials", 50, 10, 1000);
        ART_STRIKE_COOLDOWN_MINUTES = BUILDER.comment("Cooldown for Artillery Strike in minutes").defineInRange("artStrikeCooldownMinutes", 30, 1, 120);
        ART_STRIKE_RADIUS = BUILDER.comment("Artillery strike impact radius").defineInRange("artStrikeRadius", 20, 5, 100);
        BUILDER.pop();
        BUILDER.push("Faction Settings");
        BLUE_TEAM_CUSTOM_NAME = BUILDER.define("blueTeamCustomName", (Object)"BLUEFOR");
        RED_TEAM_CUSTOM_NAME = BUILDER.define("redTeamCustomName", (Object)"REDFOR");
        BUILDER.pop();
        CLIENT_BUILDER.push("Client Visuals");
        COMPASS_SCALE = CLIENT_BUILDER.comment("Scale of the HUD compass (1 = Small, 2 = Normal, 3 = Large)").defineInRange("compassScale", 2, 1, 3);
        CLIENT_BUILDER.pop();
        CLIENT_SPEC = CLIENT_BUILDER.build();
        SPEC = BUILDER.build();
    }
}


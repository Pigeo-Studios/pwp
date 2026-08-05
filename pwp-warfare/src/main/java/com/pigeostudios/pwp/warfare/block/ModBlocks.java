package com.pigeostudios.pwp.warfare.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Реестр всех блоков мода PWP: Warfare
// Содержит регистрацию блоков и их сущностей (BlockEntity)
public class ModBlocks {
   // Реестр блоков
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "pwpwarfare");
   // Реестр сущностей блоков (BlockEntity)
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "pwpwarfare");
   public static final RegistryObject<Block> AMMO_BAG_BLOCK = BLOCKS.register("ammo_bag", AmmoBagBlock::new);
   public static final RegistryObject<Block> GAME_START_TRIGGER = BLOCKS.register("game_start_trigger", GameStartTriggerBlock::new);
   public static final RegistryObject<Block> BLUE_RALLY_BLOCK = BLOCKS.register("blue_rally", RallyPointBlock::new);
   public static final RegistryObject<Block> RED_RALLY_BLOCK = BLOCKS.register("red_rally", RallyPointBlock::new);
   public static final RegistryObject<Block> HUB_BLOCK = BLOCKS.register("hub_block", HubBlock::new);
   public static final RegistryObject<Block> BARBED_WIRE_BLOCK = BLOCKS.register("barbed_wire", BarbedWireBlock::new);
   public static final RegistryObject<Block> WALL_BLOCK = BLOCKS.register("wall_block", WallBlock::new);
   public static final RegistryObject<Block> WALL_SLAB_BLOCK = BLOCKS.register("wall_slab_block", WallSlabBlock::new);
   public static final RegistryObject<Block> CAMO_NET_BLOCK = BLOCKS.register("camo_net", CamoNetBlock::new);
   public static final RegistryObject<Block> M2_CONSTRUCTION_BLOCK = BLOCKS.register("m2_construction", M2ConstructionBlock::new);
   public static final RegistryObject<Block> AGS_CONSTRUCTION_BLOCK = BLOCKS.register("ags_construction", AGSConstructionBlock::new);
   public static final RegistryObject<Block> MAIN_SUPPLY_BLOCK = BLOCKS.register("main_supply", MainSupplyBlock::new);
   public static final RegistryObject<Block> VEHICLE_STATION_BLOCK = BLOCKS.register("vehicle_station", VehicleStationBlock::new);
   public static final RegistryObject<Block> SUPPLY_CRATE_VISUAL = BLOCKS.register(
      "crate_dropped", () -> new Block(Properties.of().mapColor(MapColor.METAL).noOcclusion())
   );
   public static final RegistryObject<Block> MORTAR_CONSTRUCTION_BLOCK = BLOCKS.register("mortar_construction", MortarConstructionBlock::new);
   public static final RegistryObject<Block> TOW_CONSTRUCTION_BLOCK = BLOCKS.register("tow_construction", TOWConstructionBlock::new);
   public static final RegistryObject<Block> REB_CONSTRUCTION_BLOCK = BLOCKS.register("reb_construction", RebConstructionBlock::new);
   public static final RegistryObject<Block> AGS_AMMO_STACK_BLOCK = BLOCKS.register("ags_ammo_stack", AGSAmmoStackBlock::new);
   public static final RegistryObject<Block> M2_AMMO_STACK_BLOCK = BLOCKS.register("m2_ammo_stack", M2AmmoStackBlock::new);
   public static final RegistryObject<Block> MORTAR_SHELL_STACK_BLOCK = BLOCKS.register("mortar_shell_stack", MortarShellStackBlock::new);
   public static final RegistryObject<Block> TOW_MISSILE_STACK_BLOCK = BLOCKS.register("tow_missile_stack", TOWMissileStackBlock::new);
   public static final RegistryObject<BlockEntityType<RallyPointBlockEntity>> RALLY_BE = BLOCK_ENTITIES.register(
      "rally_be", () -> Builder.of(RallyPointBlockEntity::new, new Block[]{(Block)BLUE_RALLY_BLOCK.get(), (Block)RED_RALLY_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<HubBlockEntity>> HUB_BE = BLOCK_ENTITIES.register(
      "hub_be", () -> Builder.of(HubBlockEntity::new, new Block[]{(Block)HUB_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<BarbedWireBlockEntity>> WIRE_BE = BLOCK_ENTITIES.register(
      "wire_be", () -> Builder.of(BarbedWireBlockEntity::new, new Block[]{(Block)BARBED_WIRE_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<AGSConstructionBlockEntity>> AGS_CONSTRUCTION_BE = BLOCK_ENTITIES.register(
      "ags_construction_be", () -> Builder.of(AGSConstructionBlockEntity::new, new Block[]{(Block)AGS_CONSTRUCTION_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<M2ConstructionBlockEntity>> M2_CONSTRUCTION_BE = BLOCK_ENTITIES.register(
      "m2_construction_be", () -> Builder.of(M2ConstructionBlockEntity::new, new Block[]{(Block)M2_CONSTRUCTION_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<WallBlockEntity>> WALL_BE = BLOCK_ENTITIES.register(
      "wall_be", () -> Builder.of(WallBlockEntity::new, new Block[]{(Block)WALL_BLOCK.get(), (Block)WALL_SLAB_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<MainSupplyBlockEntity>> MAIN_SUPPLY_BE = BLOCK_ENTITIES.register(
      "main_supply_be", () -> Builder.of(MainSupplyBlockEntity::new, new Block[]{(Block)MAIN_SUPPLY_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<VehicleStationBlockEntity>> VEHICLE_STATION_BE = BLOCK_ENTITIES.register(
      "vehicle_station_be", () -> Builder.of(VehicleStationBlockEntity::new, new Block[]{(Block)VEHICLE_STATION_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<MortarConstructionBlockEntity>> MORTAR_CONSTRUCTION_BE = BLOCK_ENTITIES.register(
      "mortar_construction_be", () -> Builder.of(MortarConstructionBlockEntity::new, new Block[]{(Block)MORTAR_CONSTRUCTION_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<TOWConstructionBlockEntity>> TOW_CONSTRUCTION_BE = BLOCK_ENTITIES.register(
      "tow_construction_be", () -> Builder.of(TOWConstructionBlockEntity::new, new Block[]{(Block)TOW_CONSTRUCTION_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<RebConstructionBlockEntity>> REB_CONSTRUCTION_BE = BLOCK_ENTITIES.register(
      "reb_construction_be", () -> Builder.of(RebConstructionBlockEntity::new, new Block[]{(Block)REB_CONSTRUCTION_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<Block> VEHICLE_SPAWNER_BLOCK = BLOCKS.register("vehicle_spawner", VehicleSpawnerBlock::new);
   public static final RegistryObject<BlockEntityType<VehicleSpawnerBlockEntity>> VEHICLE_SPAWNER_BE = BLOCK_ENTITIES.register(
      "vehicle_spawner", () -> Builder.of(VehicleSpawnerBlockEntity::new, new Block[]{(Block)VEHICLE_SPAWNER_BLOCK.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<AmmoStackBlockEntity>> AMMO_STACK_BE = BLOCK_ENTITIES.register(
      "ammo_stack_be",
      () -> Builder.of(AmmoStackBlockEntity::new, new Block[]{(Block)AGS_AMMO_STACK_BLOCK.get(), (Block)M2_AMMO_STACK_BLOCK.get()}).build(null)
   );

   // Регистрирует все блоки и сущности блоков в шине событий
   public static void register(IEventBus bus) {
      BLOCKS.register(bus);
      BLOCK_ENTITIES.register(bus);
   }
}

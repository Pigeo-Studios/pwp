/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.RegistryObject
 */
package com.example.aas.block;

import com.example.aas.block.AGSAmmoStackBlock;
import com.example.aas.block.AGSConstructionBlock;
import com.example.aas.block.AGSConstructionBlockEntity;
import com.example.aas.block.AmmoBagBlock;
import com.example.aas.block.AmmoStackBlockEntity;
import com.example.aas.block.BarbedWireBlock;
import com.example.aas.block.BarbedWireBlockEntity;
import com.example.aas.block.GameStartTriggerBlock;
import com.example.aas.block.HubBlock;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.M2AmmoStackBlock;
import com.example.aas.block.M2ConstructionBlock;
import com.example.aas.block.M2ConstructionBlockEntity;
import com.example.aas.block.MainSupplyBlock;
import com.example.aas.block.MainSupplyBlockEntity;
import com.example.aas.block.MortarConstructionBlock;
import com.example.aas.block.MortarConstructionBlockEntity;
import com.example.aas.block.MortarShellStackBlock;
import com.example.aas.block.RallyPointBlock;
import com.example.aas.block.RallyPointBlockEntity;
import com.example.aas.block.TOWConstructionBlock;
import com.example.aas.block.TOWConstructionBlockEntity;
import com.example.aas.block.TOWMissileStackBlock;
import com.example.aas.block.VehicleSpawnerBlock;
import com.example.aas.block.VehicleSpawnerBlockEntity;
import com.example.aas.block.WallBlock;
import com.example.aas.block.WallBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    static final public DeferredRegister<Block> BLOCKS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCKS, (String)"aas");
    static final public DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCK_ENTITY_TYPES, (String)"aas");
    static final public RegistryObject<Block> AMMO_BAG_BLOCK = BLOCKS.register("ammo_bag", AmmoBagBlock::new);
    static final public RegistryObject<Block> GAME_START_TRIGGER = BLOCKS.register("game_start_trigger", GameStartTriggerBlock::new);
    static final public RegistryObject<Block> BLUE_RALLY_BLOCK = BLOCKS.register("blue_rally", RallyPointBlock::new);
    static final public RegistryObject<Block> RED_RALLY_BLOCK = BLOCKS.register("red_rally", RallyPointBlock::new);
    static final public RegistryObject<Block> HUB_BLOCK = BLOCKS.register("hub_block", HubBlock::new);
    static final public RegistryObject<Block> BARBED_WIRE_BLOCK = BLOCKS.register("barbed_wire", BarbedWireBlock::new);
    static final public RegistryObject<Block> WALL_BLOCK = BLOCKS.register("wall_block", WallBlock::new);
    static final public RegistryObject<Block> M2_CONSTRUCTION_BLOCK = BLOCKS.register("m2_construction", M2ConstructionBlock::new);
    static final public RegistryObject<Block> AGS_CONSTRUCTION_BLOCK = BLOCKS.register("ags_construction", AGSConstructionBlock::new);
    static final public RegistryObject<Block> MAIN_SUPPLY_BLOCK = BLOCKS.register("main_supply", MainSupplyBlock::new);
    static final public RegistryObject<Block> SUPPLY_CRATE_VISUAL = BLOCKS.register("crate_dropped", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noOcclusion()));
    static final public RegistryObject<Block> MORTAR_CONSTRUCTION_BLOCK = BLOCKS.register("mortar_construction", MortarConstructionBlock::new);
    static final public RegistryObject<Block> TOW_CONSTRUCTION_BLOCK = BLOCKS.register("tow_construction", TOWConstructionBlock::new);
    static final public RegistryObject<Block> AGS_AMMO_STACK_BLOCK = BLOCKS.register("ags_ammo_stack", AGSAmmoStackBlock::new);
    static final public RegistryObject<Block> M2_AMMO_STACK_BLOCK = BLOCKS.register("m2_ammo_stack", M2AmmoStackBlock::new);
    static final public RegistryObject<Block> MORTAR_SHELL_STACK_BLOCK = BLOCKS.register("mortar_shell_stack", MortarShellStackBlock::new);
    static final public RegistryObject<Block> TOW_MISSILE_STACK_BLOCK = BLOCKS.register("tow_missile_stack", TOWMissileStackBlock::new);
    static final public RegistryObject<BlockEntityType<RallyPointBlockEntity>> RALLY_BE = BLOCK_ENTITIES.register("rally_be", () -> BlockEntityType.Builder.of(RallyPointBlockEntity::new, (Block[])new Block[]{(Block)BLUE_RALLY_BLOCK.get(), (Block)RED_RALLY_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<HubBlockEntity>> HUB_BE = BLOCK_ENTITIES.register("hub_be", () -> BlockEntityType.Builder.of(HubBlockEntity::new, (Block[])new Block[]{(Block)HUB_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<BarbedWireBlockEntity>> WIRE_BE = BLOCK_ENTITIES.register("wire_be", () -> BlockEntityType.Builder.of(BarbedWireBlockEntity::new, (Block[])new Block[]{(Block)BARBED_WIRE_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<AGSConstructionBlockEntity>> AGS_CONSTRUCTION_BE = BLOCK_ENTITIES.register("ags_construction_be", () -> BlockEntityType.Builder.of(AGSConstructionBlockEntity::new, (Block[])new Block[]{(Block)AGS_CONSTRUCTION_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<M2ConstructionBlockEntity>> M2_CONSTRUCTION_BE = BLOCK_ENTITIES.register("m2_construction_be", () -> BlockEntityType.Builder.of(M2ConstructionBlockEntity::new, (Block[])new Block[]{(Block)M2_CONSTRUCTION_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<WallBlockEntity>> WALL_BE = BLOCK_ENTITIES.register("wall_be", () -> BlockEntityType.Builder.of(WallBlockEntity::new, (Block[])new Block[]{(Block)WALL_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<MainSupplyBlockEntity>> MAIN_SUPPLY_BE = BLOCK_ENTITIES.register("main_supply_be", () -> BlockEntityType.Builder.of(MainSupplyBlockEntity::new, (Block[])new Block[]{(Block)MAIN_SUPPLY_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<MortarConstructionBlockEntity>> MORTAR_CONSTRUCTION_BE = BLOCK_ENTITIES.register("mortar_construction_be", () -> BlockEntityType.Builder.of(MortarConstructionBlockEntity::new, (Block[])new Block[]{(Block)MORTAR_CONSTRUCTION_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<TOWConstructionBlockEntity>> TOW_CONSTRUCTION_BE = BLOCK_ENTITIES.register("tow_construction_be", () -> BlockEntityType.Builder.of(TOWConstructionBlockEntity::new, (Block[])new Block[]{(Block)TOW_CONSTRUCTION_BLOCK.get()}).build(null));
    static final public RegistryObject<Block> VEHICLE_SPAWNER_BLOCK = BLOCKS.register("vehicle_spawner", VehicleSpawnerBlock::new);
    static final public RegistryObject<BlockEntityType<VehicleSpawnerBlockEntity>> VEHICLE_SPAWNER_BE = BLOCK_ENTITIES.register("vehicle_spawner", () -> BlockEntityType.Builder.of(VehicleSpawnerBlockEntity::new, (Block[])new Block[]{(Block)VEHICLE_SPAWNER_BLOCK.get()}).build(null));
    static final public RegistryObject<BlockEntityType<AmmoStackBlockEntity>> AMMO_STACK_BE = BLOCK_ENTITIES.register("ammo_stack_be", () -> BlockEntityType.Builder.of(AmmoStackBlockEntity::new, (Block[])new Block[]{(Block)AGS_AMMO_STACK_BLOCK.get(), (Block)M2_AMMO_STACK_BLOCK.get()}).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}


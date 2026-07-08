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
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCKS, (String)"aas");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCK_ENTITY_TYPES, (String)"aas");
    public static final RegistryObject<Block> AMMO_BAG_BLOCK = BLOCKS.register("ammo_bag", AmmoBagBlock::new);
    public static final RegistryObject<Block> GAME_START_TRIGGER = BLOCKS.register("game_start_trigger", GameStartTriggerBlock::new);
    public static final RegistryObject<Block> BLUE_RALLY_BLOCK = BLOCKS.register("blue_rally", RallyPointBlock::new);
    public static final RegistryObject<Block> RED_RALLY_BLOCK = BLOCKS.register("red_rally", RallyPointBlock::new);
    public static final RegistryObject<Block> HUB_BLOCK = BLOCKS.register("hub_block", HubBlock::new);
    public static final RegistryObject<Block> BARBED_WIRE_BLOCK = BLOCKS.register("barbed_wire", BarbedWireBlock::new);
    public static final RegistryObject<Block> WALL_BLOCK = BLOCKS.register("wall_block", WallBlock::new);
    public static final RegistryObject<Block> M2_CONSTRUCTION_BLOCK = BLOCKS.register("m2_construction", M2ConstructionBlock::new);
    public static final RegistryObject<Block> AGS_CONSTRUCTION_BLOCK = BLOCKS.register("ags_construction", AGSConstructionBlock::new);
    public static final RegistryObject<Block> MAIN_SUPPLY_BLOCK = BLOCKS.register("main_supply", MainSupplyBlock::new);
    public static final RegistryObject<Block> SUPPLY_CRATE_VISUAL = BLOCKS.register("crate_dropped", () -> new Block(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60955_()));
    public static final RegistryObject<Block> MORTAR_CONSTRUCTION_BLOCK = BLOCKS.register("mortar_construction", MortarConstructionBlock::new);
    public static final RegistryObject<Block> TOW_CONSTRUCTION_BLOCK = BLOCKS.register("tow_construction", TOWConstructionBlock::new);
    public static final RegistryObject<Block> AGS_AMMO_STACK_BLOCK = BLOCKS.register("ags_ammo_stack", AGSAmmoStackBlock::new);
    public static final RegistryObject<Block> M2_AMMO_STACK_BLOCK = BLOCKS.register("m2_ammo_stack", M2AmmoStackBlock::new);
    public static final RegistryObject<Block> MORTAR_SHELL_STACK_BLOCK = BLOCKS.register("mortar_shell_stack", MortarShellStackBlock::new);
    public static final RegistryObject<Block> TOW_MISSILE_STACK_BLOCK = BLOCKS.register("tow_missile_stack", TOWMissileStackBlock::new);
    public static final RegistryObject<BlockEntityType<RallyPointBlockEntity>> RALLY_BE = BLOCK_ENTITIES.register("rally_be", () -> BlockEntityType.Builder.m_155273_(RallyPointBlockEntity::new, (Block[])new Block[]{(Block)BLUE_RALLY_BLOCK.get(), (Block)RED_RALLY_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<HubBlockEntity>> HUB_BE = BLOCK_ENTITIES.register("hub_be", () -> BlockEntityType.Builder.m_155273_(HubBlockEntity::new, (Block[])new Block[]{(Block)HUB_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<BarbedWireBlockEntity>> WIRE_BE = BLOCK_ENTITIES.register("wire_be", () -> BlockEntityType.Builder.m_155273_(BarbedWireBlockEntity::new, (Block[])new Block[]{(Block)BARBED_WIRE_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<AGSConstructionBlockEntity>> AGS_CONSTRUCTION_BE = BLOCK_ENTITIES.register("ags_construction_be", () -> BlockEntityType.Builder.m_155273_(AGSConstructionBlockEntity::new, (Block[])new Block[]{(Block)AGS_CONSTRUCTION_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<M2ConstructionBlockEntity>> M2_CONSTRUCTION_BE = BLOCK_ENTITIES.register("m2_construction_be", () -> BlockEntityType.Builder.m_155273_(M2ConstructionBlockEntity::new, (Block[])new Block[]{(Block)M2_CONSTRUCTION_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<WallBlockEntity>> WALL_BE = BLOCK_ENTITIES.register("wall_be", () -> BlockEntityType.Builder.m_155273_(WallBlockEntity::new, (Block[])new Block[]{(Block)WALL_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<MainSupplyBlockEntity>> MAIN_SUPPLY_BE = BLOCK_ENTITIES.register("main_supply_be", () -> BlockEntityType.Builder.m_155273_(MainSupplyBlockEntity::new, (Block[])new Block[]{(Block)MAIN_SUPPLY_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<MortarConstructionBlockEntity>> MORTAR_CONSTRUCTION_BE = BLOCK_ENTITIES.register("mortar_construction_be", () -> BlockEntityType.Builder.m_155273_(MortarConstructionBlockEntity::new, (Block[])new Block[]{(Block)MORTAR_CONSTRUCTION_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<TOWConstructionBlockEntity>> TOW_CONSTRUCTION_BE = BLOCK_ENTITIES.register("tow_construction_be", () -> BlockEntityType.Builder.m_155273_(TOWConstructionBlockEntity::new, (Block[])new Block[]{(Block)TOW_CONSTRUCTION_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<Block> VEHICLE_SPAWNER_BLOCK = BLOCKS.register("vehicle_spawner", VehicleSpawnerBlock::new);
    public static final RegistryObject<BlockEntityType<VehicleSpawnerBlockEntity>> VEHICLE_SPAWNER_BE = BLOCK_ENTITIES.register("vehicle_spawner", () -> BlockEntityType.Builder.m_155273_(VehicleSpawnerBlockEntity::new, (Block[])new Block[]{(Block)VEHICLE_SPAWNER_BLOCK.get()}).m_58966_(null));
    public static final RegistryObject<BlockEntityType<AmmoStackBlockEntity>> AMMO_STACK_BE = BLOCK_ENTITIES.register("ammo_stack_be", () -> BlockEntityType.Builder.m_155273_(AmmoStackBlockEntity::new, (Block[])new Block[]{(Block)AGS_AMMO_STACK_BLOCK.get(), (Block)M2_AMMO_STACK_BLOCK.get()}).m_58966_(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}


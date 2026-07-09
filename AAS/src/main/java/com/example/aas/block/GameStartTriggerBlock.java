/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 */
package com.example.aas.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class GameStartTriggerBlock
extends Block {
    static final public BooleanProperty POWERED = BlockStateProperties.POWERED;

    public GameStartTriggerBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f));
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)POWERED, (Comparable)Boolean.valueOf(false)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{POWERED});
    }

    public boolean isSignalSource(BlockState state) {
        return true;
    }

    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return (Boolean)state.getValue((Property)POWERED) != false ? 15 : 0;
    }

    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return (Boolean)state.getValue((Property)POWERED) != false ? 15 : 0;
    }

    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (((Boolean)state.getValue((Property)POWERED)).booleanValue()) {
            level.setBlock(pos, (BlockState)state.setValue((Property)POWERED, (Comparable)Boolean.valueOf(false)), 3);
        }
    }
}


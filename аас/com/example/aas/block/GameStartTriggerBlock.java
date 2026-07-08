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
    public static final BooleanProperty POWERED = BlockStateProperties.f_61448_;

    public GameStartTriggerBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_60978_(2.0f));
        this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)POWERED, (Comparable)Boolean.valueOf(false)));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{POWERED});
    }

    public boolean m_7899_(BlockState state) {
        return true;
    }

    public int m_6378_(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return (Boolean)state.m_61143_((Property)POWERED) != false ? 15 : 0;
    }

    public int m_6376_(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return (Boolean)state.m_61143_((Property)POWERED) != false ? 15 : 0;
    }

    public void m_213897_(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (((Boolean)state.m_61143_((Property)POWERED)).booleanValue()) {
            level.m_7731_(pos, (BlockState)state.m_61124_((Property)POWERED, (Comparable)Boolean.valueOf(false)), 3);
        }
    }
}


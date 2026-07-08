/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.material.MapColor
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.MainSupplyBlockEntity;
import com.example.aas.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public class MainSupplyBlock
extends BaseEntityBlock {
    public MainSupplyBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60913_(-1.0f, 3600000.0f).m_60955_());
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new MainSupplyBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return MainSupplyBlock.m_152132_(type, (BlockEntityType)((BlockEntityType)ModBlocks.MAIN_SUPPLY_BE.get()), MainSupplyBlockEntity::tick);
    }
}


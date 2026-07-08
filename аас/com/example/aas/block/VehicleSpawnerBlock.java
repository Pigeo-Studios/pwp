/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraftforge.network.NetworkHooks
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import com.example.aas.block.VehicleSpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class VehicleSpawnerBlock
extends BaseEntityBlock {
    public VehicleSpawnerBlock() {
        super(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50286_).m_60913_(-1.0f, 3600000.0f));
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new VehicleSpawnerBlockEntity(pos, state);
    }

    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.f_46443_) {
            if (player.m_7500_()) {
                BlockEntity be = level.m_7702_(pos);
                if (be instanceof VehicleSpawnerBlockEntity) {
                    VehicleSpawnerBlockEntity spawner = (VehicleSpawnerBlockEntity)be;
                    NetworkHooks.openScreen((ServerPlayer)((ServerPlayer)player), (MenuProvider)spawner, (BlockPos)pos);
                }
            } else {
                player.m_5661_((Component)Component.m_237113_((String)"Access Denied: Creative Mode Only"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.f_46443_) {
            return null;
        }
        return VehicleSpawnerBlock.m_152132_(type, (BlockEntityType)((BlockEntityType)ModBlocks.VEHICLE_SPAWNER_BE.get()), VehicleSpawnerBlockEntity::tick);
    }
}


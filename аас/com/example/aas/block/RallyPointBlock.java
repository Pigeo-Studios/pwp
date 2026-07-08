/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.minecraftforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import com.example.aas.block.RallyPointBlockEntity;
import com.example.aas.events.GameLogicEvents;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.world.AASWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class RallyPointBlock
extends BaseEntityBlock {
    public static final VoxelShape SHAPE = Shapes.m_83144_();

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return RallyPointBlock.m_152132_(type, (BlockEntityType)((BlockEntityType)ModBlocks.RALLY_BE.get()), (lvl, pos, st, be) -> {
            if (lvl.f_46443_) {
                be.handleSoundClient();
            } else {
                be.checkExpiry(lvl, pos);
            }
        });
    }

    public RallyPointBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_60978_(1.0f).m_60955_());
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.m_83040_();
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new RallyPointBlockEntity(pos, state);
    }

    public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be;
        if (!level.f_46443_ && (be = level.m_7702_(pos)) instanceof RallyPointBlockEntity) {
            RallyPointBlockEntity rallyBe = (RallyPointBlockEntity)be;
            String playerTeam = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
            String rallyTeam = "NEUTRAL";
            if (state.m_60713_((Block)ModBlocks.BLUE_RALLY_BLOCK.get())) {
                rallyTeam = "BLUE";
            } else if (state.m_60713_((Block)ModBlocks.RED_RALLY_BLOCK.get())) {
                rallyTeam = "RED";
            }
            if (rallyTeam.equals(playerTeam)) {
                rallyBe.wasDismantled = true;
                rallyBe.m_6596_();
                ServerLevel serverLevel = (ServerLevel)level;
                AASWorldData data = AASWorldData.get(serverLevel);
                int penalty = 10;
                if (rallyTeam.equals("BLUE")) {
                    data.blueTickets = Math.max(0, data.blueTickets - penalty);
                    this.broadcastMessage(serverLevel, "BLUE player dismantled Friendly Rally Point! (-10 Tickets)", ChatFormatting.BLUE);
                } else if (rallyTeam.equals("RED")) {
                    data.redTickets = Math.max(0, data.redTickets - penalty);
                    this.broadcastMessage(serverLevel, "RED player dismantled Friendly Rally Point! (-10 Tickets)", ChatFormatting.RED);
                }
                int squadId = rallyBe.getSquadId();
                if (squadId != -1) {
                    for (AASWorldData.Squad s : data.squads) {
                        if (s.id != squadId) continue;
                        s.nextRallyAvailableTick = -1L;
                        break;
                    }
                }
                data.m_77762_();
                PacketHandler.sendToAllClients(serverLevel, data);
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)level).m_46472_()), (Object)new PacketSyncSquads(data.squads));
            }
        }
        super.m_5707_(level, pos, state, player);
    }

    public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be;
        if (!state.m_60713_(newState.m_60734_()) && (be = level.m_7702_(pos)) instanceof RallyPointBlockEntity) {
            RallyPointBlockEntity rallyBe = (RallyPointBlockEntity)be;
            if (!level.m_5776_()) {
                ServerLevel serverLevel = (ServerLevel)level;
                AASWorldData data = AASWorldData.get(serverLevel);
                if (!rallyBe.isDecay && !rallyBe.wasDismantled) {
                    int penalty = 20;
                    if (state.m_60734_() == ModBlocks.BLUE_RALLY_BLOCK.get()) {
                        data.blueTickets = Math.max(0, data.blueTickets - penalty);
                        this.broadcastMessage(serverLevel, "BLUE Rally Point Destroyed! (-" + penalty + ")", ChatFormatting.BLUE);
                    } else if (state.m_60734_() == ModBlocks.RED_RALLY_BLOCK.get()) {
                        data.redTickets = Math.max(0, data.redTickets - penalty);
                        this.broadcastMessage(serverLevel, "RED Rally Point Destroyed! (-" + penalty + ")", ChatFormatting.RED);
                    }
                    GameLogicEvents.checkSirenManual(serverLevel, data);
                    GameLogicEvents.checkGameOver(serverLevel, data);
                }
                rallyBe.cleanupData(serverLevel);
                data.m_77762_();
            }
        }
        super.m_6810_(state, level, pos, newState, isMoving);
    }

    private void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
        level.m_7654_().m_6846_().m_240416_((Component)Component.m_237113_((String)text).m_130940_(color), false);
    }
}


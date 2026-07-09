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
    static final public VoxelShape SHAPE = Shapes.block();

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return RallyPointBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)ModBlocks.RALLY_BE.get()), (lvl, pos, st, be) -> {
            if (lvl.isClientSide) {
                be.handleSoundClient();
            } else {
                be.checkExpiry(lvl, pos);
            }
        });
    }

    public RallyPointBlock() {
        super(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion());
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RallyPointBlockEntity(pos, state);
    }

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be;
        if (!level.isClientSide && (be = level.getBlockEntity(pos)) instanceof RallyPointBlockEntity) {
            RallyPointBlockEntity rallyBe = (RallyPointBlockEntity)be;
            String playerTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            String rallyTeam = "NEUTRAL";
            if (state.is((Block)ModBlocks.BLUE_RALLY_BLOCK.get())) {
                rallyTeam = "BLUE";
            } else if (state.is((Block)ModBlocks.RED_RALLY_BLOCK.get())) {
                rallyTeam = "RED";
            }
            if (rallyTeam.equals(playerTeam)) {
                rallyBe.wasDismantled = true;
                rallyBe.setChanged();
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
                data.setDirty();
                PacketHandler.sendToAllClients(serverLevel, data);
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)level).dimension()), (Object)new PacketSyncSquads(data.squads));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be;
        if (!state.is(newState.getBlock()) && (be = level.getBlockEntity(pos)) instanceof RallyPointBlockEntity) {
            RallyPointBlockEntity rallyBe = (RallyPointBlockEntity)be;
            if (!level.isClientSide()) {
                ServerLevel serverLevel = (ServerLevel)level;
                AASWorldData data = AASWorldData.get(serverLevel);
                if (!rallyBe.isDecay && !rallyBe.wasDismantled) {
                    int penalty = 20;
                    if (state.getBlock() == ModBlocks.BLUE_RALLY_BLOCK.get()) {
                        data.blueTickets = Math.max(0, data.blueTickets - penalty);
                        this.broadcastMessage(serverLevel, "BLUE Rally Point Destroyed! (-" + penalty + ")", ChatFormatting.BLUE);
                    } else if (state.getBlock() == ModBlocks.RED_RALLY_BLOCK.get()) {
                        data.redTickets = Math.max(0, data.redTickets - penalty);
                        this.broadcastMessage(serverLevel, "RED Rally Point Destroyed! (-" + penalty + ")", ChatFormatting.RED);
                    }
                    GameLogicEvents.checkSirenManual(serverLevel, data);
                    GameLogicEvents.checkGameOver(serverLevel, data);
                }
                rallyBe.cleanupData(serverLevel);
                data.setDirty();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
        level.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)text).withStyle(color), false);
    }
}


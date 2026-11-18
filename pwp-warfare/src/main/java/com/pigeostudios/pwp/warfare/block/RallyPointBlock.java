package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RallyPointBlock extends BaseEntityBlock {
    public static final VoxelShape SHAPE = Shapes.block();

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, (BlockEntityType) ModBlocks.RALLY_BE.get(), (lvl, pos, st, be) -> {
            RallyPointBlockEntity rbe = (RallyPointBlockEntity) be;
            if (lvl.isClientSide) {
                rbe.handleSoundClient();
            } else {
                rbe.checkExpiry(lvl, pos);
            }
        });
    }

    public RallyPointBlock() {
        super(Properties.of().strength(1.0F).noOcclusion());
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RallyPointBlockEntity(pos, state);
    }

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RallyPointBlockEntity rallyBe) {
            String breakerTeam =
                    player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            String rallyTeam = "NEUTRAL";
            if (state.getBlock() == ModBlocks.BLUE_RALLY_BLOCK.get()) {
                rallyTeam = "BLUE";
            } else if (state.getBlock() == ModBlocks.RED_RALLY_BLOCK.get()) {
                rallyTeam = "RED";
            }
            if (rallyTeam.equals(breakerTeam)) {
                rallyBe.wasDismantled = true;
                rallyBe.setChanged();
                ServerLevel serverLevel = (ServerLevel) level;
                WarfareWorldData data = WarfareWorldData.get(serverLevel);
                int penalty = 10;
                if (rallyTeam.equals("BLUE")) {
                    data.match.blueTickets = Math.max(0, data.match.blueTickets - penalty);
                    this.broadcastMessage(
                            serverLevel,
                            Component.translatable("pwpwarfare.msg.rally.dismantled.blue", 10),
                            ChatFormatting.BLUE);
                } else if (rallyTeam.equals("RED")) {
                    data.match.redTickets = Math.max(0, data.match.redTickets - penalty);
                    this.broadcastMessage(
                            serverLevel,
                            Component.translatable("pwpwarfare.msg.rally.dismantled.red", 10),
                            ChatFormatting.RED);
                }
                int squadId = rallyBe.getSquadId();
                if (squadId != -1) {
                    for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
                        if (s.id == squadId) {
                            s.nextRallyAvailableTick = -1L;
                            break;
                        }
                    }
                }
                data.setDirty();
                PacketHandler.sendToAllClients(serverLevel, data);
                PacketHandler.syncSquadsAll(data.squadRegistry.squads);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof RallyPointBlockEntity rallyBe
                && !level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            WarfareWorldData data = WarfareWorldData.get(serverLevel);
            if (!rallyBe.isDecay && !rallyBe.wasDismantled) {
                int penalty = 20;
                if (state.getBlock() == ModBlocks.BLUE_RALLY_BLOCK.get()) {
                    data.match.blueTickets = Math.max(0, data.match.blueTickets - penalty);
                    this.broadcastMessage(
                            serverLevel,
                            Component.translatable("pwpwarfare.msg.rally.destroyed.blue", penalty),
                            ChatFormatting.BLUE);
                } else if (state.getBlock() == ModBlocks.RED_RALLY_BLOCK.get()) {
                    data.match.redTickets = Math.max(0, data.match.redTickets - penalty);
                    this.broadcastMessage(
                            serverLevel,
                            Component.translatable("pwpwarfare.msg.rally.destroyed.red", penalty),
                            ChatFormatting.RED);
                }
                GameLogicEvents.checkSirenManual(serverLevel, data);
                GameLogicEvents.checkGameOver(serverLevel, data);
            }
            rallyBe.cleanupData(serverLevel);
            data.setDirty();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void broadcastMessage(ServerLevel level, Component text, ChatFormatting color) {
        level.getServer().getPlayerList().broadcastSystemMessage(text.copy().withStyle(color), false);
    }
}

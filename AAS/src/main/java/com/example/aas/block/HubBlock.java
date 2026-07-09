/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Explosion
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.ModBlocks;
import com.example.aas.client.ClientHooks;
import com.example.aas.events.GameLogicEvents;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class HubBlock
extends BaseEntityBlock {
    static final public BooleanProperty CONSTRUCTED = BooleanProperty.create((String)"constructed");

    public HubBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 9.0f).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)CONSTRUCTED, (Comparable)Boolean.valueOf(false)));
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be;
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (((Boolean)state.getValue((Property)CONSTRUCTED)).booleanValue() && (be = level.getBlockEntity(pos)) instanceof HubBlockEntity) {
            HubBlockEntity hub = (HubBlockEntity)be;
            if (!player.isCreative()) {
                String hubTeam;
                String playerTeam = "NEUTRAL";
                if (player.getTeam() != null) {
                    playerTeam = player.getTeam().getName();
                }
                if (!(hubTeam = hub.getTeam()).equals("NEUTRAL") && !playerTeam.equalsIgnoreCase(hubTeam)) {
                    if (level.isClientSide) {
                        player.displayClientMessage((Component)Component.literal((String)"Cannot access ENEMY Hub!").withStyle(ChatFormatting.RED), true);
                    }
                    return InteractionResult.FAIL;
                }
            }
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.openHubMenu(pos));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be;
        if (!level.isClientSide && ((Boolean)state.getValue((Property)CONSTRUCTED)).booleanValue() && (be = level.getBlockEntity(pos)) instanceof HubBlockEntity) {
            String playerTeam;
            HubBlockEntity hub = (HubBlockEntity)be;
            String hubTeam = hub.getTeam();
            String string = playerTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
            if (hubTeam.equalsIgnoreCase(playerTeam)) {
                hub.wasDismantled = true;
                hub.setChanged();
                ServerLevel serverLevel = (ServerLevel)level;
                AASWorldData data = AASWorldData.get(serverLevel);
                int penalty = 10;
                if (hubTeam.equalsIgnoreCase("BLUE")) {
                    data.blueTickets = Math.max(0, data.blueTickets - penalty);
                    this.broadcastMessage(serverLevel, "BLUE player dismantled Friendly HUB! (-10 Tickets)", ChatFormatting.BLUE);
                } else if (hubTeam.equalsIgnoreCase("RED")) {
                    data.redTickets = Math.max(0, data.redTickets - penalty);
                    this.broadcastMessage(serverLevel, "RED player dismantled Friendly HUB! (-10 Tickets)", ChatFormatting.RED);
                }
                data.setDirty();
                PacketHandler.sendToAllClients(serverLevel, data);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be;
        if (!state.is(newState.getBlock()) && (be = level.getBlockEntity(pos)) instanceof HubBlockEntity) {
            HubBlockEntity hub = (HubBlockEntity)be;
            if (!level.isClientSide) {
                ServerLevel serverLevel = (ServerLevel)level;
                AASWorldData data = AASWorldData.get(serverLevel);
                data.hubs.removeIf(h -> h.pos.equals((Object)pos));
                if (((Boolean)state.getValue((Property)CONSTRUCTED)).booleanValue()) {
                    String hubTeam = hub.getTeam();
                    if (!hub.wasDismantled && !hubTeam.equals("NEUTRAL")) {
                        int penalty = 30;
                        if (hubTeam.equalsIgnoreCase("BLUE")) {
                            data.blueTickets = Math.max(0, data.blueTickets - penalty);
                            this.broadcastMessage(serverLevel, "BLUE HUB Destroyed! (-30 Tickets)", ChatFormatting.BLUE);
                        } else if (hubTeam.equalsIgnoreCase("RED")) {
                            data.redTickets = Math.max(0, data.redTickets - penalty);
                            this.broadcastMessage(serverLevel, "RED HUB Destroyed! (-30 Tickets)", ChatFormatting.RED);
                        }
                    }
                }
                GameLogicEvents.checkSirenManual(serverLevel, data);
                GameLogicEvents.checkGameOver(serverLevel, data);
                data.setDirty();
                PacketHandler.sendToAllClients(serverLevel, data);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
        level.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)text).withStyle(color), false);
    }

    public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
        return (Boolean)state.getValue((Property)CONSTRUCTED) != false ? 3.0f : 0.3f;
    }

    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return 9.0f;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{CONSTRUCTED});
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return (Boolean)state.getValue((Property)CONSTRUCTED) == false;
    }

    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return (Boolean)state.getValue((Property)CONSTRUCTED) != false ? 0.2f : 1.0f;
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (Boolean)state.getValue((Property)CONSTRUCTED) == false ? Shapes.empty() : Shapes.block();
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HubBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return HubBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)ModBlocks.HUB_BE.get()), HubBlockEntity::tick);
    }
}


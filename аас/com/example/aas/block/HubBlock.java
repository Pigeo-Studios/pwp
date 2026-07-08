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
    public static final BooleanProperty CONSTRUCTED = BooleanProperty.m_61465_((String)"constructed");

    public HubBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60913_(3.0f, 9.0f).m_60955_());
        this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)CONSTRUCTED, (Comparable)Boolean.valueOf(false)));
    }

    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be;
        if (!player.m_21120_(hand).m_41619_()) {
            return InteractionResult.PASS;
        }
        if (((Boolean)state.m_61143_((Property)CONSTRUCTED)).booleanValue() && (be = level.m_7702_(pos)) instanceof HubBlockEntity) {
            HubBlockEntity hub = (HubBlockEntity)be;
            if (!player.m_7500_()) {
                String hubTeam;
                String playerTeam = "NEUTRAL";
                if (player.m_5647_() != null) {
                    playerTeam = player.m_5647_().m_5758_();
                }
                if (!(hubTeam = hub.getTeam()).equals("NEUTRAL") && !playerTeam.equalsIgnoreCase(hubTeam)) {
                    if (level.f_46443_) {
                        player.m_5661_((Component)Component.m_237113_((String)"Cannot access ENEMY Hub!").m_130940_(ChatFormatting.RED), true);
                    }
                    return InteractionResult.FAIL;
                }
            }
            if (level.f_46443_) {
                DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.openHubMenu(pos));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be;
        if (!level.f_46443_ && ((Boolean)state.m_61143_((Property)CONSTRUCTED)).booleanValue() && (be = level.m_7702_(pos)) instanceof HubBlockEntity) {
            String playerTeam;
            HubBlockEntity hub = (HubBlockEntity)be;
            String hubTeam = hub.getTeam();
            String string = playerTeam = player.m_5647_() != null ? player.m_5647_().m_5758_() : "NEUTRAL";
            if (hubTeam.equalsIgnoreCase(playerTeam)) {
                hub.wasDismantled = true;
                hub.m_6596_();
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
                data.m_77762_();
                PacketHandler.sendToAllClients(serverLevel, data);
            }
        }
        super.m_5707_(level, pos, state, player);
    }

    public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be;
        if (!state.m_60713_(newState.m_60734_()) && (be = level.m_7702_(pos)) instanceof HubBlockEntity) {
            HubBlockEntity hub = (HubBlockEntity)be;
            if (!level.f_46443_) {
                ServerLevel serverLevel = (ServerLevel)level;
                AASWorldData data = AASWorldData.get(serverLevel);
                data.hubs.removeIf(h -> h.pos.equals((Object)pos));
                if (((Boolean)state.m_61143_((Property)CONSTRUCTED)).booleanValue()) {
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
                data.m_77762_();
                PacketHandler.sendToAllClients(serverLevel, data);
            }
        }
        super.m_6810_(state, level, pos, newState, isMoving);
    }

    private void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
        level.m_7654_().m_6846_().m_240416_((Component)Component.m_237113_((String)text).m_130940_(color), false);
    }

    public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
        return (Boolean)state.m_61143_((Property)CONSTRUCTED) != false ? 3.0f : 0.3f;
    }

    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return 9.0f;
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{CONSTRUCTED});
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    public boolean m_7420_(BlockState state, BlockGetter reader, BlockPos pos) {
        return (Boolean)state.m_61143_((Property)CONSTRUCTED) == false;
    }

    public float m_7749_(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return (Boolean)state.m_61143_((Property)CONSTRUCTED) != false ? 0.2f : 1.0f;
    }

    public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (Boolean)state.m_61143_((Property)CONSTRUCTED) == false ? Shapes.m_83040_() : Shapes.m_83144_();
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.m_83144_();
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new HubBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return HubBlock.m_152132_(type, (BlockEntityType)((BlockEntityType)ModBlocks.HUB_BE.get()), HubBlockEntity::tick);
    }
}


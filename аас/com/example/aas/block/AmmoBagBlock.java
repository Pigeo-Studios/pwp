/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.DirectionProperty
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.item.ModItems;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.world.AASWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AmmoBagBlock
extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
    public static final IntegerProperty USES = IntegerProperty.m_61631_((String)"uses", (int)1, (int)4);
    protected static final VoxelShape SHAPE = Block.m_49796_((double)3.0, (double)0.0, (double)3.0, (double)13.0, (double)7.0, (double)13.0);

    public AmmoBagBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283930_).m_60978_(0.5f).m_60955_());
        this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)FACING, (Comparable)Direction.NORTH)).m_61124_((Property)USES, (Comparable)Integer.valueOf(4)));
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.m_83040_();
    }

    public boolean m_7898_(BlockState state, LevelReader level, BlockPos pos) {
        return Block.m_49863_((LevelReader)level, (BlockPos)pos.m_7495_(), (Direction)Direction.UP);
    }

    public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (!state.m_60710_((LevelReader)level, currentPos)) {
            return Blocks.f_50016_.m_49966_();
        }
        return super.m_7417_(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Nullable
    public BlockState m_5573_(BlockPlaceContext context) {
        ItemStack stack = context.m_43722_();
        int usesLeft = 4 - stack.m_41773_();
        if (usesLeft < 1) {
            usesLeft = 1;
        }
        if (usesLeft > 4) {
            usesLeft = 4;
        }
        return (BlockState)((BlockState)this.m_49966_().m_61124_((Property)FACING, (Comparable)context.m_8125_().m_122424_())).m_61124_((Property)USES, (Comparable)Integer.valueOf(usesLeft));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{FACING, USES});
    }

    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player.m_6144_()) {
            if (!level.f_46443_) {
                int uses = (Integer)state.m_61143_((Property)USES);
                ItemStack returnStack = new ItemStack((ItemLike)ModItems.AMMO_BAG.get());
                returnStack.m_41721_(4 - uses);
                if (!player.m_150109_().m_36054_(returnStack)) {
                    player.m_36176_(returnStack, false);
                }
                level.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.m_7471_(pos, false);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.f_46443_) {
            AASWorldData.KitInfo kit;
            String kitName = player.getPersistentData().m_128461_("AAS_CurrentKit");
            if (kitName.isEmpty() || kitName.equals("Unassigned")) {
                player.m_213846_((Component)Component.m_237113_((String)"No kit assigned!").m_130940_(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }
            AASWorldData data = AASWorldData.get((ServerLevel)level);
            String t = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
            AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
            if (kit != null) {
                if (ResupplyHandler.resupplyPlayer((ServerPlayer)player, kit, true)) {
                    int currentUses = (Integer)state.m_61143_((Property)USES);
                    player.m_213846_((Component)Component.m_237113_((String)"Kit Resupplied! (Ammo Bags not refilled)").m_130940_(ChatFormatting.GREEN));
                    level.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (currentUses > 1) {
                        level.m_7731_(pos, (BlockState)state.m_61124_((Property)USES, (Comparable)Integer.valueOf(currentUses - 1)), 3);
                    } else {
                        level.m_7471_(pos, false);
                    }
                } else {
                    player.m_213846_((Component)Component.m_237113_((String)"Ammo already full!").m_130940_(ChatFormatting.YELLOW));
                }
            }
        }
        return InteractionResult.m_19078_((boolean)level.f_46443_);
    }
}


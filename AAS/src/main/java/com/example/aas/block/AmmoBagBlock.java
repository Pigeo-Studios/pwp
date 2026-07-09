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
    static final public DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    static final public IntegerProperty USES = IntegerProperty.create((String)"uses", 1, 4);
    static final protected VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 7.0, 13.0);

    public AmmoBagBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.5f).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)FACING, (Comparable)Direction.NORTH)).setValue((Property)USES, (Comparable)Integer.valueOf(4)));
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter((LevelReader)level, (BlockPos)pos.below(), (Direction)Direction.UP);
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (!state.canSurvive((LevelReader)level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        int usesLeft = 4 - stack.getDamageValue();
        if (usesLeft < 1) {
            usesLeft = 1;
        }
        if (usesLeft > 4) {
            usesLeft = 4;
        }
        return (BlockState)((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)context.getHorizontalDirection().getOpposite())).setValue((Property)USES, (Comparable)Integer.valueOf(usesLeft));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, USES});
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int uses = (Integer)state.getValue((Property)USES);
                ItemStack returnStack = new ItemStack((ItemLike)ModItems.AMMO_BAG.get());
                returnStack.setDamageValue(4 - uses);
                if (!player.getInventory().add(returnStack)) {
                    player.drop(returnStack, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.removeBlock(pos, false);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            AASWorldData.KitInfo kit;
            String kitName = player.getPersistentData().getString("AAS_CurrentKit");
            if (kitName.isEmpty() || kitName.equals("Unassigned")) {
                player.sendSystemMessage((Component)Component.literal((String)"No kit assigned!").withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }
            AASWorldData data = AASWorldData.get((ServerLevel)level);
            String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
            if (kit != null) {
                if (ResupplyHandler.resupplyPlayer((ServerPlayer)player, kit, true)) {
                    int currentUses = (Integer)state.getValue((Property)USES);
                    player.sendSystemMessage((Component)Component.literal((String)"Kit Resupplied! (Ammo Bags not refilled)").withStyle(ChatFormatting.GREEN));
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (currentUses > 1) {
                        level.setBlock(pos, (BlockState)state.setValue((Property)USES, (Comparable)Integer.valueOf(currentUses - 1)), 3);
                    } else {
                        level.removeBlock(pos, false);
                    }
                } else {
                    player.sendSystemMessage((Component)Component.literal((String)"Ammo already full!").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        return InteractionResult.sidedSuccess((boolean)level.isClientSide);
    }
}


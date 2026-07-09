/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Explosion
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.DirectionProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.BarbedWireBlockEntity;
import com.example.aas.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BarbedWireBlock
extends BaseEntityBlock {
    static final public BooleanProperty CONSTRUCTED = BooleanProperty.create((String)"constructed");
    static final public DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    static final public BooleanProperty VALID = BooleanProperty.create((String)"valid");
    static private final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

    public BarbedWireBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 9.0f).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)CONSTRUCTED, (Comparable)Boolean.valueOf(false))).setValue((Property)FACING, (Comparable)Direction.NORTH)).setValue((Property)VALID, (Comparable)Boolean.valueOf(true)));
    }

    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (((Boolean)state.getValue((Property)CONSTRUCTED)).booleanValue()) {
            entity.makeStuckInBlock(state, new Vec3(0.25, 0.05, 0.25));
            if (!level.isClientSide && entity instanceof LivingEntity && level.getGameTime() % 20L == 0L) {
                entity.hurt(level.damageSources().cactus(), 3.0f);
            }
        }
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
        return (Boolean)state.getValue((Property)CONSTRUCTED) != false ? 3.0f : 0.3f;
    }

    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return 9.0f;
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{CONSTRUCTED, FACING, VALID});
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarbedWireBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return BarbedWireBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)ModBlocks.WIRE_BE.get()), BarbedWireBlockEntity::tick);
    }
}


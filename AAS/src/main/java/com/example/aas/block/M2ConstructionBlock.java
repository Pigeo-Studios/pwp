/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
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
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.DirectionProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.M2ConstructionBlockEntity;
import com.example.aas.block.ModBlocks;
import com.example.aas.entity.M2BrowningEntity;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class M2ConstructionBlock
extends BaseEntityBlock {
    static final public DirectionProperty FACING = DirectionProperty.create((String)"facing", (Predicate)Direction.Plane.HORIZONTAL);
    static final public BooleanProperty VALID = BooleanProperty.create((String)"valid");
    static private final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public M2ConstructionBlock() {
        super(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)FACING, (Comparable)Direction.NORTH)).setValue((Property)VALID, (Comparable)Boolean.valueOf(true)));
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, VALID});
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new M2ConstructionBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return M2ConstructionBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)ModBlocks.M2_CONSTRUCTION_BE.get()), M2ConstructionBlockEntity::tick);
    }

    public void finishConstruction(ServerLevel level, BlockPos pos, BlockState state) {
        M2BrowningEntity gun = new M2BrowningEntity((Level)level, (double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5);
        Direction facing = (Direction)state.getValue((Property)FACING);
        float yaw = 0.0f;
        switch (facing) {
            case NORTH: {
                yaw = 180.0f;
                break;
            }
            case SOUTH: {
                yaw = 0.0f;
                break;
            }
            case WEST: {
                yaw = 90.0f;
                break;
            }
            case EAST: {
                yaw = -90.0f;
            }
        }
        gun.setYRot(yaw);
        gun.setTurretYaw(yaw);
        gun.setHasMagazine(false);
        level.addFreshEntity((Entity)gun);
        level.removeBlock(pos, false);
    }
}


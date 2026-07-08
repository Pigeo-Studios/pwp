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
    public static final DirectionProperty FACING = DirectionProperty.m_61546_((String)"facing", (Predicate)Direction.Plane.HORIZONTAL);
    public static final BooleanProperty VALID = BooleanProperty.m_61465_((String)"valid");
    private static final VoxelShape SHAPE = Block.m_49796_((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0);

    public M2ConstructionBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_60978_(1.0f).m_60955_());
        this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)FACING, (Comparable)Direction.NORTH)).m_61124_((Property)VALID, (Comparable)Boolean.valueOf(true)));
    }

    public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.m_83040_();
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{FACING, VALID});
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new M2ConstructionBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return M2ConstructionBlock.m_152132_(type, (BlockEntityType)((BlockEntityType)ModBlocks.M2_CONSTRUCTION_BE.get()), M2ConstructionBlockEntity::tick);
    }

    public void finishConstruction(ServerLevel level, BlockPos pos, BlockState state) {
        M2BrowningEntity gun = new M2BrowningEntity((Level)level, (double)pos.m_123341_() + 0.5, pos.m_123342_(), (double)pos.m_123343_() + 0.5);
        Direction facing = (Direction)state.m_61143_((Property)FACING);
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
        gun.m_146922_(yaw);
        gun.setTurretYaw(yaw);
        gun.setHasMagazine(false);
        level.m_7967_((Entity)gun);
        level.m_7471_(pos, false);
    }
}


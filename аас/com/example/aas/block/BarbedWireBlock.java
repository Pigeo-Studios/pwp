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
    public static final BooleanProperty CONSTRUCTED = BooleanProperty.m_61465_((String)"constructed");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
    public static final BooleanProperty VALID = BooleanProperty.m_61465_((String)"valid");
    private static final VoxelShape SHAPE = Block.m_49796_((double)1.0, (double)0.0, (double)1.0, (double)15.0, (double)15.0, (double)15.0);

    public BarbedWireBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60913_(3.0f, 9.0f).m_60955_());
        this.m_49959_((BlockState)((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)CONSTRUCTED, (Comparable)Boolean.valueOf(false))).m_61124_((Property)FACING, (Comparable)Direction.NORTH)).m_61124_((Property)VALID, (Comparable)Boolean.valueOf(true)));
    }

    public void m_7892_(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (((Boolean)state.m_61143_((Property)CONSTRUCTED)).booleanValue()) {
            entity.m_7601_(state, new Vec3(0.25, 0.05, 0.25));
            if (!level.f_46443_ && entity instanceof LivingEntity && level.m_46467_() % 20L == 0L) {
                entity.m_6469_(level.m_269111_().m_269325_(), 3.0f);
            }
        }
    }

    public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.m_83040_();
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
        return (Boolean)state.m_61143_((Property)CONSTRUCTED) != false ? 3.0f : 0.3f;
    }

    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return 9.0f;
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{CONSTRUCTED, FACING, VALID});
    }

    @Nullable
    public BlockState m_5573_(BlockPlaceContext context) {
        return (BlockState)this.m_49966_().m_61124_((Property)FACING, (Comparable)context.m_8125_().m_122424_());
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new BarbedWireBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.f_46443_) {
            return null;
        }
        return BarbedWireBlock.m_152132_(type, (BlockEntityType)((BlockEntityType)ModBlocks.WIRE_BE.get()), BarbedWireBlockEntity::tick);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
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
 *  net.minecraftforge.registries.ForgeRegistries
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import com.example.aas.block.MortarConstructionBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class MortarConstructionBlock
extends BaseEntityBlock {
    static final public DirectionProperty FACING = DirectionProperty.create((String)"facing", (Predicate)Direction.Plane.HORIZONTAL);
    static final public BooleanProperty VALID = BooleanProperty.create((String)"valid");
    static private final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public MortarConstructionBlock() {
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
        return new MortarConstructionBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return MortarConstructionBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)ModBlocks.MORTAR_CONSTRUCTION_BE.get()), MortarConstructionBlockEntity::tick);
    }

    public void finishConstruction(ServerLevel level, BlockPos pos, BlockState state) {
        double x = (double)pos.getX() + 0.5;
        double y = pos.getY();
        double z = (double)pos.getZ() + 0.5;
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
        EntityType type = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare", "mortar"));
        if (type != null) {
            Entity entity = type.create((Level)level);
            if (entity != null) {
                entity.setPos(x, y, z);
                entity.setYRot(yaw);
                level.addFreshEntity(entity);
            } else {
                System.err.println("AAS Mod: Failed to create entity 'superbwarfare:mortar'");
            }
        } else {
            System.err.println("AAS Mod: EntityType 'superbwarfare:mortar' not found!");
        }
        level.removeBlock(pos, false);
    }
}


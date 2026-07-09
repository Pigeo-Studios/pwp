/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.Containers
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Explosion
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.Level$ExplosionInteraction
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
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

import com.example.aas.block.AmmoStackBlockEntity;
import com.example.aas.config.AASConfig;
import com.example.aas.item.AGSAmmoItem;
import com.example.aas.item.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AGSAmmoStackBlock
extends BaseEntityBlock {
    static final public IntegerProperty MAGS = IntegerProperty.create((String)"mags", 1, 4);
    static final public DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final protected VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);

    public AGSAmmoStackBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noCollission().strength(0.5f).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)MAGS, (Comparable)Integer.valueOf(1))).setValue((Property)FACING, (Comparable)Direction.NORTH));
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmmoStackBlockEntity(pos, state);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be;
        if (!state.is(newState.getBlock()) && (be = level.getBlockEntity(pos)) instanceof AmmoStackBlockEntity) {
            AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
            List<Integer> counts = ammoBe.getAmmoCounts();
            for (int ammo : counts) {
                ItemStack stack = new ItemStack((ItemLike)ModItems.AGS_AMMO.get());
                AGSAmmoItem.setAmmo(stack, ammo);
                Containers.dropItemStack((Level)level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (ItemStack)stack);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void detonate(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AmmoStackBlockEntity) {
                AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
                ammoBe.clear();
            }
            int count = (Integer)state.getValue((Property)MAGS);
            float power = (float)count * 1.5f;
            level.removeBlock(pos, false);
            boolean canDestroy = (Boolean)AASConfig.AMMO_STACK_DESTRUCTION.get();
            Level.ExplosionInteraction interaction = canDestroy ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level.explode(null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, power, interaction);
        }
    }

    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && (projectile.isOnFire() || projectile.getDeltaMovement().length() > 0.5)) {
            this.detonate(level, hit.getBlockPos(), state);
        }
    }

    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            this.detonate(level, pos, state);
        }
        super.onBlockExploded(state, level, pos, explosion);
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
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)context.getHorizontalDirection().getOpposite());
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity be;
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && (be = level.getBlockEntity(pos)) instanceof AmmoStackBlockEntity) {
            AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
            int ammo = AGSAmmoItem.getAmmo(stack);
            ammoBe.addAmmoBox(ammo);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{MAGS, FACING});
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);
        int count = (Integer)state.getValue((Property)MAGS);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AmmoStackBlockEntity)) {
            return InteractionResult.FAIL;
        }
        AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int ammoInside = ammoBe.removeTopAmmoBox();
                ItemStack returnStack = new ItemStack((ItemLike)ModItems.AGS_AMMO.get());
                AGSAmmoItem.setAmmo(returnStack, ammoInside);
                if (!player.getInventory().add(returnStack)) {
                    player.drop(returnStack, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (count > 1) {
                    level.setBlock(pos, (BlockState)state.setValue((Property)MAGS, (Comparable)Integer.valueOf(count - 1)), 3);
                } else {
                    level.removeBlock(pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (heldItem.getItem() == ModItems.AGS_AMMO.get()) {
            if (count < 4) {
                if (!level.isClientSide) {
                    int ammoInHand = AGSAmmoItem.getAmmo(heldItem);
                    ammoBe.addAmmoBox(ammoInHand);
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    level.setBlock(pos, (BlockState)state.setValue((Property)MAGS, (Comparable)Integer.valueOf(count + 1)), 3);
                    level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}


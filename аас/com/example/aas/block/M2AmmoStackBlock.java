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
import com.example.aas.item.M2AmmoItem;
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

public class M2AmmoStackBlock
extends BaseEntityBlock {
    public static final IntegerProperty MAGS = IntegerProperty.m_61631_((String)"mags", (int)1, (int)4);
    public static final DirectionProperty FACING = BlockStateProperties.f_61374_;
    protected static final VoxelShape SHAPE = Block.m_49796_((double)2.0, (double)0.0, (double)2.0, (double)14.0, (double)6.0, (double)14.0);

    public M2AmmoStackBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60910_().m_60978_(0.5f).m_60955_());
        this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)MAGS, (Comparable)Integer.valueOf(1))).m_61124_((Property)FACING, (Comparable)Direction.NORTH));
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new AmmoStackBlockEntity(pos, state);
    }

    public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be;
        if (!state.m_60713_(newState.m_60734_()) && (be = level.m_7702_(pos)) instanceof AmmoStackBlockEntity) {
            AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
            List<Integer> counts = ammoBe.getAmmoCounts();
            for (int ammo : counts) {
                ItemStack stack = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
                M2AmmoItem.setAmmo(stack, ammo);
                Containers.m_18992_((Level)level, (double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_(), (ItemStack)stack);
            }
        }
        super.m_6810_(state, level, pos, newState, isMoving);
    }

    private void detonate(Level level, BlockPos pos, BlockState state) {
        if (!level.f_46443_) {
            BlockEntity be = level.m_7702_(pos);
            if (be instanceof AmmoStackBlockEntity) {
                AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
                ammoBe.clear();
            }
            int count = (Integer)state.m_61143_((Property)MAGS);
            float power = (float)count * 1.0f;
            level.m_7471_(pos, false);
            boolean canDestroy = (Boolean)AASConfig.AMMO_STACK_DESTRUCTION.get();
            Level.ExplosionInteraction interaction = canDestroy ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level.m_254849_(null, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, power, interaction);
        }
    }

    public void m_5581_(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.f_46443_ && (projectile.m_6060_() || projectile.m_20184_().m_82553_() > 0.5)) {
            this.detonate(level, hit.m_82425_(), state);
        }
    }

    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.f_46443_) {
            this.detonate(level, pos, state);
        }
        super.onBlockExploded(state, level, pos, explosion);
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
        return (BlockState)this.m_49966_().m_61124_((Property)FACING, (Comparable)context.m_8125_().m_122424_());
    }

    public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity be;
        super.m_6402_(level, pos, state, placer, stack);
        if (!level.f_46443_ && (be = level.m_7702_(pos)) instanceof AmmoStackBlockEntity) {
            AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
            int ammo = M2AmmoItem.getAmmo(stack);
            ammoBe.addAmmoBox(ammo);
        }
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{MAGS, FACING});
    }

    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.m_21120_(hand);
        int count = (Integer)state.m_61143_((Property)MAGS);
        BlockEntity be = level.m_7702_(pos);
        if (!(be instanceof AmmoStackBlockEntity)) {
            return InteractionResult.FAIL;
        }
        AmmoStackBlockEntity ammoBe = (AmmoStackBlockEntity)be;
        if (player.m_6144_()) {
            if (!level.f_46443_) {
                int ammoInside = ammoBe.removeTopAmmoBox();
                ItemStack returnStack = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
                M2AmmoItem.setAmmo(returnStack, ammoInside);
                if (!player.m_150109_().m_36054_(returnStack)) {
                    player.m_36176_(returnStack, false);
                }
                level.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (count > 1) {
                    level.m_7731_(pos, (BlockState)state.m_61124_((Property)MAGS, (Comparable)Integer.valueOf(count - 1)), 3);
                } else {
                    level.m_7471_(pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (heldItem.m_41720_() == ModItems.M2_AMMO.get()) {
            if (count < 4) {
                if (!level.f_46443_) {
                    int ammoInHand = M2AmmoItem.getAmmo(heldItem);
                    ammoBe.addAmmoBox(ammoInHand);
                    if (!player.m_7500_()) {
                        heldItem.m_41774_(1);
                    }
                    level.m_7731_(pos, (BlockState)state.m_61124_((Property)MAGS, (Comparable)Integer.valueOf(count + 1)), 3);
                    level.m_5594_(null, pos, SoundEvents.f_12065_, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}


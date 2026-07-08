/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Explosion
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.Level$ExplosionInteraction
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
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
 *  net.minecraftforge.registries.ForgeRegistries
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.block;

import com.example.aas.config.AASConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class MortarShellStackBlock
extends Block {
    public static final IntegerProperty SHELLS = IntegerProperty.m_61631_((String)"shells", (int)1, (int)8);
    public static final DirectionProperty FACING = BlockStateProperties.f_61374_;
    protected static final VoxelShape SHAPE = Block.m_49796_((double)2.0, (double)0.0, (double)2.0, (double)14.0, (double)4.0, (double)14.0);

    public MortarShellStackBlock() {
        super(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60910_().m_60978_(0.5f).m_60955_());
        this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)SHELLS, (Comparable)Integer.valueOf(1))).m_61124_((Property)FACING, (Comparable)Direction.NORTH));
    }

    private void detonate(Level level, BlockPos pos, BlockState state) {
        if (!level.f_46443_) {
            int count = (Integer)state.m_61143_((Property)SHELLS);
            float power = (float)count * 2.5f;
            level.m_7471_(pos, false);
            boolean canDestroy = (Boolean)AASConfig.AMMO_STACK_DESTRUCTION.get();
            Level.ExplosionInteraction interaction = canDestroy ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level.m_254849_(null, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, power, interaction);
        }
    }

    public void m_5581_(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.f_46443_ && (projectile.m_6060_() || projectile.m_20184_().m_82553_() > 0.1)) {
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

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{SHELLS, FACING});
    }

    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.m_21120_(hand);
        int count = (Integer)state.m_61143_((Property)SHELLS);
        if (player.m_6144_()) {
            if (!level.f_46443_) {
                ItemStack returnStack = this.getMortarItem();
                if (!player.m_150109_().m_36054_(returnStack)) {
                    player.m_36176_(returnStack, false);
                }
                level.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (count > 1) {
                    level.m_7731_(pos, (BlockState)state.m_61124_((Property)SHELLS, (Comparable)Integer.valueOf(count - 1)), 3);
                } else {
                    level.m_7471_(pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (MortarShellStackBlock.isMortarItem(heldItem)) {
            if (count < 8) {
                if (!level.f_46443_) {
                    if (!player.m_7500_()) {
                        heldItem.m_41774_(1);
                    }
                    level.m_7731_(pos, (BlockState)state.m_61124_((Property)SHELLS, (Comparable)Integer.valueOf(count + 1)), 3);
                    level.m_5594_(null, pos, SoundEvents.f_12065_, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public static boolean isMortarItem(ItemStack stack) {
        if (stack.m_41619_()) {
            return false;
        }
        Item item = stack.m_41720_();
        if (item == Items.f_42412_) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey((Object)item);
        return id != null && id.toString().equals("superbwarfare:mortar_shell");
    }

    private ItemStack getMortarItem() {
        Item modItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
        if (modItem != null && modItem != Items.f_41852_) {
            return new ItemStack((ItemLike)modItem);
        }
        return new ItemStack((ItemLike)Items.f_42412_);
    }
}


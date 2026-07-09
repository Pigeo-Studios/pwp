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

public class TOWMissileStackBlock
extends Block {
    static final public IntegerProperty STACK = IntegerProperty.create((String)"stack", 1, 4);
    static final public DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final protected VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);

    public TOWMissileStackBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noCollission().strength(0.5f).noOcclusion());
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)STACK, (Comparable)Integer.valueOf(1))).setValue((Property)FACING, (Comparable)Direction.NORTH));
    }

    public static Item getCorrectItem() {
        Item modItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "medium_anti_ground_missile"));
        if (modItem != null && modItem != Items.AIR) {
            return modItem;
        }
        return Items.SPECTRAL_ARROW;
    }

    public static boolean isTOWItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == TOWMissileStackBlock.getCorrectItem();
    }

    private void detonate(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide) {
            int count = (Integer)state.getValue((Property)STACK);
            float power = (float)count * 3.0f;
            level.removeBlock(pos, false);
            boolean canDestroy = (Boolean)AASConfig.AMMO_STACK_DESTRUCTION.get();
            Level.ExplosionInteraction interaction = canDestroy ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level.explode(null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, power, interaction);
        }
    }

    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && (projectile.isOnFire() || projectile.getDeltaMovement().length() > 0.1)) {
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

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{STACK, FACING});
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);
        int count = (Integer)state.getValue((Property)STACK);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ItemStack returnStack = new ItemStack((ItemLike)TOWMissileStackBlock.getCorrectItem());
                if (!player.getInventory().add(returnStack)) {
                    player.drop(returnStack, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (count > 1) {
                    level.setBlock(pos, (BlockState)state.setValue((Property)STACK, (Comparable)Integer.valueOf(count - 1)), 3);
                } else {
                    level.removeBlock(pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (TOWMissileStackBlock.isTOWItem(heldItem)) {
            if (count < 4) {
                if (!level.isClientSide) {
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    level.setBlock(pos, (BlockState)state.setValue((Property)STACK, (Comparable)Integer.valueOf(count + 1)), 3);
                    level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}


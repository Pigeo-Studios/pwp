package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.item.M2AmmoItem;
import com.pigeostudios.pwp.warfare.item.ModItems;
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
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
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

// Блок стопки магазинов для M2 Browning
// Хранит до 4 магазинов, взрывается при попадании снаряда
public class M2AmmoStackBlock extends BaseEntityBlock {
   // Количество магазинов (1-4)
   public static final IntegerProperty MAGS = IntegerProperty.create("mags", 1, 4);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);

   public M2AmmoStackBlock() {
      super(Properties.of().mapColor(MapColor.METAL).noCollission().strength(0.5F).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(MAGS, 1)).setValue(FACING, Direction.NORTH));
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new AmmoStackBlockEntity(pos, state);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AmmoStackBlockEntity ammoBe) {
         for (int ammo : ammoBe.getAmmoCounts()) {
            ItemStack stack = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
            M2AmmoItem.setAmmo(stack, ammo);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
         }
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   // Детонирует стопку - создаёт взрыв в зависимости от количества магазинов
   private void detonate(Level level, BlockPos pos, BlockState state) {
      if (!level.isClientSide) {
         if (level.getBlockEntity(pos) instanceof AmmoStackBlockEntity ammoBe) {
            ammoBe.clear();
         }

         int count = (Integer)state.getValue(MAGS);
         float power = count * 1.0F;
         level.removeBlock(pos, false);
         boolean canDestroy = (Boolean)WarfareConfig.AMMO_STACK_DESTRUCTION.get();
         ExplosionInteraction interaction = canDestroy ? ExplosionInteraction.TNT : ExplosionInteraction.NONE;
         level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, interaction);
      }
   }

   public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
      if (!level.isClientSide && (projectile.isOnFire() || projectile.getDeltaMovement().length() > 0.5)) {
         this.detonate(level, hit.getBlockPos(), state);
      }
   }

   // Вызывает детонацию при разрушении блока взрывом
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
      return SHAPE;
   }

   public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
      return !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof AmmoStackBlockEntity ammoBe) {
         int ammo = M2AmmoItem.getAmmo(stack);
         ammoBe.addAmmoBox(ammo);
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{MAGS, FACING});
   }

   // Обработчик взаимодействия: шифт-клик забрать магазин, обычный клик с M2_AMMO - положить
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack heldItem = player.getItemInHand(hand);
      int count = (Integer)state.getValue(MAGS);
      if (level.getBlockEntity(pos) instanceof AmmoStackBlockEntity ammoBe) {
         if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
               int ammoInside = ammoBe.removeTopAmmoBox();
               ItemStack returnStack = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
               M2AmmoItem.setAmmo(returnStack, ammoInside);
               if (!player.getInventory().add(returnStack)) {
                  player.drop(returnStack, false);
               }

               level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
               if (count > 1) {
                  level.setBlock(pos, (BlockState)state.setValue(MAGS, count - 1), 3);
               } else {
                  level.removeBlock(pos, false);
               }
            }

            return InteractionResult.SUCCESS;
         } else if (heldItem.getItem() == ModItems.M2_AMMO.get()) {
            if (count < 4) {
               if (!level.isClientSide) {
                  int ammoInHand = M2AmmoItem.getAmmo(heldItem);
                  ammoBe.addAmmoBox(ammoInHand);
                  if (!player.isCreative()) {
                     heldItem.shrink(1);
                  }

                  level.setBlock(pos, (BlockState)state.setValue(MAGS, count + 1), 3);
                  level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
               }

               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.FAIL;
            }
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.FAIL;
      }
   }
}

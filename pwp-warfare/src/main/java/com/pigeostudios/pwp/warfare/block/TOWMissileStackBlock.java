package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

// Блок стопки ракет TOW
// Хранит до 4 ракет, взрывается с большой силой при детонации
public class TOWMissileStackBlock extends Block {
   // Количество ракет (1-4)
   public static final IntegerProperty STACK = IntegerProperty.create("stack", 1, 4);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);

   public TOWMissileStackBlock() {
      super(Properties.of().mapColor(MapColor.METAL).noCollission().strength(0.5F).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(STACK, 1)).setValue(FACING, Direction.NORTH));
   }

   // Возвращает предмет ракеты TOW из мода Superb Warfare
   public static Item getCorrectItem() {
      Item modItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "medium_anti_ground_missile"));
      return modItem != null && modItem != Items.AIR ? modItem : Items.SPECTRAL_ARROW;
   }

   // Проверяет, является ли предмет ракетой TOW
   public static boolean isTOWItem(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() == getCorrectItem();
   }

   // Детонирует стопку - сильный взрыв (3.0 силы на ракету)
   private void detonate(Level level, BlockPos pos, BlockState state) {
      if (!level.isClientSide) {
         int count = (Integer)state.getValue(STACK);
         float power = count * 3.0F;
         level.removeBlock(pos, false);
         boolean canDestroy = (Boolean)WarfareConfig.AMMO_STACK_DESTRUCTION.get();
         ExplosionInteraction interaction = canDestroy ? ExplosionInteraction.TNT : ExplosionInteraction.NONE;
         level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, interaction);
      }
   }

   public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
      if (!level.isClientSide && (projectile.isOnFire() || projectile.getDeltaMovement().length() > 0.1)) {
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
      return Shapes.empty();
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

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{STACK, FACING});
   }

   // Обработчик взаимодействия: шифт-клик забрать ракету, обычный клик - положить
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack heldItem = player.getItemInHand(hand);
      int count = (Integer)state.getValue(STACK);
      if (player.isShiftKeyDown()) {
         if (!level.isClientSide) {
            ItemStack returnStack = new ItemStack(getCorrectItem());
            if (!player.getInventory().add(returnStack)) {
               player.drop(returnStack, false);
            }

            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (count > 1) {
               level.setBlock(pos, (BlockState)state.setValue(STACK, count - 1), 3);
            } else {
               level.removeBlock(pos, false);
            }
         }

         return InteractionResult.SUCCESS;
      } else if (isTOWItem(heldItem)) {
         if (count < 4) {
            if (!level.isClientSide) {
               if (!player.isCreative()) {
                  heldItem.shrink(1);
               }

               level.setBlock(pos, (BlockState)state.setValue(STACK, count + 1), 3);
               level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.FAIL;
         }
      } else {
         return InteractionResult.PASS;
      }
   }
}

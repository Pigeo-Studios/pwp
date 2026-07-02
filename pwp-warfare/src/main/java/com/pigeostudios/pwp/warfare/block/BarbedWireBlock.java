package com.pigeostudios.pwp.warfare.block;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Блок колючей проволоки
// Наносит урон игрокам при прохождении через неё
public class BarbedWireBlock extends BaseEntityBlock {
   // Флаг завершения строительства
   public static final BooleanProperty CONSTRUCTED = BooleanProperty.create("constructed");
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty VALID = BooleanProperty.create("valid");
   private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

   public BarbedWireBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(3.0F, 9.0F).noOcclusion());
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(CONSTRUCTED, false)).setValue(FACING, Direction.NORTH))
            .setValue(VALID, true)
      );
   }

   // Наносит урон и замедляет сущность при прохождении через проволоку
   public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if ((Boolean)state.getValue(CONSTRUCTED)) {
         entity.makeStuckInBlock(state, new Vec3(0.25, 0.05, 0.25));
         if (!level.isClientSide && entity instanceof LivingEntity && level.getGameTime() % 20L == 0L) {
            entity.hurt(level.damageSources().cactus(), 3.0F);
         }
      }
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(CONSTRUCTED) ? 3.0F : 0.3F;
   }

   public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      return 9.0F;
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{CONSTRUCTED, FACING, VALID});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BarbedWireBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, (BlockEntityType)ModBlocks.WIRE_BE.get(), BarbedWireBlockEntity::tick);
   }
}

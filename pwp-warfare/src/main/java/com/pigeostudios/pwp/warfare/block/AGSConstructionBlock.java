package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Блок строительства АГС-30
// При завершении строительства спаунит сущность АГС-30
public class AGSConstructionBlock extends BaseEntityBlock {
   // Направление строительства
   public static final DirectionProperty FACING = DirectionProperty.create("facing", Plane.HORIZONTAL);
   // Флаг валидности позиции для строительства
   public static final BooleanProperty VALID = BooleanProperty.create("valid");
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

   public AGSConstructionBlock() {
      super(Properties.of().strength(1.0F).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(VALID, true));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, VALID});
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new AGSConstructionBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlocks.AGS_CONSTRUCTION_BE.get(), AGSConstructionBlockEntity::tick);
   }

   // Завершает строительство - спаунит АГС-30 и удаляет блок
   public void finishConstruction(ServerLevel level, BlockPos pos, BlockState state) {
      AGS30Entity gun = new AGS30Entity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      Direction facing = (Direction)state.getValue(FACING);
      float yaw = 0.0F;
      switch (facing) {
         case NORTH:
            yaw = -90.0F;
            break;
         case SOUTH:
            yaw = 90.0F;
            break;
         case WEST:
            yaw = 180.0F;
            break;
         case EAST:
            yaw = 0.0F;
      }

      gun.setYRot(yaw);
      gun.setTurretYaw(0.0F);
      gun.setHasMagazine(false);
      level.addFreshEntity(gun);
      level.removeBlock(pos, false);
   }
}

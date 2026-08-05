package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
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

// Блок строительства M2 Browning
// При завершении строительства спаунит сущность M2 Browning
public class M2ConstructionBlock extends BaseEntityBlock {
   // Направление строительства
   public static final DirectionProperty FACING = DirectionProperty.create("facing", Plane.HORIZONTAL);
   public static final BooleanProperty VALID = BooleanProperty.create("valid");
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

   public M2ConstructionBlock() {
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
      return new M2ConstructionBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlocks.M2_CONSTRUCTION_BE.get(), M2ConstructionBlockEntity::tick);
   }

   // Завершает строительство - спаунит M2 Browning и удаляет блок
   public void finishConstruction(ServerLevel level, BlockPos pos, BlockState state) {
      M2BrowningEntity gun = new M2BrowningEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      Direction facing = (Direction)state.getValue(FACING);
      float yaw = 0.0F;
      switch (facing) {
         case NORTH:
            yaw = 180.0F;
            break;
         case SOUTH:
            yaw = 0.0F;
            break;
         case WEST:
            yaw = 90.0F;
            break;
         case EAST:
            yaw = -90.0F;
      }

      gun.setYRot(yaw);
      gun.setTurretYaw(yaw);
      gun.setHasMagazine(false);
      level.addFreshEntity(gun);
      level.removeBlock(pos, false);
   }
}

package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Блок станции техники (Vehicle Station)
// Строится лопатой по чертежу (structureId 18), после постройки пополняет боезапас
// и ремонтирует технику рядом; разбирается лопатой с возвратом части материалов
public class VehicleStationBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty CONSTRUCTED = BooleanProperty.create("constructed");
   public static final BooleanProperty VALID = BooleanProperty.create("valid");
   public static final IntegerProperty BUILD_STAGE = IntegerProperty.create("build_stage", 0, 2);
   private static final VoxelShape SHAPE = Shapes.block();

   public VehicleStationBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(3.0F, 20.0F).noOcclusion());
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
            .setValue(CONSTRUCTED, false)).setValue(VALID, true).setValue(BUILD_STAGE, 0)
      );
   }

   public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(CONSTRUCTED) ? 3.0F : 0.3F;
   }

   public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      return 20.0F;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(CONSTRUCTED) ? SHAPE : Shapes.empty();
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, CONSTRUCTED, VALID, BUILD_STAGE});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof VehicleStationBlockEntity) {
         com.pigeostudios.pwp.warfare.world.WarfareWorldData data = WarfareWorldData.get((net.minecraft.server.level.ServerLevel)level);
         data.vehicleStations.removeIf(s -> s.pos.equals(pos));
         data.setDirty();
         PacketHandler.sendToAllClients((net.minecraft.server.level.ServerLevel)level, data);
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new VehicleStationBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlocks.VEHICLE_STATION_BE.get(), VehicleStationBlockEntity::tick);
   }
}

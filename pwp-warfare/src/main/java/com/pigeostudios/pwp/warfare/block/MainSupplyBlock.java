package com.pigeostudios.pwp.warfare.block;

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

// Блок станции снабжения (Main Supply / Vehicle Station)
// Неразрушимый для креативной постановки, но через чертёж (structureId 18) ставится
// как строящаяся станция: копается лопатой, имеет стадии строительства,
// после достройки пополняет боезапас и ремонтирует технику на базе
public class MainSupplyBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty CONSTRUCTED = BooleanProperty.create("constructed");
   public static final BooleanProperty VALID = BooleanProperty.create("valid");
   public static final IntegerProperty BUILD_STAGE = IntegerProperty.create("build_stage", 0, 2);

   public MainSupplyBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noOcclusion());
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH))
            .setValue(CONSTRUCTED, true)).setValue(VALID, true).setValue(BUILD_STAGE, 2)
      );
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(CONSTRUCTED) ? 3.0F : 0.3F;
   }

   public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      return 3600000.0F;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(CONSTRUCTED) ? Shapes.block() : Shapes.empty();
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.block();
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, CONSTRUCTED, VALID, BUILD_STAGE});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MainSupplyBlockEntity) {
         com.pigeostudios.pwp.warfare.world.WarfareWorldData data = com.pigeostudios.pwp.warfare.world.WarfareWorldData.get((net.minecraft.server.level.ServerLevel)level);
         data.mainSupplies.removeIf(s -> s.pos.equals(pos));
         data.setDirty();
         com.pigeostudios.pwp.warfare.network.PacketHandler.sendToAllClients((net.minecraft.server.level.ServerLevel)level, data);
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new MainSupplyBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlocks.MAIN_SUPPLY_BE.get(), MainSupplyBlockEntity::tick);
   }
}

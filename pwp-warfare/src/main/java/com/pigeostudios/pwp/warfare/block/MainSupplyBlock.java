package com.pigeostudios.pwp.warfare.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

// Блок главного снабжения (Main Supply)
// Нерушимый блок, обеспечивающий пополнение ресурсов и ремонт техники на базе.
// Поворачивается фасадом к игроку при установке (в оригинале AAS поворота нет —
// баг, исправлен здесь); коллизия — стандартный блок 1x1x1, как в AAS
public class MainSupplyBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

   public MainSupplyBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noOcclusion());
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

package com.pigeostudios.pwp.warfare.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

// Блок главного снабжения (Main Supply)
// Неразрушимый блок, обеспечивающий пополнение ресурсов и ремонт техники на базе
public class MainSupplyBlock extends BaseEntityBlock {
   public MainSupplyBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noOcclusion());
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
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

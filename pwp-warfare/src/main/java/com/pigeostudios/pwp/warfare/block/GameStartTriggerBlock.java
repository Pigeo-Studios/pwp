package com.pigeostudios.pwp.warfare.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

// Блок триггера запуска игры
// Издаёт сигнал редстоуна при активации, автоматически выключается через некоторое время
public class GameStartTriggerBlock extends Block {
   // Состояние питания (включён/выключен)
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public GameStartTriggerBlock() {
      super(Properties.of().strength(2.0F));
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED});
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      return state.getValue(POWERED) ? 15 : 0;
   }

   public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      return state.getValue(POWERED) ? 15 : 0;
   }

   // Автоматически выключает блок через некоторое время после активации
   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(POWERED)) {
         level.setBlock(pos, (BlockState)state.setValue(POWERED, false), 3);
      }
   }
}

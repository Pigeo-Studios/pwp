package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Блок точки сбора (Rally Point)
// Позволяет отряду возрождаться в указанном месте временно
public class RallyPointBlock extends BaseEntityBlock {
   public static final VoxelShape SHAPE = Shapes.block();

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlocks.RALLY_BE.get(), (lvl, pos, st, be) -> {
         RallyPointBlockEntity rbe = (RallyPointBlockEntity)be;
         if (lvl.isClientSide) {
            rbe.handleSoundClient();
         } else {
            rbe.checkExpiry(lvl, pos);
         }
      });
   }

   public RallyPointBlock() {
      super(Properties.of().strength(1.0F).noOcclusion());
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RallyPointBlockEntity(pos, state);
   }

   // Обрабатывает уничтожение точки сбора: штраф за билеты и очистка данных отряда
   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RallyPointBlockEntity rallyBe && !level.isClientSide()) {
         ServerLevel serverLevel = (ServerLevel)level;
         WarfareWorldData data = WarfareWorldData.get(serverLevel);
         if (!rallyBe.isDecay) {
            int penalty = 20;
            if (state.getBlock() == ModBlocks.BLUE_RALLY_BLOCK.get()) {
               data.blueTickets = Math.max(0, data.blueTickets - penalty);
               this.broadcastMessage(serverLevel, "BLUE Rally Point Destroyed! (-" + penalty + ")", ChatFormatting.BLUE);
            } else if (state.getBlock() == ModBlocks.RED_RALLY_BLOCK.get()) {
               data.redTickets = Math.max(0, data.redTickets - penalty);
               this.broadcastMessage(serverLevel, "RED Rally Point Destroyed! (-" + penalty + ")", ChatFormatting.RED);
            }

            GameLogicEvents.checkSirenManual(serverLevel, data);
            GameLogicEvents.checkGameOver(serverLevel, data);
         }

         rallyBe.cleanupData(serverLevel);
         data.setDirty();
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   private void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
      level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(text).withStyle(color), false);
   }
}

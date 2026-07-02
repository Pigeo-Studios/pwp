package com.pigeostudios.pwp.warfare.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

// Блок спаунера техники
// Позволяет в креативном режиме настраивать и спаунить технику
public class VehicleSpawnerBlock extends BaseEntityBlock {
   public VehicleSpawnerBlock() {
      super(Properties.copy(Blocks.DROPPER).strength(-1.0F, 3600000.0F));
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new VehicleSpawnerBlockEntity(pos, state);
   }

   // Открывает меню спаунера техники (только для креативного режима)
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide) {
         if (player.isCreative()) {
            if (level.getBlockEntity(pos) instanceof VehicleSpawnerBlockEntity spawner) {
               NetworkHooks.openScreen((ServerPlayer)player, spawner, pos);
            }
         } else {
            player.displayClientMessage(Component.literal("Access Denied: Creative Mode Only"), true);
         }
      }

      return InteractionResult.SUCCESS;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, (BlockEntityType)ModBlocks.VEHICLE_SPAWNER_BE.get(), VehicleSpawnerBlockEntity::tick);
   }
}

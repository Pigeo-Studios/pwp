package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

// Блок передовой операционной базы (FOB/Hub)
// Позволяет команде возрождаться и строить технику после завершения строительства
public class HubBlock extends BaseEntityBlock {
   // Флаг завершения строительства хаба
   public static final BooleanProperty CONSTRUCTED = BooleanProperty.create("constructed");

   public HubBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(3.0F, 9.0F).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(CONSTRUCTED, false));
   }

   // Открывает меню хаба при взаимодействии (только для своей команды)
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      }

      if ((Boolean)state.getValue(CONSTRUCTED) && level.getBlockEntity(pos) instanceof HubBlockEntity hub) {
         if (!player.isCreative()) {
            String playerTeam = "NEUTRAL";
            if (player.getTeam() != null) {
               playerTeam = player.getTeam().getName();
            }

            String hubTeam = hub.getTeam();
            if (!hubTeam.equals("NEUTRAL") && !playerTeam.equalsIgnoreCase(hubTeam)) {
               if (level.isClientSide) {
                  player.displayClientMessage(Component.literal("Cannot access ENEMY Hub!").withStyle(ChatFormatting.RED), true);
               }

               return InteractionResult.FAIL;
            }
         }

         if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openHubMenu(pos));
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   // Обрабатывает демонтаж хаба своей командой (штраф за уничтожение своего FOB)
   public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide && (Boolean)state.getValue(CONSTRUCTED) && level.getBlockEntity(pos) instanceof HubBlockEntity hub) {
         String hubTeam = hub.getTeam();
         String playerTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
         if (hubTeam.equalsIgnoreCase(playerTeam)) {
            hub.wasDismantled = true;
            hub.setChanged();
            ServerLevel serverLevel = (ServerLevel)level;
            WarfareWorldData data = WarfareWorldData.get(serverLevel);
            int penalty = 10;
            if (hubTeam.equalsIgnoreCase("BLUE")) {
               data.blueTickets = Math.max(0, data.blueTickets - penalty);
               this.broadcastMessage(serverLevel, "BLUE player dismantled Friendly FOB! (-10 Tickets)", ChatFormatting.BLUE);
            } else if (hubTeam.equalsIgnoreCase("RED")) {
               data.redTickets = Math.max(0, data.redTickets - penalty);
               this.broadcastMessage(serverLevel, "RED player dismantled Friendly FOB! (-10 Tickets)", ChatFormatting.RED);
            }

            data.setDirty();
            PacketHandler.sendToAllClients(serverLevel, data);
         }
      }

      super.playerWillDestroy(level, pos, state, player);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HubBlockEntity hub && !level.isClientSide) {
         ServerLevel serverLevel = (ServerLevel)level;
         WarfareWorldData data = WarfareWorldData.get(serverLevel);
         data.hubs.removeIf(h -> h.pos.equals(pos));
         if ((Boolean)state.getValue(CONSTRUCTED)) {
            String hubTeam = hub.getTeam();
            if (!hub.wasDismantled && !hubTeam.equals("NEUTRAL")) {
               int penalty = 30;
               if (hubTeam.equalsIgnoreCase("BLUE")) {
                  data.blueTickets = Math.max(0, data.blueTickets - penalty);
                  this.broadcastMessage(serverLevel, "BLUE FOB Destroyed! (-30 Tickets)", ChatFormatting.BLUE);
               } else if (hubTeam.equalsIgnoreCase("RED")) {
                  data.redTickets = Math.max(0, data.redTickets - penalty);
                  this.broadcastMessage(serverLevel, "RED FOB Destroyed! (-30 Tickets)", ChatFormatting.RED);
               }
            }
         }

         GameLogicEvents.checkSirenManual(serverLevel, data);
         GameLogicEvents.checkGameOver(serverLevel, data);
         data.setDirty();
         PacketHandler.sendToAllClients(serverLevel, data);
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   // Отправляет сообщение всем игрокам на сервере
   private void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
      level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(text).withStyle(color), false);
   }

   public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(CONSTRUCTED) ? 3.0F : 0.3F;
   }

   public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      return 9.0F;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{CONSTRUCTED});
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
      return !(Boolean)state.getValue(CONSTRUCTED);
   }

   public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return state.getValue(CONSTRUCTED) ? 0.2F : 1.0F;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return !state.getValue(CONSTRUCTED) ? Shapes.empty() : Shapes.block();
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.block();
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new HubBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlocks.HUB_BE.get(), HubBlockEntity::tick);
   }
}

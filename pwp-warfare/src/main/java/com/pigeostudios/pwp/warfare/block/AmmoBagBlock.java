package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Р‘Р»РѕРє СЃСѓРјРєРё СЃ Р±РѕРµРїСЂРёРїР°СЃР°РјРё
// РџСЂРё РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРё РїРѕРїРѕР»РЅСЏРµС‚ Р·Р°РїР°СЃ РїР°С‚СЂРѕРЅРѕРІ РёРіСЂРѕРєР° РёР· РІС‹Р±СЂР°РЅРЅРѕРіРѕ РЅР°Р±РѕСЂР°
public class AmmoBagBlock extends Block {
   // РќР°РїСЂР°РІР»РµРЅРёРµ, РІ РєРѕС‚РѕСЂРѕРј СЃРјРѕС‚СЂРёС‚ СЃСѓРјРєР°
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   protected static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 7.0, 13.0);

   public AmmoBagBlock() {
      super(Properties.of().mapColor(MapColor.WOOL).strength(0.5F).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
      return !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   // РћР±СЂР°Р±РѕС‚С‡РёРє РІР·Р°РёРјРѕРґРµР№СЃС‚РІРёСЏ: РїСЂРё С€РёС„С‚-РєР»РёРєРµ РїРѕРґР±РёСЂР°РµС‚ СЃСѓРјРєСѓ, РёРЅР°С‡Рµ РїРѕРїРѕР»РЅСЏРµС‚ Р±РѕРµРїСЂРёРїР°СЃС‹
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      }

      if (player.isShiftKeyDown()) {
         if (!level.isClientSide) {
            ItemStack returnStack = new ItemStack((ItemLike)ModItems.AMMO_BAG.get());
            if (!player.getInventory().add(returnStack)) {
               player.drop(returnStack, false);
            }

            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
         }

         return InteractionResult.SUCCESS;
      } else {
         if (!level.isClientSide) {
            String kitName = player.getPersistentData().getString("WARFARE_CurrentKit");
            if (kitName.isEmpty() || kitName.equals("Unassigned")) {
               player.sendSystemMessage(Component.literal("No kit assigned!").withStyle(ChatFormatting.RED));
               return InteractionResult.SUCCESS;
            }

            WarfareWorldData data = WarfareWorldData.get((ServerLevel)level);
            String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            WarfareWorldData.KitInfo kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
            if (kit != null) {
               if (ResupplyHandler.resupplyPlayer((ServerPlayer)player, kit, true)) {
                  player.sendSystemMessage(Component.literal("Kit Resupplied! (Ammo Bags not refilled)").withStyle(ChatFormatting.GREEN));
                  level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                  level.removeBlock(pos, false);
               } else {
                  player.sendSystemMessage(Component.literal("Ammo already full!").withStyle(ChatFormatting.YELLOW));
               }
            }
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }
}

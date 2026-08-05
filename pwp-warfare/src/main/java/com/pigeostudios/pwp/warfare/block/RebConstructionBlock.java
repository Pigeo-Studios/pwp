package com.pigeostudios.pwp.warfare.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

// Блюпринт РЭБ (глушилки сигналов дронов из uncomplicated-fpv).
// Ставится через рацию за материалы, копается лопаткой, по завершении
// спавнит сущность uncomplicatedfpv:reb / uncomplicatedfpv:reb_mini.
public class RebConstructionBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = DirectionProperty.create("facing", Plane.HORIZONTAL);
   public static final BooleanProperty VALID = BooleanProperty.create("valid");
   public static final BooleanProperty MINI = BooleanProperty.create("mini");
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

   public RebConstructionBlock() {
      super(Properties.of().strength(1.0F).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any())
         .setValue(FACING, Direction.NORTH)).setValue(VALID, true).setValue(MINI, false));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING, VALID, MINI);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RebConstructionBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlocks.REB_CONSTRUCTION_BE.get(), RebConstructionBlockEntity::tick);
   }

    // Завершение стройки: спавн сущности РЭБ uncomplicated-fpv с тегом команды.
    public void finishConstruction(ServerLevel level, BlockPos pos, BlockState state) {
       boolean mini = (Boolean)state.getValue(MINI);
       String entityId = mini ? "uncomplicatedfpv:reb_mini" : "uncomplicatedfpv:reb";
       EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityId));
       if (type == null) {
          level.removeBlock(pos, false);
          return;
       }

       // Страховка: если в зоне уже стоит РЭБ (появился после установки блюпринта),
       // сущность не спавним — блюпринт просто сносится.
       double half = mini ? 1.5 : 2.5;
       double height = mini ? 2.5 : 5.0;
       AABB area = new AABB(
          pos.getX() + 0.5 - half, pos.getY(), pos.getZ() + 0.5 - half,
          pos.getX() + 0.5 + half, pos.getY() + height, pos.getZ() + 0.5 + half
       );
       for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity)null, area)) {
          ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
          if (eid != null && (eid.toString().equals("uncomplicatedfpv:reb") || eid.toString().equals("uncomplicatedfpv:reb_mini"))) {
             level.removeBlock(pos, false);
             return;
          }
       }

       Entity reb = type.create(level);
       if (reb == null) {
          level.removeBlock(pos, false);
          return;
      }

      String team = "NEUTRAL";
      if (level.getBlockEntity(pos) instanceof RebConstructionBlockEntity be) {
         team = be.getTeam();
      }

      reb.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      Direction facing = (Direction)state.getValue(FACING);
      float yaw = 0.0F;
      switch (facing) {
         case NORTH -> yaw = 180.0F;
         case SOUTH -> yaw = 0.0F;
         case WEST -> yaw = 90.0F;
         case EAST -> yaw = -90.0F;
         default -> {
         }
      }
      reb.setYRot(yaw);
      reb.getPersistentData().putString("WARFARE_VehicleTeam", team);
      reb.getPersistentData().putString("WARFARE_VehicleType", mini ? "REB_MINI" : "REB");
      level.addFreshEntity(reb);
      level.removeBlock(pos, false);
   }
}

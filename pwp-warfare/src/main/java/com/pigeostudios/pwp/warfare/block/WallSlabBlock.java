package com.pigeostudios.pwp.warfare.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Блок половинчатой стены (сляб)
// Используется в бункерах и стенах с амбразурой, имеет те же стадии строительства,
// что и обычная стена, общая сущность WALL_BE
public class WallSlabBlock extends WallBlock {
   private static final VoxelShape SHAPE = Shapes.or(
      Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
      Shapes.box(0.45, 0.5, 0.45, 0.55, 1.0, 0.55)
   );

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}

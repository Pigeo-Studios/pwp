package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// Рендер ящика снабжения как падающего блока
// Отображает блок поставки в мире с текстурами из атласа
public class SupplyCrateRenderer extends EntityRenderer<SupplyCrateEntity> {
   public SupplyCrateRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.5F;
   }

   public void render(SupplyCrateEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
      poseStack.translate(-0.5, 0.0, -0.5);
      BlockState blockState = ((Block)ModBlocks.SUPPLY_CRATE_VISUAL.get()).defaultBlockState();
      BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
      dispatcher.renderSingleBlock(blockState, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   public ResourceLocation getTextureLocation(SupplyCrateEntity entity) {
      return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
   }
}

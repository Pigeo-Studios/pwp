package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.client.model.ModelRallyPoint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// Рендер точки сбора отряда как блок-эндитей
// Отрисовывает 3D-модель с текстурой в мире
public class RallyPointRenderer implements BlockEntityRenderer<RallyPointBlockEntity> {
   private final ModelRallyPoint model = new ModelRallyPoint(ModelRallyPoint.createBodyLayer().bakeRoot());
   private static final ResourceLocation TEXTURE = new ResourceLocation("pwpwarfare", "textures/entity/rally_point.png");

   public RallyPointRenderer(Context context) {
   }

   public void render(RallyPointBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      poseStack.pushPose();
      poseStack.translate(0.5, 1.5, 0.5);
      poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
      VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
      this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }
}

package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.entity.M2BulletEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// Рендер пули M2 Browning в полёте
// Отображает тонкую полоску с текстурой, ориентированную по направлению
public class M2BulletRenderer extends EntityRenderer<M2BulletEntity> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("pwpwarfare", "textures/entity/m2_bullet.png");

   public M2BulletRenderer(Context context) {
      super(context);
   }

   public void render(M2BulletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
      VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(TEXTURE));
      Pose pose = poseStack.last();
      Matrix4f poseMatrix = pose.pose();
      Matrix3f normalMatrix = pose.normal();
      float len = 2.0F;
      float width = 0.05F;
      vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0F, -width, 0.0F, 0.0F);
      vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0F, width, 0.0F, 1.0F);
      vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0F, width, 1.0F, 1.0F);
      vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0F, -width, 1.0F, 0.0F);
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0F, -width, 0.0F, 0.0F);
      vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0F, width, 0.0F, 1.0F);
      vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0F, width, 1.0F, 1.0F);
      vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0F, -width, 1.0F, 0.0F);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v) {
      consumer.vertex(pose, x, y, z)
         .color(255, 255, 255, 255)
         .uv(u, v)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(normal, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   public ResourceLocation getTextureLocation(M2BulletEntity entity) {
      return TEXTURE;
   }
}

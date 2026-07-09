/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 */
package com.example.aas.client.renderer;

import com.example.aas.entity.M2BulletEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class M2BulletRenderer
extends EntityRenderer<M2BulletEntity> {
    static private final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/entity/m2_bullet.png");

    public M2BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(M2BulletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes((ResourceLocation)TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        float len = 2.0f;
        float width = 0.05f;
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, -width, 0.0f, 0.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, width, 0.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, width, 1.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, -width, 1.0f, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, -width, 0.0f, 0.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, width, 0.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, width, 1.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, -width, 1.0f, 0.0f);
        poseStack.popPose();
        super.render((Entity)entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v) {
        consumer.vertex(pose, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
    }

    public ResourceLocation getTextureLocation(M2BulletEntity entity) {
        return TEXTURE;
    }

    public ResourceLocation getTextureLocation(Entity entity) {
        return this.getTextureLocation((M2BulletEntity)entity);
    }

    public void render(Entity entity, float f, float f2, PoseStack poseStack, MultiBufferSource multiBufferSource, int n) {
        this.render((M2BulletEntity)entity, f, f2, poseStack, multiBufferSource, n);
    }
}


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
    private static final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/entity/m2_bullet.png");

    public M2BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(M2BulletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.m_85836_();
        poseStack.m_252781_(Axis.f_252436_.m_252977_(entity.m_146908_() - 90.0f));
        poseStack.m_252781_(Axis.f_252403_.m_252977_(entity.m_146909_()));
        VertexConsumer vertexConsumer = buffer.m_6299_(RenderType.m_110488_((ResourceLocation)TEXTURE));
        PoseStack.Pose pose = poseStack.m_85850_();
        Matrix4f poseMatrix = pose.m_252922_();
        Matrix3f normalMatrix = pose.m_252943_();
        float len = 2.0f;
        float width = 0.05f;
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, -width, 0.0f, 0.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, width, 0.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, width, 1.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, -width, 1.0f, 0.0f);
        poseStack.m_252781_(Axis.f_252529_.m_252977_(90.0f));
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, -width, 0.0f, 0.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, -len, 0.0f, width, 0.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, width, 1.0f, 1.0f);
        M2BulletRenderer.vertex(vertexConsumer, poseMatrix, normalMatrix, len, 0.0f, -width, 1.0f, 0.0f);
        poseStack.m_85849_();
        super.m_7392_((Entity)entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v) {
        consumer.m_252986_(pose, x, y, z).m_6122_(255, 255, 255, 255).m_7421_(u, v).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(normal, 0.0f, 1.0f, 0.0f).m_5752_();
    }

    public ResourceLocation getTextureLocation(M2BulletEntity entity) {
        return TEXTURE;
    }
}


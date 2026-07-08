/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 */
package com.example.aas.client.renderer;

import com.example.aas.block.RallyPointBlockEntity;
import com.example.aas.client.model.ModelRallyPoint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RallyPointRenderer
implements BlockEntityRenderer<RallyPointBlockEntity> {
    private final ModelRallyPoint model = new ModelRallyPoint(ModelRallyPoint.createBodyLayer().m_171564_());
    private static final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/entity/rally_point.png");

    public RallyPointRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(RallyPointBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.m_85836_();
        poseStack.m_85837_(0.5, 1.5, 0.5);
        poseStack.m_252781_(Axis.f_252403_.m_252977_(180.0f));
        VertexConsumer vertexConsumer = bufferSource.m_6299_(RenderType.m_110458_((ResourceLocation)TEXTURE));
        this.model.m_7695_(poseStack, vertexConsumer, packedLight, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.m_85849_();
    }
}


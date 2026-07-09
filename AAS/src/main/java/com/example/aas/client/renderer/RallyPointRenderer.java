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
 *  net.minecraft.world.level.block.entity.BlockEntity
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
import net.minecraft.world.level.block.entity.BlockEntity;

public class RallyPointRenderer
implements BlockEntityRenderer<RallyPointBlockEntity> {
    private final ModelRallyPoint model = new ModelRallyPoint(ModelRallyPoint.createBodyLayer().bakeRoot());
    static private final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/entity/rally_point.png");

    public RallyPointRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(RallyPointBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull((ResourceLocation)TEXTURE));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }

    public void render(BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int n, int n2) {
        this.render((RallyPointBlockEntity)blockEntity, f, poseStack, multiBufferSource, n, n2);
    }
}


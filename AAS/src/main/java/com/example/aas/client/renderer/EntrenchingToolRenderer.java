/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  org.joml.Matrix4f
 *  software.bernie.geckolib.model.GeoModel
 *  software.bernie.geckolib.renderer.GeoItemRenderer
 */
package com.example.aas.client.renderer;

import com.example.aas.client.model.EntrenchingToolModel;
import com.example.aas.item.EntrenchingToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EntrenchingToolRenderer
extends GeoItemRenderer<EntrenchingToolItem> {
    static private final ResourceLocation GUI_TEXTURE = new ResourceLocation("aas", "textures/item/entrenching_tool_gui.png");

    public EntrenchingToolRenderer() {
        super((GeoModel)new EntrenchingToolModel());
    }

    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (displayContext == ItemDisplayContext.GUI) {
            this.render2DIcon(poseStack, bufferSource, packedOverlay);
        } else {
            super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void render2DIcon(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        poseStack.pushPose();
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucent((ResourceLocation)GUI_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        int light = 0xF000F0;
        builder.vertex(matrix, 0.0f, 1.0f, 0.0f).color(255, 255, 255, 255).uv(0.0f, 0.0f).overlayCoords(packedOverlay).uv2(light).normal(0.0f, 0.0f, 1.0f).endVertex();
        builder.vertex(matrix, 1.0f, 1.0f, 0.0f).color(255, 255, 255, 255).uv(1.0f, 0.0f).overlayCoords(packedOverlay).uv2(light).normal(0.0f, 0.0f, 1.0f).endVertex();
        builder.vertex(matrix, 1.0f, 0.0f, 0.0f).color(255, 255, 255, 255).uv(1.0f, 1.0f).overlayCoords(packedOverlay).uv2(light).normal(0.0f, 0.0f, 1.0f).endVertex();
        builder.vertex(matrix, 0.0f, 0.0f, 0.0f).color(255, 255, 255, 255).uv(0.0f, 1.0f).overlayCoords(packedOverlay).uv2(light).normal(0.0f, 0.0f, 1.0f).endVertex();
        poseStack.popPose();
    }
}


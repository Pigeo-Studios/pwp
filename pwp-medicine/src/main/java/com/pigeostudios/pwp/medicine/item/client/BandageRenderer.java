package com.pigeostudios.pwp.medicine.item.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pigeostudios.pwp.medicine.item.BandageItem;
import com.pigeostudios.pwp.medicine.item.client.BandageModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BandageRenderer
extends GeoItemRenderer<BandageItem> {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("pwp_medicine", "textures/item/bandage_gui.png");

    public BandageRenderer() {
        super((GeoModel)new BandageModel());
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
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucent(GUI_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        builder.vertex(matrix, 0.0f, 1.0f, 0.5f).color(255, 255, 255, 255).uv(0.0f, 0.0f).overlayCoords(packedOverlay).uv2(0xF000F0).normal(0.0f, 0.0f, 1.0f).endVertex();
        builder.vertex(matrix, 1.0f, 1.0f, 0.5f).color(255, 255, 255, 255).uv(1.0f, 0.0f).overlayCoords(packedOverlay).uv2(0xF000F0).normal(0.0f, 0.0f, 1.0f).endVertex();
        builder.vertex(matrix, 1.0f, 0.0f, 0.5f).color(255, 255, 255, 255).uv(1.0f, 1.0f).overlayCoords(packedOverlay).uv2(0xF000F0).normal(0.0f, 0.0f, 1.0f).endVertex();
        builder.vertex(matrix, 0.0f, 0.0f, 0.5f).color(255, 255, 255, 255).uv(0.0f, 1.0f).overlayCoords(packedOverlay).uv2(0xF000F0).normal(0.0f, 0.0f, 1.0f).endVertex();
        poseStack.popPose();
    }
}

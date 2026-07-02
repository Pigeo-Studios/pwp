package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.client.model.EntrenchingToolModel;
import com.pigeostudios.pwp.warfare.item.EntrenchingToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoItemRenderer;

// Рендер сапёрной лопатки в руке и в GUI
// В инвентаре отображает 2D-иконку, в мире — 3D-модель
public class EntrenchingToolRenderer extends GeoItemRenderer<EntrenchingToolItem> {
   private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("pwpwarfare", "textures/item/entrenching_tool_gui.png");

   public EntrenchingToolRenderer() {
      super(new EntrenchingToolModel());
   }

   public void renderByItem(
      ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
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
      int light = 15728880;
      builder.vertex(matrix, 0.0F, 1.0F, 0.0F)
         .color(255, 255, 255, 255)
         .uv(0.0F, 0.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      builder.vertex(matrix, 1.0F, 1.0F, 0.0F)
         .color(255, 255, 255, 255)
         .uv(1.0F, 0.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      builder.vertex(matrix, 1.0F, 0.0F, 0.0F)
         .color(255, 255, 255, 255)
         .uv(1.0F, 1.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      builder.vertex(matrix, 0.0F, 0.0F, 0.0F)
         .color(255, 255, 255, 255)
         .uv(0.0F, 1.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      poseStack.popPose();
   }
}

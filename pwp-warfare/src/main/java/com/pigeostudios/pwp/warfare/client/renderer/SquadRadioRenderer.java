package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.client.model.SquadRadioModel;
import com.pigeostudios.pwp.warfare.item.RallyItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoItemRenderer;

// Рендер радиостанции в руке и в GUI
// В инвентаре отображает 2D-иконку, в мире — 3D-модель
public class SquadRadioRenderer extends GeoItemRenderer<RallyItem> {
   private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("pwpwarfare", "textures/item/squad_leader_radio_gui.png");

   public SquadRadioRenderer() {
      super(new SquadRadioModel());
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
      float min = 0.0F;
      float max = 1.0F;
      float z = 0.0F;
      int light = 15728880;
      builder.vertex(matrix, min, max, z)
         .color(255, 255, 255, 255)
         .uv(0.0F, 0.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      builder.vertex(matrix, max, max, z)
         .color(255, 255, 255, 255)
         .uv(1.0F, 0.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      builder.vertex(matrix, max, min, z)
         .color(255, 255, 255, 255)
         .uv(1.0F, 1.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      builder.vertex(matrix, min, min, z)
         .color(255, 255, 255, 255)
         .uv(0.0F, 1.0F)
         .overlayCoords(packedOverlay)
         .uv2(light)
         .normal(0.0F, 0.0F, 1.0F)
         .endVertex();
      poseStack.popPose();
   }
}

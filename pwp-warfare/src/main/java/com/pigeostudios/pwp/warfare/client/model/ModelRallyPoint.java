package com.pigeostudios.pwp.warfare.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

// Модель точки сбора отряда (классическая EntityModel Minecraft)
// Использует ModelPart для отрисовки блока сбора
public class ModelRallyPoint extends EntityModel<Entity> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("pwpwarfare", "rally_point"), "main");
   private final ModelPart Rally;

   public ModelRallyPoint(ModelPart root) {
      this.Rally = root.getChild("Rally");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Rally = partdefinition.addOrReplaceChild(
         "Rally",
         CubeListBuilder.create()
            .texOffs(0, 42)
            .addBox(-8.0F, 2.0F, -8.0F, 16.0F, 6.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(52, 30)
            .addBox(-7.0F, 0.0F, -4.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(40, 34)
            .addBox(-3.0F, -1.0F, -5.0F, 7.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 16.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      this.Rally.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
   }
}

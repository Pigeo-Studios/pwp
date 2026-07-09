/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.model.EntityModel
 *  net.minecraft.client.model.geom.ModelLayerLocation
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.model.geom.PartPose
 *  net.minecraft.client.model.geom.builders.CubeDeformation
 *  net.minecraft.client.model.geom.builders.CubeListBuilder
 *  net.minecraft.client.model.geom.builders.LayerDefinition
 *  net.minecraft.client.model.geom.builders.MeshDefinition
 *  net.minecraft.client.model.geom.builders.PartDefinition
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 */
package com.example.aas.client.model;

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

public class ModelRallyPoint
extends EntityModel<Entity> {
    static final public ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("aas", "rally_point"), "main");
    private final ModelPart Rally;

    public ModelRallyPoint(ModelPart root) {
        this.Rally = root.getChild("Rally");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition Rally = partdefinition.addOrReplaceChild("Rally", CubeListBuilder.create().texOffs(0, 42).addBox(-8.0f, 2.0f, -8.0f, 16.0f, 6.0f, 16.0f, new CubeDeformation(0.0f)).texOffs(52, 30).addBox(-7.0f, 0.0f, -4.0f, 4.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(40, 34).addBox(-3.0f, -1.0f, -5.0f, 7.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 16.0f, 0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, 64, 64);
    }

    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.Rally.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}


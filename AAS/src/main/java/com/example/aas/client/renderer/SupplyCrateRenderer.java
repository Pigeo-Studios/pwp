/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.example.aas.client.renderer;

import com.example.aas.block.ModBlocks;
import com.example.aas.entity.SupplyCrateEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SupplyCrateRenderer
extends EntityRenderer<SupplyCrateEntity> {
    public SupplyCrateRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    public void render(SupplyCrateEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        poseStack.translate(-0.5, 0.0, -0.5);
        BlockState blockState = ((Block)ModBlocks.SUPPLY_CRATE_VISUAL.get()).defaultBlockState();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.renderSingleBlock(blockState, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render((Entity)entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    public ResourceLocation getTextureLocation(SupplyCrateEntity entity) {
        return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    }

    public ResourceLocation getTextureLocation(Entity entity) {
        return this.getTextureLocation((SupplyCrateEntity)entity);
    }

    public void render(Entity entity, float f, float f2, PoseStack poseStack, MultiBufferSource multiBufferSource, int n) {
        this.render((SupplyCrateEntity)entity, f, f2, poseStack, multiBufferSource, n);
    }
}


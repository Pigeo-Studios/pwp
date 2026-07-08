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
        this.f_114477_ = 0.5f;
    }

    public void render(SupplyCrateEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.m_85836_();
        poseStack.m_252781_(Axis.f_252436_.m_252977_(180.0f - entityYaw));
        poseStack.m_85837_(-0.5, 0.0, -0.5);
        BlockState blockState = ((Block)ModBlocks.SUPPLY_CRATE_VISUAL.get()).m_49966_();
        BlockRenderDispatcher dispatcher = Minecraft.m_91087_().m_91289_();
        dispatcher.m_110912_(blockState, poseStack, buffer, packedLight, OverlayTexture.f_118083_);
        poseStack.m_85849_();
        super.m_7392_((Entity)entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    public ResourceLocation getTextureLocation(SupplyCrateEntity entity) {
        return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    }
}


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

import com.example.aas.client.model.SquadRadioModel;
import com.example.aas.item.RallyItem;
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

public class SquadRadioRenderer
extends GeoItemRenderer<RallyItem> {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("aas", "textures/item/squad_leader_radio_gui.png");

    public SquadRadioRenderer() {
        super((GeoModel)new SquadRadioModel());
    }

    public void m_108829_(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (displayContext == ItemDisplayContext.GUI) {
            this.render2DIcon(poseStack, bufferSource, packedOverlay);
        } else {
            super.m_108829_(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void render2DIcon(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        poseStack.m_85836_();
        VertexConsumer builder = bufferSource.m_6299_(RenderType.m_110473_((ResourceLocation)GUI_TEXTURE));
        Matrix4f matrix = poseStack.m_85850_().m_252922_();
        float min = 0.0f;
        float max = 1.0f;
        float z = 0.0f;
        int light = 0xF000F0;
        builder.m_252986_(matrix, min, max, z).m_6122_(255, 255, 255, 255).m_7421_(0.0f, 0.0f).m_86008_(packedOverlay).m_85969_(light).m_5601_(0.0f, 0.0f, 1.0f).m_5752_();
        builder.m_252986_(matrix, max, max, z).m_6122_(255, 255, 255, 255).m_7421_(1.0f, 0.0f).m_86008_(packedOverlay).m_85969_(light).m_5601_(0.0f, 0.0f, 1.0f).m_5752_();
        builder.m_252986_(matrix, max, min, z).m_6122_(255, 255, 255, 255).m_7421_(1.0f, 1.0f).m_86008_(packedOverlay).m_85969_(light).m_5601_(0.0f, 0.0f, 1.0f).m_5752_();
        builder.m_252986_(matrix, min, min, z).m_6122_(255, 255, 255, 255).m_7421_(0.0f, 1.0f).m_86008_(packedOverlay).m_85969_(light).m_5601_(0.0f, 0.0f, 1.0f).m_5752_();
        poseStack.m_85849_();
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.PlayerModel
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.renderer.ItemInHandRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderDispatcher
 *  net.minecraft.client.renderer.entity.player.PlayerRenderer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraft.world.item.ItemStack
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.example.aas.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ItemInHandRenderer.class})
public class FirstPersonArmFixMixin {
    @Inject(method={"renderArmWithItem"}, at={@At(value="HEAD")})
    private void aas$forceShowArm(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
        if (stack.isEmpty()) {
            boolean isRightArm;
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            PlayerRenderer renderer = (PlayerRenderer)dispatcher.getRenderer((Entity)player);
            PlayerModel model = (PlayerModel)renderer.getModel();
            HumanoidArm mainArm = player.getMainArm();
            boolean bl = hand == InteractionHand.MAIN_HAND ? mainArm == HumanoidArm.RIGHT : (isRightArm = mainArm == HumanoidArm.LEFT);
            if (isRightArm) {
                model.rightArm.visible = true;
                model.rightSleeve.visible = true;
            } else {
                model.leftArm.visible = true;
                model.leftSleeve.visible = true;
            }
        }
    }
}


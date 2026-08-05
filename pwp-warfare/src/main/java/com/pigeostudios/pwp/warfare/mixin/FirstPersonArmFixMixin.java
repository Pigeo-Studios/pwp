package com.pigeostudios.pwp.warfare.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
// Миксин: исправление отображения рук в первом лице
// Принудительно показывает руку игрока, даже если предмет в руке пуст
public class FirstPersonArmFixMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
   private void pwpwarfare$forceShowArm(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      if (stack.isEmpty()) {
         EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
         PlayerRenderer renderer = (PlayerRenderer)dispatcher.getRenderer(player);
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)renderer.getModel();
         HumanoidArm mainArm = player.getMainArm();
         boolean isRightArm = hand == InteractionHand.MAIN_HAND ? mainArm == HumanoidArm.RIGHT : mainArm == HumanoidArm.LEFT;
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

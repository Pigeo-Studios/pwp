package com.pigeostudios.pwp.warfare.mixin;

import com.pigeostudios.pwp.warfare.client.FeedbackMessageBlock;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Замена ванильного экшенбара на свой фидбек-блок. Все мгновенные сообщения
// (displayClientMessage по всему моду + любые другие моды) приходят сюда через
// setOverlayMessage — перехватываем и отменяем ванильный показ, рисуем свой
// стильный блок (FeedbackMessageBlock, ярус h-34). Таймер/гашение — свои
// (clearOverlayMessage в 1.20.1 отсутствует, записи гаснут по таймеру).
@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public abstract class GuiOverlayMessageMixin {
   @Inject(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
   private void pwp$captureOverlayMessage(Component component, boolean animateFromBottom, CallbackInfo ci) {
      FeedbackMessageBlock.show(component);
      ci.cancel();
   }
}

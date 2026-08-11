package com.pigeostudios.pwp.warfare.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Отключаем ванильный попап имени предмета над хотбаром (Gui.lastToolHighlight),
// который всплывает при переключении слота хотбара или подборе предмета в выбранный слот.
// Это отдельная система от экшенбара (setOverlayMessage/FeedbackMessageBlock) — там свой
// фидбек-блок, а тут ванильный попап не нужен и визуально дублирует ярус фидбека (h-58).
@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public abstract class SelectedItemNameMixin {
   @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
   private void pwp$hideSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
      ci.cancel();
   }
}

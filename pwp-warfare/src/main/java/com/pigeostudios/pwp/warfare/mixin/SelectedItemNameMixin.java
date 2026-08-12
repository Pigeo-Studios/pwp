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
// который всплывает при переключении слота хотбара (цифры/скролл) или подборе предмета
// в выбранный слот. Отдельная система от экшенбара (setOverlayMessage/FeedbackMessageBlock).
// ИНЦИДЕНТ 12.08.2026: в Forge 47.4.x рендер имени вынесен в оверлей-систему —
// VanillaGuiOverlay.ITEM_NAME вызывает Forge-добавленный 2-арг
// renderSelectedItemName(GuiGraphics, int) НАПРЯМУЮ (минуя 1-арг, который я отменял первым).
// Поэтому отменяем ОБА: 1-арг (ваниль, обычный ремап) + 2-арг (метод добавлен Forge-патчем,
// SRG-имени нет → remap = false, имя совпадает в dev и prod).
@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public abstract class SelectedItemNameMixin {
   @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
   private void pwp$hideSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
      ci.cancel();
   }

   @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At("HEAD"), cancellable = true, remap = false)
   private void pwp$hideSelectedItemName2(GuiGraphics guiGraphics, int yOffset, CallbackInfo ci) {
      ci.cancel();
   }
}

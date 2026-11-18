package com.pwp.coreclient.mixin;

import com.pwp.coreclient.ui.hud.HudInventory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Этап 6 HUD: сундук и шалкер не переопределяют renderLabels — ванильные тёмно-серые подписи
 * (заголовок + «Инвентарь») рисует базовый класс. При PWP-стиле их рисует {@link HudInventory} в
 * renderBg, здесь — отмена только для белого списка ({@link HudInventory#styled}).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void pwp_renderLabels(GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
        if (HudInventory.styled((AbstractContainerScreen<?>) (Object) this)) ci.cancel();
    }
}

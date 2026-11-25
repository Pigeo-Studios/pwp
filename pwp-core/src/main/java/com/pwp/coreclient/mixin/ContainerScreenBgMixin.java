package com.pwp.coreclient.mixin;

import com.pwp.coreclient.ui.hud.HudInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Этап 6 HUD: PWP-фон сундука ({@link ContainerScreen}: сундук/бочка/вагонетка/эндер-сундук) и
 * шалкера. Оба класса объявляют свой renderBg, в котором только blit текстуры — замена безопасна.
 * Подклассы (модовые экраны) не стилизуются: {@link HudInventory#styled} сверяет точный класс.
 */
@Mixin({ContainerScreen.class, ShulkerBoxScreen.class})
public abstract class ContainerScreenBgMixin {

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void pwp_renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!HudInventory.styled(self)) return;
        ci.cancel();
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) self;
        HudInventory.renderContainer(
                g,
                Minecraft.getInstance().font,
                self.getMenu().slots,
                self.getGuiLeft(),
                self.getGuiTop(),
                self.getXSize(),
                self.getYSize(),
                self.getTitle(),
                acc.pwp_getPlayerInventoryTitle(),
                acc.pwp_getInventoryLabelX(),
                acc.pwp_getInventoryLabelY());
    }
}

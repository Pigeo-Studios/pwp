package com.pwp.coreclient.mixin;

import com.pwp.coreclient.ui.hud.HudInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Этап 6 HUD: PWP-фон ванильного {@link InventoryScreen} (только при {@code hud.inventory_style} и
 * точном классе — см. {@link HudInventory#styled}). Отменяются лишь renderBg/renderLabels этого
 * класса: слоты, клики, книга рецептов и кнопка Curios работают как в ванили.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void pwp_renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        if (!HudInventory.styled(self)) return;
        ci.cancel();
        InventoryScreenAccessor mouse = (InventoryScreenAccessor) self;
        HudInventory.renderPlayer(
                g,
                Minecraft.getInstance().font,
                self.getMenu().slots,
                self.getGuiLeft(),
                self.getGuiTop(),
                self.getXSize(),
                self.getYSize(),
                mouse.pwp_getXMouse(),
                mouse.pwp_getYMouse(),
                Minecraft.getInstance().player);
    }

    /** Ванильная подпись «Crafting» тёмно-серым не читается на стекле — рисуем свою в renderBg. */
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void pwp_renderLabels(GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
        if (HudInventory.styled((InventoryScreen) (Object) this)) ci.cancel();
    }
}

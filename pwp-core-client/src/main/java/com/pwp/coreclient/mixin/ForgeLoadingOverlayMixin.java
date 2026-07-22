package com.pwp.coreclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeLoadingOverlay.class)
public class ForgeLoadingOverlayMixin {

    private static final int PWP_BG = 0xFF0D1217;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pwp$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        guiGraphics.fill(0, 0, w, h, PWP_BG);
    }
}

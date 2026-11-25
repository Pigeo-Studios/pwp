package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPLoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class LevelTransitionMixin {

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void pwp_onLevelChangeStart(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            if (mc.screen instanceof ReceivingLevelScreen || mc.screen instanceof LevelLoadingScreen) {
                mc.setScreen(new PWPLoadingScreen(PWPLoadingScreen.Context.CHANGING_DIMENSION));
            } else if (mc.screen == null) {
                mc.setScreen(new PWPLoadingScreen(PWPLoadingScreen.Context.LOADING_WORLD));
            }
        }
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void pwp_onLevelChangeEnd(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PWPLoadingScreen) {
            mc.setScreen(null);
        }
    }
}

package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPLoadingScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class LevelTransitionMixin {

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void pwp_onLevelChange(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.screen == null) {
            mc.setScreen(new PWPLoadingScreen(PWPLoadingScreen.Context.LOADING_WORLD));
        }
    }
}

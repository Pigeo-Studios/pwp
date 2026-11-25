package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique
    private static boolean pwp_initialized = false;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void pwp_replaceWithMainMenu(CallbackInfo ci) {
        if (pwp_initialized) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TitleScreen) {
            pwp_initialized = true;
            mc.setScreen(new PWPMainMenuScreen());
            ci.cancel();
        }
    }
}

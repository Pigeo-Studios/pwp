package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPLoadingView;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelLoadingScreen.class, ReceivingLevelScreen.class})
public class LevelLoadingScreenMixin {

    @Unique
    private PWPLoadingView pwp_view;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pwp_customRender(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        PWPRotatingBackground.render(gui, 0, 0, w, h);
        if (pwp_view == null) pwp_view = new PWPLoadingView();
        pwp_view.render(
                gui,
                PWPTheme.Fonts.display(),
                w,
                h,
                UIIcon.INFO,
                Component.translatable("pwp_core.loading.loading_world"),
                -1F,
                null);
    }
}

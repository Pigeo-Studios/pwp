package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPTipsWidget;
import com.pwp.coreclient.gui.screens.PWPUtils;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ LevelLoadingScreen.class, ReceivingLevelScreen.class })
public class LevelLoadingScreenMixin {

    @Unique
    private static final ResourceLocation PWP_LOADING_BG = new ResourceLocation("pwp_core_client", "textures/gui/loading.png");

    @Unique
    private long pwp_openTime;

    @Unique
    private PWPTipsWidget pwp_tips;

    @Unique
    private int pwp_lastWidth;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void pwp_customBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        gui.blit(PWP_LOADING_BG, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void pwp_onRenderHead(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (pwp_openTime == 0) {
            pwp_openTime = System.currentTimeMillis();
        }
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if (pwp_tips == null || w != pwp_lastWidth) {
            pwp_lastWidth = w;
            pwp_tips = new PWPTipsWidget((int) (w * 0.6f));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pwp_customOverlay(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        int cx = w / 2;
        int cy = h / 2;

        long elapsed = System.currentTimeMillis() - pwp_openTime;

        PWPUtils.renderLogo(gui, cx, (int) (h * 0.16f));
        PWPUtils.renderSpinner(gui, cx, cy - 30, elapsed);
        PWPUtils.renderStatusText(gui, "Загрузка мира...", cx, cy - 10, elapsed);
        PWPUtils.renderProgressBar(gui, cx, cy + 30, (int) (w * 0.3f), 4, elapsed);

        if (pwp_tips != null) {
            pwp_tips.render(gui, cx, cy + 55);
        }
    }
}

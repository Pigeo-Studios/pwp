package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.components.PWPProgressBar;
import com.pwp.coreclient.gui.screens.PWPTipsWidget;
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
    private static final ResourceLocation PWP_BG = new ResourceLocation("pwp_core_client", "textures/gui/loading.png");

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
        gui.blit(PWP_BG, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pwp_customRender(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        var font = com.pwp.coreclient.gui.theme.PWPTheme.Fonts.display();

        if (pwp_openTime == 0) {
            pwp_openTime = System.currentTimeMillis();
        }
        if (pwp_tips == null || w != pwp_lastWidth) {
            pwp_lastWidth = w;
            pwp_tips = new PWPTipsWidget((int) (w * 0.6f));
        }

        long elapsed = System.currentTimeMillis() - pwp_openTime;
        int cx = w / 2;
        int cy = h / 2;

        var pose = gui.pose();

        pose.pushPose();
        pose.translate(cx, (int) (h * 0.12f), 0);
        pose.scale(1.4f, 1.4f, 1f);
        gui.drawString(font, Component.literal("PWP"), -font.width("PWP") / 2, 0, PWPTheme.Colors.ACCENT, false);
        pose.popPose();

        String loadText = "Загрузка мира...";
        gui.drawString(font, Component.literal(loadText), cx - font.width(loadText) / 2, cy - 30, PWPTheme.Colors.TEXT_PRIMARY, false);
        PWPProgressBar.renderPulse(gui, cx - 60, cy, 120, 4, elapsed);

        if (pwp_tips != null) {
            pwp_tips.render(gui, cx, cy + 55);
        }
    }
}

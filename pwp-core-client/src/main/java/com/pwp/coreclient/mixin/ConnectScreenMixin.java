package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.screens.PWPTipsWidget;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Unique
    private long pwp_openTime;

    @Unique
    private PWPTipsWidget pwp_tips;

    @Unique
    private int pwp_lastW;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void pwp_renderBackground(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        ci.cancel();
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        PWPRotatingBackground.render(gui, 0, 0, w, h);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void pwp_overlay(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        var font = PWPTheme.Fonts.display();

        if (pwp_openTime == 0) pwp_openTime = System.currentTimeMillis();
        if (pwp_tips == null || w != pwp_lastW) {
            pwp_lastW = w;
            pwp_tips = new PWPTipsWidget((int) (w * 0.6f));
        }

        var pose = gui.pose();

        // PWP Logo
        pose.pushPose();
        pose.translate(cx, (int) (h * 0.12f), 0);
        pose.scale(1.6f, 1.6f, 1f);
        gui.drawString(font, Component.literal("PWP"), -font.width("PWP") / 2, 0, PWPTheme.Colors.ACCENT, false);
        pose.popPose();

        // Tips
        if (pwp_tips != null) {
            pwp_tips.render(gui, cx, h / 2 - 6);
        }
    }
}

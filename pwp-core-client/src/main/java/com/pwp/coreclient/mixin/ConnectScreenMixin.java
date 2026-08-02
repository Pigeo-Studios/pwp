package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.screens.PWPTipsWidget;
import com.pwp.coreclient.gui.screens.PWPUtils;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Shadow
    private Screen parent;

    @Unique
    private long pwp_openTime;

    @Unique
    private PWPTipsWidget pwp_tips;

    @Unique
    private int pwp_lastW;

    @Unique
    private PWPButton pwp_cancelButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void pwp_addCancelButton(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int boxY = h / 2 - 38;

        if (pwp_cancelButton == null) {
            pwp_cancelButton = new PWPButton(0, 0, 200, 20, Component.literal("Отмена"),
                    btn -> Minecraft.getInstance().setScreen(parent), PWPButton.Style.GHOST);
            ((ScreenInvoker) (Object) this).pwpInvokeAddRenderableWidget(pwp_cancelButton);
        }
        pwp_cancelButton.setPosition(w / 2 - 100, boxY + 54 + 40);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pwp_customRender(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        var font = PWPTheme.Fonts.display();

        PWPRotatingBackground.render(gui, 0, 0, w, h);

        if (pwp_openTime == 0) pwp_openTime = System.currentTimeMillis();
        if (pwp_tips == null || w != pwp_lastW) {
            pwp_lastW = w;
            pwp_tips = new PWPTipsWidget((int) (w * 0.6f));
        }

        long elapsed = System.currentTimeMillis() - pwp_openTime;

        var pose = gui.pose();

        // PWP Logo
        pose.pushPose();
        pose.translate(cx, (int) (h * 0.12f), 0);
        pose.scale(1.6f, 1.6f, 1f);
        gui.drawString(font, Component.literal("PWP"), -font.width("PWP") / 2, 0, PWPTheme.Colors.ACCENT, false);
        pose.popPose();

        // Content box: status + spinner
        int boxW = Math.min(280, w - 40);
        int boxH = 54;
        int boxY = h / 2 - 38;
        PWPUtils.renderBox(gui, cx, boxY, boxW, boxH);

        String connectText = "Подключение к серверу...";
        gui.drawString(font, Component.literal(connectText), cx - font.width(connectText) / 2, h / 2 - 30, PWPTheme.Colors.TEXT_PRIMARY, false);
        PWPUtils.renderSpinner(gui, cx, h / 2 + 4, elapsed);

        // Tips
        if (pwp_tips != null) {
            pwp_tips.render(gui, cx, h / 2 + 32);
        }

        // Cancel button
        if (pwp_cancelButton != null) {
            pwp_cancelButton.render(gui, mx, my, pt);
        }
    }
}

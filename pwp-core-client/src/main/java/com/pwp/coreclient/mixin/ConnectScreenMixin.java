package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.screens.PWPTipsWidget;
import com.pwp.coreclient.gui.screens.PWPUtils;
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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pwp_customRender(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;
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
        int boxY = cy - 38;
        PWPUtils.renderBox(gui, cx, boxY, boxW, boxH);

        String connectText = "Подключение к серверу...";
        gui.drawString(font, Component.literal(connectText), cx - font.width(connectText) / 2, cy - 30, PWPTheme.Colors.TEXT_PRIMARY, false);
        PWPUtils.renderSpinner(gui, cx, cy + 4, elapsed);

        // Tips
        if (pwp_tips != null) {
            pwp_tips.render(gui, cx, cy + 32);
        }

        // Cancel button at vanilla widget position (cx-50, cy+50)
        int btnW = 100;
        int btnH = 20;
        int btnX = cx - btnW / 2;
        int btnY = cy + 58;
        boolean btnHover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
        int btnR = com.pwp.coreclient.gui.theme.PWPTheme.Spacing.RADIUS_SMALL;
        int bg = btnHover ? PWPTheme.Colors.SURFACE_LIGHT : 0x00000000;
        int border = btnHover ? PWPTheme.Colors.BORDER_FOCUS : 0x00000000;
        int textCol = btnHover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM;
        com.pwp.coreclient.gui.components.RoundedRect.fill(gui, btnX, btnY, btnW, btnH, btnR, bg);
        if (btnHover) {
            com.pwp.coreclient.gui.components.RoundedRect.border(gui, btnX, btnY, btnW, btnH, btnR, 1, border);
        }
        String cancelText = "Отмена";
        gui.drawString(font, Component.literal(cancelText), cx - font.width(cancelText) / 2, btnY + 6, textCol, false);
    }
}

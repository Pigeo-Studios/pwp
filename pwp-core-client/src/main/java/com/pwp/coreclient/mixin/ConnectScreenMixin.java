package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.screens.PWPTipsWidget;
import com.pwp.coreclient.gui.screens.PWPUtils;
import com.pwp.coreclient.gui.components.RoundedRect;
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

    @Shadow
    public java.util.List<net.minecraft.client.gui.components.AbstractWidget> children() { return null; }

    @Unique
    private long pwp_openTime;

    @Unique
    private PWPTipsWidget pwp_tips;

    @Unique
    private int pwp_lastW;

    @Unique
    private int pwp_cancelX, pwp_cancelY, pwp_cancelW = 200, pwp_cancelH = 20;

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0
                && mx >= pwp_cancelX && mx <= pwp_cancelX + pwp_cancelW
                && my >= pwp_cancelY && my <= pwp_cancelY + pwp_cancelH) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        for (var child : children()) {
            if (child.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        return false;
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

        // Cancel button below content box
        pwp_cancelX = cx - pwp_cancelW / 2;
        pwp_cancelY = boxY + boxH + 22;
        boolean btnHover = mx >= pwp_cancelX && mx <= pwp_cancelX + pwp_cancelW
                        && my >= pwp_cancelY && my <= pwp_cancelY + pwp_cancelH;
        int btnR = PWPTheme.Spacing.RADIUS_SMALL;
        int bg = btnHover ? PWPTheme.Colors.SURFACE_LIGHT : 0x00000000;
        int border = btnHover ? PWPTheme.Colors.BORDER_FOCUS : 0x00000000;
        int textCol = btnHover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM;
        RoundedRect.fill(gui, pwp_cancelX, pwp_cancelY, pwp_cancelW, pwp_cancelH, btnR, bg);
        if (btnHover) {
            RoundedRect.border(gui, pwp_cancelX, pwp_cancelY, pwp_cancelW, pwp_cancelH, btnR, 1, border);
        }
        String cancelText = "Отмена";
        gui.drawString(font, Component.literal(cancelText), cx - font.width(cancelText) / 2, pwp_cancelY + 6, textCol, false);
    }
}

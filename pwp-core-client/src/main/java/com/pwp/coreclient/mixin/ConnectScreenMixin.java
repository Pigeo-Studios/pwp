package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPProgressBar;
import com.pwp.coreclient.gui.screens.PWPTipsWidget;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Unique
    private static final ResourceLocation PWP_BG = new ResourceLocation("pwp_core_client", "textures/gui/loading.png");

    @Unique
    private long pwp_openTime;

    @Unique
    private PWPTipsWidget pwp_tips;

    @Unique
    private int pwp_lastW;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void pwp_customBg(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        ci.cancel();
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        gui.blit(PWP_BG, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pwp_customRender(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        if (pwp_openTime == 0) pwp_openTime = System.currentTimeMillis();
        if (pwp_tips == null || w != pwp_lastW) {
            pwp_lastW = w;
            pwp_tips = new PWPTipsWidget((int) (w * 0.6f));
        }

        long elapsed = System.currentTimeMillis() - pwp_openTime;

        var font = PWPTheme.Fonts.display();
        var pose = gui.pose();

        pose.pushPose();
        pose.translate(cx, (int) (h * 0.12f), 0);
        pose.scale(1.4f, 1.4f, 1f);
        gui.drawCenteredString(font, Component.literal("PWP"), 0, 0, PWPTheme.Colors.ACCENT);
        pose.popPose();

        gui.drawCenteredString(font, Component.literal("РџРѕРґРєР»СЋС‡РµРЅРёРµ Рє СЃРµСЂРІРµСЂСѓ..."), cx, cy - 30, PWPTheme.Colors.TEXT_PRIMARY);
        PWPProgressBar.renderPulse(gui, cx - 60, cy, 120, 4, elapsed);

        if (pwp_tips != null) {
            pwp_tips.render(gui, cx, cy + 55);
        }

        int btnW = 100;
        int btnH = 20;
        int btnX = cx - btnW / 2;
        int btnY = h - 40;
        boolean btnHover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
        int btnR = PWPTheme.Spacing.RADIUS_SMALL;
        int bg = btnHover ? PWPTheme.Colors.SURFACE_LIGHT : 0x00000000;
        int border = btnHover ? PWPTheme.Colors.BORDER_FOCUS : 0x00000000;
        int textCol = btnHover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM;
        RoundedRect.fill(gui, btnX, btnY, btnW, btnH, btnR, bg);
        if (btnHover) {
            RoundedRect.border(gui, btnX, btnY, btnW, btnH, btnR, 1, border);
        }
        gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("РћС‚РјРµРЅР°"), cx, btnY + 6, textCol);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void pwp_onClick(double mx, double my, int button, CallbackInfo ci) {
        if (button != 0) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int h = mc.getWindow().getGuiScaledHeight();
        int btnX = cx - 50;
        int btnY = h - 40;
        if (mx >= btnX && mx <= btnX + 100 && my >= btnY && my <= btnY + 20) {
            mc.setScreen(new com.pwp.coreclient.gui.screens.PWPMainMenuScreen());
            ci.cancel();
        }
    }
}

package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin {

    @Shadow @Final
    private Component reason;

    @Unique
    private static final ResourceLocation PWP_BG = new ResourceLocation("pwp_core_client", "textures/gui/loading.png");

    @Unique
    private long pwp_openTime;

    @Unique
    private boolean pwp_showBack;

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
        long elapsed = System.currentTimeMillis() - pwp_openTime;

        var font = PWPTheme.Fonts.display();
        var pose = gui.pose();

        pose.pushPose();
        pose.translate(cx, (int) (h * 0.12f), 0);
        pose.scale(1.4f, 1.4f, 1f);
        gui.drawString(font, Component.literal("PWP"), -font.width("PWP") / 2, 0, PWPTheme.Colors.ACCENT, false);
        pose.popPose();

        float fade = Math.min(elapsed / 250.0F, 1);
        float eased = Easing.easeOutCubic(fade);

        gui.setColor(1, 1, 1, eased);
        gui.drawString(font, Component.literal("РЎРѕРµРґРёРЅРµРЅРёРµ СЂР°Р·РѕСЂРІР°РЅРѕ"), cx - font.width("РЎРѕРµРґРёРЅРµРЅРёРµ СЂР°Р·РѕСЂРІР°РЅРѕ") / 2, cy - 20, PWPTheme.Colors.DANGER, false);

        String reasonStr = reason.getString();
        if (!reasonStr.isEmpty()) {
            int maxW = (int) (w * 0.6f);
            if (font.width(reasonStr) > maxW) {
                reasonStr = font.plainSubstrByWidth(reasonStr, maxW - 4) + "...";
            }
            gui.drawString(font, Component.literal(reasonStr), cx - font.width(reasonStr) / 2, cy, PWPTheme.Colors.TEXT_SECONDARY, false);
        }
        gui.setColor(1, 1, 1, 1);

        pwp_showBack = eased >= 1;
        if (pwp_showBack) {
            int btnX = cx - 80;
            int btnY = cy + 50;
            int btnW = 160;
            int btnH = 28;
            int btnR = PWPTheme.Spacing.RADIUS_SMALL;
            int bg = PWPTheme.Styles.Button.ACCENT_BG;
            int border = PWPTheme.Colors.ACCENT_DIM;
            int textColor = PWPTheme.Styles.Button.ACCENT_TEXT;
            boolean hover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
            if (hover) {
                bg = PWPTheme.Styles.Button.ACCENT_HOVER;
            }
            com.pwp.coreclient.gui.components.RoundedRect.fill(gui, btnX, btnY, btnW, btnH, btnR, bg);
            com.pwp.coreclient.gui.components.RoundedRect.border(gui, btnX, btnY, btnW, btnH, btnR, 1, border);
            gui.drawString(font, Component.literal("Р’РµСЂРЅСѓС‚СЊСЃСЏ РІ РјРµРЅСЋ"), cx - font.width("Р’РµСЂРЅСѓС‚СЊСЃСЏ РІ РјРµРЅСЋ") / 2, btnY + 10, textColor, false);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void pwp_onClick(double mx, double my, int button, CallbackInfo ci) {
        if (button != 0 || !pwp_showBack) return;
        int cx = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int btnX = cx - 80;
        int btnY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 + 50;
        if (mx >= btnX && mx <= btnX + 160 && my >= btnY && my <= btnY + 28) {
            Minecraft.getInstance().setScreen(new com.pwp.coreclient.gui.screens.PWPMainMenuScreen());
            ci.cancel();
        }
    }
}

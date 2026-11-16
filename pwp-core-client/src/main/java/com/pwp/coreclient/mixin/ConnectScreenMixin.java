package com.pwp.coreclient.mixin;

import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.screens.PWPLoadingView;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.layout.Rect;
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
    private PWPLoadingView pwp_view;

    @Unique
    private PWPButton pwp_cancelButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void pwp_addCancelButton(CallbackInfo ci) {
        if (pwp_cancelButton == null) {
            pwp_cancelButton = new PWPButton(
                    0,
                    0,
                    PWPTheme.Spacing.BUTTON_WIDTH,
                    PWPTheme.Spacing.BUTTON_HEIGHT,
                    Component.translatable("pwp_core.ui.cancel"),
                    btn -> Minecraft.getInstance().setScreen(((ConnectScreenAccessor) (Object) this).pwpGetParent()),
                    PWPButton.Style.GHOST);
            // Регистрация через addRenderableWidget: рефмап-запись для
            // @Invoker вписывается в build.gradle (compileJava.doLast).
            ((ScreenInvoker) (Object) this).pwpInvokeAddRenderableWidget(pwp_cancelButton);
        }
        // позиция — из раскладки PWPLoadingView, пересчитывается в render
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pwp_customRender(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        var font = PWPTheme.Fonts.display();

        PWPRotatingBackground.render(gui, 0, 0, w, h);
        if (pwp_view == null) pwp_view = new PWPLoadingView();

        Component cancel = Component.translatable("pwp_core.ui.cancel");
        PWPLoadingView.Layout lay = pwp_view.render(
                gui, font, w, h, UIIcon.SERVER, Component.translatable("pwp_core.loading.connecting"), -1F, cancel);

        if (pwp_cancelButton != null) {
            Rect b = lay.button();
            pwp_cancelButton.setX(b.x());
            pwp_cancelButton.setY(b.y());
            pwp_cancelButton.setWidth(b.w());
            pwp_cancelButton.setHeight(b.h());
            pwp_cancelButton.render(gui, mx, my, pt);
        }
    }
}

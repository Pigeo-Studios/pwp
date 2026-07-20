package com.pwp.coreclient.mixin;

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
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pwp_customOverlay(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        int cx = w / 2;
        int cy = h / 2;

        long elapsed = System.currentTimeMillis() - pwp_openTime;
        float pulse = (float) (0.5 + 0.5 * Math.sin(elapsed * Math.PI * 2 / 1200.0));

        int dots = ((int) (elapsed / 400) % 4);
        String dotStr = ".".repeat(dots);

        Component title = Component.translatable("pwp_core.loading.title");
        Component subtitle = Component.literal(
            Component.translatable("pwp_core.loading.subtitle").getString() + dotStr
        );

        var font = mc.font;
        gui.drawCenteredString(font, title, cx, cy - 30, PWPTheme.Colors.TEXT_PRIMARY);
        gui.drawCenteredString(font, subtitle, cx, cy - 12, PWPTheme.Colors.TEXT_SECONDARY);

        int barWidth = 180;
        int barHeight = 4;
        int barX = cx - barWidth / 2;
        int barY = cy + 4;

        float progress = (float) ((elapsed % 3000) / 3000.0);
        int fillW = (int) (barWidth * progress);

        gui.fill(barX, barY, barX + barWidth, barY + barHeight, PWPTheme.Colors.SURFACE_DIM);
        gui.fill(barX, barY, barX + fillW, barY + barHeight, PWPTheme.Colors.ACCENT);

        int glowAlpha = (int) (50 + 30 * pulse);
        int glowColor = (Math.min(255, glowAlpha) << 24) | (PWPTheme.Colors.ACCENT & 0x00FFFFFF);
        gui.fill(barX, barY - 1, barX + barWidth, barY, glowColor);
        gui.fill(barX, barY + barHeight, barX + barWidth, barY + barHeight + 1, glowColor);

        String info = (int)(progress * 100) + "%";
        gui.drawCenteredString(font, Component.literal(info), cx, barY + barHeight + 6, PWPTheme.Colors.TEXT_DIM);
    }
}

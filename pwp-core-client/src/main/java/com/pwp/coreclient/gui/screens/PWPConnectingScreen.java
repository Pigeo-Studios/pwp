package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PWPConnectingScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("pwp_core_client", "textures/gui/main_menu.png");
    private static final long CONNECT_TIMEOUT_MS = 12000;
    private static final long ERROR_FADE_MS = 250;

    private final long openTime;
    private boolean errorState;
    private long errorTime;
    private boolean widgetsBuilt;

    private PWPTipsWidget tipsWidget;

    public PWPConnectingScreen() {
        super(Component.literal("Подключение"));
        openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        widgetsBuilt = false;
        errorState = false;
        tipsWidget = new PWPTipsWidget((int) (width * 0.6f));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        PWPUtils.renderLogo(gui, width / 2, (int) (height * 0.16f));

        long now = System.currentTimeMillis();
        long elapsed = now - openTime;

        if (errorState) {
            renderError(gui, mouseX, mouseY, now);
            return;
        }

        if (elapsed > CONNECT_TIMEOUT_MS) {
            enterErrorState();
            return;
        }

        int cx = width / 2;
        int cy = height / 2;

        PWPUtils.renderSpinner(gui, cx, cy - 14, elapsed);
        PWPUtils.renderStatusText(gui, "Подключение...", cx, cy, elapsed);
        PWPUtils.renderProgressBar(gui, cx, cy + 55, (int) (width * 0.3f), 4, elapsed);
        tipsWidget.render(gui, cx, cy + 30);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = width;
        int h = height;
        gui.blit(BG_TEXTURE, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void enterErrorState() {
        if (errorState) return;
        errorState = true;
        errorTime = System.currentTimeMillis();
    }

    private void renderError(GuiGraphics gui, int mouseX, int mouseY, long now) {
        int cx = width / 2;
        int cy = height / 2 - 10;

        if (!widgetsBuilt) {
            widgetsBuilt = true;
            errorTime = now;
        }

        float fade = Math.min((now - errorTime) / (float) ERROR_FADE_MS, 1);
        fade = Easing.easeOutCubic(fade);

        var font = Minecraft.getInstance().font;
        int titleColor = PWPUtils.withAlpha(PWPTheme.Colors.DANGER, (int) (fade * 255));
        gui.drawCenteredString(font, Component.literal("Не удалось подключиться"), cx, cy, titleColor);

        int subColor = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, (int) (fade * 200));
        gui.drawCenteredString(font, Component.literal("Сервер недоступен"), cx, cy + 14, subColor);

        if (fade >= 1 && children().isEmpty()) {
            int btnW = 200;
            int btnH = 34;
            addRenderableWidget(new PWPButton(
                cx - btnW / 2, cy + 40, btnW, btnH,
                Component.translatable("pwp_core.ui.back"),
                btn -> Minecraft.getInstance().setScreen(new PWPMainMenuScreen()),
                PWPButton.Style.DARK
            ));
        }

        super.render(gui, mouseX, mouseY, 0);
    }
}

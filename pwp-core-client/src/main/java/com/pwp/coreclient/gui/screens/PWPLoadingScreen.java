package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPProgressBar;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PWPLoadingScreen extends Screen {

    public enum Context {
        CONNECTING, LOADING_WORLD, LOADING_MAP, WAITING_DATA, DISCONNECTING, CHANGING_DIMENSION, TRANSFERRING
    }

    private final Context context;
    private final long openTime;
    private final PWPProgressBar progressBar = new PWPProgressBar();

    private PWPTipsWidget tipsWidget;
    private int prevWidth;

    private boolean hasKnownProgress;
    private boolean errorState;
    private long errorTime;
    private boolean widgetsBuilt;

    private long timeoutMs;

    public PWPLoadingScreen(Context context) {
        super(Component.literal(getContextText(context)));
        this.context = context;
        this.openTime = System.currentTimeMillis();
        this.timeoutMs = switch (context) {
            case CONNECTING -> 12000;
            case LOADING_WORLD -> 60000;
            case LOADING_MAP -> 30000;
            case WAITING_DATA -> 8000;
            case DISCONNECTING -> 5000;
            case CHANGING_DIMENSION -> 30000;
            case TRANSFERRING -> 15000;
        };
    }

    @Override
    protected void init() {
        super.init();
    }

    public void setProgress(float progress) {
        this.hasKnownProgress = true;
        progressBar.setProgress(progress);
    }

    public void setError(String message) {
        if (errorState) return;
        errorState = true;
        errorTime = System.currentTimeMillis();
    }

    public boolean isErrorState() {
        return errorState;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        long now = System.currentTimeMillis();
        long elapsed = now - openTime;
        var font = PWPTheme.Fonts.display();

        if (!errorState && timeoutMs > 0 && elapsed > timeoutMs) {
            setError("Превышено время ожидания");
        }

        int cx = width / 2;
        int cy = height / 2;

        var pose = gui.pose();

        // PWP Logo
        pose.pushPose();
        pose.translate(cx, (int) (height * 0.12f), 0);
        pose.scale(1.6f, 1.6f, 1f);
        gui.drawString(font, Component.literal("PWP"), -font.width("PWP") / 2, 0, PWPTheme.Colors.ACCENT, false);
        pose.popPose();

        if (errorState) {
            renderError(gui, mouseX, mouseY, now);
            super.render(gui, mouseX, mouseY, partialTick);
            return;
        }

        // Content box
        int boxW = Math.min(280, width - 40);
        int boxH = hasKnownProgress ? 54 : 54;
        int boxY = cy - 38;
        PWPUtils.renderBox(gui, cx, boxY, boxW, boxH);

        String statusText = getContextText(context);
        gui.drawString(font, Component.literal(statusText), cx - font.width(statusText) / 2, cy - 30, PWPTheme.Colors.TEXT_PRIMARY, false);

        if (hasKnownProgress) {
            int barW = (int) (boxW * 0.7f);
            progressBar.render(gui, cx - barW / 2, cy + 4, barW, 4, now);
        } else {
            PWPUtils.renderSpinner(gui, cx, cy + 4, elapsed);
        }

        // Tips
        if (tipsWidget == null || width != prevWidth) {
            prevWidth = width;
            tipsWidget = new PWPTipsWidget((int) (width * 0.6f));
        }
        tipsWidget.render(gui, cx, cy + 32);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        PWPRotatingBackground.render(gui, 0, 0, w, h);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderError(GuiGraphics gui, int mouseX, int mouseY, long now) {
        int cx = width / 2;
        int cy = height / 2 - 10;
        var font = PWPTheme.Fonts.display();

        if (!widgetsBuilt) {
            widgetsBuilt = true;
        }

        long errorElapsed = now - errorTime;
        float fade = Math.min(errorElapsed / 250.0F, 1);
        fade = Easing.easeOutCubic(fade);

        int boxW = Math.min(280, width - 40);
        int boxH = 64;
        int boxY = cy - 20;
        PWPUtils.renderBox(gui, cx, boxY, boxW, boxH);

        gui.setColor(1, 1, 1, fade);
        gui.drawString(font, Component.literal("Не удалось подключиться"), cx - font.width("Не удалось подключиться") / 2, cy, PWPTheme.Colors.DANGER, false);
        gui.drawString(font, Component.literal("Сервер недоступен"), cx - font.width("Сервер недоступен") / 2, cy + 14, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.setColor(1, 1, 1, 1);

        if (fade >= 1 && children().isEmpty()) {
            addRenderableWidget(new PWPButton(
                cx - 100, cy + 40, 200, 28,
                Component.translatable("pwp_core.ui.back"),
                btn -> Minecraft.getInstance().setScreen(new PWPMainMenuScreen()),
                PWPButton.Style.DARK
            ));
        }
    }

    private static String getContextText(Context ctx) {
        return switch (ctx) {
            case CONNECTING -> "Подключение к серверу...";
            case LOADING_WORLD -> "Загрузка мира...";
            case LOADING_MAP -> "Загрузка карты...";
            case WAITING_DATA -> "Загрузка...";
            case DISCONNECTING -> "Отключение...";
            case CHANGING_DIMENSION -> "Переход между мирами...";
            case TRANSFERRING -> "Переключение сервера...";
        };
    }
}

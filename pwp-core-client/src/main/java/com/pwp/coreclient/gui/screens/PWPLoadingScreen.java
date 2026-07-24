package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPProgressBar;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Random;

public class PWPLoadingScreen extends Screen {

    public enum Context {
        CONNECTING, LOADING_WORLD, LOADING_MAP, WAITING_DATA, DISCONNECTING, CHANGING_DIMENSION, TRANSFERRING
    }

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("pwp_core_client", "textures/gui/loading.png");
    private static final List<String> TIPS = List.of(
        "Совет: используйте тактическое оборудование для победы",
        "Совет: связь с отрядом — ключ к успеху",
        "Совет: следите за уровнем брони и здоровья",
        "Совет: захватывайте точки чтобы получить преимущество",
        "Совет: техника уязвима с тыла и флангов",
        "Совет: аптечки восстанавливают здоровье",
        "Совет: боеприпасы можно пополнить на точке",
        "Совет: не забывайте перезаряжаться перед боем"
    );

    private final Context context;
    private final long openTime;
    private final Random random = new Random();
    private final PWPProgressBar progressBar = new PWPProgressBar();

    private boolean hasKnownProgress;
    private boolean errorState;
    private long errorTime;
    private boolean widgetsBuilt;

    private int currentTipIndex;
    private long tipStateStart;
    private boolean tipFading;
    private long timeoutMs;

    public PWPLoadingScreen(Context context) {
        super(Component.literal(getContextText(context)));
        this.context = context;
        this.openTime = System.currentTimeMillis();
        this.currentTipIndex = random.nextInt(TIPS.size());
        this.tipStateStart = System.currentTimeMillis();
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
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        long now = System.currentTimeMillis();
        long elapsed = now - openTime;

        if (!errorState && timeoutMs > 0 && elapsed > timeoutMs) {
            setError("Превышено время ожидания");
        }

        int cx = width / 2;
        int cy = height / 2;

        var font = Minecraft.getInstance().font;
        var pose = gui.pose();

        // Small logo at top
        pose.pushPose();
        pose.translate(cx, (int) (height * 0.12f), 0);
        pose.scale(1.4f, 1.4f, 1f);
        gui.drawCenteredString(font, Component.literal("PWP"), 0, 0, PWPTheme.Colors.ACCENT);
        pose.popPose();

        if (errorState) {
            renderError(gui, mouseX, mouseY, now);
            super.render(gui, mouseX, mouseY, partialTick);
            return;
        }

        // Status text
        String statusText = getContextText(context);
        gui.drawCenteredString(font, Component.literal(statusText), cx, cy - 30, PWPTheme.Colors.TEXT_PRIMARY);

        // Progress indicator
        if (hasKnownProgress) {
            int barW = (int) (width * 0.3f);
            progressBar.render(gui, cx - barW / 2, cy, barW, 4, now);
        } else {
            PWPProgressBar.renderPulse(gui, cx - 60, cy, 120, 4, elapsed);
        }

        // Tips
        renderTip(gui, cx, cy + 30, now);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        gui.blit(BG_TEXTURE, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
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

        if (!widgetsBuilt) {
            widgetsBuilt = true;
        }

        long errorElapsed = now - errorTime;
        float fade = Math.min(errorElapsed / 250.0F, 1);
        fade = Easing.easeOutCubic(fade);

        var font = Minecraft.getInstance().font;
        gui.setColor(1, 1, 1, fade);
        gui.drawCenteredString(font, Component.literal("Не удалось подключиться"), cx, cy, PWPTheme.Colors.DANGER);
        gui.drawCenteredString(font, Component.literal("Сервер недоступен"), cx, cy + 14, PWPTheme.Colors.TEXT_SECONDARY);
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

    private void renderTip(GuiGraphics gui, int cx, int y, long now) {
        long tipElapsed = now - tipStateStart;

        if (!tipFading && tipElapsed > 4000) {
            tipFading = true;
            tipStateStart = now;
        } else if (tipFading && tipElapsed > 300) {
            tipFading = false;
            int next;
            do {
                next = random.nextInt(TIPS.size());
            } while (next == currentTipIndex);
            currentTipIndex = next;
            tipStateStart = now;
        }

        float alpha;
        if (tipFading) {
            float fadeProgress = Math.min(tipElapsed / 300.0F, 1);
            alpha = 1.0f - Easing.easeOutCubic(fadeProgress);
        } else {
            alpha = 1.0f;
        }

        var font = Minecraft.getInstance().font;
        String text = TIPS.get(currentTipIndex);
        int color = PWPTheme.Colors.TEXT_SECONDARY;
        float originalAlpha = (color >> 24) & 0xFF;
        int finalAlpha = Math.min(255, Math.max(0, (int) (alpha * originalAlpha)));
        int displayColor = (finalAlpha << 24) | (color & 0x00FFFFFF);

        gui.drawCenteredString(font, Component.literal(text), cx, y, displayColor);
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

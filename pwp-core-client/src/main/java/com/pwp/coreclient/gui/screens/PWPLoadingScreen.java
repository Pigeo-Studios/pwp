package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран загрузки/подключения PWP. Вид — {@link PWPLoadingView} (общий с ванильными экранами
 * через миксины). Поведение прежнее: таймаут по контексту переводит в состояние ошибки,
 * кнопка «Назад» (появляется после фейда ошибки) возвращает в главное меню; ESC не закрывает.
 */
public class PWPLoadingScreen extends Screen {

    public enum Context {
        CONNECTING,
        LOADING_WORLD,
        LOADING_MAP,
        WAITING_DATA,
        DISCONNECTING,
        CHANGING_DIMENSION,
        TRANSFERRING
    }

    private static final long ERROR_FADE_MS = 250;

    private final Context context;
    private final long openTime;
    private final PWPLoadingView view = new PWPLoadingView();
    private final UILayer layer = new UILayer();
    private UIButton backButton;

    private boolean hasKnownProgress;
    private float progress;
    private boolean errorState;
    private long errorTime;

    private final long timeoutMs;

    public PWPLoadingScreen(Context context) {
        super(getContextText(context));
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
        layer.clear();
        backButton = layer.add(new UIButton(Component.translatable("pwp_core.ui.back"), () -> Minecraft.getInstance()
                        .setScreen(new PWPMainMenuScreen()))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.BACK));
        backButton.setVisible(false);
    }

    public void setProgress(float progress) {
        this.hasKnownProgress = true;
        this.progress = Math.max(0F, Math.min(1F, progress));
    }

    public void setError(String message) {
        if (errorState) return;
        errorState = true;
        errorTime = System.currentTimeMillis();
    }

    public boolean isErrorState() {
        return errorState;
    }

    private float errorFade() {
        if (!errorState) return 0F;
        return Easing.easeOutCubic(Math.min((System.currentTimeMillis() - errorTime) / (float) ERROR_FADE_MS, 1F));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        long elapsed = System.currentTimeMillis() - openTime;
        if (!errorState && timeoutMs > 0 && elapsed > timeoutMs) {
            setError("timeout");
        }
        var font = PWPTheme.Fonts.display();
        if (errorState) {
            float fade = errorFade();
            Component back = Component.translatable("pwp_core.ui.back");
            PWPLoadingView.Layout lay = view.renderError(
                    gui,
                    font,
                    width,
                    height,
                    Component.translatable("pwp_core.loading.error_title"),
                    Component.translatable("pwp_core.loading.error_hint"),
                    fade,
                    back);
            backButton.setBounds(lay.button());
            backButton.setVisible(true);
            backButton.setAppearance(fade, 0, 0);
        } else {
            view.render(
                    gui,
                    font,
                    width,
                    height,
                    contextIcon(context),
                    getContextText(context),
                    hasKnownProgress ? progress : -1F,
                    null);
        }
        layer.render(gui, mouseX, mouseY, partialTick, width, height);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // кнопка кликабельна только после полного проявления (как раньше — её не было до фейда)
        if (errorState && errorFade() >= 1F && layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
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

    private static UIIcon contextIcon(Context ctx) {
        return switch (ctx) {
            case CONNECTING, TRANSFERRING, DISCONNECTING -> UIIcon.SERVER;
            default -> UIIcon.INFO;
        };
    }

    private static Component getContextText(Context ctx) {
        return Component.translatable(
                switch (ctx) {
                    case CONNECTING -> "pwp_core.loading.connecting";
                    case LOADING_WORLD -> "pwp_core.loading.loading_world";
                    case LOADING_MAP -> "pwp_core.loading.loading_map";
                    case WAITING_DATA -> "pwp_core.loading.waiting_data";
                    case DISCONNECTING -> "pwp_core.loading.disconnecting";
                    case CHANGING_DIMENSION -> "pwp_core.loading.changing_dimension";
                    case TRANSFERRING -> "pwp_core.loading.transferring";
                });
    }
}

package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPBadge;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.ClientConnectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PWPMainMenuScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("pwp_core_client", "textures/gui/main_menu.png");
    private static final int BG_W = 1920;
    private static final int BG_H = 1080;

    private final long openTime;
    private boolean connecting;

    private enum ServerStatus { UNKNOWN, ONLINE, OFFLINE, MAINTENANCE }
    private ServerStatus serverStatus = ServerStatus.UNKNOWN;

    private PWPButton playBtn;

    public PWPMainMenuScreen() {
        super(Component.literal("PWP"));
        openTime = System.currentTimeMillis();
    }

    public void setServerStatus(ServerStatus status) {
        this.serverStatus = status;
    }

    @Override
    protected void init() {
        super.init();

        int cx = width / 2;
        int btnW = Math.min(width / 4, 220);
        int btnH = 28;
        int gap = 6;

        int totalBtnH = 4 * btnH + 3 * gap;
        int startY = (int) (height * 0.52f);
        if (startY + totalBtnH > height - 60) {
            startY = height - 60 - totalBtnH;
        }

        boolean canPlay = serverStatus != ServerStatus.OFFLINE && serverStatus != ServerStatus.MAINTENANCE;

        playBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY, btnW, btnH,
            getPlayButtonText(),
            btn -> {
                if (connecting || !canPlay) return;
                connecting = true;
                Minecraft.getInstance().setScreen(new PWPLoadingScreen(PWPLoadingScreen.Context.CONNECTING));
                ClientConnectHandler.connect("pigeo.asuscomm.com", 25565);
            },
            canPlay ? PWPButton.Style.ACCENT : PWPButton.Style.DARK
        ));
        playBtn.active = canPlay;

        addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + btnH + gap, btnW, btnH,
            Component.translatable("pwp_core.main_menu.singleplayer"),
            btn -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this)),
            PWPButton.Style.PRIMARY
        ));

        addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + (btnH + gap) * 2, btnW, btnH,
            Component.translatable("pwp_core.main_menu.settings"),
            btn -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)),
            PWPButton.Style.DARK
        ));

        addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + (btnH + gap) * 3, btnW, btnH,
            Component.translatable("pwp_core.main_menu.quit"),
            btn -> Minecraft.getInstance().stop(),
            PWPButton.Style.DANGER
        ));
    }

    private Component getPlayButtonText() {
        return switch (serverStatus) {
            case OFFLINE -> Component.literal("Сервер недоступен");
            case MAINTENANCE -> Component.literal("Технические работы");
            case ONLINE -> Component.translatable("pwp_core.main_menu.play");
            default -> Component.translatable("pwp_core.main_menu.play");
        };
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        long elapsed = System.currentTimeMillis() - openTime;
        int cx = width / 2;

        float logoFade = animFade(elapsed, 0, 300);
        if (logoFade > 0) {
            renderLogo(gui, cx, (int) (height * 0.16f), logoFade);
        }

        float welcomeFade = animFade(elapsed, 200, 300);
        if (welcomeFade > 0) {
            renderWelcome(gui, cx, welcomeFade);
        }

        renderButtons(gui, mouseX, mouseY, partialTick, elapsed);

        float statusFade = animFade(elapsed, 600, 300);
        if (statusFade > 0) {
            renderServerStatus(gui, cx, statusFade);
        }

        float footerFade = animFade(elapsed, 500, 300);
        if (footerFade > 0) {
            gui.setColor(1, 1, 1, footerFade);
            PWPLayout.renderFooter(gui, "PWP v1.0.1", width, height);
            gui.setColor(1, 1, 1, 1);
        }
    }

    private void renderServerStatus(GuiGraphics gui, int cx, float fade) {
        int y = (int) (height * 0.72f);
        gui.setColor(1, 1, 1, fade);

        String text;
        int color;
        switch (serverStatus) {
            case ONLINE -> { text = "Сервер: ONLINE"; color = PWPTheme.Colors.SUCCESS; }
            case OFFLINE -> { text = "Сервер недоступен"; color = PWPTheme.Colors.DANGER; }
            case MAINTENANCE -> { text = "Технические работы"; color = PWPTheme.Colors.WARNING; }
            default -> { text = "Проверка подключения..."; color = PWPTheme.Colors.TEXT_DIM; }
        }

        gui.drawCenteredString(font, Component.literal(text), cx, y, color);

        if (serverStatus == ServerStatus.OFFLINE || serverStatus == ServerStatus.MAINTENANCE) {
            gui.drawCenteredString(font, Component.literal("Попробуйте позже"), cx, y + 12, PWPTheme.Colors.TEXT_DIM);
        }

        gui.setColor(1, 1, 1, 1);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        float scale = Math.max((float) w / BG_W, (float) h / BG_H);
        int drawW = Math.round(BG_W * scale);
        int drawH = Math.round(BG_H * scale);
        int drawX = (w - drawW) / 2;
        int drawY = (h - drawH) / 2;

        gui.blit(BG_TEXTURE, drawX, drawY, drawW, drawH, 0, 0, BG_W, BG_H, BG_W, BG_H);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void renderLogo(GuiGraphics gui, int cx, int y, float fade) {
        gui.setColor(1, 1, 1, fade);
        var font = Minecraft.getInstance().font;
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(cx, y, 0);
        float scale = PWPTheme.Fonts.SIZE_TITLE / 8f / 2f;
        pose.scale(scale, scale, 1f);
        gui.drawCenteredString(font, Component.literal("PWP"), 0, 0, PWPTheme.Colors.ACCENT);
        pose.popPose();
        gui.setColor(1, 1, 1, 1);
    }

    private void renderWelcome(GuiGraphics gui, int cx, float fade) {
        var font = Minecraft.getInstance().font;
        Minecraft mc = Minecraft.getInstance();

        String nickname = mc.player != null ? mc.player.getScoreboardName() : null;
        String welcome = nickname != null
            ? Component.translatable("pwp_core.main_menu.welcome", nickname).getString()
            : "Добро пожаловать!";

        int welcomeY = (int) (height * 0.34f);
        int slideY = (int) ((1 - fade) * 6);

        var pose = gui.pose();
        pose.pushPose();
        pose.translate(0, slideY, 0);

        gui.drawCenteredString(font, Component.literal(welcome), cx, welcomeY, PWPTheme.Colors.TEXT_PRIMARY);
        gui.drawCenteredString(font, Component.translatable("pwp_core.main_menu.subtitle"), cx, welcomeY + 16, PWPTheme.Colors.TEXT_SECONDARY);

        pose.popPose();
    }

    private void renderButtons(GuiGraphics gui, int mouseX, int mouseY, float partialTick, float elapsed) {
        var btns = renderables;
        int startDelay = 300;

        for (int i = 0; i < btns.size(); i++) {
            var w = btns.get(i);
            if (!(w instanceof PWPButton btn)) continue;

            float progress = animFade(elapsed, startDelay + i * 50, 250);
            if (progress <= 0) { btn.visible = false; continue; }

            float eased = Math.min(progress, 1);
            int slideY = (int) ((1 - eased) * 12);
            btn.setAnimAlpha(eased);
            btn.visible = true;

            var pose = gui.pose();
            pose.pushPose();
            pose.translate(0, slideY, 0);
            btn.render(gui, mouseX, mouseY, partialTick);
            pose.popPose();
        }
    }

    private static float animFade(float elapsed, long delay, long duration) {
        float t = (elapsed - delay) / (float) duration;
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return Easing.easeOutCubic(t);
    }
}

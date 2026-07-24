package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
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

    private final long openTime;
    private boolean connecting;

    private PWPButton playBtn;
    private PWPButton singleBtn;
    private PWPButton optionsBtn;
    private PWPButton quitBtn;

    public PWPMainMenuScreen() {
        super(Component.literal("PWP"));
        openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        int cx = width / 2;
        int btnW = Math.min(width / 4, 220);
        int btnH = 28;
        int gap = 6;

        int totalH = 4 * btnH + 3 * gap;
        int startY = Math.min((int) (height * 0.52f), height - 40 - totalH);

        playBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY, btnW, btnH,
            Component.translatable("pwp_core.main_menu.play"),
            btn -> {
                if (connecting) return;
                connecting = true;
                Minecraft.getInstance().setScreen(new PWPLoadingScreen(PWPLoadingScreen.Context.CONNECTING));
                ClientConnectHandler.connect("pigeo.asuscomm.com", 25565);
            },
            PWPButton.Style.ACCENT
        ));

        singleBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + btnH + gap, btnW, btnH,
            Component.translatable("pwp_core.main_menu.singleplayer"),
            btn -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this)),
            PWPButton.Style.PRIMARY
        ));

        optionsBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + (btnH + gap) * 2, btnW, btnH,
            Component.translatable("pwp_core.main_menu.settings"),
            btn -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)),
            PWPButton.Style.DARK
        ));

        quitBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + (btnH + gap) * 3, btnW, btnH,
            Component.translatable("pwp_core.main_menu.quit"),
            btn -> Minecraft.getInstance().stop(),
            PWPButton.Style.DANGER
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        long elapsed = System.currentTimeMillis() - openTime;
        int cx = width / 2;

        float logoProgress = animProgress(elapsed, 0, 300);
        if (logoProgress > 0) {
            float fade = Easing.easeOutCubic(Math.min(logoProgress, 1));
            renderLogo(gui, cx, (int) (height * 0.16f), fade);
        }

        float welcomeProgress = animProgress(elapsed, 100, 300);
        if (welcomeProgress > 0) {
            float fade = Easing.easeOutCubic(Math.min(welcomeProgress, 1));
            renderWelcome(gui, cx, fade);
        }

        renderButtons(gui, mouseX, mouseY, partialTick, elapsed);

        float footerProgress = animProgress(elapsed, 300 + 4 * 60 + 100, 300);
        if (footerProgress > 0) {
            float fade = Easing.easeOutCubic(Math.min(footerProgress, 1));
            gui.setColor(1, 1, 1, fade);
            PWPLayout.renderFooter(gui, "PWP v1.0.1", width, height);
            gui.setColor(1, 1, 1, 1);
        }
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

    private void renderLogo(GuiGraphics gui, int cx, int y, float fade) {
        gui.setColor(1, 1, 1, fade);
        var font = Minecraft.getInstance().font;
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(cx, y, 0);
        pose.scale(2.2f, 2.2f, 1f);
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

        int textColor = PWPTheme.Colors.TEXT_PRIMARY;
        int subColor = PWPTheme.Colors.TEXT_SECONDARY;

        gui.drawCenteredString(font, Component.literal(welcome), cx, welcomeY, textColor);
        gui.drawCenteredString(font, Component.translatable("pwp_core.main_menu.subtitle"), cx, welcomeY + 16, subColor);

        pose.popPose();
    }

    private void renderButtons(GuiGraphics gui, int mouseX, int mouseY, float partialTick, float elapsed) {
        PWPButton[] btns = { playBtn, singleBtn, optionsBtn, quitBtn };
        int startDelay = 300;

        for (int i = 0; i < btns.length; i++) {
            float progress = animProgress(elapsed, startDelay + i * 60, 250);

            if (progress <= 0) {
                btns[i].visible = false;
                continue;
            }

            float t = Math.min(progress, 1);
            float eased = Easing.easeOutBack(t);
            int slideY = (int) ((1 - eased) * 14);

            btns[i].setAnimAlpha(eased);
            btns[i].visible = true;

            var pose = gui.pose();
            pose.pushPose();
            pose.translate(0, slideY, 0);
            btns[i].render(gui, mouseX, mouseY, partialTick);
            pose.popPose();
        }
    }

    private float animProgress(float elapsed, long delay, long duration) {
        float t = (elapsed - delay) / (float) duration;
        return Math.min(t, 1);
    }
}

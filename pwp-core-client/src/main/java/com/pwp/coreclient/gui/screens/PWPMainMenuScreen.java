package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.ClientConnectHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PWPMainMenuScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("pwp_core_client", "textures/gui/main_menu.png");

    private static final int BTN_HEIGHT = 34;
    private static final int BTN_GAP = 8;
    private static final int ANIM_DELAY_PER_MS = 80;
    private static final int ANIM_BTN_DURATION = 250;
    private static final int ANIM_WELCOME_DURATION = 300;
    private static final int ANIM_LOGO_DURATION = 300;

    private final long openTime;
    private boolean connecting;

    private int btnWidth;
    private int startY;

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
        btnWidth = Mth.clamp(width / 4, 200, 240);

        int totalBlock = 4 * BTN_HEIGHT + 3 * BTN_GAP;
        int footerReserve = 30;
        startY = (int) (height * 0.52f);
        if (startY + totalBlock > height - footerReserve) {
            startY = height - footerReserve - totalBlock;
        }

        playBtn = addRenderableWidget(new PWPButton(
            cx - btnWidth / 2, startY, btnWidth, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.play"),
            btn -> {
                if (connecting) return;
                connecting = true;
                Minecraft.getInstance().setScreen(new PWPConnectingScreen());
                ClientConnectHandler.connect("pigeo.asuscomm.com", 25565);
            },
            PWPButton.Style.ACCENT
        ));

        singleBtn = addRenderableWidget(new PWPButton(
            cx - btnWidth / 2, startY + BTN_HEIGHT + BTN_GAP, btnWidth, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.singleplayer"),
            btn -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this)),
            PWPButton.Style.PRIMARY
        ));

        optionsBtn = addRenderableWidget(new PWPButton(
            cx - btnWidth / 2, startY + (BTN_HEIGHT + BTN_GAP) * 2, btnWidth, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.settings"),
            btn -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)),
            PWPButton.Style.DARK
        ));

        quitBtn = addRenderableWidget(new PWPButton(
            cx - btnWidth / 2, startY + (BTN_HEIGHT + BTN_GAP) * 3, btnWidth, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.quit"),
            btn -> Minecraft.getInstance().stop(),
            PWPButton.Style.DANGER
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        float elapsed = (float) (System.currentTimeMillis() - openTime);
        int cx = width / 2;
        int cy = height / 2;

        renderElement(gui, elapsed, 0, ANIM_LOGO_DURATION, (fade) -> {
            PWPUtils.renderLogo(gui, cx, (int) (height * 0.16f));
        });

        renderElement(gui, elapsed, 100, ANIM_WELCOME_DURATION, (fade) -> {
            renderWelcome(gui, cx, cy, fade);
        });

        renderButtons(gui, mouseX, mouseY, partialTick, elapsed);
        renderFooter(gui, elapsed);
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

    private void renderWelcome(GuiGraphics gui, int cx, int cy, float fade) {
        var font = Minecraft.getInstance().font;
        Minecraft mc = Minecraft.getInstance();

        String nickname = mc.player != null ? mc.player.getScoreboardName() : null;
        String welcome = nickname != null
            ? Component.translatable("pwp_core.main_menu.welcome", nickname).getString()
            : "Добро пожаловать!";

        int welcomeY = (int) (height * 0.34f);
        int slideY = (int) ((1 - fade) * 6);

        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(0, slideY, 0);

        int textColor = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_PRIMARY, (int) (fade * 255));
        int subColor = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, (int) (fade * 180));

        gui.drawCenteredString(font, Component.literal(welcome), cx, welcomeY, textColor);
        gui.drawCenteredString(font, Component.translatable("pwp_core.main_menu.subtitle"), cx, welcomeY + 16, subColor);

        pose.popPose();
    }

    private void renderButtons(GuiGraphics gui, int mouseX, int mouseY, float partialTick, float elapsed) {
        PWPButton[] btns = { playBtn, singleBtn, optionsBtn, quitBtn };

        for (int i = 0; i < btns.length; i++) {
            float progress = animProgress(elapsed, 300 + i * ANIM_DELAY_PER_MS, ANIM_BTN_DURATION);

            if (progress <= 0) {
                btns[i].visible = false;
                continue;
            }

            float t = Math.min(progress, 1);
            float eased = Easing.easeOutBack(t);
            int slideY = (int) ((1 - eased) * 14);

            btns[i].setAnimAlpha(eased);
            btns[i].visible = true;

            PoseStack pose = gui.pose();
            pose.pushPose();
            pose.translate(0, slideY, 0);
            btns[i].render(gui, mouseX, mouseY, partialTick);
            pose.popPose();
        }

        playBtn.setAnimAlpha(1.0F);
        singleBtn.setAnimAlpha(1.0F);
        optionsBtn.setAnimAlpha(1.0F);
        quitBtn.setAnimAlpha(1.0F);
    }

    private void renderFooter(GuiGraphics gui, float elapsed) {
        float progress = animProgress(elapsed, 300 + 4 * ANIM_DELAY_PER_MS + 100, 300);
        if (progress <= 0) return;
        float fade = Easing.easeOutCubic(Math.min(progress, 1));

        var font = Minecraft.getInstance().font;
        int color = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_DIM, (int) (fade * 140));
        gui.drawCenteredString(font, Component.literal("PWP v1.0.1"), width / 2, height - 12, color);
    }

    private void renderElement(GuiGraphics gui, float elapsed, long delay, long duration, ElementRenderer renderer) {
        float progress = animProgress(elapsed, delay, duration);
        if (progress <= 0) return;
        float fade = Easing.easeOutCubic(Math.min(progress, 1));
        renderer.render(fade);
    }

    private float animProgress(float elapsed, long delay, long duration) {
        float t = (elapsed - delay) / duration;
        return Math.min(t, 1);
    }

    @FunctionalInterface
    private interface ElementRenderer {
        void render(float fade);
    }
}
